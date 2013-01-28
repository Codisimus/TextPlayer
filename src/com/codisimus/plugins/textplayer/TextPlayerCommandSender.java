package com.codisimus.plugins.textplayer;

import java.util.Set;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

/**
 * Dispatches Commands for the PhatLoots Plugin
 *
 * @author Codisimus
 */
public class TextPlayerCommandSender implements CommandSender {
    User user;

    public TextPlayerCommandSender(User user) {
        this.user = user;
    }

    @Override
    public void sendMessage(String string) {
        TextPlayerMailReader.sendMsg(null, user, "[TextPlayer] Reply for input", string);
    }

    @Override
    public void sendMessage(String[] strings) {
        for (String string: strings) {
            sendMessage(string);
        }
    }

    @Override
    public Server getServer() {
        return TextPlayer.server;
    }

    @Override
    public String getName() {
        return user.name;
    }

    @Override
    public boolean isPermissionSet(String string) {
        return user.isAdmin();
    }

    @Override
    public boolean isPermissionSet(Permission prmsn) {
        return user.isAdmin();
    }

    @Override
    public boolean hasPermission(String string) {
        return user.isAdmin();
    }

    @Override
    public boolean hasPermission(Permission prmsn) {
        return user.isAdmin();
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String string, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String string, boolean bln, int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeAttachment(PermissionAttachment pa) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void recalculatePermissions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isOp() {
        return user.isAdmin();
    }

    @Override
    public void setOp(boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
