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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.nio.charset.Charset;

import java.net.Socket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class SocketListenerThread extends Thread {
    private Config config;
    private Plugin parentPlugin;
    protected ServerSocket socket;
    private List<ConnectionHandler> handlers;
   
    public SocketListenerThread (Plugin parentPlugin, Config config) {
        super("mccmd socket listener thread");

        this.parentPlugin = parentPlugin;
        this.config = config;
        this.handlers = new ArrayList<ConnectionHandler>();
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
                    for (ConnectionHandler handler : this.handlers) {
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

            ConnectionHandler handler = new ConnectionHandler(client, this.parentPlugin, this.config);
            this.handlers.add(handler); 
            handler.start();
        }
    }

    private class ConnectionHandler extends Thread {
        Config config; 
        Socket client;
        Plugin parentPlugin;
        Charset encoding;

        public ConnectionHandler (Socket client, Plugin parentPlugin, Config config) {
            super("mccmd connection handler");

            this.client = client;
            this.config = config;
            this.parentPlugin = parentPlugin;
    
            try {
                this.encoding = Charset.forName("UTF-8");
            } catch (IllegalArgumentException e) {
                this.parentPlugin.getLogger().severe("couldn't register utf-8 as a charset. get ready for some fun.");
                e.printStackTrace();
            }
        }

        private List<String> exec (String command) {
            List<String> replies;

            // create a throwaway object that will collect replies to the command 
            MessageReceiver receiver = new MessageReceiver(this.config.op);

            // give the permissions stated in the config file (if any)
            for (String name : this.config.permissions) {
                receiver.addAttachment(this.parentPlugin, name, true);
            }

            Bukkit.getServer().dispatchCommand(receiver, command);
            replies = receiver.getMessages();
            
            // throw away the object
            receiver.stopAcceptingMessages();
            receiver = null;
            
            return replies;
        }

        // converts a string to a utf-8 encoded bytearray
        private byte[] toBytes (String victim) {
            return victim.getBytes(this.encoding);
        }

        private byte[] formResponseBody (List<String> replies) {
            StringBuilder builder = new StringBuilder();

            for (String reply : replies) {
                builder.append(reply);
                builder.append("\n");
            }

            return this.toBytes(builder.toString());
        }

        public void run () {
            try {
                // XXX stop being lazy and implement a this.fromBytes() and use a standalone InputStream
                BufferedReader input = new BufferedReader(new InputStreamReader(this.client.getInputStream(), this.encoding));
                OutputStream output = this.client.getOutputStream();

                String command;

                while ((command = input.readLine()) != null) {
                    List<String> replies = this.exec(command);
                    byte[] body = this.formResponseBody(replies);
                    byte[] header = this.toBytes(Integer.toString(body.length)+"\n");
                   
                    output.write(header);
                    output.write(body);

                    output.flush();
                }
                this.client.close();
            } catch (IOException e) {
                if (this.client.isClosed()) {
                    return;
                } else {
                    this.parentPlugin.getLogger().warning("communication with client failed!");
                    // XXX same concern as above
                    e.printStackTrace();

                    try {
                        this.client.close();
                    } catch (IOException f) {
                        this.parentPlugin.getLogger().warning("failed to close the client connection");
                        // XXX same concern as above
                        f.printStackTrace();
                    }
                }
            }
        }
    }
}
