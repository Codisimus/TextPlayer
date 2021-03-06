package com.codisimus.plugins.textplayer;

import com.codisimus.plugins.textplayer.SMSGateways.Carrier;
import java.util.LinkedList;
import java.util.Random;
import org.bukkit.entity.Player;

/**
 * A User holds the contact information for a Player and when they will be contacted
 *
 * @author Codisimus
 */
public class User {
    static Encrypter encrypter = new Encrypter("SeVenTy*7");
    static Random random = new Random();

    public String name;
    public String emailOut; //An encrypted version of the User's email address
    public String emailIn = "";
    public boolean disableWhenLogged = true; //If true, texts will only be sent when the Player is offline
    public boolean massTextOptOut = false; //If true, mass text will not be sent to the Player
    public int textLimit = -1;
    public int textsSent = 0;
    public int lastText = 0; //The day that the last text was sent to this User
    public final LinkedList<String> whiteList = new LinkedList<>();

    public boolean watchingServer = false;
    public boolean watchingErrors = false;
    public final LinkedList<String> players = new LinkedList<>();
    public final LinkedList<String> items = new LinkedList<>();
    public final LinkedList<String> words = new LinkedList<>();

    public boolean chatMode = false;

    /**
     * Constructs a new User with the given name
     *
     * @param name The name of the Player
     */
    public User(String name) {
        this.name = name;
        emailOut = "";
    }

    /**
     * Constructs a new User with the given name and email
     *
     * @param name The name of the Player
     * @param email The encrypted email address
     */
    public User(String name, String email) {
        this.name = name;
        this.emailOut = email;
    }

    /**
     * Returns true if this User has the textplayer.admin node
     *
     * @return True if this User is an Admin
     */
    public boolean isAdmin() {
        return TextPlayer.admins.contains(name);
    }

    /**
     * Formats the given information into a valid email address
     *
     * @param player The Player to send messages to
     * @param carrierName The cell phone carrier or 'email'
     * @param address The phone number or email address
     */
    public void setEmail(Player player, String carrierName, String address) {
        String old = emailOut;

        if (carrierName.equals("email")) {
            //Verify the format of the given email address
            if (address.matches("^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$")) {
                emailOut = address;
            } else {
                player.sendMessage("§4Invalid e-mail address");
                return;
            }
        } else {
            //Return if the carrier is not found
            Carrier carrier;
            try {
                carrier = Carrier.valueOf(carrierName.toLowerCase());
            } catch (IllegalArgumentException notSupported) {
                player.sendMessage("§4Carrier §6" + carrierName
                        + " §4not supported, type §2/text list carriers"
                        + "§f for a list of supported Carriers");
                return;
            }

            //Remove all non-digits from the address
            address = address.replaceAll("[^\\d]", "");

            //Return if the number is not 10 digits
            switch (address.length()) {
            case 11: address = address.substring(1); //Throw out the included country code
            case 10: break;
            default:
                player.sendMessage("§4Invalid number format§f: §5Correct format is §6123-456-7890");
                return;
            }

            //Format the email address with the given number and carrier
            emailOut = SMSGateways.format(address, carrier);
        }

        //Encrypt the new email address
        emailOut = encrypter.encrypt(emailOut);

        //Return if it is not a new email address
        if (emailOut.equals(old)) {
            player.sendMessage("§4That is already your current E-mail");
            return;
        }

        player.sendMessage("§5E-mail set to§f: §6" + encrypter.decrypt(emailOut));

        //Generate a unique four digit confirmation code
        int confirmationCode = random.nextInt(9000) + 1000;
        while (TextPlayer.findUserByCode(confirmationCode) != null) {
            confirmationCode = random.nextInt(9000) + 1000;
        }

        //Send confirmation text
        player.sendMessage("§5Sending Confirmation Text...");
        TextPlayerMailReader.sendMsg(player, this, "[TextPlayer] Confirmation Code", "Reply '"
                + confirmationCode + "' to link this number to " + name);

        //Set the User as not verified
        textLimit = -confirmationCode;
        save();
    }

    /**
     * Returns true if the Player is allowed to text this User
     *
     * @param player The name of the Player who may be white listed
     * @return True if the Player is allowed to text this User
     */
    public boolean isWhiteListed(String player) {
        return whiteList.isEmpty() || whiteList.contains(player);
    }

    /**
     * Sends a text message (email) to the User
     *
     * @param subject The subject of the message
     * @param msg The message that will be emailed
     */
    public void sendText(String subject, String msg) {
        TextPlayerMailReader.sendMsg(null, this, subject, msg);
    }

    /**
     * Writes User to save file
     */
    public void save() {
        TextPlayer.save(this);
    }
}
