package info.kgeorgiy.ja.chulkov.stat.statistics;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Comparator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeSet;
import java.util.function.Function;

public abstract class AbstractNumberFormattedStatistic extends AbstractFormattedStatistic<Number> {

    private final NumberFormat inputSpecialNumberFormat;
    private final NumberFormat outputSpecialNumberFormat;

    protected AbstractNumberFormattedStatistic(final Locale locale, final Locale outputLocale, final ResourceBundle resourceBundle,
            final String keySuffix, final Function<Locale, NumberFormat> specialNumberFormatGenerator) {
        super(locale, resourceBundle, keySuffix, new TreeSet<>(Comparator.comparingDouble(Number::doubleValue)));
        this.inputSpecialNumberFormat = specialNumberFormatGenerator.apply(locale);
        this.outputSpecialNumberFormat = specialNumberFormatGenerator.apply(outputLocale);
    }

    @Override
    public void parseText(final String text) {
        for (final var pp = new ParsePosition(0); pp.getIndex() != text.length(); ) {
            final Number res = inputSpecialNumberFormat.parse(text, pp);
            if (res == null) {
                pp.setIndex(pp.getIndex() + 1);
            } else {
                registerObject(res, res.longValue());
            }
        }
    }

    @Override
    protected String objToString(final Number obj) {
        return outputSpecialNumberFormat.format(obj);
    }

    @Override
    protected String average() {
        return outputSpecialNumberFormat.format(getAvgDouble());
    }
}
