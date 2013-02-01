package com.codisimus.plugins.textplayer;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * Loads Plugin config settings
 *
 * @author Codisimus
 */
public class TextPlayerConfig {
    private static Properties p;

    public static void load() {
        //Load Config settings
        FileInputStream fis = null;
        try {
            //Copy the file from the jar if it is missing
            File file = new File(TextPlayer.dataFolder + "/config.properties");
            if (!file.exists()) {
                TextPlayer.plugin.saveResource("config.properties", true);
            }

            //Load config file
            p = new Properties();
            fis = new FileInputStream(file);
            p.load(fis);

            TextPlayerMailReader.interval = loadInt("CheckMailInterval", 60);

            TextPlayerMailReader.notify = loadBool("NotifyInServerLog", false);
            TextPlayerMailReader.debug = loadBool("Debug", false);

            Econ.cost = loadInt("CostToText", 0);
            Econ.costAdmin = loadInt("CostToTextAnAdmin", 0);

            String admins = loadString("Admins", "");
            if (!admins.isEmpty()) {
                TextPlayer.admins.addAll(Arrays.asList(admins.split("'")));
            }
        } catch (Exception missingProp) {
            TextPlayer.logger.severe("Failed to load TextPlayer Config");
            missingProp.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Loads the given key and prints an error if the key is missing
     *
     * @param key The key to be loaded
     * @return The String value of the loaded key
     */
    private static String loadString(String key, String defaultString) {
        if (p.containsKey(key)) {
            return p.getProperty(key);
        } else {
            TextPlayer.logger.severe("Missing value for " + key);
            TextPlayer.logger.severe("Please regenerate the config.properties file (delete the old file to allow a new one to be created)");
            TextPlayer.logger.severe("DO NOT POST A TICKET FOR THIS MESSAGE, IT WILL JUST BE IGNORED");
            return defaultString;
        }
    }

    /**
     * Loads the given key and prints an error if the key is not an Integer
     *
     * @param key The key to be loaded
     * @return The Integer value of the loaded key
     */
    private static int loadInt(String key, int defaultValue) {
        String string = loadString(key, null);
        try {
            return Integer.parseInt(string);
        } catch (Exception e) {
            TextPlayer.logger.severe("The setting for " + key + " must be a valid integer");
            TextPlayer.logger.severe("DO NOT POST A TICKET FOR THIS MESSAGE, IT WILL JUST BE IGNORED");
            return defaultValue;
        }
    }

    /**
     * Loads the given key and prints an error if the key is not a boolean
     *
     * @param key The key to be loaded
     * @return The boolean value of the loaded key
     */
    private static boolean loadBool(String key, boolean defaultValue) {
        String string = loadString(key, null);
        try {
            return Boolean.parseBoolean(string);
        } catch (Exception e) {
            TextPlayer.logger.severe("The setting for " + key + " must be 'true' or 'false' ");
            TextPlayer.logger.severe("DO NOT POST A TICKET FOR THIS MESSAGE, IT WILL JUST BE IGNORED");
            return defaultValue;
        }
    }
}
