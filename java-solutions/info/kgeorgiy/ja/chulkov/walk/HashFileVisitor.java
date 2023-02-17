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

    public HashFileVisitor(HashResultsHandler resultsHandler, MessageDigest messageDigest) {
        this.resultsHandler = resultsHandler;
        this.messageDigest = messageDigest;
    }


    @Override
    public FileVisitResult visitFile(T path, BasicFileAttributes attrs) {
        // :NOTE: reuse
        try (
                InputStream is = new BufferedInputStream(Files.newInputStream(path));
                DigestInputStream dis = new DigestInputStream(is, messageDigest)
        ) {
            while (dis.read(buffer) != -1) ;
            resultsHandler.processSuccess(dis.getMessageDigest().digest(), path);
        } catch (IOException e) {
            System.err.println("Error on reading file " + path + " : " + e.getMessage());
            resultsHandler.processError(path.toString());
        } catch (SecurityException e) {
            System.err.println("Error on access to file " + path + " : " + e.getMessage());
            resultsHandler.processError(path.toString());
        }
        return FileVisitResult.CONTINUE;
    }


    @Override
    public FileVisitResult visitFileFailed(T path, IOException exc) {
        resultsHandler.processError(path.toString());
        return FileVisitResult.CONTINUE;
    }
}
