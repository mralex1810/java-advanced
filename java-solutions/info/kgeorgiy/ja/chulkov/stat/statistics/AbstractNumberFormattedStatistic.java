package info.kgeorgiy.ja.chulkov.stat.statistics;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Comparator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeSet;

public abstract class AbstractNumberFormattedStatistic extends AbstractFormattedStatistic<Number> {

    private final NumberFormat specialNumberFormat;

    protected AbstractNumberFormattedStatistic(final Locale locale, final ResourceBundle resourceBundle,
            final String keySuffix, final NumberFormat specialNumberFormat) {
        super(locale, resourceBundle, keySuffix, new TreeSet<>(Comparator.comparingLong(Number::longValue)));
        this.specialNumberFormat = specialNumberFormat;
    }

    @Override
    public void parseText(final String text) {
        for (final var pp = new ParsePosition(0); pp.getIndex() != text.length(); ) {
            final Number res = specialNumberFormat.parse(text, pp);
            if (res == null) {
                pp.setIndex(pp.getIndex() + 1);
            } else {
                registerObject(res, res.longValue());
            }
        }
    }

    @Override
    protected String objToString(final Number obj) {
        return specialNumberFormat.format(obj);
    }

    @Override
    protected String average() {
        return specialNumberFormat.format(getAvgDouble());
    }
}
