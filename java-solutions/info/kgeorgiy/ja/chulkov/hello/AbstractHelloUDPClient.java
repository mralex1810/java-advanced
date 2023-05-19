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

    /**
     * Timeout for iterations of HelloClient
     */
    public static final int TIMEOUT = 100;

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


    /**
     * Help class to run {@link HelloClient} from CLI
     */
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

        /**
         * Help method to run {@link HelloClient#run(String, int, String, int, int)} from CLI
         *
         * @param args array of string {host, port, prefix, threads, requests}
         */
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

    /**
     * A context class used by a threaded application to manage thread-specific information for executing requests. The
     * class provides methods for retrieving and updating request-related information, validating answers, generating
     * request strings, and checking if all requests have been completed.
     */
    protected static class ThreadHelloContext {

        private final int threadId;
        private final String prefix;
        private final int requests;
        private int request = 1;

        /**
         * Constructs a ThreadHelloContext object with the specified thread ID, prefix, and number of requests.
         *
         * @param threadId The ID of the thread.
         * @param prefix   The prefix string to be used in request generation.
         * @param requests The total number of requests to be executed.
         */
        public ThreadHelloContext(final int threadId, final String prefix, final int requests) {
            this.threadId = threadId;
            this.prefix = prefix;
            this.requests = requests;
        }

        /**
         * Parses integers from a string and returns them as a list.
         *
         * @param string The string containing integers.
         * @return A list of integers parsed from the input string.
         */
        private static List<Integer> parseIntegersFromString(final String string) {
            final List<Integer> ans = new ArrayList<>();
            final var scanner = new Scanner(new ByteArrayInputStream(string.getBytes()));
            while (scanner.cachNext(Character::isDigit)) {
                ans.add(Integer.parseInt(scanner.next()));
            }
            return ans;
        }

        /**
         * Checks if a boolean condition is false and prints an error message if it is true.
         *
         * @param bool  The boolean condition to check.
         * @param error The error message to print if the condition is true.
         * @return {@code true} if the condition is false, {@code false} otherwise.
         */
        private static boolean checkNotFail(final boolean bool, final String error) {
            if (bool) {
                System.err.println(error);
                return false;
            }
            return true;
        }

        /**
         * Returns the current request number.
         *
         * @return The current request number.
         */
        public int getRequest() {
            return request;
        }

        /**
         * Increments the request number by one.
         */
        public void increment() {
            request++;
        }

        /**
         * Validates the answer received from a request.
         *
         * @param ans The answer string to validate.
         * @return {@code true} if the answer is valid, {@code false} otherwise.
         */
        public boolean validateAnswer(final String ans) {
            final var numbers = parseIntegersFromString(ans);
            return checkNotFail(numbers.size() != 2, "Not two numbers in string")
                    && checkNotFail(numbers.get(0) != threadId, "First number isn't thread num")
                    && checkNotFail(numbers.get(1) != request, "Second number isn't request");
        }

        /**
         * Generates the request string for the current thread and request number.
         *
         * @return The generated request string.
         */
        public String makeRequest() {
            return prefix + threadId + "_" + request;
        }

        /**
         * Checks if all requests have been completed for the current thread.
         *
         * @return {@code true} if all requests have been completed, {@code false} otherwise.
         */
        public boolean isEnded() {
            return request > requests;
        }
    }
}
