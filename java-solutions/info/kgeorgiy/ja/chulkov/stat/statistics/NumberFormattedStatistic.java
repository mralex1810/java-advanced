package info.kgeorgiy.ja.chulkov.stat.statistics;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class NumberFormattedStatistic extends AbstractNumberFormattedStatistic {

    public NumberFormattedStatistic(final Locale inputLocale, final Locale outputLocale, final ResourceBundle resourceBundle) {
        super(inputLocale, outputLocale, resourceBundle, NumberFormat::getNumberInstance);
    }

    @Override
    protected String getName() {
        return "number";
    }

    @Override
    protected String getMultipleName() {
        return "numbers";
    }

    @Override
    protected Gender getGender() {
        return Gender.NEUTER;
    }
}
