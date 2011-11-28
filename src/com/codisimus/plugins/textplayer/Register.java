package com.codisimus.plugins.textplayer;

import com.codisimus.plugins.textplayer.register.payment.Method;
import com.codisimus.plugins.textplayer.register.payment.Method.MethodAccount;
import org.bukkit.entity.Player;

/**
 * Manages payment/rewards of using Warps
 * Uses Nijikokun's Register API
 *
 * @author Codisimus
 */
public class Register {
    public static String economy;
    public static Method econ;
    public static int cost;
    public static int costAdmin;

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
        
        //Charge if the price is not 0 and the Player does not have the 'textplayer.free' node
        if (cost > 0 && !TextPlayer.hasPermission(player, "free")) {
            MethodAccount account = econ.getAccount(player.getName());
            
            //Return false if the Player has insufficient funds
            if (!account.hasEnough(price)) {
                player.sendMessage("You need "+econ.format(price)+" to message that user");
                return false;
            }
            
            account.subtract(price);
            player.sendMessage("Charged "+econ.format(price)+" to send message");
        }
        
        return true;
    }
}
