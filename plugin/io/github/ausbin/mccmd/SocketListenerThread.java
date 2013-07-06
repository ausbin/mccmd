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

import java.io.IOException;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

class SocketListenerThread extends Thread {
    private Config config;
    private Plugin parentPlugin;
    protected ServerSocket socket;
    private List<ConnectionThread> handlers;
   
    public SocketListenerThread (Plugin parentPlugin, Config config) {
        super("mccmd socket listener thread");

        this.parentPlugin = parentPlugin;
        this.config = config;
        this.handlers = new ArrayList<ConnectionThread>();
    }

    //@Override
    public void run () {
        try {
            this.socket = new ServerSocket(this.config.port, 0, this.config.addr);
            this.socket.setReuseAddress(true);
        } catch (IOException e) {
            this.parentPlugin.getLogger().severe("couldn't bind to port " + this.config.port + "! bailing out");

            // XXX is there a better way to do this? i would use
            // Logger.throwing(), but that sets the severity of the traceback
            // to FINEST, which is too low for the vanilla Logger to display.
            // (this code puts it under SEVERE somehow)
            e.printStackTrace();
            return;
        }

        while (true) {
            Socket client;

            try {
                client = this.socket.accept();
            } catch (IOException e) { 
                // if the main thread closed our socket, end the thread
                if (this.socket.isClosed()) {
                    // kill each of the open connections
                    for (ConnectionThread handler : this.handlers) {
                        if (handler.isAlive()) {
                            try {
                                handler.client.close();
                            } catch (IOException f) {
                                this.parentPlugin.getLogger().warning("couldn't close a client connection while shutting down");
                                // XXX same concern as above
                                f.printStackTrace();
                            }
                        }
                    }
                    // end this thread
                    return;
                // otherwise it was probably a random networking problem.
                // choke out an error and move on the the next connection.
                } else {
                    this.parentPlugin.getLogger().warning("accept() failed");
                    // XXX same concern as above
                    e.printStackTrace();
                    continue;
                }
            }

            ConnectionThread handler = new ConnectionThread(client, this.parentPlugin, this.config);
            this.handlers.add(handler); 
            handler.start();
        }
    }
}
