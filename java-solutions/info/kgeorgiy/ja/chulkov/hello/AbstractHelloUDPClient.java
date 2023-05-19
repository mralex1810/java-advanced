package info.kgeorgiy.ja.chulkov.hello;

import info.kgeorgiy.ja.chulkov.utils.ArgumentsUtils;
import info.kgeorgiy.ja.chulkov.utils.Scanner;
import info.kgeorgiy.java.advanced.hello.HelloClient;
import java.io.ByteArrayInputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

abstract class AbstractHelloUDPClient implements HelloClient {
    public static final int TIMEOUT = 100;

    protected SocketAddress prepareAddress(final String host, final int port) {
        final SocketAddress address;
        try {
            address = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (final UnknownHostException e) {
            throw new UncheckedIOException(e);
        }
        return address;
    }


    protected abstract static class ClientMainHelper {
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

        public void mainHelp(final String[] args) {
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
                    getHelloClient().run(args[0], port, args[2], threads, requests);
                } catch (final RuntimeException e) {
                    System.err.println(e.getMessage());
                }
            } catch (final NumberFormatException e) {
                System.err.println(e.getMessage());
                printUsage();
            }
        }

        protected abstract HelloClient getHelloClient();
    }

    protected static class ThreadHelloContext {

        private final int threadId;
        private final String prefix;
        private final int requests;
        private int request = 1;

        public ThreadHelloContext(final int threadId, final String prefix, final int requests) {
            this.threadId = threadId;
            this.prefix = prefix;
            this.requests = requests;
        }

        private static List<Integer> parseIntegersFromString(final String string) {
            final List<Integer> ans = new ArrayList<>();
            final var scanner = new Scanner(new ByteArrayInputStream(string.getBytes()));
            while (scanner.cachNext(Character::isDigit)) {
                ans.add(Integer.parseInt(scanner.next()));
            }
            return ans;
        }

        private static boolean checkNotFail(final boolean bool, final String error) {
            if (bool) {
                System.err.println(error);
                return false;
            }
            return true;
        }

        public int getRequest() {
            return request;
        }

        public void increment() {
            request++;
        }

        public boolean validateAnswer(final String ans) {
            final var numbers = parseIntegersFromString(ans);
            return checkNotFail(numbers.size() != 2, "Not two numbers in string")
                    && checkNotFail(numbers.get(0) != threadId, "First numbers isn't thread num")
                    && checkNotFail(numbers.get(1) != request, "Second numbers isn't request");
        }

        public String makeRequest() {
            return prefix + threadId + "_" + request;
        }

        public boolean isEnded() {
            return request > requests;
        }
    }
}
