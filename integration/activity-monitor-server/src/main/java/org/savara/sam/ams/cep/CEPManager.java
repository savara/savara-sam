/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008-11, Red Hat Middleware LLC, and others contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.savara.sam.ams.cep;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Destination;
import javax.jms.MessageListener;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.conf.EventProcessingOption;
import org.drools.conf.MBeansOption;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.WorkingMemoryEntryPoint;
import org.savara.sam.activity.Situation;
import org.savara.sam.aq.ActiveChangeType;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.ActiveQuerySpec;
import org.savara.sam.aqs.ActiveQueryServer;
import org.savara.sam.aqs.JEEActiveQueryManager;

public class CEPManager<S,T> extends JEEActiveQueryManager<S,T> implements MessageListener {
	
	private static final Logger LOG=Logger.getLogger(CEPManager.class.getName());
	
	private static SAMServices _services=null;
	private static java.util.Map<String,StatefulKnowledgeSession> _sessions=
						new java.util.HashMap<String,StatefulKnowledgeSession>();
	private StatefulKnowledgeSession _session=null;

	public CEPManager(ActiveQuerySpec spec, String parentName) {
		super(spec, parentName);
	}
	
	public void init(org.infinispan.manager.CacheContainer container,
				Destination source, Destination notification, Destination... destinations) {
		super.init(container, source, notification, destinations);
		
		if (_services == null) {
			_services = new SAMServicesImpl(
					ActiveQueryServer.getInstance().<Situation>getActiveQuery("Situations"));			
		}
		
		_session = createSession();
	}
	
    private StatefulKnowledgeSession createSession() {
    	StatefulKnowledgeSession ret=null;
    	
    	synchronized(_sessions) {
    		ret = _sessions.get(getActiveQueryName());
    		
    		if (ret == null) {    			
		        KnowledgeBase kbase = loadRuleBase();
		        
		        if (kbase != null) {
			        ret = kbase.newStatefulKnowledgeSession();
			        
			        if (ret != null) {
				        ret.setGlobal("services", _services);
				        ret.fireAllRules();
				        
				        _sessions.put(getActiveQueryName(), ret);
			        }
		        }
    		}
    	}

        return (ret);
    }

    private KnowledgeBase loadRuleBase() {
    	String cepRuleBase=getActiveQueryName()+".drl";
    	
        try {
        	KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder();
            builder.add(ResourceFactory.newInputStreamResource(
            		Thread.currentThread().getContextClassLoader().getResourceAsStream("/"+cepRuleBase)),
                             ResourceType.determineResourceType(cepRuleBase));
    		if (LOG.isLoggable(Level.FINE)) {
    			LOG.fine("Loaded CEP rules '"+cepRuleBase+"'");		
    		}

	        if( builder.hasErrors() ) {
	            LOG.severe("CEP rules have errors: "+builder.getErrors());
	        } else {
		        KnowledgeBaseConfiguration conf = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
		        conf.setOption( EventProcessingOption.STREAM );
		        conf.setOption( MBeansOption.ENABLED );
		        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase(getActiveQueryName(), conf );
		        kbase.addKnowledgePackages( builder.getKnowledgePackages() );
		        return kbase;
	        }
        
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, "Failed to load CEP rules '"+
            			cepRuleBase+"' for AQ '"+getActiveQueryName()+"'", e);
        }
        
        return (null);
    }

    @SuppressWarnings("unchecked")
	@Override
	protected T processActivity(String sourceAQName, S sourceActivity, ActiveChangeType changeType,
								int retriesLeft) throws Exception {
		
		// Get AQ spec for this source AQ name
		ActiveQuerySpec spec=getActiveQueryManager().getActiveQuerySpec(sourceAQName);
		
		_services.setResult(null);
		
		if (spec != null) {
			Object event=spec.resolve(sourceActivity);
			
			if (event != null) {
				// Get entry point
				// TODO: If not simple lookup, then may want to cache this
				WorkingMemoryEntryPoint entryPoint=_session.getWorkingMemoryEntryPoint(sourceAQName);
				
				if (entryPoint != null) {
					entryPoint.insert(event);
					
					// TODO: Not sure if possible to delay evaluation, until after
					// all events in batch have been processed/inserted - but then
					// how to trace the individal results??
					_session.fireAllRules();
					
				} else if (LOG.isLoggable(Level.FINEST)) {
					LOG.finest("No entry point for source AQ '"+sourceAQName+
							"' on CEP AQ '"+getActiveQueryName()+"'");
				}
				
			} else if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("Failed to process CEP event '"+sourceActivity+"'");
			}
		} else if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("No AQ spec for '"+sourceAQName+"'");
		}
		
		return((T)_services.getResult());
	}
	
    /**
     * This class implements the SAMServices interface provided to the CEP
     * rules.
     *
     * @param <T> The target active query element type
     */
	public static class SAMServicesImpl implements SAMServices {

		private ThreadLocal<Object> _result=new ThreadLocal<Object>();
		private ActiveQuery<Situation> _situations=null;
		
		public SAMServicesImpl(ActiveQuery<Situation> situations) {
			_situations = situations;
		}
		
		public void record(Situation situation) {
			_situations.add(situation);
		}
		
		public void logInfo(String info) {
			LOG.info(info);
		}

		public void logError(String error) {
			LOG.severe(error);
		}

		public void logDebug(String debug) {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine(debug);
			}
		}

		public void setResult(Object result) {
			_result.set(result);
		}

		public Object getResult() {
			return _result.get();
		}
		
	}
}
