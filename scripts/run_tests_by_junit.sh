#!/usr/bin/env sh
path=$(cd ..; pwd)

javac -d "$(pwd)" \
  "$path"/java-solutions/info/kgeorgiy/ja/chulkov/bank/BankTests.java\
  -cp "$path/java-solutions:$path/lib/*"
java -cp ".:$path/lib/*" org.junit.runner.JUnitCore info.kgeorgiy.ja.chulkov.bank.BankTests
exitcode=$?
rm -r info
exit $exitcode