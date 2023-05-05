package info.kgeorgiy.ja.chulkov.hello;

import info.kgeorgiy.ja.chulkov.utils.ArgumentsUtils;
import info.kgeorgiy.java.advanced.hello.HelloClient;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Implementation of {@link HelloClient} with main method
 */
public class HelloUDPClient implements HelloClient {

    private static final int TIMEOUT = 50;

    /**
     * Get UTF-8 data from {@link DatagramPacket}
     *
     * @param packet packet with encoded data
     * @return decoded string
     */
    public static String getDecodedData(final DatagramPacket packet) {
        return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(packet.getData(), 0, packet.getLength())).toString();
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

    private static boolean checkNotFail(final boolean bool, final String error) {
        if (bool) {
            System.err.println(error);
            return false;
        }
        return true;
    }

    private static List<Integer> parseIntsFromString(final String string) {
        final List<Integer> ans = new ArrayList<>();
        for (int i = 0; i < string.length(); i++) {
            if (Character.isDigit(string.charAt(i))) {
                final int numberBeginIndex = i;

                while (i < string.length() && Character.isDigit(string.charAt(i)) ) {
                    i++;
                }

                final String numberString = string.substring(numberBeginIndex, i);
                ans.add(Integer.parseInt(numberString));
            }
        }

        return ans;
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final InetAddress address;
        final AtomicReference<RuntimeException> exception = new AtomicReference<>(null);
        try {
            address = InetAddress.getByName(host);
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }
        final Thread mainThread = Thread.currentThread();
        final var threadsList = IntStream.range(1, threads + 1)
                .mapToObj(threadNum -> new Thread(() ->
                        threadAction(port, (requestNum) -> prefix + threadNum + "_" + requestNum,
                                requests, address, exception, threadNum,
                                mainThread))
                )
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
            final int port,
            final Function<Integer, String> requestGenerator,
            final int requests,
            final InetAddress address,
            final AtomicReference<RuntimeException> exception,
            final int threadNum,
            final Thread mainThread
    ) {
        try (final var datagramSocket = new DatagramSocket()) {
            datagramSocket.setSoTimeout(TIMEOUT);
            for (int requestNum = 1; requestNum < requests + 1; requestNum++) {
                while (!Thread.interrupted()) {
                    final String request = requestGenerator.apply(requestNum);
                    final var bytes = request.getBytes(StandardCharsets.UTF_8);
                    final var packetToSend = new DatagramPacket(bytes, bytes.length, address, port);
                    try {
                        datagramSocket.send(packetToSend);
                        final var packetForReceive = new DatagramPacket(new byte[datagramSocket.getReceiveBufferSize()],
                                datagramSocket.getReceiveBufferSize());
                        datagramSocket.receive(packetForReceive);
                        final var ans = getDecodedData(packetForReceive);
                        if (validateAnswer(ans,
                                threadNum,
                                requestNum)) {
                            System.out.println(request + " " + ans);
                            break;
                        } else {
                            System.err.println("Bad answer: " + ans);
                        }
                    } catch (final IOException | RuntimeException e) {
                        System.err.println("Error on " + request + " " + e.getMessage());
                    }
                }
            }
        } catch (final SocketException | RuntimeException e) {
            if (exception.get() == null) {
                exception.set(
                        new RuntimeException("Error on executing client functions in thread"));
                mainThread.interrupt();
            }
            exception.get().addSuppressed(e);
        }
    }

    private boolean validateAnswer(final String ans, final int threadNum,
            final int request) {
        final var numbers = parseIntsFromString(ans);
        return checkNotFail(numbers.size() != 2, "Not two numbers in string")
                && checkNotFail(numbers.get(0) != threadNum, "First numbers isn't thread num")
                && checkNotFail(numbers.get(1) != request, "Second numbers isn't request");
    }
}
