package edu.unc.lib.deposit.normalize;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.METSParseException;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.METSProfile;

public abstract class AbstractMETS2N3BagJob extends AbstractDepositJob {

	private static final Logger log = LoggerFactory.getLogger(AbstractMETS2N3BagJob.class);

	@Autowired
	protected SchematronValidator schematronValidator = null;
	@Autowired
	private Schema metsSipSchema = null;

	public SchematronValidator getSchematronValidator() {
		return schematronValidator;
	}

	public void setSchematronValidator(SchematronValidator schematronValidator) {
		this.schematronValidator = schematronValidator;
	}

	public AbstractMETS2N3BagJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	public AbstractMETS2N3BagJob() {
		super();
	}

	protected File getMETSFile() {
		File dataDir = new File(getDepositDirectory(), "data");
		File result = new File(dataDir, "mets.xml");
		if(!result.exists()) {
			result = new File(dataDir, "METS.xml");
		}
		if(!result.exists()) {
			result = new File(dataDir, "METS.XML");
		}
		if(!result.exists()) {
			result = new File(dataDir, getDepositStatus().get(DepositField.fileName.name()));
		}
		return result;
	}

	public Schema getMetsSipSchema() {
		return metsSipSchema;
	}

	public void setMetsSipSchema(Schema metsSipSchema) {
		this.metsSipSchema = metsSipSchema;
	}

	protected void assignPIDs(Document mets) {
		int count = 0;
		Iterator<Element> divs = mets.getDescendants(new ElementFilter("div", JDOMNamespaceUtil.METS_NS));
		while(divs.hasNext()) {
			Element div = divs.next();
			String cids = div.getAttributeValue("CONTENTIDS");
			if(cids != null && cids.contains("info:fedora/")) continue;
			UUID uuid = UUID.randomUUID();
			PID pid = new PID("uuid:"+uuid.toString());
			if(cids == null) {
				div.setAttribute("CONTENTIDS", pid.getURI());
			} else {
				StringBuilder sb = new StringBuilder(pid.getURI());
				for(String s : cids.split("\\s")) {
					sb.append(" ").append(s);
				}
				div.setAttribute("CONTENTIDS", sb.toString());
			}
			Element pidEvent = getEventLog().logEvent(Type.NORMALIZATION, "Assigned PID to object defined in a METS div", pid);
			appendDepositEvent(pid, pidEvent);
			count++;
		}
		recordDepositEvent(Type.NORMALIZATION, "Assigned {0,choice,1#PID|2#PIDs} to {0,choice,1#one object|2#{0,number} objects} ", count);
	}

	protected Document loadMETS() {
		Document mets = null;
		SAXBuilder builder = new SAXBuilder();
		try {
			mets = builder.build(getMETSFile());
		} catch (Exception e) {
			failJob(e, "Unexpected error parsing METS file.");
		}
		return mets;
	}

	protected void saveMETS(Document mets) {
		try(FileOutputStream fos = new FileOutputStream(getMETSFile())) {
			new XMLOutputter().output(mets, fos);
		} catch(Exception e) {
			failJob(e, "Unexpected error saving METS.");
		}
	}

	protected void validateMETS() {
		if(!getMETSFile().exists()) {
			failJob("Cannot find a METS file", "A METS was not found in the expected locations: mets.xml, METS.xml, METS.XML.");
		}
		Validator metsValidator = getMetsSipSchema().newValidator();
		METSParseException handler = new METSParseException(
				"There was a problem parsing METS XML.");
		metsValidator.setErrorHandler(handler);
		try {
			metsValidator.validate(new StreamSource(getMETSFile()));
		} catch (SAXException e) {
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage());
			}
			failJob(handler, "METS is not valid with respect to schemas.");
		} catch (IOException e) {
			failJob(e, "Cannot parse METS file.");
		}
		recordDepositEvent(Type.VALIDATION, "METS schema(s) validated");
	}

	/**
	 * Stores a reference to the METS file for this deposit as the manifest
	 */
	protected void addManifestURI() {
		File metsFile = getMETSFile();
		log.debug("Adding manifest URI referencing {}", metsFile);
		getDepositStatusFactory().addManifest(getDepositUUID(), metsFile.getAbsolutePath());
	}

	protected void validateProfile(METSProfile profile) {
		List<String> errors = schematronValidator.validateReportErrors(new StreamSource(getMETSFile()), profile.getName());
		if(errors != null && errors.size() > 0) {
			String msg = MessageFormat.format("METS is not valid with respect to profile: {0}", profile.getName());
			StringBuilder details = new StringBuilder();
			for(String error : errors) {
				details.append(error).append('\n');
			}
			failJob(msg, details.toString());
		}
	}

}
