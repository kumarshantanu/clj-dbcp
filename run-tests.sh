#!/usr/bin/env bash

# You may need to uncomment/edit the following line on Mac OS X
#JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.7.0_06.jdk/Contents/Home/

function each_test {
  ./clean.sh && lein2 clean
  lein2 with-profile dev,$1 test
  lein2 clean && ./clean.sh
}

if [ -z "$1" ]; then
  each_test 1.4
elif [ "$1" == "all" ]; then
  for a in 1.2 1.3 1.4 1.5; do
    each_test $a
  done
else
  for a in $1 $2 $3 $4 $5 $6; do
    each_test $a
  done
fi

