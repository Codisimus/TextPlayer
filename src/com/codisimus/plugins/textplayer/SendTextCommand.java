package com.codisimus.plugins.textplayer;

import com.codisimus.plugins.textplayer.CommandHandler.CodCommand;
import org.bukkit.entity.Player;

/**
 * Executes Player Commands for sending a message
 *
 * @author Codisimus
 */
public class SendTextCommand {

    @CodCommand(
        command = "&variable",
        weight = 1,
        usage = {
            "§2<command> <Name> <Message>§b Send message to User"
        }
    )
    public boolean text(Player player, String userName, String[] args) {
        User user = TextPlayer.findUser(userName);
        if (user == null) {
            player.sendMessage("§4User §6" + args[0] + " §4not found");
        } else if (!user.isWhiteListed(player.getName())) {
            player.sendMessage("§6" + user.name + " §4has not white listed you");
        } else if (!player.hasPermission("textplayer." + (user.isAdmin() ? "text" : "textadmin"))) {
            player.sendMessage(TextCommand.PERMISSION_MSG);
        } else if (Econ.Charge(player, user.isAdmin())) {
            String msg = player.getName() + ": " + concatArgs(args);
            TextPlayerMailReader.sendMsg(player, user, "Msg from " + player.getName(), msg);
        }
        return true;
    }

    @CodCommand(
        command = "all",
        weight = 2,
        usage = {
            "§2<command> <Message>§b Send message to all Users"
        },
        permission = "textplayer.masstext"
    )
    public boolean massText(Player player, String userName, String[] args) {
        String msg = player.getName() + ": " + concatArgs(args);
        TextPlayer.massText(msg);
        player.sendMessage("§5Mass text has been sent!");
        return true;
    }

    /**
     * Concats arguments together to create a sentence from words.
     *
     * @param args the arguments to concat
     * @return The new String that was created
     */
    public static String concatArgs(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= args.length - 1; i++) {
            sb.append(" ");
            sb.append(args[i]);
        }
        return sb.substring(1);
    }
}