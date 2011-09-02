
package TextPlayer;

import java.util.LinkedList;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;


/**
 *
 * @author Codisimus
 */
public class TextPlayerBlockListener extends BlockListener {

    @Override
    public void onBlockPlace (BlockPlaceEvent event) {
        String block = event.getBlock().getType().name().toLowerCase();
        String player = event.getPlayer().getName();
        if (block.equals("tnt")) {
            LinkedList<User> users = SaveSystem.getUsers();
            for(User user : users) {
                if (user.isWatchingItem("tnt"))
                    Mailer.sendMsg(null, user, player+" has placed tnt");
            }
        }
        else if(block.equals("lava")) {
            LinkedList<User> users = SaveSystem.getUsers();
            for(User user : users) {
                if (user.isWatchingItem("lava"))
                    Mailer.sendMsg(null, user, player+" has placed lava");
            }
        }
    }

    @Override
    public void onBlockIgnite (BlockIgniteEvent event) {
        Player player = event.getPlayer();
        if (player != null) {
            LinkedList<User> users = SaveSystem.getUsers();
            for(User user : users) {
                if (user.isWatchingItem("fire"))
                    Mailer.sendMsg(null, user, player.getName()+" has lit a fire");
            }
        }
    }
}
