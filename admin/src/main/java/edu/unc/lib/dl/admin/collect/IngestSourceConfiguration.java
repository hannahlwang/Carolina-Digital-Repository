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
package edu.unc.lib.dl.admin.collect;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Configuration for a ingest source
 * 
 * @author bbpennel
 * @date Oct 22, 2015
 */
public class IngestSourceConfiguration {

	private String id;
	private String name;
	private Map<String, List<String>> patterns;
	private List<String> containers;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, List<String>> getPatterns() {
		return patterns;
	}

	public void setPatterns(Map<String, List<String>> patterns) {
		// Make sure all paths end with /'s otherwise glob patterns won't match
		Iterator<Entry<String, List<String>>> patternIt = patterns.entrySet().iterator();
		while (patternIt.hasNext()) {
			Entry<String, List<String>> patternEntry = patternIt.next();
			if (!patternEntry.getKey().endsWith("/")) {
				patternIt.remove();
				patterns.put(patternEntry.getKey() + "/", patternEntry.getValue());
			}
		}
		this.patterns = patterns;
	}

	public List<String> getContainers() {
		return containers;
	}

	public void setContainers(List<String> containers) {
		this.containers = containers;
	}

}
