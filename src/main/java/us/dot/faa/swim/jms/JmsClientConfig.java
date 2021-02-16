package us.dot.faa.swim.jms;

import java.util.Hashtable;

public class JmsClientConfig {

	private Hashtable<String, Object> jndiProperties = new Hashtable<String, Object>();

	public JmsClientConfig() {}
	
	public JmsClientConfig(Hashtable<String, Object> jndiProperties) {
		this.jndiProperties = jndiProperties;
	}
	
	public String getInitialContextFactory() {
		return (String)this.jndiProperties.get("java.naming.factory.initial");
	}

	public String getProviderUrl() {
		return (String)this.jndiProperties.get("java.naming.provider.url");
	}

	public String getUsername() {
		return (String)this.jndiProperties.get("java.naming.security.principal");
	}

	public String getPassword() {
		return (String)this.jndiProperties.get("java.naming.security.credentials");
	}
	
	public String getSolaceMessageVpn() {
		return (String)this.jndiProperties.get("Solace_JMS_VPN");
	}
	
	public String getSolaceSslTrustStore() {
		return (String)this.jndiProperties.get("Solace_JMS_SSL_TrustStore");
	}
	
	public String getSolaceJndiConnectionRetries() {
		return (String)this.jndiProperties.get("Solace_JMS_JNDI_ConnectRetries");
	}

	public Hashtable<String, Object> getJndiProperties() {
		return this.jndiProperties;
	}

	public JmsClientConfig setInitialContextFactory(String initialContextFactory) {		
		this.jndiProperties.put("java.naming.factory.initial",initialContextFactory);
		return this;
	}

	public JmsClientConfig setProviderUrl(String providerUrl) {
		this.jndiProperties.put("java.naming.provider.url",providerUrl);
		return this;
	}

	public JmsClientConfig setUsername(String username) {
		this.jndiProperties.put("java.naming.security.principal", username);
		return this;
	}

	public JmsClientConfig setPassword(String password) {
		this.jndiProperties.put("java.naming.security.credentials", password);
		return this;
	}

	public JmsClientConfig setJndiProperties(Hashtable<String, Object> additionalJndiProperties) {
		this.jndiProperties.putAll(additionalJndiProperties);
		return this;
	}
	
	public JmsClientConfig setSolaceMessageVpn(String solaceMessageVpn) {
		this.jndiProperties.put("Solace_JMS_VPN",solaceMessageVpn);
		return this;
	}
	
	public JmsClientConfig setSolaceSslTrustStore(String solaceSslTrustStore) {
		this.jndiProperties.put("Solace_JMS_SSL_TrustStore",solaceSslTrustStore);
		return this;
	}
	
	public JmsClientConfig setSolaceJndiConnectionRetries(int solaceJndiConnectionRetries) {
		this.jndiProperties.put("Solace_JMS_JNDI_ConnectRetries", solaceJndiConnectionRetries);
		return this;
	}
}