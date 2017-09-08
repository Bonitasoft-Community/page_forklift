package org.bonitasoft.forklift.artefact;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.bonitasoft.engine.business.application.Application;
import org.bonitasoft.engine.business.application.ApplicationImportPolicy;
import org.bonitasoft.engine.business.application.ApplicationSearchDescriptor;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ImportException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.forklift.ForkliftAPI.BonitaAccessor;
import org.bonitasoft.forklift.source.Source;
import org.bonitasoft.log.event.BEvent;

public class ArtefactLivingApplication extends Artefact {

	private static Logger logger = Logger.getLogger(ArtefactResource.class.getName());

	public String name;
	public String version;
	public Date dateCreation;

	public ArtefactLivingApplication(String name, String version, String description, Date dateCreation, Source sourceOrigin) {
		super( "livingapp", name, version, description, dateCreation, sourceOrigin);
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
	public DeployOperation detectDeployment(BonitaAccessor bonitaAccessor)
	{
		DeployOperation deployOperation = new DeployOperation();
		try {
			Application application = searchByName(bonitaAccessor);
			if (application!=null)
			{
				deployOperation.presentDateArtefact = application.getLastUpdateDate();
				deployOperation.presentVersionArtefact = null;
		}
		} catch (SearchException e) {
			deployOperation.detectionStatus = DetectionStatus.DETECTIONFAILED;
			deployOperation.listEvents.add(new BEvent(EventErrorAtDetection, e, "Application [" + getName() + "]"));
	}
		return deployOperation;
	}
	
	
	@Override
	public DeployOperation deploy(BonitaAccessor bonitaAccessor) {
		DeployOperation deployOperation = new DeployOperation();
			
		try {
			Application application = searchByName(bonitaAccessor);
			if (application!=null)
				bonitaAccessor.applicationAPI.deleteApplication( application.getId() );
			
			bonitaAccessor.applicationAPI.importApplications(content.toByteArray(), ApplicationImportPolicy.FAIL_ON_DUPLICATES);
			deployOperation.deploymentStatus= DeploymentStatus.DEPLOYED;
		} catch (AlreadyExistsException e) {
			deployOperation.listEvents.add( new BEvent(EventErrorAtDeployment, e, "Application "+getName()));
			deployOperation.deploymentStatus= DeploymentStatus.DEPLOYEDFAILED;
			logger.severe("Forklift.ArtefactLivingApplication Error during deployement "+getName()+" : "+e.toString());
		} catch (ImportException e) {
			deployOperation.listEvents.add( new BEvent(EventErrorAtDeployment, e, "Application "+getName()));
			deployOperation.deploymentStatus= DeploymentStatus.DEPLOYEDFAILED;
			logger.severe("Forklift.ArtefactLivingApplication Error during deployement "+getName()+" : "+e.toString());
			} catch (DeletionException e) {
				deployOperation.listEvents.add( new BEvent(EventErrorAtDeployment, e, "Application "+getName()));
				deployOperation.deploymentStatus= DeploymentStatus.DEPLOYEDFAILED;
				logger.severe("Forklift.ArtefactLivingApplication Error during deployement "+getName()+" : "+e.toString());
		} catch (SearchException e) {
			deployOperation.listEvents.add( new BEvent(EventErrorAtDeployment, e, "Application "+getName()));
			deployOperation.deploymentStatus= DeploymentStatus.DEPLOYEDFAILED;
			logger.severe("Forklift.ArtefactLivingApplication Error during deployement "+getName()+" : "+e.toString());
			}
		
		
		return deployOperation;
	}
	
	public Application searchByName(BonitaAccessor bonitaAccessor) throws SearchException
	{
		SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0,10);
		searchOptionsBuilder.filter(ApplicationSearchDescriptor.DISPLAY_NAME, getName());
		SearchResult<Application> searchResultApplication = bonitaAccessor.applicationAPI.searchApplications(searchOptionsBuilder.done());
		for (final Application application : searchResultApplication.getResult()) 
			return application;
		return null;
	}
}
