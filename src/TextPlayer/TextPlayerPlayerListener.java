
package TextPlayer;

import java.util.LinkedList;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 *
 * @author Codisimus
 */
public class TextPlayerPlayerListener extends PlayerListener {

    @Override
    public void onPlayerCommandPreprocess (PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage();
        String[] split = msg.split(" ");
        if (split[0].equals("/text")) {
            event.setCancelled(true);
            try {
                if (split[1].equals("set")) {
                    if (!TextPlayer.hasPermission(player, "use")) {
                        player.sendMessage("You do not have permission to do that");
                        return;
                    }
                    User user = SaveSystem.findUser(player.getName());
                    if (user == null) {
                        user = new User(player, split[2], split[3]);
                        if (user.getEmail() != null) {
                            player.sendMessage("Sending Confirmation Text...");
                            Mailer.sendMsg(player, user, "Reply 'enable' to link this number to "+user.name);
                            SaveSystem.addUser(user);
                        }
                    }
                    else {
                        String success = user.setEmail(split[3], split[2]);
                        player.sendMessage(success);
                        if (success.startsWith("Email")) {
                            player.sendMessage("Sending Confirmation Text...");
                            Mailer.sendMsg(player, user, "Reply 'enable' to link this number to "+user.name);
                            user.textLimit = -1;
                            SaveSystem.saveUsers();
                        }
                    }
                }
                else if (split[1].equals("list")) {
                    if (split[2].equals("carriers")) {
                        if (!TextPlayer.hasPermission(player, "use")) {
                            player.sendMessage("You do not have permission to do that");
                            return;
                        }
                        player.sendMessage("§5Supported Carriers:");
                        player.sendMessage("§2At&t, Bell, BeeLine, Bouygues, Cricket, D1, E-Plus, Etisalat, Fido");
                        player.sendMessage("§2Koodo, LMT, MetroPCS, Mobistar, NetCom, Optimus, Optus, Orange, O2-UK");
                        player.sendMessage("§2O2-Germany, Rogers, SFR, SoftBank, Sprint, Starhub, Sunrise, Swisscom");
                        player.sendMessage("§2TDC, Telecom, Telenor, Tele2, Telia, Telstra, Telus, Three, TMN");
                        player.sendMessage("§2T-Mobile, T-Mobile-Czech, US-Cellular, Verizon, Virgin-Mobile");
                        player.sendMessage("§2Virgin-Mobile-Canada, Vivo, Vodafone-Germany, Vodafone-Greece");
                        player.sendMessage("§2Vodafone-Italy, Vodafone-UK");
                        player.sendMessage("§bIf your cell phone provider is not listed,");
                        player.sendMessage("§bhave an admin contact Codisimus about adding it.");
                    }
                    else if(split[2].equals("users")) {
                        if (!TextPlayer.hasPermission(player, "listusers")) {
                            player.sendMessage("You do not have permission to do that");
                            return;
                        }
                        String userList = "";
                        LinkedList<User> users = SaveSystem.getUsers();
                        for(User user : users)
                            userList = userList.concat(user.name+", ");
                        player.sendMessage("§eCurrent Users:");
                        player.sendMessage("§2"+userList);
                    }
                    else if(split[2].equals("watch")) {
                        if (!TextPlayer.hasPermission(player, "use")) {
                            player.sendMessage("You do not have permission to do that");
                            return;
                        }
                        User user = SaveSystem.findUser(player.getName());
                        player.sendMessage("§2Watching Users:");
                        player.sendMessage(user.getWatchingUsers().substring(1).replaceAll(",", ", "));
                        player.sendMessage("§2Watching Words:");
                        player.sendMessage(user.getWatchingWords().substring(1).replaceAll(",", ", "));
                    }
                }
                else if (split[1].equals("watch")) {
                    if (!TextPlayer.hasPermission(player, "watch."+split[2])) {
                        player.sendMessage("You do not have permission to do that");
                        return;
                    }
                    User user = SaveSystem.findUser(player.getName());
                    if (user == null)
                        player.sendMessage("You must first add your contact info");
                    else
                        if (split[2].equals("server"))
                            if (user.watchUser("server")) {
                                player.sendMessage("You will be alerted when Server come online");
                                SaveSystem.saveUsers();
                            }
                            else
                                player.sendMessage("You are already watching "+split[3]);
                        else if(split[2].equals("user"))
                            if (user.watchUser(split[3])) {
                                player.sendMessage("Now watching "+split[3]);
                                SaveSystem.saveUsers();
                            }
                            else
                                player.sendMessage("You are already watching "+split[3]);
                        else if (split[2].equals("item"))
                            if (user.watchItem(split[3])) {
                                player.sendMessage("Now watching "+split[3]);
                                SaveSystem.saveUsers();
                            }
                            else
                                player.sendMessage("You are already watching "+split[3]);
                        else if (split[2].equals("word"))
                            if (user.watchWord(split[3])) {
                                player.sendMessage("Now watching "+split[3]);
                                SaveSystem.saveUsers();
                            }
                            else
                                player.sendMessage("You are already watching "+split[3]);
                }
                else if (split[1].equals("unwatch")) {
                    if (!TextPlayer.hasPermission(player, "watch."+split[2])) {
                        player.sendMessage("You do not have permission to do that");
                        return;
                    }
                    User user = SaveSystem.findUser(player.getName());
                    if (user == null)
                        player.sendMessage("You must first add your contact info");
                    else
                        if (split[2].equals("server"))
                            if (user.unwatchUser("server")) {
                                player.sendMessage("No longer watching the server");
                                SaveSystem.saveUsers();
                            }
                            else
                                player.sendMessage("You were not watching the server");
                        else if (split[2].equals("user"))
                            if (user.unwatchUser(split[3])) {
                                player.sendMessage("No longer watching "+split[3]);
                                SaveSystem.saveUsers();
                            }
                            else
                                player.sendMessage("You were not watching "+split[3]);
                        else if (split[2].equals("item"))
                            if (user.unwatchItem(split[3])) {
                                player.sendMessage("No longer watching "+split[3]);
                                SaveSystem.saveUsers();
                            }
                            else
                                player.sendMessage("You were not watching "+split[3]);
                        else
                            throw new Exception();
                }
                else if (split[1].startsWith("limit")) {
                    if (!TextPlayer.hasPermission(player, "use")) {
                        player.sendMessage("You do not have permission to do that");
                        return;
                    }
                    User user = SaveSystem.findUser(player.getName());
                    if (user == null)
                        player.sendMessage("You must first add your contact info");
                    else {
                        user.textLimit = Integer.parseInt(split[2]);
                        player.sendMessage("You will receive no more than "+split[2]+" texts each day");
                    }
                }
                else if (split[1].equals("disable")) {
                    if (!TextPlayer.hasPermission(player, "use")) {
                        player.sendMessage("You do not have permission to do that");
                        return;
                    }
                    User user = SaveSystem.findUser(player.getName());
                    if (user == null)
                        player.sendMessage("You must first add your contact info");
                    else {
                        user.disableWhenLogged = true;
                        player.sendMessage("You will not receive texts when you are logged on");
                    }
                }
                else if (split[1].equals("enable")) {
                    if (!TextPlayer.hasPermission(player, "use")) {
                        player.sendMessage("You do not have permission to do that");
                        return;
                    }
                    User user = SaveSystem.findUser(player.getName());
                    if (user == null)
                        player.sendMessage("You must first add your contact info");
                    else {
                        user.disableWhenLogged =false;
                        player.sendMessage("You will receive texts when you are logged on");
                    }
                }
                else if (split[1].equals("help")) {
                    throw new Exception();
                }
                else {
                    User to = SaveSystem.findUser(split[1]);
                    if (to == null)
                        player.sendMessage("User "+split[1]+" not found");
                    else {
                        String perm = "text";
                        if (to.isAdmin())
                            perm = "textadmin";
                        if (TextPlayer.hasPermission(player, perm)) {
                            if (Register.Charge(player, to.isAdmin())) {
                                String replace = split[0]+" "+split[1];
                                String text = msg.replace(replace, player.getName()+":");
                                Mailer.sendMsg(player, to, text);
                            }
                        }
                        else
                            player.sendMessage("You do not have permission to do that");
                    }
                }
            }
            catch (Exception help) {
                player.sendMessage("§5     TextPlayer Help Page:");
                player.sendMessage("§eTextPlayer is used to send messages to a Users phone/email");
                player.sendMessage("§2/text [Name] [Message]§b Sends message to User");
                player.sendMessage("§2/text set [Carrier] [Number]§b Receive messages to phone");
                player.sendMessage("§2/text set email [Address]§b Receive messages to email address");
                player.sendMessage("§2/text watch user [Name]§b Receive message when user logs on");
                player.sendMessage("§2/text watch server§b Be alerted when Server comes online");
                player.sendMessage("§2/text watch item [Name]§b Receive message when item is placed");
                player.sendMessage("§2/text watch word [Word]§b Receive message when word is spoken");
                player.sendMessage("§2/text unwatch user [Name]§b Unwatch a user");
                player.sendMessage("§2/text unwatch server§b Unwatch the Server");
                player.sendMessage("§2/text unwatch item [Name]§b Unwatch an item");
                player.sendMessage("§2/text unwatch word [Name]§b Unwatch an word");
                player.sendMessage("§2/text disable§b Disable texts while logged on");
                player.sendMessage("§2/text enable§b Enable texts while logged on");
                player.sendMessage("§2/text limit [Number]§b Limit number of texts received each day");
                player.sendMessage("§2/text list carriers§b List supported Carriers");
                player.sendMessage("§2/text list users§b List current Users");
                player.sendMessage("§2/text list watch§b List who you are watching");
            }
        }
    }

    @Override
    public void onPlayerQuit (final PlayerQuitEvent event) {
        Thread check = new Thread() {
            @Override
            public void run() {
                String logged = event.getPlayer().getName();
                LinkedList<User> users = SaveSystem.getUsers();
                for (User user : users) {
                    if (user.name.equals(logged)) {
                        user.logged = true;
                        try {
                            Thread.currentThread().sleep(60000);
                        }
                        catch (Exception e) {
                        }
                        if (TextPlayer.server.getPlayer(logged) != null)
                            return;
                        user.logged = false;
                        break;
                    }
                }
                for (User user : users) {
                    if (user.isWatchingUser(logged))
                        Mailer.sendMsg(null, user, logged+" has logged off");
                }
            }
        };
        check.start();
    }
    
    @Override
    public void onPlayerJoin (PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String logged = player.getName();
        LinkedList<User> users = SaveSystem.getUsers();
        for (User user : users) {
            if (user.isWatchingUser(logged))
                Mailer.sendMsg(null, user, logged+" has logged on");
        }
    }

    @Override
    public void onPlayerChat (PlayerChatEvent event) {
        String msg = event.getMessage();
        LinkedList<User> users = SaveSystem.getUsers();
        for (User user : users) {
            if (user.isWatchingWord(msg))
                Mailer.sendMsg(null, user, event.getPlayer().getName()+": "+msg);
        }
    }
}