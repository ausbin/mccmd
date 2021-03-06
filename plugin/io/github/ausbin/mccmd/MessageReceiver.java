/*
 * Copyright (c) 2013 Austin Adams
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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

class MessageReceiver implements CommandSender, Permissible, ServerOperator {
    private boolean op;
    // string representation of the remote host (e.g. "10.0.2.1:3242")
    private String remote;
    private List<String> messages;
    private PermissibleBase perm;
    private boolean acceptingMessages;
    
    public MessageReceiver (boolean op, String remote) {
        this.op = op;
        this.remote = remote;
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
        return "mccmd@" + this.remote;
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
