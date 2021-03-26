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

import com.codahale.metrics.Meter;

public class JmsMessageProcessor {
	int processingThreadCount;
	Thread processorThread;
	ThreadPoolExecutor executor;
	int processingQueueSize = 100;
	int consumptionYieldTimeInSeconds = 1;
	Instant suspendConsumptionUntil = Instant.now();
	Meter consumedMessagesMeter = new Meter();
	Meter processedMessagesMeter = new Meter();
	JmsMessageWorker jmsMessageWorker;
	boolean running = false;

	final CountDownLatch latch = new CountDownLatch(1);

	public JmsMessageProcessor(MessageConsumer consumer, int processingThreadCount, int processingQueueSize,
			JmsMessageWorker worker) throws JMSException {

		final List<MessageConsumer> consumerList = new ArrayList<MessageConsumer>();
		consumerList.add(consumer);
		initialize(consumerList, processingThreadCount, processingQueueSize, worker);
	}

	public JmsMessageProcessor(List<MessageConsumer> consumers, int processingThreadCount, int processingQueueSize,
			JmsMessageWorker worker) throws Exception {

		initialize(consumers, processingThreadCount, processingQueueSize, worker);
	}

	private void initialize(List<MessageConsumer> consumers, int processingThreadCount, int processingQueueSize,
			JmsMessageWorker worker) throws JMSException {

		this.jmsMessageWorker = worker;
		this.processingQueueSize = processingQueueSize;
		this.processingThreadCount = processingThreadCount;

		processorThread = new Thread() {

			@Override
			public void run() {
				this.setName("JmsMessageProcessor-Thread");
				try {

					while (running) {
						consumers.parallelStream().forEach(c -> {
							consumeMessage(c);
						});
					}

					latch.await();
				} catch (InterruptedException e) {
					executor.shutdown();
					try {
						executor.awaitTermination(10, TimeUnit.SECONDS);
					} catch (InterruptedException e1) {
						throw new RuntimeException(e1);
					}
					throw new RuntimeException(e);
				}
			}
		};
	}

	private void consumeMessage(MessageConsumer consumer) {
		try {
			Message jmsMessage = consumer.receive(1000);
			if (jmsMessage != null) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						if(jmsMessageWorker.processesMessage(jmsMessage)){
							processedMessagesMeter.mark();
							try {
								jmsMessage.acknowledge();
							} catch (JMSException e) {
								throw new RuntimeException(e);
							}
						}
					}
				});
			}
			consumedMessagesMeter.mark();
		} catch (JMSException e) {
			throw new RuntimeException(e);
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

	public void start() {

		this.running = true;
		executor = new ThreadPoolExecutor(processingThreadCount, processingThreadCount, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(processingQueueSize), new RunInCallingThreadOrThrow());
		processorThread.start();
	}

	public void stop() throws Exception {
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
				executor.setMaximumPoolSize(executor.getMaximumPoolSize() + 1);
			} finally

			{
				lock.unlock();
			}
		}
	}

}
