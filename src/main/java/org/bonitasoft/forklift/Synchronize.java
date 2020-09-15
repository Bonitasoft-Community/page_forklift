package org.bonitasoft.forklift;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.forklift.ForkliftAPI.ResultSynchronization;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.store.BonitaStore;
import org.bonitasoft.store.BonitaStore.UrlToDownload;
import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.BonitaStoreDirectory;
import org.bonitasoft.store.BonitaStoreParameters;
import org.bonitasoft.store.BonitaStoreResult;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.Artifact.TypeArtifact;
import org.bonitasoft.store.artifact.ArtifactBDM;
import org.bonitasoft.store.artifact.ArtifactCustomPage;
import org.bonitasoft.store.artifact.ArtifactLayout;
import org.bonitasoft.store.artifact.ArtifactLivingApplication;
import org.bonitasoft.store.artifact.ArtifactLookAndFeel;
import org.bonitasoft.store.artifact.ArtifactOrganization;
import org.bonitasoft.store.artifact.ArtifactProcess;
import org.bonitasoft.store.artifact.ArtifactProfile;
import org.bonitasoft.store.artifact.ArtifactRestApi;
import org.bonitasoft.store.artifact.ArtifactTheme;
import org.bonitasoft.store.artifact.FactoryArtifact.ArtifactResult;
import org.bonitasoft.store.artifactdeploy.DeployStrategy.Action;
import org.bonitasoft.store.artifactdeploy.DeployStrategy.DeployOperation;
import org.bonitasoft.store.artifactdeploy.DeployStrategy.DeploymentStatus;
import org.bonitasoft.store.artifactdeploy.DeployStrategy.DetectionStatus;
import org.bonitasoft.store.toolbox.LoggerStore;

public class Synchronize {

    private static final String CST_JSON_ACTION = "action";

    private static final String CST_JSON_VERSIONARTIFACT = "version";

    private static final String CST_JSON_NAMEARTIFACT = "name";

    private static final String CST_JSON_TYPEARTIFACT = "type";

    private final static BEvent eventDeploymentFailed = new BEvent(Synchronize.class.getName(), 1, Level.ERROR, "Error during Deployment", "An error occures at the deploiment", "Deployment can done partialy", "Check the exception");
    private final static BEvent eventCanMoveToArchive = new BEvent(Synchronize.class.getName(), 2, Level.APPLICATIONERROR, "Can't move to archived", "The artefact can't be move to the archive directory", "Artefact will be study a new time, but then mark as 'already loaded'",
            "Check the exception (access right ?)");

    private ConfigurationSet configurationSet;

    public void setConfiguration(ConfigurationSet configurationSet) {
        this.configurationSet = configurationSet;
    }

    /**
     * @param bonitaAccessor
     * @return
     */
    public ResultSynchronization detect(BonitaStoreAccessor bonitaAccessor) {
        ResultSynchronization resultSynchronization = new ResultSynchronization();
        Date dateBegin = new Date();
        resultSynchronization.addReportLine("Detection start at " + ForkliftAPI.sdf.format(dateBegin));

        Map<String, Long> numberPerContent = new HashMap<>();
        LoggerStore loggerStore = new LoggerStore();
        BonitaStoreParameters detectionParameters = configurationSet.getDetectionParameters();
        
        // run each source
        for (BonitaStore bonitaStore : configurationSet.listSources) {
            resultSynchronization.addReportLine("Source " + bonitaStore.getName());
            
            // open the store
            List<BEvent> listEvents = bonitaStore.begin(detectionParameters, loggerStore );
            resultSynchronization.addErrorsEvent(listEvents);
            if (BEventFactory.isError(listEvents))
                continue;
   
            BonitaStoreResult storeResult = bonitaStore.getListArtifacts(detectionParameters, loggerStore);

            orderArtefacts(storeResult);

            if (configurationSet.contentMap.size() == 0) {
                resultSynchronization.addReportLine(" Scope is empty, nothing will be managed");
            }

            for (ArtifactResult artifactResult : storeResult.listArtifacts) {
                // if the configuration allow this artifact ?
                if (!configurationSet.isContentAllow(artifactResult.artifact)) {
                    resultSynchronization.addReportLine("Artifact [" + artifactResult.artifact.getType() + " " + artifactResult.artifact.getName() + "] detected, but this type is not in the scope.");

                } else {
                    Long nb = numberPerContent.get( artifactResult.artifact.getType().toString().toLowerCase());
                    numberPerContent.put(artifactResult.artifact.getType().toString().toLowerCase(), nb == null ? 1L : nb + 1);

                    resultSynchronization.addReport("  Detect " + artifactResult.artifact.getType().toString() + " [" + artifactResult.artifact.getName()+"]; ");
                    DeployOperation deployOperation = artifactResult.artifact.detectDeployment(detectionParameters, bonitaAccessor, loggerStore);
                    artifactResult.logAnalysis.append( deployOperation.logAnalysis.toString());
                    
                    /** some artifact does not have any detection status, complete it */
                    if (deployOperation.detectionStatus == null) {
                        // create one analysis
                        if (deployOperation.presentDateArtifact == null) {
                            deployOperation.detectionStatus = DetectionStatus.NEWARTEFAC;
                            deployOperation.addAnalysisLine( "The artefact is new, deploy this version");
                        } else if (deployOperation.presentDateArtifact != null && deployOperation.presentDateArtifact.equals(artifactResult.artifact.getDate())) {
                            deployOperation.detectionStatus = DetectionStatus.SAME;
                            deployOperation.addAnalysisLine( "A version exist with the same date (" + ForkliftAPI.sdf.format(artifactResult.artifact.getDate()) + ")");
                        } else if (deployOperation.presentDateArtifact.before(artifactResult.artifact.getDate())) {
                            deployOperation.detectionStatus = DetectionStatus.NEWVERSION;
                            deployOperation.addAnalysisLine( "The version is new" );
                        } else {
                            deployOperation.detectionStatus = DetectionStatus.OLDVERSION;
                            deployOperation.addAnalysisLine( "The version is older, you should ignore this one");
                        }
                    }
                    resultSynchronization.addErrorsEvent(deployOperation.listEvents);
                    // Add the complete report line, analysis stay item per item
                    resultSynchronization.addReportLine(deployOperation.report.toString());
                    deployOperation.artifact = artifactResult.artifact; // to be sure
                    deployOperation.logAnalysis = artifactResult.logAnalysis;
                    
                    // calculate the appropriate decision
                    switch (deployOperation.detectionStatus) {
                        case NEWARTEFAC:
                        case NEWVERSION:
                            deployOperation.action = Action.DEPLOY;
                            break;
                        case OLDVERSION:
                        case SAME:
                            deployOperation.action = Action.DELETE;
                            break;
                        case DETECTIONFAILED:
                            deployOperation.action = Action.IGNORE;
                            break;
                        case UNDETERMINED:
                            deployOperation.action = Action.IGNORE;
                            break;
                    }

                    resultSynchronization.addDetection(deployOperation);
                }
            }
            resultSynchronization.addErrorsEvent(bonitaStore.end( detectionParameters, loggerStore ));

        } // end the store

        // report of the analysis
        boolean isNothingIsAllow = true;
        for (String typeId : configurationSet.getContentAllow().keySet()) {
            if (Boolean.TRUE.equals(configurationSet.getContentAllow().get(typeId))) {
                Long nb = numberPerContent.get(typeId.toLowerCase());

                resultSynchronization.addReportLine(typeId + " ..." + (nb == null ? "0" : nb));
                isNothingIsAllow = false;
            }
        }
        if (isNothingIsAllow)
            resultSynchronization.addReportLine("No content detection: check the configuration to detect one item as minimum");
        return resultSynchronization;

    }

    /**
     * Play the automatic synchronization now !
     * 
     * @param processAPI
     * @param identityAPI
     * @return
     */

    public ResultSynchronization start(BonitaStoreAccessor bonitaAccessor) {
        ResultSynchronization resultSynchronization = new ResultSynchronization();
        Date dateBegin = new Date();
        int countDetectArtefact = 0;

        int countDeployArtifactWithSuccess = 0;
        int countDeployArtifactFailed = 0;
        int countDeployArtifactIgnored = 0;
        try {
            resultSynchronization.addReportLine("Synchronisation start at " + ForkliftAPI.sdf.format(dateBegin));
            LoggerStore loggerStore = new LoggerStore();

            BonitaStoreParameters detectionParameters = configurationSet.getDetectionParameters();
            detectionParameters.withNotAvailable = true;
            // run each source
            for (BonitaStore bonitaStore : configurationSet.listSources) {
                resultSynchronization.addReportLine("Source " + bonitaStore.getName());
                
                // open the store
                List<BEvent> listEvents = bonitaStore.begin(detectionParameters, loggerStore );
                resultSynchronization.addErrorsEvent(listEvents);
                if (BEventFactory.isError(listEvents))
                    continue;
                
                
                BonitaStoreResult storeResult = bonitaStore.getListArtifacts(detectionParameters, loggerStore);

                orderArtefacts(storeResult);

                for (ArtifactResult artifactResult : storeResult.listArtifacts) {
                    countDetectArtefact++;

                    // is this artifact is part of the listAction ?
                    boolean allowDeployment = false;
                    boolean askSourceToRemoveArtifact = false;
                    if (configurationSet.listActions != null) {
                        for (Map<String, Object> actionMap : configurationSet.listActions) {
                            TypeArtifact typeMap = TypeArtifact.valueOf((String) actionMap.get(CST_JSON_TYPEARTIFACT));
                            if (artifactResult.artifact.getType().equals(typeMap)
                                    && isEquals(artifactResult.artifact.getName(), (String) actionMap.get(CST_JSON_NAMEARTIFACT))
                                    && isEquals(artifactResult.artifact.getVersion(), (String) actionMap.get(CST_JSON_VERSIONARTIFACT))) {
                                // we get the action to do
                                if (Action.DEPLOY.toString().equals(actionMap.get(CST_JSON_ACTION))) {
                                    allowDeployment = true;
                                    askSourceToRemoveArtifact = true;
                                }
                                if (Action.DELETE.toString().equals(actionMap.get(CST_JSON_ACTION)))
                                    askSourceToRemoveArtifact = true;

                            }
                        }
                    } else
                        allowDeployment = configurationSet.isContentAllow(artifactResult.artifact);

                    DeployOperation deployOperation = new DeployOperation();
                    deployOperation.artifact = artifactResult.artifact;

                    String logDeployment ="";
                    // if the configuration allow this artifact ?
                    if (allowDeployment) {
                        logDeployment += artifactResult.artifact.getType() + " " + artifactResult.artifact.getBonitaName() + " : ";
                        // load it ?
                        boolean continueOperation = true;
                        if (! artifactResult.artifact.isLoaded())
                        {
                            logDeployment += "loaded,";
                            BonitaStoreResult bonitaStoreResult = bonitaStore.loadArtifact(artifactResult.artifact, UrlToDownload.LASTRELEASE, loggerStore);
                            if (BEventFactory.isError(bonitaStoreResult.getEvents())) {
                                deployOperation.listEvents.addAll( bonitaStoreResult.getEvents() );
                                deployOperation.deploymentStatus = DeploymentStatus.LOADFAILED;
                                logDeployment += " failed.";
                                continueOperation = false;
                            }
                        }
                        if (continueOperation) {
                            logDeployment += "Deploy ";
                            DeployOperation deployOperationDeploy = artifactResult.artifact.deploy(detectionParameters, bonitaAccessor, loggerStore);
                            deployOperation.listEvents.addAll( deployOperationDeploy.listEvents );
                            deployOperation.deploymentStatus = deployOperationDeploy.deploymentStatus;
                            logDeployment += deployOperationDeploy.report;

                        }
                        // decision can be updated now
                        askSourceToRemoveArtifact = false;

                        switch (deployOperation.deploymentStatus) {
                            case REMOVEFAIL:
                                logDeployment += "ERROR: Remove current failed";
                                countDeployArtifactFailed++;
                                break;
                            case NOTHINGDONE:
                                logDeployment += "INFO: Nothing is done";
                                countDeployArtifactFailed++;
                                break;
                            case NEWALREADYINPLACE:
                                logDeployment += "INFO: A new artefact is already in place";
                                askSourceToRemoveArtifact = true;
                                countDeployArtifactIgnored++;
                                break;
                            case LOADFAILED:
                                logDeployment += "ERROR: Load the content of the artefact failed";
                                countDeployArtifactFailed++;
                                break;
                            case DEPLOYEDFAILED:
                                logDeployment += "ERROR: Deployment failed";
                                countDeployArtifactFailed++;
                                break;
                            case LOADED:
                                logDeployment += "LOADED: Deployment is done, but artifact is not ENABLE";
                                askSourceToRemoveArtifact = true;
                                countDeployArtifactWithSuccess++;
                                break;
                            case DEPLOYED:
                                logDeployment += "SUCCESS: Deployment is done";
                                askSourceToRemoveArtifact = true;
                                countDeployArtifactWithSuccess++;
                                break;
                        }

                        // do not add the event here, result will be available object per object resultSynchronization.addErrorsEvent(deployOperation.listEvents);
                    }
                    if (askSourceToRemoveArtifact) {
                        if (bonitaStore instanceof BonitaStoreDirectory) {
                            // move to an archive directory
                            File sourcePath = ((BonitaStoreDirectory)bonitaStore).getDirectory();
                            File destinationPath = new File( sourcePath.getAbsoluteFile()+"/archive" );
                            try {
                                
                                TypesCast.moveFile( artifactResult.artifact.getFileName(), sourcePath, destinationPath, true);
                                logDeployment+="Move to archive path;";
                            }catch(Exception e ) {
                                deployOperation.listEvents.add( new BEvent( eventCanMoveToArchive, "File["+artifactResult.artifact.getFileName()+"] move from["+sourcePath.getAbsolutePath()+"] to ["+destinationPath.getAbsolutePath()+"]"));
                            }
                        }
                        
                        if (deployOperation.deploymentStatus == null)
                            deployOperation.deploymentStatus = DeploymentStatus.DELETED;
                    }

                    resultSynchronization.addDetection(deployOperation);
                    
                    resultSynchronization.addReportLine(logDeployment);
                    artifactResult.artifact.clean(); // remove all content to limit the memory
                } // end listArtefact
                
                // end the store
                resultSynchronization.addErrorsEvent(bonitaStore.end( detectionParameters, loggerStore ));
            } // end source

        } catch (Exception e) {
            resultSynchronization.addErrorEvent(new BEvent(eventDeploymentFailed, e, ""));
        }
        Date dateEnd = new Date();
        resultSynchronization.addReport("Synchronisation End at " + ForkliftAPI.sdf.format(dateEnd) + " in " + (dateEnd.getTime() - dateBegin.getTime()) + " ms");
        resultSynchronization.addReport("  Detected artefacts      : " + countDetectArtefact);
        resultSynchronization.addReport("  Deployment with success : " + countDeployArtifactWithSuccess);
        resultSynchronization.addReport("  Deployment failed       : " + countDeployArtifactFailed);
        resultSynchronization.addReport("  Deployment ignored      : " + countDeployArtifactIgnored);

        resultSynchronization.addReport("Synchronisation End at " + ForkliftAPI.sdf.format(new Date()) + " in ");

        return resultSynchronization;
    }

    private boolean isEquals(String o1, Object o2) {
        if (o1 == null && o2 == null)
            return true;
        if (o1 != null && o2 != null)
            return o1.equals(o2);
        return false;
    }

    private void orderArtefacts(BonitaStoreResult storeResult) {
        final List<Class> listOrder = new ArrayList<Class>();
        listOrder.add(ArtifactLayout.class);
        listOrder.add(ArtifactTheme.class);
        listOrder.add(ArtifactLookAndFeel.class);

        listOrder.add(ArtifactBDM.class);

        listOrder.add(ArtifactOrganization.class);
        listOrder.add(ArtifactRestApi.class);
        listOrder.add(ArtifactCustomPage.class);
        listOrder.add(ArtifactProcess.class);
        listOrder.add(ArtifactProfile.class);

        listOrder.add(ArtifactLivingApplication.class);

        // Attention, deployment must be done in a certain order
        Collections.sort(storeResult.listArtifacts, new Comparator<ArtifactResult>() {

            public int compare(ArtifactResult s1,
                    ArtifactResult s2) {
                int rangeS1 = 0;
                int rangeS2 = 0;

                for (int i = 0; i < listOrder.size(); i++) {
                    if (listOrder.get(i).equals(s1.artifact.getClass()))
                        rangeS1 = i;
                    if (listOrder.get(i).equals(s2.artifact.getClass()))
                        rangeS2 = i;
                }
                if (rangeS1 != rangeS2)
                    return Integer.valueOf(rangeS1).compareTo(rangeS2);
                return s1.artifact.getName().compareTo(s2.artifact.getName());
            }
        });
    }

}
