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
package org.savara.sam.aqs;

//import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.savara.sam.aq.ActiveChangeType;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.ActiveQueryManager;
import org.savara.sam.aq.ActiveQuerySpec;
import org.savara.sam.aq.DefaultActiveQuery;
import org.savara.sam.aq.Predicate;

public class JEEActiveQueryManager<S,T> implements MessageListener {
	
	private static final String AQ_RETRY_COUNT = "AQRetryCount";

	private static final Logger LOG=Logger.getLogger(JEEActiveQueryManager.class.getName());
	
	private static final int MAX_RETRY = 6;
	
	private ActiveQuerySpec _activeQuerySpec=null;
	private String _parentActiveQueryName=null;

	private java.util.List<MessageProducer> _producers=new java.util.Vector<MessageProducer>();
	private MessageProducer _source=null;
	private MessageProducer _notifier=null;

	private org.infinispan.manager.CacheContainer _container;
	private org.infinispan.Cache<String, DefaultActiveQuery<T>> _cache;
	
	private static java.util.List<String> _messageIds=new java.util.Vector<String>();

	//@Inject
	private ActiveQueryServer _activeQueryServer=null;
	
	public JEEActiveQueryManager(ActiveQuerySpec spec, String parentName) {
		_activeQuerySpec = spec;
		_parentActiveQueryName = parentName;
	}
	
	public void init(org.infinispan.manager.CacheContainer container,
						Destination source, Destination notification, Destination... destinations) {
		_container = container;		
		
		_cache = _container.getCache("queries");
		
		try {
			if (source != null) {
				_source = ActiveQueryServer.getSession().createProducer(source);
			}
			
			if (notification != null) {
				_notifier = ActiveQueryServer.getSession().createProducer(notification);
			}
			
			for (Destination d : destinations) {
				_producers.add(ActiveQueryServer.getSession().createProducer(d));
			}
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to setup JMS connection/session", e);
		}
		
		// Register AQ spec
		if (_activeQueryServer == null) {
			// TODO: Needs to be injected!
			_activeQueryServer = ActiveQueryServer.getInstance();
		}
		
		_activeQueryServer.register(getActiveQuerySpec());
	}
	
	public void close() {
		/*
		try {
			_session.close();
			//_connection.close();
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to close JMS connection/session", e);
		}
		*/
	}
	
	public ActiveQueryManager getActiveQueryManager() {
		return(_activeQueryServer);
	}
	
	protected void initRootAQ(ActiveQuery<T> root) {
	}
	
	protected ActiveQuerySpec getActiveQuerySpec() {
		return(_activeQuerySpec);
	}
	
	protected String getActiveQueryName() {
		return(getActiveQuerySpec().getName());
	}

	protected ActiveQuery<T> getActiveQuery() {
		DefaultActiveQuery<T> ret=_cache.get(getActiveQueryName());
		
		if (ret == null) {
			boolean refresh=false;
			
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("Active Query '"+getActiveQueryName()+"' not available in cache");
			}

			// Get parent AQ
			if (_parentActiveQueryName == null) {
				ret = new DefaultActiveQuery<T>(getActiveQueryName(), getPredicate());
				initRootAQ(ret);
				
				refresh = true;
				
			} else {			
				DefaultActiveQuery<T> parent=_cache.get(_parentActiveQueryName);
				
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("Parent Active Query '"+_parentActiveQueryName+"' of AQ '"
								+getActiveQueryName()+"' not available in cache");
				}

				if (parent == null) {
					// Need to go through init procedure
					sendInitRequest();
				} else {
					ret = parent.createChild(getActiveQueryName(), getPredicate());
					
					refresh = true;
				}
			}
			
			if (ret != null) {
				_cache.put(getActiveQueryName(), ret);
				
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("Creating Active Query: "+getActiveQueryName()+" = "+ret);
				}
				
				if (refresh) {
					sendRefresh();
				}
			}
		} else {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("Using existing Active Query: "+getActiveQueryName()+" = "+ret);
			}
		}
		
		return(ret);
	}
	
	protected Predicate<T> getPredicate() {
		return(null);
	}
	
	@SuppressWarnings("unchecked")
	protected T processActivity(String sourceAQName, S sourceActivity, ActiveChangeType changeType,
						int retriesLeft) throws Exception {
		return((T)sourceActivity);
	}
	
	protected ActiveChangeType processChangeType(T targetActivity, ActiveChangeType changeType) {
		return(changeType);
	}
	
	protected void sendInitRequest() {
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("Active Query '"+getActiveQueryName()
					+"' sending 'init' to parent AQ '"+_parentActiveQueryName+"'");
		}

		try {
			Message m=ActiveQueryServer.getSession().createTextMessage(AQDefinitions.INIT_COMMAND);
			Destination dest=ActiveQueryServer.getSession().createQueue(_parentActiveQueryName);
			MessageProducer mp=ActiveQueryServer.getSession().createProducer(dest);
			
			mp.send(m);
			
			mp.close();
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "AQ '"+getActiveQueryName()+
					"' failed to send init request to parent AQ '"+
					_parentActiveQueryName+"'", e);
		}
	}
	
	protected void sendRefresh() {
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("Active Query '"+getActiveQueryName()+"' sending refresh to child AQs");
		}

		try {
			Message m=ActiveQueryServer.getSession().createTextMessage(AQDefinitions.REFRESH_COMMAND);
			
			for (MessageProducer mp : _producers) {
				mp.send(m);
			}
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "AQ '"+getActiveQueryName()+
					"' failed to send refresh to child AQs", e);
		}
	}
	
	@SuppressWarnings("unchecked")
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

		if (!handle) {
			return;
		}
		
		if (message instanceof ObjectMessage) {
			try {
				ActiveQuery<T> aq=getActiveQuery();
				
				// TODO: If active query not returned, then need to postpone change
				// Can it be added back on the queue? retry?
				if (aq == null) {
					javax.jms.ObjectMessage om=ActiveQueryServer.getSession().createObjectMessage(
										((ObjectMessage)message).getObject());
					java.util.Enumeration<?> iter=message.getPropertyNames();
					while (iter.hasMoreElements()) {
						String name=(String)iter.nextElement();
						if (!name.startsWith("JMSX")) {
							om.setObjectProperty(name, message.getObjectProperty(name));
						}
					}
					
					retry(om);
				} else {
					java.util.List<S> activities=null;
					java.util.Vector<T> forwardAdditions=null;
					java.util.Vector<T> forwardUpdates=null;
					java.util.Vector<T> forwardRemovals=null;
					java.util.Vector<S> retries=null;
					
					String sourceAQName=message.getStringProperty(AQDefinitions.ACTIVE_QUERY_NAME);
					
					ActiveChangeType changeType=ActiveChangeType.valueOf(
							message.getStringProperty(AQDefinitions.AQ_CHANGETYPE_PROPERTY));
					
					Object obj=((ObjectMessage)message).getObject();

					// Workaround to deal with both single objects and lists - should
					// restructure really!
					if (obj instanceof java.util.List<?>) {
						activities = (java.util.List<S>)obj;
					} else {
						activities = new java.util.Vector<S>();
						activities.add((S)obj);
					}
					
					int retriesLeft=MAX_RETRY-getRetryCount(message);
					
					if (LOG.isLoggable(Level.FINEST)) {
						LOG.finest("Started processing AQ<"+getActiveQueryName()+"> message="+
								message+" retriesLeft="+retriesLeft+" activities="+activities);
					}
				
					for (S sourceActivity : activities) {
						T activity=null;
						
						try {
							activity = processActivity(sourceAQName, sourceActivity, changeType, retriesLeft);

							if (activity != null) {
								
								switch(processChangeType(activity, changeType)) {
								case Add:
									if (aq.add(activity)) {
										
										// Propagate to child queries and topics
										if (LOG.isLoggable(Level.FINEST)) {
											LOG.finest("AQ "+getActiveQueryName()+" propagate addition activity = "+activity);
										}
										
										if (forwardAdditions == null) {
											forwardAdditions = new java.util.Vector<T>();
										}
										
										forwardAdditions.add(activity);
										
									} else if (LOG.isLoggable(Level.FINEST)) {
										LOG.finest("AQ "+getActiveQueryName()+" ignore addition activity = "+activity);
									}
									break;
								case Update:
									if (aq.update(activity)) {
										
										// Propagate to child queries and topics
										if (LOG.isLoggable(Level.FINEST)) {
											LOG.finest("AQ "+getActiveQueryName()+" propagate update activity = "+activity);
										}
										
										if (forwardUpdates == null) {
											forwardUpdates = new java.util.Vector<T>();
										}
										
										forwardUpdates.add(activity);
										
									} else if (LOG.isLoggable(Level.FINEST)) {
										LOG.finest("AQ "+getActiveQueryName()+" ignore update activity = "+activity);
									}
									break;
								case Remove:
									if (aq.remove(activity)) {
										
										// Propagate to child queries and topics
										if (LOG.isLoggable(Level.FINEST)) {
											LOG.finest("AQ "+getActiveQueryName()+" propagate removal activity = "+activity);
										}
										
										if (forwardRemovals == null) {
											forwardRemovals = new java.util.Vector<T>();
										}
										
										forwardRemovals.add(activity);
										
									} else if (LOG.isLoggable(Level.FINEST)) {
										LOG.finest("AQ "+getActiveQueryName()+" ignore removal activity = "+activity);
									}
									break;
								}
								
							} else if (LOG.isLoggable(Level.FINEST)) {
								LOG.finest("AQ "+getActiveQueryName()+" didn't transform source activity = "+sourceActivity);
							}
						} catch(Exception e) {
							if (LOG.isLoggable(Level.FINEST)) {
								LOG.log(Level.FINEST, "AQ "+getActiveQueryName()+" initiating a retry", e);
							}
							
							if (retries == null) {
								retries = new java.util.Vector<S>();
							}
							retries.add(sourceActivity);
						}
					}
					
					if (retries != null) {
						// Send retry request with only those activities that failed
						if (LOG.isLoggable(Level.FINEST)) {
							LOG.finest("Sending retry list with: "+retries);
						}

						javax.jms.ObjectMessage om=ActiveQueryServer.getSession().createObjectMessage(retries);
						java.util.Enumeration<?> iter=message.getPropertyNames();
						while (iter.hasMoreElements()) {
							String name=(String)iter.nextElement();
							if (!name.startsWith("JMSX")) {
								om.setObjectProperty(name, message.getObjectProperty(name));
							}
						}
						
						retry(om);
					}
					
					if (forwardAdditions != null) {
						forwardChange(forwardAdditions, ActiveChangeType.Add);
					}
	
					if (forwardUpdates != null) {
						forwardChange(forwardUpdates, ActiveChangeType.Update);
					}
	
					if (forwardRemovals != null) {
						forwardChange(forwardRemovals, ActiveChangeType.Remove);
					}
					
					if (LOG.isLoggable(Level.FINEST)) {
						LOG.finest("Finished processing AQ<"+getActiveQueryName()+"> message="+
								message+" retriesLeft="+retriesLeft+" activities="+activities);
					}
				}
				
			} catch(Exception e) {
				LOG.log(Level.SEVERE, "Failed to handle activity event '"+message+"'", e);
			}
		} else if (message instanceof TextMessage) {
			try {
				String command=((TextMessage)message).getText();
				
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("AQ '"+getActiveQueryName()+"' received '"+command+"' command");
				}

				// Process command
				if (command.equals(AQDefinitions.INIT_COMMAND)) {
					// Attempt to get active query - if not available, then requests
					// init of parent	
					getActiveQuery();
				} else if (command.equals(AQDefinitions.REFRESH_COMMAND)) {
					if (getActiveQuery() == null) {
						if (LOG.isLoggable(Level.FINE)) {
							LOG.fine("Refresh of '"+getActiveQueryName()+
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
	
	protected int getRetryCount(javax.jms.Message message) throws Exception {
		int ret=0;
		
		if (message.propertyExists(AQ_RETRY_COUNT)) {
			ret = message.getIntProperty(AQ_RETRY_COUNT);
		} else {
			ret = 0;
		}

		return(ret);
	}
	
	protected boolean retry(javax.jms.ObjectMessage message) throws Exception {
		boolean ret=false;
		
		if (message.propertyExists(AQ_RETRY_COUNT)) {
			int retryCount=message.getIntProperty(AQ_RETRY_COUNT);

			if (retryCount < MAX_RETRY) {
				message.setIntProperty(AQ_RETRY_COUNT, retryCount+1);
				ret = true;
			} else {
				LOG.severe("Max retries ("+MAX_RETRY+") reached for message="+
								message+" contents="+message.getObject());
			}
		} else {
			message.setIntProperty(AQ_RETRY_COUNT, 1);
			ret = true;
		}
		
		if (ret) {
			if (LOG.isLoggable(Level.FINEST)) {
				LOG.finest("Retrying message: "+message);
			}
			_source.send(message);
		}
		
		return(ret);
	}
	
	protected void forwardChange(java.io.Serializable value, ActiveChangeType changeType)
								throws Exception {
		
		// Forward to child AQs
		Message m=ActiveQueryServer.getSession().createObjectMessage(value);
		m.setStringProperty(AQDefinitions.AQ_CHANGETYPE_PROPERTY, changeType.name());
		
		m.setStringProperty(AQDefinitions.ACTIVE_QUERY_NAME, getActiveQueryName());
		
		for (MessageProducer mp : getMessageProducers()) {
			mp.send(m);
		}

		// Send notification to interested listeners
		if (_notifier != null) {
			java.io.ByteArrayOutputStream baos=new java.io.ByteArrayOutputStream();
			java.io.ObjectOutputStream oos=new java.io.ObjectOutputStream(baos);
			oos.writeObject(value);
			
			byte[] b=baos.toByteArray();
			
			javax.jms.BytesMessage bm=ActiveQueryServer.getSession().createBytesMessage();
			bm.writeBytes(b);
			
			baos.close();
			oos.close();
			
			bm.setStringProperty(AQDefinitions.AQ_CHANGETYPE_PROPERTY, changeType.name());
			
			bm.setStringProperty(AQDefinitions.ACTIVE_QUERY_NAME, getActiveQueryName());
		
			_notifier.send(bm);
		}
	}
	
	protected java.util.List<MessageProducer> getMessageProducers() {
		return(_producers);
	}
	
	/*
	protected javax.jms.Session getJMSSession() {
		return(_session);
	}
	*/
}
