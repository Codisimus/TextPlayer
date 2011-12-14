package com.codisimus.plugins.textplayer;

import com.codisimus.plugins.textplayer.listeners.MailListener;
import com.codisimus.plugins.textplayer.listeners.BlockEventListener;
import com.codisimus.plugins.textplayer.listeners.CommandListener;
import com.codisimus.plugins.textplayer.listeners.PlayerEventListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads Plugin and manages Permissions
 *
 * @author Codisimus
 */
public class TextPlayer extends JavaPlugin {
    public static String perm;
    public static Permission permission;
    public static Server server;
    public static PluginManager pm;
    public static Encrypter encrypter = new Encrypter("SeVenTy*7");
    public static Properties p;

    @Override
    public void onDisable () {
        //Stop checking for new mail
        MailListener.enabled = false;
        MailListener.loop = false;
        System.out.println("[TextPlayer] Checking for new mail disabled until server start");
    }

    /**
     * Calls methods to load this Plugin when it is enabled
     *
     */
    @Override
    public void onEnable () {
        server = getServer();
        pm = server.getPluginManager();
        
        checkFiles();
        loadSettings();
        
        //Find Permissions
        RegisteredServiceProvider<Permission> permissionProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null)
            permission = permissionProvider.getProvider();
        
        //Find Economy
        RegisteredServiceProvider<Economy> economyProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null)
            Econ.economy = economyProvider.getProvider();
        
        SaveSystem.load();

        //Start checking mail if the email.properties file is filled out
        if (MailListener.username.equals(""))
            System.err.println("[TextPlayer] Please create email account for email.properties");
        else {
            MailListener.checkMail();
            System.out.println("[TextPlayer] Checking for new mail...");
        }

        //Register Events
        PlayerEventListener playerListener = new PlayerEventListener();
        BlockEventListener blockListener = new BlockEventListener();
        pm.registerEvent(Type.PLAYER_CHAT, playerListener, Priority.Monitor, this);
        pm.registerEvent(Type.PLAYER_JOIN, playerListener, Priority.Monitor, this);
        pm.registerEvent(Type.PLAYER_QUIT, playerListener, Priority.Monitor, this);
        pm.registerEvent(Type.BLOCK_PLACE, blockListener, Priority.Monitor, this);
        pm.registerEvent(Type.BLOCK_IGNITE, blockListener, Priority.Monitor, this);
        getCommand("text").setExecutor(new CommandListener());
        
        System.out.println("TextPlayer "+this.getDescription().getVersion()+" is enabled!");

        for (User user: SaveSystem.users)
            if (user.watchingServer)
                MailListener.sendMsg(null, user, "Server has just come online");
    }
    
    /**
     * Makes sure all needed files exist
     * 
     */
    public void checkFiles() {
        if (!new File("lib/mail.jar").exists())
            moveFile("mail.jar");
        
        if (!new File("plugins/TextPlayer/config.properties").exists())
            moveFile("config.properties");
        
        if (!new File("plugins/TextPlayer/sms.gateways").exists())
            moveFile("sms.gateways");
        
        if (!new File("plugins/TextPlayer/email.properties").exists()) {
            p = new Properties();
            p.setProperty("Username", "");
            p.setProperty("Password", "");
            p.setProperty("PasswordEncrypted", "");
            p.setProperty("SMTPHost", "smtp.gmail.com");
            p.setProperty("IMAPHost", "imap.gmail.com");
            p.setProperty("SMTPPort", "25");
            p.setProperty("IMAPPort", "993");
            try {
                p.store(new FileOutputStream("plugins/TextPlayer/email.properties"), null);
            }
            catch (Exception e) {
            }
        }
    }
    
    /**
     * Moves file from TextPlayer.jar to appropriate folder
     * Destination folder is created if it doesn't exist
     * 
     * @param fileName The name of the file to be moved
     */
    public static void moveFile(String fileName) {
        try {
            //Retrieve file from this plugin's .jar
            JarFile jar = new JarFile("plugins/TextPlayer.jar");
            ZipEntry entry = jar.getEntry(fileName);
            
            String destination = "plugins/TextPlayer/";
            
            //If the file being moved is mail.jar move to lib/ and ask the Admin to reload
            if (fileName.equals("mail.jar")) {
                System.err.println("[TextPlayer] Moving Files... Please Reload Server");
                destination = "lib/";
            }
            
            //Create the destination folder if it does not exist
            File file = new File(destination.substring(0, destination.length()-1));
            if (!file.exists())
                file.mkdir();
            
            //Copy the file
            File efile = new File(destination, fileName);
            InputStream in = new BufferedInputStream(jar.getInputStream(entry));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(efile));
            byte[] buffer = new byte[2048];
            while (true) {
                int nBytes = in.read(buffer);
                if (nBytes <= 0)
                    break;
                out.write(buffer, 0, nBytes);
            }
            out.flush();
            out.close();
            in.close();
        }
        catch (Exception moveFailed) {
            System.err.println("[TextPlayer] File Move Failed!");
            moveFailed.printStackTrace();
        }
    }
    
    /**
     * Loads settings from the config.properties file
     * 
     */
    public void loadSettings() {
        p = new Properties();
        try {
            p.load(new FileInputStream("plugins/TextPlayer/config.properties"));
            
            MailListener.interval = Integer.parseInt(loadValue("CheckMailInterval"));
            MailListener.refresh = Integer.parseInt(loadValue("RefreshIMAPConnection"));

            MailListener.notify = Boolean.parseBoolean(loadValue("NotifyInServerLog"));
            MailListener.debug = Boolean.parseBoolean(loadValue("Debug"));

            Econ.cost = Integer.parseInt(loadValue("CostToText"));
            Econ.costAdmin = Integer.parseInt(loadValue("CostToTextAnAdmin"));

            p.load(new FileInputStream("plugins/TextPlayer/email.properties"));

            MailListener.username = loadValue("Username");
            String passToEncrypt = loadValue("Password");
            MailListener.pass = loadValue("PasswordEncrypted");

            MailListener.smtphost = loadValue("SMTPHost");
            MailListener.imaphost = loadValue("IMAPHost");
            MailListener.smtpport = Integer.parseInt(loadValue("SMTPPort"));
            MailListener.imapport = Integer.parseInt(loadValue("IMAPPort"));

            //Encrypt the password if it is not already encrypted
            if (!passToEncrypt.isEmpty()) {
                MailListener.pass = encrypter.encrypt(passToEncrypt);
                p.setProperty("PasswordEncrypted", MailListener.pass);
                p.setProperty("Password", "");

                //Save the email.properties file with the newly encrypted password
                p.store(new FileOutputStream("plugins/TextPlayer/email.properties"), null);
            }
        }
        catch (Exception missingProp) {
            System.err.println("Failed to load ButtonWarp "+this.getDescription().getVersion());
            missingProp.printStackTrace();
        }
    }

    /**
     * Loads the given key and prints an error if the key is missing
     *
     * @param key The key to be loaded
     * @return The String value of the loaded key
     */
    public String loadValue(String key) {
        //Print an error if the key is not found
        if (!p.containsKey(key)) {
            System.err.println("[TextPlayer] Missing value for "+key);
            System.err.println("[TextPlayer] Please regenerate the config.properties file");
            System.err.println("[TextPlayer] If still getting this error, regenerate the email.properties file");
        }
        
        return p.getProperty(key);
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
}
