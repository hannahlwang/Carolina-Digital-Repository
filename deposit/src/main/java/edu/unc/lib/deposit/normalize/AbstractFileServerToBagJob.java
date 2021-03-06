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
package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.deposit.work.DepositGraphUtils.dprop;
import static edu.unc.lib.deposit.work.DepositGraphUtils.fprop;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.CONTAINER;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.staging.Stages;

/**
 * Abstract deposit normalization job which processes walks file system paths to interpret them into n3 and MODS for
 * deposit
 * 
 * @author lfarrell
 */
public abstract class AbstractFileServerToBagJob extends AbstractDepositJob {
	private static final Logger log = LoggerFactory
			.getLogger(AbstractFileServerToBagJob.class);
	
	@Autowired
	public Stages stages;
	
	private Map<String, Bag> pathToFolderBagCache;
	
	public AbstractFileServerToBagJob() {
		pathToFolderBagCache = new HashMap<>();
	}
	
	public AbstractFileServerToBagJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
		
		pathToFolderBagCache = new HashMap<>();
	}

	@Override
	public abstract void runJob();
	
	protected Bag getSourceBag(Bag depositBag, File sourceFile) {
		Model model = depositBag.getModel();
		Map<String, String> status = getDepositStatus();
		
		PID containerPID = new PID("uuid:" + UUID.randomUUID());
		Bag bagFolder = model.createBag(containerPID.getURI());
		model.add(bagFolder, dprop(model, DepositRelationship.label), 
				status.get(DepositField.fileName.name()));
		model.add(bagFolder, fprop(model, FedoraProperty.hasModel), 
				model.createResource(CONTAINER.getURI().toString()));
		depositBag.add(bagFolder);
		
		// Cache the source bag folder
		pathToFolderBagCache.put(sourceFile.getName(), bagFolder);
		
		// Add extra descriptive information
		addDescription(containerPID, status);
		
		return bagFolder;
	}
	
	/**
	 * Creates and returns a Jena Resource for the given path representing a file,
	 * adding it to the hierarchy for the deposit  
	 * 
	 * @param sourceBag
	 * @param filepath
	 * @return
	 */
	protected Resource getFileResource(Bag sourceBag, String filepath) {
		Bag parentBag = getParentBag(sourceBag, filepath);

		PID pid = createPID();

		Resource fileResource = sourceBag.getModel().createResource(pid.getURI());
		parentBag.add(fileResource);

		return fileResource;
	}
	
	/**
	 * Creates and returns a Jena Bag for the given filepath representing a folder, and adds
	 * it to the hierarchy for the deposit
	 * 
	 * @param sourceBag
	 * @param filepath
	 * @param model
	 * @return
	 */
	protected Bag getFolderBag(Bag sourceBag, String filepath) {
		Bag parentBag = getParentBag(sourceBag, filepath);
		
		PID pid = createPID();
		
		Bag bagFolder = sourceBag.getModel().createBag(pid.getURI());
		parentBag.add(bagFolder);
		
		pathToFolderBagCache.put(filepath, bagFolder);
		return bagFolder;
	}
	
	private PID createPID() {
		UUID uuid = UUID.randomUUID();
		PID pid = new PID("uuid:" + uuid.toString());
		
		return pid;
	}
	
	/**
	 * Returns a Jena Bag object for the parent folder of the given filepath, creating the parent if it is not present.
	 * 
	 * @param sourceBag
	 * @param filepath
	 * @return
	 */
	protected Bag getParentBag(Bag sourceBag, String filepath) {
		// Retrieve the bag from the cache by base filepath if available.
		String basePath = Paths.get(filepath).getParent().toString();
		if (pathToFolderBagCache.containsKey(basePath)) {
			return pathToFolderBagCache.get(basePath);
		}
		
		Model model = sourceBag.getModel();
		
		// find or create a folder resource for the filepath
		String[] pathSegments = filepath.split("/");
		
		// Nothing to do with paths that only have data
		if (pathSegments.length <= 2) {
			return sourceBag;
		}
		
		Property labelProp = dprop(model, DepositRelationship.label);
		Property hasModelProp = model.createProperty(FedoraProperty.hasModel.getURI().toString());
		Resource containerResource = model.createResource(CONTAINER.getURI().toString());
		
		Bag currentNode = sourceBag;
		
		for (int i = 1; i < pathSegments.length - 1; i++) {
			
			String segment = pathSegments[i];
			String folderPath = StringUtils.join(Arrays.copyOfRange(pathSegments, 0, i + 1), "/");
			
			if (pathToFolderBagCache.containsKey(folderPath)) {
				currentNode = pathToFolderBagCache.get(folderPath);
				continue;
			}
			
			log.debug("No cached folder bag for {}, creating new one", folderPath);
			// No existing folder was found, create one
			PID pid = new PID("uuid:" + UUID.randomUUID().toString());
			
			Bag childBag = model.createBag(pid.getURI());
			currentNode.add(childBag);
			
			model.add(childBag, labelProp, segment);
			model.add(childBag, hasModelProp, containerResource);
			
			pathToFolderBagCache.put(folderPath, childBag);
			
			currentNode = childBag;
		}
		
		return currentNode;
	}
	
	/**
	 * Adds additional metadata fields for the root bag container if they are provided
	 * 
	 * @param containerPID
	 * @param status
	 */
	public void addDescription(PID containerPID, Map<String, String> status) {
		Document doc = new Document();
		Element mods = new Element("mods", JDOMNamespaceUtil.MODS_V3_NS);
		doc.addContent(mods);
		
		if (status.containsKey(DepositField.extras.name())) {
			ObjectMapper mapper = new ObjectMapper();
			try {
				JsonNode node = mapper.readTree(status.get(DepositField.extras.name()));
				
				JsonNode accessionNode = node.get("accessionNumber");
				if (accessionNode != null) {
					Element identifier = new Element("identifier", JDOMNamespaceUtil.MODS_V3_NS);
					identifier.setText(accessionNode.asText());
					identifier.setAttribute("type", "local");
					identifier.setAttribute("displayLabel", "Accession Identifier");
					mods.addContent(identifier);
				}
				
				JsonNode mediaNode = node.get("mediaId");
				if (mediaNode != null) {
					Element identifier = new Element("identifier", JDOMNamespaceUtil.MODS_V3_NS);
					identifier.setText(mediaNode.asText());
					identifier.setAttribute("type", "local");
					identifier.setAttribute("displayLabel", "Source Identifier");
					mods.addContent(identifier);
				}
			} catch (IOException e) {
				failJob(e, "Failed to parse extras data for {}", getDepositPID());
				log.error("Failed to parse extras data for {}", this.getDepositPID(), e);
			}
		}
		
		// Persist the MODS file to disk if there were any fields added
		if (mods.getChildren().size() > 0) {
			final File modsFolder = getDescriptionDir();
			modsFolder.mkdirs();
			File modsFile = new File(modsFolder, containerPID.getUUID() + ".xml");
			try (FileOutputStream fos = new FileOutputStream(modsFile)) {
				new XMLOutputter(org.jdom2.output.Format.getPrettyFormat()).output(mods.getDocument(), fos);
			} catch (IOException e) {
				failJob(e, "Unable to write descriptive metadata for bag deposit {}", getDepositPID());
			}
			
		}
	}

	public void setStages(Stages stages) {
		this.stages = stages;
	}

}