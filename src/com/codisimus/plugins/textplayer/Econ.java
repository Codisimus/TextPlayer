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
    public static int cost;
    public static int costAdmin;

    /**
     * Charges a Player a given amount of money, which goes to a Player/Bank
     * 
     * @param player The name of the Player to be charged
     * @param source The Player/Bank that will receive the money
     * @param amount The amount that will be charged
     * @return True if the transaction was successful
     */
    public static boolean charge(Player player, String source, double amount) {
        String name = player.getName();
        
        //Cancel if the Player cannot afford the transaction
        if (!economy.has(name, amount)) {
            player.sendMessage("You need "+economy.format(amount)+" to activate that");
            return false;
        }
        
        economy.withdrawPlayer(name, amount);
        
        //Money does not go to anyone if the source is the server
        if (source.equalsIgnoreCase("server"))
            return true;
        
        if (source.startsWith("bank:"))
            //Send money to a bank account
            economy.bankDeposit(source.substring(5), amount);
        else
            //Send money to a Player
            economy.depositPlayer(source, amount);
        
        return true;
    }
    
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
                player.sendMessage("You need "+format(price)+" to message that user");
                return false;
            }
            
            economy.withdrawPlayer(playerName, price);
            player.sendMessage("Charged "+format(price)+" to send message");
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
