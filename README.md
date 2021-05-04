# FAA SWIM JMS Client

Provides a very basic JMS Client that simplifies connecting and consuming from FAA SWIM.


## Installing

  1. Clone this repository
  2. Run mvn clean install
  3. Add dependency to applicable project pom

```xml
	<dependency>
		<groupId>us.dot.faa.swim.jms</groupId>
		<artifactId>jms-client</artifactId>
		<version>1.0</version>
	</dependency>
```

## Usage

Import SWIM JMS Client 

```java
	import us.dot.faa.swim.jms.JmsClient;
	import us.dot.faa.swim.jms.JmsClientConfig;
```

Create new JMS Client Config

```java
	JmsClientConfig jmsClientConfig = new JmsClientConfig()
                .setInitialContextFactory("Initial_Context_Factory_Class")
                .setProviderUrl("Provider_Url")
                .setUsername("Username")
                .setPassword("Password")
                // Solace applicable properties, if required
                .setSolaceMessageVpn("Solace_Message_Vpn")
                .setSolaceSslTrustStore("path/to/ssl/trust")
                .setSolaceJndiConnectionRetries(-1);
```

Create new JMS Client form Config

```java
	JmsClient jmsClient = new JmsClient(jmsClientConfig);
```

Connect JMS Client

```java
	jmsClient.connect("Connection_Factory_Name");
```

Create one or more Message Consumers

```java
	MessageConsumer consumer = jmsClient.createConsumer("JMS_DESTINATION");
	
```

Consuming using an Asynchronous Message Consumer
	

```java
	consumer.setMessageListener(this);
```

```java
	@Override
	public void onMessage(Message jmsMessage) {
		//assuming message is of type text message
		System.out.println(((TextMessage)jmsMessage).getText());
	}
```

Consuming using an Synchronous Message Consumer

```java
	Message jmsMessage = consumer.recieve();
	
	//assuming message is of type text message
	System.out.println(((TextMessage)jmsMessage).getText());
```

## JMS Message Processor

JMS Message Processor provides a high performance processor that takes one or more MessageConsumers, consumes messages, and queues for single threaded or parallel processing.  If consumption rate is faster than processing rate, resulting in processing queue reaching max capacity, consumption will be stopped until processing threads are available. This allows for cases of either inbound message bursts or a delay in processing (e.g. writing to a database). If consumption yielding happens often, increase the parallel processor count and/or the processing queue size accordingly. If consumption rate is insufficient, increase the concurrent consumer count accordingly. Processing logic is controlled via the MessageListener interface onMessage method.

```java

	String jmsDestination = "JMS_DESTINATION";
	int concurrentConsumerCount = 4;
	int parallelProcessorCount = 2;
	int processingQueueSize = 10000;
	ArrayList<MessageConsumer> consumers = new ArrayList<MessageConsumer>();
	
	for(int i=0; i < concurrentConsumerCount; i++) {
	MessageConsumer consumer = jmsClient.createConsumer(jmsDestination);
		consumers.add(consumer);
	}

	JmsMessageProcessor processor = new JmsMessageProcessor(consumers, parallelProcessorCount, processingQueueSize, this);
	
	processor.start();
	
	//do other stuff//
	
	//stop processor before exiting application
	processor.stop();
	
```

```java

	public void onMessage(Message jmsMessage) {
		try {					
			//assuming message is of type text message
			System.out.println(((TextMessage)jmsMessage).getText());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
```
