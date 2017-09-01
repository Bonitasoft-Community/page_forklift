package org.bonitasoft.forklift.source;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.DiscriminatorColumn;

import org.bonitasoft.engine.bpm.bar.InvalidBusinessArchiveFormatException;
import org.bonitasoft.forklift.ForkliftAPI;
import org.bonitasoft.forklift.artefact.Artefact;
import org.bonitasoft.forklift.artefact.ArtefactProcess;
import org.bonitasoft.forklift.artefact.ArtefactProfile;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

public class SourceDirectory extends Source {

	private static Logger logger = Logger.getLogger(ForkliftAPI.class.getName());

	private static BEvent EventLoadFailed = new BEvent(SourceDirectory.class.getName(), 1, Level.APPLICATIONERROR, "Error at load time", "The artefact can't be loaded", "Artefact is not accessible", "Check the exception");
	private static BEvent EventCantMoveToArchive = new BEvent(SourceDirectory.class.getName(), 2, Level.APPLICATIONERROR, "Can't move to archived", "The artefact can't be move to the archive directory", "Artefact will be study a new time, but then mark as 'already loaded'",
			"Check the exception (access right ?)");

	/**
	 * directory given by administrator
	 */
	public String directory;
	/**
	 * Directory get back from java.io
	 */
	public String directoryFilePath;

	/**
	 * Same name than in the HTML
	 */
	private static String cstDirectory = "directory";

	/**
	 * same name as the type in the HTML
	 */
	public static String cstTypeName = "DIR";

	public SourceDirectory(String directory) {
		this.directory = directory;
	}

	public static Source getInstance(Map<String, Object> record) {
		String directory = (String) record.get(cstDirectory);
		Source sourceDirectory = new SourceDirectory(directory);
		return sourceDirectory;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> record = new HashMap<String, Object>();
		record.put(Source.cstType, SourceDirectory.cstTypeName);
		record.put(SourceDirectory.cstDirectory, directory);
		return record;
	}

	/**
	 * all theses artefact were correctly installed. So, the source can decide
	 * what to do with this information
	 * 
	 * @param listArtefact
	 */

	public String getName() {
		return "Directory -" + directory;
	}

	/**
	 * get the directory and then return all contents
	 */
	public List<Artefact> getListArtefactDetected() {
		List<Artefact> listArtefacts = new ArrayList<Artefact>();

		try
		{
		File dirMonitor = new File(directory);
		this.directoryFilePath = dirMonitor.getAbsolutePath();
		for (File fileContent : dirMonitor.listFiles()) {
			if (fileContent.isDirectory())
				continue;
			Artefact artefact = null;
			String fileName = fileContent.getName();
			String logAnalysis = "analysis[" + fileName + "]";
			BasicFileAttributes attributes = Files.readAttributes( fileContent.toPath(), BasicFileAttributes.class);
	        
			Date dateFile = new Date( attributes.lastModifiedTime().toMillis() );

			// ----------------------- process
			if (fileName.endsWith(".bar")) {

				int separator = fileName.indexOf("--");
				if (separator != -1) {
					String processName = fileName.substring(0, separator);

					String processVersion = fileName.substring(separator + 2);
					processVersion = processVersion.substring(0, processVersion.length() - 4); // remove
																								// .bar
					logAnalysis += "Process[" + processName + "] version[" + processVersion + "] detected";
					artefact = new ArtefactProcess(processName, processVersion,"", dateFile, this);
				}
			// XML file : profile
			} else if (fileName.endsWith(".xml"))
			{
				// <profile isDefault="false" name="testmyprofile">
				// so, do not use the XML Parse which take time, try a direct approach reading the file
				try
				{
					String line=readLine( fileContent, 2);
					if (line !=null && line.trim().startsWith("<profile "))
					{
						// this is a profile
						int profileNamePos = line.indexOf("name=\"");
						
						if (profileNamePos!=-1)
						{
							profileNamePos+= "name=\"".length();
							int endProfileName=line.indexOf("\"", profileNamePos);
							String name=line.substring(profileNamePos, endProfileName);
							logAnalysis += "profile[" + name + "] detected";
							
							artefact = new ArtefactProfile(name, null, "", dateFile, this);
						}
					}
				}
				catch(Exception e)
				{
					
				}
				// ----------------------- Nothing
			} else {
				logAnalysis += "No detection.";
			}

			if (artefact != null) {
				artefact.privateSourceInformation = fileName;
				listArtefacts.add(artefact);
			}
			logger.info("ForkList.SourceDirectory " + logAnalysis);

		}
		}
		catch(Exception e)
		{
			logger.info("SourceDirectory.getListArtefactDetected Exception [" + e.toString() + "]");

		}
		return listArtefacts;
	}

	/**
	 * 
	 */
	public List<BEvent> loadArtefact(Artefact artefact) {
		List<BEvent> listEvents = new ArrayList<BEvent>();
		String fileName = (String) artefact.privateSourceInformation;
		// we saved in the private source the file name
		File file = new File(directoryFilePath + File.separator + fileName);
		listEvents.addAll(artefact.loadFromFile(file));

		return listEvents;
	}

	/**
	 * deployment where correct, so now archive the artefact
	 */
	@Override
	public List<BEvent> removeArtefact(Artefact artefact) {
		// move the artefact to the directory "archive
		List<BEvent> listEvents = new ArrayList<BEvent>();
		String fileName = (String) artefact.privateSourceInformation;
		try {

			File dirAchive = new File(directoryFilePath + File.separator + "archives/");
			if (!dirAchive.exists())
				dirAchive.mkdir();

			// Check iof not file already exist in archive
			
			// we save in the private source the file name
			File file = new File(directoryFilePath + File.separator + fileName);
			File fileArchived=new File(directoryFilePath + File.separator + "archives" + File.separator + fileName);
			if (fileArchived.exists())
			{
				fileArchived.delete();
			}
			if (!file.renameTo( fileArchived)) {
				listEvents.add(new BEvent(EventCantMoveToArchive, "File[" + fileName + "] to [" + directoryFilePath + File.separator + "archives]"));
			}
		} catch (Exception e) {
			listEvents.add(new BEvent(EventCantMoveToArchive, e, "File[" + fileName + "] to [" + directoryFilePath + File.separator + "archives] Error"));

		}

		return listEvents;
	}

	/*
	 * ***********************************************************************
	 */
	/*                                                                         */
	/* Toolbox */
	/*                                                                         */
	/*                                                                         */
	/*
	 * ***********************************************************************
	 */

	/**
	 * read a line
	 * 
	 * @param file
	 * @param lineToRead
	 * @return
	 */
	protected String readLine(File file, int lineToRead) throws Exception {
		BufferedReader br = null;
		FileReader fr = null;
		try {
			fr = new FileReader(file);

			br = new BufferedReader(fr);

			int lineNumber = 0;
			String sCurrentLine;

			while ((sCurrentLine = br.readLine()) != null) {
				lineNumber++;
				if (lineToRead == lineNumber)
					return sCurrentLine;
			}
			return null;
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if (br != null)
					br.close();

				if (fr != null)
					fr.close();
			} catch (Exception e) {
			}
		}
	}
}
