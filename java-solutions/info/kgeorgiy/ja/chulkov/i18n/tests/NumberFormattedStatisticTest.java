package info.kgeorgiy.ja.chulkov.i18n.tests;

import static info.kgeorgiy.ja.chulkov.i18n.tests.TestsData.LOCALES;
import static info.kgeorgiy.ja.chulkov.i18n.tests.TestsData.NUMBERS;

import info.kgeorgiy.ja.chulkov.i18n.statistics.AbstractNumberFormattedStatistic;
import info.kgeorgiy.ja.chulkov.i18n.statistics.NumberFormattedStatistic;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.Assert;
import org.junit.Test;

public class NumberFormattedStatisticTest {

    public static final Locale output = Locale.US;
    public static final Function<Locale, NumberFormat> NUMBER_GEN = NumberFormat::getNumberInstance;
    public static final Function<Locale, AbstractNumberFormattedStatistic> STATISTIC_GEN = (locale) ->
            new NumberFormattedStatistic(locale, output, null);

    public static void simpleTest(final Number number, final Function<Locale, NumberFormat> numberFormatGenerator,
            final Function<Locale, AbstractNumberFormattedStatistic> statisticGenerator) {
        LOCALES.forEach(
                locale -> checkNumbers(numberFormatGenerator.apply(locale).format(number), 1,
                        number.doubleValue(), () -> statisticGenerator.apply(locale))
        );
    }

    public static void checkShuffled(final Function<Locale, NumberFormat> numberFormatGenerator,
            final Function<Locale, AbstractNumberFormattedStatistic> statisticGenerator) {
        LOCALES.forEach(
                locale -> {
                    for (final var number1 : NUMBERS) {
                        for (final var number2 : NUMBERS) {
                            checkNumbers("abacaba "
                                            + numberFormatGenerator.apply(locale).format(number1)
                                            + " magic words.\n "
                                            + numberFormatGenerator.apply(locale).format(number2)
                                            + " end\n\n\t", 2, number1.doubleValue() + number2.doubleValue(),
                                    () -> statisticGenerator.apply(locale));
                        }
                    }

                }
        );
    }

    public static void checkNumbers(final String locale1, final int expected, final double a,
            final Supplier<AbstractNumberFormattedStatistic> supplier) {
        final var numberFormattedStatistic = supplier.get();
        numberFormattedStatistic.parseText(locale1);
        Assert.assertEquals(expected, numberFormattedStatistic.getCounter());
        Assert.assertEquals(a, numberFormattedStatistic.getTotal().doubleValue(), 1e-10d);
    }

    @Test
    public void simpleNumbersTest() {
        simpleTest(42, NUMBER_GEN, STATISTIC_GEN);
    }

    @Test
    public void differentNumbersTest() {
        NUMBERS.forEach(it -> simpleTest(it, NUMBER_GEN, STATISTIC_GEN));
    }

    @Test
    public void shuffledNumbersTest() {
        checkShuffled(NUMBER_GEN, STATISTIC_GEN);
    }
}
