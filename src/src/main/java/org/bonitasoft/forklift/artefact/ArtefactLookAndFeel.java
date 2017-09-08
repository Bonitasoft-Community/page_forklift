package org.bonitasoft.forklift.artefact;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bonitasoft.forklift.ForkliftAPI.BonitaAccessor;
import org.bonitasoft.forklift.source.Source;
import org.bonitasoft.log.event.BEvent;

public class ArtefactLookAndFeel extends Artefact {

	public String name;
	public String version;
	public Date dateCreation;

	public ArtefactLookAndFeel(String name, String version, String description, Date dateCreation, Source sourceOrigin) {
		super( "LookAndFeel", name, version, description, dateCreation, sourceOrigin);
	}

	

	public List<BEvent> loadFromFile(File file) {
		return new ArrayList<BEvent>();
	}

	@Override
	public DeployOperation detectDeployment(BonitaAccessor bonitaAccessor)
	{
		DeployOperation deployOperation = new DeployOperation();
		deployOperation.detectionStatus= DetectionStatus.SAME;
		return deployOperation;
	}
	
	
	@Override
	public DeployOperation deploy(BonitaAccessor bonitaAccessor) {
		DeployOperation deployOperation = new DeployOperation();
		deployOperation.deploymentStatus= DeploymentStatus.DEPLOYEDFAILED;
		return deployOperation;
	}

}
