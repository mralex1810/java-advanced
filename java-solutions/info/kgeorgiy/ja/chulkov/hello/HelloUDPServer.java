package info.kgeorgiy.ja.chulkov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class HelloUDPServer implements HelloServer {

    public static final int LENGTH_LIMIT = 1024;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Function<DatagramPacket, Supplier<byte[]>> taskGenerator = (it) -> () ->
            String.format("Hello, %s", HelloUDPClient.getDecodedData(it)).getBytes(StandardCharsets.UTF_8);
    private DatagramSocket datagramSocket;
    private List<Thread> threadsList;

    @Override
    public void start(final int port, final int threads) {
        try {
            datagramSocket = new DatagramSocket(port);
        } catch (final SocketException e) {
            throw new RuntimeException(e);
        }
        final Runnable task = () -> {
            while (!Thread.interrupted()) {
                try {
                    final var datagramPacketForReceive = new DatagramPacket(new byte[LENGTH_LIMIT], LENGTH_LIMIT);
                    datagramSocket.receive(datagramPacketForReceive);
                    CompletableFuture.supplyAsync(taskGenerator.apply(datagramPacketForReceive))
                            .thenAcceptAsync((ans) -> {
                                        final var datagramPacketToSend = new DatagramPacket(
                                                ans,
                                                ans.length,
                                                datagramPacketForReceive.getAddress(),
                                                datagramPacketForReceive.getPort());
                                        try {
                                            datagramSocket.send(datagramPacketToSend);
                                        } catch (final IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                            ).get();
                } catch (final IOException | InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        threadsList = IntStream.range(0, threads)
                .mapToObj(it -> new Thread(task))
                .peek(Thread::start)
                .toList();
    }

    @Override
    public void close() {
        threadsList.forEach(Thread::interrupt);
        datagramSocket.close();
        threadsList.forEach(it -> {
            while (true) {
                try {
                    it.join();
                    break;
                } catch (final InterruptedException ignored) {
                }
            }
        });
    }
}
