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
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.savara.sam.aq.ActiveListener;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.ActiveQueryProxy;

// NOTE: Not for use in server, as cannot add message listener - but this version could be used
// possibly as part of a client side implementation?

public class JMSActiveQueryProxy<T> extends ActiveQueryProxy<T> {

	private static final Logger LOG=Logger.getLogger(JMSActiveQueryProxy.class.getName());

	ConnectionFactory _connectionFactory;
	Connection _connection;
	Session _session;
	MessageConsumer _consumer;

	public JMSActiveQueryProxy(ConnectionFactory cf, ActiveQuery<T> aq) {
		super(aq);
		_connectionFactory = cf;
	}
	
	public void addActiveListener(ActiveListener<T> l) {
		if (numberOfActiveListeners() == 0) {
			// Subscribe to the topic associated with the active query name
			init();
		}
		super.addActiveListener(l);
	}
	
	public void removeActiveListener(ActiveListener<T> l) {
		super.removeActiveListener(l);
		if (numberOfActiveListeners() == 0) {
			// Subscribe to the topic associated with the active query name
			close();
		}
	}
	
	protected void init() {
		try {
			_connection = _connectionFactory.createConnection();
			_session = _connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
			
			Destination dest=_session.createTopic(getName());
			
			_consumer = _session.createConsumer(dest);
			
			_consumer.setMessageListener(new MessageListener() {

				public void onMessage(Message m) {
					if (m instanceof ObjectMessage) {
						try {
							@SuppressWarnings("unchecked")
							T value=(T)((ObjectMessage)m).getObject();
							
							if (m.getBooleanProperty("include")) {
								notifyAddition(value);
							} else {
								notifyRemoval(value);
							}
						} catch(Exception e) {
							LOG.log(Level.SEVERE, "Failed to retrieve active query notification", e);
						}
					}
				}
			});
			
			_connection.start();
			
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to initialize JMS", e);
		}
	}
	
	protected void close() {
		try {
			_session.close();
			_connection.close();
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to close JMS", e);
		}
		
	}
}
