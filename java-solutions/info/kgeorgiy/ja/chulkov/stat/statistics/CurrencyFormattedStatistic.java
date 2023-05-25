package info.kgeorgiy.ja.chulkov.stat.statistics;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class CurrencyFormattedStatistic extends AbstractNumberFormattedStatistic {
    public CurrencyFormattedStatistic(final Locale locale, final Locale outputLocale, final ResourceBundle resourceBundle) {
        super(locale, outputLocale, resourceBundle, "_currency",
                NumberFormat::getCurrencyInstance);
    }
}
