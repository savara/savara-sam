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
 * This interface represents a listener for notifications
 * regarding changes to an active query.
 *
 */
public interface ActiveListener<T> {

	/**
	 * This method notifies the listener that the supplied
	 * value has been added to the active query.
	 * 
	 * @param value The value
	 */
	public void valueAdded(T value);
	
	/**
	 * This method notifies the listener that the supplied
	 * value has been updated within the active query.
	 * 
	 * @param value The value
	 */
	public void valueUpdated(T value);
	
	/**
	 * This method notifies the listener that the supplied
	 * value has been removed from the active query.
	 * 
	 * @param value The value
	 */
	public void valueRemoved(T value);
	
	/**
	 * This method notifies the listener that the active
	 * query has been refreshed.
	 */
	public void refresh();
	
}
