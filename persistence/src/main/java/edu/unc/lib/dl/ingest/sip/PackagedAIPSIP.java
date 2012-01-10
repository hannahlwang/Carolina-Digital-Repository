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
package edu.unc.lib.dl.ingest.sip;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage;

public class PackagedAIPSIP implements SubmissionInformationPackage {
	private ArchivalInformationPackage aip = null;
	private Agent owner = null;

	PackagedAIPSIP(Agent owner, ArchivalInformationPackage aip) {
		this.owner = owner;
		this.aip = aip;
	}

	public ArchivalInformationPackage getAip() {
		return aip;
	}

	public Agent getOwner() {
		return this.owner;
	}

}
