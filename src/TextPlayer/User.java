
package TextPlayer;

import java.io.FileInputStream;
import java.util.Properties;
import org.bukkit.entity.Player;

/**
 * @author Codisimus
 */
public class User {
    private static String world = TextPlayer.server.getWorlds().get(0).getName();
    private static Encrypter encrypter = new Encrypter("SeVenTy*7");
    protected String name;
    private String email = null;
    protected boolean disableWhenLogged = false;
    protected int textLimit = -1;
    protected int textsSent = 0;
    protected int lastText = 0;
    private String watchingUsers = ",";
    private String watchingItems = ",";
    private String watchingWords = "";
    protected boolean logged = false;

    protected User (String name, String email, boolean disableWhenLogged, int textLimit,
            int textsSent, int lastText, String users, String items, String words) {
        this.name = name;
        this.email = email;
        this.disableWhenLogged = disableWhenLogged;
        this.textLimit = textLimit;
        this.textsSent = textsSent;
        this.lastText = lastText;
        this.watchingUsers = users;
        this.watchingItems = items;
        this.watchingWords = words;
    }

    protected User (Player player, String network, String number) {
        String success = setEmail(number, network);
        player.sendMessage(success);
        name = player.getName();
    }
    
    protected boolean isAdmin() {
        if (!TextPlayer.perm.equals("permissions 3"))
            return false;
        else if (TextPlayer.permissions != null)
            return TextPlayer.permissions.getUserObject(world, name).hasPermission("textplayer.admin");
        return false;
    }

    protected boolean watchUser(String player) {
        player = player.toLowerCase().concat(",");
        if (watchingUsers.contains(player))
            return false;
        watchingUsers = watchingUsers.concat(player);
        return true;
    }

    protected boolean watchItem(String item) {
        item = item.toLowerCase().concat(",");
        if (watchingItems.contains(item))
            return false;
        watchingItems = watchingItems.concat(item);
        return true;
    }

    protected boolean watchWord(String word) {
        word = word.toLowerCase().concat(",");
        if (watchingWords.contains(word))
            return false;
        watchingWords = watchingWords.concat(word);
        return true;
    }

    protected boolean unwatchUser(String player) {
        player = player.toLowerCase().concat(",");
        if (watchingUsers.contains(player)) {
            watchingUsers = watchingUsers.replace(player, "");
            return true;
        }
        return false;
    }

    protected boolean unwatchItem(String item) {
        item = item.toLowerCase().concat(",");
        if (watchingUsers.contains(item)) {
            watchingUsers = watchingUsers.replace(item, "");
            return true;
        }
        return false;
    }

    protected boolean unwatchWord(String word) {
        word = word.toLowerCase().concat(",");
        if (watchingWords.contains(word)) {
            watchingWords = watchingWords.replace(word, "");
            return true;
        }
        return false;
    }

    protected boolean isWatchingUser(String player) {
        player = player.toLowerCase().concat(",");
        if (watchingUsers.contains(player))
            return true;
        return false;
    }

    protected boolean isWatchingItem(String item) {
        item = item.toLowerCase().concat(",");
        if (watchingUsers.contains(item))
            return true;
        return false;
    }

    protected boolean isWatchingWord(String msg) {
        msg = msg.toLowerCase();
        String[] words = watchingWords.toLowerCase().split(",");
        for (String word : words) {
            if (msg.contains(word))
                return true;
        }
        return false;
    }

    protected String getEmail() {
        return email;
    }

    protected String getWatchingUsers() {
        return watchingUsers;
    }

    protected String getWatchingItems() {
        return watchingItems;
    }

    protected String getWatchingWords() {
        return watchingWords;
    }

    protected String setEmail(String number, String network) {
        number = number.replaceAll("-", "");
        network = network.replaceAll("-", "");
        network = network.toLowerCase();
        String old = email;
        if (network.equals("email")) {
            if (number.contains("@") && number.contains("."))
                email = number;
            else
                return "Invalid e-mail address";
        }
        else {
            if (number.length() == 11)
                number = number.substring(1);
            if (number.length() != 10)
                return "Invalid number format";
            try {
                Properties p = new Properties();
                p.load(new FileInputStream("plugins/TextPlayer/sms.gateways"));
                if (Double.parseDouble(p.getProperty("Version")) < 0.9) {
                    TextPlayer.moveFile("sms.gateways");
                    return setEmail(number, network);
                }
                String gateway = p.getProperty(network, null);
                if (gateway == null)
                    return "Network not supported";
                else
                    email = gateway.replace("<number>", number);
            }
            catch (Exception ex) {
                TextPlayer.moveFile("sms.gateways");
                return setEmail(number, network);
            }
        }
        email = encrypter.encrypt(email);
        if (email.equals(old))
            return "That is already your current number";
        return "E-mail set to: "+encrypter.decrypt(email);
    }
}
