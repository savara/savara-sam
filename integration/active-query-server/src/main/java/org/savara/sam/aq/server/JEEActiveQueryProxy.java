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

import org.jboss.logging.Logger;
import org.savara.sam.aq.ActiveListener;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.ActiveQueryProxy;

public class JEEActiveQueryProxy<T> extends ActiveQueryProxy<T> {

	private static final Logger LOG=Logger.getLogger(JEEActiveQueryProxy.class.getName());

	public JEEActiveQueryProxy(ActiveQuery<T> aq) {
		super(aq);
		if (LOG.isInfoEnabled()) {
			LOG.info("Create JEE ActiveQueryProxy "+this+" for AQ "+aq.getName());
		}
	}
	
	public void addActiveListener(ActiveListener<T> l) {
		if (numberOfActiveListeners() == 0) {
			// Subscribe to the topic associated with the active query name
LOG.info(">> SUBSCRIBE TO NOTIFICATIONS");
			init();
		}
LOG.info(">> Add listener "+l+": size before="+this.numberOfActiveListeners());
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
	
	protected void init() {
		ActiveQueryNotificationManager.register(this);
	}
	
	protected void close() {
		ActiveQueryNotificationManager.unregister(this);
	}
}
