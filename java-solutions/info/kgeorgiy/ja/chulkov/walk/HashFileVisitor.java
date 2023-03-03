package info.kgeorgiy.ja.chulkov.walk;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class HashFileVisitor<T extends Path> extends SimpleFileVisitor<T> {
    private final HashResultsHandler resultsHandler;
    private final byte[] buffer = new byte[1024];
    private final MessageDigest messageDigest;

    public HashFileVisitor(final HashResultsHandler resultsHandler, final MessageDigest messageDigest) {
        this.resultsHandler = resultsHandler;
        this.messageDigest = messageDigest;
    }

    @Override
    @SuppressWarnings({"StatementWithEmptyBody"})
    public FileVisitResult visitFile(final T path, final BasicFileAttributes attrs) throws IOException {
        // :NOTE: reuse
        try (
                final InputStream is = new BufferedInputStream(Files.newInputStream(path));
                final DigestInputStream dis = new DigestInputStream(is, messageDigest)
        ) {
            while (dis.read(buffer) != -1) ;
        } catch (final IOException | SecurityException e) {
            System.err.println("Error on reading file " + path + " : " + e.getMessage());
            // :NOTE: digest()?
            resultsHandler.processError(path.toString(), messageDigest.digest());
            return FileVisitResult.CONTINUE;
        }
        resultsHandler.processSuccess(path, messageDigest.digest());
        return FileVisitResult.CONTINUE;
    }


    @Override
    public FileVisitResult visitFileFailed(final T path, final IOException exc) throws IOException {
        resultsHandler.processError(path.toString());
        return FileVisitResult.CONTINUE;
    }
}
