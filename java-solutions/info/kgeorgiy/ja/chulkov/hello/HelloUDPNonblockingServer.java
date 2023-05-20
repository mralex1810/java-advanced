package info.kgeorgiy.ja.chulkov.hello;

import info.kgeorgiy.ja.chulkov.utils.UDPUtils;
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
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Implementation of {@link HelloServer} with main method and nonblocking operations
 */
public class HelloUDPNonblockingServer extends AbstractHelloUDPServer {

    private Selector selector;
    private Queue<Packet> toSend;
    private Queue<Packet> toReceive;
    private final Consumer<SelectionKey> keyProcessor = this::processKey;

    /**
     * Method to run {@link HelloUDPNonblockingServer} from CLI
     *
     * @param args array of string {port, threads}
     */
    public static void main(final String[] args) {
        mainHelp(args, HelloUDPNonblockingServer::new);
    }

    @Override
    protected void getterIteration() throws IOException {
        selector.select(keyProcessor);
    }

    private void processKey(final SelectionKey key) {
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

    @Override
    protected void prepare(final int port, final int threads) {
        try {
            selector = Selector.open();
            final var datagramChannel = DatagramChannel.open();
            datagramChannel.configureBlocking(false);
            datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            datagramChannel.bind(new InetSocketAddress(port));
            datagramChannel.register(selector, SelectionKey.OP_READ);
            final SelectionKey key = selector.keys().stream().findAny().orElseThrow();
            toSend = new ArrayDeque<>(threads);
            toReceive = new ArrayDeque<>(threads);
            for (int i = 0; i < threads; i++) {
                toReceive.add(new Packet(datagramChannel.socket().getReceiveBufferSize()));
            }
            toReceive.forEach(it -> it.initTask(key, toSend));
        } catch (final IOException e) {
            closeSelectorAndChannels();
            throw new UncheckedIOException(e);
        }

    }

    private void doWriteOperation(final SelectionKey key,
            final DatagramChannel channel) {
        final Packet answer;
        synchronized (toSend) {
            answer = toSend.remove();
            if (toSend.isEmpty()) {
                key.interestOpsAnd(~SelectionKey.OP_WRITE);
            }
        }
        try {
            channel.send(answer.getBuffer(), answer.getAddress());
        } catch (final IOException e) {
            e.printStackTrace();
        }
        toReceive.add(answer);
        key.interestOpsOr(SelectionKey.OP_READ);
    }

    private void doReadOperation(final SelectionKey key,
            final DatagramChannel channel) throws IOException {
        final Packet packet;
        if (toReceive.isEmpty()) {
            key.interestOpsAnd(~SelectionKey.OP_READ);
            return;
        }
        packet = toReceive.remove();
        packet.getBuffer().clear();
        packet.setAddress(channel.receive(packet.getBuffer()));
        taskExecutorService.execute(packet.getTask());
    }

    @Override
    public void close() {
        closeSelectorAndChannels();
        super.close();
    }

    private void closeSelectorAndChannels() {
        selector.keys().forEach(UDPUtils::closeChannel);
        try {
            selector.close();
        } catch (final IOException ignored) {
        }
    }

    private static class Packet {

        private final ByteBuffer buffer;
        private final Runnable bufferModifier;
        private SocketAddress address;
        private Runnable task;

        public Packet(final int bufferSize) {
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

        public void initTask(final SelectionKey key, final Queue<Packet> toSend) {
            task = () -> {
                try {
                    bufferModifier.run();
                    synchronized (toSend) {
                        toSend.add(this);
                        key.interestOpsOr(SelectionKey.OP_WRITE);
                    }
                    key.selector().wakeup();
                } catch (final RuntimeException e) {
                    System.err.println("Error on executing task " + e.getMessage());
                }
            };
        }

        public Runnable getTask() {
            return task;
        }
    }
}
