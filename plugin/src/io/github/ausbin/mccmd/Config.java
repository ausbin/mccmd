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
