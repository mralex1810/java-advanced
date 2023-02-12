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
import java.security.NoSuchAlgorithmException;

public class HashFileVisitor<T extends Path> extends SimpleFileVisitor<T> {
    private static final byte[] EMPTY_HASH = new byte[32];

    private final CheckedIOExceptionConsumer<HashFileInfo> resultsConsumer;

    public HashFileVisitor(CheckedIOExceptionConsumer<HashFileInfo> resultsConsumer) {
        this.resultsConsumer = resultsConsumer;
    }

    public void processError(String path) {
        processResult(new HashFileInfo(EMPTY_HASH, path));
    }

    private void processResult(HashFileInfo info) {
        try {
            resultsConsumer.accept(info);
        } catch (IOException e) {
            System.err.println("Error on writing in output file");
        }
    }

    @Override
    public FileVisitResult visitFile(T path, BasicFileAttributes attrs) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(path);
                 DigestInputStream dis = new DigestInputStream(new BufferedInputStream(is), messageDigest)) {
                while (dis.read() != -1) ;
                processResult(new HashFileInfo(dis.getMessageDigest().digest(), path.toString()));
            } catch (IOException e) {
                System.err.println("Error on reading file " + path);
                processError(path.toString());
            }
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SDK hasn't support of SHA-256");
            processError(path.toString());
        }
        return FileVisitResult.CONTINUE;
    }


    @Override
    public FileVisitResult visitFileFailed(T path, IOException exc) {
        processError(path.toString());
        return FileVisitResult.CONTINUE;
    }
}
