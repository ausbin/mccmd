#!/bin/bash
# warning: this ugly hack may cause aneurysms

[ -z "$CLASSPATH" ] && {
    echo "no \$CLASSPATH specified. try:" >&2
    echo " CLASSPATH=/path/to/bukkit.jar $0" >&2
    exit 1
}

rm -rvf build
cp -rv src build

# remove vim swap files
find build -name '.*.sw?' -delete

pushd build/
    pushd io/github/ausbin/mccmd
        javac -Xlint:all *.java || exit 1
        rm *.java
    popd

    zip -r ../mccmd.jar ../../LICENSE * 
popd

