#!/bin/sh
lein uberjar && cat stub.sh target/jsetags-*-standalone.jar > jsetags && chmod +x jsetags
echo "Packaged: `pwd`/jsetags"
