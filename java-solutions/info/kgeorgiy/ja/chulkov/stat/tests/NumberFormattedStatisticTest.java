package info.kgeorgiy.ja.chulkov.stat.tests;

import static info.kgeorgiy.ja.chulkov.stat.tests.TestsData.LOCALES;
import static info.kgeorgiy.ja.chulkov.stat.tests.TestsData.NUMBERS;

import info.kgeorgiy.ja.chulkov.stat.statistics.NumberFormattedStatistic;
import java.text.NumberFormat;
import java.util.Locale;
import org.junit.Assert;
import org.junit.Test;

public class NumberFormattedStatisticTest {

    Locale output = Locale.US;

    void simpleTest(final Number number) {
        LOCALES.forEach(
                locale -> checkNumbers(locale, NumberFormat.getNumberInstance(locale).format(number), 1,
                        number.doubleValue())
        );
    }

    @Test
    public void simpleNumbersTest() {
        simpleTest(42);
    }

    @Test
    public void differentNumbersTest() {
        NUMBERS.forEach(this::simpleTest);
    }

    @Test
    public void shuffledNumbersTest() {
        LOCALES.forEach(
                locale -> {
                    for (final var a : NUMBERS) {
                        for (final var b: NUMBERS) {
                            checkNumbers(locale, "abacaba "
                                    + NumberFormat.getNumberInstance(locale).format(a)
                                    + " magic words.\n "
                                    + NumberFormat.getNumberInstance(locale).format(b)
                                    + " end\n\n\t", 2, a.doubleValue() + b.doubleValue());
                        }
                    }
                    
                }
        );
    }

    private void checkNumbers(final Locale locale, final String locale1, final int expected, final double a) {
        final var numberFormattedStatistic =
                new NumberFormattedStatistic(locale, output, null);
        numberFormattedStatistic.parseText(locale1);
        Assert.assertEquals(expected, numberFormattedStatistic.getCounter());
        Assert.assertEquals(a, numberFormattedStatistic.getTotal().doubleValue(), 1e-10d);
    }
}
