package info.kgeorgiy.ja.chulkov.utils;

import java.util.Arrays;
import java.util.Objects;

/**
 * Utils class for working with arguments of main
 */
public class ArgumentsUtils {

    private ArgumentsUtils() {
    }

    /**
     * Parse non-negative integer from string
     *
     * @param src  string to parse int from
     * @param name name of parsing field
     * @return int from src string
     * @throws NumberFormatException if string isn't integer or integer < 0
     */
    public static int parseNonNegativeInt(final String src, final String name) throws NumberFormatException {
        final int res = Integer.parseInt(src);
        if (res < 0) {
            throw new NumberFormatException(name + " must be positive");
        }
        return res;
    }

    public static void checkNonNullsArgs(final String[] args) {
        Objects.requireNonNull(args);
        Arrays.stream(args).forEach(Objects::requireNonNull);
    }
}
