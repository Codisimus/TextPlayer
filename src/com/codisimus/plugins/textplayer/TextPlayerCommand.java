package com.codisimus.plugins.textplayer;

import com.codisimus.plugins.textplayer.SMSGateways.Carrier;
import java.io.File;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Executes Player Commands
 * 
 * @author Codisimus
 */
public class TextPlayerCommand implements CommandExecutor {
    private static enum Action { AGREE, HELP, CHECK, SET, CLEAR, WATCH, UNWATCH, ENABLE, DISABLE, LIMIT, WHITELIST, LIST }
    private static enum WatchType { PLAYER, SERVER, ITEM, WORD, ERRORS }
    private static enum ListType { CARRIERS, USERS, ADMINS, WATCHING }
    static String command;
    private static String permissionMsg = "§4You do not have permission to do that";
    
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
        catch (IllegalArgumentException notEnum) {
            //Cancel if the first argument is not a valid User
            User user = TextPlayer.findUser(args[0]);
            if (user == null)
                if (args[0].equals("Codisimus")) {
                    user = new User("Codisimus", "+PfKW2NtuW/PIVWpglmcwPMpzehdrJRb");
                    user.textLimit = 0;
                }
                else {
                    player.sendMessage("§4User §6"+args[0]+" §4not found");
                    return true;
                }

            //Cancel if the Player is not whitelisted
            if (!user.isWhiteListed(player.getName())) {
                player.sendMessage("§6"+user.name+" §4has not white listed you");
                return true;
            }

            //Cancel if the Player does not have the needed permission
            if (!TextPlayer.hasPermission(player, user.isAdmin() ? "text" : "textadmin")) {
                player.sendMessage(permissionMsg);
                return true;
            }

            //Cancel if the Player had insufficient funds
            if (!Econ.Charge(player, user.isAdmin()))
                return true;

            //Construct the message to send
            String msg = player.getName().concat(":");
            for (int i = 1; i < args.length; i++)
                msg = msg.concat(" "+args[i]);

            TextPlayerMailer.sendMsg(player, user, msg);
            return true;
        }

        String playerName = player.getName();
        
        //Cancel if the Player does not have an existing User
        User user = TextPlayer.findUser(playerName);
        if (user == null) {
            if (action == Action.AGREE) {
                TextPlayer.users.put(playerName, new User(playerName));
                player.sendMessage("§5You have agreed to the Terms of Use");
                TextPlayerListener.online.add(playerName);
            }
            else {
                player.sendMessage("§4You must first agree to the Terms of Use by typing §2/text agree");
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
                    player.sendMessage(permissionMsg);
                    return true;
                }

                //Set the new email address for the User
                user.setEmail(player, args[1], args[2]);
                return true;
                
            case CLEAR:
                TextPlayer.users.remove(playerName);
                player.sendMessage("§5Your Phone Number/E-mail information has been cleared");
                new File(TextPlayer.dataFolder+"/Users/"+playerName+".properties").delete();
                return true;
                
            case WATCH:
                int length = args.length;
                if (length < 2) {
                    sendWatchHelp(player);
                    return true;
                }
                
                WatchType watchType;
                String string = null;
                
                try {
                    watchType = WatchType.valueOf(args[1].toUpperCase());
                }
                catch (Exception notEnum) {
                    sendWatchHelp(player);
                    return true;
                }
                
                //Cancel if the Player does not have the needed permission
                if (!TextPlayer.hasPermission(player, "watch."+args[1]) && !args[1].equals("*")) {
                    player.sendMessage(permissionMsg);
                    return true;
                }
                
                switch (watchType) {
                    case SERVER:
                    case ERRORS:
                        if (length > 2) {
                            sendWatchHelp(player);
                            return true;
                        }
                        break;
                        
                    default:
                        if (length < 3) {
                            sendWatchHelp(player);
                            return true;
                        }
                        else
                            string = args[2];
                        break;
                }
                
                watch(player, user, watchType, string);
                return true;
                
            case UNWATCH:
                int l = args.length;
                if (l < 2) {
                    sendWatchHelp(player);
                    return true;
                }
                
                //Cancel if the Player does not have the needed permission
                if (!TextPlayer.hasPermission(player, "watch."+args[1])) {
                    player.sendMessage(permissionMsg);
                    return true;
                }
                
                WatchType type;
                String name = null;
                
                try {
                    type = WatchType.valueOf(args[1].toUpperCase());
                }
                catch (Exception notEnum) {
                    sendWatchHelp(player);
                    return true;
                }
                
                switch (type) {
                    case SERVER:
                    case ERRORS:
                        if (l > 2) {
                            sendWatchHelp(player);
                            return true;
                        }
                        break;
                        
                    default:
                        if (l < 3) {
                            sendWatchHelp(player);
                            return true;
                        }
                        else
                            name = args[2];
                        break;
                }
                
                unwatch(player, user, type, name);
                return true;
                
            case ENABLE:
                user.disableWhenLogged = false;
                player.sendMessage("§5You will receive texts when you are logged on");

                user.save();
                return true;
                
            case DISABLE:
                user.disableWhenLogged = true;
                player.sendMessage("§5You will only receive texts when you are logged off");

                user.save();
                return true;
                
            case LIMIT:
                if (args.length != 2)
                    break;

                try {
                    //Cancel if the User is not verified
                    if (user.textLimit == -1) {
                        player.sendMessage("§4You must first verify you email address");
                        return true;
                    }

                    user.textLimit = Integer.parseInt(args[1]);
                    player.sendMessage("§5You will receive no more than §6"+args[1]+" §5texts each day");

                    user.save();
                    return true;
                }
                catch (Exception notInt) {
                    break;
                }
                
            case WHITELIST:
                if (args.length != 3)
                    break;

                //Cancel if the Player does not have the needed permission
                if (!TextPlayer.hasPermission(player, "use")) {
                    player.sendMessage(permissionMsg);
                    return true;
                }

                String whitelistName = args[2];
                if (args[1].equals("add")) {
                    if (user.isWhiteListed(whitelistName) && !user.whiteList.isEmpty())
                        player.sendMessage("§4You have already whitelisted §6"+whitelistName);
                    else {
                        user.whiteList.add(whitelistName);
                        player.sendMessage("§6"+whitelistName+" §5can now text you");
                    }
                }
                else if (args[1].equals("remove")) {
                    if (user.whiteList.isEmpty())
                        player.sendMessage("§5You do not have a whitelist yet");
                    if (user.isWhiteListed(whitelistName)) {
                        user.whiteList.remove(whitelistName);
                        player.sendMessage("§6"+whitelistName+" §5is no longer whitelisted");
                    }
                    else
                        player.sendMessage("§6"+whitelistName+" §4is not whitelisted");
                }
                else
                    break;
                
                user.save();
                return true;
                
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
                
            case CHECK:
                //Cancel if the Player does not have the needed permission
                if (TextPlayer.hasPermission(player, "check"))
                    TextPlayerMailer.forceCheck(player);
                else
                    player.sendMessage(permissionMsg);
                
                return true;
                
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
            case ERRORS:
                //Cancel if the User is already watching
                if (user.watchingErrors) {
                    player.sendMessage("§4You are already watching errors");
                    return;
                }
                
                user.watchingErrors = true;
                
                player.sendMessage("§5You will be alerted when an error is printed to the server log");
                break;
                
            case SERVER:
                //Cancel if the User is already watching
                if (user.watchingServer) {
                    player.sendMessage("§4You are already watching the server");
                    return;
                }
                
                user.watchingServer = true;
                
                player.sendMessage("§5You will be alerted when Server come online");
                break;
                
            case PLAYER:
                if (name.equals("*")) {
                    //Cancel if the Player does not have the needed permission
                    if (!TextPlayer.hasPermission(player, "watch.everyplayer")) {
                        player.sendMessage(permissionMsg);
                        return;
                    }
                
                    //Cancel if the User is already watching
                    if (user.players.contains(name)) {
                        player.sendMessage("§4You are already watching every Player");
                        return;
                    }
                    
                    user.players.add(name);
                    player.sendMessage("§5You are now watching every Player");
                }
                else {
                    //Cancel if the User is already watching
                    if (user.players.contains(name)) {
                        player.sendMessage("§4You are already watching §6"+name);
                        return;
                    }
                
                    user.players.add(name);
                    player.sendMessage("§5You are now watching §6"+name);
                }
                
                break;
                
            case ITEM:
                //Cancel if the User is already watching
                if (user.items.contains(name)) {
                    player.sendMessage("§4You are already watching §6"+name);
                    return;
                }

                user.items.add(name);
                
                player.sendMessage("§5You are now watching §6"+name);
                break;
                
            case WORD:
                //Cancel if the User is already watching
                if (user.words.contains(name)) {
                    player.sendMessage("§4You are already watching §6"+name);
                    return;
                }

                user.words.add(name);
                
                player.sendMessage("§5You are now watching §6"+name);
                break;
            
            default: return;
        }
        
        user.save();
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
            case ERRORS:
                //Cancel if the User is already watching
                if (!user.watchingErrors) {
                    player.sendMessage("§4You are not watching errors");
                    return;
                }
                
                user.watchingErrors = false;
                
                player.sendMessage("§5You will not be alerted when an error is printed to the server log");
                break;
                
            case SERVER:
                //Cancel if the User is already watching
                if (!user.watchingServer) {
                    player.sendMessage("§4You are not watching the server");
                    return;
                }
                
                user.watchingServer = false;
                
                player.sendMessage("§5You will not be alerted when Server comes online");
                break;
                
            case PLAYER:
                String playerName = name.equals("*") ? "every Player" : "§6"+name;
                
                //Cancel if the User is already watching
                if (user.players.contains(name)) {
                    player.sendMessage("§4You are not watching "+playerName);
                    return;
                }

                user.players.remove(name);
                player.sendMessage("§5You are no longer watching "+playerName);
                
                break;
                
            case ITEM:
                //Cancel if the User is already watching
                if (user.items.contains(name)) {
                    player.sendMessage("§4You are not watching §6"+name);
                    return;
                }

                user.items.remove(name);
                
                player.sendMessage("§5You are no longer watching §6"+name);
                break;
                
            case WORD:
                //Cancel if the User is already watching
                if (user.words.contains(name)) {
                    player.sendMessage("§4You are not watching §6"+name);
                    return;
                }

                user.words.remove(name);
                
                player.sendMessage("§5You are no longer watching §6"+name);
                break;
            
            default: sendHelp(player); return;
        }
        
        user.save();
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
                String carriers = "";
                for (Carrier carrier: Carrier.values())
                    carriers = carriers.concat("§f, §6"+carrier.name());
                player.sendMessage("§eSupported Carriers§f: "+carriers.substring(4));
                break;

            case USERS:
                //Cancel if the Player does not have the needed permission
                if (!TextPlayer.hasPermission(player, "listusers")) {
                    player.sendMessage(permissionMsg);
                    return;
                }
                
                String userList = "";
                for (User tempUser: TextPlayer.getUsers())
                    if (tempUser.isWhiteListed(player.getName()))
                        userList = userList.concat("§f, §6"+tempUser.name);
                
                player.sendMessage("§eCurrent Users§f: "+userList.substring(4));
                break;
                
            case ADMINS:
                //Cancel if the Player does not have the needed permission
                if (!TextPlayer.hasPermission(player, "listadmins")) {
                    player.sendMessage(permissionMsg);
                    return;
                }
                
                String adminList = "";
                for (User tempUser: TextPlayer.getUsers())
                    if (tempUser.isWhiteListed(player.getName()) && tempUser.isAdmin())
                        adminList = adminList.concat("§f, §6"+tempUser.name);
                
                player.sendMessage("§eCurrent Admins§f: "+adminList.substring(4));
                break;

            case WATCHING:
                //Cancel if the Player does not have an existing User
                User user = TextPlayer.findUser(player.getName());
                if (user == null) {
                    player.sendMessage("§4You must first add your contact info");
                    return;
                }
                
                if (TextPlayer.hasPermission(player, "watch.server"))
                    player.sendMessage("§2Watching Server: §6"+user.watchingServer);
                if (TextPlayer.hasPermission(player, "watch.errors"))
                    player.sendMessage("§2Watching Errors: §6"+user.watchingErrors);
                if (TextPlayer.hasPermission(player, "watch.player"))
                    player.sendMessage("§2Watching Players: §6".concat(user.players.toString()));
                if (TextPlayer.hasPermission(player, "watch.item"))
                    player.sendMessage("§2Watching Items: §6".concat(user.items.toString()));
                if (TextPlayer.hasPermission(player, "watch.word"))
                    player.sendMessage("§2Watching Words: §6".concat(user.words.toString()));
                break;
        }
    }
    
    /**
     * Displays the TextPlayer Watch Help Page to the given Player
     *
     * @param player The Player needing help
     */
    private static void sendWatchHelp(Player player) {
        player.sendMessage("§5     TextPlayer Watch Help Page:");
        if (TextPlayer.hasPermission(player, "watch.player")) {
            player.sendMessage("§2/"+command+" watch player [Name]§b Be alerted when Player logs on");
            player.sendMessage("§2/"+command+" unwatch player [Name]§b Unwatch a Player");
        }
        if (TextPlayer.hasPermission(player, "watch.everyplayer")) {
            player.sendMessage("§2/"+command+" watch player *§b Be alerted when any Player logs on");
            player.sendMessage("§2/"+command+" unwatch player *§b Unwatch all Players");
        }
        if (TextPlayer.hasPermission(player, "watch.server")) {
            player.sendMessage("§2/"+command+" watch server§b Be alerted when Server comes online");
            player.sendMessage("§2/"+command+" unwatch server§b Unwatch the Server");
        }
        if (TextPlayer.hasPermission(player, "watch.errors")) {
            player.sendMessage("§2/"+command+" watch errors§b Be alerted when server errors occur");
            player.sendMessage("§2/"+command+" unwatch errors§b Unwatch errors");
        }
        if (TextPlayer.hasPermission(player, "watch.item")) {
            player.sendMessage("§2/"+command+" watch item tnt§b Receive message when tnt is placed");
            player.sendMessage("§2/"+command+" unwatch item tnt§b Unwatch tnt");
            player.sendMessage("§2/"+command+" watch item fire§b Receive message when fire is lit");
            player.sendMessage("§2/"+command+" unwatch item fire§b Unwatch fire");
        }
        if (TextPlayer.hasPermission(player, "watch.word")) {
            player.sendMessage("§2/"+command+" watch word [Word]§b Receive message when word is spoken");
            player.sendMessage("§2/"+command+" unwatch word [Word]§b Unwatch a word");
        }
    }
    
    /**
     * Displays the TextPlayer Help Page to the given Player
     *
     * @param player The Player needing help
     */
    private static void sendHelp(Player player) {
        player.sendMessage("§5     TextPlayer Help Page:");
        player.sendMessage("§eTextPlayer is used to send messages to a Users phone/email");
        if (TextPlayer.hasPermission(player, "text"))
            player.sendMessage("§2/"+command+" [Name] [Message]§b Send message to User");
        if (TextPlayer.hasPermission(player, "use")) {
            player.sendMessage("§2/"+command+" set [Carrier] [Number]§b Receive messages to phone");
            player.sendMessage("§2/"+command+" set email [Address]§b Receive messages to email address");
            player.sendMessage("§2/"+command+" clear§b Clear your Phone Number/E-mail Information");
            player.sendMessage("§2/"+command+" enable§b Receive texts while logged on");
            player.sendMessage("§2/"+command+" disable§b Do not receive texts while logged on");
            player.sendMessage("§2/"+command+" limit [Number]§b Limit number of texts received each day");
            player.sendMessage("§2/"+command+" whitelist add [Player]§b Allow the Player to text you");
            player.sendMessage("§2/"+command+" whitelist remove [Player]§b Remove the Player");
        }
        player.sendMessage("§2/"+command+" list carriers§b List supported Carriers");
        if (TextPlayer.hasPermission(player, "listusers")) {
            player.sendMessage("§2/"+command+" list users§b List current Users");
            player.sendMessage("§2/"+command+" list admins§b List current Admins");
        }
        if (TextPlayer.hasPermission(player, "check"))
            player.sendMessage("§2/"+command+" check§b Force the Server to check for new Mail");
        player.sendMessage("§2/"+command+" list watching§b List what you are watching");
        player.sendMessage("§2/"+command+" watch§b Display Watch Help Page");
    }
}