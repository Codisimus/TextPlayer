package com.codisimus.plugins.textplayer.listeners;

import com.codisimus.plugins.textplayer.TextPlayer;
import com.codisimus.plugins.textplayer.User;
import java.util.LinkedList;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for logging and speaking events to alert Users
 *
 * @author Codisimus
 */
public class PlayerEventListener extends PlayerListener {
    static LinkedList<String> online = new LinkedList<String>();

    /**
     * Sends alerts when Players log off
     * Alerts are delayed for one minute in case the Player logs back in
     * 
     * @param event The PlayerQuitEvent that occurred
     */
    @Override
    public void onPlayerQuit (final PlayerQuitEvent event) {
        //Start a new Thread
        Thread check = new Thread() {
            @Override
            public void run() {
                String logged = event.getPlayer().getName();
                
                //Wait for one minute
                try {
                    Thread.currentThread().sleep(60000);
                }
                catch (Exception e) {
                }

                //Return if the Player logged back on
                if (TextPlayer.server.getPlayer(logged) != null)
                    return;

                //Set the User as offline
                online.remove(logged);
                
                //Send an alert to each Player watching the Player who logged
                for (User user: TextPlayer.users)
                    if ((user.players.contains("*") || user.players.contains(logged))
                            && !user.name.equals(logged))
                        MailListener.sendMsg(null, user, logged+" has logged off");
            }
        };
        check.start();
    }
    
    /**
     * Sends alerts when Players log on
     * 
     * @param event The PlayerJoinEvent that occurred
     */
    @Override
    public void onPlayerJoin (PlayerJoinEvent event) {
        //Return if the Player was online less than a minute ago
        String logged = event.getPlayer().getName();
        if (online.contains(logged))
            return;
        
        online.add(logged);
        
        //Send an alert to each Player watching the Player who logged
        for (User user: TextPlayer.users)
            if ((user.players.contains("*") || user.players.contains(logged))
                    && !user.name.equals(logged))
                MailListener.sendMsg(null, user, logged+" has logged on");
    }

    /**
     * Sends alerts when Players speak watched words
     * 
     * @param event The PlayerJoinEvent that occurred
     */
    @Override
    public void onPlayerChat (PlayerChatEvent event) {
        String msg = event.getMessage();
        String player = event.getPlayer().getName();
        
        //Send an alert to each Player that is watching a word that was spoken
        for (User user: TextPlayer.users)
            if (user.words.contains(msg) && !user.name.equals(player))
                MailListener.sendMsg(null, user, event.getPlayer().getName()+": "+msg);
    }
}