/**
 * Copyright 2016 The University of North Carolina at Chapel Hill
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

import static org.junit.Assert.*;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.JedisPool;

public class DepositStatusFactoryTest {
	
	private DepositStatusFactory factory;
	private JedisPool jedisPool;
	
	@Before
	public void setup() {
		factory = new DepositStatusFactory();
		jedisPool = new JedisPool("localhost", 6379);
		factory.setJedisPool(jedisPool);
	}

	@Test
	public void testAddThenGetManifest() {
		final String uuid = Integer.toString(new Random().nextInt(99999));
		final String filename1 = "bagit.txt";
		final String filename2 = "manifest-md5.txt";
		
		factory.addManifest(uuid, filename1);
		factory.addManifest(uuid,  filename2);
		List<String >filenames = factory.getManifestURIs(uuid);
		
		assertTrue(filenames.size() == 2);
		assertEquals(filename1, filenames.get(0));
		assertEquals(filename2, filenames.get(1));
	}

}
