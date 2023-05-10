path=$(cd ../..; pwd)
lib=$path/java-advanced-2023/lib
modules=$path/java-advanced-2023/modules
modele_pref=$modules/info.kgeorgiy.java.advanced.
base="$modele_pref"base
student="$modele_pref"student
implementor="$modele_pref"implementor
concurrent="$modele_pref"concurrent
mapper="$modele_pref"mapper
crawler="$modele_pref"crawler
hello="$modele_pref"hello

javadoc -private -author -d "$path/java-advanced/javadoc" \
  "$path"/java-advanced/java-solutions/info/kgeorgiy/ja/chulkov/*/*.java\
  "$path"/java-advanced/java-solutions/info/kgeorgiy/ja/chulkov/*/*/*.java\
  -cp "$lib/*:$path/java-solutions:$base:$student:$implementor:$concurrent:$mapper:$crawler:$hello"
