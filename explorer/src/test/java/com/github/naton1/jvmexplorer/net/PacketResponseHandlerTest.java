package com.github.naton1.jvmexplorer.net;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class PacketResponseHandlerTest {

	@Test
	void givenValidPackets_whenStreamPacketsAndEndReceivedAfterAllPackets_thenStreamProcessedSuccessfully()
			throws InterruptedException {
		final AtomicBoolean cleanupCalled = new AtomicBoolean(false);
		final AtomicInteger updateCount = new AtomicInteger(0);
		final PacketResponseHandler<String> packetResponseHandler =
				new PacketResponseHandler<>(() -> cleanupCalled.set(
				true), updateCount::set);

		final Thread packetProcessor = new Thread(() -> {
			packetResponseHandler.onPacketReceived(createPackets(1000));
			packetResponseHandler.onPacketReceived(createPackets(1000));
			packetResponseHandler.receivedEnd(2000);
		});
		packetProcessor.start();

		final List<String> results = packetResponseHandler.getPacketStream(30, TimeUnit.SECONDS)
		                                                  .collect(Collectors.toList());

		packetProcessor.join();

		Assertions.assertTrue(cleanupCalled.get());
		Assertions.assertEquals(2000, updateCount.get());
		Assertions.assertEquals(2000, results.size());
	}

	private String[] createPackets(int len) {
		return IntStream.range(0, len).mapToObj(i -> UUID.randomUUID().toString()).toArray(String[]::new);
	}

	@Test
	void givenValidPackets_whenStreamPacketsAndEndReceivedBeforeAllPackets_thenStreamProcessedSuccessfully()
			throws InterruptedException {
		final AtomicBoolean cleanupCalled = new AtomicBoolean(false);
		final AtomicInteger updateCount = new AtomicInteger(0);
		final PacketResponseHandler<String> packetResponseHandler =
				new PacketResponseHandler<>(() -> cleanupCalled.set(
				true), updateCount::set);

		final Thread packetProcessor = new Thread(() -> {
			packetResponseHandler.onPacketReceived(createPackets(1000));
			packetResponseHandler.receivedEnd(2000);
			packetResponseHandler.onPacketReceived(createPackets(1000));
		});
		packetProcessor.start();

		final List<String> results = packetResponseHandler.getPacketStream(30, TimeUnit.SECONDS)
		                                                  .collect(Collectors.toList());

		packetProcessor.join();

		Assertions.assertTrue(cleanupCalled.get());
		Assertions.assertEquals(2000, updateCount.get());
		Assertions.assertEquals(2000, results.size());
	}

	@Test
	void givenInterruptMidPacketStream_whenStreamPackets_thenStreamInterruptedSuccessfully()
			throws InterruptedException {
		final AtomicBoolean cleanupCalled = new AtomicBoolean(false);
		final AtomicInteger updateCount = new AtomicInteger(0);
		final PacketResponseHandler<String> packetResponseHandler =
				new PacketResponseHandler<>(() -> cleanupCalled.set(
				true), updateCount::set);

		final Thread packetProcessor = new Thread(() -> {
			packetResponseHandler.onPacketReceived(createPackets(1000));
			packetResponseHandler.receivedEnd(2000);
			packetResponseHandler.onPacketReceived(createPackets(300));
			packetResponseHandler.interrupt();
			packetResponseHandler.onPacketReceived(createPackets(100));
		});
		packetProcessor.start();

		// This will work properly if it doesn't time out waiting for packets. It should exit on interrupt.
		final List<String> results = packetResponseHandler.getPacketStream(30, TimeUnit.SECONDS)
		                                                  .collect(Collectors.toList());

		packetProcessor.join();

		// clean up called on interrupt, but not on timeout
		Assertions.assertTrue(cleanupCalled.get());
		Assertions.assertTrue(results.size() < 2000);
		// it should process at least everything received until the interrupt
		Assertions.assertTrue(results.size() >= 1300);
	}

	@Test
	void givenNoPacketEndReceived_whenStreamPackets_thenStreamTimesOutSuccessfully() throws InterruptedException {
		final AtomicBoolean cleanupCalled = new AtomicBoolean(false);
		final AtomicInteger updateCount = new AtomicInteger(0);
		final PacketResponseHandler<String> packetResponseHandler =
				new PacketResponseHandler<>(() -> cleanupCalled.set(
				true), updateCount::set);

		final Thread packetProcessor = new Thread(() -> {
			packetResponseHandler.onPacketReceived(createPackets(1000));
		});
		packetProcessor.start();

		final long start = System.currentTimeMillis();

		final List<String> results = packetResponseHandler.getPacketStream(50, TimeUnit.MILLISECONDS)
		                                                  .collect(Collectors.toList());

		packetProcessor.join();

		// Say if this takes more than a second, it didn't time out properly. This is technically prone to a rare
		// race condition if for some reason the thread doesn't run right away after timing out.
		Assertions.assertTrue(System.currentTimeMillis() < start + 1000);

		Assertions.assertFalse(cleanupCalled.get()); // cleanup not called on timeout
	}

}