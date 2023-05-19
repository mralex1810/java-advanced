package info.kgeorgiy.ja.chulkov.hello;


import static info.kgeorgiy.ja.chulkov.utils.UDPUtils.dataToByteBuffer;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.CompletableFuture;


/**
 * Implementation of {@link HelloServer} with main method and blocking operations
 */
public class HelloUDPServer extends AbstractHelloUDPServer {

    private DatagramSocket datagramSocket;

    /**
     * Method to run {@link HelloUDPServer} from CLI
     *
     * @param args array of string {port, threads}
     */
    public static void main(final String[] args) {
        new BlockingServerMainHelper().mainHelp(args);
    }

    private static class BlockingServerMainHelper extends ServerMainHelper {

        @Override
        protected HelloServer getHelloServer() {
            return new HelloUDPServer();
        }
    }

    @Override
    protected void getterIteration() throws IOException, InterruptedException {
        final var datagramPacketForReceive = new DatagramPacket(
                new byte[datagramSocket.getReceiveBufferSize()],
                datagramSocket.getReceiveBufferSize()
        );
        datagramSocket.receive(datagramPacketForReceive);
        semaphore.acquire();
        CompletableFuture
                .supplyAsync(taskGenerator.apply(dataToByteBuffer(datagramPacketForReceive)),
                        taskExecutorService)
                .thenAccept((ans) -> {
                    final var datagramPacketToSend = new DatagramPacket(
                            ans.array(),
                            ans.limit(),
                            datagramPacketForReceive.getSocketAddress());
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
                    return null;
                });
    }

    @Override
    protected void prepare(final int port) {
        try {
            datagramSocket = new DatagramSocket(port);
        } catch (final SocketException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        datagramSocket.close();
        super.close();
    }
}
