package org.bonitasoft.forklift;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.ext.properties.BonitaProperties;
import org.bonitasoft.forklift.artefact.Artefact;
import org.bonitasoft.forklift.artefact.Artefact.DeployOperation;
import org.bonitasoft.forklift.artefact.ArtefactProcess;
import org.bonitasoft.forklift.source.SourceDirectory;
import org.json.simple.JSONValue;

import org.bonitasoft.forklift.source.Source;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;

public class ForkliftAPI {

	private static Logger logger = Logger.getLogger(ForkliftAPI.class.getName());
	public static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:MM:ss");
	
	private static BEvent EventBadPropertiesStructure = new BEvent(ForkliftAPI.class.getName(), 1, Level.APPLICATIONERROR, "Bad structure", "The variable store in the properties should be a LIST", "The load failed", "Set a new list and click on SAVE to update the current information");
	private static BEvent EventSourceSaved = new BEvent(ForkliftAPI.class.getName(), 2, Level.SUCCESS, "Sources saved", "Sources are saved on the server");

	public static ForkliftAPI getInstance() {
		return new ForkliftAPI();
	}

	/* *********************************************************************** */
	/*                                                                         */
	/* Synchronisation */
	/*                                                                         */
	/*                                                                         */
	/* *********************************************************************** */
	
	/**
	 * the apiAccessor is not available everytime in the same way : build a new
	 * one
	 *
	 */
	public static class BonitaAccessor {
		public APISession apiSession;
		public ProcessAPI processAPI;
		public IdentityAPI identityAPI;
		public ProfileAPI profileAPI;
		
		public ProfileAPI getProfileAPI()
		{
			try
			{
				ProfileAPI profile=null;
				Class<?> clazz = Class.forName("com.bonitasoft.engine.api.TenantAPIAccessor");
				if (clazz!=null)
				{
					Method method = clazz.getMethod("getProfileAPI", APISession.class);
					profile = (ProfileAPI) method.invoke(null, apiSession);
				}				
				else
					profile=TenantAPIAccessor.getProfileAPI(apiSession);
				return profile;
			}catch (Exception e)
			{
				return null;
			}
			
		}
	}

	public static class ResultSynchronization {
		private List<BEvent> listEvents  =new ArrayList<BEvent>();
		private StringBuffer report= new StringBuffer();

		private List<DeployOperation> listDeployAnalysis  =new ArrayList<DeployOperation>();

		public void addReport(String lineReport) {
			report.append( lineReport).append( "\n");
		}
		public void addErrorEvent( BEvent errorEvent )
		{
			listEvents.add( errorEvent);
			addReport("Error "+errorEvent.toString() );
		}
		public void addErrorsEvent( List<BEvent> listErrorsEvent )
		{
			for (BEvent errorEvent : listErrorsEvent)
				addErrorEvent(errorEvent);
		}
		public void addDetection( DeployOperation deployAnalysis)
		{
			listDeployAnalysis.add(deployAnalysis);
		}
	
		public List<BEvent> getEvents()
		{ return listEvents;};
		public String getReport ()
		{ return report.toString(); }
		public Object toJsonObject()
		{
			Map<String,Object> mapJson = new HashMap<String,Object>();
			mapJson.put("report", report.toString());				

			List<Map<String,Object>> listDeployItem = new ArrayList<Map<String,Object>>();
			mapJson.put("items",listDeployItem);
			for (DeployOperation deployAnalysis : listDeployAnalysis )
			{
				Map<String,Object> mapDeployItem = new HashMap<String,Object>();
				listDeployItem.add(mapDeployItem);
				mapDeployItem.put("type", deployAnalysis.artefact.getTypeId());
				mapDeployItem.put("name", deployAnalysis.artefact.getName());
				mapDeployItem.put("version", deployAnalysis.artefact.getVersion());
				mapDeployItem.put("date", sdf.format( deployAnalysis.artefact.getDate()));
				if (deployAnalysis.deploymentStatus!=null)
					mapDeployItem.put("deploystatus", deployAnalysis.deploymentStatus.toString());
				if (deployAnalysis.detectionStatus!=null)
					mapDeployItem.put("detectionstatus", deployAnalysis.detectionStatus.toString());
				
				if (deployAnalysis.action!=null)
					mapDeployItem.put("action", deployAnalysis.action.toString());
				mapDeployItem.put("report", deployAnalysis.report);				
				mapDeployItem.put("presentversion", deployAnalysis.presentVersionArtefact);
				mapDeployItem.put("presentdate", (deployAnalysis.presentDateArtefact==null) ? null : sdf.format( deployAnalysis.presentDateArtefact));
				if (deployAnalysis.listEvents!=null)
					mapDeployItem.put("listevents", BEventFactory.getHtml(deployAnalysis.listEvents) );
					
			}
			return mapJson;
		}
	}

	
	public ResultSynchronization detect(ConfigurationSet configurationSet, BonitaAccessor bonitaAccessor) {
		Synchronize synchronize = new Synchronize();
		synchronize.setConfiguration(configurationSet);
		return synchronize.detect(bonitaAccessor);

	}
	
	public ResultSynchronization synchronize(ConfigurationSet configurationSet, BonitaAccessor bonitaAccessor) {
		Synchronize synchronize = new Synchronize();
		synchronize.setConfiguration(configurationSet);
		return synchronize.start(bonitaAccessor);

	}

	/**
	 * ForkLiftParam
	 */
	public static class ForkliftParam {

		int searchMaxResources = 1000;
		List<Map<String, Object>> sources;
		Map<String, Object> options;

		/**
		 * @param jsonSt
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public static ForkliftParam getInstanceFromJsonSt(final String jsonSt) {
			if (jsonSt == null) {
				return new ForkliftParam();
			}
			final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);
			if (jsonHash == null) {
				return new ForkliftParam();
			}
			final ForkliftParam forkliftParam = new ForkliftParam();
			forkliftParam.sources = (List<Map<String, Object>>) jsonHash.get("sources");
			forkliftParam.options = (Map<String, Object>) jsonHash.get("options");

			return forkliftParam;
		}
	}

	/*
	 * *************************************************************************
	 * **
	 */
	/*                                                                             */
	/* Save / load sources */
	/*                                                                             */
	/*
	 * *************************************************************************
	 * **
	 */
	private final static String cstConfigSources = "sources";
	private final static String cstConfigContent = "content";

	public static class ConfigurationSet {
		public List<Source> listSources = new ArrayList<Source>();
		
		public Map<String, Boolean> contentMap;

		public List<BEvent> listEvents;

		public List<Map<String,Object>> listActions;
		
		public void setActions(String jsonSt)
		{
			final Object jsonObject = JSONValue.parse(jsonSt);
			if (jsonObject instanceof List)
			{
				listActions = (List) jsonObject;
			}
		}
		
		public Object toJsonObject() {
			Map<String, Object> mapSet = new HashMap<String, Object>();

			List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>();
			for (Source source : listSources)
				listMap.add(source.toMap());
			mapSet.put(cstConfigSources, listMap);

			/*
			 * Map<String,Object> contentMap = new HashMap<String,Object>();
			 * contentMap.put("organization", contentOrganization);
			 * contentMap.put("layout", contentLayout);
			 * contentMap.put("theme",contentTheme);
			 * contentMap.put("pages",contentPages);
			 * contentMap.put("restapi",contentRestapi);
			 * contentMap.put("profile",contentProfile);
			 * contentMap.put("livingapp",contentLivingapp);
			 * contentMap.put("bdm",contentBdm);
			 * contentMap.put("process",contentProcess);
			 */

			mapSet.put(cstConfigContent, contentMap);
			return mapSet;
		}

		public void fromJsonObject(Map<String, Object> configMap) {
			List listSourceOb = (List) configMap.get(cstConfigSources);
			for (Object sourceOb : (List) listSourceOb) {
				Source source = Source.getInstance((Map<String, Object>) sourceOb);
				listSources.add(source);
			}
			contentMap = (Map) configMap.get(cstConfigContent);
			/*
			 * contentOrganization = (Boolean) contentMap.get("organization");
			 * contentLayout = (Boolean) contentMap.get("layout"); contentTheme=
			 * (Boolean) contentMap.get("theme"); contentPages= (Boolean)
			 * contentMap.get("pages"); contentRestapi= (Boolean)
			 * contentMap.get("restapi"); contentProfile= (Boolean)
			 * contentMap.get("profile"); contentLivingapp= (Boolean)
			 * contentMap.get("livingapp"); contentBdm = (Boolean)
			 * contentMap.get("bdm"); contentProcess=(Boolean)
			 * contentMap.get("process");
			 */
		}

		/**
		 * administrator give the list of all artefact which can be
		 * synchronized.
		 * 
		 * @param artefact
		 * @return
		 */
		public boolean isContentAllow(Artefact artefact) {
			Boolean contentArtefact = (Boolean) contentMap.get(artefact.getTypeId());
			return (contentArtefact == null) ? false : contentArtefact;
		}
		public Map<String, Boolean> getContentAllow()
		{
			return contentMap;
		}

	}

	private static String cstPropertiesSource = "sources";

	public ConfigurationSet loadConfiguration(String name, String pageName, long tenantId) {
		logger.info("ForkliftAPI.loadSource ~~~~~~~~");

		ConfigurationSet configurationSet = new ConfigurationSet();
		BonitaProperties bonitaProperties = new BonitaProperties(pageName, tenantId);

		configurationSet.listEvents = bonitaProperties.load();
		if (BEventFactory.isError(configurationSet.listEvents)) {
			logger.info("ForkliftAPI.loadSource  Error load properties :" + configurationSet.listEvents);
			return configurationSet;
		}
		// get the information
		String configSt = bonitaProperties.getProperty(cstPropertiesSource + name);
		try {
			Object configOb = (configSt != null && configSt.length() > 0) ? JSONValue.parse(configSt) : null;
			if (configOb == null)
				return configurationSet; // empty
			Map<String, Object> configMap = (Map) configOb;
			configurationSet.fromJsonObject(configMap);
		} catch (Exception e) {
			logger.info("ForkliftAPI.loadSource listSource is not a MAP [" + configSt + "] class=" + configSt.getClass().getName());
			configurationSet.listEvents.add(EventBadPropertiesStructure);
		}

		logger.info("ForkliftAPI.loadSource END ~~~~~~~~ found [" + configurationSet.listSources.size() + "] values");

		return configurationSet;
	}

	/**
	 * 
	 * @param name
	 * @param listSources
	 * @param pageName
	 * @param tenantId
	 * @return
	 */
	public List<BEvent> saveConfiguration(String name, ConfigurationSet configurationSet, String pageName, long tenantId) {
		logger.info("ForkliftAPI.saveSource ~~~~~~~~");
		BonitaProperties bonitaProperties = new BonitaProperties(pageName, tenantId);

		List<BEvent> listEvents = bonitaProperties.load();
		if (BEventFactory.isError(listEvents)) {
			logger.info("ForkliftAPI.saveSource , Error load properties :" + listEvents);
			return listEvents;
		}
		// save all informations

		String configSt = JSONValue.toJSONString(configurationSet.toJsonObject());

		bonitaProperties.setProperty(cstPropertiesSource + name, configSt);
		listEvents.addAll(bonitaProperties.store());
		if (!BEventFactory.isError(listEvents))
			listEvents.add(EventSourceSaved);

		logger.info("ForkliftAPI.saveSource , result :" + listEvents);

		return listEvents;
	}

}
