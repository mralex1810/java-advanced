package info.kgeorgiy.ja.chulkov.stat.statistics;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeSet;

public class DateFormattedStatistic extends AbstractFormattedStatistic<Date> {
    private final DateFormat dateFormat;
    public DateFormattedStatistic(final Locale locale, final ResourceBundle resourceBundle) {
        super(locale, resourceBundle, "_date", new TreeSet<>());
        dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, locale);
    }

    @Override
    protected String objToString(final Date obj) {
        return dateFormat.format(obj);
    }

    @Override
    protected String average() {
        return dateFormat.format(new Date((long) getAvgDouble()));
    }

    @Override
    public void parseText(final String text) {
        for (final var pp = new ParsePosition(0); pp.getIndex() != text.length(); ) {
            final Date res = dateFormat.parse(text, pp);
            if (res == null) {
                pp.setIndex(pp.getIndex() + 1);
            } else {
                registerObject(res, res.getTime());
            }
        }
    }

}
