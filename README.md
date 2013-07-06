mccmd
=====

a suite for elegantly running a minecraft server under [systemd][].

It currently consists of:

1. a systemd service file for craftbukkit
2. a java craftbukkit plugin implementing the [mccmd protocol][]
3. a c client for the mccmd protocol

All components are under the [MIT License][].

systemd unit
------------
`minecraft.service` is a template systemd unit for craftbukkit. While
it is a complete and working service file, you should modify it to your own
requirements. Keep in mind, though, that running the minecraft server as its
own user and group in its own directory is probably a good idea.

Like all custom units, `minecraft.service` should live in
`/etc/systemd/system/`.

bukkit plugin
-------------
You can build the plugin with the bundled Makefile, but a (possibly outdated)
prebuilt version is available [here][plugindownload] if you're feeling lazy.

    $ git clone git://github.com/ausbin/mccmd.git mccmd
    $ make Mccmd.jar

The [yaml][] config file is pretty simple. If a config file doesn't
already exist, the defaults are copied over when the plugin is enabled.
By default, each command is sent from a [CommandSender][] that claims to
be an op, but you can turn this off and/or manually set permissions if 
you want.

client
------
The client is a very simple C program that reads lines from stdin and sends
them to a mccmd server. If both stdin and stdout are ttys, it provides a cute 
little interactive prompt with readline support.
By default in an interactive session, minecraft chat formatting (colors and
styles) are converted to [ANSI escape sequences][].
For a list of the conversions, [look at the 
code](https://github.com/ausbin/mccmd/blob/master/mccmd.c). 

    $ git clone git://github.com/ausbin/mccmd.git mccmd
    $ make mccmd
    $ ./mccmd -h
    usage: mccmd [-r|-i|-c|-h] [host [port]]
    
    send commands to a minecraft server.
    (default host is localhost, default port is 5454)
    
    -r  remove minecraft chat formatting
        (the default when not in a tty)
    -i  leave minecraft chat formatting intact
    -c  convert minecraft chat formatting into ansi
        escape sequences (the default in a tty)
    -h  show this message
    
    https://github.com/ausbin/mccmd

The client requires a compiler that supports [C89][] and a system with 
[POSIX][] and [readline][]. If you're on Arch, just download the [PKGBUILD][]
into a fresh directory and run `makepkg`.

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
[client][] converts them to [ANSI escape sequences][] by default in
interactive sessions)

[systemd]: http://freedesktop.org/wiki/Software/systemd/
[mccmd protocol]: #protocol
[client]: #client
[controlseq]: http://wiki.vg/Chat#Control_Sequences
[UTF-8]: http://utf-8.com/
[ANSI escape sequences]: https://en.wikipedia.org/wiki/ANSI_escape_code
[plugindownload]: http://206.253.166.8/~austin/builds/Mccmd.jar
[CommandSender]: http://jd.bukkit.org/dev/apidocs/org/bukkit/command/CommandSender.html
[C89]: https://en.wikipedia.org/wiki/ANSI_C
[POSIX]: https://en.wikipedia.org/wiki/POSIX
[readline]: http://cnswww.cns.cwru.edu/php/chet/readline/rltop.html
[yaml]: https://en.wikipedia.org/wiki/YAML
[PKGBUILD]: https://raw.github.com/ausbin/mccmd/master/PKGBUILD
[MIT License]: https://en.wikipedia.org/wiki/MIT_License
