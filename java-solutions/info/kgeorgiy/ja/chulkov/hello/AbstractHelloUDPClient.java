package info.kgeorgiy.ja.chulkov.hello;

import info.kgeorgiy.ja.chulkov.utils.ArgumentsUtils;
import info.kgeorgiy.java.advanced.hello.HelloClient;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

abstract class AbstractHelloUDPClient implements HelloClient {

    /**
     * Timeout for iterations of HelloClient
     */
    public static final int TIMEOUT = 50;
    public static final int BUFFER_SIZE = 4096;

    private static void printUsage() {
        System.err.println("""
                    Usage: HelloClient host port prefix threads requests
                    host -- the name or ip address of the computer running the server
                    port -- port number to send requests to
                    prefix -- request prefix
                    threads -- number of parallel request threads
                    requests -- number of requests per thread
                """);
    }

    /**
     * Help method to run {@link HelloClient#run(String, int, String, int, int)} from CLI
     *
     * @param args array of string {host, port, prefix, threads, requests}
     */
    protected static void mainHelp(final String[] args, final Supplier<HelloClient> helloClientSupplier) {
        Objects.requireNonNull(args);
        Arrays.stream(args).forEach(Objects::requireNonNull);
        if (args.length != 5) {
            printUsage();
            return;
        }
        try {
            final int port = ArgumentsUtils.parseNonNegativeInt(args[1], "port");
            final int threads = ArgumentsUtils.parseNonNegativeInt(args[3], "threads");
            final int requests = ArgumentsUtils.parseNonNegativeInt(args[4], "requests");
            try {
                helloClientSupplier.get().run(args[0], port, args[2], threads, requests);
            } catch (final RuntimeException e) {
                System.err.println(e.getMessage());
            }
        } catch (final NumberFormatException e) {
            System.err.println(e.getMessage());
            printUsage();
        }
    }

    /**
     * Prepares a SocketAddress based on the provided host and port.
     *
     * @param host The host address or hostname.
     * @param port The port number.
     * @return A SocketAddress representing the host and port.
     * @throws UncheckedIOException if the host is unknown.
     */
    protected SocketAddress prepareAddress(final String host, final int port) {
        final SocketAddress address;
        try {
            address = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (final UnknownHostException e) {
            throw new UncheckedIOException(e);
        }
        return address;
    }
}
