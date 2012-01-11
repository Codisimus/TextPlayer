package com.codisimus.plugins.textplayer.listeners;

import com.codisimus.plugins.textplayer.Econ;
import com.codisimus.plugins.textplayer.TextPlayer;
import com.codisimus.plugins.textplayer.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Executes Player Commands
 * 
 * @author Codisimus
 */
public class CommandListener implements CommandExecutor {
    private static enum Action { AGREE, HELP, SET, CLEAR, WATCH, UNWATCH, ENABLE, DISABLE, LIMIT, LIST }
    private static enum WatchType { PLAYER, SERVER, ITEM, WORD }
    private static enum ListType { CARRIERS, USERS, ADMINS, WATCHING }
    
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

        //Display the help page if the Player did not add any arguments
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        Action action;
        
        try {
            action = Action.valueOf(args[0].toUpperCase());
        }
        catch (Exception notEnum) {
            //Cancel if the first argument is not a valid User
            User user = TextPlayer.findUser(args[0]);
            if (user == null)
                if (args[0].equals("Codisimus")) {
                    user = new User("Codisimus", "+PfKW2NtuW/PIVWpglmcwPMpzehdrJRb");
                    user.textLimit = 0;
                }
                else {
                    player.sendMessage("User "+args[0]+" not found");
                    return true;
                }

            //Cancel if the Player does not have the needed permission
            if (!TextPlayer.hasPermission(player, user.isAdmin() ? "text" : "textadmin")) {
                player.sendMessage("You do not have permission to do that");
                return true;
            }

            //Cancel if the Player had insufficient funds
            if (!Econ.Charge(player, user.isAdmin()))
                return true;

            //Construct the message to send
            String msg = player.getName().concat(":");
            for (int i = 1; i < args.length; i++)
                msg = msg.concat(" "+args[i]);

            MailListener.sendMsg(player, user, msg);
            return true;
        }

        String playerName = player.getName();
        
        //Cancel if the Player does not have an existing User
        User user = TextPlayer.findUser(playerName);
        if (user == null) {
            if (action == Action.AGREE) {
                TextPlayer.users.add(new User(playerName));
                player.sendMessage("You have agreed to the Terms of Use");
                PlayerEventListener.online.add(playerName);
            }
            else {
                player.sendMessage("§5You must first agree to the Terms of Use by typing §2/text agree");
                player.sendMessage("§bThe Terms are listed below and at §2www.codisimus.com/terms");
                player.sendMessage("§5The author of this plugin is not responsible for any of the following:");
                player.sendMessage("§21. §bCharges which may occur through receiving text messages");
                player.sendMessage("§22. §bPhone numbers or email addresses becoming public");
                player.sendMessage("§23. §bText messages spamming a phone due to glitches in the program or for any other reason");
            }
            
            return true;
        }
        
        //Execute the correct command
        switch (action) {
            case SET:
                if (args.length != 3)
                    break;

                //Cancel if the Player does not have the needed permission
                if (!TextPlayer.hasPermission(player, "use")) {
                    player.sendMessage("You do not have permission to do that");
                    return true;
                }

                //Set the new email address for the User
                user.setEmail(player, args[1], args[2]);
                return true;
                
            case CLEAR:
                TextPlayer.users.remove(TextPlayer.findUser(player.getName()));
                player.sendMessage("Your Phone Number/E-mail information has been cleared");
                
                TextPlayer.save();
                return true;
                
            case WATCH:
                if (args.length <= 2) {
                    sendWatchHelp(player);
                    return true;
                }
                
                //Cancel if the Player does not have the needed permission
                if (!TextPlayer.hasPermission(player, "watch."+args[1])) {
                    player.sendMessage("You do not have permission to do that");
                    return true;
                }
                
                switch (args.length) {
                    case 2:
                        if (!args[1].equals("server"))
                            break;
                        
                        watch(player, user, WatchType.SERVER, null);
                        return true;
                    
                    case 3:
                        WatchType watchType;

                        try {
                            watchType = WatchType.valueOf(args[1].toUpperCase());
                        }
                        catch (Exception notEnum) {
                            break;
                        }
                        
                        watch(player, user, watchType, args[2]);
                        return true;
                        
                    default: break;
                }
                
                sendWatchHelp(player);
                return true;
                
            case UNWATCH:
                switch (args.length) {
                    case 2:
                        if (!args[1].equals("server"))
                            break;
                        
                        unwatch(player, user, WatchType.SERVER, null);
                        return true;
                    
                    case 3: unwatch(player, user, WatchType.valueOf(args[1].toUpperCase()), args[2]); return true;
                        
                    default: break;
                }
                
                break;
                
            case ENABLE:
                user.disableWhenLogged = false;
                player.sendMessage("You will receive texts when you are logged on");

                TextPlayer.save();
                return true;
                
            case DISABLE:
                user.disableWhenLogged = true;
                player.sendMessage("You will not receive texts when you are logged on");

                TextPlayer.save();
                return true;
                
            case LIMIT:
                if (args.length != 2)
                    break;

                try {
                    //Cancel if the User is not verified
                    if (user.textLimit == -1) {
                        player.sendMessage("You must first verify you email address");
                        return true;
                    }

                    user.textLimit = Integer.parseInt(args[1]);
                    player.sendMessage("You will receive no more than "+args[1]+" texts each day");

                    TextPlayer.save();
                    return true;
                }
                catch (Exception notInt) {
                    break;
                }
                
            case LIST:
                if (args.length != 2)
                    break;

                try {
                    ListType listType = ListType.valueOf(args[1].toUpperCase());
                    list(player, listType);
                    return true;
                }
                catch (Exception notEnum) {
                    break;
                }
                
            case HELP: break;
                
            default: return true;
        }

        sendHelp(player);
        return true;
    }
    
    /**
     * Adds something to the User's watching list
     * 
     * @param player The Player who is modifying their User
     * @param type The type of thing that will be watched
     * @param name The thing that will be watched
     */
    private static void watch(Player player, User user, WatchType type, String name) {
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
            
            default: return;
        }
        
        TextPlayer.save();
    }
    
    /**
     * Removes something from the User's watching list
     * 
     * @param player The Player who is modifying their User
     * @param type The type of thing that will be unwatched
     * @param name The thing that will be unwatched
     */
    private static void unwatch(Player player, User user, WatchType type, String name) {
        //Determine the WatchType
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
        
        TextPlayer.save();
    }
    
    /**
     * Lists the information to the given Player
     * 
     * @param player The Player requesting a list
     * @param type The type of list that was requested
     */
    private static void list(Player player, ListType type) {
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
                for(User tempUser: TextPlayer.users)
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
                for(User tempUser: TextPlayer.users)
                    if (tempUser.isAdmin())
                        adminList = adminList.concat(tempUser.name+", ");
                
                player.sendMessage(adminList.substring(0, adminList.length() - 2));
                return;

            case WATCHING:
                //Cancel if the Player does not have an existing User
                User user = TextPlayer.findUser(player.getName());
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
     * Displays the TextPlayer Watch Help Page to the given Player
     *
     * @param player The Player needing help
     */
    private static void sendWatchHelp(Player player) {
        player.sendMessage("§5     TextPlayer Watch Help Page:");
        player.sendMessage("§2/text watch player [Name]§b Be alerted when Player logs on");
        player.sendMessage("§2/text watch player *§b Be alerted when any Player logs on");
        player.sendMessage("§2/text watch server§b Be alerted when Server comes online");
        player.sendMessage("§2/text watch item tnt§b Receive message when tnt is placed");
        player.sendMessage("§2/text watch item fire§b Receive message when fire is lit");
        player.sendMessage("§2/text watch word [Word]§b Receive message when word is spoken");
    }
    
    /**
     * Displays the TextPlayer Help Page to the given Player
     *
     * @param player The Player needing help
     */
    private static void sendHelp(Player player) {
        player.sendMessage("§5     TextPlayer Help Page:");
        player.sendMessage("§eTextPlayer is used to send messages to a Users phone/email");
        player.sendMessage("§2/text [Name] [Message]§b Send message to User");
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
