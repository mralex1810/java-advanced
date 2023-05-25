package info.kgeorgiy.ja.chulkov.stat.statistics;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeSet;

public class DateFormattedStatistic extends AbstractFormattedStatistic<Date> {
    private final DateFormat inputDateFormat;
    private final DateFormat outDateFormat;
    public DateFormattedStatistic(final Locale inputLocale, final Locale outputLocale, final ResourceBundle resourceBundle) {
        super(outputLocale, resourceBundle, "_date", new TreeSet<>());
        inputDateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, inputLocale);
        outDateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, outputLocale);
    }

    @Override
    protected String objToString(final Date obj) {
        return outDateFormat.format(obj);
    }

    @Override
    protected String average() {
        return outDateFormat.format(new Date((long) getAvgDouble()));
    }

    @Override
    public void parseText(final String text) {
        for (final var pp = new ParsePosition(0); pp.getIndex() != text.length(); ) {
            final Date res = inputDateFormat.parse(text, pp);
            if (res == null) {
                pp.setIndex(pp.getIndex() + 1);
            } else {
                registerObject(res, res.getTime());
            }
        }
    }

}
