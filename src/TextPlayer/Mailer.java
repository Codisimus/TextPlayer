
package TextPlayer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import sun.misc.BASE64Decoder;

/**
 *
 * @author Codisimus
 */
public class Mailer {
    protected static boolean notify;
    protected static String smtphost;
    protected static String imaphost;
    protected static int smtpport;
    protected static int imapport;
    protected static String username;
    protected static String pass;
    protected static int interval;
    protected static int refresh;
    private static BASE64Decoder decoder = new BASE64Decoder();

    public static void sendMsg(final Player player, final User user, final String text) {
        if (notify)
            System.out.println("Sending Message...");
        if (player != null)
            player.sendMessage("Sending Message...");
        Thread send = new Thread() {
            @Override
            public void run() {
                String msg = text;
                if (user.disableWhenLogged)
                    if (user.logged)
                        if (TextPlayer.server.getPlayer(user.name) != null) {
                            if (notify)
                                System.out.println("User is currently online");
                            if (player != null)
                                player.sendMessage("User is currently online");
                            return;
                        }
                if (user.textLimit < 0 && !(msg.startsWith("Reply 'enable' to link")
                        || msg.startsWith("Texts to this number have been disabled"))) {
                    if (notify)
                        System.out.println("User's Number/Email has not been verified");
                    if (player != null)
                        player.sendMessage("User's Number/Email has not been verified");
                    return;
                }
                if (user.textLimit > 0) {
                    int day = Calendar.DAY_OF_YEAR;
                    if (user.lastText != day) {
                        user.lastText = day;
                        user.textsSent = 0;
                        SaveSystem.saveUsers();
                    }
                    if (user.textsSent >= user.textLimit) {
                        if (player != null) {
                            if (notify)
                                System.out.println("User maxed out their text limit");
                            if (player != null)
                                player.sendMessage("User maxed out their text limit");
                            return;
                        }
                    }
                    else {
                        user.textsSent = user.textsSent + 1;
                        if (user.textLimit - user.textsSent == 1)
                            msg = text.concat(" *1 txt left 4 2day");
                        else if (user.textLimit - user.textsSent == 0)
                            msg = text.concat(" *last txt 4 2day");
                        SaveSystem.saveUsers();
                    }
                }
                try {
                    Properties props = System.getProperties();
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.smtp.auth", "true");
                    Session session = Session.getDefaultInstance(props, 
                            new TextPlayerAuthenticator(username, TextPlayer.encrypter.decrypt(pass)));
                    //session.setDebug(true);
                    MimeMessage message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(username));
                    message.setRecipient(Message.RecipientType.TO, new InternetAddress(TextPlayer.encrypter.decrypt(user.getEmail())));
                    message.setText(msg);
                    Transport transport = session.getTransport("smtp");
                    transport.connect(smtphost, smtpport, username, TextPlayer.encrypter.decrypt(pass));
                    transport.sendMessage(message, message.getAllRecipients());
                    transport.close();
                    if (notify)
                        System.out.println("Message Sent!");
                    if (player != null)
                        player.sendMessage("Message Sent!");
                }
                catch (Exception sendFailed) {
                    sendFailed.printStackTrace();
                    if (notify)
                        System.out.println("Send Failed");
                    if (player != null)
                        player.sendMessage("Send Failed");
                }
            }
        };
        send.start();
    }
    
    protected static void checkMail() {
        //Start a new Thread
        Thread reconnect = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        //Start a new Thread
                        Thread check = new Thread() {
                            @Override
                            public void run() {
                                try {
                                    while (true) {
                                        Session session = Session.getDefaultInstance(System.getProperties(), 
                                                new TextPlayerAuthenticator(username, TextPlayer.encrypter.decrypt(pass)));
                                        Store store = session.getStore("imaps");
                                        store.connect(imaphost, imapport, username, TextPlayer.encrypter.decrypt(pass));
                                        Folder inbox = store.getFolder("Inbox");
                                        boolean loop = true;
                                        while (loop) {
                                            if (interval != 0)
                                                loop = false;
                                            while (inbox.getMessageCount() > 0) {
                                                inbox.open(Folder.READ_WRITE);
                                                Message[] messages = inbox.getMessages();
                                                for (Message message : messages) {
                                                    try {
                                                        User user = null;
                                                        Address[] addresses = message.getFrom();
                                                        LinkedList<User> users = SaveSystem.getUsers();
                                                        for (Address address : addresses) {
                                                            String from = address.toString().toLowerCase();
                                                            for(User tempUser : users) {
                                                                String email = TextPlayer.encrypter.decrypt(tempUser.getEmail()).toLowerCase();
                                                                if (from.contains(email)) {
                                                                    user = tempUser;
                                                                }
                                                            }
                                                        }
                                                        String msg = cleanUp(getMsg(message));
                                                        String[] split = msg.split(" ");
                                                        if (split[0].equals("enable") || split[0].equals("'enable'")) {
                                                            user.textLimit = 0;
                                                            SaveSystem.saveUsers();
                                                            sendMsg(null, user, "Number/Email linked to "+user.name);
                                                        }
                                                        else if (user.textLimit == -1)
                                                            sendMsg(null, user, "Reply 'enable' to link this number to "+user.name);
                                                        else if (split[0].equals("stop") || split[0].equals("disable")) {
                                                            sendMsg(null, user, "Texts to this number have been disabled, To receive texts reply 'enable'");
                                                            user.textLimit = -1;
                                                            SaveSystem.saveUsers();
                                                        }
                                                        else if (split[0].equals("pl") || split[0].equals("playerlist") ||
                                                                    split[0].equals("players") || split[0].equals("who")) {
                                                            String list = "Player Count: "+TextPlayer.server.getOnlinePlayers().length;
                                                            for (Player player : TextPlayer.server.getOnlinePlayers())
                                                                list = list.concat(", "+player.getName());
                                                            sendMsg(null, user, list);
                                                        }
                                                        else if (split[0].equals("find")) {
                                                            Player player = TextPlayer.server.getPlayer(split[1].trim());
                                                            String status = "online";
                                                            if (player == null)
                                                                status = "offline";
                                                            sendMsg(null, user, player.getName()+" is currently "+status);
                                                        }
                                                        else if (split[0].equals("tell")) {
                                                            String text = msg.substring(split[0].length() + split[1].length() + 1);
                                                            Player player = TextPlayer.server.getPlayer(split[1]);
                                                            if (player == null)
                                                                sendMsg(null, user, player.getName()+" is currently offline");
                                                            else
                                                                player.sendMessage("§5Text from "+user.name+":§f"+text);
                                                        } 
                                                        else if (split[0].equals("text")) {
                                                            String text = msg.substring(split[0].length() + split[1].length() + 1);
                                                            User user2 = SaveSystem.findUser(split[1]);
                                                            if (user2 == null)
                                                                sendMsg(null, user, split[1]+" does not have a TextPlayer account");
                                                            else
                                                                sendMsg(null, user, "Text from "+user.name+":"+text);
                                                        }
                                                        else if (split[0].equals("say")) {
                                                            System.out.println("[TextPlayer] "+user.name+":"+msg.substring(3));
                                                            TextPlayer.server.broadcastMessage("§5[TextPlayer] "+user.name+":§f"+msg.substring(3));
                                                        }
                                                        else if (user.isAdmin())
                                                            if (split[0].equals("rl"))
                                                                TextPlayer.server.reload();
                                                            else
                                                                TextPlayer.server.dispatchCommand(getSender(user), msg);
                                                    }
                                                    catch (Exception e) {
                                                        if (notify)
                                                            System.out.println("Error reading email, Message thrown out");
                                                    }
                                                    message.setFlag(Flags.Flag.DELETED, true);
                                                }
                                                inbox.close(true);
                                            }
                                        }
                                        store.close();
                                        Thread.currentThread().sleep(interval * 1000);
                                    }
                                }
                                catch (InterruptedException refresh) {
                                }
                                catch (Exception ex) {
                                    System.out.println("Could not read incoming mail!");
                                    ex.printStackTrace();
                                }
                            }
                        };
                        check.start();
                        Thread.currentThread().sleep(refresh * 3600000);
                        check.interrupt();
                        if (notify)
                            System.out.println("IMAP Connection Refreshed");
                    }
                }
                catch (Exception ex) {
                }
            }
        };
        reconnect.start();
    }
    
    private static String getMsg(Message message) throws Exception {
        if (message.isMimeType("multipart/*")) {
            InputStreamReader isr = new InputStreamReader(message.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line = br.readLine();
            while (line != null)
                if (line.contains("<br>"))
                    return line.replaceAll("<br>", "\n");
                else {
                    line = new String(decoder.decodeBuffer(line));
                    if (line.contains("<br>"))
                        return line.replaceAll("<br>", "\n");
                    else
                        line = br.readLine();
                }
        }
        else if (message.isMimeType("text/*"))
            return s2S(message.getInputStream());
        return "";
    }
    
    private static String s2S(InputStream is) throws Exception {
        if (is != null) {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1)
                    writer.write(buffer, 0, n);
            }
            finally {
                is.close();
            }
            return writer.toString();
        }
        return "";
    }
    
    private static String cleanUp(String msg) {
        if (msg.contains("RE:"))
            msg = msg.replaceAll("RE:", "");
        while (msg.startsWith(" ") || msg.startsWith("/") || msg.startsWith("\n"))
            msg = msg.substring(1);
        String[] cleanUp = msg.split("\n");
        msg = cleanUp[0].trim();
        msg = msg.substring(0, 1).toLowerCase().concat(msg.substring(1));
        return msg;
    }
    
    private static CommandSender getSender(final User user) {
        return new CommandSender() {
            @Override
            public void sendMessage(String string) {
                sendMsg(null, user, string);
            }

            @Override
            public boolean isOp() {
                return user.isAdmin();
            }

            @Override
            public Server getServer() {
                return TextPlayer.server;
            }
            
            @Override
            public String getName() {
                return user.name;
            }

            @Override
            public boolean isPermissionSet(String string) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public boolean isPermissionSet(Permission prmsn) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public boolean hasPermission(String string) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public boolean hasPermission(Permission prmsn) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public PermissionAttachment addAttachment(Plugin plugin, String string, boolean bln) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public PermissionAttachment addAttachment(Plugin plugin) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public PermissionAttachment addAttachment(Plugin plugin, String string, boolean bln, int i) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public PermissionAttachment addAttachment(Plugin plugin, int i) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void removeAttachment(PermissionAttachment pa) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void recalculatePermissions() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Set<PermissionAttachmentInfo> getEffectivePermissions() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void setOp(boolean bln) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
    }
}
