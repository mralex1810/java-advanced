package info.kgeorgiy.ja.chulkov.stat.statistics;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class CurrencyFormattedStatistic extends AbstractNumberFormattedStatistic {
    public CurrencyFormattedStatistic(final Locale locale, final ResourceBundle resourceBundle) {
        super(locale, resourceBundle, "_currency",
                NumberFormat.getCurrencyInstance(locale));
    }
}
