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
package edu.unc.lib.dl.data.ingest.solr.action;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;

/**
 * @author bbpennel
 * @date Jun 15, 2015
 */
public class UpdateTreeSetRelationsAction extends UpdateChildSetAction {
	
	public UpdateTreeSetRelationsAction() {
		addDocumentMode = false;
	}

	@Override
	public DocumentIndexingPackage getDocumentIndexingPackage(PID pid, DocumentIndexingPackage parent)
			throws IndexingException {
		DocumentIndexingPackage dip = new DocumentIndexingPackage(pid);
		dip.setParentDocument(parent);
		// Get all triples in order to retrieve children
		dip.setTriples(tsqs.fetchAllTriples(dip.getPid()));

		return dip;
	}
}