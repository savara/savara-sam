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
package org.savara.sam.aq.server;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.jboss.ejb3.annotation.Pool;
import org.jboss.ejb3.annotation.defaults.*;

import java.util.logging.Level;
import java.util.logging.Logger;

@MessageDriven(name = "ActiveQueryNotificationManager", messageListenerInterface = MessageListener.class,
               activationConfig =
                     {
                        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
                        @ActivationConfigProperty(propertyName = "destination", propertyValue = "topic/aq/Notifications"),
                        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1")
                     })
@TransactionManagement(value= TransactionManagementType.CONTAINER)
@TransactionAttribute(value= TransactionAttributeType.REQUIRED)
@Pool(value = PoolDefaults.POOL_IMPLEMENTATION_STRICTMAX, maxSize = 1, timeout = 10000)
public class ActiveQueryNotificationManager implements MessageListener {
	
	private static final Logger LOG=Logger.getLogger(ActiveQueryNotificationManager.class.getName());
	
	private static java.util.Map<String, java.util.List<JEEActiveQueryProxy<?>>> _listeners=
							new java.util.HashMap<String, java.util.List<JEEActiveQueryProxy<?>>>();
	
	private static java.util.List<String> _messageIds=new java.util.Vector<String>();
	
	public ActiveQueryNotificationManager() {
	}
	
	public void onMessage(Message message) {
		
		try {
			boolean handle=false;
			
			// TODO: Temporary workaround as setting poolsize does not seem to work,
			// so tries to process a message through two MDB instances
			synchronized(_messageIds) {
				if (!_messageIds.contains(message.getJMSMessageID())) {
					_messageIds.add(message.getJMSMessageID());
					handle = true;

					// Check if some messages should be flushed
					if (_messageIds.size() > 500) {
						for (int i=0; i < 100; i++) {
							_messageIds.remove(0);
						}
					}					
				}
			}

			if (handle) {
				String aqname=message.getStringProperty(AQDefinitions.ACTIVE_QUERY_NAME);
	
				if (message instanceof ObjectMessage) {
					
					Object val=((ObjectMessage)message).getObject();
					
					if (LOG.isLoggable(Level.FINEST)) {
						LOG.finest("Received Notification="+
									message+" for AQ="+message.getStringProperty(AQDefinitions.ACTIVE_QUERY_NAME));
					}
					
					if (val instanceof java.util.List<?>) {
						for (Object subval : (java.util.List<?>)val) {
							dispatch(aqname, subval, message.getBooleanProperty(AQDefinitions.AQ_INCLUDE_PROPERTY));
						}
					} else {
						dispatch(aqname, val, message.getBooleanProperty(AQDefinitions.AQ_INCLUDE_PROPERTY));
					}
				} else if (message instanceof TextMessage) {
					String command=((TextMessage)message).getText();
					
					dispatch(aqname, command);
				}
			}

		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to handle notification", e);
		}
	}
	
	protected static void dispatch(String dest, String command) {
		synchronized(_listeners) {
			java.util.List<JEEActiveQueryProxy<?>> list=_listeners.get(dest);
			if (list != null) {
				for (JEEActiveQueryProxy<?> aq : list) {
					if (LOG.isLoggable(Level.FINE)) {
						LOG.fine("Dispatch command '"+command+"' to AQ ["+dest+"]");
					}
					
					aq.notifyRefresh();
				}
			}
		}
	}
	protected static void dispatch(String dest, Object val, boolean addition) {
		synchronized(_listeners) {
			java.util.List<JEEActiveQueryProxy<?>> list=_listeners.get(dest);
			if (list != null) {
				for (JEEActiveQueryProxy<?> aq : list) {
					if (LOG.isLoggable(Level.FINE)) {
						LOG.fine("Dispatch "+(addition ? "ADD" : "REMOVE")+
									" notification "+val+" to AQ ["+dest+"]");
					}
					
					if (addition) {
						aq.notifyAddition(val);
					} else {
						aq.notifyRemoval(val);
					}
				}
			}
		}
	}
	
	protected static void register(JEEActiveQueryProxy<?> aq) {
		synchronized(_listeners) {
			java.util.List<JEEActiveQueryProxy<?>> list=_listeners.get(aq.getName());
			if (list == null) {
				list = new java.util.Vector<JEEActiveQueryProxy<?>>();
				_listeners.put(aq.getName(), list);
			}
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("Register QA proxy "+aq+" for notifications");
			}
			list.add(aq);
		}
	}
	
	protected static void unregister(JEEActiveQueryProxy<?> aq) {
		synchronized(_listeners) {
			java.util.List<JEEActiveQueryProxy<?>> list=_listeners.get(aq.getName());
			if (list != null) {
				list.remove(aq);
				if (list.size() == 0) {
					if (LOG.isLoggable(Level.FINE)) {
						LOG.fine("Unregister QA proxy "+aq+" for notifications");
					}
					_listeners.remove(aq.getName());
				}
			}
		}
	}
}
