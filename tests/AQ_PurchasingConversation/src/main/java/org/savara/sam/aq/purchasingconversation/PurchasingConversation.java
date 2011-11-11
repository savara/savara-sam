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
package org.savara.sam.aq.purchasingconversation;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageListener;

import org.savara.sam.conversation.ConversationManager;

@MessageDriven(name = "PurchasingConversation", messageListenerInterface = MessageListener.class,
               activationConfig =
                     {
                        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/aq/PurchasingConversation")
                     })
@TransactionManagement(value= TransactionManagementType.CONTAINER)
@TransactionAttribute(value= TransactionAttributeType.REQUIRED)
public class PurchasingConversation extends ConversationManager implements MessageListener {
	
	private static final String ACTIVE_QUERY_NAME = "PurchasingConversation";
	
	private static final String MODEL="PurchaseGoods.cdm";

	@Resource(mappedName = "java:/JmsXA")
	ConnectionFactory _connectionFactory;
	
	@Resource(mappedName = "java:/queue/aq/PurchasingConversation")
	Destination _sourceQueue;
	
	@Resource(mappedName = "java:/topic/aq/Notifications")
	Destination _notificationTopic;

	@Resource(mappedName="java:jboss/infinispan/sam")
	private org.infinispan.manager.CacheContainer _container;
	
	public PurchasingConversation() {
		super(ACTIVE_QUERY_NAME);
	}
	
	@PostConstruct
	public void init() {
		super.init(MODEL, _connectionFactory, _container, _sourceQueue, _notificationTopic);
		
		getResolver().addMessageTypeIDLocator("{http://www.jboss.org/examples/store}BuyRequest", "//@id");
		getResolver().addMessageTypeIDLocator("{http://www.jboss.org/examples/store}BuyConfirmed", "//@id");
		getResolver().addMessageTypeIDLocator("{http://www.jboss.org/examples/store}BuyFailed", "//@id");
		getResolver().addMessageTypeIDLocator("{http://www.jboss.org/examples/store}AccountNotFound", "//@id");
		getResolver().addMessageTypeIDLocator("{http://www.jboss.org/examples/creditAgency}CreditCheckRequest", "//@id");
		getResolver().addMessageTypeIDLocator("{http://www.jboss.org/examples/creditAgency}CreditRating", "//@id");
		getResolver().addMessageTypeIDLocator("{http://www.jboss.org/examples/creditAgency}CustomerUnknown", "//@id");
		getResolver().addMessageTypeIDLocator("{http://www.jboss.org/examples/logistics}DeliveryRequest", "//@id");
		getResolver().addMessageTypeIDLocator("{http://www.jboss.org/examples/logistics}DeliveryConfirmed", "//@id");
	}

	@PreDestroy
	public void close() {
		super.close();
	}
}
