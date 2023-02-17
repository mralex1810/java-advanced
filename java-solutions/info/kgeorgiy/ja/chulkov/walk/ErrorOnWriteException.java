package info.kgeorgiy.ja.chulkov.walk;

public class ErrorOnWriteException extends RuntimeException {
    public ErrorOnWriteException(Throwable cause) {
        super(cause);
    }
}
