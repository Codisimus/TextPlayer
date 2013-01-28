package com.codisimus.plugins.textplayer;

import java.util.LinkedList;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for logging and speaking events to alert Users
 *
 * @author Codisimus
 */
public class TextPlayerListener implements Listener {
    static LinkedList<String> online = new LinkedList<String>();

    /**
     * Sends alerts when Players log off
     * Alerts are delayed for one minute in case the Player logs back in
     *
     * @param event The PlayerQuitEvent that occurred
     */
    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final String logged = event.getPlayer().getName();

        //Execute in one minute
        TextPlayer.server.getScheduler().runTaskLaterAsynchronously(TextPlayer.plugin, new Runnable() {
            @Override
            public void run() {
                //Return if the Player logged back on
                if (TextPlayer.server.getPlayer(logged) != null) {
                    return;
                }

                //Set the User as offline
                online.remove(logged);

                //Send an alert to each Player watching the Player who logged
                for (User user: TextPlayer.getUsers()) {
                    if ((user.players.contains("*") || user.players.contains(logged.toLowerCase()))
                            && !user.name.equals(logged)) {
                        TextPlayerMailReader.sendMsg(null, user, "TextPlayer Player Watcher", logged + " has logged off");
                    }
                }
            }
        }, 1200L);
    }

    /**
     * Sends alerts when Players log on
     *
     * @param event The PlayerJoinEvent that occurred
     */
    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        //Return if the Player was online less than a minute ago
        String logged = event.getPlayer().getName();
        if (online.contains(logged)) {
            return;
        }

        online.add(logged);

        //Send an alert to each Player watching the Player who logged
        for (User user: TextPlayer.getUsers()) {
            if ((user.players.contains("*") || user.players.contains(logged.toLowerCase()))
                    && !user.name.equals(logged)) {
                TextPlayerMailReader.sendMsg(null, user, "TextPlayer Player Watcher", logged + " has logged on");
            }
        }
    }

    /**
     * Sends alerts when Players speak watched words
     *
     * @param event The PlayerJoinEvent that occurred
     */
    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String msg = event.getMessage();
        String player = event.getPlayer().getName();

        //Send an alert to each Player that is watching a word that was spoken
        for (User user: TextPlayer.getUsers()) {
            if (!user.name.equals(player)) {
                for (String word: user.words) {
                    if (msg.contains(word)) {
                        TextPlayerMailReader.sendMsg(null, user, "Someone said " + word, event.getPlayer().getName() + ": " + msg);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Sends alerts to Players watching TNT
     *
     * @param event The BlockPlaceEvent that occurred
     */
    @EventHandler (priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }

        //Cancel if the event was not placing TNT
        if (event.getBlock().getTypeId() != 46) {
            return;
        }

        String player = event.getPlayer().getName();

        //Send an alert to each Player watching TNT
        for (User user: TextPlayer.getUsers()) {
            if (user.items.contains("tnt")) {
                TextPlayerMailReader.sendMsg(null, user, "Possible Griefing", player + " has placed TNT");
            }
        }
    }

    /**
     * Sends alerts to Players watching fire
     *
     * @param event The BlockIgniteEvent that occurred
     */
    @EventHandler (priority = EventPriority.MONITOR)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.isCancelled()) {
            return;
        }

        //Cancel if the event was not caused by a Player
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        //Send an alert to each Player watching fire
        for (User user: TextPlayer.getUsers()) {
            if (user.items.contains("fire")) {
                TextPlayerMailReader.sendMsg(null, user, "Possible Griefing", player.getName() + " has lit a fire");
            }
        }
    }
}
