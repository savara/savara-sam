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

import org.jboss.logging.Logger;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.ActiveQueryManager;
import org.savara.sam.aq.Predicate;

//import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;

@Typed(ActiveQueryManager.class)
//@javax.enterprise.inject.Default
//@ApplicationScoped
public class ActiveQueryServer implements ActiveQueryManager {
	
	private static final Logger LOG=Logger.getLogger(ActiveQueryServer.class.getName());
	
	@Resource(mappedName="java:jboss/infinispan/sam")
	private org.infinispan.manager.CacheContainer _container;
	private org.infinispan.Cache<String, ActiveQuery<?>> _cache;
	 
	/*
	@Resource(mappedName = "java:/JmsXA")
	ConnectionFactory _connectionFactory;
	
	Connection _connection=null;
	Session _session=null;
	MessageProducer _producer=null;
*/
	public ActiveQueryServer() {
	}
	
	/**
	 * This constructor explicitly creates an instance of the active query manager
	 * supplying the JMS connection factory and cache container.
	 * 
	 * @param cf The connection factory
	 * @param cc The cache container
	 */
	public ActiveQueryServer(org.infinispan.manager.CacheContainer cc) {
		//_connectionFactory = cf;
		_container = cc;
		
		init();	// When injection working, this won't be necessary
	}

	@SuppressWarnings("unchecked")
	public <T> ActiveQuery<T> getActiveQuery(String name) {
		return (new JEEActiveQueryProxy<T>((ActiveQuery<T>)_cache.get(name)));
	}

	public <T> ActiveQuery<T> createActiveQuery(ActiveQuery<T> parent,
			Predicate<T> predicate) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@PostConstruct
	public void init() {
		LOG.info("Initialize Active Query Server");
		
		/*
		try {
			_connection = _connectionFactory.createConnection();
			_session = _connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
			//_producer = _session.createProducer(_root);
		} catch(Exception e) {
			LOG.error("Failed to initialize JMS", e);
		}
		*/
		
		_cache = _container.getCache("queries");
	}
	
	@PreDestroy
	public void close() {
		LOG.info("Closing Active Query Server");

		/*
		try {
			_session.close();
			_connection.close();
		} catch(Exception e) {
			LOG.error("Failed to close JMS", e);
		}
		*/
	}
}
