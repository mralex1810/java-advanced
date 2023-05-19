package info.kgeorgiy.ja.chulkov.utils;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Utils class for working with UDP packets
 */
public class UDPUtils {

    private UDPUtils() {
    }

    /**
     * Decodes the data from a ByteBuffer using the UTF-8 character set.
     *
     * @param byteBuffer The ByteBuffer containing the encoded data.
     * @return A String representation of the decoded data.
     */
    public static String getDecodedData(final ByteBuffer byteBuffer) {
        return StandardCharsets.UTF_8.decode(byteBuffer).toString();
    }

    /**
     * Converts the data from a DatagramPacket into a ByteBuffer.
     *
     * @param packet The DatagramPacket containing the data.
     * @return A ByteBuffer containing the data from the DatagramPacket.
     */
    public static ByteBuffer dataToByteBuffer(final DatagramPacket packet) {
        final var byteBuffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
        byteBuffer.position(packet.getLength());
        return byteBuffer;
    }
}
