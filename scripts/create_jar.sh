#!/usr/bin/env sh
path=$(cd ../..; pwd)
modules=$path/java-advanced-2023/modules
implementor=info.kgeorgiy.java.advanced.implementor
javac -d "$(pwd)" \
  $path/java-advanced/java-solutions/info/kgeorgiy/ja/chulkov/implementor/Implementor.java\
  -cp "$modules/$implementor:$path/java-advanced/java-solutions"
jar cfem Implementor.jar info.kgeorgiy.ja.chulkov.implementor.Implementor MANIFEST.MF info
rm -r info