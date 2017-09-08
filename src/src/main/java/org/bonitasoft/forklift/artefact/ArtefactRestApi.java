package org.bonitasoft.forklift.artefact;

import java.util.Date;

import org.bonitasoft.forklift.ForkliftAPI.BonitaAccessor;
import org.bonitasoft.forklift.artefact.Artefact.DeployOperation;
import org.bonitasoft.forklift.source.Source;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

public class ArtefactRestApi extends ArtefactResource {
	protected static BEvent EventDeploymentRestFailed = new BEvent(ArtefactRestApi.class.getName(), 1, Level.APPLICATIONERROR, "Rest deployment Error", "The Rest API is deployed but will failed", "The call will return a 403", "Upload manualy the restApi");

	public String name;
	public String version;
	public Date dateCreation;

	public ArtefactRestApi(String name, String version, String description, Date dateCreation, Source sourceOrigin) {
		super( "restapi", name, version, description, dateCreation, sourceOrigin);
	}

	@Override
	public String getContentType() {
		return "apiExtension";
	}


	@Override
	public DeployOperation deploy(BonitaAccessor bonitaAccessor) {
		DeployOperation deployOperation = super.deploy(bonitaAccessor);
		if (deployOperation.deploymentStatus == DeploymentStatus.DEPLOYED)
		{
			// in fact, no ! See https://bonitasoft.atlassian.net/browse/BS-16862
			deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
			deployOperation.listEvents.add( EventDeploymentRestFailed);
		}
		return deployOperation;
	}
}
