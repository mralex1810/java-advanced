package info.kgeorgiy.ja.chulkov.i18n.statistics;

import java.text.BreakIterator;
import java.text.Collator;
import java.util.Locale;
import java.util.ResourceBundle;

public final class SentenceFormattedStatistic extends AbstractTextFormattedStatistic {

    public SentenceFormattedStatistic(final Locale locale, final Locale outputLocale, final ResourceBundle resourceBundle, final Collator collator) {
        super(outputLocale, resourceBundle, BreakIterator.getSentenceInstance(locale), collator);
    }

    @Override
    protected boolean isCorrect(final String subtext) {
        return subtext.length() != 1;
    }

    @Override
    protected String getName() {
        return "sentence";
    }

    @Override
    protected String getMultipleName() {
        return "sentences";
    }

    @Override
    protected Gender getGender() {
        return Gender.NEUTER;
    }
}
