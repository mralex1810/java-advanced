package info.kgeorgiy.ja.chulkov.hello;

import info.kgeorgiy.ja.chulkov.utils.ArgumentsUtils;
import info.kgeorgiy.java.advanced.hello.HelloClient;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

abstract class AbstractHelloUDPClient implements HelloClient {

    /**
     * Timeout for iterations of HelloClient
     */
    public static final int TIMEOUT = 50;

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

    protected static class ThreadHelloContext {

        public static final byte[] DELIMITER_BYTES = "_".getBytes(StandardCharsets.UTF_8);
        public static final CharsetDecoder UTF_8_DECODER = StandardCharsets.UTF_8.newDecoder();
        public static final String SPACE = " ";
        public static final int RADIX = 10;
        private final int threadId;
        private final byte[] prefixBytes;
        private final int requests;
        private final ByteBuffer requestBytes;
        private final CharBuffer requestChars;
        private final ByteBuffer answerBytes;
        private final CharBuffer answerChars;
        private int request = 1;

        public ThreadHelloContext(final int threadId, final String prefix, final int requests, final int bufferSize) {
            this.threadId = threadId;
            this.prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);
            this.requests = requests;
            requestBytes = ByteBuffer.allocate(bufferSize);
            requestChars = CharBuffer.allocate(bufferSize);
            answerBytes = ByteBuffer.allocate(bufferSize);
            answerChars = CharBuffer.allocate(bufferSize);
        }

        private static boolean checkFail(final boolean bool, final String error) {
            if (bool) {
                System.err.println(error);
                return true;
            }
            return false;
        }

        private void syncBytesToChars(final ByteBuffer src, final CharBuffer out) {
            src.flip();
            out.clear();
            UTF_8_DECODER.decode(src, out, true);
            out.flip();
        }

        public ByteBuffer getAnswerBytes() {
            return answerBytes;
        }

        public ByteBuffer getRequestBytes() {
            return requestBytes;
        }

        public int getRequest() {
            return request;
        }

        public void increment() {
            request++;
        }

        public boolean validateAnswer() {
            syncBytesToChars(answerBytes, answerChars);
            int numbers = 0;
            for (int i = 0; i < answerChars.length(); i++) {
                if (Character.isDigit(answerChars.charAt(i))) {
                    final int numberBeginIndex = i;

                    while (i < answerChars.length() && Character.isDigit(answerChars.charAt(i))) {
                        i++;
                    }
                    final int res;
                    try {
                        res = Integer.parseInt(answerChars, numberBeginIndex, i, 10);
                    } catch (final NumberFormatException e) {
                        System.err.println(e.getMessage());
                        return false;
                    }
                    if (numbers == 0) {
                        if (checkFail(res != threadId, "First number isn't thread num")) {
                            return false;
                        }
                    }
                    if (numbers == 1) {
                        if (checkFail(res != request, "Second number isn't request")) {
                            return false;
                        }
                    }
                    numbers++;
                }
            }
            return !checkFail(numbers != 2, "Not two numbers in string");
        }

        private static void putIntToByteBufferAsString(int src, final ByteBuffer out) {
            final int start = out.position();
            int size = 0;
            if (src == 0) {
                out.put((byte) Character.forDigit(0, RADIX));
                size++;
            }
            while (src != 0) {
                out.put((byte) Character.forDigit(src % RADIX, RADIX));
                src /= RADIX;
                size++;
            }
            for (int i = 0; i < size / 2; i++) {
                final byte tmp = out.get(start + i);
                out.put(start + i, out.get(out.position() - i - 1));
                out.put(out.position() - i - 1, tmp);
            }
        }

        public void printRequestAndAnswer() {
            syncBytesToChars(requestBytes, requestChars);
            System.out.append(requestChars);
            System.out.print(SPACE);
            System.out.append(answerChars);
            System.out.println();
        }

        public void makeRequest() {
            requestBytes.clear();
            requestBytes.put(prefixBytes);
            putIntToByteBufferAsString(threadId, requestBytes);
            requestBytes.put(DELIMITER_BYTES);
            putIntToByteBufferAsString(request, requestBytes);
            requestBytes.flip();
        }

        public boolean isEnded() {
            return request > requests;
        }
    }
}
