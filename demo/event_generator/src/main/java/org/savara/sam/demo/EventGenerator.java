package org.savara.sam.demo;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.savara.sam.activity.ActivityModel.*;
import org.savara.sam.activity.ServiceModel.*;

public class EventGenerator {

   public static void main(String[] args) throws Exception {
      Connection connection = null;
      try {
         //Step 2. Perfom a lookup on the queue
         Queue queue = HornetQJMSClient.createQueue("ActivityMonitorServer");

         //Step 3. Perform a lookup on the Connection Factory
         TransportConfiguration transportConfiguration = new TransportConfiguration(NettyConnectorFactory.class.getName());

         ConnectionFactory cf = (ConnectionFactory) HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);

         //Step 4.Create a JMS Connection
         connection = cf.createConnection();

         //Step 5. Create a JMS Session
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

         //Step 6. Create a JMS Message Producer
         MessageProducer producer = session.createProducer(queue);

         //Step 7. Create a Text Message

	 int i=1;
	 String correlation="si"+System.currentTimeMillis();

	 ComponentId cid=ComponentId.newBuilder().setComponentType("Broker").setInstanceId(""+i).build();

	// Request
	ServiceInvocation si=ServiceInvocation.newBuilder().setServiceType("Broker").setCorrelation(correlation).
			setOperation("buy").setInvocationType(ServiceInvocation.InvocationType.REQUEST).build();

	 Activity activity=Activity.newBuilder().setId(cid).setTimestamp(System.currentTimeMillis()).
				setServiceInvocation(si).setPrincipal("gbrown").build();

	 byte[] mesg=activity.toByteArray();

	 javax.jms.BytesMessage message = session.createBytesMessage();

	 message.writeInt(mesg.length);
	 message.writeBytes(mesg);
	 message.writeInt(0);

	 producer.send(message);

	 try {
		Thread.sleep(5000);
	 } catch(Exception e) {
		e.printStackTrace();
	 }

	 cid=ComponentId.newBuilder().setComponentType("Broker").setInstanceId(""+i).build();

	// Normal response
	 si=ServiceInvocation.newBuilder().setServiceType("Broker").setCorrelation(correlation).
			setOperation("buy").setInvocationType(ServiceInvocation.InvocationType.RESPONSE).build();

	 activity=Activity.newBuilder().setId(cid).setTimestamp(System.currentTimeMillis()).
				setServiceInvocation(si).setPrincipal("gbrown").build();

	 mesg=activity.toByteArray();

	 message = session.createBytesMessage();

	 message.writeInt(mesg.length);
	 message.writeBytes(mesg);
	 message.writeInt(0);

	 producer.send(message);
      } finally {
         if(connection != null) {
            connection.close();
         }
      }
   }
}
