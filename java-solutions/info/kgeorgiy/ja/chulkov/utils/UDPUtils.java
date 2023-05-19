package info.kgeorgiy.ja.chulkov.utils;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class UDPUtils {
    private UDPUtils() {}

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
}
