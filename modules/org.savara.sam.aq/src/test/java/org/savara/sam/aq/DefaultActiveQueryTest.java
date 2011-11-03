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

import static org.junit.Assert.*;

import org.junit.Test;

public class DefaultActiveQueryTest {

	@Test
	public void testSingleActiveQueryWithPredicate() {
		String obj1="Hello";
		String obj2="World";
		String obj3="Message";
		
		TestPredicate<String> pred=new TestPredicate<String>(new String[] {obj2});
	
		ActiveQuery<String> aq=new DefaultActiveQuery<String>("aq1", pred);
		
		if (aq.size() != 0) {
			fail("Query should be empty");
		}
		
		if (aq.add(obj1)) {
			fail("Object 1 should not have been added");
		}
		
		if (!aq.add(obj2)) {
			fail("Object 2 should have been added");
		}
		
		if (aq.add(obj3)) {
			fail("Object 3 should not have been added");
		}
		
		if (aq.size() != 1) {
			fail("Should be one element in results");
		}
	}

}
