package com.codisimus.plugins.textplayer;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads Plugin and manages Data/Permissions
 *
 * @author Codisimus
 */
public class TextPlayer extends JavaPlugin {
    static final HashMap<String, User> users = new HashMap<>();
    static final HashSet<String> admins = new HashSet<>();
    static String dataFolder;
    static Plugin plugin;
    static Logger logger;
    private static Properties p;
    private static Properties email;

    /**
     * Calls methods to load this Plugin when it is enabled
     */
    @Override
    public void onEnable () {
        //System.setProperty("javax.activation.debug", "true");

        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        //MailcapCommandMap cMap = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        //cMap.addMailcap("text/plain; ; x-java-content-handler=com.sun.mail.handlers.text_plain");
        //cMap.addMailcap("text/html; ; x-java-content-handler=com.sun.mail.handlers.text_html");
        //cMap.addMailcap("text/xml; ; x-java-content-handler=com.sun.mail.handlers.text_xml");
        //cMap.addMailcap("multipart/*; ; x-java-content-handler=com.sun.mail.handlers.multipart_mixed; x-java-fallback-entry=true");
        //cMap.addMailcap("message/rfc822; ; x-java-content-handler=com.sun.mail.handlers.message_rfc822");

        plugin = this;
        logger = getLogger();

        File dir = this.getDataFolder();
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        dataFolder = dir.getPath();

        File file = new File("lib", "mail.jar");
        if (!file.exists()) {
            logger.severe("Copying library files from jar... Reloading Plugin");
            dir = new File("lib");
            if (!dir.isDirectory()) {
                dir.mkdir();
            }
            this.saveResource("mail.jar", true);
            new File(dataFolder, "mail.jar").renameTo(file);
            PluginManager pm = Bukkit.getPluginManager();
            pm.disablePlugin(this);
            pm.enablePlugin(this);
            return;
        }

        file = new File(dataFolder, "email.properties");
        if (!file.exists()) {
            email = new Properties();
            email.setProperty("Username", "");
            email.setProperty("Password", "");
            email.setProperty("PasswordEncrypted", "");
            email.setProperty("POP3Host", "pop.gmail.com");
            email.setProperty("SMTPHost", "smtp.gmail.com");
            email.setProperty("SMTPPort", "587");
            email.setProperty("IMAPHost", "imap.gmail.com");
            email.setProperty("IMAPPort", "993");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                email.store(fos, null);
            } catch (Exception e) {
                logger.severe("Unable to create initial email.properties file");
            }
        }

        loadSettings();

        Econ.setupEconomy();

        dir = new File(dataFolder, "Users");
        if (!dir.isDirectory()) {
            dir.mkdir();
        } else {
            loadData();
        }

        /* Register Events */
        Bukkit.getPluginManager().registerEvents(new TextPlayerListener(), this);
        getServer().getLogger().addHandler(new LogListener());

        /* Register the command found in the plugin.yml */
        String command = (String) getDescription().getCommands().keySet().toArray()[0];
        CommandHandler handler = new CommandHandler(this, command);
        handler.registerCommands(TextCommand.class);
        handler.registerCommands(SendTextCommand.class);

        Properties version = new Properties();
        try {
            version.load(this.getResource("version.properties"));
        } catch (Exception ex) {
            logger.severe("version.properties file not found within jar");
        }
        logger.info("TextPlayer " + this.getDescription().getVersion() + " (Build "+version.getProperty("Build") + ") is enabled!");

        for (Player player : Bukkit.getOnlinePlayers()) {
            TextPlayerListener.online.add(player.getName());
        }

        for (User user : users.values()) {
            if (user.watchingServer) {
                TextPlayerMailReader.sendMsg(null, user, "TextPlayer Server Watcher", "Server has just come online");
            }
        }

        //Start checking mail if the email.properties file is filled out
        if (TextPlayerMailReader.username.equals("")) {
            logger.severe("Please create email account for email.properties");
        } else {
            TextPlayerMailReader.MailListener();
        }
    }

    /**
     * Loads settings from the config.properties file
     */
    public void loadSettings() {
        FileInputStream fis = null;
        try {
            //Copy the file from the jar if it is missing
            File file = new File(dataFolder, "config.properties");
            if (!file.exists()) {
                this.saveResource("config.properties", true);
            }

            //Load config file
            p = new Properties();
            fis = new FileInputStream(file);
            p.load(new FileInputStream(dataFolder + File.separator + "email.properties"));

            TextPlayerMailReader.username = loadValue("Username");
            String passToEncrypt = loadValue("Password");
            TextPlayerMailReader.pass = loadValue("PasswordEncrypted");

            TextPlayerMailReader.pop3host = loadValue("POP3Host");
            TextPlayerMailReader.smtphost = loadValue("SMTPHost");
            TextPlayerMailReader.smtpport = Integer.parseInt(loadValue("SMTPPort"));
            TextPlayerMailReader.imaphost = loadValue("IMAPHost");
            TextPlayerMailReader.imapport = Integer.parseInt(loadValue("IMAPPort"));
            TextPlayerMailReader.imap = !TextPlayerMailReader.imaphost.isEmpty();
            TextPlayerMailReader.imap = false;

            //Encrypt the password if it is not already encrypted
            if (!passToEncrypt.isEmpty()) {
                TextPlayerMailReader.pass = User.encrypter.encrypt(passToEncrypt);
                p.setProperty("PasswordEncrypted", TextPlayerMailReader.pass);
                p.setProperty("Password", "");

                //Save the email.properties file with the newly encrypted password
                p.store(new FileOutputStream(dataFolder + File.separator + "email.properties"), null);
            }
        } catch (Exception missingProp) {
            logger.severe("Failed to load email settings. This plugin may not function properly");
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
            logger.severe("Please regenerate the email.properties file");
            return null;
        }
    }

    /**
     * Loads properties for each User from save files
     */
    public static void loadData() {
        for (File file : new File(dataFolder, "Users" + File.separator).listFiles()) {
            String name = file.getName();
            if (name.endsWith(".properties")) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    //Load the Properties file for reading
                    Properties p = new Properties();
                    p.load(fis);
                    fis.close();

                    //Construct a new User using the file name
                    User user = new User(name.substring(0, name.length() - 11), p.getProperty("Email"));

                    user.emailIn = p.containsKey("EmailIn") ? p.getProperty("EmailIn") : "";

                    user.disableWhenLogged = Boolean.parseBoolean(p.getProperty("DisableWhenLogged"));
                    user.textLimit = Integer.parseInt(p.getProperty("TextLimit"));
                    user.textsSent = Integer.parseInt(p.getProperty("TextsSent"));
                    user.lastText = Integer.parseInt(p.getProperty("LastText"));

                    user.watchingServer = Boolean.parseBoolean(p.getProperty("WatchingServer"));
                    user.watchingErrors = Boolean.parseBoolean(p.getProperty("WatchingErrors"));

                    if (p.contains("MassTextOptOut")) {
                        user.massTextOptOut = Boolean.parseBoolean(p.getProperty("MassTextOptOut"));
                    }

                    String value = p.getProperty("WhiteList");
                    if (!value.isEmpty()) {
                        user.whiteList.addAll(Arrays.asList(value.split(", ")));
                    }

                    value = p.getProperty("WatchingPlayers").toLowerCase();
                    if (!value.isEmpty()) {
                        user.players.addAll(Arrays.asList(value.split(", ")));
                    }

                    value = p.getProperty("WatchingItems");
                    if (!value.isEmpty()) {
                        user.items.addAll(Arrays.asList(value.split(", ")));
                    }

                    value = p.getProperty("WatchingWords");
                    if (!value.isEmpty()) {
                        user.words.addAll(Arrays.asList(value.split(", ")));
                    }

                    users.put(user.name, user);
                } catch (Exception loadFailed) {
                    logger.severe("Failed to load " + name);
                    loadFailed.printStackTrace();
                }
            }
        }
    }

    /**
     * Writes the given User to save file
     * Old file is overwritten
     *
     * @param user The given User
     */
    public static void save(User user) {
        try {
            Properties p = new Properties();

            p.setProperty("Email", user.emailOut);
            p.setProperty("EmailIn", user.emailIn);
            p.setProperty("DisableWhenLogged", String.valueOf(user.disableWhenLogged));
            p.setProperty("TextLimit", String.valueOf(user.textLimit));
            p.setProperty("TextsSent", String.valueOf(user.textsSent));
            p.setProperty("LastText", String.valueOf(user.lastText));
            p.setProperty("WatchingServer", String.valueOf(user.watchingServer));
            p.setProperty("WatchingErrors", String.valueOf(user.watchingErrors));
            p.setProperty("MassTextOptOut", String.valueOf(user.massTextOptOut));

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
            File file = new File(dataFolder, "Users" + File.separator + user.name + ".properties");
            if (!file.exists()) {
                file.createNewFile();
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                p.store(fos, null);
            }
        } catch (Exception saveFailed) {
            logger.severe("Save Failed!");
            saveFailed.printStackTrace();
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
     * Returns the User with the given email
     *
     * @param email The email of the User you wish to find
     * @return The User with the given email or null if not found
     */
    public static User findUserByEmail(String email) {
        //Iterate through all Users to find the one with the given Name
        for (User user: users.values()) {
            if (!user.emailIn.isEmpty() && email.equals(User.encrypter.decrypt(user.emailIn))) {
                return user;
            }
        }

        //Return null because the User does not exist
        return null;
    }

    /**
     * Returns the User with the given Confirmation Code
     *
     * @param code The Confirmation Code for the User
     * @return The User with the given Code or null if not found
     */
    protected static User findUserByCode(int code) {
        code = -code;
        for (User user: users.values()) {
            if (user.textLimit == code) {
                return user;
            }
        }
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

    /**
     * Sends a message to all registered Users
     * The message is not sent to Users who opted out
     *
     * @param message The message to send
     */
    public static void massText(String message) {
        for (User user : TextPlayer.getUsers()) {
            if (!user.massTextOptOut) {
                TextPlayerMailReader.sendMsg(null, user, "Mass Text", message);
            }
        }
    }
}
