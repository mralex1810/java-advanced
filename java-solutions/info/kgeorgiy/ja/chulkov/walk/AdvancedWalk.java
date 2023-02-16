package info.kgeorgiy.ja.chulkov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.EnumSet;

public class AdvancedWalk {

    public static void recursiveWalk(String[] args) {
        walk(args, Integer.MAX_VALUE);
    }

    public static void nonRecursiveWalk(String[] args) {
        walk(args, 0);
    }

    private static void walk(String[] args, int depth) {
        if (args == null || args.length < 2 || args[0] == null || args[1] == null) {
            System.err.println("Programs needs two arguments for work: nonnull input and output files");
            return;
        }
        try (BufferedReader in = new BufferedReader(new FileReader(args[0], StandardCharsets.UTF_8));
             Writer out = new BufferedWriter(new FileWriter(args[1], StandardCharsets.UTF_8));
             HashResultsHandler handler = new HashResultsHandler(out)) {
            var visitor = new HashFileVisitor<>(handler);
            in.lines().forEach(file -> {
                try {
                    Files.walkFileTree(Path.of(file), EnumSet.noneOf(FileVisitOption.class), depth, visitor);
                } catch (IOException e) {
                    // Unreachable
                    System.err.println("Error on processing file " + file);
                } catch (InvalidPathException e) {
                    System.err.println("Error on getting path of " + file);
                    handler.processError(file);
                } catch (SecurityException e) {
                    System.err.println("Security manager denies access to " + file);
                    handler.processError(file);
                }
            });
        } catch (IOException e) {
            System.err.println("Error on reading input file");
        }
    }
}
