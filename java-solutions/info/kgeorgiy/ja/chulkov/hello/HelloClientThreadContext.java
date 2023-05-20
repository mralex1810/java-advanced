package info.kgeorgiy.ja.chulkov.hello;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

class HelloClientThreadContext {

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

    public HelloClientThreadContext(final int threadId, final String prefix, final int requests,
            final int bufferSize) {
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

    private void syncBytesToChars(final ByteBuffer src, final CharBuffer out) {
        src.flip();
        out.clear();
        UTF_8_DECODER.decode(src, out, false);
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
