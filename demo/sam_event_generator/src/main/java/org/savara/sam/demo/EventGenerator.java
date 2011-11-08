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
package org.savara.sam.demo;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.savara.sam.activity.ActivityModel.*;
import org.savara.sam.activity.ServiceModel.*;
import org.savara.sam.collector.ActivityCollector;
import org.savara.sam.internal.collector.JMSActivityCollectorImpl;

public class EventGenerator {
	
	private static String[] _principals=new String[] { "gary", "jeff", "steve", "viv", "joe", "jane", "john", "lisa" };
	
	private ActivityCollector _collector=null;
	
	private java.util.Random _random=new java.util.Random(System.currentTimeMillis());
	
	private java.util.concurrent.ThreadPoolExecutor _executor=
			new java.util.concurrent.ThreadPoolExecutor(200, 200, 10,
					TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10000));

	public static void main(String[] args) throws Exception {
		EventGenerator egen=new EventGenerator();
		
		egen.run();
		
		System.out.println("Exiting event generator.");
	}
	
	public void run() {
		
		_collector = new JMSActivityCollectorImpl();
		((JMSActivityCollectorImpl)_collector).init();
		
		while (displayOptions() != 0);
		
		((JMSActivityCollectorImpl)_collector).close();
		
		_executor.shutdown();
	}
	
	protected int getInt(int def) {
		int ret=0;
		
		try {
			byte[] b=new byte[1024];
			System.in.read(b);
			
			String str=new String(b).trim();
			
			if (str.trim().length() == 0) {
				ret = def;
			} else {
				ret = Integer.parseInt(str);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return(ret);
	}
	
	protected int displayOptions() {
		int ret=0;
		
		System.out.println("\r\n-----------------------------\r\n");
		System.out.println("0) Exit");
		System.out.println("1) Send successful purchase");
		System.out.println("2) Send unsuccessful purchase");
		System.out.println("3) Random successful or unsuccessful purchase");
		
		System.out.println("Enter option:");
		
		ret = getInt(0);
		
		if (ret != 0) {
			System.out.println("Enter number of iterations (default 1):");
			int repeat=getInt(1);
			
			for (int i=0; i < repeat; i++) {
				switch(ret) {
				case 1:
					_executor.execute(new Runnable() {
						public void run() {						
							sendSuccessfulPurchase("id"+_random.nextLong(), getPrincipal(), getDelay());
						}
					});
					break;
				case 2:
					_executor.execute(new Runnable() {
						public void run() {						
							sendUnsuccessfulPurchase("id"+_random.nextLong(), getPrincipal(), getDelay());
						}
					});
					break;
				case 3:
					_executor.execute(new Runnable() {
						public void run() {
							int val=_random.nextInt();
							if ((val % 2) == 0) {
								sendSuccessfulPurchase("id"+_random.nextLong(), getPrincipal(), getDelay());
							} else {
								sendUnsuccessfulPurchase("id"+_random.nextLong(), getPrincipal(), getDelay());
							}
						}
					});
					break;
				}
			}
		}
		
		return(ret);
	}
	
	protected String getMessageContent(String id, String name) {
		String ret=null;
		String path="messages/"+name+".xml";
		
		java.io.InputStream is=ClassLoader.getSystemResourceAsStream(path);
		
		if (is == null) {
			is = EventGenerator.class.getResourceAsStream("/"+path);
		}
		
		if (is != null) {
			try {
				byte[] b=new byte[is.available()];
				is.read(b);
				is.close();
				
				ret = new String(b);
				
				ret = ret.replaceAll("%CID%", id);
				
			} catch(Exception e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("Failed to get message content for '"+name+"'");
		}
		
		return(ret);
	}
	
	protected void sendSuccessfulPurchase(String id, String principal, long delay) {
		String buyCorrelation="buy"+id;
		String checkCreditCorrelation="checkCredit"+id;
		String deliverCorrelation="deliver"+id;

		// Buy Request
		ComponentId cid=ComponentId.newBuilder().setComponentType("Store").setInstanceId(id).build();

		Message m=Message.newBuilder().setMessageType("{http://www.jboss.org/examples/store}BuyRequest").
									setContent(getMessageContent(id, "BuyRequest")).build();
		
		ServiceInvocation si=ServiceInvocation.newBuilder().setServiceType("{http://www.jboss.org/examples/store}Store").setCorrelation(buyCorrelation).
					setOperation("buy").setInvocationType(ServiceInvocation.InvocationType.REQUEST).
					setDirection(ServiceInvocation.Direction.INBOUND).addMessage(m).
					build();

		Activity activity=Activity.newBuilder().setId(cid).setTimestamp(System.currentTimeMillis()).
					setServiceInvocation(si).setPrincipal(principal).build();

		_collector.process(activity);
		
		// Check Credit Request
		cid = ComponentId.newBuilder().setComponentType("Store").setInstanceId(id).build();

		m = Message.newBuilder().setMessageType("{http://www.jboss.org/examples/creditAgency}CreditCheckRequest").
				setContent(getMessageContent(id, "CreditCheckRequest")).build();

		si = ServiceInvocation.newBuilder().setServiceType("{http://www.jboss.org/examples/creditAgency}CreditAgency").setCorrelation(checkCreditCorrelation).
				setOperation("checkCredit").setInvocationType(ServiceInvocation.InvocationType.REQUEST).
				setDirection(ServiceInvocation.Direction.OUTBOUND).addMessage(m).
				build();
		
		activity = Activity.newBuilder().setId(cid).setTimestamp(System.currentTimeMillis()).
				setServiceInvocation(si).setPrincipal(principal).build();

		_collector.process(activity);

		if (delay > 0) {
			try {
				Thread.sleep(delay/2);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		// Check Credit Response
		cid = ComponentId.newBuilder().setComponentType("Store").setInstanceId(id).build();

		m = Message.newBuilder().setMessageType("{http://www.jboss.org/examples/creditAgency}CreditRating").
				setContent(getMessageContent(id, "CreditRating1")).build();

		si = ServiceInvocation.newBuilder().setServiceType("{http://www.jboss.org/examples/creditAgency}CreditAgency").setCorrelation(checkCreditCorrelation).
				setOperation("checkCredit").setInvocationType(ServiceInvocation.InvocationType.RESPONSE).
				setDirection(ServiceInvocation.Direction.INBOUND).addMessage(m).
				build();
		
		activity = Activity.newBuilder().setId(cid).setTimestamp(System.currentTimeMillis()).
				setServiceInvocation(si).setPrincipal(principal).build();

		_collector.process(activity);

		// Logistics Deliver Request
		cid = ComponentId.newBuilder().setComponentType("Store").setInstanceId(id).build();

		m = Message.newBuilder().setMessageType("{http://www.jboss.org/examples/logistics}DeliveryRequest").
				setContent(getMessageContent(id, "DeliveryRequest")).build();

		si = ServiceInvocation.newBuilder().setServiceType("{http://www.jboss.org/examples/logistics}Logistics").setCorrelation(deliverCorrelation).
				setOperation("deliver").setInvocationType(ServiceInvocation.InvocationType.REQUEST).
				setDirection(ServiceInvocation.Direction.OUTBOUND).addMessage(m).
				build();
		
		activity = Activity.newBuilder().setId(cid).setTimestamp(System.currentTimeMillis()).
				setServiceInvocation(si).setPrincipal(principal).build();

		_collector.process(activity);

		if (delay > 0) {
			try {
				Thread.sleep(delay/2);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		// Logistics Deliver Response
		cid = ComponentId.newBuilder().setComponentType("Store").setInstanceId(id).build();

		m = Message.newBuilder().setMessageType("{http://www.jboss.org/examples/logistics}DeliveryConfirmed").
				setContent(getMessageContent(id, "DeliveryConfirmed")).build();

		si = ServiceInvocation.newBuilder().setServiceType("{http://www.jboss.org/examples/logistics}Logistics").setCorrelation(deliverCorrelation).
				setOperation("deliver").setInvocationType(ServiceInvocation.InvocationType.RESPONSE).
				setDirection(ServiceInvocation.Direction.INBOUND).addMessage(m).
				build();
		
		activity = Activity.newBuilder().setId(cid).setTimestamp(System.currentTimeMillis()).
				setServiceInvocation(si).setPrincipal(principal).build();

		_collector.process(activity);

		// Buy Response
		cid = ComponentId.newBuilder().setComponentType("Store").setInstanceId(id).build();

		m = Message.newBuilder().setMessageType("{http://www.jboss.org/examples/store}BuyConfirmed").
				setContent(getMessageContent(id, "BuyConfirmed")).build();

		si = ServiceInvocation.newBuilder().setServiceType("{http://www.jboss.org/examples/store}Store").setCorrelation(buyCorrelation).
				setOperation("buy").setInvocationType(ServiceInvocation.InvocationType.RESPONSE).
				setDirection(ServiceInvocation.Direction.OUTBOUND).addMessage(m).
				build();

		activity = Activity.newBuilder().setId(cid).setTimestamp(System.currentTimeMillis()).
				setServiceInvocation(si).setPrincipal(principal).build();

		_collector.process(activity);
	}
	
	protected void sendUnsuccessfulPurchase(String id, String principal, long delay) {
		String correlation="buy"+id;

		ComponentId cid=ComponentId.newBuilder().setComponentType("Store").setInstanceId(id).build();

		// Request
		ServiceInvocation si=ServiceInvocation.newBuilder().setServiceType("Store").setCorrelation(correlation).
					setOperation("buy").setInvocationType(ServiceInvocation.InvocationType.REQUEST).build();

		Activity activity=Activity.newBuilder().setId(cid).setTimestamp(System.currentTimeMillis()).
					setServiceInvocation(si).setPrincipal(principal).build();

		_collector.process(activity);
		
		if (delay > 0) {
			try {
				Thread.sleep(delay);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		cid = ComponentId.newBuilder().setComponentType("Store").setInstanceId(id).build();

		// Fault response
		si = ServiceInvocation.newBuilder().setServiceType("Store").setCorrelation(correlation).
				setOperation("buy").setInvocationType(ServiceInvocation.InvocationType.RESPONSE).
				setFault("BuyFailed").build();

		activity = Activity.newBuilder().setId(cid).setTimestamp(System.currentTimeMillis()).
				setServiceInvocation(si).setPrincipal(principal).build();

		_collector.process(activity);
	}
	
	protected String getPrincipal() {
		int pos=(int)(Math.random() * _principals.length);
		return(_principals[pos]);
	}
	
	protected long getDelay() {
		int val=(int)(Math.random() * 5000);
		return(5000 + val);
	}
}
