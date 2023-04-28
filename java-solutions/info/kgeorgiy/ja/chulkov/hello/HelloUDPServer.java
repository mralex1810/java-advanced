package info.kgeorgiy.ja.chulkov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

public class HelloUDPServer implements HelloServer {

    public static final int LENGTH_LIMIT = 1024;
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
                    final var ans = String.format("Hello, %s", HelloUDPClient.getDecodedData(datagramPacketForReceive))
                            .getBytes(StandardCharsets.UTF_8);
                    final var datagramPacketToSend = new DatagramPacket(
                            ans,
                            ans.length,
                            datagramPacketForReceive.getAddress(),
                            datagramPacketForReceive.getPort());
                    datagramSocket.send(datagramPacketToSend);
                } catch (final IOException e) {
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
