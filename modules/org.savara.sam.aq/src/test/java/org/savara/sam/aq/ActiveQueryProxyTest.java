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

public class ActiveQueryProxyTest {

	@Test
	public void testSingleActiveQueryNotifications() {
		String obj1="Hello";
		String obj2="World";
		String obj3="Message";
		
		TestPredicate<String> pred=new TestPredicate<String>(new String[] {obj1, obj2});
	
		ActiveQuery<String> aq=new ActiveQueryProxy<String>("aq1", new DefaultActiveQuery<String>("aq1", pred));
		
		TestActiveListener l=new TestActiveListener();
		aq.addActiveListener(l);
		
		if (aq.size() != 0) {
			fail("Query should be empty");
		}
		
		if (!aq.add(obj1)) {
			fail("Object 1 should have been added");
		}
		
		if (!aq.add(obj2)) {
			fail("Object 2 should have been added");
		}
		
		if (aq.add(obj3)) {
			fail("Object 3 should not have been added");
		}
		
		if (l.getAdded().size() != 2) {
			fail("Listener should have 2 additions: "+l.getAdded().size());
		}
		
		if (aq.remove(obj3)) {
			fail("Object 3 should not have been removed");
		}
		
		if (l.getRemoved().size() != 0) {
			fail("Listener should have 0 removals: "+l.getRemoved().size());
		}
		
		if (!aq.remove(obj2)) {
			fail("Object 2 should have been removed");
		}
		
		if (l.getRemoved().size() != 1) {
			fail("Listener should have 1 removals: "+l.getRemoved().size());
		}
		
		if (aq.size() != 1) {
			fail("Should be one element in results");
		}
	}

	@Test
	public void testActiveQueryAddPropagation() {
		String obj1="Hello";
		String obj2="World";
		String obj3="Message";
		
		TestPredicate<String> pred1=new TestPredicate<String>(new String[] {obj1, obj2});
	
		ActiveQuery<String> aq1=new ActiveQueryProxy<String>("aq1", new DefaultActiveQuery<String>("aq1", pred1));
		
		TestPredicate<String> pred2=new TestPredicate<String>(new String[] {obj2});

		ActiveQuery<String> aq2=new ActiveQueryProxy<String>("aq2", new DefaultActiveQuery<String>("aq2", pred2));
		aq1.addActiveListener(aq2.getChangeHandler());
		
		if (aq1.size() != 0) {
			fail("Query1 should be empty");
		}
		
		if (!aq1.add(obj1)) {
			fail("Object 1 should have been added");
		}
		
		if (!aq1.add(obj2)) {
			fail("Object 2 should have been added");
		}
		
		if (aq1.add(obj3)) {
			fail("Object 3 should not have been added");
		}
		
		if (aq1.size() != 2) {
			fail("Should be two element in aq1 results");
		}
		
		if (aq2.size() != 1) {
			fail("Should be one element in aq2 results");
		}
		
		if (aq2.getContents().get(0) != obj2) {
			fail("AQ2 result incorrect");
		}
	}

}
