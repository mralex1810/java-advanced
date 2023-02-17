package info.kgeorgiy.ja.chulkov.walk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.*;
import java.util.EnumSet;

public class AdvancedWalk {

    public static void recursiveWalk(String[] args) {
        walk(args, Integer.MAX_VALUE);
    }

    public static void nonRecursiveWalk(String[] args) {
        walk(args, 0);
    }

    private static void walk(String[] args, int depth) {
        if (args == null) {
            System.err.println("Args must be non null");
            return;
        }
        if (args.length < 2) {
            System.err.println("Program needs two arguments for work: input and output files");
            return;
        }
        if (args[0] == null) {
            System.err.println("Input file must be non null");
            return;
        }
        if (args[1] == null) {
            System.err.println("Output file must be non null");
            return;
        }
        try {
            Path outputFile = Path.of(args[1]);
            try {
                if (outputFile.getParent() != null && !Files.exists(outputFile.getParent())) {
                    Files.createDirectories(outputFile.getParent());
                }
            } catch (IOException e) {
                System.err.println("Can't create parent dirs of output files " + e.getMessage());
                return;
            }
            try (BufferedReader in = Files.newBufferedReader(Path.of(args[0]))) {
                try (Writer out = Files.newBufferedWriter(outputFile);
                     HashResultsHandler handler = new HashResultsHandler(out)) {
                    try {
                        FileVisitor<Path> visitor = new HashFileVisitor<>(handler);
                        for (String file = in.readLine(); file != null; file = in.readLine()) {
                            walkFromOneFile(file, depth, visitor, handler);
                        }
                    } catch (IOException e) {
                        System.err.println("Error on reading input file " + e.getMessage());
                    }
                } catch (IOException e) {
                    System.err.println("Error on open output file " + e.getMessage());
                }
            } catch (IOException e) {
                System.err.println("Error on open input file " + e.getMessage());
            } catch (InvalidPathException e) {
                System.err.println("Input file string isn't path " + e.getReason());
            }
        } catch (InvalidPathException e) {
            System.err.println("Output file string isn't path: " + e.getReason());
        }
    }


    private static void walkFromOneFile(String file, int depth, FileVisitor<Path> visitor, HashResultsHandler handler) {
        try {
            Files.walkFileTree(Path.of(file), EnumSet.noneOf(FileVisitOption.class), depth, visitor);
        } catch (IOException e) {
            // Unreachable
            System.err.println("Error on processing file " + file + " : " + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Error on getting path of " + file  + " : " + e.getReason());
            handler.processError(file);
        } catch (SecurityException e) {
            System.err.println("Security manager denies access to " + file  + " : " + e.getMessage());
            handler.processError(file);
        }

    }
}
