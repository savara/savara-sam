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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Session;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.ActiveQueryManager;
import org.savara.sam.aq.ActiveQueryProxy;
import org.savara.sam.aq.DefaultActiveQuery;
import org.savara.sam.aq.Predicate;

public class ActiveQueryServer implements ActiveQueryManager {
	
	private static final Logger LOG=Logger.getLogger(ActiveQueryServer.class.getName());
	
	@Resource(mappedName="java:jboss/infinispan/sam")
	private org.infinispan.manager.CacheContainer _container;
	
	@Resource(mappedName = "java:/JmsXA")
	private ConnectionFactory _connectionFactory;
	
	private Connection _connection=null;
	private Session _session=null;
	private org.infinispan.Cache<String, ActiveQuery<?>> _cache;
	 
	public ActiveQueryServer() {
	}

	/**
	 * {@inheritDoc}
	 */
	public <T> ActiveQuery<T> getActiveQuery(String name) {
		return (new JEEActiveQueryProxy<T>(name, _session, _cache));
	}

	/**
	 * {@inheritDoc}
	 */
	public <T> ActiveQuery<T> createActiveQuery(ActiveQuery<T> parent,
			Predicate<T> predicate) {
		ActiveQuery<T> ret=new ActiveQueryProxy<T>(null, new DefaultActiveQuery<T>(null, predicate, true));
		
		parent.addActiveListener(ret.getChangeHandler());
		
		return (ret);
	}
	
	@PostConstruct
	public void init() {
		LOG.info("Initialize Active Query Server");
		
		try {
			_connection = _connectionFactory.createConnection();
			_session = _connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to setup JMS connection/session", e);
		}

		_cache = _container.getCache("queries");
	}
	
	@PreDestroy
	public void close() {
		LOG.info("Closing Active Query Server");

		try {						
			_session.close();
			_connection.close();
			
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to close JMS connection/session", e);
		}
	}
}
