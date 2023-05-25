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
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class TextStatistics {

    public static final String ERROR_FORMAT = "%s: %s%n";
    public static final String RESOURCE_BUNDLE = "info.kgeorgiy.ja.chulkov.stat.StatisticsResourceBundle";

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
            System.err.format(
                    "%s. %n %s: <%s> <%s> <%s> <%s> %n",
                    bundle.getString("Incorrect_usage"),
                    bundle.getString("Usage"),
                    bundle.getString("text_locale"),
                    bundle.getString("write_local"),
                    bundle.getString("text_file"),
                    bundle.getString("output_file")
            );
            return;
        }
        final var inputLocale = toLocale(args[0], bundle.getString("input_locale_is_not_found"));
        final var outputLocale = toLocale(args[1], bundle.getString("output_locale_is_not_found"));
        try {
            bundle = ResourceBundle.getBundle(RESOURCE_BUNDLE, outputLocale);
        } catch (final MissingResourceException e) {
            System.err.format(ERROR_FORMAT, bundle.getString("bundle_not_found"), e.getLocalizedMessage());
        }
        String text = null;
        try {
            text = Files.readString(Path.of(args[2]), StandardCharsets.UTF_8);
        } catch (final InvalidPathException e) {
            System.err.format(ERROR_FORMAT, bundle.getString("invalid_input_path"), e.getLocalizedMessage());
        } catch (final IOException e) {
            System.err.format(ERROR_FORMAT, bundle.getString("io_exception_on_read"), e.getLocalizedMessage());
        }
        if (inputLocale == null || outputLocale == null || text == null) {
            return;
        }
        final List<FormattedStatistic> statistics = getStatistics(bundle, inputLocale, outputLocale);
        if (args.length > 4) {
            final var ruLocale = Locale.of("ru", "RU");
            final var enLocale = Locale.of("en", "US");
            final var ruBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE, ruLocale);
            final var enBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE, enLocale);
            printResults(args[2], args[3] + "_ru_RU.out", ruBundle, text,
                    getStatistics(ruBundle, inputLocale, ruLocale));
            printResults(args[2], args[3] + "_en_US.out", enBundle, text,
                    getStatistics(enBundle, inputLocale, enLocale));
        } else {
            printResults(args[2], args[3], bundle, text, statistics);
        }

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
                out.println("\t" + statistic.formattedCount() + ".");
            }
            for (final FormattedStatistic statistic : statistics) {
                out.println(statistic.getTitle());
                for (final String str : statistic.getStats()) {
                    out.println("\t" + str + ".");
                }
            }
        } catch (final IOException e) {
            System.err.printf(ERROR_FORMAT, bundle.getString("io_exception_on_write"), e.getLocalizedMessage());
        }
    }
}