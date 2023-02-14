package info.kgeorgiy.ja.chulkov.walk;


import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.HexFormat;

public class HashResultsHandler implements Closeable {
    private final Writer writer;
    private static final String ERROR_HASH = "0".repeat(64);

    public HashResultsHandler(Writer writer) {
        this.writer = writer;
    }

    public void processSuccess(byte[] hash, Path file) {
        processResult(HexFormat.of().formatHex(hash), file.toString());
    }

    public void processError(String path) {
        processResult(ERROR_HASH, path);
    }

    private void processResult(String hexHash, String path) {
        try {
            writer.write(hexHash);
            writer.write(" ");
            writer.write(path);
            writer.write(System.lineSeparator());
        } catch (IOException e) {
            System.err.println("Error on writing in output file");
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
