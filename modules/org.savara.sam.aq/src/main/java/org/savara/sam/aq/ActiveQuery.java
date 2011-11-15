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
 * This class represents the Active Query.
 *
 * @param <T> The element type
 */
public interface ActiveQuery<T> {

	/**
	 * This method returns the name of the active query.
	 * 
	 * @return The name
	 */
	public String getName();
	
	/**
	 * This method returns the predicate associated with the
	 * active query.
	 * 
	 * @return The predicate
	 */
	public Predicate<T> getPredicate();
	
	/**
	 * This method returns the change handler responsible
	 * for dealing with notifications from the parent
	 * query.
	 * 
	 * @return The change handler
	 */
	public ActiveListener<T> getChangeHandler();
	
	/**
	 * This method adds a listener that will be notified
	 * when changes occur to the results associated with the
	 * active query.
	 * 
	 * @param l The listener
	 */
	public void addActiveListener(ActiveListener<T> l);
	
	/**
	 * This method removes an active listener.
	 * 
	 * @param l The listener
	 */
	public void removeActiveListener(ActiveListener<T> l);
	
	/**
	 * This method evaluates the supplied value to determine
	 * if it should be added to the active query.
	 * 
	 * @param value The value
	 * @return Whether the value was added
	 */
	public boolean add(T value);
	
	/**
	 * This method evaluates the supplied value to determine
	 * if it should be updated within the active query.
	 * 
	 * @param value The value
	 * @return Whether the value was updated
	 */
	public boolean update(T value);
	
	/**
	 * This method evaluates the supplied value to determine
	 * if it should be removed from the active query.
	 * 
	 * @param value The value
	 * @return Whether the value was removed
	 */
	public boolean remove(T value);

	/**
	 * This method returns a list containing
	 * the results associated with this active query.
	 * 
	 * Note that if this active query is created as a
	 * locally maintained active query, then a read-only
	 * list will be returned, to avoid being effected
	 * by updates being applied to the contents.
	 * 
	 * @return The list
	 */
	public java.util.List<T> getContents();

	/**
	 * This method determines whether a value exists in the
	 * Active Query contents.
	 * 
	 * @param value The value
	 * @return Whether the value exists in the contents
	 */
	public boolean contains(T value);

	/**
	 * This method returns the size of the result set associated
	 * with the active query.
	 * 
	 * @return The size
	 */
	public int size();

}
