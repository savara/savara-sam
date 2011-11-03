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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.savara.sam.aq.ActiveListener;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.ActiveQueryProxy;

public class JEEActiveQueryProxy<T> extends ActiveQueryProxy<T> {

	private static final Logger LOG=Logger.getLogger(JEEActiveQueryProxy.class.getName());
	
	private org.infinispan.Cache<String, ActiveQuery<?>> _cache;
	private Session _session=null;
	private boolean _sentInitRequest=false;

	public JEEActiveQueryProxy(String activeQueryName, Session session, org.infinispan.Cache<String, ActiveQuery<?>> cache) {
		super(activeQueryName, null);
		
		_session = session;
		_cache = cache;	
		
		if (LOG.isLoggable(Level.FINEST)) {
			LOG.finest("Create JEE ActiveQueryProxy "+this+" for AQ "+activeQueryName);
		}
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
				Destination dest=_session.createQueue(getName());
				MessageProducer mp=_session.createProducer(dest);
				
				TextMessage m=_session.createTextMessage("init");				
				mp.send(m);
				
				//mp.close();
				
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
	
	@SuppressWarnings("unchecked")
	protected void notifyAddition(Object val) {
		super.notifyAddition((T)val);
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
}
