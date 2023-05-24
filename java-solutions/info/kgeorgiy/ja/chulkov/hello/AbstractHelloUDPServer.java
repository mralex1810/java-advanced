package info.kgeorgiy.ja.chulkov.hello;

import static info.kgeorgiy.ja.chulkov.utils.ArgumentsUtils.parseNonNegativeInt;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * An abstract implementation of the HelloServer interface that provides common functionality
 * <p>
 * for UDP servers.
 */
abstract class AbstractHelloUDPServer implements HelloServer {

    /**
     * The maximum number of tasks allowed in the server.
     */
    public static final int MAX_TASKS = 128;

    private static final byte[] helloBytes = "Hello, ".getBytes(StandardCharsets.UTF_8);

    /**
     * A function that generates tasks based on a ByteBuffer input.
     */
    public static final Function<ByteBuffer, Runnable> taskGenerator = (buffer) -> () -> {
        buffer.limit(buffer.position() + helloBytes.length);
        System.arraycopy(buffer.array(), 0, buffer.array(), helloBytes.length, buffer.limit() - helloBytes.length);
        System.arraycopy(helloBytes, 0, buffer.array(), 0, helloBytes.length);
        buffer.rewind();
    };


    /**
     * The executor service used to execute tasks.
     */
    protected ExecutorService taskExecutorService;

    /**
     * The getter thread responsible for receiving requests.
     */
    private Thread getterThread;

    /**
     * The state of the server.
     */
    private State state = State.NOT_STARTED;

    static void mainHelp(final String[] args, final Supplier<HelloServer> helloServerSupplier) {
        Objects.requireNonNull(args);
        Arrays.stream(args).forEach(Objects::requireNonNull);
        if (args.length != 2) {
            printUsage();
            return;
        }
        try {
            final int port = parseNonNegativeInt(args[0], "port");
            final int threads = parseNonNegativeInt(args[1], "threads");
            try {
                helloServerSupplier.get().start(port, threads);
            } catch (final RuntimeException e) {
                System.err.println(e.getMessage());
            }
        } catch (final RuntimeException e) {
            System.err.println(e.getMessage());
            printUsage();
        }
    }

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
            throw new IllegalStateException("Server is already started");
        }
        state = State.RUNNING;
        taskExecutorService = new ThreadPoolExecutor(threads, threads,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(threads),
                new ThreadPoolExecutor.CallerRunsPolicy());
        prepare(port, threads);
        getterThread = new Thread(() -> {
            while (state == State.RUNNING && !Thread.interrupted()) {
                try {
                    getterIteration();
                } catch (final IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        getterThread.start();
    }

    /**
     * Performs a single iteration of the getter thread.
     *
     * @throws IOException          If an I/O error occurs during the iteration.
     * @throws InterruptedException If the getter thread is interrupted during the iteration.
     */
    protected abstract void getterIteration() throws IOException, InterruptedException;

    /**
     * Prepares the server to listen on the specified port.
     *
     * @param port    The port number on which requests will be received.
     * @param threads Number of working threads
     */
    protected abstract void prepare(int port, final int threads);

    @Override
    public void close() {
        if (state != State.RUNNING) {
            throw new IllegalStateException("Server is not running");
        }
        state = State.CLOSED;
        getterThread.interrupt();
        taskExecutorService.close();
    }


    /**
     * Represents the state of the server.
     */
    protected enum State {
        NOT_STARTED,
        RUNNING,
        CLOSED
    }
}
