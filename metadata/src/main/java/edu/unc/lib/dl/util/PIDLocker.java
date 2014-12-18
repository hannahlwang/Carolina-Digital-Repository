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

import java.util.Arrays;

import net.greghaines.jesque.Config;
import net.greghaines.jesque.client.Client;
import net.greghaines.jesque.client.ClientImpl;
import net.greghaines.jesque.utils.JesqueUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import edu.unc.lib.dl.fedora.PID;

/**
 * Lock which allows for locking on individual PIDs. Implemented as a distributed lock
 *
 * @author bbpennel
 * @date Sep 15, 2014
 */
public class PIDLocker {
	private static final Logger log = LoggerFactory.getLogger(PIDLocker.class);

	public static final String HOLDER_PREFIX = "lock-pid-";

	public static final boolean LUA_LOCK = true;

	private Config jesqueConfig;
	private JedisPool jedisPool;

	// Time to live for a lock, measured in seconds
	private int lockTimeout;
	// Time between attempts to capture a lock, in milliseconds
	private long lockRetry;

	private Client makeClient() {
		Client result = new ClientImpl(jesqueConfig);
		return result;
	}

	public String getHolderName() {
		return HOLDER_PREFIX + Thread.currentThread().getId();
	}

	public static String getLockKey(PID pid) {
		return pid.getUUID() + ".lock";
	}

	private static final String lockScript = "--lockscript, parameters: lock_key, lock_timeout, lock_value\n"
			+ "local lock = redis.call('get', KEYS[1]);\n"
			+ "if not lock or lock == ARGV[2] then\n"
			+ "	return redis.call('setex', KEYS[1], ARGV[1], ARGV[2])\n;"
			+ "end\n"
			+ "return false";

	private static final String unlockScript = "--lockscript, parameters: lock_key, lock_value\n"
			+ "local lockHolder = redis.call('get', KEYS[1]);\n"
			+ "if lockHolder == ARGV[1] then\n"
			+ "	return redis.call('del', KEYS[1]);\n"
			+ "end\n"
			+ "return false";

	public void lock(PID pid) {
		if (LUA_LOCK) {
			lockLua(pid);
		} else {
			lockResque(pid);
		}
	}

	public boolean unlock(PID pid) {
		if (LUA_LOCK) {
			return unlockLua(pid);
		} else {
			return unlockResque(pid);
		}
	}

	public void lockLua(PID pid) {
		String lockHolder = getHolderName();

		while (true) {
			Jedis jedis = jedisPool.getResource();
			try {
				log.debug("Acquiring lock on {} for thread {}", pid.getPid(), Thread.currentThread().getId());
				long start = System.currentTimeMillis();
				Object result = jedis.eval(lockScript, Arrays.asList(getLockKey(pid)),
						Arrays.asList(Integer.toString(lockTimeout), getHolderName()));
				System.out.println("Locked in " + (System.currentTimeMillis() - start));
				if ("OK".equals(result)) {
					log.debug("Acquired lock on {} for thread {}", pid.getPid(), Thread.currentThread().getId());
					return;
				} else {
					log.debug("Lock for {} belonged to different thread, waiting in thread {}", pid, lockHolder);
					Thread.sleep(lockRetry);
				}
			} catch (InterruptedException e) {
				log.info("Interrupted attempt to acquire lock for {}", pid.getPid());
			} finally {
				jedisPool.returnResource(jedis);

			}
		}
	}

	public boolean unlockLua(PID pid) {
		String lockHolder = getHolderName();

		Jedis jedis = jedisPool.getResource();
		try {
			log.debug("Releasing lock on {} for thread {}", pid.getPid(), lockHolder);
			long start = System.currentTimeMillis();
			Object result = jedis.eval(unlockScript, Arrays.asList(getLockKey(pid)), Arrays.asList(getHolderName()));
			System.out.println("UnLocked in " + (System.currentTimeMillis() - start));
			log.debug("DELETE RESULT {}", result);
			if (result == null) {
				log.debug("Lock for {} belonged to different thread, {} cannot unlock it", pid, lockHolder);
				return false;
			} else {
				log.debug("Released lock on {} for thread {}", pid.getPid(), lockHolder);
				return true;
			}
		} finally {
			jedisPool.returnResource(jedis);
		}
	}

	/**
	 * Obtain a lock on a particular pid. If a lock can't be obtained the thread will wait. If this thread already owns
	 * the lock, the lock will be refreshed
	 *
	 * @param pid
	 *           pid of the object to lock
	 */
	public void lockResque(PID pid) {
		log.debug("Acquiring lock on {} for thread {}", pid.getPid(), Thread.currentThread().getId());
		Client client = makeClient();
		try {
			String lockHolder = getHolderName();
			while (true) {
				boolean acquired = client.acquireLock(getLockKey(pid), lockHolder, lockTimeout);
				if (acquired) {
					log.debug("Acquired lock on {} for thread {}", pid.getPid(), Thread.currentThread().getId());
					break;
				} else {
					log.debug("Lock for {} belonged to different thread, waiting in thread {}", pid, lockHolder);
					Thread.sleep(lockRetry);
				}
			}
		} catch (InterruptedException e) {
			log.info("Interrupted attempt to acquire lock for {}", pid.getPid());
		} finally {
			client.end();
		}
	}

	/**
	 * Release a lock for the specified pid so long as this thread owns the lock
	 *
	 * @param pid
	 */
	public boolean unlockResque(PID pid) {
		log.debug("Releasing lock on {} from thread {}", pid.getPid(), Thread.currentThread().getId());

		String lockKey = JesqueUtils.createKey(jesqueConfig.getNamespace(), getLockKey(pid));
		String lockHolder = getHolderName();

		Jedis jedis = null;
		Transaction tx = null;
		while (true) {
			jedis = jedisPool.getResource();
			try {
				log.debug("Multiing {}", jedis.getClient().isInMulti());
				log.debug("LOOPING {}", Thread.currentThread().getId());
				jedis.watch(lockKey);
				log.debug("Multiing {}", jedis.getClient().isInMulti());
				if (Thread.currentThread().getId() != 1)
					log.debug("HALT");
				String registeredHolder = jedis.get(lockKey);

				tx = jedis.multi();

				if (registeredHolder != null && registeredHolder.equals(lockHolder)) {
					log.debug("Releasing lock on {} from thread {}", pid.getPid(), Thread.currentThread().getId());
					tx.del(lockKey);

					if (tx.exec() != null) {
						log.debug("Released lock on {} from thread {}", pid.getPid(), Thread.currentThread().getId());
						return true;
					} else {
						log.debug("Failed to transactionally unlock {}, retrying {}", pid.getPid(), Thread.currentThread()
								.getId());
					}
				} else {
					log.debug("Multiing {}", jedis.getClient().isInMulti());
					tx.discard();

					log.debug("A different thread, {}, has taken over the lock for {}", registeredHolder, pid.getPid());
					return false;
				}
			} finally {
				if (jedis.getClient().isInMulti() && tx != null) {
					tx.discard();
					log.debug("Discarding incomplete transaction for {}", Thread.currentThread().getId());
				}
				jedisPool.returnResource(jedis);
			}
		}
	}

	public void setJesqueConfig(Config jesqueConfig) {
		this.jesqueConfig = jesqueConfig;
	}

	public void setJedisPool(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

	public void setLockTimeout(int lockTimeout) {
		this.lockTimeout = lockTimeout;
	}

	public void setLockRetry(long lockRetry) {
		this.lockRetry = lockRetry;
	}
}
