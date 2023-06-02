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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link HelloServer} with main method and nonblocking operations
 */
public class HelloUDPNonblockingServer extends AbstractHelloUDPServer {

    private Selector selector;
    private Queue<Packet> toSend;
    private Queue<Packet> toReceive;

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
        selector.select(this::processKey);
    }

    private void processKey(final SelectionKey key) {
        try {
            final DatagramChannel channel = (DatagramChannel) key.channel();
            if (key.isWritable()) {
                doWriteOperation(key, channel);
            }
            if (key.isReadable()) {
                doReadOperation(key, channel);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void prepare(final int port, final int threads) {
        taskExecutorService = new ThreadPoolExecutor(threads, threads,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(threads)
        );
        toSend = new ArrayDeque<>(threads);
        toReceive = new ArrayDeque<>(threads);
        try {
            selector = Selector.open();
            final var datagramChannel = DatagramChannel.open();
            configureDatagramChannel(port, datagramChannel);

            final SelectionKey key = selector.keys().stream().findAny().orElseThrow();
            for (int i = 0; i < threads; i++) {
                toReceive.add(new Packet(datagramChannel.socket().getReceiveBufferSize(), key));
            }
        } catch (final IOException e) {
            closeSelectorAndChannels();
            throw new UncheckedIOException(e);
        }
    }

    private void configureDatagramChannel(final int port, final DatagramChannel datagramChannel) throws IOException {
        try {
            datagramChannel.configureBlocking(false);
            datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            datagramChannel.bind(new InetSocketAddress(port));
            datagramChannel.register(selector, SelectionKey.OP_READ);
        } catch (final IOException e) {
            datagramChannel.close();
            throw e;
        }
    }

    private void doWriteOperation(final SelectionKey key, final DatagramChannel channel) {
        final Packet answer;
        synchronized (toSend) { // :NOTE: synchronized
            answer = toSend.poll();
            if (answer == null) {
                key.interestOpsAnd(~SelectionKey.OP_WRITE);
                return;
            }
        }
        try {
            channel.send(answer.getBuffer(), answer.getAddress());
        } catch (final IOException e) {
            processException(e); // :NOTE: unificate
        }
        toReceive.add(answer);
        key.interestOpsOr(SelectionKey.OP_READ);
    }

    // :NOTE: formatting
    private void doReadOperation(final SelectionKey key, final DatagramChannel channel) throws IOException {
        final Packet packet = toReceive.poll();
        if (packet == null) {
            key.interestOpsAnd(~SelectionKey.OP_READ);
            return;
        }
        packet.setAddress(channel.receive(packet.getBuffer().clear().position(BUFFER_OFFSET)));
        taskExecutorService.execute(packet.getTask());
    }

    @Override
    public void close() {
        super.close();
        closeSelectorAndChannels();
    }

    private void closeSelectorAndChannels() {
        selector.keys().forEach(UDPUtils::closeChannel);
        try {
            selector.close();
        } catch (final IOException ignored) {
        }
    }

    private class Packet {

        private final ByteBuffer buffer;
        private SocketAddress address;
        private final Runnable task;

        public Packet(final int bufferSize, final SelectionKey key) {
            buffer = ByteBuffer.allocate(bufferSize);
            prepareNewByteBytes(buffer.array());
            task = () -> process(key);
        }

        private void process(final SelectionKey key) {
            try {
                buffer.flip();
                synchronized (toSend) {
                    toSend.add(this);
                    key.interestOpsOr(SelectionKey.OP_WRITE); // :NOTE: external management
                }
                key.selector().wakeup();
            } catch (final RuntimeException e) {
                processException(e);
            }
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

        public Runnable getTask() {
            return task;
        }
    }
}
