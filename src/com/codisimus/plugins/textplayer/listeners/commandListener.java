package com.codisimus.plugins.textplayer.listeners;

import com.codisimus.plugins.textplayer.Register;
import com.codisimus.plugins.textplayer.SaveSystem;
import com.codisimus.plugins.textplayer.TextPlayer;
import com.codisimus.plugins.textplayer.User;
import com.google.common.collect.Sets;
import java.util.HashSet;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Executes Player Commands
 * 
 * @author Codisimus
 */
public class commandListener implements CommandExecutor {
    public static enum Action { HELP, SET, CLEAR, WATCH, UNWATCH, ENABLE, DISABLE, LIMIT, LIST }
    public static enum WatchType { PLAYER, SERVER, ITEM, WORD }
    public static enum ListType { CARRIERS, USERS, ADMINS, WATCHING }
    public static final HashSet TRANSPARENT = Sets.newHashSet((byte)27, (byte)28,
            (byte)37, (byte)38, (byte)39, (byte)40, (byte)50, (byte)65, (byte)66,
            (byte)69, (byte)70, (byte)72, (byte)75, (byte)76, (byte)78);
    
    /**
     * Listens for ButtonWarp commands to execute them
     * 
     * @param sender The CommandSender who may not be a Player
     * @param command The command that was executed
     * @param alias The alias that the sender used
     * @param args The arguments for the command
     * @return true always
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        //Cancel if the command is not from a Player
        if (!(sender instanceof Player))
            return true;
        
        Player player = (Player)sender;

        //Display help page if the Player did not add any arguments
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        User user;
        
        //Execute the correct command
        switch (Action.valueOf(args[0])) {
            case SET:
                if (args.length == 3)
                    set(player, args[1], args[2]);
                else
                    sendHelp(player);
                
                return true;
                
            case CLEAR:
                SaveSystem.users.remove(SaveSystem.findUser(player.getName()));
                player.sendMessage("Your Phone Number/E-mail information has been cleared");
                
                SaveSystem.save();
                return true;
                
            case WATCH:
                //Cancel if the Player does not have the needed permission
                if (!TextPlayer.hasPermission(player, "watch."+args[1])) {
                    player.sendMessage("You do not have permission to do that");
                    return true;
                }
                
                switch (args.length) {
                    case 2:
                        if (!args[1].equals("server"))
                            break;
                        
                        watch(player, WatchType.SERVER, null);
                        return true;
                    
                    case 3: watch(player, WatchType.valueOf(args[1]), args[2]); return true;
                        
                    default: break;
                }
                
                watch(player, null, null);
                return true;
                
            case UNWATCH:
                switch (args.length) {
                    case 2:
                        if (!args[1].equals("server"))
                            break;
                        
                        unwatch(player, WatchType.SERVER, null);
                        return true;
                    
                    case 3: unwatch(player, WatchType.valueOf(args[1]), args[2]); return true;
                        
                    default: break;
                }
                
                sendHelp(player);
                return true;
                
            case ENABLE:
                user = SaveSystem.findUser(player.getName());
                if (user == null) {
                    player.sendMessage("You must first add your contact info");
                    return true;
                }

                user.disableWhenLogged = false;
                player.sendMessage("You will receive texts when you are logged on");

                SaveSystem.save();
                return true;
                
            case DISABLE:
                user = SaveSystem.findUser(player.getName());
                if (user == null) {
                    player.sendMessage("You must first add your contact info");
                    return true;
                }

                user.disableWhenLogged = true;
                player.sendMessage("You will not receive texts when you are logged on");

                SaveSystem.save();
                return true;
                
            case LIMIT:
                if (args.length == 2)
                    try {
                        user = SaveSystem.findUser(player.getName());
                        if (user == null) {
                            player.sendMessage("You must first add your contact info");
                            return true;
                        }
                        
                        //Cancel if the User is not verified
                        if (user.textLimit == -1) {
                            player.sendMessage("You must first verify you email address");
                            return true;
                        }

                        user.textLimit = Integer.parseInt(args[1]);
                        player.sendMessage("You will receive no more than "+args[1]+" texts each day");

                        SaveSystem.save();
                        return true;
                    }
                    catch (Exception notInt) {
                        break;
                    }
                
                sendHelp(player);
                return true;
                
            case LIST:
                if (args.length == 2)
                    list(player, ListType.valueOf(args[1]));
                else
                    sendHelp(player);
                
                return true;
                
            case HELP: sendHelp(player); return true;
                
            default: //Command == Send a Text Message
                //Cancel if the first argument is not a valid User
                user = SaveSystem.findUser(args[0]);
                if (user == null) {
                    player.sendMessage("User "+args[0]+" not found");
                    return true;
                }
                
                //Cancel if the Player does not have the needed permission
                if (!TextPlayer.hasPermission(player, user.isAdmin() ? "text" : "textadmin")) {
                    player.sendMessage("You do not have permission to do that");
                    return true;
                }
                
                //Cancel if the Player had insufficient funds
                if (!Register.Charge(player, user.isAdmin()))
                    return true;
                
                //Construct the message to send
                String msg = player.getName().concat(":");
                for (int i = 1; i < args.length; i++)
                    msg = msg.concat(" "+args[i]);

                mailListener.sendMsg(player, user, msg);
                return true;
        }
        
        return true;
    }
    
    /**
     * Changes the email address of a User
     * 
     * @param player The Player that the User represents
     * @param carrier The cell phone carrier or 'email'
     * @param address The phone number or email address
     */
    public static void set(Player player, String carrier, String address) {
        //Cancel if the Player does not have the needed permission
        if (!TextPlayer.hasPermission(player, "use")) {
            player.sendMessage("You do not have permission to do that");
            return;
        }
        
        //Check if the User needs to be created
        User user = SaveSystem.findUser(player.getName());
        if (user == null) {
            //Create a new User
            user = new User(player, carrier, address);
            
            //Cancel if the email could not be set
            if (user.email == null)
                return;
            
            player.sendMessage("Sending Confirmation Text...");
            mailListener.sendMsg(player, user, "Reply 'enable' to link this number to "+user.name);
            SaveSystem.users.add(user);
        }
        else {
            //Set the new email and send the returned message to the Player
            String success = user.setEmail(carrier, address);
            player.sendMessage(success);
            
            //Return if setting the email was unsuccesful
            if (!success.startsWith("Email"))
                return;
            
            player.sendMessage("Sending Confirmation Text...");
            mailListener.sendMsg(player, user, "Reply 'enable' to link this number to "+user.name);

            //Set the User as not verified
            user.textLimit = -1;
        }
        
        SaveSystem.save();
    }
    
    /**
     * Adds something to the User's watching list
     * 
     * @param player The Player who is modifying their User
     * @param type The type of thing that will be watched
     * @param name The thing that will be watched
     */
    public static void watch(Player player, WatchType type, String name) {
        //Cancel if the Player does not have an existing User
        User user = SaveSystem.findUser(player.getName());
        if (user == null) {
            player.sendMessage("You must first add your contact info");
            return;
        }
        
        //Determine the WatchType
        switch (type) {
            case SERVER:
                //Cancel if the User is already watching
                if (user.watchingServer) {
                    player.sendMessage("You are already watching the server");
                    return;
                }
                
                user.watchingServer = true;
                
                player.sendMessage("You will be alerted when Server come online");
                break;
                
            case PLAYER:
                if (name.equals("*")) {
                    //Cancel if the Player does not have the needed permission
                    if (!TextPlayer.hasPermission(player, "watch.everyplayer")) {
                        player.sendMessage("You do not have permission to do that");
                        return;
                    }
                
                    //Cancel if the User is already watching
                    if (user.players.contains(name)) {
                        player.sendMessage("You are already watching every Player");
                        return;
                    }
                    
                    user.players.add(name);
                    player.sendMessage("Now watching every Player");
                }
                else {
                    //Cancel if the User is already watching
                    if (user.players.contains(name)) {
                        player.sendMessage("You are already watching "+name);
                        return;
                    }
                
                    user.players.add(name);
                    player.sendMessage("Now watching "+name);
                }
                
                break;
                
            case ITEM:
                //Cancel if the User is already watching
                if (user.items.contains(name)) {
                    player.sendMessage("You are already watching "+name);
                    return;
                }

                user.items.add(name);
                
                player.sendMessage("Now watching "+name);
                break;
                
            case WORD:
                //Cancel if the User is already watching
                if (user.words.contains(name)) {
                    player.sendMessage("You are already watching "+name);
                    return;
                }

                user.words.add(name);
                
                player.sendMessage("Now watching "+name);
                break;
            
            default:
                player.sendMessage("§5     TextPlayer Watch Help Page:");
                player.sendMessage("§2/text watch player [Name]§b Be alerted when Player logs on");
                player.sendMessage("§2/text watch player *§b Be alerted when any Player logs on");
                player.sendMessage("§2/text watch server§b Be alerted when Server comes online");
                player.sendMessage("§2/text watch item tnt§b Receive message when tnt is placed");
                player.sendMessage("§2/text watch item fire§b Receive message when fire is lit");
                player.sendMessage("§2/text watch word [Word]§b Receive message when word is spoken");
                return;
        }
        
        SaveSystem.save();
    }
    
    /**
     * Removes something from the User's watching list
     * 
     * @param player The Player who is modifying their User
     * @param type The type of thing that will be unwatched
     * @param name The thing that will be unwatched
     */
    public static void unwatch(Player player, WatchType type, String name) {
        //Cancel if the Player does not have an existing User
        User user = SaveSystem.findUser(player.getName());
        if (user == null) {
            player.sendMessage("You must first add your contact info");
            return;
        }
        
        switch (type) {
            case SERVER:
                //Cancel if the User is already watching
                if (!user.watchingServer) {
                    player.sendMessage("You are not watching the server");
                    return;
                }
                
                user.watchingServer = false;
                
                player.sendMessage("You will not be alerted when Server come online");
                break;
                
            case PLAYER:
                if (name.equals("*")) {
                    //Cancel if the User is already watching
                    if (user.players.contains(name)) {
                        player.sendMessage("You are not watching every Player");
                        return;
                    }
                    
                    user.players.remove(name);
                    player.sendMessage("No longer watching every Player");
                }
                else {
                    //Cancel if the User is already watching
                    if (user.players.contains(name)) {
                        player.sendMessage("You are not watching "+name);
                        return;
                    }
                
                    user.players.remove(name);
                    player.sendMessage("No longer watching "+name);
                }
                
                break;
                
            case ITEM:
                //Cancel if the User is already watching
                if (user.items.contains(name)) {
                    player.sendMessage("You are not watching "+name);
                    return;
                }

                user.items.remove(name);
                
                player.sendMessage("No longer watching "+name);
                break;
                
            case WORD:
                //Cancel if the User is already watching
                if (user.words.contains(name)) {
                    player.sendMessage("You are not watching "+name);
                    return;
                }

                user.words.remove(name);
                
                player.sendMessage("No longer watching "+name);
                break;
            
            default: sendHelp(player); return;
        }
        
        SaveSystem.save();
    }
    
    /**
     * Lists the information to the given Player
     * 
     * @param player The Player requesting a list
     * @param type The type of list that was requested
     */
    public static void list(Player player, ListType type) {
        //Determine what to list
        switch (type) {
            case CARRIERS:
                player.sendMessage("§5Supported Carriers:");
                player.sendMessage("§2At&t, Bell, BeeLine, Bouygues, Cricket, D1, E-Plus, Etisalat, Fido");
                player.sendMessage("§2Koodo, LMT, MetroPCS, Mobistar, MTS, NetCom, Optimus, Optus, Orange");
                player.sendMessage("§2O2-UK, O2-Germany, Rogers, SFR, SoftBank, Sprint, Starhub, Sunrise");
                player.sendMessage("§2Swisscom, TDC, Telecom, Telenor, Tele2, Telia, Telstra, Telus, Three");
                player.sendMessage("§2TMN, T-Mobile, T-Mobile-Czech, US-Cellular, Verizon, Virgin-Mobile");
                player.sendMessage("§2Virgin-Mobile-Canada, Vivo, Vodafone-Germany, Vodafone-Greece");
                player.sendMessage("§2Vodafone-Iceland, Vodafone-Italy, Vodafone-UK");
                player.sendMessage("§bIf your cell phone provider is not listed,");
                player.sendMessage("§bhave an admin contact Codisimus about adding it.");
                return;

            case USERS:
                //Cancel if the Player does not have the needed permission
                if (!TextPlayer.hasPermission(player, "listusers")) {
                    player.sendMessage("You do not have permission to do that");
                    return;
                }
                
                String userList = "§eCurrent Users:§2  ";
                for(User tempUser: SaveSystem.users)
                    userList = userList.concat(tempUser.name+", ");
                
                player.sendMessage(userList.substring(0, userList.length() - 2));
                return;
                
            case ADMINS:
                //Cancel if the Player does not have the needed permission
                if (!TextPlayer.hasPermission(player, "listadmins")) {
                    player.sendMessage("You do not have permission to do that");
                    return;
                }
                
                String adminList = "§eCurrent Admins:§2  ";
                for(User tempUser: SaveSystem.users)
                    if (tempUser.isAdmin())
                        adminList = adminList.concat(tempUser.name+", ");
                
                player.sendMessage(adminList.substring(0, adminList.length() - 2));
                return;

            case WATCHING:
                //Cancel if the Player does not have an existing User
                User user = SaveSystem.findUser(player.getName());
                if (user == null) {
                    player.sendMessage("You must first add your contact info");
                    return;
                }
                
                player.sendMessage("§2Watching Server: "+user.watchingServer);
                player.sendMessage("§2Watching Players: ".concat(user.players.toString()));
                player.sendMessage("§2Watching Items: ".concat(user.items.toString()));
                player.sendMessage("§2Watching Words: ".concat(user.words.toString()));
                return;
        }
    }
    
    /**
     * Displays the TextPlayer Help Page to the given Player
     *
     * @param player The Player needing help
     */
    public static void sendHelp(Player player) {
        player.sendMessage("§5     TextPlayer Help Page:");
        player.sendMessage("§eTextPlayer is used to send messages to a Users phone/email");
        player.sendMessage("§2/text [Name] [Message]§b Sends message to User");
        player.sendMessage("§2/text set [Carrier] [Number]§b Receive messages to phone");
        player.sendMessage("§2/text set email [Address]§b Receive messages to email address");
        player.sendMessage("§2/text clear§b Clear your Phone Number/E-mail Information");
        player.sendMessage("§2/text watch§b Display Watch Help Page");
        player.sendMessage("§2/text unwatch player [Name]§b Unwatch a Player");
        player.sendMessage("§2/text unwatch server§b Unwatch the Server");
        player.sendMessage("§2/text unwatch item [Name]§b Unwatch an item");
        player.sendMessage("§2/text unwatch word [Name]§b Unwatch a word");
        player.sendMessage("§2/text [enable/disable]§b Enable/Disable texts while logged on");
        player.sendMessage("§2/text limit [Number]§b Limit number of texts received each day");
        player.sendMessage("§2/text list carriers§b List supported Carriers");
        player.sendMessage("§2/text list users§b List current Users");
        player.sendMessage("§2/text list admins§b List current Admins");
        player.sendMessage("§2/text list watching§b List what you are watching");
    }
}
