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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;

import org.savara.sam.aq.ActiveChangeType;
import org.savara.sam.aq.ActiveListener;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.ActiveQueryProxy;
import org.savara.sam.aq.Predicate;

public class JEEActiveQueryProxy<T> extends ActiveQueryProxy<T> {

	private static final Logger LOG=Logger.getLogger(JEEActiveQueryProxy.class.getName());
	
	private org.infinispan.Cache<String, ActiveQuery<?>> _cache;
	private boolean _sentInitRequest=false;
	
	private ClassLoader _classLoader=Thread.currentThread().getContextClassLoader();
	private java.util.concurrent.BlockingQueue<Notification> _notifications=
				new java.util.concurrent.ArrayBlockingQueue<Notification>(100);

	public JEEActiveQueryProxy(String activeQueryName, org.infinispan.Cache<String, ActiveQuery<?>> cache) {
		super(activeQueryName, null);
		
		_cache = cache;	
		
		if (LOG.isLoggable(Level.FINEST)) {
			LOG.finest("Create JEE ActiveQueryProxy "+this+" for AQ "+activeQueryName+
							" with classloader: "+_classLoader);
		}
		
		// Start thread to receive notifications and dispatch them
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						Notification n=_notifications.take();
						n.apply(JEEActiveQueryProxy.this);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected ActiveQuery<T> getSource() {
		ActiveQuery<T> ret=(ActiveQuery<T>)_cache.get(getName());
		
		if (LOG.isLoggable(Level.FINEST)) {
			LOG.finest("Initial AQ '"+getName()+"' = "+ret);
		}

		if (ret == null && !_sentInitRequest) {
			try {
				// Send init request
				Destination dest=ActiveQueryServer.getSession().createQueue(getName());
				MessageProducer mp=ActiveQueryServer.getSession().createProducer(dest);
				
				TextMessage m=ActiveQueryServer.getSession().createTextMessage(AQDefinitions.INIT_COMMAND);				
				mp.send(m);
				
				mp.close();
				
				if (LOG.isLoggable(Level.FINE)) {
					LOG.fine("Sent 'init' command for AQ '"+getName()+"' on destination: "+dest);
				}
				
				_sentInitRequest = true;
				
			} catch(Exception e) {
				LOG.log(Level.SEVERE, "Failed to send initialisation request", e);
			}
		}
		
		if (LOG.isLoggable(Level.FINEST)) {
			LOG.finest("Returning AQ '"+getName()+"' = "+ret);
		}

		return (ret);
	}
	
	public void addActiveListener(ActiveListener<T> l) {
		if (numberOfActiveListeners() == 0) {
			// Subscribe to the topic associated with the active query name
			init();
		}
		super.addActiveListener(l);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean add(T value) {
		return(submitChange(value, ActiveChangeType.Add));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean update(T value) {
		return(submitChange(value, ActiveChangeType.Update));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean remove(T value) {
		return(submitChange(value, ActiveChangeType.Remove));
	}
	
	protected boolean submitChange(T value, ActiveChangeType changeType) {
		boolean ret=false;

		if (changeType == ActiveChangeType.Add || changeType == ActiveChangeType.Update) {
			Predicate<T> predicate=getPredicate();
		
			ret = (predicate == null || predicate.evaluate(value));
		} else if (changeType == ActiveChangeType.Remove) {
			
			ret = contains(value);
		}
		
		if (ret) {
			// Submit addition to AQ queue
			try {
				javax.jms.ObjectMessage om=ActiveQueryServer.getSession().createObjectMessage(
							(java.io.Serializable)value);
				
				om.setStringProperty(AQDefinitions.AQ_CHANGETYPE_PROPERTY, changeType.name());
				
				om.setStringProperty(AQDefinitions.ACTIVE_QUERY_NAME, getName());
				
				Destination dest=ActiveQueryServer.getSession().createQueue(getName());
				MessageProducer mp=ActiveQueryServer.getSession().createProducer(dest);
				
				mp.send(om);
				
				mp.close();
				
			} catch(Exception e) {
				LOG.log(Level.SEVERE, "Failed to change '"+changeType.name()+
						"' to AQ '"+getName()+"'", e);
				ret = false;
			}
		}
		
		return(ret);
	}
	
	@SuppressWarnings("unchecked")
	protected void notifyAddition(Object val) {
		super.notifyAddition((T)val);
	}
	
	protected void notifyAddition(final byte[] val) {
		_notifications.add(new Notification(val, ActiveChangeType.Add));
	}
	
	public void removeActiveListener(ActiveListener<T> l) {
		super.removeActiveListener(l);
		if (numberOfActiveListeners() == 0) {
			// Subscribe to the topic associated with the active query name
			close();
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void notifyRemoval(Object val) {
		super.notifyRemoval((T) val);
	}
	
	protected void notifyRemoval(final byte[] val) {
		_notifications.add(new Notification(val, ActiveChangeType.Remove));
	}
	
	@Override
	protected void notifyRefresh() {
		super.notifyRefresh();
		_sentInitRequest = false;
	}
	
	protected void init() {
		ActiveQueryNotificationManager.register(this);
	}
	
	protected void close() {
		ActiveQueryNotificationManager.unregister(this);
	}

	public Object getObject(byte[] bArr){
		try{
			java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(bArr);
			CLObjectInputStream ois = new CLObjectInputStream(bais, _classLoader);
			return ois.readObject();
		} catch(Exception err) {
			err.printStackTrace();
		}
		return null;
	}

	protected class Notification {
		private byte[] _value=null;
		private ActiveChangeType _changeType=null;
		
		public Notification(byte[] b, ActiveChangeType changeType) {
			_value = b;
			_changeType = changeType;
		}
		
		public void apply(JEEActiveQueryProxy<?> proxy) {
			
			Object value=getObject(_value);
			
			if (value == null) {
				LOG.severe("Active Query '"+JEEActiveQueryProxy.this.getName()+
							" trying to send 'null' notification");
			} else {
			
				if (value instanceof java.util.List<?>) {
					for (Object val : (java.util.List<?>)value) {
						if (_changeType == ActiveChangeType.Add) {
							proxy.notifyAddition(val);
						} else if (_changeType == ActiveChangeType.Remove) {
							proxy.notifyRemoval(val);
						}
					}
				} else {
					if (_changeType == ActiveChangeType.Add) {
						proxy.notifyAddition(value);
					} else if (_changeType == ActiveChangeType.Remove) {
						proxy.notifyRemoval(value);
					}
				}
			}
		}
	}
	
	/**
	 * Alternate ObjectInputStream implementation required to load the classes
	 * being deserialized from a nominated classloader.
	 *
	 */
	public static class CLObjectInputStream extends java.io.ObjectInputStream {

		private ClassLoader _classLoader=null;
		
		public CLObjectInputStream(java.io.InputStream in, ClassLoader cl) throws java.io.IOException {
			super(in);
			_classLoader = cl;
		}

		@Override
		public Class<?> resolveClass(java.io.ObjectStreamClass desc) throws java.io.IOException,
							ClassNotFoundException {
			try {
				Class<?> ret=_classLoader.loadClass(desc.getName());

				if (LOG.isLoggable(Level.FINEST)) {
					LOG.finest("Loading class '"+desc.getName()+"' = "+ret);				
				}
				
				return ret;
			} catch (Exception e) {
				LOG.severe("Failed to load class '"+desc.getName()+"': "+e);
			}
		
			return super.resolveClass(desc);
		}
	}
}
