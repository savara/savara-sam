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
package org.savara.sam.aq.purchasingresponsetime;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.savara.sam.activity.ActivityAnalysis;
import org.savara.sam.activity.ActivitySummary;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.DefaultActiveQuery;

@MessageDriven(name = "PurchasingResponseTime", messageListenerInterface = MessageListener.class,
               activationConfig =
                     {
                        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/aq/PurchasingResponseTime")
                     })
@TransactionManagement(value= TransactionManagementType.CONTAINER)
@TransactionAttribute(value= TransactionAttributeType.REQUIRED)
public class PurchasingResponseTime implements MessageListener {
	
	private static final Logger LOG=Logger.getLogger(PurchasingResponseTime.class.getName());

	private static final String ACTIVE_QUERY_NAME = "PurchasingResponseTime";

	@Resource(mappedName = "java:/JmsXA")
	ConnectionFactory _connectionFactory;
	
	Connection _connection=null;
	Session _session=null;
	MessageProducer _purchasingResponseTimeTopicProducer=null;

	@Resource(mappedName = "java:/topics/aq/PurchasingResponseTime")
	Destination _purchasingResponseTimeTopic;

	@Resource(mappedName="java:jboss/infinispan/sam")
	private org.infinispan.manager.CacheContainer _container;
	private org.infinispan.Cache<String, DefaultActiveQuery<ActivityAnalysis>> _cache;
	private org.infinispan.Cache<String, ActivitySummary> _siCache;
	
	public PurchasingResponseTime() {
	}
	
	@PostConstruct
	public void init() {
		_cache = _container.getCache("queries");
		_siCache = _container.getCache("serviceInvocations");
		
		try {
			_connection = _connectionFactory.createConnection();
			_session = _connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
			_purchasingResponseTimeTopicProducer = _session.createProducer(_purchasingResponseTimeTopic);
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to setup JMS connection/session", e);
		}
	}

	@PreDestroy
	public void close() {
		try {
			_session.close();
			_connection.close();
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to close JMS connection/session", e);
		}
	}

	protected ActiveQuery<ActivityAnalysis> getActiveQuery() {
		DefaultActiveQuery<ActivityAnalysis> ret=_cache.get(ACTIVE_QUERY_NAME);
		
		if (ret == null) {
			ret = new DefaultActiveQuery<ActivityAnalysis>(ACTIVE_QUERY_NAME, null);
			_cache.put(ACTIVE_QUERY_NAME, ret);
			
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("Creating Active Query: "+ACTIVE_QUERY_NAME+" = "+ret);
			}
		} else {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("Using existing Active Query: "+ACTIVE_QUERY_NAME+" = "+ret);
			}
		}
		
		return(ret);
	}
	
	public void onMessage(Message message) {
		
		if (message instanceof ObjectMessage) {
			try {
				ActivitySummary activity=(ActivitySummary)((ObjectMessage)message).getObject();
				
				// Check if service interaction with correlation
				if (activity.getServiceInvocation() != null &&
						activity.getServiceInvocation().getCorrelation() != null) {
					String correlation=activity.getServiceInvocation().getCorrelation();
					
					// Check if correlated invocation already exists
					ActivitySummary other=_siCache.get(correlation);
					
					if (other == null) {
						_siCache.put(correlation, activity, 150, TimeUnit.SECONDS);
					} else {
						// Create activity results object for correlated match
						ActivityAnalysis aa=new ActivityAnalysis();
						
						long responseTime=activity.getTimestamp()-other.getTimestamp();
						
						aa.addProperty("requestTimestamp", Long.class.getName(), other.getTimestamp());
						aa.addProperty("requestId", String.class.getName(), other.getId());
						aa.addProperty("responseId", String.class.getName(), activity.getId());
						aa.addProperty("responseTime", Long.class.getName(), responseTime);
						aa.addProperty("serviceType", String.class.getName(), activity.getServiceInvocation().getServiceType());
						aa.addProperty("operation", String.class.getName(), activity.getServiceInvocation().getOperation());
						aa.addProperty("fault", String.class.getName(), activity.getServiceInvocation().getFault());
						
						ActiveQuery<ActivityAnalysis> aq=getActiveQuery();
						
						if (aq.add(aa)) {
							
							// Propagate to child queries and topics
							if (LOG.isLoggable(Level.FINEST)) {
								LOG.finest("AQ "+ACTIVE_QUERY_NAME+" propagate activity = "+activity);
							}
							
							Message m=_session.createObjectMessage(aa);
							m.setBooleanProperty("include", true); // Whether activity should be added or removed
							
							_purchasingResponseTimeTopicProducer.send(m);
							
						} else if (LOG.isLoggable(Level.FINEST)) {
							LOG.finest("AQ "+ACTIVE_QUERY_NAME+" ignore activity = "+activity);
						}
					}
				}
				
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}
