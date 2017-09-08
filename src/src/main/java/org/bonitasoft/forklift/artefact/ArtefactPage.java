package org.bonitasoft.forklift.artefact;

import java.util.Date;

import org.bonitasoft.forklift.source.Source;



public class ArtefactPage extends ArtefactResource {


	public String name;
	public String version;
	public String description;
	public Date dateCreation = new Date();

	
	public ArtefactPage(String name, String version, String description, Date dateCreation, Source sourceOrigin) {
		super( "pages", name, version, description, dateCreation, sourceOrigin);
	}

	
	@Override
	public String getContentType()
	{ return "page"; };
	
	/*
	@Override
	public DeployOperation deploy(BonitaAccessor bonitaAccessor) {
		DeployOperation deployOperation = new DeployOperation();
		// 
		
		Page currentPage = null;
		try {
			currentPage = bonitaAccessor.pageAPI.getPageByName(getName());
			// getPageByName does not work : search manually

	
			if (currentPage != null) {
				bonitaAccessor.pageAPI.updatePageContent(currentPage.getId(), content.toByteArray());
			} else {
				Page page = bonitaAccessor.pageAPI.createPage(getName(), content.toByteArray());
			}
			deployOperation.deploymentStatus= DeploymentStatus.DEPLOYED;
		} catch(Exception e)
		{
			deployOperation.deploymentStatus= DeploymentStatus.DEPLOYEDFAILED;
			deployOperation.listEvents.add( new BEvent(EventErrorAtDeployment, e, "Page ["+getName()+"]"));
		}
		return deployOperation;
	}
	*/

}
