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
package org.savara.sam.conversation;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageListener;

import org.savara.monitor.ConversationId;
import org.savara.monitor.Message;
import org.savara.monitor.MonitorResult;
import org.savara.protocol.ProtocolCriteria;
import org.savara.protocol.ProtocolCriteria.Direction;
import org.savara.protocol.ProtocolId;
import org.savara.protocol.repository.ProtocolRepository;
import org.savara.sam.activity.ActivityModel.Activity;
import org.savara.sam.activity.ActivitySummary;
import org.savara.sam.activity.ServiceModel;
import org.savara.sam.aq.ActiveChangeType;
import org.savara.sam.aq.server.JEEActiveQueryManager;
import org.scribble.common.resource.ResourceContent;
import org.scribble.protocol.DefaultProtocolContext;
import org.scribble.protocol.ProtocolContext;
import org.scribble.protocol.model.ProtocolModel;
import org.scribble.protocol.model.Role;

public class ConversationManager extends JEEActiveQueryManager<ActivitySummary,ConversationDetails> implements MessageListener {
	
	private static final Logger LOG=Logger.getLogger(ConversationManager.class.getName());
	
	private org.savara.monitor.Monitor _monitor=null;
	private org.infinispan.Cache<String,Activity> _activities=null;
	private org.infinispan.Cache<ConversationId,ConversationDetails> _conversationDetails=null;
	private org.infinispan.manager.CacheContainer _container=null;
	
	private XPathConversationResolver _resolver=new XPathConversationResolver();
	
	public ConversationManager(String conversationName) {
		super(conversationName, null);
	}
	
	public void init(String model, ConnectionFactory connectionFactory,
				org.infinispan.manager.CacheContainer container,
					Destination notification, Destination... destinations) {
		super.init(connectionFactory, container, notification, destinations);
		
		_container = container;
		
		java.net.URL url=Thread.currentThread().getContextClassLoader().getResource("/"+model);
		
		//java.net.URL url=getClass().getResource("/"+MODEL);

		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("Loading model '"+model+"' from URL: "+url);		
		}
		
		try {
			initMonitor(url.toURI());
			
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to initialize monitor", e);
		}
		
		_activities = _container.getCache("activities");
		_conversationDetails = _container.getCache("conversationDetails");
	}
	
	protected XPathConversationResolver getResolver() {
		return(_resolver);
	}
	
	protected void initMonitor(java.net.URI uri) {
		ProtocolRepository pr=new InJarProtocolRepository(uri, "Store");
		
		_monitor = new org.savara.monitor.impl.DefaultMonitor();
		_monitor.setProtocolRepository(pr);
		_monitor.setConversationResolver(_resolver);
		_monitor.setSessionStore(new CachedSessionStore(_container));
	}

	@Override
	protected ConversationDetails processActivity(ActivitySummary activity, ActiveChangeType changeType) {
		ConversationDetails ret=null;
		
		// Pull full activity event with message content
		Activity act=_activities.get(activity.getId());
		
		if (act == null) {
			LOG.severe("Conversation manager failed to retrieve activity '"+activity.getId()+"'");
			return (null);
		}
		
		Message mesg=new Message();
		mesg.setDirection(act.getServiceInvocation().getDirection() ==
					ServiceModel.ServiceInvocation.Direction.INBOUND ?
							Direction.Inbound : Direction.Outbound);
		mesg.setOperator(act.getServiceInvocation().getOperation());
		for (org.savara.sam.activity.ServiceModel.Message sm :
						act.getServiceInvocation().getMessageList()) {
			mesg.getTypes().add(sm.getMessageType());
			mesg.getValues().add(sm.getContent());
		}
		
		MonitorResult result=_monitor.process(null, null, mesg);
		
		if (LOG.isLoggable(Level.FINEST)) {
			LOG.finest("Monitored activity="+activity+" result="+result);
		}
		
		if (result != null) {
			
			// TODO: Could have a retry mechanism here, if the result is invalid

			if (result.getConversationId() != null) {
				
				// Add activity summary to conversation details
				ret = _conversationDetails.get(result.getConversationId());
				
				if (ret == null) {
					if (LOG.isLoggable(Level.FINEST)) {
						LOG.finest("Creating Conversation Details for "+result.getConversationId());
					}
					ret = new ConversationDetails(result.getConversationId());
					_conversationDetails.put(result.getConversationId(), ret);
				}
				
				ret.addActivity(activity, result);
				
				if (LOG.isLoggable(Level.FINEST)) {
					LOG.finest("Updating Conversation Details for cid="+
								result.getConversationId()+" : added activity "+activity);
				}
				_conversationDetails.replace(result.getConversationId(), ret);
				
			} else if (result.isValid()) {
				LOG.severe("Monitor returned valid result, but with no conversation id");
			}
		}
		
		return(ret);
	}
		
	public static class InJarProtocolRepository implements ProtocolRepository {
		
		private ProtocolModel _model=null;
		private java.util.Map<ProtocolId,ProtocolModel> _localModels=
						new java.util.HashMap<ProtocolId,ProtocolModel>();
		
		public InJarProtocolRepository(java.net.URI uri, String... roles) {
			
			org.scribble.protocol.parser.ProtocolParser parser=
					new org.savara.pi4soa.cdm.parser.CDMProtocolParser();
			org.scribble.protocol.projection.ProtocolProjector projector=
					new org.scribble.protocol.projection.impl.ProtocolProjectorImpl();
			ProtocolContext context=new DefaultProtocolContext();
			
			org.scribble.common.logging.Journal journal=
					new org.scribble.common.logging.CachedJournal();
			
			try {
				_model = parser.parse(context, new ResourceContent(uri), journal);
				
				// Project to relevant roles
				for (String role : roles) {
					ProtocolId pid=new ProtocolId(_model.getProtocol().getName(), role);
					
					ProtocolModel lm=projector.project(context, _model,
									new Role(role), journal);
					
					_localModels.put(pid, lm);
				}
				
			} catch(Exception e) {
				LOG.log(Level.SEVERE, "Failed to parse protocol model '"+uri+"'", e);
			}
		}

		public ProtocolModel getProtocol(ProtocolId pid) {
			return (_localModels.get(pid));
		}

		public List<ProtocolId> getProtocols(ProtocolCriteria criteria) {
			// TODO: Need to filter out the protocols that are related to
			// the supplied criteria (e.g. message types etc).
			List<ProtocolId> ret=new java.util.Vector<ProtocolId>(_localModels.keySet());
			
			if (LOG.isLoggable(Level.FINEST)) {
				LOG.finest("Get protocol ids for criteria="+criteria+" ret="+ret);
			}
			
			return (ret);
		}
	}
}
