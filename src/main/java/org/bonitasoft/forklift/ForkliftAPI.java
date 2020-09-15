package org.bonitasoft.forklift;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.properties.BonitaProperties;
import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.artifactdeploy.DeployStrategy.DeployOperation;
import org.json.simple.JSONValue;

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

    public static class ResultSynchronization {

        private List<BEvent> listEvents = new ArrayList<>();
        private StringBuffer report = new StringBuffer();

        private List<DeployOperation> listDeployAnalysis = new ArrayList<>();

        public void addReport(String lineReport) {
            report.append(lineReport);
        }

        public void addReportLine(String lineReport) {
            report.append(lineReport).append("\n");
        }

        public void addErrorEvent(BEvent errorEvent) {
            listEvents.add(errorEvent);
            addReport("Error " + errorEvent.toString());
        }

        public void addErrorsEvent(List<BEvent> listErrorsEvent) {
            for (BEvent errorEvent : listErrorsEvent)
                addErrorEvent(errorEvent);
        }

        public void addDetection(DeployOperation deployAnalysis) {
            listDeployAnalysis.add(deployAnalysis);
        }

        public List<BEvent> getEvents() {
            return listEvents;
        };

        public String getReport() {
            return report.toString();
        }

        public Object toJsonObject() {
            Map<String, Object> mapJson = new HashMap<>();
            mapJson.put("report", report.toString());

            List<Map<String, Object>> listDeployItem = new ArrayList<>();
            mapJson.put("items", listDeployItem);
            for (DeployOperation deployAnalysis : listDeployAnalysis) {
                Map<String, Object> mapDeployItem = new HashMap<>();
                listDeployItem.add(mapDeployItem);
                mapDeployItem.put("type", deployAnalysis.artifact.getType().toString());
                mapDeployItem.put("name", deployAnalysis.artifact.getName());
                mapDeployItem.put("bonitaname", deployAnalysis.artifact.getBonitaName());
                mapDeployItem.put("bonitastore", deployAnalysis.artifact.getStore().getName());
                String displayName = deployAnalysis.artifact.getStore().getDisplayName();
                if (displayName == null || displayName.trim().isEmpty())
                    displayName = deployAnalysis.artifact.getStore().getName();
                mapDeployItem.put("bonitastoredisplayname", displayName);

                mapDeployItem.put("version", deployAnalysis.artifact.getVersion());
                mapDeployItem.put("date", sdf.format(deployAnalysis.artifact.getDate()));
                if (deployAnalysis.deploymentStatus != null)
                    mapDeployItem.put("deploystatus", deployAnalysis.deploymentStatus.toString());
                if (deployAnalysis.detectionStatus != null)
                    mapDeployItem.put("detectionstatus", deployAnalysis.detectionStatus.toString());

                if (deployAnalysis.action != null)
                    mapDeployItem.put("action", deployAnalysis.action.toString());
                mapDeployItem.put("analysis", deployAnalysis.analysis.toString());
                mapDeployItem.put("loganalysis", deployAnalysis.logAnalysis.toString());

                mapDeployItem.put("presentversion", deployAnalysis.presentVersionArtifact);
                mapDeployItem.put("presentdate", (deployAnalysis.presentDateArtifact == null) ? null : sdf.format(deployAnalysis.presentDateArtifact));
                if (deployAnalysis.listEvents != null && !deployAnalysis.listEvents.isEmpty())
                    mapDeployItem.put("listevents", BEventFactory.getSyntheticHtml(deployAnalysis.listEvents));

            }
            return mapJson;
        }
    }

    public ResultSynchronization detect(ConfigurationSet configurationSet, BonitaStoreAccessor bonitaAccessor) {
        Synchronize synchronize = new Synchronize();
        synchronize.setConfiguration(configurationSet);
        return synchronize.detect(bonitaAccessor);

    }

    public ResultSynchronization synchronize(ConfigurationSet configurationSet, BonitaStoreAccessor bonitaAccessor) {
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
    final static String cstConfigSources = "sources";
    final static String cstConfigContent = "content";

    private static String cstPropertiesSource = "sources";

    /**
     * @param name
     * @param pageName
     * @param tenantId
     * @return
     */
    public ConfigurationSet init(String name, String pageName, long tenantId) {
        return loadConfiguration(name, pageName, tenantId, true);
    }

    /**
     * @param name
     * @param pageName
     * @param tenantId
     * @return
     */
    public ConfigurationSet loadConfiguration(String name, String pageName, long tenantId) {
        return loadConfiguration(name, pageName, tenantId, false);
    }

    /**
     * Internal
     * 
     * @param name
     * @param pageName
     * @param tenantId
     * @param checkDatabase
     * @return
     */
    private ConfigurationSet loadConfiguration(String name, String pageName, long tenantId, boolean checkDatabase) {
        logger.info("ForkliftAPI.loadSource ~~~~~~~~");

        ConfigurationSet configurationSet = new ConfigurationSet();
        BonitaProperties bonitaProperties = new BonitaProperties(pageName, tenantId);
        bonitaProperties.setCheckDatabase(checkDatabase);

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
            Map<String, Object> configMap = (Map<String, Object>) configOb;
            configurationSet.fromJsonObject(configMap);
        } catch (Exception e) {
            logger.info("ForkliftAPI.loadSource listSource is not a MAP [" + configSt + "] class=" + configSt.getClass().getName());
            configurationSet.listEvents.add(EventBadPropertiesStructure);
        }

        logger.info("ForkliftAPI.loadSource END ~~~~~~~~ found [" + configurationSet.listSources.size() + "] values");

        return configurationSet;
    }

    /**
     * @param name
     * @param listSources
     * @param pageName
     * @param tenantId
     * @return
     */
    public List<BEvent> saveConfiguration(String name, ConfigurationSet configurationSet, String pageName, long tenantId) {
        logger.info("ForkliftAPI.saveSource ~~~~~~~~");
        BonitaProperties bonitaProperties = new BonitaProperties(pageName, tenantId);
        bonitaProperties.setCheckDatabase(false);

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
