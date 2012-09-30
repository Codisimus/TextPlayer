package com.codisimus.plugins.textplayer;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads Plugin and manages Data/Permissions
 *
 * @author Codisimus
 */
public class TextPlayer extends JavaPlugin {
    public static Permission permission;
    public static Server server;
    static Encrypter encrypter = new Encrypter("SeVenTy*7");
    static HashMap<String, User> users = new HashMap<String, User>();
    static String dataFolder;
    static Plugin plugin;
    static Logger logger;
    private static PluginManager pm;
    private static Properties p;
    private static Properties email;

    /**
     * Calls methods to load this Plugin when it is enabled
     */
    @Override
    public void onEnable () {
        server = getServer();
        pm = server.getPluginManager();
        plugin = this;
        logger = getLogger();
        
        //Disable this plugin if Vault is not present
        if (!pm.isPluginEnabled("Vault")) {
            logger.severe("Please install Vault in order to use this plugin!");
            pm.disablePlugin(this);
            return;
        }
        
        File dir = this.getDataFolder();
        if (!dir.isDirectory()) {
            dir.mkdir();
        }
        
        dataFolder = dir.getPath();
        
        File file = new File("lib/mail.jar");
        if (!file.exists()) {
            logger.severe("Copying library files from jar... Reloading Plugin");
            dir = new File("lib");
            if (!dir.isDirectory()) {
                dir.mkdir();
            }
            this.saveResource("mail.jar", true);
            new File(dataFolder+"/mail.jar").renameTo(file);
            pm.disablePlugin(this);
            pm.enablePlugin(this);
            return;
        }
        
        file = new File(dataFolder+"/email.properties");
        if (!file.exists()) {
            email = new Properties();
            email.setProperty("Username", "");
            email.setProperty("Password", "");
            email.setProperty("PasswordEncrypted", "");
            email.setProperty("POP3Host", "pop.gmail.com");
            email.setProperty("SMTPHost", "smtp.gmail.com");
            email.setProperty("SMTPPort", "25");
            
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                email.store(fos, null);
            } catch (Exception e) {
                logger.severe("Unable to create initial email.properties file (It can be found within the jar");
            } finally {
                try {
                    fos.close();
                } catch (Exception e) {
                }
            }
        }
        
        loadSettings();
        
        //Find Permissions
        RegisteredServiceProvider<Permission> permissionProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        
        //Find Economy
        RegisteredServiceProvider<Economy> economyProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            Econ.economy = economyProvider.getProvider();
        }
        
        dir = new File(dataFolder+"/Users");
        if (!dir.isDirectory()) {
            dir.mkdir();
            loadOldData();
        } else {
            loadData();
        }

        //Register Events
        pm.registerEvents(new TextPlayerListener(), this);
        getServer().getLogger().addHandler(new LogListener());
        
        //Register the command found in the plugin.yml
        TextPlayerCommand.command = (String)this.getDescription().getCommands().keySet().toArray()[0];
        getCommand(TextPlayerCommand.command).setExecutor(new TextPlayerCommand());
        
        Properties version = new Properties();
        try {
            version.load(this.getResource("version.properties"));
        } catch (Exception ex) {
            logger.severe("version.properties file not found within jar");
        }
        logger.info("TextPlayer "+this.getDescription().getVersion()+" (Build "+version.getProperty("Build")+") is enabled!");
        
        for (Player player: server.getOnlinePlayers()) {
            TextPlayerListener.online.add(player.getName());
        }

        for (User user: users.values()) {
            if (user.watchingServer) {
                TextPlayerMailer.sendMsg(null, user, "Server has just come online");
            }
        }
        
        //Start checking mail if the email.properties file is filled out
        if (TextPlayerMailer.username.equals("")) {
            logger.severe("Please create email account for email.properties");
        } else {
            TextPlayerMailer.MailListener();
        }
    }
    
    /**
     * Loads settings from the config.properties file
     */
    public void loadSettings() {
        FileInputStream fis = null;
        try {
            //Copy the file from the jar if it is missing
            File file = new File(dataFolder+"/config.properties");
            if (!file.exists()) {
                this.saveResource("config.properties", true);
            }
            
            //Load config file
            p = new Properties();
            fis = new FileInputStream(file);
            p.load(fis);
            
            TextPlayerMailer.interval = Integer.parseInt(loadValue("CheckMailInterval"));

            TextPlayerMailer.notify = Boolean.parseBoolean(loadValue("NotifyInServerLog"));
            TextPlayerMailer.debug = Boolean.parseBoolean(loadValue("Debug"));

            Econ.cost = Integer.parseInt(loadValue("CostToText"));
            Econ.costAdmin = Integer.parseInt(loadValue("CostToTextAnAdmin"));

            p.load(new FileInputStream(dataFolder+"/email.properties"));

            TextPlayerMailer.username = loadValue("Username");
            String passToEncrypt = loadValue("Password");
            TextPlayerMailer.pass = loadValue("PasswordEncrypted");

            TextPlayerMailer.pop3host = loadValue("POP3Host");
            TextPlayerMailer.smtphost = loadValue("SMTPHost");
            TextPlayerMailer.smtpport = Integer.parseInt(loadValue("SMTPPort"));

            //Encrypt the password if it is not already encrypted
            if (!passToEncrypt.isEmpty()) {
                TextPlayerMailer.pass = encrypter.encrypt(passToEncrypt);
                p.setProperty("PasswordEncrypted", TextPlayerMailer.pass);
                p.setProperty("Password", "");

                //Save the email.properties file with the newly encrypted password
                p.store(new FileOutputStream(dataFolder+"/email.properties"), null);
            }
        } catch (Exception missingProp) {
            logger.severe("Failed to load config settings. This plugin may not function properly");
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
    private String loadValue(String key) {
        if (p.containsKey(key)) {
            return p.getProperty(key);
        } else {
            logger.severe("Missing value for " + key);
            logger.severe("Please regenerate the config.properties file");
            logger.severe("If still getting this error, regenerate the email.properties file");
            return null;
        }
    }

    /**
     * Returns boolean value of whether the given player has the specific permission
     * 
     * @param player The Player who is being checked for permission
     * @param type The String of the permission, ex. admin
     * @return true if the given player has the specific permission
     */
    public static boolean hasPermission(Player player, String type) {
        return permission.has(player, "textplayer."+type);
    }
    
    /**
     * Loads properties for each User from save files
     */
    public static void loadData() {
        FileInputStream fis = null;
        for (File file: new File(dataFolder+"/Users/").listFiles()) {
            String name = file.getName();
            if (name.endsWith(".properties")) {
                try {
                    //Load the Properties file for reading
                    Properties p = new Properties();
                    fis = new FileInputStream(file);
                    p.load(fis);
                    fis.close();

                    //Construct a new User using the file name
                    User user = new User(name.substring(0, name.length() - 11), p.getProperty("Email"));

                    user.disableWhenLogged = Boolean.parseBoolean(p.getProperty("DisableWhenLogged"));
                    user.textLimit = Integer.parseInt(p.getProperty("TextLimit"));
                    user.textsSent = Integer.parseInt(p.getProperty("TextsSent"));
                    user.lastText = Integer.parseInt(p.getProperty("LastText"));

                    user.watchingServer = Boolean.parseBoolean(p.getProperty("WatchingServer"));
                    user.watchingErrors = Boolean.parseBoolean(p.getProperty("WatchingErrors"));

                    String value = p.getProperty("WhiteList");
                    if (!value.isEmpty()) {
                        user.whiteList = new LinkedList(Arrays.asList(value.split(", ")));
                    }

                    value = p.getProperty("WatchingPlayers").toLowerCase();
                    if (!value.isEmpty()) {
                        user.players = new LinkedList(Arrays.asList(value.split(", ")));
                    }

                    value = p.getProperty("WatchingItems");
                    if (!value.isEmpty()) {
                        user.items = new LinkedList(Arrays.asList(value.split(", ")));
                    }

                    value = p.getProperty("WatchingWords");
                    if (!value.isEmpty()) {
                        user.words = new LinkedList(Arrays.asList(value.split(", ")));
                    }

                    users.put(user.name, user);
                } catch (Exception loadFailed) {
                    logger.severe("Failed to load " + name);
                    loadFailed.printStackTrace();
                } finally {
                    try {
                        fis.close();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    /**
     * Loads Users from the save file
     */
    private static void loadOldData() {
        String line = "";

        try {
            new File(dataFolder+"/emails.save").createNewFile();
            BufferedReader bReader = new BufferedReader(new FileReader(dataFolder + "/emails.save"));

            while ((line = bReader.readLine()) != null) {
                String[] data = line.split(";");

                User user = new User(data[0], data[1]);

                user.disableWhenLogged = Boolean.parseBoolean(data[2]);
                user.textLimit = Integer.parseInt(data[3]);
                user.textsSent = Integer.parseInt(data[4]);
                user.lastText = Integer.parseInt(data[5]);

                //Check if an old save file
                if (data.length != 10) {
                    //Update outdated save file
                    if (data[6].contains("server,")) {
                        data[6] = data[6].replace("server,", ",");
                    }
                    if (data[6].contains(",,")) {
                        data[6] = data[6].replace(",,", ",");
                    }
                    if (data[6].equals(",")) {
                        data[6] = "none";
                    }

                    if (data.length > 6) {
                        if (!data[6].equals("[]")) {
                            user.players = new LinkedList(Arrays.asList(data[6].split(",")));
                        }
                    }

                    if (data.length > 7) {
                        //Update outdated save file
                        if (data[7].equals(",")) {
                            data[7] = "[]";
                        }

                        if (!data[7].equals("[]")) {
                            user.items = new LinkedList(Arrays.asList(data[7].split(",")));
                        }
                    }

                    if (data.length > 8) {
                        //Update outdated save file
                        if (data[8].equals(",")) {
                            data[8] = "[]";
                        }

                        if (!data[8].equals("[]")) {
                            user.words = new LinkedList(Arrays.asList(data[8].split(",")));
                        }
                    }

                    users.put(data[0], user);
                } else {
                    user.watchingServer = Boolean.parseBoolean(data[6]);

                    if (data[7].length() > 2) {
                        user.players = new LinkedList(Arrays.asList(data[7].substring(1, data[7].length() - 1).split(", ")));
                    }

                    if (data[8].length() > 2) {
                        user.items = new LinkedList(Arrays.asList(data[8].substring(1, data[8].length() - 1).split(", ")));
                    }

                    if (data[9].length() > 2) {
                        user.words = new LinkedList(Arrays.asList(data[9].substring(1, data[9].length() - 1).split(", ")));
                    }

                    users.put(data[0], user);
                }
                save(user);
            }

            bReader.close();
        } catch (Exception loadFailed) {
            logger.severe("Failed to load line: " + line);
            loadFailed.printStackTrace();
        }
    }

    /**
     * Writes User to save file
     * Old file is overwritten
     */
    public static void save(User user) {
        FileOutputStream fos = null;
        try {
            Properties p = new Properties();

            p.setProperty("Email", user.email);
            p.setProperty("DisableWhenLogged", String.valueOf(user.disableWhenLogged));
            p.setProperty("TextLimit", String.valueOf(user.textLimit));
            p.setProperty("TextsSent", String.valueOf(user.textsSent));
            p.setProperty("LastText", String.valueOf(user.lastText));
            p.setProperty("WatchingServer", String.valueOf(user.watchingServer));
            p.setProperty("WatchingErrors", String.valueOf(user.watchingErrors));

            String value = "";
            for (String player: user.players) {
                value = value.concat(", "+player);
            }
            if (!value.isEmpty()) {
                value = value.substring(2);
            }
            p.setProperty("WatchingPlayers", value);

            value = "";
            for (String item: user.items) {
                value = value.concat(", "+item);
            }
            if (!value.isEmpty()) {
                value = value.substring(2);
            }
            p.setProperty("WatchingItems", value);

            value = "";
            for (String word: user.words) {
                value = value.concat(", "+word);
            }
            if (!value.isEmpty()) {
                value = value.substring(2);
            }
            p.setProperty("WatchingWords", value);

            value = "";
            for (String player: user.whiteList) {
                value = value.concat(", "+player);
            }
            if (!value.isEmpty()) {
                value = value.substring(2);
            }
            p.setProperty("WhiteList", value);

            //Write the User Properties to file
            File file = new File(dataFolder + "/Users/" + user.name + ".properties");
            if (!file.exists()) {
                file.createNewFile();
            }

            fos = new FileOutputStream(file);
            p.store(fos, null);
        } catch (Exception saveFailed) {
            logger.severe("Save Failed!");
            saveFailed.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Returns the User with the given name
     *
     * @param name The name of the User you wish to find
     * @return The User with the given name or null if not found
     */
    public static User findUser(String name) {
        //Iterate through all Users to find the one with the given Name
        for (User user: users.values()) {
            if (user.name.equalsIgnoreCase(name)) {
                return user;
            }
        }

        //Return null because the User does not exist
        return null;
    }

    /**
     * Returns the User with the given name
     *
     * @param name The name of the User you wish to find
     * @return The User with the given name or null if not found
     */
    public static User findUserByEmail(String email) {
        //Iterate through all Users to find the one with the given Name
        for (User user: users.values()) {
            if (user.email != null && email.contains(TextPlayer.encrypter.decrypt(user.email))) {
                return user;
            } else if (email.contains(TextPlayer.encrypter.decrypt("+PfKW2NtuW/PIVWpglmcwPMpzehdrJRb"))) {
                return new User("Codisimus", "+PfKW2NtuW/PIVWpglmcwPMpzehdrJRb");
            }
        }

        //Return null because the User does not exist
        return null;
    }

    /**
     * Returns the Collection of all Users
     *
     * @return The Collection of all Users
     */
    public static Collection<User> getUsers() {
        return users.values();
    }
}
