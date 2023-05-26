package info.kgeorgiy.ja.chulkov.i18n.tests;

import static info.kgeorgiy.ja.chulkov.i18n.tests.TestsData.DATES;
import static info.kgeorgiy.ja.chulkov.i18n.tests.TestsData.FORMATS;
import static info.kgeorgiy.ja.chulkov.i18n.tests.TestsData.LOCALES;
import static info.kgeorgiy.ja.chulkov.i18n.tests.TestsData.get26May;

import info.kgeorgiy.ja.chulkov.i18n.statistics.DateFormattedStatistic;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.junit.Assert;
import org.junit.Test;

public class DateFormattedStatisticTests {

    Locale output = Locale.US;

    void simpleTest(final Date date, final int style) {
        LOCALES.forEach(
                locale -> {
                    final var dateFormattedStatistic = new DateFormattedStatistic(locale, output, null);
                    final var dateStr = DateFormat.getDateInstance(style, locale).format(date);
                    System.out.println(dateStr);
                    dateFormattedStatistic.parseText(dateStr);
                    Assert.assertEquals(1, dateFormattedStatistic.getCounter());
                    Assert.assertEquals(date.getTime(), dateFormattedStatistic.getTotal().longValue());
                }
        );
    }

    @Test
    public void simpleDateTest() {
        final Calendar calendar = get26May();
        simpleTest(calendar.getTime(), DateFormat.DEFAULT);
    }


    @Test
    public void differentStyleTest() {
        final Calendar calendar = get26May();
        FORMATS.forEach(it -> simpleTest(calendar.getTime(), it));
    }

    @Test
    public void differentDatesTest() {
        DATES.forEach(it -> simpleTest(it.getTime(), DateFormat.DEFAULT));
    }

    @Test
    public void differentDatesStyleTest() {
        FORMATS.forEach(format -> DATES.forEach(date -> simpleTest(date.getTime(), format)));
    }

    @Test
    public void shuffledDatesTest() {
        LOCALES.forEach(
                locale -> {
                    for (final var format1 : FORMATS) {
                        for (final var format2 : FORMATS) {
                            for (final var date1 : DATES) {
                                for (final var date2 : DATES) {
                                    checkTwoDatesFormats(locale, format1, format2, date1, date2);
                                }
                            }
                        }
                    }

                }
        );
    }

    private void checkTwoDatesFormats(final Locale locale,
            final Integer format1, final Integer format2,
            final Calendar date1, final Calendar date2) {
        final var dateFormattedStatistic = new DateFormattedStatistic(locale, output, null);
        final var str = "abacaba "
                + DateFormat.getDateInstance(format1, locale).format(date1.getTime())
                + " magic words.\n "
                + DateFormat.getDateInstance(format2, locale).format(date2.getTime())
                + " end\n\n\t";
        dateFormattedStatistic.parseText(str);
        System.out.println(date1.getTime() + " : " + date2.getTime());
        Assert.assertEquals(2, dateFormattedStatistic.getCounter());
        Assert.assertEquals(dateFormattedStatistic.getTotal().longValue(),
                date1.getTime().getTime() + date2.getTime().getTime());
    }
}
