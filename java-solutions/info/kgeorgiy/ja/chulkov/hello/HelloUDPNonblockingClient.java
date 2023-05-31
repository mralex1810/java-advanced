package info.kgeorgiy.ja.chulkov.hello;


import info.kgeorgiy.ja.chulkov.utils.UDPUtils;
import info.kgeorgiy.java.advanced.hello.HelloClient;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

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
        mainHelp(args, HelloUDPNonblockingClient::new);
    }

    private static void doReadOperation(
        final DatagramChannel channel,
        final HelloClientThreadContext context,
        final SelectionKey key
    )
        throws IOException {
        context.getAnswerBytes().clear();
        channel.receive(context.getAnswerBytes());
        if (context.validateAnswer()) {
            context.printRequestAndAnswer();
            context.increment();
            if (context.isEnded()) {
                UDPUtils.closeChannel(channel);
                key.cancel();
            }
        }
    }

    private static Selector prepareSelector(
        final String prefix,
        final int threads,
        final int requests,
        final SocketAddress address
    ) throws IOException {
        final Selector selector = Selector.open();
        try {
            // :NOTE: 1..n
            for (int thread = 0; thread < threads; thread++) {
                final var datagramChannel = DatagramChannel.open();
                // :NOTE: leak
                try {
                    datagramChannel.configureBlocking(false);
                    datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                    datagramChannel.connect(address);
                    datagramChannel.register(selector, SelectionKey.OP_WRITE,
                        new HelloClientThreadContext(thread + 1, prefix, requests,
                            datagramChannel.socket().getReceiveBufferSize()
                        )
                    );
                } catch (final IOException e) {
                    datagramChannel.close();
                    throw e;
                }
            }
        } catch (final IOException e) {
            UDPUtils.closeSelectorWithChannels(selector);
            throw new UncheckedIOException(e);
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
                doReadOperation(channel, context, key);
            }
        } catch (final IOException e) {
            System.err.println(e.getMessage());
        }
    }


    @Override
    public void run(
        final String host,
        final int port,
        final String prefix,
        final int threads,
        final int requests
    ) {
        final SocketAddress address = prepareAddress(host, port);
        try {
            final Selector selector = prepareSelector(prefix, threads, requests, address);
            try {
                while (!selector.keys().isEmpty()) {
                    if (selector.select(key -> processKey(address, key), TIMEOUT) == 0) {
                        selector.keys().forEach(it -> it.interestOpsOr(SelectionKey.OP_WRITE));
                    }
                }
            } finally {
                UDPUtils.closeSelectorWithChannels(selector);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
