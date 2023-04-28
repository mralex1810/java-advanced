package info.kgeorgiy.ja.chulkov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class HelloUDPClient implements HelloClient {

    private static final List<String> ANSWERS =
            List.of("Hello, %s", "%s ආයුබෝවන්", "Բարեւ, %s", "مرحبا %s", "Салом %s", "Здраво %s", "Здравейте %s",
                    "Прывітанне %s", "Привіт %s", "Привет, %s", "Поздрав %s", "سلام به %s", "שלום %s", "Γεια σας %s",
                    "העלא %s", "ہیل%s٪ ے", "Bonjou %s", "Bonjour %s", "Bună ziua %s", "Ciao %s", "Dia duit %s",
                    "Dobrý deň %s", "Dobrý den, %s", "Habari %s", "Halló %s", "Hallo %s", "Halo %s", "Hei %s", "Hej %s",
                    "Hello  %s", "Hello %s", "Hello %s", "Helo %s", "Hola %s", "Kaixo %s", "Kamusta %s", "Merhaba %s",
                    "Olá %s", "Ola %s", "Përshëndetje %s", "Pozdrav %s", "Pozdravljeni %s", "Salom %s", "Sawubona %s",
                    "Sveiki %s", "Tere %s", "Witaj %s", "Xin chào %s", "ສະບາຍດີ %s", "สวัสดี %s", "ഹലോ %s", "ಹಲೋ %s",
                    "హలో %s", "हॅलो %s", "नमस्कार%sको", "হ্যালো %s", "ਹੈਲੋ %s", "હેલો %s", "வணக்கம் %s",
                    "ကို %s မင်္ဂလာပါ", "გამარჯობა %s", "ជំរាបសួរ %s បាន", "こんにちは%s", "你好%s", "안녕하세요  %s");

    private static final List<Function<String, String>> MODIFICATION = List.of(
            s -> s,
            s -> s.replaceAll("([^0-9])", "$1$1"),
            (s) -> s.replaceAll("[^0-9]", "_"),
            (s) -> s.replaceAll("[^0-9]", "-")
//            (s) -> {
//
//                var it = Pattern.compile("([^0-9]+)").matcher(s);
//                for (int i = 0; i < it.groupCount() * it.groupCount(); i++) {
//                    it.replaceAll(it -> it.)
//                }
//            }
    );


    private static Stream<IntInt> requestsForThread(final int requests, final int thread) {
        return IntStream.range(0, requests)
                .mapToObj(it -> new IntInt(thread, it));
    }

    private static String toString(final DatagramPacketPairAnswer it) {
        return getDecodedData(it.packetToSend) + " " + getDecodedData(it.packetForReceive);
    }

    public static String getDecodedData(final DatagramPacket it) {
        return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(it.getData(), 0, it.getLength())).toString();
    }

    private static Runnable getRunnable(final int port, final DatagramSocket datagramSocket, final ReentrantLock lock,
            final InetAddress address, final String it) {
        return () -> {
            final var bytes = it.getBytes(StandardCharsets.UTF_8);
            final var packetToSend = new DatagramPacket(bytes, bytes.length, address, port);
            final var packetForReceive = new DatagramPacket(new byte[1024], 1024);
            while (true) {
                try {
                    lock.lock();
                    try {
                        datagramSocket.send(packetToSend);
                        datagramSocket.receive(packetForReceive);
                    } finally {
                        lock.unlock();
                    }
                    final var ans = getDecodedData(packetForReceive);
                    if (isGood(it, ans)) {
                        System.out.println(it + " " + ans);
                        break;
                    } else {
                        System.err.println("Bad response:" + it + " " + ans);
                    }
                } catch (final IOException e) {
                    System.err.println(it + " "
                            + e.getMessage());
                }
            }
        };
    }

    private static boolean isGood(final String it, final String ans) {
        for (final var answer : ANSWERS) {
            for (final var modify : MODIFICATION) {
                if (ans.equals(String.format(answer, modify.apply(it)))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        try (
                final var datagramSocket = new DatagramSocket()
        ) {
            final ReentrantLock lock = new ReentrantLock();
            datagramSocket.setSoTimeout(200);
            final var address = InetAddress.getByName(host);
            final var threadsList = IntStream.range(0, threads)
                    .mapToObj(threadNum ->
                            requestsForThread(requests, threadNum)
                                    .map(it -> prefix + it.a + "_" + it.b)
                                    .map(it -> getRunnable(port, datagramSocket, lock, address, it))
                                    .toList()
                    )
                    .map(runnables -> new Thread(() -> runnables.forEach(Runnable::run)))
                    .peek(Thread::start)
                    .toList();
            threadsList.forEach(thread -> {
                while (true) {
                    try {
                        thread.join();
                        break;
                    } catch (final InterruptedException ignored) {
                    }
                }
            });

        } catch (final UnknownHostException | SocketException e) {
            throw new RuntimeException(e);
        }


    }

    private record DatagramPacketPairAnswer(DatagramPacket packetToSend, DatagramPacket packetForReceive,
                                            String expected) {

    }

    private record BytesString(byte[] bytes, String string) {

    }

    private record IntInt(int a, int b) {

    }
}
