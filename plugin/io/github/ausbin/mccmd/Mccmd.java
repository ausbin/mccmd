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
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

public class Mccmd extends JavaPlugin {
    Config config;
    SocketListenerThread listener;

    @Override
    public void onEnable () {
        // copy over the default config.yml
        this.saveDefaultConfig();

        this.config = new Config(this.getConfig());
        this.listener = new SocketListenerThread(this, this.config);
        this.listener.start();
    }

    @Override
    public void onDisable () {
        // cause the accept() the thread is waiting on to throw a
        // SocketException. this frees up the thread and allows it
        // to close the socket and cannibalize itself.
        try {
            this.listener.socket.close();
        } catch (IOException e) {
            // we don't care.
        }

        this.listener = null;
    }
}
