package info.kgeorgiy.ja.chulkov.stat.statistics;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class NumberFormattedStatistic extends AbstractNumberFormattedStatistic {

    public NumberFormattedStatistic(final Locale locale, final ResourceBundle resourceBundle) {
        super(locale, resourceBundle, "_number", NumberFormat.getNumberInstance(locale));
    }
}
