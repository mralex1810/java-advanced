package info.kgeorgiy.ja.chulkov.stat.statistics;

import java.text.BreakIterator;
import java.text.Collator;
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
        super(outputLocale, resourceBundle, keySuffix, new TreeSet<>(collator));
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
        return "%s: \"%s\"";
    }

    private String formatLengthString(final String str, final String key) {
        return String.format("%s: %s (\"%s\")",
                bundle.getString(key + keySuffix),
                str == null ? bundle.getString("not_found") : outputNumberFormat.format(str.length()),
                str == null ? "" : str
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
        return formatLengthString(minLength, "min_length");
    }

    public String formattedMaxLengthStat() {
        return formatLengthString(maxLength, "max_length");
    }

    @Override
    protected String average() {
        return outputNumberFormat.format(getAvgDouble());
    }

}
