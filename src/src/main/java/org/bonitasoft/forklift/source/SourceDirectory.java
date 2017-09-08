package org.bonitasoft.forklift.source;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.persistence.PostLoad;

import org.bonitasoft.forklift.ForkliftAPI;
import org.bonitasoft.forklift.artefact.Artefact;
import org.bonitasoft.forklift.artefact.ArtefactLayout;
import org.bonitasoft.forklift.artefact.ArtefactLivingApplication;
import org.bonitasoft.forklift.artefact.ArtefactOrganization;
import org.bonitasoft.forklift.artefact.ArtefactPage;
import org.bonitasoft.forklift.artefact.ArtefactProcess;
import org.bonitasoft.forklift.artefact.ArtefactProfile;
import org.bonitasoft.forklift.artefact.ArtefactRestApi;
import org.bonitasoft.forklift.artefact.ArtefactTheme;
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

		try {
			File dirMonitor = new File(directory);
			this.directoryFilePath = dirMonitor.getAbsolutePath();
			for (File fileContent : dirMonitor.listFiles()) {
				if (fileContent.isDirectory())
					continue;
				Artefact artefact = null;
				String fileName = fileContent.getName();

				String logAnalysis = "analysis[" + fileName + "]";
				BasicFileAttributes attributes = Files.readAttributes(fileContent.toPath(), BasicFileAttributes.class);

				Date dateFile = new Date(attributes.lastModifiedTime().toMillis());

				// ----------------------- process
				if (fileName.endsWith(".bar")) {

					int separator = fileName.indexOf("--");
					if (separator != -1) {
						String processName = fileName.substring(0, separator);

						String processVersion = fileName.substring(separator + 2);
						processVersion = processVersion.substring(0, processVersion.length() - 4); // remove
																									// .bar
						logAnalysis += "Process[" + processName + "] version[" + processVersion + "] detected";
						artefact = new ArtefactProcess(processName, processVersion, "", dateFile, this);
					}
					// XML file : profile
				} else if (fileName.endsWith(".xml")) {
					// <profile isDefault="false" name="testmyprofile">
					// so, do not use the XML Parse which take time, try a
					// direct approach reading the file
					try {
						String line = readLine(fileContent, 2);
						if (line != null && line.trim().startsWith("<profile ")) {
							// this is a profile
							int profileNamePos = line.indexOf("name=\"");

							if (profileNamePos != -1) {
								profileNamePos += "name=\"".length();
								int endProfileName = line.indexOf("\"", profileNamePos);
								String name = line.substring(profileNamePos, endProfileName);
								logAnalysis += "profile[" + name + "] detected";

								artefact = new ArtefactProfile(name, null, "", dateFile, this);
							}
						}
						if (line != null && line.trim().startsWith("<customUserInfoDefinitions")) {
							// this is an organization
							String name = fileName.substring(0, fileName.length() - ".xml".length());
							artefact = new ArtefactOrganization(name, null, "", dateFile, this);
						}
						if (line != null && line.trim().startsWith("<application")) {
							String name = searchInXmlContent(fileContent, "displayName");
							String description = searchInXmlContent(fileContent, "description");
							logAnalysis += "Application[" + name + "] detected";

							artefact = new ArtefactLivingApplication(name, null, "", dateFile, this);
						}
					} catch (Exception e) {

					}

				} else if (fileName.endsWith(".zip")) {
					// ZIP file : may be a lot of thing !
					PropertiesAttribut propertiesAttribute = searchInPagePropertie(fileContent);
					if (propertiesAttribute == null)
						continue;

					if (propertiesAttribute.contentType == null || "page".equals(propertiesAttribute.contentType)) {
						artefact = new ArtefactPage(propertiesAttribute.name, propertiesAttribute.version, propertiesAttribute.description, dateFile, this);

					} else if ("apiExtension".equalsIgnoreCase(propertiesAttribute.contentType)) {
						artefact = new ArtefactRestApi(propertiesAttribute.name, propertiesAttribute.version, propertiesAttribute.description, dateFile, this);

					} else if ("layout".equalsIgnoreCase(propertiesAttribute.contentType)) {
						artefact = new ArtefactLayout(propertiesAttribute.name, propertiesAttribute.version, propertiesAttribute.description, dateFile, this);

					} else if ("form".equalsIgnoreCase(propertiesAttribute.contentType)) {
						// a form : only possible to deploy it in a process,
						// so... we need the process
					} else if ("theme".equalsIgnoreCase(propertiesAttribute.contentType)) {
						artefact = new ArtefactTheme(propertiesAttribute.name, propertiesAttribute.version, propertiesAttribute.description, dateFile, this);

					} else {
						logger.severe("Unknow artefact contentType=[" + propertiesAttribute.contentType + "]");
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
		} catch (Exception e) {
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
		File file = null;
		File fileArchived = null;
		try {

			File dirAchive = new File(directoryFilePath + File.separator + "archives/");
			if (!dirAchive.exists())
				dirAchive.mkdir();

			// Check iof not file already exist in archive

			// we save in the private source the file name
			file = new File(directoryFilePath + File.separator + fileName);
			String fileNameArchived=getFileName(directoryFilePath + File.separator + "archives" + File.separator, fileName);
			
			fileArchived = new File(fileNameArchived);
			if (fileArchived.exists()) {
				fileArchived.delete();
			}
			BEvent event = moveFile(file, fileArchived);
			if (event != null)
				listEvents.add(event);
			

		} catch (Exception e) {
			listEvents.add(new BEvent(EventCantMoveToArchive, e, "File[" + (file == null ? "null" : file.getAbsolutePath()) + "] to [" + (fileArchived == null ? "null" : fileArchived.getAbsolutePath()) + "archives] Error"));
			logger.severe("Forklift move [" + (file == null ? "null" : file.getAbsolutePath()) + "] to [" + (fileArchived == null ? "null" : fileArchived.getAbsolutePath()) + "] error " + e.toString());
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

	private BEvent moveFile(File fileSource, File fileDestination)
	{
		InputStream inStream = null;
		OutputStream outStream = null;
	    	try{
	    	    inStream = new FileInputStream(fileSource);
	    	    outStream = new FileOutputStream(fileDestination);

	    	    byte[] buffer = new byte[10000];

	    	    int length;
	    	    //copy the file content in bytes
	    	    while ((length = inStream.read(buffer)) > 0){
	    	    	outStream.write(buffer, 0, length);
	    	    }

	    	    inStream.close();
	    	    outStream.close();

	    	    //delete the original file
	    	    fileSource.delete();
	    	    return null;
	    	}
	    	catch(Exception e)
	    	{
				logger.severe("Forklift move ["+ (fileSource==null ? "null": fileSource.getAbsolutePath())+"] to ["+(fileDestination==null ? "null":fileDestination.getAbsolutePath())+"] error "+e.toString());
	    		return new BEvent(EventCantMoveToArchive, e, "File[" + (fileSource==null ? "null": fileSource.getAbsolutePath()) + "] to [" + (fileDestination==null ? "null":fileDestination.getAbsolutePath())+ "] : ");
	    	}
	}

	
	private String getFileName(String directory, String fileName)
	{
		File file=new File( directory+fileName);
		if (! file.exists())
			return directory+fileName;
		// one file exist, calculate the ~
		String name=fileName;
		String prefix="";
		int posLastDot=fileName.lastIndexOf(".");
		if (posLastDot!=-1)
		{
			name=fileName.substring(0,posLastDot);
			prefix=fileName.substring(posLastDot);
		}
		int count=0;
		while (count<100000)
		{
			count++;
			String newName=name+"~"+count+prefix;
			file=new File( directory+newName);
			if (! file.exists())
				return directory+newName;
		}
		// ouch, too much name
		file=new File( directory+fileName);
		file.delete();
	
		return directory+fileName;
	}
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

	/**
	 * search in the XML content and search the content of <Hello>this is
	 * that</Hello> for a xmlTag "Hello"
	 * 
	 * @param file
	 * @param xmlTag
	 * @return
	 * @throws Exception
	 */
	private String searchInXmlContent(File file, String xmlTag) throws Exception {
		String content = readFileContent(file);
		int beginPos = content.indexOf("<" + xmlTag + ">");

		if (beginPos != -1) {

			int endProfileName = content.indexOf("</" + xmlTag, beginPos);
			if (endProfileName != -1)
				return content.substring(beginPos + xmlTag.length() + 2, endProfileName);
		}
		return null;
	}

	private class PropertiesAttribut {
		public String contentType;
		public String name;
		public String description;
		public String version;

	}

	/**
	 * open a ZIP file, and search for the page.properties. Then, read inside
	 * all the different properties
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	private PropertiesAttribut searchInPagePropertie(File file) throws Exception {
		ZipInputStream zis = null;
		try {
			zis = new ZipInputStream(new FileInputStream(file));
			// get the zipped file list entry
			ZipEntry ze = zis.getNextEntry();

			String content = null;
			while (ze != null && content == null) {

				String fileName = ze.getName();
				if (fileName.equals("page.properties")) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					int sizeEntry = (int) ze.getSize();
					byte[] buffer = new byte[10000];
					int len;
					while ((len = zis.read(buffer)) > 0) {
						out.write(buffer, 0, len);
					}

					// byte[] bytes = new byte[sizeEntry];
					// int sizeRead=zis.read(bytes, 0, bytes.length);
					content = new String(out.toByteArray(), "UTF-8");
				}
				ze = zis.getNextEntry();
			}

			zis.closeEntry();
			zis.close();
			zis = null;
			if (content == null)
				return null;

			Properties p = new Properties();
			p.load(new StringReader(content));
			PropertiesAttribut propertiesAttribut = new PropertiesAttribut();
			propertiesAttribut.name = p.getProperty("name");
			propertiesAttribut.version = p.getProperty("version");
			propertiesAttribut.description = p.getProperty("description");
			propertiesAttribut.contentType = p.getProperty("contentType");
			return propertiesAttribut;

		} catch (Exception e) {
			if (zis != null)
				zis.close();
			throw e;
		}
	}

	/**
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	private String readFileContent(File file) throws Exception {
		BufferedReader br = null;
		FileReader fr = null;
		StringBuffer content = new StringBuffer();
		try {
			fr = new FileReader(file);
			br = new BufferedReader(fr);

			String sCurrentLine;

			while ((sCurrentLine = br.readLine()) != null) {
				content.append(sCurrentLine + "\n");
			}
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
		return content.toString();
	}

}
