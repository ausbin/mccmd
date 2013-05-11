package io.github.ausbin.mccmd;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class MessageReceiver implements CommandSender, Permissible, ServerOperator {
    private boolean op;
    private List<String> messages;
    private PermissibleBase perm;
    private boolean acceptingMessages;
    
    public MessageReceiver (boolean op) {
        this.op = op;
        this.messages = new ArrayList<String>();
        this.perm = new PermissibleBase(this);
        this.acceptingMessages = true;
    }

    protected void stopAcceptingMessages () {
        this.acceptingMessages = false;
        this.messages = null;
    }

    protected List<String> getMessages () {
        return this.messages;
    }

    // just for good measure
    public boolean isPlayer () {
        return false;
    }

    /// methods for CommandSender

    public void sendMessage (String message) {
        if (this.acceptingMessages) {
            this.messages.add(message);
        }
    }
    
    public void sendMessage (String[] messages) {
        if (this.acceptingMessages) {
            for (String message : messages) {
                this.messages.add(message);
            }
        }
    }

    public String getName () {
        return "mccmd";
    }

    public Server getServer () {
        return Bukkit.getServer();
    }

    /// methods for ServerOperator

    public boolean isOp () {
        return this.op;
    }

    public void setOp (boolean value) {
        throw new UnsupportedOperationException("cannot change operator status of a throwaway mccmd command receiver");
    }

    /// methods for Permissible

    public PermissionAttachment addAttachment (Plugin plugin) {
        return this.perm.addAttachment(plugin);
    }

    public PermissionAttachment addAttachment (Plugin plugin, int ticks) {
        return this.perm.addAttachment(plugin, ticks);
    }

    public PermissionAttachment addAttachment (Plugin plugin, String name, boolean value) {
        return this.perm.addAttachment(plugin, name, value);
    }

    public PermissionAttachment addAttachment (Plugin plugin, String name, boolean value, int ticks) {
        return this.perm.addAttachment(plugin, name, value, ticks);
    }

    public Set<PermissionAttachmentInfo> getEffectivePermissions () {
        return this.perm.getEffectivePermissions();
    }

    public boolean hasPermission (Permission perm) {
        return this.perm.hasPermission(perm);
    }

    public boolean hasPermission (String name) {
        return this.perm.hasPermission(name);
    }

    public boolean isPermissionSet (Permission perm) {
        return this.perm.isPermissionSet(perm);
    }

    public boolean isPermissionSet (String name) {
        return this.perm.isPermissionSet(name);
    }

    public void recalculatePermissions () {
        this.perm.recalculatePermissions();
    }

    public void removeAttachment (PermissionAttachment attachment) {
        this.perm.removeAttachment(attachment);
    }
}
