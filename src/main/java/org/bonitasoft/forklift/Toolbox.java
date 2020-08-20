package org.bonitasoft.forklift;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
     * Move file
     * 
     * @param fileName
     * @param sourcePath
     * @param pathToMove
     * @throws IOException
     */
    public static void moveFile(String fileName, File sourcePath, File pathToMove, boolean overwrite) throws IOException {
        String fromFile = sourcePath.getAbsolutePath() + "/" + fileName;
        String toFile = pathToMove.getAbsolutePath() + "/" + fileName;

        if (!pathToMove.isDirectory())
            pathToMove.mkdir();
        
        Path source = Paths.get(fromFile);
        Path target = Paths.get(toFile);

        if (overwrite) {
            File targetFile = new File( toFile);
            targetFile.delete();
        }

        Files.move(source, target);

    }
}
