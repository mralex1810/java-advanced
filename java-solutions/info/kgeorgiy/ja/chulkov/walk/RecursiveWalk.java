package info.kgeorgiy.ja.chulkov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HexFormat;

public class RecursiveWalk {

    public static void main(String[] args) {
        if (args == null || args.length < 2 || args[0] == null || args[1] == null) {
            System.err.println("Programs needs two arguments for work: nonnull input and output files");
            return;
//            System.exit(1);
        }
        try (BufferedReader in = new BufferedReader(new FileReader(args[0], StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(args[1], StandardCharsets.UTF_8)) {
            var visitor = new HashFileVisitor<>(info ->
                    out.println(HexFormat.of().formatHex(info.hash()) + " " + info.file()));
            in.lines().forEach(file -> {
                try {
                    Path path = Path.of(file);
                    Files.walkFileTree(path, visitor);
                } catch (IOException e) {
                    // Unreachable
                    System.err.println("Error on processing file " + file);
                } catch (InvalidPathException e) {
                    System.err.println("Error on getting path of " + file);
                    visitor.processError(file);
                }
            });
        } catch (IOException e) {
            System.err.println("Error on reading input file");
        }
    }
}
