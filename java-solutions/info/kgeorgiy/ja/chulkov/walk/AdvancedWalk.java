package info.kgeorgiy.ja.chulkov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
                if (outputFile.getParent() == null) {
                    System.err.println("Can't create parent dirs of output files");
                    return;
                }
                if (!Files.exists(outputFile.getParent())) {
                    Files.createDirectories(outputFile.getParent());
                }
            } catch (IOException e) {
                System.err.println("Can't create parent dirs of output files");
                return;
            }
            try (BufferedReader in = new BufferedReader(new FileReader(args[0], StandardCharsets.UTF_8))) {
                try (Writer out = new BufferedWriter(new FileWriter(outputFile.toFile(), StandardCharsets.UTF_8));
                     HashResultsHandler handler = new HashResultsHandler(out)) {
                    FileVisitor<Path> visitor = new HashFileVisitor<>(handler);
                    for (String file = in.readLine(); file != null; file = in.readLine()) {
                        walkFromOneFile(file, depth, visitor, handler);
                    }
                } catch (IOException e) {
                    System.err.println("Error on open output file");
                }
            } catch (IOException e) {
                System.err.println("Error on open input file");
            }
        } catch (InvalidPathException e) {
            System.err.println("Output file string isn't path");
        }
    }

    private static void walkFromOneFile(String file, int depth, FileVisitor<Path> visitor, HashResultsHandler
            handler) {
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

    }
}
