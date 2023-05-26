package info.kgeorgiy.ja.chulkov.stat.statistics;

import java.text.BreakIterator;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeSet;

public abstract class AbstractTextFormattedStatistic extends AbstractFormattedStatistic<String> {

    private final BreakIterator iterator;
    private String minLength = null;
    private String maxLength = null;

    protected AbstractTextFormattedStatistic(final Locale outputLocale, final ResourceBundle resourceBundle,
            final BreakIterator iterator, final Collator collator, final String keySuffix) {
        super(outputLocale, resourceBundle, new TreeSet<>(collator));
        this.iterator = iterator;
    }

    @Override
    protected String objToString(final String obj) {
        return obj;
    }

    @Override
    public void parseText(final String text) {
        iterator.setText(text);

        for (int start = iterator.first(), end = iterator.next(); end != BreakIterator.DONE;
                start = end, end = iterator.next()) {
            final var subtext = text.substring(start, end).strip();
            if (isCorrect(subtext)) {
                registerObject(subtext, subtext.length());
                checkFirst(subtext);
                checkMinMaxLength(subtext);
            }
        }
    }

    protected abstract boolean isCorrect(String subtext);

    private void checkMinMaxLength(final String subtext) {
        if (minLength.length() > subtext.length()) {
            minLength = subtext;
        }
        if (maxLength.length() < subtext.length()) {
            maxLength = subtext;
        }
    }

    private void checkFirst(final String subtext) {
        if (minLength == null || maxLength == null) {
            minLength = subtext;
            maxLength = subtext;
        }
    }

    @Override
    protected String minMaxFormat() {
        return bundle.getString("min_max_text_format");
    }

    private String formatLengthString(final String str, final String key) {
        return MessageFormat.format(bundle.getString("length_format"),
                bundle.getString(key + "_" + Gender.FEMININE.getKey()),
                bundle.getString("length"),
                bundle.getString("of_" + getName()),
                counter == 0 ? bundle.getString(NOT_FOUND) : outputNumberFormat.format(str.length()),
                counter == 0 ? "" : str
        );
    }


    @Override
    public List<String> getStats() {
        return List.of(
                formattedCountWithUniqueStat(),
                formattedMinStat(),
                formattedMaxStat(),
                formattedMinLengthStat(),
                formattedMaxLengthStat(),
                formattedAverageStat()
        );
    }

    public String formattedMinLengthStat() {
        return formatLengthString(minLength, "min");
    }

    public String formattedMaxLengthStat() {
        return formatLengthString(maxLength, "max");
    }

    @Override
    protected String formattedAverageStat() {
        return String.format("%s %s %s: %s",
                bundle.getString("avg" + "_" + Gender.FEMININE.getKey()),
                bundle.getString("length"),
                bundle.getString("of_" + getName()),
                counter == 0 ? bundle.getString(NOT_FOUND) : average());
    }

    @Override
    protected String average() {
        return outputNumberFormat.format(getAvgDouble());
    }

}
