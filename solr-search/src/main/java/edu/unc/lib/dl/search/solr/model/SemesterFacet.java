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
package edu.unc.lib.dl.search.solr.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.util.DateTimeUtil;

/**
 * @author bbpennel
 * @date Oct 20, 2015
 */
public class SemesterFacet extends GenericFacet {
	private static final Logger log = LoggerFactory.getLogger(SemesterFacet.class);
	
	@Override
	public void setDisplayValue(String value) {
		if (value == null) {
			this.displayValue = null;
			return;
		}
		
		try {
			this.displayValue = DateTimeUtil.parseUTCDateToSemester(value);
		} catch (IllegalArgumentException e) {
			this.displayValue = null;
			log.debug("Failed to parse display value for semester facet value {}", value);
		}
	}
}
