package us.dot.faa.swim.jms;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class JmsClient {

	private JmsClientConfig config;
	private InitialContext initialcontext;
	private ConnectionFactory connectionFactory;
	private Connection connection;

	public JmsClient(JmsClientConfig config) throws NamingException {
		this.config = config;
		this.initialcontext = new InitialContext(config.getJndiProperties());
	}

	public JmsClientConfig getConfig() {
		return this.config;
	}
	
	public JmsClientConfig setConfig(JmsClientConfig config) {
		this.config = config;
		return this.config;
	}
	
	public void reInitialize() throws NamingException
	{
		this.initialcontext.close();
		this.initialcontext = new InitialContext(config.getJndiProperties());
	}

	public void connect(final String connectionFactoryName) throws NamingException, JMSException {
		this.connectionFactory = (ConnectionFactory) this.initialcontext.lookup(connectionFactoryName);
		this.connection = this.connectionFactory.createConnection();
		this.connection.start();
	}

	public void connect(final String connectionFactoryName, ExceptionListener exceptionListener)
			throws NamingException, JMSException {
		this.connectionFactory = (ConnectionFactory) this.initialcontext.lookup(connectionFactoryName);
		this.connection = this.connectionFactory.createConnection();
		this.connection.setExceptionListener(exceptionListener);
		this.connection.start();
	}
	
	public void close() throws NamingException {
		this.initialcontext.close();
	}

	public MessageConsumer createConsumer(String destinationName) throws Exception {
		return createConsumer(destinationName, Session.AUTO_ACKNOWLEDGE);
	}

	
	public MessageConsumer createConsumer(String destinationName, int ackMode) throws Exception {

		Destination destination = (Destination) this.initialcontext.lookup(destinationName);
		Session session = this.connection.createSession(false, ackMode);
		MessageConsumer messageConsumer = session.createConsumer(destination);
		return messageConsumer;
	}	

}