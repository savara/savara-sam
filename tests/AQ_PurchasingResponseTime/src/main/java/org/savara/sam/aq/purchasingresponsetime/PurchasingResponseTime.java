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
package org.savara.sam.aq.purchasingresponsetime;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.Destination;
import javax.jms.MessageListener;

import org.savara.sam.activity.ActivityAnalysis;
import org.savara.sam.ams.cep.CEPManager;
import org.savara.sam.aq.ActiveQuerySpec;

@MessageDriven(name = "PurchasingResponseTime", messageListenerInterface = MessageListener.class,
               activationConfig =
                     {
                        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/aq/PurchasingResponseTime")
                     })
@TransactionManagement(value= TransactionManagementType.CONTAINER)
@TransactionAttribute(value= TransactionAttributeType.REQUIRED)
public class PurchasingResponseTime extends CEPManager<String,ActivityAnalysis> implements MessageListener {
	
	private static final String ACTIVE_QUERY_NAME = "PurchasingResponseTime";

	@Resource(mappedName = "java:/topic/aq/Notifications")
	Destination _notificationTopic;

	@Resource(mappedName = "java:/queue/aq/PurchasingResponseTime")
	Destination _sourceQueue;

	@Resource(mappedName="java:jboss/infinispan/sam")
	private org.infinispan.manager.CacheContainer _container;

	// TODO: Determine how best to handle this type of active query, that has
	// multiple parent AQs???
	
	public PurchasingResponseTime() {
		super(new ActiveQuerySpec(ACTIVE_QUERY_NAME, ActivityAnalysis.class, ActivityAnalysis.class),
				null);
	}
	
	@PostConstruct
	public void init() {
		super.init(_container, _sourceQueue, _notificationTopic);
	}

	@PreDestroy
	public void close() {
		super.close();
	}
}
