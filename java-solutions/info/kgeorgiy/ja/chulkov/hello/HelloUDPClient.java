package info.kgeorgiy.ja.chulkov.hello;

import info.kgeorgiy.ja.chulkov.utils.UDPUtils;
import info.kgeorgiy.java.advanced.hello.HelloClient;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

/**
 * Implementation of {@link HelloClient} with main method and blocking multithreading operations
 */
public class HelloUDPClient extends AbstractHelloUDPClient {


    /**
     * Method to run {@link HelloUDPClient#run(String, int, String, int, int)} from CLI
     *
     * @param args array of string {host, port, prefix, threads, requests}
     */
    public static void main(final String[] args) {
        mainHelp(args, HelloUDPClient::new);
    }

    private void threadAction(
            final HelloClientThreadContext context,
            final SocketAddress address,
            final AtomicReference<RuntimeException> exception,
            final Thread mainThread
    ) {
        try (final var datagramSocket = new DatagramSocket()) {
            datagramSocket.setSoTimeout(TIMEOUT);
            while (!Thread.interrupted() && !context.isEnded()) {
                context.makeRequest();
                final var bytes = context.getRequestBytes();
                final var packetToSend = new DatagramPacket(bytes.array(), bytes.limit(), address);
                try {
                    datagramSocket.send(packetToSend);
                    final var packetForReceive = new DatagramPacket(new byte[datagramSocket.getReceiveBufferSize()],
                            datagramSocket.getReceiveBufferSize());
                    datagramSocket.receive(packetForReceive);
                    context.getAnswerBytes().clear();
                    context.getAnswerBytes().put(UDPUtils.dataToByteBuffer(packetForReceive).flip());
                    if (context.validateAnswer()) {
                        context.printRequestAndAnswer();
                        context.increment();
                    }
                } catch (final IOException | RuntimeException e) {
                    System.err.println(e.getMessage());
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


    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final SocketAddress address = prepareAddress(host, port);
        final AtomicReference<RuntimeException> exception = new AtomicReference<>(null);
        final Thread mainThread = Thread.currentThread();
        final var threadsList = IntStream.range(1, threads + 1)
                .mapToObj(threadNum -> new HelloClientThreadContext(threadNum, prefix, requests, BUFFER_SIZE))
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
}
