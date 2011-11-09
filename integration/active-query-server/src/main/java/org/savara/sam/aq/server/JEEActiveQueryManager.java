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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.savara.sam.aq.ActiveChangeType;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.DefaultActiveQuery;
import org.savara.sam.aq.Predicate;

public class JEEActiveQueryManager<S,T> implements MessageListener {
	
	private static final Logger LOG=Logger.getLogger(JEEActiveQueryManager.class.getName());
	
	private String _activeQueryName=null;
	private String _parentActiveQueryName=null;
	private ConnectionFactory _connectionFactory=null;
	private Connection _connection=null;
	private Session _session=null;
	private java.util.List<MessageProducer> _producers=new java.util.Vector<MessageProducer>();
	private MessageProducer _notifier=null;

	private org.infinispan.manager.CacheContainer _container;
	private org.infinispan.Cache<String, DefaultActiveQuery<T>> _cache;
	
	public JEEActiveQueryManager(String name, String parentName) {
		_activeQueryName = name;
		_parentActiveQueryName = parentName;
	}
	
	public void init(ConnectionFactory connectionFactory, org.infinispan.manager.CacheContainer container,
						Destination notification, Destination... destinations) {
		_connectionFactory = connectionFactory;
		_container = container;		
		
		_cache = _container.getCache("queries");
		
		try {
			_connection = _connectionFactory.createConnection();
			_session = _connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
			
			if (notification != null) {
				_notifier = _session.createProducer(notification);
			}
			
			for (Destination d : destinations) {
				_producers.add(_session.createProducer(d));
			}
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to setup JMS connection/session", e);
		}
	}

	public void close() {
		try {
			_session.close();
			_connection.close();
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to close JMS connection/session", e);
		}
	}
	
	protected void initRootAQ(ActiveQuery<T> root) {
	}
	
	protected String getActiveQueryName() {
		return(_activeQueryName);
	}

	protected ActiveQuery<T> getActiveQuery() {
		DefaultActiveQuery<T> ret=_cache.get(_activeQueryName);
		
		if (ret == null) {
			boolean refresh=false;
			
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("Active Query '"+_activeQueryName+"' not available in cache");
			}

			// Get parent AQ
			if (_parentActiveQueryName == null) {
				ret = new DefaultActiveQuery<T>(_activeQueryName, getPredicate());
				initRootAQ(ret);
				
				refresh = true;
				
			} else {			
				DefaultActiveQuery<T> parent=_cache.get(_parentActiveQueryName);
				
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("Parent Active Query '"+_parentActiveQueryName+"' of AQ '"
								+_activeQueryName+"' not available in cache");
				}

				if (parent == null) {
					// Need to go through init procedure
					sendInitRequest();
				} else {
					ret = new DefaultActiveQuery<T>(_activeQueryName, getPredicate(), parent);
					
					refresh = true;
				}
			}
			
			if (ret != null) {
				_cache.put(_activeQueryName, ret);
				
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("Creating Active Query: "+_activeQueryName+" = "+ret);
				}
				
				if (refresh) {
					sendRefresh();
				}
			}
		} else {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("Using existing Active Query: "+_activeQueryName+" = "+ret);
			}
		}
		
		return(ret);
	}
	
	protected Predicate<T> getPredicate() {
		return(null);
	}
	
	@SuppressWarnings("unchecked")
	protected T process(S sourceActivity) {
		return((T)sourceActivity);
	}
	
	protected void sendInitRequest() {
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("Active Query '"+_activeQueryName
					+"' sending 'init' to parent AQ '"+_parentActiveQueryName+"'");
		}

		try {
			Message m=_session.createTextMessage(AQDefinitions.INIT_COMMAND);
			Destination dest=_session.createQueue(_parentActiveQueryName);
			MessageProducer mp=_session.createProducer(dest);
			
			mp.send(m);
			
			mp.close();
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "AQ '"+_activeQueryName+
					"' failed to send init request to parent AQ '"+
					_parentActiveQueryName+"'", e);
		}
	}
	
	protected void sendRefresh() {
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("Active Query '"+_activeQueryName+"' sending refresh to child AQs");
		}

		try {
			Message m=_session.createTextMessage(AQDefinitions.REFRESH_COMMAND);
			
			for (MessageProducer mp : _producers) {
				mp.send(m);
			}
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "AQ '"+_activeQueryName+
					"' failed to send refresh to child AQs", e);
		}
	}
	
	public void onMessage(Message message) {
		
		if (message instanceof ObjectMessage) {
			try {
				@SuppressWarnings("unchecked")
				java.util.List<S> activities=(java.util.List<S>)((ObjectMessage)message).getObject();
				java.util.Vector<T> forward=null;
				ActiveChangeType changeType=ActiveChangeType.valueOf(
							message.getStringProperty(AQDefinitions.AQ_CHANGETYPE_PROPERTY));
				
				ActiveQuery<T> aq=getActiveQuery();
				
				// TODO: If active query not returned, then need to postpone change
				// Can it be added back on the queue? retry?
				
				for (S sourceActivity : activities) {
					
					T activity=process(sourceActivity);
					
					if (activity != null) {
						boolean process=false;
						
						switch(changeType) {
						case Add:
							process = aq.add(activity);
							break;
						case Update:
							process = aq.update(activity);
							break;
						case Remove:
							process = aq.remove(activity);
							break;
						}
						
						if (process) {
							
							// Propagate to child queries and topics
							if (LOG.isLoggable(Level.FINEST)) {
								LOG.finest("AQ "+_activeQueryName+" propagate activity = "+activity);
							}
							
							if (forward == null) {
								forward = new java.util.Vector<T>();
							}
							
							forward.add(activity);
							
						} else if (LOG.isLoggable(Level.FINEST)) {
							LOG.finest("AQ "+_activeQueryName+" ignore activity = "+activity);
						}
					} else if (LOG.isLoggable(Level.FINEST)) {
						LOG.finest("AQ "+_activeQueryName+" didn't transform source activity = "+sourceActivity);
					}
				}
				
				if (forward != null) {
					forwardChange(forward, changeType);
				}

			} catch(Exception e) {
				LOG.log(Level.SEVERE, "Failed to handle activity event '"+message+"'", e);
			}
		} else if (message instanceof TextMessage) {
			try {
				String command=((TextMessage)message).getText();
				
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("AQ '"+_activeQueryName+"' received '"+command+"' command");
				}

				// Process command
				if (command.equals(AQDefinitions.INIT_COMMAND)) {
					// Attempt to get active query - if not available, then requests
					// init of parent	
					getActiveQuery();
				} else if (command.equals(AQDefinitions.REFRESH_COMMAND)) {
					if (getActiveQuery() == null) {
						if (LOG.isLoggable(Level.FINE)) {
							LOG.fine("Refresh of '"+_activeQueryName+
									"' returned an empty active query, so have re-initiated the parent");
						}
					} else {
						sendRefresh();
					}
				}
			} catch(Exception e) {
				LOG.log(Level.SEVERE, "Failed to handle command '"+message+"'", e);
			}
		}
	}
	
	protected void forwardChange(java.io.Serializable value, ActiveChangeType changeType)
								throws Exception {
		
		// Forward to child AQs
		Message m=_session.createObjectMessage(value);
		m.setStringProperty(AQDefinitions.AQ_CHANGETYPE_PROPERTY, changeType.name());
		
		m.setStringProperty(AQDefinitions.ACTIVE_QUERY_NAME, _activeQueryName);
		
		for (MessageProducer mp : getMessageProducers()) {
			mp.send(m);
		}

		// Send notification to interested listeners
		if (_notifier != null) {
			java.io.ByteArrayOutputStream baos=new java.io.ByteArrayOutputStream();
			java.io.ObjectOutputStream oos=new java.io.ObjectOutputStream(baos);
			oos.writeObject(value);
			
			byte[] b=baos.toByteArray();
			
			javax.jms.BytesMessage bm=_session.createBytesMessage();
			bm.writeBytes(b);
			
			baos.close();
			oos.close();
			
			bm.setStringProperty(AQDefinitions.AQ_CHANGETYPE_PROPERTY, changeType.name());
			
			bm.setStringProperty(AQDefinitions.ACTIVE_QUERY_NAME, _activeQueryName);
		
			_notifier.send(bm);
		}
	}
	
	protected java.util.List<MessageProducer> getMessageProducers() {
		return(_producers);
	}
	
	protected javax.jms.Session getJMSSession() {
		return(_session);
	}
}
