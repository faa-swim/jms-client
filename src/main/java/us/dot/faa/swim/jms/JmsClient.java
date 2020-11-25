package us.dot.faa.swim.jms;

import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class JmsClient {

	private InitialContext initialcontext;
	private ConnectionFactory connectionFactory;
	private Connection connection;

	public JmsClient(final Hashtable<String, Object> jndiProperties) throws NamingException {
		try {
			this.initialcontext = new InitialContext(jndiProperties);
		} catch (final NamingException e) {
			throw e;
		}
	}

	public JmsClient(final String initialContextFactoryName, final String providerUrl, final String username,
			final String password, final String solaceMessageVpn, final String scdsTrustStore) throws NamingException {
		try {
			Hashtable<String, Object> jndiProperties = new Hashtable<>();
			jndiProperties.put("java.naming.factory.initial", initialContextFactoryName);
			jndiProperties.put("java.naming.provider.url", providerUrl);
			jndiProperties.put("java.naming.security.principal", username);
			jndiProperties.put("java.naming.security.credentials", password);
			jndiProperties.put("Solace_JMS_VPN", solaceMessageVpn);
			jndiProperties.put("Solace_JMS_SSL_TrustStore", scdsTrustStore);
			jndiProperties.put("Solace_JMS_JNDI_ConnectRetries", -1);

			this.initialcontext = new InitialContext(jndiProperties);
		} catch (final NamingException e) {
			throw e;
		}
	}

	public void connect(final String connectionFactoryName, ExceptionListener exceptionListener) throws NamingException, JMSException {
		this.connectionFactory = (ConnectionFactory) this.initialcontext.lookup(connectionFactoryName);
		this.connection = this.connectionFactory.createConnection();
		this.connection.setExceptionListener(exceptionListener);
		this.connection.start();
	}

	public void close() throws NamingException {
		this.initialcontext.close();
	}

	public MessageConsumer createConsumer(String destinationName, MessageListener messageListener, int ackMode)
			throws Exception {

		Destination destination = (Destination) this.initialcontext.lookup(destinationName);
		Session session = this.connection.createSession(false, ackMode);
		MessageConsumer messageConsumer = session.createConsumer(destination);
		messageConsumer.setMessageListener(messageListener);
		return messageConsumer;
	}
}