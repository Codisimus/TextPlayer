package com.codisimus.plugins.textplayer;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

/**
 * Manages payment/rewards of using Warps
 * 
 * @author Codisimus
 */
public class Econ {
    public static Economy economy;
    static int cost;
    static int costAdmin;
    
    /**
     * Charges a Player a predetermined amount of money
     * 
     * @param player The name of the Player to be charged
     * @param admin True if the Player will be charged for texting an Admin
     * @return True if the transaction was successful
     */
    public static boolean Charge(Player player, boolean admin) {
        //Determine the price to be charged
        int price = admin ? costAdmin : cost;
        String playerName = player.getName();
        
        //Charge if the price is not 0 and the Player does not have the 'textplayer.free' node
        if (cost > 0 && !TextPlayer.hasPermission(player, "free")) {
            //Return false if the Player has insufficient funds
            if (!economy.has(playerName, price)) {
                player.sendMessage("§4You need §6"+format(price)+" §4to message that user");
                return false;
            }
            
            economy.withdrawPlayer(playerName, price);
            player.sendMessage("§5Charged §6"+format(price)+" §5to send message");
        }
        
        return true;
    }
    
    /**
     * Formats the money amount by adding the unit
     * 
     * @param amount The amount of money to be formatted
     * @return The String of the amount + currency name
     */
    public static String format(double amount) {
        return economy.format(amount).replace(".00", "");
    }
}
