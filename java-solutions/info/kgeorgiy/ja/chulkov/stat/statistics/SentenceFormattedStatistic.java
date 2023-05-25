package info.kgeorgiy.ja.chulkov.stat.statistics;

import java.text.BreakIterator;
import java.text.Collator;
import java.util.Locale;
import java.util.ResourceBundle;

public final class SentenceFormattedStatistic extends AbstractTextFormattedStatistic {

    public SentenceFormattedStatistic(final Locale locale, final ResourceBundle resourceBundle, final Collator collator) {
        super(locale, resourceBundle, BreakIterator.getSentenceInstance(locale), collator, "_sentence");
    }

    @Override
    protected boolean isCorrect(final String subtext) {
        return true;
    }
}
