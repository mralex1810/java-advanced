package info.kgeorgiy.ja.chulkov.walk;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
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
import java.util.ArrayList;
import java.util.List;

public class HashFileVisitor<T extends Path> extends SimpleFileVisitor<T> {
    private final List<HashFileInfo> results = new ArrayList<>();
    private static final byte[] EMPTY_HASH = new byte[256];

    private void addErrorPathToResult(T path) {
        results.add(new HashFileInfo(EMPTY_HASH, path));
    }

    @Override
    public FileVisitResult visitFile(T path, BasicFileAttributes attrs) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(path);
                 DigestInputStream dis = new DigestInputStream(new BufferedInputStream(is), messageDigest)) {
                while (dis.read() != -1) ;
                results.add(new HashFileInfo(dis.getMessageDigest().digest(), path));
            } catch (IOException e) {
                System.err.println("Error on reading file " + path);
                addErrorPathToResult(path);
            }
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SDK hasn't support of SHA-256");
            throw new RuntimeException(e);
        }
        return FileVisitResult.CONTINUE;
    }


    @Override
    public FileVisitResult visitFileFailed(T path, IOException exc) {
        addErrorPathToResult(path);
        return FileVisitResult.CONTINUE;
    }

    public List<HashFileInfo> getResults() {
        return results;
    }
}
