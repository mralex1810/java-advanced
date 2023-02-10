package info.kgeorgiy.ja.chulkov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;

public class RecursiveWalk {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Programs needs two arguments for work: input and output files");
            return;
//            System.exit(1);
        }
        var visitor = new HashFileVisitor<>();
        try (BufferedReader in = new BufferedReader(new FileReader(args[0], StandardCharsets.UTF_8))) {
            in.lines().forEach(file -> {
                try {
                    Files.walkFileTree(Path.of(file), visitor);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(args[1], StandardCharsets.UTF_8)))) {
            for (var info : visitor.getResults()) {
                out.print(HexFormat.of().formatHex(info.hash()) + " " + info.file());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
