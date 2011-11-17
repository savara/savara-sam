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
package org.savara.sam.ams.conversations;

import static org.junit.Assert.*;

import org.junit.Test;
import org.savara.monitor.ConversationId;
import org.savara.monitor.Message;
import org.savara.sam.ams.conversations.XPathConversationResolver;

public class XPathConversationResolverTest {

	@Test
	public void test() {
		XPathConversationResolver resolver=new XPathConversationResolver();
		
		resolver.addMessageTypeIDLocator("TestMessage", "//@id");
		
		String id="abc";
		
		Message mesg=new Message();
		mesg.getTypes().add("TestMessage");
		mesg.getValues().add("<message id=\""+id+"\" />");
		
		ConversationId cid=resolver.getConversationId(null, mesg);
		
		if (cid == null) {
			fail("Conversation id is null");
		}
		
		if (!cid.getId().equals(id)) {
			fail("Conversation id incorrect, expecting '"+id+"' but got: "+cid.getId());
		}
	}

}
