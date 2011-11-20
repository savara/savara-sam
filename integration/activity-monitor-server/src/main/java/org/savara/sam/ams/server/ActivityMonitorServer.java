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
package org.savara.sam.ams.server;

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

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.savara.sam.activity.ActivityModel;
import org.savara.sam.activity.ActivityModel.Activity;
import org.savara.sam.aq.ActiveChangeType;
import org.savara.sam.aqs.AQDefinitions;

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
	
	@Resource(mappedName = "java:/queue/aq/Root")
	Destination _root;
	
	@Resource(mappedName="java:jboss/infinispan/sam")
	private org.infinispan.manager.CacheContainer _container;
	private org.infinispan.Cache<String, org.savara.sam.activity.ActivityModel.Activity> _cache;
	 
	private static Connection _connection=null;
	private static Session _session=null;
	private static MessageProducer _producer=null;
	//private static java.util.Random _random=new java.util.Random();
	//private static int _messageCount=0;
	
	private static int _amsCount=0;
	
	private static java.util.List<String> _messageIds=new java.util.Vector<String>();
	
	public ActivityMonitorServer() {
	}
	
	@PostConstruct
	public void init() {
		LOG.info("Initialize Activity Monitor Server");
		
		_amsCount++;
		
		if (_connection == null) {
			try {
				_connection = _connectionFactory.createConnection();
				_session = _connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
				
				_producer = _session.createProducer(_root);
			} catch(Exception e) {
				LOG.log(Level.SEVERE, "Failed to initialize JMS", e);
			}
		}
			
		_cache = _container.getCache("activities");
	}
	
	@PreDestroy
	public void close() {
		LOG.info("Closing Activity Monitor Server");
		
		_amsCount--;
		
		if (_amsCount <= 0) {
			try {
				_session.close();
				_connection.close();
			} catch(Exception e) {
				LOG.log(Level.SEVERE, "Failed to close JMS", e);
			}
			
			_connection = null;
		}
	}

	public void onMessage(Message message) {
		
		boolean handle=false;
		
		// Filter out duplicates
		try {
			synchronized(_messageIds) {
				if (!_messageIds.contains(message.getJMSMessageID())) {
					_messageIds.add(message.getJMSMessageID());
					handle = true;
	
					// Check if some messages should be flushed
					if (_messageIds.size() > 5000) {
						for (int i=0; i < 1000; i++) {
							_messageIds.remove(0);
						}
					}					
				}
			}
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to manage message ids", e);
		}

		if (handle && message instanceof BytesMessage) {
			boolean finished=false;
			
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("ActivityMonitorServer("+this+"): Received activity event batch: "+message);
			}
			
			// TODO: Need to provide utility mechanism for building messages
			// for sending to an active query (or analyser)
			//java.util.Vector<ActivitySummary> list=new java.util.Vector<ActivitySummary>();
			java.util.Vector<String> list=new java.util.Vector<String>();
			
			do {
				try {
					int len=((BytesMessage)message).readInt();
				
					if (len > 0) {
						byte[] b=new byte[len];
						
						((BytesMessage)message).readBytes(b);
							
						Activity act=ActivityModel.Activity.parseFrom(b);
						
						// Store message in the database and get id
						String id=UUID.randomUUID().toString();
						
						// Set the id on the activity object
						act = act.toBuilder().setId(id).build();
						
						// Store message in the cache
						if (LOG.isLoggable(Level.FINEST)) {
							LOG.finest("Storing activity '"+id+"' in cache");
						}
						
						_cache.put(id, act);
						
						if (LOG.isLoggable(Level.FINEST)) {
							LOG.finest("Stored activity '"+id+"' in cache");
						}
						
						// Create the activity summary
						list.add(id); //new ActivitySummary(id, act));
						
					} else {
						finished = true;

						Message m=_session.createObjectMessage(list);
						m.setStringProperty(AQDefinitions.AQ_CHANGETYPE_PROPERTY, ActiveChangeType.Add.name());
						
						if (LOG.isLoggable(Level.FINE)) {
							LOG.fine("ActivityMonitorServer ("+this+") : Sending activity: "+m);
						}
						
						_producer.send(m);
					}
				} catch(Exception e) {
					LOG.log(Level.SEVERE, "Failed to process activity event batch", e);
				}
			} while (!finished);
		}
	}
}
