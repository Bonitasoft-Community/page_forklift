package org.bonitasoft.forklift.artefact;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.log.event.BEvent;

import org.bonitasoft.forklift.ForkliftAPI.BonitaAccessor;
import org.bonitasoft.forklift.source.DeploySet;
import org.bonitasoft.forklift.source.Source;

public abstract class Artefact {

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


}
