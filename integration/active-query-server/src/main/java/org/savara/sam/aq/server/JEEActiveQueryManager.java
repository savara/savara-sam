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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.DefaultActiveQuery;
import org.savara.sam.aq.Predicate;

public class JEEActiveQueryManager<T> implements MessageListener {
	
	private static final Logger LOG=Logger.getLogger(JEEActiveQueryManager.class.getName());
	
	private String _activeQueryName=null;
	private ConnectionFactory _connectionFactory=null;
	private Connection _connection=null;
	private Session _session=null;
	private java.util.List<MessageProducer> _producers=new java.util.Vector<MessageProducer>();

	private org.infinispan.manager.CacheContainer _container;
	private org.infinispan.Cache<String, DefaultActiveQuery<T>> _cache;
	
	public JEEActiveQueryManager(String name) {
		_activeQueryName = name;
	}
	
	public void init(ConnectionFactory connectionFactory, org.infinispan.manager.CacheContainer container,
						Destination... destinations) {
		_connectionFactory = connectionFactory;
		_container = container;		
		
		_cache = _container.getCache("queries");
		
		try {
			_connection = _connectionFactory.createConnection();
			_session = _connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
			
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

	protected ActiveQuery<T> getActiveQuery() {
		DefaultActiveQuery<T> ret=_cache.get(_activeQueryName);
		
		if (ret == null) {
			ret = new DefaultActiveQuery<T>(_activeQueryName, getPredicate());
			_cache.put(_activeQueryName, ret);
			
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("Creating Active Query: "+_activeQueryName+" = "+ret);
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
	
	public void onMessage(Message message) {
		
		if (message instanceof ObjectMessage) {
			try {
				// TODO: Replace object message with bytes, and allow multiple messages to be dealt with
				
				@SuppressWarnings("unchecked")
				T activity=(T)((ObjectMessage)message).getObject();
				
				ActiveQuery<T> aq=getActiveQuery();
				
				if (aq.add(activity)) {
					
					// Propagate to child queries and topics
					if (LOG.isLoggable(Level.FINEST)) {
						LOG.finest("AQ "+_activeQueryName+" propagate activity = "+activity);
					}
					
					Message m=_session.createObjectMessage((java.io.Serializable)activity);
					m.setBooleanProperty("include", true); // Whether activity should be added or removed
					
					for (MessageProducer mp : _producers) {
						mp.send(m);
					}

				} else if (LOG.isLoggable(Level.FINEST)) {
					LOG.finest("AQ "+_activeQueryName+" ignore activity = "+activity);
				}
				
			} catch(Exception e) {
				LOG.log(Level.SEVERE, "Failed to handle activity event '"+message+"'", e);
			}
		}
	}
}
