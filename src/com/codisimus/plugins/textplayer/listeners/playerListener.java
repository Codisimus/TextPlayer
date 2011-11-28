package com.codisimus.plugins.textplayer.listeners;

import com.codisimus.plugins.textplayer.SaveSystem;
import com.codisimus.plugins.textplayer.TextPlayer;
import com.codisimus.plugins.textplayer.User;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for logging and speaking events to alert Users
 *
 * @author Codisimus
 */
public class playerListener extends PlayerListener {

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
                
                //Find the logged User if they exist
                for (User user: SaveSystem.users)
                    if (user.name.equals(logged)) {
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
                        user.online = false;
                        break;
                    }
                
                //Send an alert to each Player watching the Player who logged
                for (User user: SaveSystem.users)
                    if (user.players.contains("*") || user.players.contains(logged))
                        mailListener.sendMsg(null, user, logged+" has logged off");
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
        if (SaveSystem.findUser(logged).online)
            return;
        
        //Send an alert to each Player watching the Player who logged
        for (User user: SaveSystem.users)
            if (user.players.contains("*") || user.players.contains(logged))
                mailListener.sendMsg(null, user, logged+" has logged on");
    }

    /**
     * Sends alerts when Players speak watched words
     * 
     * @param event The PlayerJoinEvent that occurred
     */
    @Override
    public void onPlayerChat (PlayerChatEvent event) {
        //Send an alert to each Player that is watching a word that was spoken
        String msg = event.getMessage();
        for (User user: SaveSystem.users)
            if (user.words.contains(msg))
                mailListener.sendMsg(null, user, event.getPlayer().getName()+": "+msg);
    }
}