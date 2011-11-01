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
package org.savara.sam.aq.root;

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
import org.savara.sam.activity.ActivitySummary;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.DefaultActiveQuery;

@MessageDriven(name = "Root", messageListenerInterface = MessageListener.class,
               activationConfig =
                     {
                        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/aq/Root")
                     })
@TransactionManagement(value= TransactionManagementType.CONTAINER)
@TransactionAttribute(value= TransactionAttributeType.REQUIRED)
public class Root implements MessageListener {
	
	private static final Logger LOG=Logger.getLogger(Root.class.getName());
	
	private static final String ACTIVE_QUERY_NAME = "Root";

	@Resource(mappedName = "java:/JmsXA")
	ConnectionFactory _connectionFactory;
	
	Connection _connection=null;
	Session _session=null;
	MessageProducer _purchasingProducer=null;
	MessageProducer _rootTopicProducer=null;

	@Resource(mappedName = "java:/queues/aq/Purchasing")
	Destination _purchasing;
	
	@Resource(mappedName = "java:/topics/aq/Root")
	Destination _rootTopic;
	
	//@Inject
	//ActiveQueryManager _activeQueryManager;
	
	@Resource(mappedName="java:jboss/infinispan/sam")
	private org.infinispan.manager.CacheContainer _container;
	private org.infinispan.Cache<String, DefaultActiveQuery<ActivitySummary>> _cache;
	
	public Root() {
	}
	
	@PostConstruct
	public void init() {

		_cache = _container.getCache("queries");
		
		try {
			_connection = _connectionFactory.createConnection();
			_session = _connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
			_purchasingProducer = _session.createProducer(_purchasing);
			_rootTopicProducer = _session.createProducer(_rootTopic);
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

	protected ActiveQuery<ActivitySummary> getActiveQuery() {
		DefaultActiveQuery<ActivitySummary> ret=_cache.get(ACTIVE_QUERY_NAME);
		
		if (ret == null) {
			ret = new DefaultActiveQuery<ActivitySummary>(ACTIVE_QUERY_NAME, null);
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
				
				ActiveQuery<ActivitySummary> aq=getActiveQuery();
				
				if (aq.add(activity)) {
					
					// Propagate to child queries and topics
					if (LOG.isLoggable(Level.FINEST)) {
						LOG.finest("AQ "+ACTIVE_QUERY_NAME+" propagate activity = "+activity);
					}
					
					Message m=_session.createObjectMessage(activity);
					m.setBooleanProperty("include", true); // Whether activity should be added or removed
					
					_purchasingProducer.send(m);
					_rootTopicProducer.send(m);
					
				} else if (LOG.isLoggable(Level.FINEST)) {
					LOG.finest("AQ "+ACTIVE_QUERY_NAME+" ignore activity = "+activity);
				}
				
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}
