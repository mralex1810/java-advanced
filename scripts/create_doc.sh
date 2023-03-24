path=$(cd ../..; pwd)
lib=$path/java-advanced-2023/lib
modules=$path/java-advanced-2023/modules
implementor=info.kgeorgiy.java.advanced.implementor
base=info.kgeorgiy.java.advanced.base

javadoc -private -author -d "$path/java-solutions/javadoc" \
  "$path"/java-advanced/java-solutions/info/kgeorgiy/ja/chulkov/implementor/*.java\
  "$modules"/$implementor/info/kgeorgiy/java/advanced/implementor/Impler.java\
  "$modules"/$implementor/info/kgeorgiy/java/advanced/implementor/JarImpler.java\
  "$modules"/$implementor/info/kgeorgiy/java/advanced/implementor/ImplerException.java\
  -cp "$lib/junit-4.11.jar:$modules/$implementor:$modules/$base:$path/java-solutions"
