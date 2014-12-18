/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.greghaines.jesque.Config;
import net.greghaines.jesque.utils.JesqueUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import edu.unc.lib.dl.fedora.PID;

/**
 * @author bbpennel
 * @date Dec 9, 2014
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/jedis-context.xml" })
@DirtiesContext
public class PIDLockerIT {
	private static final Logger log = LoggerFactory.getLogger(PIDLockerIT.class);

	@Autowired
	private JedisPool jedisPool;
	@Autowired
	private Config jesqueConfig;
	private Jedis jedis;

	@Autowired
	private PIDLocker locker;

	@Before
	public void init() {
		jedis = jedisPool.getResource();
		jedis.flushAll();
	}

	@After
	public void cleanup() {
		jedisPool.returnResource(jedis);
	}

	private String getLockKey(PID pid) {
		if (PIDLocker.LUA_LOCK) {
			return PIDLocker.getLockKey(pid);
		} else {
			return JesqueUtils.createKey(jesqueConfig.getNamespace(), PIDLocker.getLockKey(pid));
		}
	}

	@Test
	public void lockAndUnlockTest() throws Exception {
		PID pid = new PID("uuid:test");

		locker.lock(pid);

		String lockKey = getLockKey(pid);
		String registeredHolder = jedis.get(lockKey);
		String holderName = locker.getHolderName();

		assertNotNull("Lock was not found", registeredHolder);
		assertEquals("Lock was assigned to the wrong holder", holderName, registeredHolder);

		locker.unlock(pid);

		String result = jedis.get(lockKey);
		assertNull("Lock was not released", result);
	}

	@Test
	public void lockExpiredTest() throws Exception {
		PID pid = new PID("uuid:test");

		locker.lock(pid);

		String lockKey = getLockKey(pid);
		String registeredHolder = jedis.get(lockKey);

		assertNotNull("Lock was not found", registeredHolder);

		Thread.sleep(1200L);

		String result = jedis.get(lockKey);
		assertNull("Lock did not expire", result);
	}

	@Test
	public void renewLockTest() throws Exception {
		PID pid = new PID("uuid:test");

		locker.lock(pid);

		String lockKey = getLockKey(pid);

		Thread.sleep(500L);
		String result = jedis.get(lockKey);
		assertNotNull("Lock should not have expired", result);

		// Renew the lock
		locker.lock(pid);

		// Wait until after the lock would normally have expired after one second
		Thread.sleep(700L);

		result = jedis.get(lockKey);
		assertNotNull("Lock should not have expired after being renewed", result);

		Thread.sleep(400L);
		result = jedis.get(lockKey);
		assertNull("Lock should have expired", result);
	}

	@Test
	public void contentionTest() throws Exception {
		final PID pid = new PID("uuid:test");

		Thread competitor1 = getLockThread(pid, 200L);
		Thread competitor2 = getLockThread(pid, 500L);

		String competitor1Holder = PIDLocker.HOLDER_PREFIX + competitor1.getId();
		String competitor2Holder = PIDLocker.HOLDER_PREFIX + competitor2.getId();

		String lockKey = getLockKey(pid);

		competitor1.start();
		Thread.sleep(50L);
		competitor2.start();
		Thread.sleep(50L);

		String holder = jedis.get(lockKey);
		assertEquals("First competitor should have hold of lock", competitor1Holder, holder);

		Thread.sleep(250L);
		log.debug("Checking that lock has transfered to second competitor thread");
		holder = jedis.get(lockKey);
		assertEquals("Second competitor should have captured the lock", competitor2Holder, holder);

		competitor1.join();
		competitor2.join();

		log.debug("All locking threads should have completed");

		holder = jedis.get(lockKey);
		assertNull("All competitors should have left the arena", holder);
	}

	private Thread getLockThread(final PID pid, final long delay) {
		return new Thread(new Runnable() {
			@Override
			public void run() {
				log.debug("Locker thread {} about to try to acquire lock on {} for {}", new Object[] {
						Thread.currentThread().getId(), pid, delay });

				locker.lock(pid);

				log.debug("Locker thread {} acquired lock on {}", Thread.currentThread().getId(), pid);

				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					log.debug("Interrupted while waiting for thread {}", Thread.currentThread().getId());
				}

				log.debug("Locker thread {} about to release lock on {}", Thread.currentThread().getId(), pid);

				locker.unlock(pid);

				log.debug("Locker thread {} released lock on {}", Thread.currentThread().getId(), pid);
			}
		});
	};

	@Test
	public void multipleLocksSameThreadTest() {
		PID pid1 = new PID("uuid:test1");
		PID pid2 = new PID("uuid:test2");

		String lockKey1 = getLockKey(pid1);
		String lockKey2 = getLockKey(pid2);
		String holderName = locker.getHolderName();

		locker.lock(pid1);

		assertNotNull("Lock was not found", jedis.get(lockKey1));

		locker.lock(pid2);
		assertEquals("First lock was not correctly held", holderName, jedis.get(lockKey1));
		assertEquals("Second lock was not correctly held", holderName, jedis.get(lockKey2));

		locker.unlock(pid1);

		assertNull("First lock was not released", jedis.get(lockKey1));
		assertNotNull("Second lock released prematurely", jedis.get(lockKey2));

		locker.unlock(pid2);

		assertNull("Second lock was not released", jedis.get(lockKey2));
	}

	@Test
	public void stolenExpiredTest() throws Exception {
		PID pid = new PID("uuid:test");

		String lockKey = getLockKey(pid);

		locker.lock(pid);

		assertNotNull("Lock was not found", jedis.get(lockKey));

		// Wait to expire the main lock
		Thread.sleep(1500L);

		assertNull("Lock should have expired", jedis.get(lockKey));

		Thread thief = getLockThread(pid, 200L);
		String thiefHolder = PIDLocker.HOLDER_PREFIX + thief.getId();

		thief.start();

		Thread.sleep(10L);

		assertEquals("Competiting thread should own the lock", thiefHolder, jedis.get(lockKey));
		boolean result = locker.unlock(pid);
		assertFalse("Unlocking incorrectly was successful for expired lock", result);

		assertEquals("Competiting thread lost lock after main thread released it", thiefHolder, jedis.get(lockKey));

		thief.join();

		assertNull("Competiting thread should have released the lock", jedis.get(lockKey));
	}

	@Test
	public void randomStressTest() throws Exception {

		final PID pid = new PID("uuid:test");
		final String lockKey = getLockKey(pid);

		int numberOfThreads = 100;
		List<Future<?>> threads = new ArrayList<>(numberOfThreads);

		ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

		for (int i = 0; i < numberOfThreads; i++) {
			Thread competitor = getAssertingThread(pid, lockKey);
			threads.add(executor.submit(competitor));
		}

		Exception thrown = null;
		// Wait for all threads to finish
		for (Future<?> thread : threads) {
			try {
				thread.get(60000, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				thrown = e;
			}
		}
		// Rethrow the last exception after all the threads have been joined so that they won't mess up other tests
		if (thrown != null) {
			throw thrown;
		}

		assertNull("All threads should have released the lock", jedis.get(lockKey));
	}

	@Test
	public void multiPIDRandomStressTest() throws Exception {

		int numberOfPids = 5;
		int numberOfThreads = 10;
		List<Future<?>> threads = new ArrayList<>(numberOfThreads);

		ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

		for (int i = 0; i < numberOfThreads; i++) {
			final PID pid = new PID("uuid:test" + (int) (Math.random() * numberOfPids));
			final String lockKey = getLockKey(pid);

			Thread competitor = getAssertingThread(pid, lockKey);
			threads.add(executor.submit(competitor));
		}

		Exception thrown = null;
		// Wait for all threads to finish
		for (Future<?> thread : threads) {
			try {
				thread.get(60000, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				log.error("An exception occurred while waiting for locking thread", e);
				thrown = e;
			}
		}
		// Rethrow the last exception after all the threads have been joined so that they won't mess up other tests
		if (thrown != null) {
			throw thrown;
		}

		for (int i = 0; i < numberOfPids; i++) {
			final PID pid = new PID("uuid:test" + i);
			final String lockKey = getLockKey(pid);
			assertNull("All threads should have released the lock", jedis.get(lockKey));
		}

	}

	private Thread getAssertingThread(final PID pid, final String lockKey) {
		return new Thread(new Runnable() {
			@Override
			public void run() {
				final long startDelay = (long) (Math.random() * 5);
				final long holdDelay = (long) (Math.random() * 20);

				String holderName = PIDLocker.HOLDER_PREFIX + Thread.currentThread().getId();

				try {
					Thread.sleep(startDelay);
				} catch (InterruptedException e) {
					log.debug("Interrupted while waiting for thread {}", holderName);
				}

				log.debug("Locker thread {} about to try to acquire lock on {} for {}", holderName, pid);

				locker.lock(pid);

				assertEquals("Thread should exclusively own the lock", holderName, jedis.get(lockKey));

				log.debug("Locker thread {} acquired lock on {}", holderName, pid);

				try {
					Thread.sleep(holdDelay);
				} catch (InterruptedException e) {
					log.debug("Interrupted while waiting for thread {}", holderName);
				}

				log.debug("Locker thread {} about to release lock on {}", holderName, pid);

				locker.unlock(pid);

				assertNotEquals("Thread should no longer own the lock", holderName, jedis.get(lockKey));

				log.debug("Locker thread {} released lock on {}", holderName, pid);
			}
		});
	}
}
