package info.kgeorgiy.ja.chulkov.hello;

import static info.kgeorgiy.ja.chulkov.utils.ArgumentsUtils.parseNonNegativeInt;

import info.kgeorgiy.ja.chulkov.utils.UDPUtils;
import info.kgeorgiy.java.advanced.hello.HelloServer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    /**
     * A function that generates tasks based on a ByteBuffer input.
     */
    public final Function<ByteBuffer, Supplier<ByteBuffer>> taskGenerator = (it) -> () ->
            ByteBuffer.wrap(
                        String.format("Hello, %s", UDPUtils.getDecodedData(it))
                                .getBytes(StandardCharsets.UTF_8)
                );

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

    /**
     * Prints the usage information for the HelloServer class.
     */
    private static void printUsage() {
        System.err.println("""
                Usage: HelloServer port threads
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
        taskExecutorService = Executors.newFixedThreadPool(threads);
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
     * @param threads
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

    public abstract static class ServerMainHelper {

        public void mainHelp(final String[] args) {
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
                    final var server = getHelloServer();
                    server.start(port, threads);
                } catch (final RuntimeException e) {
                    System.err.println(e.getMessage());
                }
            } catch (final RuntimeException e) {
                System.err.println(e.getMessage());
                printUsage();
            }
        }

        protected abstract HelloServer getHelloServer();

        public void printUsage() {
            System.err.println("""
                        Usage: HelloUDPServer port threads
                        port -- port number on which requests will be received
                        threads -- number of worker threads that will process requests
                    """);
        }
    }
}
