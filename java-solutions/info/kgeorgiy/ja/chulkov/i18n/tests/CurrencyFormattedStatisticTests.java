package info.kgeorgiy.ja.chulkov.i18n.tests;

import static info.kgeorgiy.ja.chulkov.i18n.tests.NumberFormattedStatisticTest.checkShuffled;
import static info.kgeorgiy.ja.chulkov.i18n.tests.NumberFormattedStatisticTest.output;
import static info.kgeorgiy.ja.chulkov.i18n.tests.NumberFormattedStatisticTest.simpleTest;
import static info.kgeorgiy.ja.chulkov.i18n.tests.TestsData.NUMBERS;

import info.kgeorgiy.ja.chulkov.i18n.statistics.AbstractNumberFormattedStatistic;
import info.kgeorgiy.ja.chulkov.i18n.statistics.NumberFormattedStatistic;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Function;
import org.junit.Test;

public class CurrencyFormattedStatisticTests {

    public static final Function<Locale, NumberFormat> CURRENCY_GEN = NumberFormat::getNumberInstance;
    public static final Function<Locale, AbstractNumberFormattedStatistic> STATISTIC_GEN = (locale) ->
            new NumberFormattedStatistic(locale, output, null);

    @Test
    public void simpleCurrencyTest() {
        simpleTest(42, CURRENCY_GEN, STATISTIC_GEN);
    }

    @Test
    public void differentCurrencyTest() {
        NUMBERS.forEach(it -> simpleTest(it, CURRENCY_GEN, STATISTIC_GEN));
    }

    @Test
    public void shuffledCurrencyTest() {
        checkShuffled(CURRENCY_GEN, STATISTIC_GEN);
    }

}
