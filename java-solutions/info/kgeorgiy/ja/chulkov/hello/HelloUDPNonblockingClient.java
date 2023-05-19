package info.kgeorgiy.ja.chulkov.hello;

import info.kgeorgiy.ja.chulkov.utils.UDPUtils;
import info.kgeorgiy.java.advanced.hello.HelloClient;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of {@link HelloClient} with main method and nonblocking operations`
 */
public class HelloUDPNonblockingClient extends AbstractHelloUDPClient {

    /**
     * Method to run {@link HelloUDPNonblockingClient#run(String, int, String, int, int)} from CLI
     *
     * @param args array of string {host, port, prefix, threads, requests}
     */
    public static void main(final String[] args) {
        new NonBlockingClientMainHelper().mainHelp(args);
    }

    private static void doReadOperation(
            final DatagramChannel channel,
            final ThreadHelloContext context,
            final String request)
            throws IOException {
        final var bytes = ByteBuffer.allocate(channel.socket().getReceiveBufferSize());
        channel.receive(bytes);
        bytes.flip();
        final String answer = UDPUtils.getDecodedData(bytes);
        if (context.validateAnswer(answer)) {
            System.out.println(request + " " + answer);
            context.increment();
            if (context.isEnded()) {
                try {
                    channel.close();
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        } else {
            System.out.println("Bad answer: " + request + " " + answer);
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
                    new ThreadHelloContext(thread, prefix, requests));
        }

        return selector;
    }

    private static void doSelectorIteration(final SocketAddress address, final Selector selector) {
        if (selector.selectedKeys().isEmpty()) {
            selector.keys().forEach(it -> it.interestOpsOr(SelectionKey.OP_WRITE));
        } else {
            processKeys(address, selector);
        }
        selector.selectedKeys().clear();
    }

    private static void processKeys(final SocketAddress address, final Selector selector) {
        for (final var key : selector.selectedKeys()) {
            final var channel = (DatagramChannel) key.channel();
            final var context = (ThreadHelloContext) key.attachment();
            final String request = context.makeRequest();
            try {
                if (key.isWritable()) {
                    channel.send(ByteBuffer.wrap(request.getBytes(StandardCharsets.UTF_8)), address);
                    key.interestOps(SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    doReadOperation(channel, context, request);
                }
            } catch (final IOException | RuntimeException e) {
                System.err.println("Error on " + request + " " + e.getMessage());
            }
        }
    }

    @Override
    public void run(
            final String host,
            final int port,
            final String prefix,
            final int threads,
            final int requests) {
        final SocketAddress address = prepareAddress(host, port);
        try (final Selector selector = prepareSelector(prefix, threads, requests, address)) {
            while (!selector.keys().isEmpty()) {
                try {
                    selector.select(TIMEOUT);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
                if (selector.keys().isEmpty()) {
                    break;
                }
                doSelectorIteration(address, selector);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class NonBlockingClientMainHelper extends ClientMainHelper {

        @Override
        protected HelloClient getHelloClient() {
            return new HelloUDPNonblockingClient();
        }
    }


}
