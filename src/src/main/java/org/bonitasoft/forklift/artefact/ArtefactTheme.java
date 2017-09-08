package org.bonitasoft.forklift.artefact;

import java.util.Date;

import org.bonitasoft.forklift.source.Source;

public class ArtefactTheme extends ArtefactResource {


	public ArtefactTheme(String name, String version, String description, Date dateCreation, Source sourceOrigin) {
		super( "theme", name, version, description, dateCreation, sourceOrigin);
	}

	@Override
	public String getContentType() {
		return "theme";
	}


}
