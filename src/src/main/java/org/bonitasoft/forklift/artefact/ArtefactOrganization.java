package org.bonitasoft.forklift.artefact;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.bonitasoft.engine.identity.ImportPolicy;
import org.bonitasoft.engine.identity.OrganizationImportException;
import org.bonitasoft.forklift.ForkliftAPI.BonitaAccessor;
import org.bonitasoft.forklift.source.Source;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

public class ArtefactOrganization extends Artefact {

	private static Logger logger = Logger.getLogger(ArtefactResource.class.getName());

	public String name;
	public String version;
	public Date dateCreation;


	public ArtefactOrganization(String name, String version, String description, Date dateCreation, Source sourceOrigin) {
		super( "organization", name, version, description, dateCreation, sourceOrigin);
	}

	

	@Override
	public DeployOperation detectDeployment(BonitaAccessor bonitaAccessor)
	{
		DeployOperation deployOperation = new DeployOperation();
		deployOperation.detectionStatus= DetectionStatus.NEWARTEFAC;
		return deployOperation;
	}
	
	protected ByteArrayOutputStream content;

	@Override
	public List<BEvent> loadFromFile(File file) {
		List<BEvent> listEvents = new ArrayList<BEvent>();
		try {
			content = readFile(file);
		} catch (Exception e) {
			listEvents.add(new BEvent(EventReadFile, e, "Page[" + getName() + "] file[" + file.getAbsolutePath() + "]"));
		}
		return listEvents;
	}
	
	
	@Override
	public DeployOperation deploy(BonitaAccessor bonitaAccessor) {
		DeployOperation deployOperation = new DeployOperation();
		try {
			bonitaAccessor.organisationAPI.importOrganization( content.toString(), ImportPolicy.MERGE_DUPLICATES );
		} catch (OrganizationImportException e) {
			deployOperation.listEvents.add( new BEvent( EventErrorAtDeployment, e, ""));
			logger.severe("Forklift.ArtefactOrganization  error import organization "+e.toString());
		}
		deployOperation.deploymentStatus= DeploymentStatus.DEPLOYED;
		return deployOperation;
	}
}
