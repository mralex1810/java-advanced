package info.kgeorgiy.ja.chulkov.hello;

import static info.kgeorgiy.ja.chulkov.utils.ArgumentsUtils.parseNonNegativeInt;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Implementation of {@link HelloServer} with main method
 */
public class HelloUDPServer implements HelloServer {

    public static final int MAX_TASKS = 1024;
    private final Function<DatagramPacket, Supplier<byte[]>> taskGenerator = (it) -> () ->
            String.format("Hello, %s", HelloUDPClient.getDecodedData(it)).getBytes(StandardCharsets.UTF_8);
    private final Semaphore semaphore = new Semaphore(MAX_TASKS);
    private Thread getterThread;
    private ExecutorService taskExecutorService;
    private DatagramSocket datagramSocket;

    /**
     * Method to run {@link HelloUDPServer} from CLI
     * @param args array of string {port, threads}
     */
    public static void main(final String[] args) {
        Objects.requireNonNull(args);
        Arrays.stream(args).forEach(Objects::requireNonNull);
        if (args.length != 2) {
            printUsage();
            return;
        }
        try {
            final int port = parseNonNegativeInt(args[0], "port");
            final int threads = parseNonNegativeInt(args[1], "threads");
            try (final var server = new HelloUDPServer()){
                server.start(port, threads);
            } catch (final RuntimeException e) {
                System.err.println(e.getMessage());
            }
        } catch (final RuntimeException ignored) {
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
        taskExecutorService = Executors.newFixedThreadPool(threads);
        try {
            datagramSocket = new DatagramSocket(port);
        } catch (final SocketException e) {
            throw new RuntimeException(e);
        }
        getterThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    final var datagramPacketForReceive = new DatagramPacket(
                            new byte[datagramSocket.getReceiveBufferSize()],
                            datagramSocket.getReceiveBufferSize()
                    );
                    datagramSocket.receive(datagramPacketForReceive);
                    semaphore.acquire();
                    CompletableFuture
                            .supplyAsync(taskGenerator.apply(datagramPacketForReceive), taskExecutorService)
                            .thenAccept((ans) -> {
                                final var datagramPacketToSend = new DatagramPacket(
                                        ans,
                                        ans.length,
                                        datagramPacketForReceive.getAddress(),
                                        datagramPacketForReceive.getPort());
                                try {
                                    datagramSocket.send(datagramPacketToSend);
                                } catch (final IOException e) {
                                    System.err.println("Error on executing task");
                                }
                            })
                            .handle((ans, e) -> {
                                semaphore.release();
                                if (e != null) {
                                    System.err.println("Error on executing task " + e.getMessage());
                                }
                                return ans;
                            });
                } catch (final IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        getterThread.start();
    }

    @Override
    public void close() {
        datagramSocket.close();
        getterThread.interrupt();
        taskExecutorService.shutdown();
        while (true) {
            try {
                getterThread.join();
                if (taskExecutorService.awaitTermination(1, TimeUnit.DAYS)) {
                    break;
                }
            } catch (final InterruptedException ignored) {
            }
        }
    }
}
