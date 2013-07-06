# client (C) stuff
CFLAGS = 

# java misery (plugin)
JAR = jar
JAVAC = javac
PLUGIN = plugin
NAMESPACE = io/github/ausbin/mccmd
CLASSES = $(wildcard $(PLUGIN)/$(NAMESPACE)/*.java)
COMPILED = $(CLASSES:%.java=%.class)
CONFIG = $(wildcard $(PLUGIN)/*.yml)
CURL = curl
BUKKITJAR = bukkit.jar
BUKKITURL = http://dl.bukkit.org/downloads/bukkit/get/02168_1.5.2-R1.0/bukkit.jar

all: mccmd Mccmd.jar

mccmd: mccmd.c
	$(CC) -lreadline $(CFLAGS) -o $@ $<

# retrieve a compatible copy of the bukkit api
# sort of a hack, but it works. if you don't want to use this,
# pass BUKKITJAR=/path/to/my/bukkit.jar as an argument to make.
$(BUKKITJAR):
	$(CURL) -Lo $@ $(BUKKITURL)

# each javac call requires a JVM spinup, so cherrypicking and calling 
# javac on every modified source file is actually slower than using a 
# single javac call to rebuild everything
$(COMPILED): $(CLASSES) $(BUKKITJAR)
	$(JAVAC) -classpath $(BUKKITJAR) $(JAVACFLAGS) $(CLASSES)

Mccmd.jar: $(COMPILED) $(CONFIG)
	$(JAR) cf $@ $(CONFIG:$(PLUGIN)/%=-C $(PLUGIN) %) \
	             $(COMPILED:$(PLUGIN)/%=-C $(PLUGIN) %) 

clean: 
	rm -f $(BUKKITJAR) $(COMPILED) mccmd Mccmd.jar
