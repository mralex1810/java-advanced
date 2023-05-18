package info.kgeorgiy.ja.chulkov.hello;

import info.kgeorgiy.ja.chulkov.utils.ArgumentsUtils;
import info.kgeorgiy.ja.chulkov.utils.Scanner;
import info.kgeorgiy.java.advanced.hello.HelloClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

/**
 * Implementation of {@link HelloClient} with main method
 */
public class HelloUDPClient implements HelloClient {

    private static final int TIMEOUT = 100;

    /**
     * Get UTF-8 data from {@link ByteBuffer}
     *
     * @param byteBuffer packet with encoded data
     * @return decoded string
     */
    public static String getDecodedData(final ByteBuffer byteBuffer) {
        return StandardCharsets.UTF_8.decode(byteBuffer).toString();
    }

    public static ByteBuffer dataToByteBuffer(final DatagramPacket packet) {
        return ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
    }

    /**
     * Method to run {@link HelloUDPClient#run(String, int, String, int, int)} from CLI
     *
     * @param args array of string {host, port, prefix, threads, requests}
     */
    public static void main(final String[] args) {
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
                new HelloUDPClient().run(args[0], port, args[2], threads, requests);
            } catch (final RuntimeException e) {
                System.err.println(e.getMessage());
            }
        } catch (final RuntimeException ignored) {
        }
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


    static SocketAddress prepareAddress(final String host, final int port) {
        final SocketAddress address;
        try {
            address = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (final UnknownHostException e) {
            throw new UncheckedIOException(e);
        }
        return address;
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final SocketAddress address = prepareAddress(host, port);
        final AtomicReference<RuntimeException> exception = new AtomicReference<>(null);
        final Thread mainThread = Thread.currentThread();
        final var threadsList = IntStream.range(1, threads + 1)
                .mapToObj(threadNum -> new ThreadHelloContext(threadNum, prefix, requests))
                .<Runnable>map(context -> () -> threadAction(context, address, exception, mainThread))
                .map(Thread::new)
                .peek(Thread::start)
                .toList();
        boolean interrupted = false;
        for (final Thread thread : threadsList) {
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (final InterruptedException ignored) {
                    if (exception.get() != null && !interrupted) {
                        threadsList.forEach(Thread::interrupt);
                        interrupted = true;
                    }
                }
            }
        }
        if (exception.get() != null) {
            throw exception.get();
        }
    }

    private void threadAction(
            final ThreadHelloContext threadHelloContext,
            final SocketAddress address,
            final AtomicReference<RuntimeException> exception,
            final Thread mainThread
    ) {
        try (final var datagramSocket = new DatagramSocket()) {
            datagramSocket.setSoTimeout(TIMEOUT);
            while (!Thread.interrupted() && !threadHelloContext.isEnded()) {
                final String request = threadHelloContext.makeRequest();
                final var bytes = request.getBytes(StandardCharsets.UTF_8);
                final var packetToSend = new DatagramPacket(bytes, bytes.length, address);
                try {
                    datagramSocket.send(packetToSend);
                    final var packetForReceive = new DatagramPacket(new byte[datagramSocket.getReceiveBufferSize()],
                            datagramSocket.getReceiveBufferSize());
                    datagramSocket.receive(packetForReceive);
                    final var ans = getDecodedData(dataToByteBuffer(packetForReceive));
                    if (threadHelloContext.validateAnswer(ans)) {
                        System.out.println(request + " " + ans);
                        threadHelloContext.increment();
                    } else {
                        System.err.println("Bad answer: " + ans);
                    }
                } catch (final IOException | RuntimeException e) {
                    System.err.println("Error on " + request + " " + e.getMessage());
                }
            }
        } catch (final SocketException | RuntimeException e) {
            if (exception.get() == null) {
                exception.set(new RuntimeException("Error on executing client functions in thread"));
                mainThread.interrupt();
            }
            exception.get().addSuppressed(e);
        }
    }

    static class ThreadHelloContext {

        private final int threadId;
        private final String prefix;
        private final int requests;
        private int request = 1;

        public ThreadHelloContext(final int threadId, final String prefix, final int requests) {
            this.threadId = threadId;
            this.prefix = prefix;
            this.requests = requests;
        }

        private static List<Integer> parseIntsFromString(final String string) {
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
            final var numbers = parseIntsFromString(ans);
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
