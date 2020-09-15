package org.bonitasoft.forklift;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.store.BonitaStore;
import org.bonitasoft.store.BonitaStoreAPI;
import org.bonitasoft.store.BonitaStoreDirectory;
import org.bonitasoft.store.BonitaStoreFactory;
import org.bonitasoft.store.BonitaStoreParameters;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.Artifact.TypeArtifact;
import org.json.simple.JSONValue;

public class ConfigurationSet {
	public List<BonitaStore> listSources = new ArrayList<>();
	
	public Map<String, Boolean> contentMap;

	public List<BEvent> listEvents;

	public List<Map<String,Object>> listActions;
	
	@SuppressWarnings("unchecked")
    public void setActions(String jsonSt)
	{
	    if (jsonSt==null)
	        return;
		final Object jsonObject = JSONValue.parse(jsonSt);
		if (jsonObject instanceof List)
		{
			listActions = (List<Map<String, Object>>) jsonObject;
		}
	}
	
	public Object toJsonObject() {
		Map<String, Object> mapSet = new HashMap<>();

		List<Map<String, Object>> listMap = new ArrayList<>();
		for (BonitaStore store : listSources)
			listMap.add(store.toMap());
		mapSet.put(ForkliftAPI.cstConfigSources, listMap);

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

		mapSet.put(ForkliftAPI.cstConfigContent, contentMap);
		return mapSet;
	}

	/**
	 * This method is call from the configuration, oposite of the toJsonObject
	 * @param configMap
	 */
	@SuppressWarnings("unchecked")
    public void fromJsonObject(Map<String, Object> configMap) {
        List<Map<String,Object>> listSourceOb = (List<Map<String, Object>>) configMap.get(ForkliftAPI.cstConfigSources);
		BonitaStoreAPI bonitaStoreAPI = BonitaStoreAPI.getInstance(); 
		BonitaStoreFactory bonitaStoreFactory = bonitaStoreAPI.getBonitaStoreFactory();
		for (Map<String,Object> sourceMap : listSourceOb) {
		    BonitaStore store = bonitaStoreFactory.getBonitaStore(sourceMap);
		    if (store!=null)
		        listSources.add(store);
		}
		contentMap = (Map<String, Boolean>) configMap.get(ForkliftAPI.cstConfigContent);
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

	public final static String CST_JSON_TYPESOURCE= BonitaStore.CST_BONITA_STORE_TYPE;
	public final static String CST_JSON_TYPESOURCE_DIR= BonitaStoreDirectory.CST_TYPE_DIR;
	
    
	
	  
	/**
	 * administrator give the list of all artifact which can be
	 * synchronized.
	 * 
	 * @param artefact
	 * @return
	 */
	public boolean isContentAllow(Artifact artefact) {
		Boolean contentArtefact = TypesCast.getBoolean( contentMap.get( artefact.getType().toString().toLowerCase()), false);
		return (contentArtefact == null) ? false : contentArtefact;
	}
	public Map<String, Boolean> getContentAllow()
	{
		return contentMap;
	}

	private boolean isContentMap(String name ) {
	    return TypesCast.getBoolean(contentMap.get( name ), false);
	}
	public BonitaStoreParameters getDetectionParameters() {
	    BonitaStoreParameters detectionParameters = new BonitaStoreParameters();
	    detectionParameters.listTypeArtifacts = new ArrayList<>();
        if (isContentMap( "layout"))
            detectionParameters.listTypeArtifacts.add(TypeArtifact.LAYOUT);
        if (isContentMap( "theme"))
            detectionParameters.listTypeArtifacts.add(TypeArtifact.THEME);
        if (isContentMap( "lookandfeel"))
            detectionParameters.listTypeArtifacts.add(TypeArtifact.LOOKANDFEEL);
        if (isContentMap( "organization"))
            detectionParameters.listTypeArtifacts.add(TypeArtifact.ORGANIZATION);
        if (isContentMap( "restapi"))
            detectionParameters.listTypeArtifacts.add(TypeArtifact.RESTAPI);
        if (isContentMap( "custompage"))
            detectionParameters.listTypeArtifacts.add(TypeArtifact.CUSTOMPAGE);
        if (isContentMap( "process"))
            detectionParameters.listTypeArtifacts.add(TypeArtifact.PROCESS);
        if (isContentMap( "profile"))
            detectionParameters.listTypeArtifacts.add(TypeArtifact.PROFILE);
        if (isContentMap( "livingapp"))
            detectionParameters.listTypeArtifacts.add(TypeArtifact.LIVINGAPP);
        if (isContentMap( "bdm"))
            detectionParameters.listTypeArtifacts.add(TypeArtifact.BDM);
        
        detectionParameters.processEnable =isContentMap("processenable");
        detectionParameters.processManagerActor =isContentMap("processmanageractor");
        detectionParameters.processCategory =isContentMap("processcategory");
                
	    return detectionParameters;
	}
}