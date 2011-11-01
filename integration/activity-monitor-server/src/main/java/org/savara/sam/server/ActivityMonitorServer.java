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
package org.savara.sam.server;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.savara.sam.activity.ActivityModel;
import org.savara.sam.activity.ActivityModel.Activity;
import org.savara.sam.activity.ActivitySummary;

@MessageDriven(name = "ActivityMonitorServer", messageListenerInterface = MessageListener.class,
               activationConfig =
                     {
                        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/ActivityMonitorServer")
                     })
@TransactionManagement(value= TransactionManagementType.CONTAINER)
@TransactionAttribute(value= TransactionAttributeType.REQUIRED)
public class ActivityMonitorServer implements MessageListener {
	
	private static final Logger LOG=Logger.getLogger(ActivityMonitorServer.class.getName());
	
	@Resource(mappedName = "java:/JmsXA")
	ConnectionFactory _connectionFactory;
	
	@Resource(mappedName = "java:/queues/aq/Root")
	Destination _root;
	
	@Resource(mappedName="java:jboss/infinispan/sam")
	private org.infinispan.manager.CacheContainer _container;
	private org.infinispan.Cache<String, org.savara.sam.activity.ActivityModel.Activity> _cache;
	 
	Connection _connection=null;
	Session _session=null;
	MessageProducer _producer=null;
	java.util.Random _random=new java.util.Random();

	public ActivityMonitorServer() {
	}
	
	@PostConstruct
	public void init() {
		LOG.info("Initialize Activity Monitor Server");
		
		try {
			_connection = _connectionFactory.createConnection();
			_session = _connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
			_producer = _session.createProducer(_root);
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to initialize JMS", e);
		}
		
		_cache = _container.getCache("activities");
	}
	
	@PreDestroy
	public void close() {
		LOG.info("Closing Activity Monitor Server");
		
		try {
			_session.close();
			_connection.close();
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to close JMS", e);
		}
	}

	public void onMessage(Message message) {
		
		if (message instanceof BytesMessage) {
			// Decode messages
			
			boolean finished=false;
			
			if (LOG.isLoggable(Level.FINEST)) {
				LOG.finest("ActivityMonitorServer: Received activity event batch: "+message);
			}
			
			// TODO: Need to provide utility mechanism for building messages
			// for sending to an active query (or analyser)
			do {
				try {
					int len=((BytesMessage)message).readInt();
				
					if (len > 0) {
						byte[] b=new byte[len];
						
						((BytesMessage)message).readBytes(b);
							
						Activity act=ActivityModel.Activity.parseFrom(b);
						
						// Store message in the database and get id
						String id="id"+_random.nextInt();
						
						// Store message in the cache
						_cache.put(id, act);
						
						// Create the activity summary
						ActivitySummary summary=new ActivitySummary(id, act);
								
						Message m=_session.createObjectMessage(summary);
						m.setBooleanProperty("include", true); // Whether activity should be added or removed
						
						if (LOG.isLoggable(Level.FINEST)) {
							LOG.finest("ActivityMonitorServer: Sending activity: "+m);
						}
						
						_producer.send(m);
					} else {
						finished = true;
					}
				} catch(Exception e) {
					LOG.log(Level.SEVERE, "Failed to process activity event batch", e);
				}
			} while (!finished);
		}
	}
}
