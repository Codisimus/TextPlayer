package com.codisimus.plugins.textplayer.listeners;

import com.codisimus.plugins.textplayer.SaveSystem;
import com.codisimus.plugins.textplayer.User;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Listens for griefing events to alert Users
 *
 * @author Codisimus
 */
public class blockListener extends BlockListener {

    /**
     * Sends alerts to Players watching TNT
     * 
     * @param event The BlockPlaceEvent that occurred
     */
    @Override
    public void onBlockPlace (BlockPlaceEvent event) {
        //Cancel if the event was not placing TNT
        if (event.getBlock().getTypeId() != 46)
            return;
        
        String player = event.getPlayer().getName();
        
        //Send an alert to each Player watching TNT
        for(User user: SaveSystem.users)
            if (user.items.contains("tnt"))
                mailListener.sendMsg(null, user, player+" has placed TNT");
    }

    /**
     * Sends alerts to Players watching fire
     * 
     * @param event The BlockIgniteEvent that occurred
     */
    @Override
    public void onBlockIgnite (BlockIgniteEvent event) {
        //Cancel if the event was not caused by a Player
        Player player = event.getPlayer();
        if (player == null)
           return;
        
        //Send an alert to each Player watching fire
        for(User user: SaveSystem.users)
            if (user.items.contains("fire"))
                mailListener.sendMsg(null, user, player.getName()+" has lit a fire");
    }
}
