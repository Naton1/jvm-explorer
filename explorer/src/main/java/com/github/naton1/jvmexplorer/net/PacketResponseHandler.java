package com.github.naton1.jvmexplorer.net;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
class PacketResponseHandler<T> {

	private static final Object INTERRUPT = new Object();

	private final Runnable onCleanup;
	private final Consumer<Integer> onUpdateCount;
	private final BlockingQueue<Object> linkedBlockingQueue = new LinkedBlockingQueue<>();

	private volatile int receivedItemCount = 0;
	private volatile int totalItemsSent = -1;
	private volatile boolean addedInterrupt = false;

	public synchronized void onPacketReceived(T[] packets) {
		receivedItemCount += packets.length;
		Collections.addAll(linkedBlockingQueue, packets);
		log.debug("Received packet. Total items received: {}", receivedItemCount);
		onUpdateCount.accept(receivedItemCount);
		if (totalItemsSent != -1 && totalItemsSent == receivedItemCount) {
			log.debug("Received all packets.");
			interrupt();
		}
	}

	public synchronized void interrupt() {
		if (addedInterrupt) {
			return;
		}
		addedInterrupt = true;
		log.debug("Ending packet collection, received: {}", receivedItemCount);
		linkedBlockingQueue.add(INTERRUPT);
		onCleanup.run();
	}

	public synchronized void receivedEnd(int totalItemsSent) {
		log.debug("Received total item count: {}", totalItemsSent);
		this.totalItemsSent = totalItemsSent;
		if (this.totalItemsSent == this.receivedItemCount) {
			interrupt();
		}
	}

	public Stream<T> getPacketStream(long timeout, TimeUnit timeUnit) {
		log.trace("Creating stream...");
		final long durationMs = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
		final long endMs = System.currentTimeMillis() + durationMs;
		return Stream.generate(() -> {
			while (true) {
				log.trace("Generating...");
				final long remainingMs = endMs - System.currentTimeMillis();
				try {
					final Object t = linkedBlockingQueue.poll(remainingMs, TimeUnit.MILLISECONDS);
					if (t == null) {
						log.warn("Timed out waiting for packets");
						return INTERRUPT;
					}
					if (t == INTERRUPT) {
						if (!linkedBlockingQueue.isEmpty()) {
							log.debug("Received interrupt but queue is not empty... re-adding interrupt to end, and "
							          + "moving on");
							linkedBlockingQueue.add(INTERRUPT);
							continue;
						}
					}
					return t;
				}
				catch (InterruptedException e) {
					log.warn("Interrupted while waiting", e);
					e.printStackTrace();
					return null;
				}
			}
		}).takeWhile(o -> {
			if (o == INTERRUPT) {
				log.debug("Ending stream, received {}", receivedItemCount);
				return false;
			}
			return true;
		}).map(o -> (T) o);
	}

}
