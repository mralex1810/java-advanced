package info.kgeorgiy.ja.chulkov.stat;

import static info.kgeorgiy.ja.chulkov.utils.ArgumentsUtils.toLocale;

import info.kgeorgiy.ja.chulkov.stat.statistics.CurrencyFormattedStatistic;
import info.kgeorgiy.ja.chulkov.stat.statistics.DateFormattedStatistic;
import info.kgeorgiy.ja.chulkov.stat.statistics.FormattedStatistic;
import info.kgeorgiy.ja.chulkov.stat.statistics.NumberFormattedStatistic;
import info.kgeorgiy.ja.chulkov.stat.statistics.SentenceFormattedStatistic;
import info.kgeorgiy.ja.chulkov.stat.statistics.WordFormattedStatistic;
import info.kgeorgiy.ja.chulkov.utils.ArgumentsUtils;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class TextStatistics {

    public static final String ERROR_FORMAT = "error_format";
    public static final String RESOURCE_BUNDLE = "info.kgeorgiy.ja.chulkov.stat.StatisticsResourceBundle";
    public static final String STATISTIC_FORMAT = "statistic_format";

    public static void main(final String[] args) {
        ArgumentsUtils.checkNonNullsArgs(args);
        ResourceBundle bundle = null;
        try {
            bundle = ResourceBundle.getBundle("info.kgeorgiy.ja.chulkov.stat.StatisticsResourceBundle",
                    Locale.getDefault());
        } catch (final MissingResourceException ignored) {
        }
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("info.kgeorgiy.ja.chulkov.stat.StatisticsResourceBundle", Locale.US);
        }
        if (args.length < 4) {
            System.err.println(
                    MessageFormat.format(bundle.getString("usage_format"),
                            bundle.getString("Incorrect_usage"),
                            bundle.getString("Usage"),
                            bundle.getString("text_locale"),
                            bundle.getString("write_local"),
                            bundle.getString("text_file"),
                            bundle.getString("output_file")
                    ));
            return;
        }
        final var inputLocale = toLocale(args[0], bundle.getString("input_locale_is_not_found"));
        final var outputLocale = toLocale(args[1], bundle.getString("output_locale_is_not_found"));
        try {
            bundle = ResourceBundle.getBundle(RESOURCE_BUNDLE, outputLocale);
        } catch (final MissingResourceException e) {
            printException(bundle, e, "bundle_not_found");
        }
        String text = null;
        try {
            text = Files.readString(Path.of(args[2]), StandardCharsets.UTF_8);
        } catch (final InvalidPathException e) {
            printException(bundle, e, "invalid_input_path");
        } catch (final IOException e) {
            printException(bundle, e, "io_exception_on_read");
        }
        if (inputLocale == null || outputLocale == null || text == null) {
            return;
        }
        final List<FormattedStatistic> statistics = getStatistics(bundle, inputLocale, outputLocale);
        printResults(args[2], args[3], bundle, text, statistics);

    }

    private static void printException(final ResourceBundle bundle, final Exception e,
            final String invalidInputPath) {
        System.err.println(MessageFormat.format(bundle.getString(ERROR_FORMAT),
                bundle.getString(invalidInputPath),
                e.getLocalizedMessage()));
    }

    private static List<FormattedStatistic> getStatistics(final ResourceBundle bundle,
            final Locale inputLocale, final Locale outputLocale) {
        final var collator = Collator.getInstance(inputLocale);
        return List.of(
                new SentenceFormattedStatistic(inputLocale, outputLocale, bundle, collator),
                new WordFormattedStatistic(inputLocale, outputLocale, bundle, collator),
                new NumberFormattedStatistic(inputLocale, outputLocale, bundle),
                new CurrencyFormattedStatistic(inputLocale, outputLocale, bundle),
                new DateFormattedStatistic(inputLocale, outputLocale, bundle)
        );
    }

    private static void printResults(final String inputFileName, final String outputFileName,
            final ResourceBundle bundle, final String text, final List<FormattedStatistic> statistics) {
        try (final var out = new PrintStream(Files.newOutputStream(Path.of(outputFileName)), false,
                StandardCharsets.UTF_8)) {
            out.println(bundle.getString("analyzing_file") + " \"" + inputFileName + "\"");
            out.println(bundle.getString("summary"));
            for (final FormattedStatistic statistic : statistics) {
                statistic.parseText(text);
                out.println(MessageFormat.format(bundle.getString(STATISTIC_FORMAT),
                        statistic.formattedCount()));
            }
            for (final FormattedStatistic statistic : statistics) {
                out.println(statistic.getTitle());
                for (final String str : statistic.getStats()) {
                    out.println(MessageFormat.format(bundle.getString(STATISTIC_FORMAT), str));
                }
            }
        } catch (final IOException e) {
            System.err.println(MessageFormat.format(bundle.getString(ERROR_FORMAT),
                    bundle.getString("io_exception_on_write"),
                    e.getLocalizedMessage()));
        }
    }
}