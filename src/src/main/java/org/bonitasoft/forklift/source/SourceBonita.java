package org.bonitasoft.forklift.source;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.forklift.artefact.Artefact;
import org.bonitasoft.log.event.BEvent;

public class SourceBonita extends Source {
	/**
	 * Same name than in the HTML
	 */
	private static String cstHost = "host";
	private static String cstPort = "port";
	private static String cstLogin = "login";
	private static String cstPassword = "password";

	/**
	 * same name as the type in the HTML
	 */
	public static String cstTypeName = "BONITA";

	private String host;
	private int port;
	private String login;
	private String password;

	public SourceBonita(String host, int port, String login, String password) {
		this.host = host;
		this.port = port;
		this.login = login;
		this.password = password;

	}

	public static Source getInstance(Map<String, Object> record) {
		String host = (String) record.get(cstHost);
		int port = Integer.parseInt(record.get(cstPort).toString());
		String login = (String) record.get(cstLogin);
		String password = (String) record.get(cstPassword);
		Source sourceBonita = new SourceBonita(host, port, login, password);
		return sourceBonita;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> record = new HashMap<String, Object>();
		record.put(Source.cstType, SourceBonita.cstTypeName);
		record.put(SourceBonita.cstHost, host);
		record.put(SourceBonita.cstPort, port);
		record.put(SourceBonita.cstLogin, login);
		record.put(SourceBonita.cstPassword, password);
		return record;
	}

	/**
	 * get the directory and then return all contents
	 */
	public List<Artefact> getListArtefactDetected() {
		return null;
	}

	/**
	 * all theses artefact were correctly installed. So, the source can decide
	 * what to do with this information
	 * 
	 * @param listArtefact
	 */

	public String getName() {
		return "Bonita -" + host;
	}

	@Override
	public List<BEvent> loadArtefact(Artefact artefact) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BEvent> removeArtefact(Artefact artefact) {
		// TODO Auto-generated method stub
		return null;
	}

}
