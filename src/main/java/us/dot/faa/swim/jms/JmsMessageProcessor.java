package us.dot.faa.swim.jms;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

import com.codahale.metrics.Meter;

public class JmsMessageProcessor implements MessageListener {
	int processingThreadCount;
	ThreadPoolExecutor executor;
	int processingQueueSize = 100;
	int consumptionYieldTimeInSeconds = 1;
	Instant suspendConsumptionUntil = Instant.now();
	Meter consumedMessagesMeter = new Meter();
	Meter processedMessagesMeter = new Meter();
	List<MessageConsumer> consumers;
	MessageListener messageListener;
	boolean running = false;

	final CountDownLatch latch = new CountDownLatch(1);

	public JmsMessageProcessor(MessageConsumer consumer, int processingThreadCount, int processingQueueSize,
			MessageListener messageListener) throws JMSException {

		final List<MessageConsumer> consumerList = new ArrayList<MessageConsumer>();
		consumerList.add(consumer);
		initialize(consumerList, processingThreadCount, processingQueueSize, messageListener);
	}

	public JmsMessageProcessor(List<MessageConsumer> consumers, int processingThreadCount, int processingQueueSize,
			MessageListener messageListener) throws Exception {

		initialize(consumers, processingThreadCount, processingQueueSize, messageListener);
	}

	private void initialize(List<MessageConsumer> consumers, int processingThreadCount, int processingQueueSize,
			MessageListener messageListener) throws JMSException {

		this.messageListener = messageListener;
		this.processingQueueSize = processingQueueSize;
		this.processingThreadCount = processingThreadCount;
		this.consumers = consumers;
	}

	public void onMessage(Message message) {
		if (message != null) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					messageListener.onMessage(message);
					processedMessagesMeter.mark();
				}
			});
			consumedMessagesMeter.mark();
		}
	}

	public int getConsumptionYeildTimeInSeconds() {
		return this.consumptionYieldTimeInSeconds;
	}

	public JmsMessageProcessor setConsumptionYeildTimeInSeconds(int yieldTimeInSeconds) {
		this.consumptionYieldTimeInSeconds = yieldTimeInSeconds;
		return this;
	}

	public long getTotalConsumedMessages() {
		return this.consumedMessagesMeter.getCount();
	}

	public double getConsumedMessagePerSecond() {
		return this.consumedMessagesMeter.getOneMinuteRate();
	}

	public long getTotalProcessedMessages() {
		return this.processedMessagesMeter.getCount();
	}

	public double getProcessedMessagePerSecond() {
		return this.processedMessagesMeter.getOneMinuteRate();
	}

	public int getQueuedMessageCount() {
		return this.executor.getQueue().size();
	}

	public void start() throws JMSException {

		this.running = true;
		executor = new ThreadPoolExecutor(processingThreadCount, processingThreadCount, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(processingQueueSize), new RunInCallingThreadOrThrow());

		for (MessageConsumer messageConsumer : this.consumers) {
			try {
				messageConsumer.setMessageListener(this);
			} catch (Exception e) {
				throw e;
			}
		}
	}

	public void stop() throws Exception {

		for (MessageConsumer messageConsumer : this.consumers) {
			try {
				messageConsumer.setMessageListener(null);
			} catch (Exception e) {
				throw e;
			}
		}

		this.running = false;
		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.SECONDS);
	}

	class RunInCallingThreadOrThrow implements RejectedExecutionHandler {

		private final Lock lock = new ReentrantLock();

		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			lock.lock();
			try {
				if (!executor.isShutdown()) {
					r.run();
				} else {
					throw new RuntimeException(new Exception("Executor Shutdown"));
				}
			} finally

			{
				lock.unlock();
			}
		}
	}

}
