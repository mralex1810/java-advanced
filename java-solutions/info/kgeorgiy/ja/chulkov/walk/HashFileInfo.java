package info.kgeorgiy.ja.chulkov.walk;

import java.nio.file.Path;

public record HashFileInfo(byte[] hash, Path file) {
}
