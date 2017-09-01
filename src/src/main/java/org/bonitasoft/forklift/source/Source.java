package org.bonitasoft.forklift.source;

import java.util.List;
import java.util.Map;

import org.bonitasoft.forklift.artefact.Artefact;
import org.bonitasoft.log.event.BEvent;

public abstract class Source {

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
	public static String cstType = "type";

	/**
	 * create the source from the map
	 */
	public static Source getInstance(Map<String, Object> record) {
		String type = (String) record.get(cstType);
		if (SourceDirectory.cstTypeName.equals(type))
			return SourceDirectory.getInstance(record);
		if (SourceBonita.cstTypeName.equals(type))
			return SourceBonita.getInstance(record);
		return null;
	}

	/**
	 * return the source as a MAP to save it
	 * 
	 * @return
	 */
	public abstract Map<String, Object> toMap();

	/*
	 * *************************************************************************
	 * **
	 */
	/*                                                                             */
	/* Information */
	/*                                                                             */
	/*
	 * *************************************************************************
	 * **
	 */
	/**
	 * return a name to identify the source
	 * 
	 * @return
	 */
	public abstract String getName();

	/*
	 * *************************************************************************
	 * **
	 */
	/*                                                                             */
	/* Operations */
	/*                                                                             */
	/*
	 * *************************************************************************
	 * **
	 */
	/**
	 * contact the source and return the list of all artefact availables
	 *
	 * @return
	 */
	public abstract List<Artefact> getListArtefactDetected();

	/**
	 * some source play the load in two step : first just reference the
	 * artefact, but not load it If this is necessary, then the artefact can
	 * decide to be loaded for the deployment
	 * 
	 * @param artefact
	 */
	public abstract List<BEvent> loadArtefact(Artefact artefact);

	/**
	 * artefact is managed with success, then the source can remove the artefact
	 * 
	 * @param artefact
	 * @return
	 */
	public abstract List<BEvent> removeArtefact(Artefact artefact);

}
