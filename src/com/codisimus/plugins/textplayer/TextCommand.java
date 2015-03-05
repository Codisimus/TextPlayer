package com.codisimus.plugins.textplayer;

import com.codisimus.plugins.textplayer.CommandHandler.CodCommand;
import com.codisimus.plugins.textplayer.SMSGateways.Carrier;
import java.io.File;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Executes Player Commands
 *
 * @author Codisimus
 */
public class TextCommand {
    static final String PERMISSION_MSG = "§4You do not have permission to do that";

    @CodCommand(
        command = "agree",
        weight = 10,
        usage = {
            "§2<command>§b Agree to the terms of use at §awww.codisimus.com/terms"
        }
    )
    public boolean agree(Player player) {
        String playerName = player.getName();
        TextPlayer.users.put(playerName, new User(playerName));
        player.sendMessage("§5You have agreed to the Terms of Use");
        TextPlayerListener.online.add(playerName);
        return true;
    }

    @CodCommand(
        command = "set",
        weight = 11,
        usage = {
            "§2<command>§b Clear your Phone Number/E-mail Information"
        },
        permission = "textplayer.use",
        mustHaveAgreedToTerms = true
    )
    public boolean set(Player player, String carrier, String address) {
        TextPlayer.findUser(player.getName()).setEmail(player, carrier, address);
        return true;
    }

    @CodCommand(
        command = "clear",
        weight = 12,
        usage = {
            "§2<command>§b Clear your Phone Number/E-mail Information"
        },
        mustHaveAgreedToTerms = true
    )
    public boolean clear(Player player) {
        TextPlayer.users.remove(player.getName());
        player.sendMessage("§5Your Phone Number/E-mail information has been cleared");
        new File(TextPlayer.dataFolder, "Users" + File.separator + player.getName() + ".properties").delete();
        return true;
    }

    @CodCommand(
        command = "enable",
        weight = 20,
        usage = {
            "§2<command>§b Receive texts while logged on"
        },
        permission = "textplayer.use",
        mustHaveAgreedToTerms = true
    )
    public boolean enable(Player player) {
        User user = TextPlayer.findUser(player.getName());
        user.disableWhenLogged = false;
        player.sendMessage("§5You will receive texts when you are logged on");
        user.save();
        return true;
    }

    @CodCommand(
        command = "disable",
        weight = 21,
        usage = {
            "§2<command>§b Do not receive texts while logged on"
        },
        permission = "textplayer.use",
        mustHaveAgreedToTerms = true
    )
    public boolean disable(Player player) {
        User user = TextPlayer.findUser(player.getName());
        user.disableWhenLogged = true;
        player.sendMessage("§5You will only receive texts when you are logged off");
        user.save();
        return true;
    }

    @CodCommand(
        command = "limit",
        weight = 22,
        usage = {
            "§2<command> <Amount>§b Limit number of texts received each day"
        },
        permission = "textplayer.use",
        mustHaveAgreedToTerms = true
    )
    public boolean limit(Player player, int amount) {
        User user = TextPlayer.findUser(player.getName());

        //Cancel if the User is not verified
        if (user.textLimit == -1) {
            player.sendMessage("§4You must first verify you email address");
            return true;
        }

        user.textLimit = amount;
        player.sendMessage("§5You will receive no more than §6"
                            + amount + " §5texts each day");

        user.save();
        return true;
    }

    @CodCommand(
        command = "whitelist",
        subcommand = "add",
        weight = 23,
        usage = {
            "§2<command> <Player>§b Allow the Player to text you"
        },
        permission = "textplayer.use",
        mustHaveAgreedToTerms = true
    )
    public boolean whitelistAdd(Player player, String name) {
        User user = TextPlayer.findUser(player.getName());
        if (!user.whiteList.isEmpty() && user.isWhiteListed(name)) {
            player.sendMessage("§4You have already whitelisted §6" + name);
        } else {
            user.whiteList.add(name);
            player.sendMessage("§6" + name + " §5can now text you");
        }
        return true;
    }

    @CodCommand(
        command = "whitelist",
        subcommand = "remove",
        weight = 24,
        usage = {
            "§2<command> <Player>§b Remove the Player from your Whitelist"
        },
        permission = "textplayer.use",
        mustHaveAgreedToTerms = true
    )
    public boolean whitelistRemove(Player player, String name) {
        User user = TextPlayer.findUser(player.getName());
        if (user.whiteList.isEmpty()) {
            player.sendMessage("§4You do not have a whitelist yet");
        }

        if (user.whiteList.remove(name)) {
            player.sendMessage("§6" + name + " §5is no longer whitelisted");
        } else {
            player.sendMessage("§6" + name + " §4is not whitelisted");
        }
        if (user.whiteList.isEmpty()) {
            player.sendMessage("§6Your whitelist is empty. That means it is inactive and anyone can text you.");
        }
        return true;
    }

    @CodCommand(
        command = "mass",
        subcommand = "optout",
        weight = 25,
        usage = {
            "§2<command>§b Opt out of receiving mass texts from the server"
        },
        permission = "textplayer.use",
        mustHaveAgreedToTerms = true
    )
    public boolean massOptout(Player player) {
        User user = TextPlayer.findUser(player.getName());
        user.massTextOptOut = true;
        player.sendMessage("§5You will not receive mass texts that are sent from the Server");
        return true;
    }

    @CodCommand(
        command = "mass",
        subcommand = "optin",
        weight = 25.1,
        usage = {
            "§2<command>§b Opt in to receiving mass texts from the server"
        },
        permission = "textplayer.use",
        mustHaveAgreedToTerms = true
    )
    public boolean massOptin(Player player) {
        User user = TextPlayer.findUser(player.getName());
        user.massTextOptOut = false;
        player.sendMessage("§5You will receive mass texts that are sent from the Server");
        return true;
    }

    @CodCommand(
        command = "check",
        weight = 30,
        usage = {
            "§2<command>§b Force the Server to check for new Mail"
        },
        permission = "textplayer.check"
    )
    public boolean check(CommandSender sender) {
        TextPlayerMailReader.forceCheck(sender);
        return true;
    }

    @CodCommand(
        command = "watch",
        subcommand = "server",
        weight = 40,
        usage = {
            "§2<command>§b Be alerted when server errors occur"
        },
        permission = "textplayer.watch.server",
        mustHaveAgreedToTerms = true
    )
    public boolean watchServer(Player player) {
        User user = TextPlayer.findUser(player.getName());
        if (user.watchingServer) {
            player.sendMessage("§4You are already watching the server");
        } else {
            user.watchingServer = true;
            player.sendMessage("§5You will be alerted when server comes online");
            user.save();
        }
        return true;
    }

    @CodCommand(
        command = "watch",
        subcommand = "player",
        weight = 40.1,
        usage = {
            "§2<command> <Player>§b Be alerted when a Player logs on",
            "§2<command> *§b Be alerted when any Player logs on"
        },
        permission = "textplayer.watch.player",
        mustHaveAgreedToTerms = true
    )
    public boolean watchPlayer(Player player, String name) {
        User user = TextPlayer.findUser(player.getName());
        if (name.equals("*")) {
            if (!player.hasPermission("textplayer.watch.everyplayer")) {
                player.sendMessage(PERMISSION_MSG);
            } else if (user.players.contains(name)) {
                player.sendMessage("§4You are already watching every Player");
            } else {
                user.players.add(name);
                player.sendMessage("§5You are now watching every Player");
                user.save();
            }
        } else {
            if (user.players.contains(name.toLowerCase())) {
                player.sendMessage("§4You are already watching §6" + name);
            } else {
                user.players.add(name.toLowerCase());
                player.sendMessage("§5You are now watching §6" + name);
                user.save();
            }
        }
        return true;
    }

    @CodCommand(
        command = "watch",
        subcommand = "errors",
        weight = 40.2,
        usage = {
            "§2<command>§b Be alerted when server errors occur"
        },
        permission = "textplayer.watch.errors",
        mustHaveAgreedToTerms = true
    )
    public boolean watchErrors(Player player) {
        User user = TextPlayer.findUser(player.getName());
        if (user.watchingErrors) {
            player.sendMessage("§4You are already watching errors");
        } else {
            user.watchingErrors = true;
            player.sendMessage("§5You will be alerted when an error is printed to the server log");
            user.save();
        }
        return true;
    }

    @CodCommand(
        command = "watch",
        subcommand = "item",
        weight = 40.3,
        usage = {
            "§2<command> tnt§b Receive message when tnt is place",
            "§2<command> fire§b Receive message when a fire is lit"
        },
        permission = "textplayer.watch.item",
        mustHaveAgreedToTerms = true
    )
    public boolean watchItem(Player player, String name) {
        User user = TextPlayer.findUser(player.getName());
        if (user.items.contains(name)) {
            player.sendMessage("§4You are already watching §6" + name);
        } else {
            user.items.add(name);
            player.sendMessage("§5You are now watching §6" + name);
            user.save();
        }
        return true;
    }

    @CodCommand(
        command = "watch",
        subcommand = "word",
        weight = 40.4,
        usage = {
            "§2<command> <Word>§b Receive message when a word is spoken"
        },
        permission = "textplayer.watch.word",
        mustHaveAgreedToTerms = true
    )
    public boolean watchWord(Player player, String name) {
        User user = TextPlayer.findUser(player.getName());
        if (user.words.contains(name)) {
            player.sendMessage("§4You are already watching §6" + name);
        } else {
            user.words.add(name);
            player.sendMessage("§5You are now watching §6" + name);
            user.save();
        }
        return true;
    }

    @CodCommand(
        command = "unwatch",
        subcommand = "server",
        weight = 41,
        usage = {
            "§2<command>§b Unwatch the server"
        },
        permission = "textplayer.watch.server",
        mustHaveAgreedToTerms = true
    )
    public boolean unwatchServer(Player player) {
        User user = TextPlayer.findUser(player.getName());
        if (user.watchingServer) {
            user.watchingServer = false;
            player.sendMessage("§4You are no longer watching the server");
            user.save();
        } else {
            player.sendMessage("§5You are not watching the Server");
        }
        return true;
    }

    @CodCommand(
        command = "unwatch",
        subcommand = "player",
        weight = 41.1,
        usage = {
            "§2<command> <Player>§b Unwatch a Player"
        },
        permission = "textplayer.watch.player",
        mustHaveAgreedToTerms = true
    )
    public boolean unwatchPlayer(Player player, String name) {
        User user = TextPlayer.findUser(player.getName());
        if (name.equals("*")) {
            if (user.players.remove(name)) {
                player.sendMessage("§4You are no longer watching every Player");
                user.save();
            } else {
                player.sendMessage("§5You are not watching every Player");
            }
        } else {
            if (user.players.remove(name.toLowerCase())) {
                player.sendMessage("§4You are no longer watching §6" + name);
                user.save();
            } else {
                player.sendMessage("§5You are not watching §6" + name);
            }
        }
        return true;
    }

    @CodCommand(
        command = "unwatch",
        subcommand = "errors",
        weight = 41.2,
        usage = {
            "§2<command>§b Unwatch server errors"
        },
        permission = "textplayer.watch.errors",
        mustHaveAgreedToTerms = true
    )
    public boolean unwatchErrors(Player player) {
        User user = TextPlayer.findUser(player.getName());
        if (user.watchingErrors) {
            user.watchingErrors = false;
            player.sendMessage("§4You are already watching errors");
            user.save();
        } else {
            player.sendMessage("§5You will be alerted when an error is printed to the server log");
        }
        return true;
    }

    @CodCommand(
        command = "unwatch",
        subcommand = "item",
        weight = 41.3,
        usage = {
            "§2<command> tnt§b Unwatch tnt",
            "§2<command> fire§b Unwatch fire"
        },
        permission = "textplayer.watch.item",
        mustHaveAgreedToTerms = true
    )
    public boolean unwatchItem(Player player, String name) {
        User user = TextPlayer.findUser(player.getName());
        if (user.items.remove(name)) {
            player.sendMessage("§4You are already watching §6" + name);
            user.save();
        } else {
            player.sendMessage("§5You are not watching §6" + name);
        }
        return true;
    }

    @CodCommand(
        command = "unwatch",
        subcommand = "word",
        weight = 41.4,
        usage = {
            "§2<command> <Word>§b Unwatch a word"
        },
        permission = "textplayer.watch.word",
        mustHaveAgreedToTerms = true
    )
    public boolean unwatchWord(Player player, String name) {
        User user = TextPlayer.findUser(player.getName());
        if (user.words.remove(name)) {
            player.sendMessage("§4You are already watching §6" + name);
            user.save();
        } else {
            player.sendMessage("§5You are not watching §6" + name);
        }
        return true;
    }

    @CodCommand(
        command = "list",
        subcommand = "carriers",
        weight = 50,
        usage = {
            "§2<command>§b List supported Carriers"
        }
    )
    public boolean listCarriers(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        for (Carrier carrier : Carrier.values()) {
            if (sb.length() == 0) {
                sb.append("§eSupported Carriers§f: ");
            } else {
                sb.append("§f, §6");
            }
            sb.append(carrier.name());
        }

        sender.sendMessage(sb.toString());
        return true;
    }

    @CodCommand(
        command = "list",
        subcommand = "users",
        weight = 50.1,
        usage = {
            "§2<command>§b List current Users"
        },
        permission = "textplayer.listusers"
    )
    public boolean listUsers(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        for (User tempUser : TextPlayer.getUsers()) {
            if (tempUser.isWhiteListed(sender.getName())) {
                if (sb.length() == 0) {
                    sb.append("§eCurrent Users§f: ");
                } else {
                    sb.append("§f, §6");
                }
                sb.append(tempUser.name);
            }
        }

        sender.sendMessage(sb.toString());
        return true;
    }

    @CodCommand(
        command = "list",
        subcommand = "admins",
        weight = 50.2,
        usage = {
            "§2<command>§b List current Admins"
        },
        permission = "textplayer.listusers"
    )
    public boolean listAdmins(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        for (User tempUser : TextPlayer.getUsers()) {
            if (tempUser.isAdmin() && tempUser.isWhiteListed(sender.getName())) {
                if (sb.length() == 0) {
                    sb.append("§eCurrent Admins§f: ");
                } else {
                    sb.append("§f, §6");
                }
                sb.append(tempUser.name);
            }
        }

        sender.sendMessage(sb.toString());
        return true;
    }

    @CodCommand(
        command = "list",
        subcommand = "watching",
        weight = 50.3,
        usage = {
            "§2<command>§b List what you are watching"
        },
        mustHaveAgreedToTerms = true
    )
    public boolean listWatching(Player player) {
        //Cancel if the Player does not have an existing User
        User user = TextPlayer.findUser(player.getName());
        if (user == null) {
            player.sendMessage("§4You must first add your contact info");
            return true;
        }

        if (player.hasPermission("textplayer.watch.server")) {
            player.sendMessage("§2Watching Server: §6" + user.watchingServer);
        }
        if (player.hasPermission("textplayer.watch.errors")) {
            player.sendMessage("§2Watching Errors: §6" + user.watchingErrors);
        }
        if (player.hasPermission("textplayer.watch.player")) {
            player.sendMessage("§2Watching Players: §6".concat(user.players.toString()));
        }
        if (player.hasPermission("textplayer.watch.item")) {
            player.sendMessage("§2Watching Items: §6".concat(user.items.toString()));
        }
        if (player.hasPermission("textplayer.watch.word")) {
            player.sendMessage("§2Watching Words: §6".concat(user.words.toString()));
        }
        return true;
    }
}
