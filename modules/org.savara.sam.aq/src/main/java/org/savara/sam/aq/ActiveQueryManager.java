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
package org.savara.sam.aq;

/**
 * This interface represents the active query manager responsible for
 * accessing system configured active queries, and creating local
 * active queries based on existing instances.
 *
 */
public interface ActiveQueryManager {

	/**
	 * This method returns the active query associated with the supplied name.
	 * 
	 * @param name The active query name
	 * @return The active query, or null if not found
	 */
	public <T> ActiveQuery<T> getActiveQuery(String name);

	/**
	 * This method creates a local active query based on a supplied
	 * parent active query, and a predicate.
	 * 
	 * @param parent The parent active query
	 * @param predicate The predicate
	 * @return The local active query
	 */
	public <T> ActiveQuery<T> createActiveQuery(ActiveQuery<T> parent, Predicate<T> predicate);
	
}
