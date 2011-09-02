
package TextPlayer;

import com.nijikokun.register.payment.Method;
import com.nijikokun.register.payment.Method.MethodAccount;
import org.bukkit.entity.Player;

/**
 *
 * @author Codisimus
 */
public class Register {
    protected static String economy;
    protected static Method econ;
    protected static int cost;
    protected static int costAdmin;

    protected static boolean Charge(Player player, boolean admin) {
        int price = cost;
        if (admin)
            price = costAdmin;
        if (cost > 0 && !TextPlayer.hasPermission(player, "free")) {
            MethodAccount account = econ.getAccount(player.getName());
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
