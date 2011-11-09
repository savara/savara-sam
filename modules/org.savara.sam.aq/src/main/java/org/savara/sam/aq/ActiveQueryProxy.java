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

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents the Active Query.
 *
 * @param <T> The element type
 */
public class ActiveQueryProxy<T> implements ActiveQuery<T> {

	private static final Logger LOG=Logger.getLogger(ActiveQueryProxy.class.getName());
	
	private String _name=null;
	private ActiveQuery<T> _activeQuery=null;
	private java.util.Set<ActiveListener<T>> _listeners=new java.util.HashSet<ActiveListener<T>>();
	private ChangeHandler _changeHandler=new ChangeHandler();

	/**
	 * This is the constructor for the active query proxy.
	 * 
	 * @param name The query name
	 * @param aq The active query
	 */
	public ActiveQueryProxy(String name, ActiveQuery<T> aq) {
		_name = name;
		_activeQuery = aq;
	}
	
	protected ActiveQuery<T> getSource() {
		return (_activeQuery);
	}
	
	/**
	 * This method returns the name of the active query.
	 * 
	 * @return The name
	 */
	public String getName() {
		return (_name);
	}
	
	/**
	 * This method returns the predicate associated with the
	 * active query.
	 * 
	 * @return The predicate
	 */
	public Predicate<T> getPredicate() {
		ActiveQuery<T> aq=getSource();
		if (aq == null) {
			return (null);
		}
		return (getSource().getPredicate());
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ActiveListener<T> getChangeHandler() {
		return (_changeHandler);
	}
	
	/**
	 * This method adds a listener that will be notified
	 * when changes occur to the results associated with the
	 * active query.
	 * 
	 * @param l The listener
	 */
	public void addActiveListener(ActiveListener<T> l) {
		synchronized(_listeners) {
			_listeners.add(l);
			
			if (LOG.isLoggable(Level.FINER)) {
				LOG.finer("Added active listener "+l+" - now "+_listeners.size()+" listeners");
			}
		}
	}
	
	/**
	 * This method removes an active listener.
	 * 
	 * @param l The listener
	 */
	public void removeActiveListener(ActiveListener<T> l) {
		synchronized(_listeners) {
			_listeners.remove(l);
			
			if (LOG.isLoggable(Level.FINER)) {
				LOG.finer("Removed active listener "+l+" - now "+_listeners.size()+" listeners");
			}
		}
	}
	
	protected int numberOfActiveListeners() {
		return(_listeners.size());
	}
	
	/**
	 * This method evaluates the supplied value to determine
	 * if it should be added to active query.
	 * 
	 * @param value The value
	 * @return Whether the value was added
	 */
	public boolean add(T value) {
		boolean ret=false;
		
		ActiveQuery<T> aq=getSource();
		
		if (aq != null) {
			ret = aq.add(value);
			if (ret) {
				notifyAddition(value);
			}
		}
		
		return (ret);
	}
	
	protected void notifyAddition(T value) {
		if (_listeners.size() > 0) {
			synchronized(_listeners) {
				for (ActiveListener<T> l : _listeners) {
					if (LOG.isLoggable(Level.FINEST)) {
						LOG.finest("Dispatching addition of '"+value+"' to: "+l);
					}
					l.valueAdded(value);						
				}
			}
		} else if (LOG.isLoggable(Level.FINEST)) {
			LOG.finest("Dispatching addition of '"+value+"' but no listeners");
		}
	}
	
	/**
	 * This method evaluates the supplied value to determine
	 * if it should be removed to active query.
	 * 
	 * @param value The value
	 * @return Whether the value was added
	 */
	public boolean remove(T value) {
		boolean ret=getSource().remove(value);
			
		if (ret) {
			notifyRemoval(value);						
		}
		
		return (ret);
	}
	
	protected void notifyRemoval(T value) {
		if (_listeners.size() > 0) {
			synchronized(_listeners) {
				for (ActiveListener<T> l : _listeners) {
					if (LOG.isLoggable(Level.FINEST)) {
						LOG.finest("Dispatching removal of '"+value+"' to: "+l);
					}
					l.valueRemoved(value);						
				}
			}
		} else if (LOG.isLoggable(Level.FINEST)) {
			LOG.finest("Dispatching removal of '"+value+"' but no listeners");
		}
	}
	
	/**
	 * This method returns the iterator for the results.
	 * 
	 * @return The iterator
	 */
	@SuppressWarnings("unchecked")
	public java.util.Iterator<T> getResults() {
		ActiveQuery<T> aq=getSource();
		
		if (aq != null) {
			return (getSource().getResults());
		}
		
		return (Collections.EMPTY_LIST.iterator());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public java.util.List<T> getContents() {
		ActiveQuery<T> aq=getSource();
		
		if (aq != null) {
			return (getSource().getContents());
		}
		
		return ((java.util.List<T>)Collections.EMPTY_LIST);
	}

	protected void notifyRefresh() {
		if (_listeners.size() > 0) {
			synchronized(_listeners) {
				for (ActiveListener<T> l : _listeners) {
					l.refresh();						
				}
			}
		}
	}
	
	/**
	 * This method returns the size of the result set associated
	 * with the active query.
	 * 
	 * @return The size
	 */
	public int size() {
		ActiveQuery<T> aq=getSource();
		
		if (aq != null) {
			return (getSource().size());
		}
		
		return (0);
	}
	
	/**
	 * This class provides the change handler for the
	 * active query proxy.
	 *
	 */
	public class ChangeHandler implements ActiveListener<T> {
		
		public ChangeHandler() {
		}

		public void valueAdded(T value) {
			add(value);
		}

		public void valueRemoved(T value) {
			remove(value);
		}
		
		public void refresh() {
			notifyRefresh();
		}
	}
}
