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
package edu.unc.lib.dl.fedora;

import org.jdom2.Document;

/**
 * @author bbpennel
 * @date Jul 2, 2015
 */
public class DatastreamDocument {
	private final Document document;
	private final String lastModified;

	public DatastreamDocument(Document document, String lastModified) {
		super();
		this.document = document;
		this.lastModified = lastModified;
	}

	public Document getDocument() {
		return document;
	}

	public String getLastModified() {
		return lastModified;
	}
}
