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

import org.savara.sam.aq.ActiveQuerySpec;

public class JEECacheActiveQuerySpec extends ActiveQuerySpec {

	private org.infinispan.Cache<?,?> _cache=null;

	/**
	 * The Active Query specification constructor.
	 * 
	 * @param name The active query name
	 * @param type The type represented by the active query
	 * @param internalType Optional type that defines the internal representation
	 * 				if different from the 'type'
	 */
	public JEECacheActiveQuerySpec(String name, Class<?> type, Class<?> internalType) {
		super(name, type, internalType);
	}
	
	public void setCache(org.infinispan.Cache<?,?> cache) {
		_cache = cache;
	}
	
	public Object resolve(Object source) {
		Object ret=null;
		
		if (_cache != null) {
			ret = _cache.get(source);
		}
		
		return(ret);
	}
}
