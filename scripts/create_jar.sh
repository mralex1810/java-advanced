#!/usr/bin/env sh
path=$(cd ..; pwd)
modules=$path/java-advanced-2023/modules
implementor=info.kgeorgiy.java.advanced.implementor
javac -d "$(pwd)" \
  $path/java-solutions/info/kgeorgiy/ja/chulkov/implementor/Implementor.java\
  -cp "$modules/$implementor:$path/java-solutions"
jar cfe Implementor.jar info.kgeorgiy.ja.chulkov.implementor.Implementor info
rm -r info