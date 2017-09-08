package org.bonitasoft.forklift.artefact;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.bonitasoft.forklift.ForkliftAPI.BonitaAccessor;
import org.bonitasoft.forklift.source.DeploySet;
import org.bonitasoft.forklift.source.Source;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

public abstract class Artefact {

	protected static BEvent EventErrorAtDeployment = new BEvent(Artefact.class.getName(), 1, Level.APPLICATIONERROR, "Error at deployment", "The artefact can't be deployed", "artefact is not accessible", "Check the exception");
	protected static BEvent EventErrorAtDetection = new BEvent(Artefact.class.getName(), 2, Level.APPLICATIONERROR, "Error at detection", "Detection on the server for this artefact failed, can't know if the artefact exist or not", "Artefact can't be deployed", "Check the exception");
	protected static BEvent EventReadFile = new BEvent(Artefact.class.getName(), 3, Level.APPLICATIONERROR, "File error", "The file can't be read", "The artefact is ignored", "Check the exception");

	
	public enum DetectionStatus {
		NEWARTEFAC, SAME, OLDVERSION, NEWVERSION,DETECTIONFAILED
	};

	public enum Action {
		DEPLOY, IGNORE, DELETE
	};

	public enum DeploymentStatus {
		NOTHINGDONE, NEWALREADYINPLACE, REMOVEFAIL, LOADFAILED, DEPLOYEDFAILED, DEPLOYED, DELETED, BADBONITAVERSION
	};

	public static class DeployOperation {
		public Artefact artefact;
		/**
		 * in case of detection, the deployStatus is updated
		 */
		public DetectionStatus detectionStatus;
		public Action action;
		/**
		 * in case of deployment, the deployStatus is updated
		 */
		public DeploymentStatus deploymentStatus;

		public Date presentDateArtefact;
		public String presentVersionArtefact;

		public List<BEvent> listEvents = new ArrayList<BEvent>();
		public String report;
	}

	private String typeId;
	private String name;
	private String version;
	private String description;
	private Date dateArtefact;

	
	public Source sourceOrigin;
	public DeploySet deploySet;

	/*
	 * to retrieve any information, source can save the information it wants
	 */
	public Object privateSourceInformation;

	/**
	 * constraint : each artefact has a minimum a Name, a Version, and a date of creation
	 * @param typeId the string must be egals with the HTML
	 * @param name name of the artefact
	 * @param version may be null if the artefact is not manage in version
	 * 
	 * @param sourceOrigin
	 */
	public Artefact(String typeId, String name, String version, String description, Date dateArtefact, Source sourceOrigin)
	{
		this.typeId = typeId;
		this.name= name;
		this.version= version;
		this.description=description;
		this.dateArtefact = dateArtefact;
				
		this.sourceOrigin = sourceOrigin;
	}

	public String getTypeId() {
		return typeId;
	}

	
	public String getName() {
		return name;
	}

	
	public String getVersion() {
		return version;
	}

	
	public String getDescription() {
		return description;
	}

	
	public Date getDate() {
		return dateArtefact;
	}

	/**
	 * to get performance, the artefact is in general just referenced (name +
	 * version) and not loaded.
	 */
	public boolean isLoaded = false;

	public abstract List<BEvent> loadFromFile(File file);

	public Map<String, Object> toMap() {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		final Map<String, Object> result = new HashMap<String, Object>();
		result.put("source", sourceOrigin == null ? null : sourceOrigin.getName());
		result.put("deployset", deploySet == null ? null : deploySet.getName());
		result.put("type", getTypeId());
		result.put("name", getName());
		result.put("version", getVersion());
		result.put("desc", getDescription());
		result.put("dateartefact", sdf.format(deploySet == null ? getDate() : deploySet.getDate()));
		return result;
	}

	/**
	 * Deployment operation
	 */
	/**
	 * Detect This method check the current server by the new artefact, and
	 * return a analysis. It do no operation, only the analysis
	 * 
	 * @param bonitaAccessor
	 * @return return a status
	 */
	public abstract DeployOperation detectDeployment(BonitaAccessor bonitaAccessor);

	/**
	 * do the deployment
	 * 
	 * @param bonitaAccessor
	 * @return
	 */
	public abstract DeployOperation deploy(BonitaAccessor bonitaAccessor);

	public DeploySet getDeploySet() {
		return deploySet;
	}

	protected ByteArrayOutputStream readFile( File file) throws FileNotFoundException, IOException
	{
	 ByteArrayOutputStream out = new ByteArrayOutputStream();
	 IOUtils.copy(new FileInputStream(file), out);
	 return out;
	}
}
