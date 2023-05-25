package info.kgeorgiy.ja.chulkov.utils;

import java.util.Arrays;
import java.util.Locale;
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
     * @throws NumberFormatException if string isn't integer or integer below 0
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

    public static Locale toLocale(final String localeStr, final String errorMessage) {
        final var flags = localeStr.split("_");
        final var localeBuilder = new Locale.Builder().setLanguage(flags[0]);
        if (flags.length > 1) {
            localeBuilder.setRegion(flags[1]);
        }
        if (flags.length > 2) {
            localeBuilder.setVariant(flags[2]);
        }
        final var locale = localeBuilder.build();
        if (locale == null) {
            System.err.println(errorMessage);
        }
        return locale;
    }
}
