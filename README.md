mccmd
=====

a suite for elegantly running a minecraft server under [systemd][].

It currently consists of:

1. a systemd service file for craftbukkit
2. a java craftbukkit plugin implementing the [mccmd protocol][]
3. a python client for the mccmd protocol

All components are under the [MIT License][].

An Arch PKGBUILD for the client and systemd unit can be found 
[here][PKGBUILD]. A source tarball is [here][sourcetarball].

systemd unit
------------
By default, `minecraft.service` is set up to run craftbukkit in
`/srv/minecraft/` as the user "minecraft" and the group "minecraft".

You should either create those or modify the content of the unit once
it's in `/etc/systemd/system/`.

bukkit plugin
-------------
You can download a build of the mccmd bukkit plugin from 
[here][plugindownload], but if you want to build it yourself,
try this (you'll have to find a suitable bukkit build for yourself, of course):

    $ git clone git://github.com/ausbin/mccmd.git mccmd
    $ cd mccmd/plugin/
    $ curl -o bukkit.jar http://repo.bukkit.org/content/groups/public/org/bukkit/bukkit/$bukkitrel/$bukkitbuild.jar
    $ CLASSPATH=bukkit.jar ./build.sh

The [yaml][] config file is pretty simple. If a config file doesn't
already exist, the defaults are copied over when the plugin is enabled.
By default, each command is sent from a [CommandSender][] that claims to
be an op, but you can turn this off and/or manually set permissions if 
you want.

client
------
The client is a crappy python 3 script that provides an interactive
prompt for sending commands to a server (if both stdin and stdout
are ttys). It converts [Minecraft chat control sequences][controlseq]
to [ANSI escape sequences][]. For a list of the conversions, [look at
the code](https://github.com/ausbin/mccmd/blob/master/mccmd.py). 

    $ ./mccmd.py --help
    usage: mccmd.py [-h] [-p] [-c] [host] [port]

    send commands to a craftbukkit server via the mccmd plugin and/or convert or
    remove minecraft chat control characters.

    positional arguments:
      host           host to connect to (defaults to localhost)
      port           port to connect to on 'host' (defaults to 5454)

    optional arguments:
      -h, --help     show this help message and exit
      -p, --process  convert or remove minecraft chat control characters in stdin
                     instead of connecting to a server (nullifies all other
                     options except for -c/--nocolor)
      -c, --nocolor  discard minecraft chat control sequences instead of
                     converting them directly to ansi control sequences

    https://github.com/ausbin/mccmd

The client can also be used to process minecraft chat control sequences via
the `--process` option. The `--nocolor` flag can be paired with it,
which is handy for cleaning out text that was formatted for minecraft
chat.

    $ echo "§d§lpretty pink so pretty" | ./mccmd.py --process --nocolor
    pretty pink so pretty

protocol
--------
The mccmd protocol has no authentication or encryption. It's designed to
be used locally, although both the client and server support only TCP
sockets.
(Unix Domain Sockets would be a great solution, but Java is more or 
less castrated in terms of platform-specific features and there
aren't really any non-dead Java libraries for unix sockets available).

The default TCP port for mccmd is 5454. The protocol, on a high level,
goes something like this:

1. client sends a command terminated by a newline
2. server responds with a two-part message
    1. the header, which contains, in text form, the number of bytes in
       the *body*. terminated by a newline
    2. the body, containing the server's response to the issued
       command. [Minecraft chat control sequences][controlseq]
       must be filtered or converted by the client

All messages are encoded in [UTF-8][]. This is a good idea anyway, but
it's necessary because many responses contain §, which signifies the 
start of a [minecraft chat control sequence][controlseq].

For example, sending `help\n` produces:

    524
    §e--------- §fHelp: Index (1/6) §e---------------------
    §7Use /help [n] to get page n of help.
    §6Aliases: §fLists command aliases
    §6Bukkit: §fAll commands for Bukkit
    §6dynmap: §fAll commands for dynmap
    §6Motd: §fAll commands for Motd
    §6/ban: §fPrevents the specified player from using this server
    §6/ban-ip: §fPrevents the specified IP address from using this server
    §6/banlist: §fView all players banned from this server
    §6/clear: §fClears the player's inventory. Can specify item and data filters too.

Notice that 524 is in text form and measures the number of bytes left
*after* the newline that terminates the header. Also notice that
[minecraft chat control sequences][controlseq] are intact. (The bundled
[client][] converts them to [ANSI escape sequences][] automatically by
default)

[systemd]: http://freedesktop.org/wiki/Software/systemd/
[mccmd protocol]: #protocol
[client]: #client
[controlseq]: http://wiki.vg/Chat#Control_Sequences
[UTF-8]: http://utf-8.com/
[ANSI escape sequences]: https://en.wikipedia.org/wiki/ANSI_escape_code
[plugindownload]: http://206.253.166.8/~austin/builds/mccmd.jar
[CommandSender]: http://jd.bukkit.org/dev/apidocs/org/bukkit/command/CommandSender.html
[yaml]: https://en.wikipedia.org/wiki/YAML
[PKGBUILD]: https://raw.github.com/ausbin/mccmd/master/PKGBUILD
[sourcetarball]: http://206.253.166.8/~austin/builds/mccmd.tar.xz
[MIT License]: https://en.wikipedia.org/wiki/MIT_License
