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

import java.util.List;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.bukkit.configuration.file.FileConfiguration;

public class Config {
    public int port;
    public boolean op;
    public InetAddress addr;
    public List<String> permissions;

    public void reload (FileConfiguration config) {
        // load config
        this.op = config.getBoolean("op");
        this.port = config.getInt("bind.port");

        try {
            this.addr = InetAddress.getByName(config.getString("bind.addr"));
        } catch (UnknownHostException e) {
            // XXX log an error instead of silently failing
            // XXX use what's in the default config.yml instead of this
            //     (what's going on here is sort of a violation of DRY)
            this.addr = InetAddress.getLoopbackAddress();
        }

        this.permissions = config.getStringList("permissions");
    }

    public Config (FileConfiguration config) {
        this.reload(config);
    }
}
