
package TextPlayer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.tehkode.permissions.PermissionManager;

/**
 *
 * @author Codisimus
 */
public class TextPlayer extends JavaPlugin {
    protected static String perm;
    protected static PermissionManager permissions;
    protected static Server server;
    protected static PluginManager pm;
    protected static Encrypter encrypter = new Encrypter("SeVenTy*7");
    private static Properties p;

    @Override
    public void onDisable () {
    }

    @Override
    public void onEnable () {
        server = getServer();
        pm = server.getPluginManager();
        checkFiles();
        loadSettings();
        SaveSystem.loadUsers();
        if (Mailer.username.equals(""))
            System.err.println("[TextPlayer] Please create email account for email.properties");
        else
            Mailer.checkMail();
        registerEvents();
        System.out.println("TextPlayer "+this.getDescription().getVersion()+" is enabled!");
        LinkedList<User> users = SaveSystem.getUsers();
        for (User user : users)
            if (user.isWatchingUser("server"))
                Mailer.sendMsg(null, user, "Server has just come online");
    }
    
    private void checkFiles() {
        File file = new File("lib/mail.jar");
        if (!file.exists())
            moveFile("mail.jar");
        file = new File("plugins/TextPlayer/config.properties");
        if (!file.exists())
            moveFile("config.properties");
        file = new File("plugins/TextPlayer/sms.gateways");
        if (!file.exists())
            moveFile("sms.gateways");
        file = new File("plugins/TextPlayer/email.properties");
        if (!file.exists()) {
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
    
    protected static void moveFile(String fileName) {
        try {
            JarFile jar = new JarFile("plugins/TextPlayer.jar");
            ZipEntry entry = jar.getEntry(fileName);
            String destination = "plugins/TextPlayer/";
            if (fileName.equals("mail.jar")) {
                System.err.println("[TextPlayer] Moving Files... Please Reload Server");
                destination = "lib/";
            }
            File file = new File(destination.substring(0, destination.length()-1));
            if (!file.exists())
                file.mkdir();
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
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadSettings() {
        p = new Properties();
        try {
            p.load(new FileInputStream("plugins/TextPlayer/config.properties"));
        }
        catch (Exception e) {
        }
        Mailer.interval = Integer.parseInt(loadValue("CheckMailInterval"));
        Mailer.refresh = Integer.parseInt(loadValue("RefreshIMAPConnection"));
        Mailer.notify = Boolean.parseBoolean(loadValue("NotifyInServerLog"));
        Register.economy = loadValue("Economy");
        Register.cost = Integer.parseInt(loadValue("CostToText"));
        Register.costAdmin = Integer.parseInt(loadValue("CostToTextAnAdmin"));
        PluginListener.useOP = Boolean.parseBoolean(loadValue("UseOP"));
        p = new Properties();
        try {
            p.load(new FileInputStream("plugins/TextPlayer/email.properties"));
        }
        catch (Exception e) {
        }
        Mailer.username = loadValue("Username");
        String passToEncrypt = loadValue("Password");
        Mailer.pass = loadValue("PasswordEncrypted");
        Mailer.smtphost = loadValue("SMTPHost");
        Mailer.imaphost = loadValue("IMAPHost");
        Mailer.smtpport = Integer.parseInt(loadValue("SMTPPort"));
        Mailer.imapport = Integer.parseInt(loadValue("IMAPPort"));
        if (!passToEncrypt.isEmpty()) {
            Mailer.pass = encrypter.encrypt(passToEncrypt);
            p.setProperty("PasswordEncrypted", Mailer.pass);
            p.setProperty("Password", "");
            try {
                p.store(new FileOutputStream("plugins/TextPlayer/email.properties"), null);
            }
            catch (Exception e) {
            }
        }
    }

    private String loadValue(String key) {
        if (!p.containsKey(key)) {
            System.err.println("[TextPlayer] Missing value for "+key);
            System.err.println("[TextPlayer] Please regenerate the config.properties file");
            System.err.println("[TextPlayer] If still getting this error, regenerate the email.properties file");
        }
        return p.getProperty(key);
    }
    
    /**
     * Registers events for the TextPlayer Plugin
     *
     */
    private void registerEvents() {
        TextPlayerPlayerListener playerListener = new TextPlayerPlayerListener();
        TextPlayerBlockListener blockListener = new TextPlayerBlockListener();
        pm.registerEvent(Event.Type.PLUGIN_ENABLE, new PluginListener(), Priority.Monitor, this);
        pm.registerEvent(Type.PLAYER_COMMAND_PREPROCESS, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_CHAT, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.BLOCK_PLACE, blockListener, Priority.Normal, this);
        pm.registerEvent(Type.BLOCK_IGNITE, blockListener, Priority.Normal, this);
    }

    public static boolean hasPermission(Player player, String type) {
        if (permissions != null)
            return permissions.has(player, "textplayer."+type);
        else if (type.equals("free"))
            return false;
        return true;
    }
    
    public static User getUser(String name) {
        return SaveSystem.findUser(name);
    }
}
