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
package org.savara.sam.ams.conversations;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Destination;
import javax.jms.MessageListener;

//import org.infinispan.context.Flag;
import org.infinispan.context.Flag;
import org.savara.monitor.ConversationId;
import org.savara.monitor.Message;
import org.savara.monitor.MonitorResult;
import org.savara.protocol.ProtocolCriteria;
import org.savara.protocol.ProtocolCriteria.Direction;
import org.savara.protocol.ProtocolId;
import org.savara.protocol.repository.ProtocolRepository;
import org.savara.sam.activity.ActivityModel.Activity;
import org.savara.sam.activity.ConversationDetails;
import org.savara.sam.activity.ServiceModel;
import org.savara.sam.activity.ServiceModel.ServiceInvocation;
import org.savara.sam.activity.Situation;
import org.savara.sam.activity.Situation.Priority;
import org.savara.sam.activity.Situation.Severity;
import org.savara.sam.aq.ActiveChangeType;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aqs.ActiveQueryServer;
import org.savara.sam.aqs.JEEActiveQueryManager;
import org.savara.sam.aqs.JEECacheActiveQuerySpec;
import org.scribble.common.resource.ResourceContent;
import org.scribble.protocol.DefaultProtocolContext;
import org.scribble.protocol.ProtocolContext;
import org.scribble.protocol.model.ProtocolModel;
import org.scribble.protocol.model.Role;

public abstract class ConversationManager extends JEEActiveQueryManager<String,ConversationId> implements MessageListener {
	
	private static final Logger LOG=Logger.getLogger(ConversationManager.class.getName());
	
	private org.savara.monitor.Monitor _monitor=null;
	private org.infinispan.Cache<String,Activity> _activities=null;
	//private org.infinispan.AdvancedCache<ConversationId,ConversationDetails> _conversationDetails=null;
	private org.infinispan.manager.CacheContainer _container=null;
	private ActiveQuery<Situation> _situations=null;
	
	private static java.util.concurrent.ConcurrentMap<ConversationId,ConversationDetails> _conversationDetails=
					new java.util.concurrent.ConcurrentHashMap<ConversationId,ConversationDetails>();

	private XPathConversationResolver _resolver=new XPathConversationResolver();
	
	public ConversationManager(String conversationName) {
		super(new JEECacheActiveQuerySpec<ConversationId,ConversationDetails>(conversationName,
					ConversationDetails.class, ConversationId.class), null);
	}
	
	@SuppressWarnings("unchecked")
	public void init(org.infinispan.manager.CacheContainer container,
				Destination source, Destination notification, Destination... destinations) {
		super.init(container, source, notification, destinations);
		
		_container = container;
		
		try {
			initMonitor();
			
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to initialize monitor", e);
		}
		
		_activities = _container.getCache("activities");
		
		/*
		org.infinispan.Cache<ConversationId,ConversationDetails> conversationDetails=
							_container.getCache("conversationDetails");
		
		// TODO: See if possible to get a 'lockIfAvailable' with boolean result - so instead of
		// failing and setting the cache in an inconsistent state, it retains valid transaction
		// but app could can decide how to deal with the issue
		_conversationDetails = conversationDetails.getAdvancedCache()
				.withFlags(Flag.SKIP_LOCKING);	// To ignore lock
				//.withFlags(Flag.FAIL_SILENTLY);	// To ignore lock failures
		*/
		
		((JEECacheActiveQuerySpec<ConversationId,ConversationDetails>)getActiveQuerySpec()).setCache(_conversationDetails);
		
		_situations = ActiveQueryServer.getInstance().getActiveQuery("Situations");
	}
	
	protected XPathConversationResolver getResolver() {
		return(_resolver);
	}
	
	protected void initMonitor() {
		_monitor = new org.savara.monitor.impl.DefaultMonitor();
		_monitor.setProtocolRepository(getProtocolRepository());
		_monitor.setConversationResolver(_resolver);
		_monitor.setSessionStore(new CachedSessionStore(_container));
	}

	@Override
	protected ConversationId processActivity(String sourceAQName, String id, ActiveChangeType changeType,
								int retriesLeft) throws Exception {
		ConversationId ret=null;
		
		Activity act=_activities.get(id);
		if (LOG.isLoggable(Level.FINEST)) {
			LOG.finest("Got cached activity with key '"+id+"' ret="+ret);
		}
		
		if (act == null) {
			if (LOG.isLoggable(Level.FINEST)) {
				LOG.finest("Conversation manager failed to retrieve activity '"+id+"' - retrying...");
			}
			if (retriesLeft == 0) {
				LOG.severe("Failed to process activity for conversation '"+getActiveQueryName()+"'");
			}
			throw new Exception("Failed to find activity with activity id '"+id+"' - retrying");
		} else if (LOG.isLoggable(Level.FINEST)) {
			LOG.finest("Conversation manager retrieved activity '"+id+"'");
		}
		
		Message mesg=new Message();
		mesg.setDirection(act.getServiceInvocation().getDirection() ==
					ServiceModel.ServiceInvocation.Direction.INBOUND ?
							Direction.Inbound : Direction.Outbound);
		mesg.setOperator(act.getServiceInvocation().getOperation());
		mesg.setFault(act.getServiceInvocation().getFault());
		
		for (org.savara.sam.activity.ServiceModel.Message sm :
						act.getServiceInvocation().getMessageList()) {
			mesg.getTypes().add(sm.getMessageType());
			mesg.getValues().add(sm.getContent());
		}
		
		if (LOG.isLoggable(Level.FINEST)) {
			LOG.finest("Monitor activity="+act+" message="+mesg);
		}
		
		MonitorResult result=_monitor.process(null, null, mesg);
		
		if (LOG.isLoggable(Level.FINEST)) {
			LOG.finest("Monitored activity="+act+" message="+mesg+" result="+result);
		}
		
		if (result != null) {
			
			// TODO: Could have a retry mechanism here, if the result is invalid

			if (result.getConversationId() != null) {
				
				// If retries remaining, and result is invalid, then retry
				if (retriesLeft > 0 && !result.isValid()) {
					if (LOG.isLoggable(Level.FINEST)) {
						LOG.finest("Conversation result="+result+
							" message="+mesg+" failed to validate: "+act+" - so retrying...");
					}
					throw new Exception("Conversation '"+result.getConversationId()+
							"' failed to validate: "+act);
				} else if (!result.isValid()) {
					if (LOG.isLoggable(Level.FINEST)) {
						LOG.finest("Conversation result="+result+
							"' message="+mesg+" failed");
					}
				}
				
				// Add activity summary to conversation details
				ConversationDetails details=_conversationDetails.get(result.getConversationId());
				
				if (details == null) {
					if (LOG.isLoggable(Level.FINEST)) {
						LOG.finest("Creating Conversation Details for "+result.getConversationId());
					}
					details = new ConversationDetails(result.getConversationId());
					_conversationDetails.put(result.getConversationId(), details);
				}
				
				boolean wasValid=details.isValid();
				
				details.addActivity(id, act, result);
				
				if (LOG.isLoggable(Level.FINEST)) {
					LOG.finest("Updating Conversation Details for cid="+
								result.getConversationId()+" : added activity "+act);
				}
				
				//_conversationDetails.lock(result.getConversationId());
				
				_conversationDetails.replace(result.getConversationId(), details);
				
				ret = result.getConversationId();
				
				// If failed, then record situation if this activity caused the
				// conversation instance to be invalid
				if (!result.isValid() && wasValid) {
					Situation situation=new Situation(act.getPrincipal(),
							"Conversation '"+getActiveQueryName()+"' instance '"+
								result.getConversationId()+
								"' validation failure",
								Severity.Major, Priority.High);
					
					situation.setProperty(null, "ActivityId", id);
					situation.setProperty(null, "ConversationId", result.getConversationId().getId());
					situation.setProperty(null, "InteractionSummary", getInteractionSummary(act));
					
					_situations.add(situation);
				}
				
			} else {
				LOG.severe("Monitor returned valid="+result.isValid()+" result, but with no conversation id");
			}
		}
		
		return(ret);
	}
		
	@Override
	protected ActiveChangeType processChangeType(ConversationId targetActivity, ActiveChangeType changeType) {
		ActiveChangeType ret=changeType;
		
		switch (changeType) {
		case Add:
			ActiveQuery<ConversationId> aq=getActiveQuery();
			if (aq != null) {
				// If activity already in contents, then change to update
				if (aq.getContents().contains(targetActivity)) {
					changeType = ActiveChangeType.Update;
				}
			}
			break;
		case Update:
			break;
		case Remove:
			break;
		}
		
		return(ret);
	}
	
	protected abstract ProtocolRepository getProtocolRepository();
	
	protected String getInteractionSummary(Activity act) {
		String ret=(act.getServiceInvocation().getDirection() == ServiceInvocation.Direction.INBOUND ?
						"Receiving " : "Invoking ");
				
		ret += act.getServiceInvocation().getOperation()+"(";
		
		for (int i=0; i < act.getServiceInvocation().getMessageList().size(); i++) {
			ServiceModel.Message m=act.getServiceInvocation().getMessageList().get(i);
			ret += m.getMessageType();
			
			if (i < act.getServiceInvocation().getMessageList().size()-1) {
				ret += ",";
			}
		}
		
		ret += ") on "+act.getServiceInvocation().getServiceType();
		
		return(ret);
	}
}
