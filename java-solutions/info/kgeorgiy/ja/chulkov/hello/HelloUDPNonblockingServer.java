package info.kgeorgiy.ja.chulkov.hello;

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
import java.util.concurrent.ConcurrentLinkedQueue;

public class HelloUDPNonblockingServer extends AbstractHelloUDPServer {

    public static final int BUFFER_SIZE = 2048;
    private Selector selector;
    private ConcurrentLinkedQueue<Answer> toSend;

    @Override
    protected void getterIteration() throws IOException {
        selector.select();
        for (final var key : selector.selectedKeys()) {
            try {
                final DatagramChannel channel = (DatagramChannel) key.channel();

                if (!toSend.isEmpty() && key.isWritable()) {
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
    protected void prepare(final int port) {
        try {
            selector = Selector.open();
            final var datagramChannel = DatagramChannel.open();
            datagramChannel.configureBlocking(false);
            datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            datagramChannel.bind(new InetSocketAddress(port));
            datagramChannel.register(selector, SelectionKey.OP_READ);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        toSend = new ConcurrentLinkedQueue<>();
    }

    private void doWriteOperation(final ConcurrentLinkedQueue<Answer> toSend, final SelectionKey key,
            final DatagramChannel channel) {
        final var answer = toSend.remove();
        if (toSend.isEmpty()) {
            key.interestOpsAnd(~SelectionKey.OP_WRITE);
        }
        try {
            channel.send(answer.buffer(), answer.inetAddress());
        } catch (final IOException e) {
            e.printStackTrace();
        }
        semaphore.release();
        key.interestOpsOr(SelectionKey.OP_READ);
    }

    private void doReadOperation(final ConcurrentLinkedQueue<Answer> toSend, final SelectionKey key,
            final DatagramChannel channel) throws IOException {
        final var bytes = ByteBuffer.allocate(BUFFER_SIZE);
        final var sender = channel.receive(bytes);
        bytes.flip();
        if (semaphore.tryAcquire()) {
            CompletableFuture
                    .supplyAsync(taskGenerator.apply(bytes), taskExecutorService)
                    .thenApply(reqAnswer -> new Answer(reqAnswer, sender))
                    .thenAccept((answer) -> {
                        toSend.add(answer);
                        key.interestOpsOr(SelectionKey.OP_WRITE);
                        selector.wakeup();
                    })
                    .exceptionally((e) -> {
                        System.err.println("Error on executing task " + e.getMessage());
                        return null;
                    });
        } else {
            key.interestOpsAnd(~SelectionKey.OP_READ);
        }
    }

    @Override
    public void close() {
        try {
            selector.close();
        } catch (final IOException ignored) {
        }
        super.close();
    }

    private record Answer(ByteBuffer buffer, SocketAddress inetAddress) {

    }
}
