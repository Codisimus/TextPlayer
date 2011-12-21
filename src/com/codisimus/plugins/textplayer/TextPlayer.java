package com.codisimus.plugins.textplayer;

import com.codisimus.plugins.textplayer.listeners.MailListener;
import com.codisimus.plugins.textplayer.listeners.BlockEventListener;
import com.codisimus.plugins.textplayer.listeners.CommandListener;
import com.codisimus.plugins.textplayer.listeners.PlayerEventListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
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
 * Loads Plugin and manages Data/Permissions
 *
 * @author Codisimus
 */
public class TextPlayer extends JavaPlugin {
    public static Permission permission;
    public static Server server;
    private static PluginManager pm;
    public static Encrypter encrypter = new Encrypter("SeVenTy*7");
    private static Properties p;
    public static LinkedList<User> users = new LinkedList<User>();

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
        
        loadData();

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

        for (User user: users)
            if (user.watchingServer)
                MailListener.sendMsg(null, user, "Server has just come online");
    }
    
    /**
     * Makes sure all needed files exist
     * 
     */
    private void checkFiles() {
        if (!new File("lib/mail.jar").exists())
            moveFile("mail.jar");
        
        if (!new File("plugins/TextPlayer/config.properties").exists())
            moveFile("config.properties");
        
        if (!new File("plugins/TextPlayer/sms.gateways").exists())
            moveFile("sms.gateways");
        
        if (!new File("plugins/TextPlayer/email.properties").exists()) {
            try {
                p = new Properties();
                
                p.setProperty("Username", "");
                p.setProperty("Password", "");
                p.setProperty("PasswordEncrypted", "");
                p.setProperty("SMTPHost", "smtp.gmail.com");
                p.setProperty("IMAPHost", "imap.gmail.com");
                p.setProperty("SMTPPort", "25");
                p.setProperty("IMAPPort", "993");
                
                FileOutputStream fos = new FileOutputStream("plugins/TextPlayer/email.properties");
                p.store(fos, null);
                fos.close();
            }
            catch (Exception e) {
            }
        }
        
        try {
            FileInputStream fis = new FileInputStream("plugins/TextPlayer/sms.gateways");
            p.load(fis);

            //Check if the gateways file is outdated
            if (Double.parseDouble(p.getProperty("Version")) < 1.0)
                moveFile("sms.gateways");
            
            fis.close();
        }
        catch (Exception e) {
        }
    }
    
    /**
     * Moves file from TextPlayer.jar to appropriate folder
     * Destination folder is created if it doesn't exist
     * 
     * @param fileName The name of the file to be moved
     */
    private static void moveFile(String fileName) {
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
            
            File efile = new File(destination, fileName);
            InputStream in = new BufferedInputStream(jar.getInputStream(entry));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(efile));
            byte[] buffer = new byte[2048];
            
            //Copy the file
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
            FileInputStream fis = new FileInputStream("plugins/TextPlayer/config.properties");
            p.load(fis);
            
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
            
            fis.close();
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
    private String loadValue(String key) {
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
    
    /**
     * Loads Users from the save file
     * 
     */
    private static void loadData() {
        String line = "";
        
        try {
            new File("plugins/TextPlayer/emails.save").createNewFile();
            BufferedReader bReader = new BufferedReader(new FileReader("plugins/TextPlayer/emails.save"));
            
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
                    if (data[6].contains("server,"))
                        data[6] = data[6].replaceAll("server,", ",");
                    if (data[6].contains(",,"))
                        data[6] = data[6].replaceAll(",,", ",");
                    if (data[6].equals(","))
                        data[6] = "none";

                    if (data.length > 6)
                        if (!data[6].equals("none")) 
                            user.players = new LinkedList(Arrays.asList(data[6].split(",")));

                    if (data.length > 7) {
                        //Update outdated save file
                        if (data[7].equals(","))
                            data[7] = "none";

                        if (!data[7].equals("none"))
                            user.items = new LinkedList(Arrays.asList(data[7].split(",")));
                    }
                    
                    if (data.length > 8) {
                        //Update outdated save file
                        if (data[8].equals(","))
                            data[8] = "none";

                        if (!data[8].equals("none"))
                            user.words = new LinkedList(Arrays.asList(data[8].split(",")));
                    }
                    
                    users.add(user);
                    
                    save();
                }
                else {
                    user.watchingServer = Boolean.parseBoolean(data[6]);
                    
                    user.players = new LinkedList(Arrays.asList(data[7].substring(1, data[7].length() - 1).split(", ")));

                    user.items = new LinkedList(Arrays.asList(data[8].substring(1, data[8].length() - 1).split(", ")));

                    user.words = new LinkedList(Arrays.asList(data[9].substring(1, data[9].length() - 1).split(", ")));

                    users.add(user);
                }
            }
            
            bReader.close();
        }
        catch (Exception loadFailed) {
            System.err.println("[TextPlayer] Load failed, saving turned off to prevent loss of data");
            System.err.println("[TextPlayer] Errored line: "+line);
            loadFailed.printStackTrace();
        }
    }

    /**
     * Writes Users to save file
     * Old file is overwritten
     */
    public static void save() {
        try {
            BufferedWriter bWriter = new BufferedWriter(new FileWriter("plugins/TextPlayer/emails.save"));
            
            for(User user: users) {
                //Writes User to file in the following format
                //name;email;disabledWhenLogged;textLimit;TextsSent;lastText;watchingServer;watchedPlayers;watchedItems;watchedWords;
                bWriter.write(user.name.concat(";"));
                bWriter.write(user.email.concat(";"));
                bWriter.write(user.disableWhenLogged+";");
                bWriter.write(user.textLimit+";");
                bWriter.write(user.textsSent+";");
                bWriter.write(user.lastText+";");
                bWriter.write(user.watchingServer+";");
                bWriter.write(user.players.toString().concat(";"));
                bWriter.write(user.items.toString().concat(";"));
                bWriter.write(user.words.toString().concat(";"));
                
                //Write each User on it's own line
                bWriter.newLine();
            }
            
            bWriter.close();
        }
        catch (Exception saveFailed) {
            System.err.println("[TextPlayer] Save Failed!");
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
        for(User user : users)
            if (user.name.equalsIgnoreCase(name))
                return user;
        
        //Return null because the User does not exist
        return null;
    }
}