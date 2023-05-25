package info.kgeorgiy.ja.chulkov.stat.statistics;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.NavigableSet;
import java.util.ResourceBundle;
import java.util.TreeSet;

public abstract class AbstractFormattedStatistic<T> implements FormattedStatistic {


    public static final String NOT_FOUND = "not_found";
    protected final ResourceBundle bundle;
    protected final String keySuffix;
    private final NavigableSet<T> previous;
    protected final NumberFormat outputNumberFormat;

    protected int counter = 0;
    protected BigInteger total = BigInteger.ZERO;

    public AbstractFormattedStatistic(final Locale outputLocale, final ResourceBundle bundle, final String keySuffix,
            final TreeSet<T> previous) {
        this.bundle = bundle;
        this.keySuffix = keySuffix;
        this.previous = previous;
        outputNumberFormat = NumberFormat.getNumberInstance(outputLocale);
    }

    protected void registerObject(final T value, final long totalAdd) {
        counter++;
        previous.add(value);
        total = total.add(BigInteger.valueOf(totalAdd));
    }

    @Override
    public String getTitle() {
        return bundle.getString("name" + keySuffix);
    }

    @Override
    public String formattedCount() {
        return String.format("%s: %s", bundle.getString("count" + keySuffix), outputNumberFormat.format(counter));
    }

    public String formattedCountWithUniqueStat() {
        return String.format("%s: %s (%s %s)",
                bundle.getString("count" + keySuffix),
                outputNumberFormat.format(counter),
                outputNumberFormat.format(previous.size()),
                getUniqueWord(previous.size())
        );
    }

    private String getUniqueWord(final int unique) {
        if (unique % 10 == 1 && unique % 100 != 11) {
            return bundle.getString("unique_one");
        } else {
            return bundle.getString("unique_not_one");
        }
    }

    @Override
    public List<String> getStats() {
        return List.of(
                formattedCountWithUniqueStat(),
                formattedMinStat(),
                formattedMaxStat(),
                formattedAverageStat()
        );
    }

    protected String minMaxFormat() {
        return "%s: %s";
    }

    public String formattedMaxStat() {
        return String.format(minMaxFormat(),
                bundle.getString("max" + keySuffix),
                previous.isEmpty() ? bundle.getString("not_found") : objToString(previous.last()));
    }

    public String formattedMinStat() {
        return String.format(minMaxFormat(),
                bundle.getString("min" + keySuffix),
                previous.isEmpty() ? bundle.getString(NOT_FOUND) : objToString(previous.first()));
    }

    protected abstract String objToString(T obj);

    protected String formattedAverageStat() {
        return String.format("%s: %s",
                bundle.getString("average" + keySuffix),
                counter == 0 ? bundle.getString(NOT_FOUND) : average());
    }

    protected abstract String average();

    protected double getAvgDouble() {
        return total.doubleValue() / counter;
    }
}
