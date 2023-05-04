package info.kgeorgiy.ja.chulkov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class HelloUDPServer implements HelloServer {

    public static final int LENGTH_LIMIT = 1024;
    private final Function<DatagramPacket, Supplier<byte[]>> taskGenerator = (it) -> () ->
            String.format("Hello, %s", HelloUDPClient.getDecodedData(it)).getBytes(StandardCharsets.UTF_8);
    private final Semaphore semaphore = new Semaphore(1024);
    private Thread getterThread;
    private ExecutorService taskExecutorService;
    private DatagramSocket datagramSocket;

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
                    final var datagramPacketForReceive = new DatagramPacket(new byte[LENGTH_LIMIT], LENGTH_LIMIT);
                    datagramSocket.receive(datagramPacketForReceive);
                    semaphore.acquire();
                    CompletableFuture
                            .supplyAsync(taskGenerator.apply(datagramPacketForReceive), taskExecutorService)
                            .thenAccept((ans) -> {
                                try {
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
                                } finally {
                                    semaphore.release();
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
        getterThread.interrupt();
        taskExecutorService.shutdownNow();
        datagramSocket.close();
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
