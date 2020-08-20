package org.bonitasoft.forklift;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.forklift.ForkliftAPI.ConfigurationSet;
import org.bonitasoft.forklift.ForkliftAPI.ResultSynchronization;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.store.BonitaStore;
import org.bonitasoft.store.BonitaStore.DetectionParameters;
import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.BonitaStoreDirectory;
import org.bonitasoft.store.BonitaStoreResult;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.Artifact.TypeArtifact;
import org.bonitasoft.store.artifactdeploy.DeployStrategy.Action;
import org.bonitasoft.store.artifactdeploy.DeployStrategy.DeployOperation;
import org.bonitasoft.store.artifactdeploy.DeployStrategy.DeploymentStatus;
import org.bonitasoft.store.artifactdeploy.DeployStrategy.DetectionStatus;
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
import org.bonitasoft.store.toolbox.LoggerStore;

public class Synchronize {

    private static BEvent EventDeploymentFailed = new BEvent(Synchronize.class.getName(), 1, Level.ERROR, "Error during Deployment", "An error occures at the deploiment", "Deployment can done partialy", "Check the exception");

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
        DetectionParameters detectionParameters = new DetectionParameters();
        // run each source
        for (BonitaStore source : configurationSet.listSources) {
            resultSynchronization.addReportLine("Source " + source.getName());
            BonitaStoreResult listArtifacts = source.getListArtifacts(detectionParameters, loggerStore);

            orderArtefacts(listArtifacts);

            if (configurationSet.contentMap.size() == 0) {
                resultSynchronization.addReportLine(" Scope is empty, nothing will be managed");
            }

            for (Artifact artifact : listArtifacts.listArtifacts) {
                // if the configuration allow this artifact ?
                if (!configurationSet.isContentAllow(artifact)) {
                    resultSynchronization.addReportLine("Artifact [" + artifact.getType() + " " + artifact.getName() + "] detected, but this type is not in the scope.");

                } else {
                    Long nb = numberPerContent.get(artifact.getType().toString().toLowerCase());
                    numberPerContent.put(artifact.getType().toString().toLowerCase(), nb == null ? 1L : nb + 1);

                    resultSynchronization.addReport("  Detect " + artifact.getType().toString() + " " + artifact.getName()+"; ");
                    DeployOperation deployOperation = artifact.detectDeployment(bonitaAccessor, loggerStore);
                    if (deployOperation.detectionStatus == null) {
                        // create one analysis
                        if (deployOperation.presentDateArtifact == null) {
                            deployOperation.detectionStatus = DetectionStatus.NEWARTEFAC;
                            deployOperation.report = "The artefact is new, deploy this version";
                        } else if (deployOperation.presentDateArtifact != null && deployOperation.presentDateArtifact.equals(artifact.getDate())) {
                            deployOperation.detectionStatus = DetectionStatus.SAME;
                            deployOperation.report = "A version exist with the same date (" + ForkliftAPI.sdf.format(artifact.getDate()) + ")";
                        } else if (deployOperation.presentDateArtifact.before(artifact.getDate())) {
                            deployOperation.detectionStatus = DetectionStatus.NEWVERSION;
                            deployOperation.report = "The version is new";
                        } else {
                            deployOperation.detectionStatus = DetectionStatus.OLDVERSION;
                            deployOperation.report = "The version is older, you should ignore this one";
                        }
                    }
                    resultSynchronization.addErrorsEvent(deployOperation.listEvents);
                    resultSynchronization.addReportLine(deployOperation.report);
                    deployOperation.artifact = artifact; // to be sure

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
                    }

                    resultSynchronization.addDetection(deployOperation);
                }
            }
        }

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

        int countDeployArtefactWithSuccess = 0;
        int countDeployArtefactFailed = 0;
        int countDeployArtefactIgnored = 0;
        try {
            resultSynchronization.addReportLine("Synchronisation start at " + ForkliftAPI.sdf.format(dateBegin));
            LoggerStore loggerStore = new LoggerStore();

            DetectionParameters detectionParameters = new DetectionParameters();
            detectionParameters.withNotAvailable = true;
            // run each source
            for (BonitaStore bonitaStore : configurationSet.listSources) {
                resultSynchronization.addReportLine("Source " + bonitaStore.getName());
                BonitaStoreResult listArtefact = bonitaStore.getListArtifacts(detectionParameters, loggerStore);

                orderArtefacts(listArtefact);

                for (Artifact artefact : listArtefact.listArtifacts) {
                    countDetectArtefact++;

                    // is this artifact is part of the listAction ?
                    boolean allowDeployment = false;
                    boolean askSourceToRemoveArtefact = false;
                    if (configurationSet.listActions != null) {
                        for (Map<String, Object> actionMap : configurationSet.listActions) {
                            TypeArtifact typeMap = TypeArtifact.valueOf((String) actionMap.get("type"));
                            if (artefact.getType().equals(typeMap)
                                    && isEquals(artefact.getName(), (String) actionMap.get("name"))
                                    && isEquals(artefact.getVersion(), (String) actionMap.get("version"))) {
                                // we get the action to do
                                if (Action.DEPLOY.toString().equals(actionMap.get("action"))) {
                                    allowDeployment = true;
                                    askSourceToRemoveArtefact = true;
                                }
                                if (Action.DELETE.toString().equals(actionMap.get("action")))
                                    askSourceToRemoveArtefact = true;

                            }
                        }
                    } else
                        allowDeployment = configurationSet.isContentAllow(artefact);

                    DeployOperation deployOperation = null;
                    String logDeployment ="";
                    // if the configuration allow this artefact ?
                    if (allowDeployment) {
                        logDeployment += artefact.getType() + " " + artefact.getName() + " : ";
                        // load it ?
                        boolean continuueOperation = true;
                        if (false) // ! artefact.isLoaded)
                        {
                            logDeployment += "loaded,";
                            List<BEvent> listEvents = new ArrayList<BEvent>();
                            // TODO  artefact.sourceOrigin.loadArtefact(artefact);
                            if (BEventFactory.isError(listEvents)) {
                                deployOperation = new DeployOperation();
                                deployOperation.artifact = artefact;
                                deployOperation.listEvents = listEvents;
                                deployOperation.deploymentStatus = DeploymentStatus.LOADFAILED;
                                logDeployment += " failed.";
                                continuueOperation = false;
                            }
                        }
                        if (continuueOperation) {
                            logDeployment += "Deploy ";
                            deployOperation = artefact.deploy(bonitaAccessor, loggerStore);
                            deployOperation.artifact = artefact;
                        }
                        logDeployment += deployOperation.report;
                        // decision can be updated now
                        askSourceToRemoveArtefact = false;

                        switch (deployOperation.deploymentStatus) {
                            case REMOVEFAIL:
                                logDeployment += "ERROR: Remove current failed";
                                countDeployArtefactFailed++;
                                break;
                            case NOTHINGDONE:
                                logDeployment += "INFO: Nothing is done";
                                countDeployArtefactFailed++;
                                break;
                            case NEWALREADYINPLACE:
                                logDeployment += "INFO: A new artefact is already in place";
                                askSourceToRemoveArtefact = true;
                                countDeployArtefactIgnored++;
                                break;
                            case LOADFAILED:
                                logDeployment += "ERROR: Load the content of the artefact failed";
                                countDeployArtefactFailed++;
                                break;
                            case DEPLOYEDFAILED:
                                logDeployment += "ERROR: Deployment failed";
                                countDeployArtefactFailed++;
                                break;
                            case DEPLOYED:
                                logDeployment += "SUCCESS: Deployment is done";
                                askSourceToRemoveArtefact = true;
                                countDeployArtefactWithSuccess++;
                                break;
                        }

                        resultSynchronization.addErrorsEvent(deployOperation.listEvents);
                    }
                    if (askSourceToRemoveArtefact) {
                        if (deployOperation == null) {
                            // If not exist ==> THats mean only a delete was requested
                            deployOperation = new DeployOperation();
                            deployOperation.artifact = artefact;
                        }

                        List<BEvent> listEvents = new ArrayList<>();
                        if (bonitaStore instanceof BonitaStoreDirectory) {
                            // move to an archive directory
                            try {
                                File sourcePath = ((BonitaStoreDirectory)bonitaStore).getDirectory();
                                File destinationPath = new File( sourcePath.getAbsoluteFile()+"/archive" );
                                
                                Toolbox.moveFile(  artefact.getFileName(), sourcePath, destinationPath, true);
                                logDeployment+="Move to archive path;";
                            }catch(Exception e ) {}
                        }
                        if (BEventFactory.isError(listEvents))
                            resultSynchronization.addErrorsEvent(listEvents);
                        if (deployOperation.deploymentStatus == null)
                            deployOperation.deploymentStatus = DeploymentStatus.DELETED;
                    }
                    if (deployOperation != null)
                        resultSynchronization.addDetection(deployOperation);
                    
                    resultSynchronization.addReportLine(logDeployment);

                } // end listArtefact
            } // end source
        } catch (Exception e) {
            resultSynchronization.addErrorEvent(new BEvent(EventDeploymentFailed, e, ""));
        }
        Date dateEnd = new Date();
        resultSynchronization.addReport("Synchronisation End at " + ForkliftAPI.sdf.format(dateEnd) + " in " + (dateEnd.getTime() - dateBegin.getTime()) + " ms");
        resultSynchronization.addReport("  Detected artefacts      : " + countDetectArtefact);
        resultSynchronization.addReport("  Deployment with success : " + countDeployArtefactWithSuccess);
        resultSynchronization.addReport("  Deployment failed       : " + countDeployArtefactFailed);
        resultSynchronization.addReport("  Deployment ignored      : " + countDeployArtefactIgnored);

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

    private void orderArtefacts(BonitaStoreResult listArtefact) {
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
        Collections.sort(listArtefact.listArtifacts, new Comparator<Artifact>() {

            public int compare(Artifact s1,
                    Artifact s2) {
                int rangeS1 = 0;
                int rangeS2 = 0;

                for (int i = 0; i < listOrder.size(); i++) {
                    if (listOrder.get(i).equals(s1.getClass()))
                        rangeS1 = i;
                    if (listOrder.get(i).equals(s2.getClass()))
                        rangeS2 = i;
                }
                if (rangeS1 != rangeS2)
                    return Integer.valueOf(rangeS1).compareTo(rangeS2);
                return s1.getName().compareTo(s2.getName());
            }
        });
    }

}
