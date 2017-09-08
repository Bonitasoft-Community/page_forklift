package org.bonitasoft.forklift.artefact;

import java.util.Date;

import org.bonitasoft.forklift.source.Source;

public class ArtefactLayout extends ArtefactResource {

	public ArtefactLayout(String name, String version, String description, Date dateCreation, Source sourceOrigin) {
		super( "layout", name, version, description, dateCreation, sourceOrigin);
	}

	@Override
	public String getContentType() {
		return "layout";
	}

	

}
