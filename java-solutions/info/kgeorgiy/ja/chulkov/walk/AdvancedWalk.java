package info.kgeorgiy.ja.chulkov.walk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.*;
import java.util.EnumSet;

public class AdvancedWalk {

    public static void recursiveWalk(final String[] args) {
        walk(args, Integer.MAX_VALUE);
    }

    public static void nonRecursiveWalk(final String[] args) {
        walk(args, 0);
    }

    private static void walk(final String[] args, final int depth) {
        if (args == null) {
            System.err.println("Args must be non null");
            return;
        }
        if (args.length < 2) {
            System.err.println("Program needs two arguments for work: input and output files");
            return;
        }
        // :NOTE: copy-paste
        if (args[0] == null) {
            System.err.println("Input file must be non null");
            return;
        }
        if (args[1] == null) {
            System.err.println("Output file must be non null");
            return;
        }
        try {
            final Path outputFile = Path.of(args[1]);
            try {
                // :NOTE: outputFile.getParent()
                if (outputFile.getParent() != null && !Files.exists(outputFile.getParent())) {
                    Files.createDirectories(outputFile.getParent());
                }
            } catch (final IOException e) {
                System.err.println("Can't create parent dirs of output files " + e.getMessage());
                // :NOTE: ??
                return;
            }

            try (final BufferedReader in = Files.newBufferedReader(Path.of(args[0]))) {
                try (final Writer out = Files.newBufferedWriter(outputFile);
                     // :NOTE: double close
                     final HashResultsHandler handler = new HashResultsHandler(out)) {
                    try {
                        final FileVisitor<Path> visitor = new HashFileVisitor<>(handler);
                        for (String file = in.readLine(); file != null; file = in.readLine()) {
                            walkFromOneFile(file, depth, visitor, handler);
                        }
                    } catch (final IOException e) {
                        System.err.println("Error on reading input file " + e.getMessage());
                    }
                } catch (final IOException e) {
                    System.err.println("Error on open output file " + e.getMessage());
                }
            } catch (final IOException e) {
                System.err.println("Error on open input file " + e.getMessage());
            } catch (final InvalidPathException e) {
                System.err.println("Input file string isn't path " + e.getReason());
            }
        } catch (final InvalidPathException e) {
            System.err.println("Output file string isn't path: " + e.getReason());
        }
    }


    private static void walkFromOneFile(final String file, final int depth, final FileVisitor<Path> visitor, final HashResultsHandler handler) {
        try {
            Files.walkFileTree(Path.of(file), EnumSet.noneOf(FileVisitOption.class), depth, visitor);
        } catch (final IOException e) {
            // Unreachable
            System.err.println("Error on processing file " + file + " : " + e.getMessage());
        } catch (final InvalidPathException e) {
            System.err.println("Error on getting path of " + file  + " : " + e.getReason());
            handler.processError(file);
        } catch (final SecurityException e) {
            System.err.println("Security manager denies access to " + file  + " : " + e.getMessage());
            handler.processError(file);
        }

    }
}
