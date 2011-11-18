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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.savara.monitor.ConversationId;
import org.savara.monitor.ConversationResolver;
import org.savara.monitor.Message;
import org.scribble.protocol.monitor.model.Description;
import org.xml.sax.InputSource;

/**
 * This class provides an XPath based implementation of the ConversationResolver
 * interface.
 *
 */
public class XPathConversationResolver implements ConversationResolver {
	
	private static Logger LOG=Logger.getLogger(XPathConversationResolver.class.getName());
	
	private java.util.Map<String,XPathExpression> _messageTypeToXPath=
							new java.util.HashMap<String,XPathExpression>();
	
	private javax.xml.parsers.DocumentBuilder _builder=null;
	
	public XPathConversationResolver() {
		try {
			_builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch(Exception e) {
			e.printStackTrace();
		}	
	}
	
	public void addMessageTypeIDLocator(String mesgType, String xpathExpr) {
		
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression xpe=xpath.compile(xpathExpr);
			
			_messageTypeToXPath.put(mesgType, xpe);
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to add XPath expression '"+
						xpathExpr+"' for message type '"+mesgType+"'", e);
		}
	}

	public ConversationId getConversationId(Description desc, Message mesg) {
		ConversationId ret=null;
		
		// TODO: Need to handle multiple conversation ids eventually
		
		for (int i=0; ret == null && i < mesg.getTypes().size(); i++) {
			XPathExpression xpathExpr=_messageTypeToXPath.get(mesg.getTypes().get(i));
			
			if (xpathExpr != null) {				
				String value=mesg.getValues().get(i);
				
				try {
					InputSource is=new InputSource(new java.io.ByteArrayInputStream(value.getBytes()));
					
					org.w3c.dom.Document doc=_builder.parse(is);
					
					String result=xpathExpr.evaluate(doc);
					
					if (result != null) {
						ret = new ConversationId(result);
					}
				} catch(Exception e) {
					LOG.log(Level.SEVERE, "Failed to evaluate xpath expression '"+
								xpathExpr+"' against value '"+value+"'", e);
				}
			}
		}

		if (LOG.isLoggable(Level.FINEST)) {
			LOG.finest("Conversation id for '"+mesg+"' = "+ret);
		}
		
		return (ret);
	}
}
