package info.kgeorgiy.ja.chulkov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Implementation of {@link HelloClient} with main method and nonblocking operations`
 */
public class HelloUDPNonblockingClient extends AbstractHelloUDPClient {

    public static final Consumer<SelectionKey> MAKE_WRITABLE = it -> it.interestOpsOr(SelectionKey.OP_WRITE);

    /**
     * Method to run {@link HelloUDPNonblockingClient#run(String, int, String, int, int)} from CLI
     *
     * @param args array of string {host, port, prefix, threads, requests}
     */
    public static void main(final String[] args) {
        mainHelp(args, HelloUDPNonblockingClient::new);
    }

    private static void doReadOperation(
            final DatagramChannel channel,
            final HelloClientThreadContext context)
            throws IOException {
        context.getAnswerBytes().clear();
        channel.receive(context.getAnswerBytes());
        if (context.validateAnswer()) {
            context.printRequestAndAnswer();
            context.increment();
            if (context.isEnded()) {
                closeChannel(channel);
            }
        }
    }

    private static Selector prepareSelector(
            final String prefix,
            final int threads,
            final int requests,
            final SocketAddress address) throws IOException {
        final Selector selector;
        selector = Selector.open();
        for (int thread = 1; thread <= threads; thread++) {
            final var datagramChannel = DatagramChannel.open();
            datagramChannel.configureBlocking(false);
            datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            datagramChannel.connect(address);
            datagramChannel.register(selector, SelectionKey.OP_WRITE,
                    new HelloClientThreadContext(thread, prefix, requests, BUFFER_SIZE));
        }

        return selector;
    }

    private static void processKey(final SocketAddress address, final SelectionKey key) {
        final var channel = (DatagramChannel) key.channel();
        final var context = (HelloClientThreadContext) key.attachment();
        try {
            if (key.isWritable()) {
                context.makeRequest();
                channel.send(context.getRequestBytes(), address);
                key.interestOps(SelectionKey.OP_READ);
            } else if (key.isReadable()) {
                doReadOperation(channel, context);
            }
        } catch (final IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void closeChannel(final SelectionKey selectionKey) {
        closeChannel(selectionKey.channel());
    }

    private static void closeChannel(final Channel channel) {
        while (true) {
            try {
                channel.close();
                break;
            } catch (final IOException ignored) {
            }
        }
    }

    private static Consumer<SelectionKey> getOpenChecker(final AtomicBoolean isOpened) {
        return key -> {
            if (key.channel().isOpen()) {
                isOpened.set(true);
            }
        };
    }

    @Override
    public void run(
            final String host,
            final int port,
            final String prefix,
            final int threads,
            final int requests) {
        final SocketAddress address = prepareAddress(host, port);
        final AtomicBoolean isOpened = new AtomicBoolean(false);
        final Consumer<SelectionKey> openChecker = getOpenChecker(isOpened);
        final Consumer<SelectionKey> keyProcessor = key -> processKey(address, key);
        try (final Selector selector = prepareSelector(prefix, threads, requests, address)) {
            try {
                while (!selector.keys().isEmpty()) {
                    if (selector.select(keyProcessor, TIMEOUT) == 0) {
                        selector.keys().forEach(MAKE_WRITABLE);
                    }
                    isOpened.set(false);
                    selector.keys().forEach(openChecker);
                    if (!isOpened.get()) {
                        break;
                    }
                }
            } finally {
                selector.keys().forEach(HelloUDPNonblockingClient::closeChannel);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }


}
