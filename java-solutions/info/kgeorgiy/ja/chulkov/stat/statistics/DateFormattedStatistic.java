package info.kgeorgiy.ja.chulkov.stat.statistics;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeSet;
import java.util.stream.Stream;

public class DateFormattedStatistic extends AbstractFormattedStatistic<Date> {

    private final List<DateFormat> inputDateFormats;
    private final DateFormat outDateFormat;

    public DateFormattedStatistic(final Locale inputLocale, final Locale outputLocale,
            final ResourceBundle resourceBundle) {
        super(outputLocale, resourceBundle, "_date", new TreeSet<>());
        inputDateFormats = Stream.of(DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL)
                .map(it -> DateFormat.getDateInstance(it, inputLocale))
                .toList();
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
    protected String getName() {
        return "date";
    }

    @Override
    protected String getMultipleName() {
        return "dates";
    }

    @Override
    protected Gender getGender() {
        return Gender.FEMININE;
    }

    @Override
    public void parseText(final String text) {
        for (final var position = new ParsePosition(0); position.getIndex() != text.length(); ) {
            boolean dateParsed = false;
            for (final var dateFormat : inputDateFormats) {
                final Date parsedDate = dateFormat.parse(text, position);
                if (parsedDate != null) {
                    registerObject(parsedDate, parsedDate.getTime());
                    dateParsed = true;
                    break;
                }
            }
            if (!dateParsed) {
                position.setIndex(position.getIndex() + 1);
            }
        }
    }

}
