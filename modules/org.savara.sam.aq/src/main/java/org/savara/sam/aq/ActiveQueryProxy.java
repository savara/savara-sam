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
	private ActiveQuery<?> _activeQuery=null;
	private java.util.Set<ActiveListener<T>> _listeners=new java.util.HashSet<ActiveListener<T>>();
	private ChangeHandler _changeHandler=new ChangeHandler();

	/**
	 * This is the constructor for the active query proxy.
	 * 
	 * @param name The query name
	 * @param aq The active query
	 */
	public ActiveQueryProxy(String name, ActiveQuery<?> aq) {
		_name = name;
		_activeQuery = aq;
	}
	
	protected ActiveQuery<?> getSource() {
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
		ActiveQuery<?> aq=getSource();
		if (aq == null) {
			return (null);
		}
		return (transformPredicate(getSource().getPredicate()));
	}
	
	@SuppressWarnings("unchecked")
	protected Predicate<T> transformPredicate(Predicate<?> pred) {
		return((Predicate<T>)pred);
	}
	
	protected Object transformFromExternal(T value) {
		return(value);
	}
	
	@SuppressWarnings("unchecked")
	protected T transformToExternal(Object value) {
		return((T)value);
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
		
		@SuppressWarnings("unchecked")
		ActiveQuery<Object> aq=(ActiveQuery<Object>)getSource();
		
		if (aq != null) {
			ret = aq.add(transformFromExternal(value));
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
	 * {@inheritDoc}
	 */
	public boolean update(T value) {
		boolean ret=false;
		
		@SuppressWarnings("unchecked")
		ActiveQuery<Object> aq=(ActiveQuery<Object>)getSource();
		
		if (aq != null) {
			ret = aq.update(transformFromExternal(value));
			if (ret) {
				notifyUpdate(value);
			}
		}
		
		return (ret);
	}
	
	protected void notifyUpdate(T value) {
		if (_listeners.size() > 0) {
			synchronized(_listeners) {
				for (ActiveListener<T> l : _listeners) {
					if (LOG.isLoggable(Level.FINEST)) {
						LOG.finest("Dispatching update of '"+value+"' to: "+l);
					}
					l.valueUpdated(value);						
				}
			}
		} else if (LOG.isLoggable(Level.FINEST)) {
			LOG.finest("Dispatching update of '"+value+"' but no listeners");
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
		@SuppressWarnings("unchecked")
		boolean ret=((ActiveQuery<Object>)getSource()).remove(transformFromExternal(value));
			
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
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public java.util.List<T> getContents() {
		ActiveQuery<Object> aq=(ActiveQuery<Object>)getSource();
		
		if (aq != null) {
			java.util.List<T> ret=new java.util.Vector<T>();
			for (Object source : getSource().getContents()) {
				ret.add(transformToExternal(source));
			}
			return (ret);
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
		@SuppressWarnings("unchecked")
		ActiveQuery<Object> aq=(ActiveQuery<Object>)getSource();
		
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

		public void valueUpdated(T value) {
			update(value);
		}

		public void valueRemoved(T value) {
			remove(value);
		}
		
		public void refresh() {
			notifyRefresh();
		}
	}
}
