package org.bonitasoft.forklift.artefact;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveFactory;
import org.bonitasoft.engine.bpm.bar.InvalidBusinessArchiveFormatException;
import org.bonitasoft.engine.bpm.process.ActivationState;
import org.bonitasoft.engine.bpm.process.ProcessActivationException;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessDeployException;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessEnablementException;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.forklift.ForkliftAPI;
import org.bonitasoft.forklift.ForkliftAPI.BonitaAccessor;
import org.bonitasoft.forklift.source.Source;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

public class ArtefactProcess extends Artefact {

	
	private BusinessArchive businessArchive;

	private static BEvent EventErrorAtDeployment = new BEvent(ArtefactProcess.class.getName(), 1, Level.APPLICATIONERROR, "Error at deployment", "The Process can't be deployed", "Process is not accessible", "Check the exception");
	private static BEvent EventInvalidBarFile = new BEvent(ArtefactProcess.class.getName(), 2, Level.APPLICATIONERROR, "Invalid Bar file", "The bar file can't be read", "The artefact is ignored", "Check the exception");
	private static BEvent EventCantRemoveCurrentProcess = new BEvent(ArtefactProcess.class.getName(), 3, Level.APPLICATIONERROR, "Current process can't be removed", "To deploy the new process (same name, same version), current process has to be removed. The operation failed.", "Deployment of the new process is not possible", "Check the exception");
	private static BEvent EventErrorAtEnablement = new BEvent(ArtefactProcess.class.getName(), 4, Level.APPLICATIONERROR, "Error at Enablement", "The Process is deployment, but not enable", "Process can't be used", "Check the error in the administration part");


	public ArtefactProcess(String processName, String processVersion, String processDescription, Date dateProcess, Source sourceOrigin) {
		super( "process", processName, processVersion, processDescription, dateProcess, sourceOrigin);
	}

	

	/**
	 * load from the filefile
	 * 
	 * @param file
	 * @throws IOException
	 * @throws InvalidBusinessArchiveFormatException
	 */
	public List<BEvent> loadFromFile(File file) {
		List<BEvent> listEvents = new ArrayList<BEvent>();

		try {
			businessArchive = BusinessArchiveFactory.readBusinessArchive(file);
		} catch (Exception e) {
			listEvents.add(new BEvent(EventInvalidBarFile, e, file.getName()));
		}
		return listEvents;
	}
	
	

	
	/* *********************************************************************** */
	/*                                                                         */
	/* Deployement */
	/*                                                                         */
	/*                                                                         */
	/* *********************************************************************** */

	@Override
	public DeployOperation detectDeployment(BonitaAccessor bonitaAccessor) {
		DeployOperation deployOperation=new DeployOperation();
		Long processDefinitionId=null;
		try
		{
			processDefinitionId = bonitaAccessor.processAPI.getProcessDefinitionId(getName(), getVersion());
	
		} catch (ProcessDefinitionNotFoundException pe)
		{}
		
		if (processDefinitionId!=null)
		{
			try
			{
				ProcessDeploymentInfo processDeploymentInfo = bonitaAccessor.processAPI.getProcessDeploymentInfo(processDefinitionId);
				deployOperation.presentDateArtefact=processDeploymentInfo.getDeploymentDate();
				deployOperation.presentVersionArtefact= getVersion();
				if (processDeploymentInfo.getDeploymentDate().equals(getDate()))
				{
					deployOperation.detectionStatus= DetectionStatus.SAME;
					deployOperation.report="A version exist with the same date ("+ForkliftAPI.sdf.format(getDate())+")";
				}
				else if (processDeploymentInfo.getDeploymentDate().before(getDate()))
				{
					deployOperation.detectionStatus= DetectionStatus.NEWVERSION;
					deployOperation.report="The version is new";
				}
				else
				{
					deployOperation.detectionStatus= DetectionStatus.OLDVERSION;
					deployOperation.report="The version is older, you should ignore this process";
				}
				return deployOperation;
			} catch  (ProcessDefinitionNotFoundException pe)
			{
				// this one is not normal : engine just get back the processDefinitionId
			}
		}
		// is the process exist ? 
		try
		{
			processDefinitionId =  bonitaAccessor.processAPI.getLatestProcessDefinitionId(getName());
			ProcessDeploymentInfo processDeploymentInfo = bonitaAccessor.processAPI.getProcessDeploymentInfo(processDefinitionId);
			deployOperation.presentDateArtefact = processDeploymentInfo.getDeploymentDate();
			deployOperation.presentVersionArtefact= processDeploymentInfo.getVersion();

			// event if the same process was deployement AFTER, we considere this artefact as a NEWVERSION
			deployOperation.detectionStatus= DetectionStatus.NEWVERSION;
			deployOperation.report="The process with this version does not exist, deploy this version";

		} catch (ProcessDefinitionNotFoundException pe)
		{
			deployOperation.detectionStatus= DetectionStatus.NEWARTEFAC;
			deployOperation.report="This process is completely new";

		}
		return deployOperation;
	}

	/**
	 * 
	 */
	@Override
	public DeployOperation deploy(BonitaAccessor bonitaAccessor) {
		DeployOperation deployOperation=new DeployOperation();
		deployOperation.deploymentStatus = DeploymentStatus.NOTHINGDONE;

		// artefact is the process
		boolean doLoad = false;

		try {
			Long processDefinitionId = bonitaAccessor.processAPI.getProcessDefinitionId(getName(), getVersion());
			ProcessDeploymentInfo processDeploymentInfo = bonitaAccessor.processAPI.getProcessDeploymentInfo(processDefinitionId);

			if (processDeploymentInfo.getActivationState()==ActivationState.ENABLED) 
				bonitaAccessor.processAPI.disableProcess(processDefinitionId);
			
			bonitaAccessor.processAPI.deleteProcessDefinition(processDefinitionId);
			doLoad = true;

		} catch (ProcessDefinitionNotFoundException e) {
			doLoad = true; // this not exist
		} catch (DeletionException e) {
			deployOperation.listEvents.add( new BEvent(EventCantRemoveCurrentProcess,e, "Process Name["+getName()+"] Version["+getVersion()+"]") );
			doLoad = false;
		} catch (ProcessActivationException e) {
			deployOperation.listEvents.add( new BEvent(EventCantRemoveCurrentProcess,e, "Process Name["+getName()+"] Version["+getVersion()+"]") );
			doLoad = false;
		}

		if (doLoad) {
			
			// deploy it
			try {
				// bonitaAccessor.processAPI.deployAndEnableProcess(businessArchive);
				ProcessDefinition processDefinition=bonitaAccessor.processAPI.deploy(businessArchive);
				bonitaAccessor.processAPI.enableProcess( processDefinition.getId());
				
				deployOperation.deploymentStatus = DeploymentStatus.DEPLOYED;

			} catch (ProcessDeployException e) {
				deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
				deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, e, "Process " + getName() + "/" + getVersion()));
			} catch (ProcessDefinitionNotFoundException e) {
				deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
				deployOperation.listEvents.add(new BEvent(EventErrorAtEnablement, e, "Process " + getName() + "/" + getVersion()));
		} catch (ProcessEnablementException e) {
				deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
				deployOperation.listEvents.add(new BEvent(EventErrorAtEnablement, e, "Process " + getName() + "/" + getVersion()));
			} catch (AlreadyExistsException e) {
				deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
				deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, e, "Process " + getName() + "/" + getVersion()));
			}
		}
		return deployOperation;
	}

}
