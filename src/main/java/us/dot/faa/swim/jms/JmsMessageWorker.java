package us.dot.faa.swim.jms;

import javax.jms.Message;

public interface JmsMessageWorker {
	
	public void processesMessage(Message message);	
}
