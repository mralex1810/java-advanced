package info.kgeorgiy.ja.chulkov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.function.Supplier;


abstract class AbstractHelloUDPServer implements HelloServer {

    public static final int MAX_TASKS = 1024;

    public final Function<ByteBuffer, Supplier<ByteBuffer>> taskGenerator = (it) -> () ->
            ByteBuffer.wrap(
                    String.format("Hello, %s", HelloUDPClient.getDecodedData(it))
                            .getBytes(StandardCharsets.UTF_8)
            );
    protected final Semaphore semaphore = new Semaphore(MAX_TASKS);
    protected ExecutorService taskExecutorService;
    private Thread getterThread;
    private State state = State.NOT_STARTED;

    private static void printUsage() {
        System.err.println("""
                    Usage: HelloUDPServer port threads
                    port -- port number on which requests will be received
                    threads -- number of worker threads that will process requests
                """);
    }

    @Override
    public void start(final int port, final int threads) {
        if (state != State.NOT_STARTED) {
            throw new IllegalStateException("Server is started already");
        }
        state = State.RUNNING;
        taskExecutorService = Executors.newFixedThreadPool(threads);
        prepare(port);
        getterThread = new Thread(() -> {
            while (state == State.RUNNING) {
                try {
                    getterIteration();
                } catch (final IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        getterThread.start();
    }

    protected abstract void getterIteration() throws IOException, InterruptedException;

    protected abstract void prepare(int port);

    @Override
    public void close() {
        if (state != State.RUNNING) {
            throw new IllegalStateException("Server isn't running");
        }
        state = State.CLOSED;
        getterThread.interrupt();
        taskExecutorService.close();
    }

    private enum State {
        NOT_STARTED,
        RUNNING,
        CLOSED
    }
}
