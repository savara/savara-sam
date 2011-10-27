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

import org.jboss.logging.Logger;

@MessageDriven(name = "ActiveQueryNotificationManager", messageListenerInterface = MessageListener.class,
               activationConfig =
                     {
                        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
                        @ActivationConfigProperty(propertyName = "destination", propertyValue = "topic/aq/*")
                     })
@TransactionManagement(value= TransactionManagementType.CONTAINER)
@TransactionAttribute(value= TransactionAttributeType.REQUIRED)
public class ActiveQueryNotificationManager implements MessageListener {
	
	private static final Logger LOG=Logger.getLogger(ActiveQueryNotificationManager.class.getName());
	
	private static java.util.Map<String, java.util.List<JEEActiveQueryProxy<?>>> _listeners=
							new java.util.HashMap<String, java.util.List<JEEActiveQueryProxy<?>>>();
	
	public ActiveQueryNotificationManager() {
	}
	
	public void onMessage(Message message) {
		
		try {
			if (message instanceof ObjectMessage && message.getJMSDestination() instanceof javax.jms.Topic) {
				String dest=((javax.jms.Topic)message.getJMSDestination()).getTopicName();
				Object val=((ObjectMessage)message).getObject();
				dispatch(dest, val, message.getBooleanProperty("include"));
			}
		} catch(Exception e) {
			LOG.error("Failed to handle notification", e);
		}
	}
	
	protected static void dispatch(String dest, Object val, boolean addition) {
		synchronized(_listeners) {
			java.util.List<JEEActiveQueryProxy<?>> list=_listeners.get(dest);
			if (list != null) {
				for (JEEActiveQueryProxy<?> aq : list) {
					if (LOG.isInfoEnabled()) {
						LOG.info("Dispatch "+(addition ? "ADD" : "REMOVE")+
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
			if (LOG.isInfoEnabled()) {
				LOG.info("Register QA proxy "+aq+" for notifications");
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
					if (LOG.isInfoEnabled()) {
						LOG.info("Unregister QA proxy "+aq+" for notifications");
					}
					_listeners.remove(aq.getName());
				}
			}
		}
	}
}
