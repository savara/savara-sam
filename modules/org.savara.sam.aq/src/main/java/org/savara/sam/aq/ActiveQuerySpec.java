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

public class ActiveQuerySpec {
	
	private String _name=null;
	private Class<?> _type=null;
	private Class<?> _internalType;
	
	/**
	 * The Active Query specification constructor.
	 * 
	 * @param name The active query name
	 * @param type The type represented by the active query
	 * @param internalType Optional type that defines the internal representation
	 * 				if different from the 'type'
	 */
	public ActiveQuerySpec(String name, Class<?> type, Class<?> internalType) {
		_name = name;
		_type = type;
	}

	public String getName() {
		return (_name);
	}
	
	public Class<?> getType() {
		return (_type);
	}
	
	public Class<?> getInternalType() {
		return (_internalType);
	}
	
	/**
	 * This method converts the supplied 'internal' source representation
	 * into the external representation associated with the 'type' of the
	 * Active Query. This method is only relevant where an internal representation
	 * is used, that is different to the 'type' defined for the Active Query,
	 * and is used in cases where the AQ deals with a more concise representation
	 * that only needs to be resolved to the full representation upon use.
	 * 
	 * @param source The source or internal representation
	 * @return The external representation
	 */
	public Object resolve(Object source) {
		return(source);
	}
}
