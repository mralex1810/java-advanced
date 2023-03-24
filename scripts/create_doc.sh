path=$(cd ../..; pwd)
lib=$path/java-advanced-2023/lib
modules=$path/java-advanced-2023/modules
implementor=info.kgeorgiy.java.advanced.implementor
implementor_dir="$modules"/$implementor/info/kgeorgiy/java/advanced/implementor
base=info.kgeorgiy.java.advanced.base

javadoc -private -author -d "$path/java-advanced/javadoc" \
  "$path"/java-advanced/java-solutions/info/kgeorgiy/ja/chulkov/implementor/*.java\
  "$implementor_dir"/Impler.java\
  "$implementor_dir"/JarImpler.java\
  "$implementor_dir"/ImplerException.java\
  -cp "$lib/junit-4.11.jar:$modules/$implementor:$modules/$base:$path/java-solutions"
