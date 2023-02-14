package info.kgeorgiy.ja.chulkov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.EnumSet;

public class RecursiveWalk {

    public static void main(String[] args) {
        AdvancedWalk.recursiveWalk(args);
    }
}
