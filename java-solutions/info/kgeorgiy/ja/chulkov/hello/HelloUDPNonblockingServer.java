package info.kgeorgiy.ja.chulkov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link HelloServer} with main method and nonblocking operations
 */
public class HelloUDPNonblockingServer extends AbstractHelloUDPServer {

    public static final int BUFFER_SIZE = 2048;
    private Selector selector;
    private Queue<Packet> toSend;
    private Queue<Packet> toReceive;

    /**
     * Method to run {@link HelloUDPNonblockingServer} from CLI
     *
     * @param args array of string {port, threads}
     */
    public static void main(final String[] args) {
        new NonblockingServerMainHelper().mainHelp(args);
    }

    @Override
    protected void getterIteration() throws IOException {
        selector.select();
        for (final var key : selector.selectedKeys()) {
            try {
                final DatagramChannel channel = (DatagramChannel) key.channel();
                if (key.isWritable()) {
                    doWriteOperation(toSend, key, channel);
                } else if (key.isReadable()) {
                    doReadOperation(toSend, key, channel);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        selector.selectedKeys().clear();
    }

    @Override
    protected void prepare(final int port, final int threads) {
        try {
            selector = Selector.open();
            final var datagramChannel = DatagramChannel.open();
            datagramChannel.configureBlocking(false);
            datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            datagramChannel.bind(new InetSocketAddress(port));
            datagramChannel.register(selector, SelectionKey.OP_READ);
            toSend = new ArrayBlockingQueue<>(threads);
            toReceive = new ArrayBlockingQueue<>(threads);
            for (int i = 0; i < threads; i++) {
                toReceive.add(new Packet(BUFFER_SIZE));
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    private void doWriteOperation(final Queue<Packet> toSend, final SelectionKey key,
            final DatagramChannel channel) {
        final var answer = toSend.remove();
        if (toSend.isEmpty()) {
            key.interestOpsAnd(~SelectionKey.OP_WRITE);
            selector.wakeup();
        }
        try {
            channel.send(answer.getBuffer(), answer.getAddress());
        } catch (final IOException e) {
            e.printStackTrace();
        }
        toReceive.add(answer);
        key.interestOpsOr(SelectionKey.OP_READ);
        selector.wakeup();
    }

    private void doReadOperation(final Queue<Packet> toSend, final SelectionKey key,
            final DatagramChannel channel) throws IOException {
        if (toReceive.isEmpty()) {
            key.interestOpsAnd(~SelectionKey.OP_READ);
            selector.wakeup();
            return;
        }
        final var packet = toReceive.remove();
        packet.getBuffer().clear();
        final var sender = channel.receive(packet.getBuffer());
        packet.getBuffer().flip();

        CompletableFuture
                .supplyAsync(taskGenerator.apply(packet.getBuffer()), taskExecutorService)
                .thenAccept(reqAnswer ->  {
                    packet.getBuffer().clear();
                    packet.getBuffer().put(reqAnswer.array());
                    packet.getBuffer().flip();
                    packet.setAddress(sender);
                })
                .thenRun(() -> {
                    toSend.add(packet);
                    key.interestOpsOr(SelectionKey.OP_WRITE);
                    selector.wakeup();
                })
                .exceptionally((e) -> {
                    System.err.println("Error on executing task " + e.getMessage());
                    return null;
                });
    }

    @Override
    public void close() {
        try {
            selector.close();
        } catch (final IOException ignored) {
        }
        super.close();
    }

    private static class NonblockingServerMainHelper extends ServerMainHelper {

        @Override
        protected HelloServer getHelloServer() {
            return new HelloUDPNonblockingServer();
        }
    }

    private static class Packet {
        private final ByteBuffer buffer;
        private SocketAddress address;

        public Packet(final int bufferSize) {
            this(bufferSize, null);
        }

        public Packet(final int bufferSize, final SocketAddress address) {
            buffer = ByteBuffer.allocate(bufferSize);
            this.address = address;
        }

        public ByteBuffer getBuffer() {
            return buffer;
        }

        public SocketAddress getAddress() {
            return address;
        }

        public void setAddress(final SocketAddress inetAddress) {
            this.address = inetAddress;
        }
    }
}
