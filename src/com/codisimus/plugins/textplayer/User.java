package com.codisimus.plugins.textplayer;

import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.Properties;
import org.bukkit.entity.Player;

/**
 * A User holds the contact information for a Player and when they will be contacted
 * 
 * @author Codisimus
 */
public class User {
    public static String world = TextPlayer.server.getWorlds().get(0).getName();
    public static Encrypter encrypter = new Encrypter("SeVenTy*7"); 
    
    public String name;
    public String email; //An encrypted version of the User's email address
    public boolean disableWhenLogged = false; //If true, texts will only be sent when the Player is offline
    public int textLimit = -1;
    public int textsSent = 0;
    public int lastText = 0; //The day that the last text was sent to this User
    
    public boolean watchingServer = false;
    public LinkedList<String> players = new LinkedList<String>();
    public LinkedList<String> items = new LinkedList<String>();
    public LinkedList<String> words = new LinkedList<String>();
    
    public boolean online = true;

    /**
     * Constructs a new User with the given name and email
     * 
     * @param name The name of the Player
     * @param email The encrypted email address
     */
    public User (String name, String email) {
        this.name = name;
        this.email = email;
    }

    /**
     * Constructs a new User with the given un-encrypted contact information
     * 
     * @param player The Player the User represents
     * @param network The cell phone carrier or 'email'
     * @param number The phone number or email address
     */
    public User (Player player, String network, String number) {
        //Discover if the information is valid and format/encrypt
        String success = setEmail(number, network);
        
        player.sendMessage(success);
        name = player.getName();
    }
    
    /**
     * Returns true if this User has the textplayer.admin node
     * 
     * @return True if this User is an Admin
     */
    public boolean isAdmin() {
        //Check for the permission node if a Permission Plugin is present
        if (TextPlayer.permissions != null)
            return TextPlayer.permissions.has(name, "textplayer.admin", world);
        
        //Return whether the User is an OP
        return TextPlayer.server.getOfflinePlayer(name).isOp();
    }

    /**
     * Formats the given information into a valid email address
     * 
     * @param carrier The cell phone carrier or 'email'
     * @param address The phone number or email address
     * @return The message that will be sent to the Player
     */
    public final String setEmail(String carrier, String address) {
        String old = email;
        
        //Format the given Strings
        address = address.replaceAll("-", "");
        carrier = carrier.replaceAll("-", "").toLowerCase();
        
        if (carrier.equals("email"))
            //Check the format of the given email address
            if (address.contains("@") && address.contains("."))
                email = address;
            else
                return "Invalid e-mail address";
        else {
            //Return if the number is not 10 digits
            switch (address.length()) {
                case 10: break;
                case 11: address = address.substring(1); break; //Throw out the included country code
                default: return "Invalid number format";
            }
                
            try {
                Properties p = new Properties();
                p.load(new FileInputStream("plugins/TextPlayer/sms.gateways"));
                
                //Check if the gateways file is outdated
                if (Double.parseDouble(p.getProperty("Version")) < 1.0) {
                    TextPlayer.moveFile("sms.gateways");
                    return setEmail(carrier, address);
                }
                
                //Return if the carrier is not found
                String gateway = p.getProperty(carrier);
                if (gateway == null)
                    return "Carrier not supported";
                
                //Format the number into the correct email address
                email = gateway.replace("<number>", address);
            }
            catch (Exception ex) {
                TextPlayer.moveFile("sms.gateways");
                return setEmail(carrier, address);
            }
        }
        
        //Encrypt the new email address
        email = encrypter.encrypt(email);
        
        if (email.equals(old))
            return "That is already your current number";
        
        return "E-mail set to: "+encrypter.decrypt(email);
    }
}