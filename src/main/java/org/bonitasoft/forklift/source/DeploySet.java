package org.bonitasoft.forklift.source;

import java.util.Date;

import org.bonitasoft.store.BonitaStore;

/**
 * the source may contains a DeploymentSet, which is a set of artefact. All
 * theses artefact shoud be deploy together
 * 
 * @author pierre
 *
 */
public class DeploySet {

	private String nameSet;
	private Date dateSet;
	private String description;
	private BonitaStore sourceDetection;

	public String getName() {
		return nameSet;
	}

	public Date getDate() {
		return dateSet;
	}

	public String getDescription() {
		return description;
	}

}
