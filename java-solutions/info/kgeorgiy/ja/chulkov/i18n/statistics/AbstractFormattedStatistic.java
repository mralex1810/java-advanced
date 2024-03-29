package info.kgeorgiy.ja.chulkov.i18n.statistics;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.NavigableSet;
import java.util.ResourceBundle;
import java.util.TreeSet;
import java.util.function.Supplier;

public abstract class AbstractFormattedStatistic<T> implements FormattedStatistic {


    public static final String NOT_FOUND = "not_found";
    protected final ResourceBundle bundle;
    protected final NumberFormat outputNumberFormat;
    private final NavigableSet<T> occurrences;
    protected int counter = 0;
    protected BigDecimal total = BigDecimal.ZERO;

    protected AbstractFormattedStatistic(final Locale outputLocale, final ResourceBundle bundle,
            final TreeSet<T> previous) {
        this.bundle = bundle;
        this.occurrences = previous;
        outputNumberFormat = NumberFormat.getNumberInstance(outputLocale);
    }

    protected void registerObject(final T value, final double totalAdd) {
        counter++;
        occurrences.add(value);
        total = total.add(BigDecimal.valueOf(totalAdd));
    }

    @Override
    public String getTitle() {
        return MessageFormat.format(bundle.getString("title_format"),
                bundle.getString("statistic"),
                bundle.getString("on_" + getMultipleName()));
    }

    @Override
    public String formattedSummary() {
        return MessageFormat.format(bundle.getString("count_format"),
                bundle.getString("count"),
                bundle.getString(getMultipleName()),
                outputNumberFormat.format(counter));
    }

    public String formattedCountWithUniqueStat() {
        return MessageFormat.format(bundle.getString("count_uniq_format"),
                bundle.getString("count"),
                bundle.getString(getMultipleName()),
                outputNumberFormat.format(counter),
                outputNumberFormat.format(occurrences.size()),
                getUniqueWord(occurrences.size())
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
        return bundle.getString("min_max_base_format");
    }

    protected String formatMinMaxAvgT(final String key, final Supplier<T> elemGetter) {
        return formatMinMaxAvg(key, () -> objToString(elemGetter.get()), getGender());
    }

    protected String formatMinMaxAvg(final String key, final Supplier<String> elemGetter,
            final Gender gender) {
        return MessageFormat.format(minMaxFormat(),
                bundle.getString(key + "_" + gender.getKey()),
                bundle.getString(getName()),
                isEmpty() ? bundle.getString(NOT_FOUND) : elemGetter.get());
    }

    private boolean isEmpty() {
        return counter == 0;
    }

    public String formattedMaxStat() {
        return formatMinMaxAvgT("max", occurrences::last);
    }

    public String formattedMinStat() {
        return formatMinMaxAvgT("min", occurrences::first);
    }

    protected String formattedAverageStat() {
        return formatMinMaxAvg("avg", this::average, getGender());
    }

    protected double getAvgDouble() {
        return total.doubleValue() / counter;
    }

    public int getCounter() {
        return counter;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public T min() {
        return occurrences.isEmpty() ? null : occurrences.first();
    }

    public T max() {
        return occurrences.isEmpty() ? null : occurrences.last();
    }

    protected abstract String objToString(T obj);

    protected abstract String average();

    protected abstract String getName();

    protected abstract String getMultipleName();

    protected abstract Gender getGender();
}
