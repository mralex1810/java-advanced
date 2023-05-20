package info.kgeorgiy.ja.chulkov.hello;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

/**
 * The HelloClientThreadContext class represents the context for a client thread in the HelloClient application. It
 * provides methods for managing the thread's ID, prefix, number of requests, and request/response buffers.
 */
class HelloClientThreadContext {

    /**
     * The delimiter bytes used to separate integers in the request.
     */
    public static final byte[] DELIMITER_BYTES = "_".getBytes(StandardCharsets.UTF_8);

    /**
     * The UTF-8 decoder for decoding bytes to characters.
     */
    public static final CharsetDecoder UTF_8_DECODER = StandardCharsets.UTF_8.newDecoder();

    /**
     * The space character used for printing the request and response.
     */
    public static final String SPACE = " ";

    /**
     * The radix used for converting integers to strings.
     */
    public static final int RADIX = 10;

    /**
     * The ID of the client thread.
     */
    private final int threadId;

    /**
     * The bytes representing the prefix string for the requests.
     */
    private final byte[] prefixBytes;

    /**
     * The total number of requests to be sent.
     */
    private final int requests;

    /**
     * The buffer for storing the client's request bytes.
     */
    private final ByteBuffer requestBytes;

    /**
     * The buffer for storing the client's request characters.
     */
    private final CharBuffer requestChars;

    /**
     * The buffer for storing the server's response bytes.
     */
    private final ByteBuffer answerBytes;

    /**
     * The buffer for storing the server's response characters.
     */
    private final CharBuffer answerChars;

    /**
     * The current request number.
     */
    private int request = 1;

    /**
     * Constructs a HelloClientThreadContext object with the specified thread ID, prefix, number of requests, and buffer
     * size.
     *
     * @param threadId   The ID of the client thread.
     * @param prefix     The prefix string for the requests.
     * @param requests   The total number of requests to be sent.
     * @param bufferSize The size of the request/response buffer.
     */
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

    /**
     * Checks if a boolean condition is true and prints an error message if it is.
     *
     * @param bool  The boolean condition to check.
     * @param error The error message to print if the condition is true.
     * @return {@code true} if the condition is true, {@code false} otherwise.
     */
    private static boolean checkFail(final boolean bool, final String error) {
        if (bool) {
            System.err.println(error);
            return true;
        }
        return false;
    }

    /**
     * Puts an integer value as a string into a ByteBuffer.
     *
     * @param src The integer value to be put into the ByteBuffer.
     * @param out The ByteBuffer to store the integer value as a string.
     */
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

    /**
     * Converts a ByteBuffer to a CharBuffer using UTF-8 decoding.
     *
     * @param src The source ByteBuffer to be converted.
     * @param out The target CharBuffer to store the converted characters.
     */
    private void syncBytesToChars(final ByteBuffer src, final CharBuffer out) {
        src.flip();
        out.clear();
        UTF_8_DECODER.decode(src, out, false);
        out.flip();
    }

    /**
     * Gets the ByteBuffer containing the server's response.
     *
     * @return The ByteBuffer containing the server's response.
     */
    public ByteBuffer getAnswerBytes() {
        return answerBytes;
    }

    /**
     * Gets the ByteBuffer containing the client's request.
     *
     * @return The ByteBuffer containing the client's request.
     */
    public ByteBuffer getRequestBytes() {
        return requestBytes;
    }

    /**
     * Gets the current request number.
     *
     * @return The current request number.
     */
    public int getRequest() {
        return request;
    }

    /**
     * Increments the request number by 1.
     */
    public void increment() {
        request++;
    }

    /**
     * Validates the server's answer by checking if it contains two numbers: the thread ID and the request number.
     *
     * @return {@code true} if the answer is valid, {@code false} otherwise.
     */
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

    /**
     * Prints the client's request and the server's answer.
     */
    public void printRequestAndAnswer() {
        syncBytesToChars(requestBytes, requestChars);
        System.out.append(requestChars);
        System.out.print(SPACE);
        System.out.append(answerChars);
        System.out.println();
    }

    /**
     * Constructs the client's request by combining the prefix, thread ID, and request number.
     */
    public void makeRequest() {
        requestBytes.clear();
        requestBytes.put(prefixBytes);
        putIntToByteBufferAsString(threadId, requestBytes);
        requestBytes.put(DELIMITER_BYTES);
        putIntToByteBufferAsString(request, requestBytes);
        requestBytes.flip();
    }

    /**
     * Checks if all the requests have been sent.
     *
     * @return {@code true} if all requests have been sent, {@code false} otherwise.
     */
    public boolean isEnded() {
        return request > requests;
    }
}

