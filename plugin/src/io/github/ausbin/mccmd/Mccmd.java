package io.github.ausbin.mccmd;

import java.net.InetAddress;
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
