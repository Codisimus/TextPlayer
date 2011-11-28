package com.codisimus.plugins.textplayer.listeners;

import com.codisimus.plugins.textplayer.Register;
import com.codisimus.plugins.textplayer.TextPlayer;
import com.codisimus.plugins.textplayer.register.payment.Methods;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import ru.tehkode.permissions.bukkit.PermissionsEx;

/**
 * Checks for Permission/Economy Plugins whenever a Plugin is enabled
 * 
 * @author Codisimus
 */
public class pluginListener extends ServerListener {
    public static boolean useBP;

    /**
     * Executes methods to look for various types of Plugins to link
     *
     * @param event The PluginEnableEvent that occurred
     */
    @Override
    public void onPluginEnable(PluginEnableEvent event) {
        linkPermissions();
        linkEconomy();
    }

    /**
     * Finds and links a Permission Plugin
     *
     */
    public void linkPermissions() {
        //Return if we have already have a permissions Plugin
        if (TextPlayer.permissions != null)
            return;

        //Return if PermissionsEx is not enabled
        if (!TextPlayer.pm.isPluginEnabled("PermissionsEx"))
            return;

        //Return if BukkitPermissions will be used
        if (useBP)
            return;

        TextPlayer.permissions = PermissionsEx.getPermissionManager();
        System.out.println("[TextPlayer] Successfully linked with PermissionsEx!");
    }

    /**
     * Finds and links an Economy Plugin
     *
     */
    public void linkEconomy() {
        //Return if we already have an Economy Plugin
        if (Methods.hasMethod())
            return;

        //Return if no Economy is wanted
        if (Register.economy.equalsIgnoreCase("none"))
            return;

        //Set the preferred Plugin if there is one
        if (!Register.economy.equalsIgnoreCase("auto"))
            Methods.setPreferred(Register.economy);

        //Find an Economy Plugin (will first look for the preferred Plugin)
        Methods.setMethod(TextPlayer.pm);
        
        //Return if no Economy Plugin was found
        if (!Methods.hasMethod())
            return;

        //Reset Methods if the preferred Economy was not found
        if (!Methods.getMethod().getName().equalsIgnoreCase(Register.economy) && !Register.economy.equalsIgnoreCase("auto")) {
            Methods.reset();
            return;
        }

        Register.econ = Methods.getMethod();
        System.out.println("[TextPlayer] Successfully linked with "+Register.econ.getName()+" "+Register.econ.getVersion()+"!");
    }
}