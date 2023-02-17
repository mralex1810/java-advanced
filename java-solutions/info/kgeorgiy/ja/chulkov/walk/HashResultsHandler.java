package info.kgeorgiy.ja.chulkov.walk;


import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.HexFormat;

public class HashResultsHandler implements Closeable {
    private static final String ERROR_HASH_HEX = "0".repeat(64);
    private final Writer writer;

    public HashResultsHandler(Writer writer) {
        this.writer = writer;
    }

    public void processSuccess(byte[] hash, Path file) {
        processResult(HexFormat.of().formatHex(hash), file.toString());
    }

    public void processError(String path) {
        processResult(ERROR_HASH_HEX, path);
    }

    private void processResult(String hexHash, String path) {
        try {
            writer.write(hexHash + " " + path + System.lineSeparator());
        } catch (final IOException e) {
            // :NOTE: ??
            throw new ErrorOnWriteException(e);
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}