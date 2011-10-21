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

package edu.unc.lib.dl.cdr.services.processing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.model.PIDMessage;

/**
 * 
 * @author bbpennel
 * 
 */
public abstract class MessageFilter {
	private static final Logger LOG = LoggerFactory.getLogger(MessageFilter.class);
	protected static String identifier;
	
	public boolean filter(PIDMessage msg) {
		return false;
	}
	
	public String getIdentifier(){
		return identifier;
	}
}