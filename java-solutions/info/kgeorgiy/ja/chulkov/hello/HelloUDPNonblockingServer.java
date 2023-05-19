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
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Implementation of {@link HelloServer} with main method and nonblocking operations
 */
public class HelloUDPNonblockingServer extends AbstractHelloUDPServer {

    public static final Function<Throwable, Void> LOG_ERROR = (e) -> {
        System.err.println("Error on executing task " + e.getMessage());
        return null;
    };
    private Selector selector;
    private Runnable SELECTOR_WAKEUP;

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
        selector.select(20);
        for (final var key : selector.selectedKeys()) {
            try {
                final DatagramChannel channel = (DatagramChannel) key.channel();
                if (key.isWritable()) {
                    doWriteOperation(key, channel);
                } else if (key.isReadable()) {
                    doReadOperation(key, channel);
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
            datagramChannel.register(selector, SelectionKey.OP_READ,
                    new Attachment(datagramChannel.socket().getReceiveBufferSize()));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    private void doWriteOperation(final SelectionKey key,
            final DatagramChannel channel) {
        final var packet = (Attachment) key.attachment();
        try {
            channel.send(packet.getBuffer(), packet.getAddress());
        } catch (final IOException e) {
            e.printStackTrace();
        }
        key.interestOpsOr(SelectionKey.OP_READ);
        key.interestOpsAnd(~SelectionKey.OP_WRITE);
    }

    private void doReadOperation(final SelectionKey key,
            final DatagramChannel channel) throws IOException {
        final var packet = (Attachment) key.attachment();
        packet.getBuffer().clear();
        packet.setAddress(channel.receive(packet.getBuffer()));
        key.interestOpsAnd(0);

        SELECTOR_WAKEUP = () -> selector.wakeup();
        CompletableFuture
                .runAsync(packet.getBufferModifier(), taskExecutorService)
                .thenRun(() -> key.interestOpsOr(SelectionKey.OP_WRITE))
                .thenRun(SELECTOR_WAKEUP)
                .exceptionally(LOG_ERROR);
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

    private static class Attachment {

        private final ByteBuffer buffer;
        private final Runnable bufferModifier;
        private SocketAddress address;

        public Attachment(final int bufferSize) {
            buffer = ByteBuffer.allocate(bufferSize);
            this.bufferModifier = taskGenerator.apply(buffer);
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

        public synchronized Runnable getBufferModifier() {
            return bufferModifier;
        }
    }
}
