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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents the Active Query.
 *
 * @param <T> The element type
 */
public class DefaultActiveQuery<T> implements ActiveQuery<T>, java.io.Serializable {

	private static final long serialVersionUID = -5464978674476882997L;
	
	private static final Logger LOG=Logger.getLogger(DefaultActiveQuery.class.getName());
	
	private String _name=null;
	private Predicate<T> _predicate=null;
	private java.util.List<T> _contents=new java.util.Vector<T>();
	private boolean _copyOnRead=false;

	/**
	 * The constructor for the active query.
	 * 
	 * @param name The name of the active query
	 * @param predicate The predicate
	 */
	public DefaultActiveQuery(String name, Predicate<T> predicate) {
		_name = name;
		_predicate = predicate;
	}
	
	/**
	 * The constructor for the active query.
	 * 
	 * @param name The name of the active query
	 * @param predicate The predicate
	 * @param copyOnRead Whether to copy the query results before client reads
	 */
	public DefaultActiveQuery(String name, Predicate<T> predicate, boolean copyOnRead) {
		this(name, predicate);
		_copyOnRead = copyOnRead;
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
	 * This method returns a child active query initialized from this
	 * active query as its parent.
	 * 
	 * @param name The active query name
	 * @param predicate The predicate
	 * @return The child active query
	 */
	public DefaultActiveQuery<T> createChild(String name, Predicate<T> predicate) {
		DefaultActiveQuery<T> ret=new DefaultActiveQuery<T>(name, predicate);
		
		synchronized(_contents) {
			java.util.Iterator<T> iter=_contents.iterator();
			while (iter.hasNext()) {
				T val=iter.next();
				ret.add(val);
			}
		}
		
		return(ret);
	}
	
	/**
	 * This method returns the predicate associated with the
	 * active query.
	 * 
	 * @return The predicate
	 */
	public Predicate<T> getPredicate() {
		return (_predicate);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ActiveListener<T> getChangeHandler() {
		throw new UnsupportedOperationException("Cannot support active listeners on persistent form of active query");
	}
	
	/**
	 * This method adds a listener that will be notified
	 * when changes occur to the results associated with the
	 * active query.
	 * 
	 * @param l The listener
	 */
	public void addActiveListener(ActiveListener<T> l) {
		throw new UnsupportedOperationException("Cannot support active listeners on persistent form of active query");
	}
	
	/**
	 * This method removes an active listener.
	 * 
	 * @param l The listener
	 */
	public void removeActiveListener(ActiveListener<T> l) {
		throw new UnsupportedOperationException("Cannot support active listeners on persistent form of active query");
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean add(T value) {
		boolean ret=false;
		
		if (evaluate(value)) {
			ret = true;
			
			synchronized(_contents) {
				// Check if already added, in case duplicate notifications
				if (!_contents.contains(value)) {
					_contents.add(value);
				}
			}
		}
		
		return (ret);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean update(T value) {
		boolean ret=false;
		
		if (evaluate(value)) {
			ret = true;
			
			synchronized(_contents) {
				int index=_contents.indexOf(value);
				
				if (index == -1) {
					_contents.add(value);
				} else {
					_contents.set(index, value);
				}
			}
		}
		
		return (ret);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean remove(T value) {
		boolean ret=false;
		
		if (evaluate(value)) {
			ret = true;
			
			synchronized(_contents) {
				_contents.remove(value);
			}
		}
		
		return (ret);
	}
	
	/**
	 * This method evaluates the supplied value against a
	 * configured predicate.
	 * 
	 * @param value The value
	 * @return The result of the evaluation
	 */
	protected boolean evaluate(T value) {
		boolean ret=true;

		if (_predicate != null) {
			ret = _predicate.evaluate(value);
		}
		
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("Evaluated '"+value+"' against '"+_predicate+"' = "+ret);	
		}
		
		return(ret);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public java.util.List<T> getContents() {
		if (_copyOnRead) {
			java.util.Vector<T> copy=new java.util.Vector<T>(_contents);
			return (copy);
		} else {
			return (_contents);
		}
	}
	
	/**
	 * This method determines whether a value exists in the
	 * Active Query contents.
	 * 
	 * @param value The value
	 * @return Whether the value exists in the contents
	 */
	public boolean contains(T value) {
		return(_contents.contains(value));
	}
	
	/**
	 * This method returns the size of the result set associated
	 * with the active query.
	 * 
	 * @return The size
	 */
	public int size() {
		return (_contents.size());
	}
}
