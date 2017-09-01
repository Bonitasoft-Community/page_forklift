package org.bonitasoft.forklift;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Toolbox {

	/**
	 * this is the logger to use in FoodTruck Attention to reduce the usage, and
	 * to use foodTruckParam.log, then the log information can be manage at the
	 * Input level, as a parameters
	 */

	public static SimpleDateFormat sdfJavasscript = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	static Long getLong(final Object parameter, final Long defaultValue) {
		if (parameter == null) {
			return defaultValue;
		}
		try {
			return Long.valueOf(parameter.toString());
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	static Integer getInteger(final Object parameter, final Integer defaultValue) {
		if (parameter == null) {
			return defaultValue;
		}
		try {
			return Integer.valueOf(parameter.toString());
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	static Boolean getBoolean(final Object parameter, final Boolean defaultValue) {
		if (parameter == null) {
			return defaultValue;
		}
		try {
			return Boolean.valueOf(parameter.toString());
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	static String getString(final Object parameter, final String defaultValue) {
		if (parameter == null) {
			return defaultValue;
		}
		try {
			return parameter.toString();
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	static Date getDate(final Object parameter, final Date defaultValue) {
		if (parameter == null) {
			return defaultValue;
		}
		try {

			return sdfJavasscript.parse(parameter.toString());
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	/**
	 * @param parameter
	 * @param defaultValue
	 * @return
	 */
	static List<Map<String, String>> getList(final Object parameter, final List<Map<String, String>> defaultValue) {
		if (parameter == null) {
			return defaultValue;
		}
		try {
			return (List<Map<String, String>>) parameter;
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	/**
	 * @param parameter
	 * @param defaultValue
	 * @return
	 */
	static Map<String, Object> getMap(final Object parameter, final Map<String, Object> defaultValue) {
		if (parameter == null) {
			return defaultValue;
		}
		try {
			return (Map<String, Object>) parameter;
		} catch (final Exception e) {
			return defaultValue;
		}
	}

	/**
	 * calculate the file name
	 *
	 * @param directory
	 * @param domain
	 * @param configFileName
	 * @return
	 */
	public static String getConfigFileName(final String ldapSynchronizerPath, final String domain, final String configFileName) {
		final String fileName = ldapSynchronizerPath + File.separator + domain + File.separator + configFileName;
		// logger.info("CraneTruck.Toolbox: configuration [" + configFileName +
		// "] file is [" + fileName + "]");
		return fileName;
	}

	/**
	 * This class : - get the apps available on BonitaTStore - get the local
	 * Apps install - give a status apps per apps if
	 * foodTruckParam.listcustompage is true, then the custom page is checked if
	 * foodTruckParam.callAppStoreCustomPages is true, the AppsStore is call to
	 * search new apps, new update
	 *
	 * @param foodTruckParam
	 * @param pageAPI
	 * @param profileAPI
	 * @return
	 */

}
