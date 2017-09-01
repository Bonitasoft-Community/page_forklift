package org.bonitasoft.forklift.artefact;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.profile.ImportPolicy;
import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.forklift.ForkliftAPI;
import org.bonitasoft.forklift.ForkliftAPI.BonitaAccessor;
import org.bonitasoft.forklift.ForkliftAPI.ResultSynchronization;
import org.bonitasoft.forklift.artefact.Artefact.DeployOperation;
import org.bonitasoft.forklift.artefact.Artefact.DeploymentStatus;
import org.bonitasoft.forklift.artefact.Artefact.DetectionStatus;
import org.bonitasoft.forklift.source.Source;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;

public class ArtefactProfile extends Artefact {

	private static BEvent EventErrorAtload = new BEvent(ArtefactProfile.class.getName(), 1, Level.APPLICATIONERROR, "Can't load profile file", "The profile can't be read", "profile is not accessible", "Check the exception");
	private static BEvent EventDetectionFailed = new BEvent(ArtefactProfile.class.getName(), 2, Level.ERROR, "Detection failed", "The list of profile can't be read", "profile will not be deployed", "Check the exception");
	private static BEvent EventErrorAtDeployment = new BEvent(ArtefactProfile.class.getName(), 3, Level.APPLICATIONERROR, "Can't load profile file", "The profile can't be read", "profile is not accessible", "Check the exception");

	public String name;
	public String version;
	public Date dateCreation;

	byte[] profileContent = null; 
	public ArtefactProfile(String profileName, String profileVersion, String description, Date dateProfile, Source sourceOrigin) {
		super( "profile", profileName, profileVersion, description, dateProfile, sourceOrigin);
		this.name = profileName;
		this.version = profileVersion;
	}

	public List<BEvent> loadFromFile(File file) {
		List<BEvent> listEvents = new ArrayList<BEvent>();

		// the file is an XML, and should contains on the second line the structure
		FileInputStream fileContent = null;
		try {
			fileContent = new FileInputStream( file );
			profileContent = IOUtils.toByteArray(fileContent) ;
		} catch (Exception e) {
			listEvents.add(new BEvent(EventErrorAtload, e, file.getName()));
		}
		finally
		{
			if (fileContent!=null)
				try {
					fileContent.close();
				} catch (IOException e) {
				}
		}
		return new ArrayList<BEvent>();
	}

	@Override
	public DeployOperation detectDeployment(BonitaAccessor bonitaAccessor)
	{
		DeployOperation deployOperation = new DeployOperation();
		try
		{
		SearchResult<Profile> searchProfile = bonitaAccessor.profileAPI.searchProfiles( new SearchOptionsBuilder(0,1000).done());
		for (Profile profile : searchProfile.getResult())
		{
			if (profile.getName().equals( getName()))
			{
				deployOperation.presentDateArtefact= profile.getLastUpdateDate();
				
				if (profile.getLastUpdateDate().equals( getDate() ))
				{
					deployOperation.detectionStatus= DetectionStatus.SAME;
					deployOperation.report="the profile exist with the same date ("+ForkliftAPI.sdf.format(getDate())+")";

				}
				else if (profile.getLastUpdateDate().before( getDate() ))
				{
					deployOperation.detectionStatus= DetectionStatus.NEWVERSION;
					deployOperation.report="The profile has a newest date";
					
				}
				else 
				{
					deployOperation.detectionStatus= DetectionStatus.OLDVERSION;
					deployOperation.report="The profile on the server is newest";
				}
				return deployOperation;
			}
		}
		deployOperation.report="This profile is new";
		deployOperation.detectionStatus= DetectionStatus.NEWARTEFAC;
		}
		catch( Exception e)
		{
			deployOperation.listEvents.add( new BEvent(EventDetectionFailed, e, "Profile name["+name+"]" ));
			deployOperation.detectionStatus=DetectionStatus.DETECTIONFAILED;
		}
		return deployOperation;
	}
	
	
	@Override
	public DeployOperation deploy(BonitaAccessor bonitaAccessor) {
		DeployOperation deployOperation = new DeployOperation();
		deployOperation.deploymentStatus= DeploymentStatus.NOTHINGDONE;
		
		
		// game here : we just include the ORG import, but this fonction is a COM function
		try
		{
			// ok, please give me the correct profileAPI (expected the com here)
			ProfileAPI profileAPI= bonitaAccessor.getProfileAPI();

			
			// get the policy : com.bonitasoft.engine.profile.ImportPolicy.REPLACE_DUPLICATES;
			 Class<?> clImportPolicy = Class.forName("com.bonitasoft.engine.profile.ImportPolicy");
			 if (clImportPolicy==null)
			 {
					deployOperation.listEvents.add( new BEvent(EventErrorAtDeployment, "Profile name["+name+"] - Only Subscription edition" ));
					deployOperation.deploymentStatus= DeploymentStatus.BADBONITAVERSION;
					return deployOperation;
			 }
			 Object[] params= new Object[] { String.class };
			 Method methodValueOf = clImportPolicy.getMethod("valueOf",String.class);
			 Object importPolicy =  methodValueOf.invoke(null,"REPLACE_DUPLICATES" );
			  
			
			params= new Object[] { profileContent, importPolicy};

			// this methode can't be join..
			// java.lang.reflect.Method method = profileAPI.getClass().getMethod("importProfiles", byte[].class, clImportPolicy.getClass() );
			// so let search it
			Method[] listMethods =	profileAPI.getClass().getMethods();
			
			// String methods="";
			for (Method method : listMethods)
			{
				// methods+=method.getName()+";";
				if (method.getName().equals("importProfiles"))
				{
					/* Class[] listParam = method.getParameterTypes();
					for (Class oneParam :listParam)
						methods+="("+oneParam.getName()+")";
					*/
					method.invoke(profileAPI, params);
					deployOperation.deploymentStatus = DeploymentStatus.DEPLOYED;
				}
			}
			
			if (deployOperation.deploymentStatus== DeploymentStatus.NOTHINGDONE)
			{
				deployOperation.listEvents.add( new BEvent(EventErrorAtDeployment, "Profile name["+name+"] - Only Subscription edition" ));
				deployOperation.deploymentStatus= DeploymentStatus.BADBONITAVERSION;

			}
		}
		catch( Exception e)
		{
			deployOperation.listEvents.add( new BEvent(EventErrorAtDeployment, e, "Profile name["+name+"]" ));
			deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
		}
		
		return deployOperation;
	}
}
