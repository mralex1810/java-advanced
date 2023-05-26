package info.kgeorgiy.ja.chulkov.i18n.statistics;

import java.text.BreakIterator;
import java.text.Collator;
import java.util.Locale;
import java.util.ResourceBundle;

public final class WordFormattedStatistic extends AbstractTextFormattedStatistic {

    public WordFormattedStatistic(final Locale locale, final Locale outputLocale, final ResourceBundle resourceBundle, final Collator collator) {
        super(outputLocale, resourceBundle, BreakIterator.getWordInstance(locale), collator);
    }

    @Override
    protected boolean isCorrect(final String subtext) {
        if (subtext.length() == 0) {
            return false;
        }

        return subtext.codePoints().allMatch(Character::isAlphabetic);
    }

    @Override
    protected String getName() {
        return "word";
    }

    @Override
    protected String getMultipleName() {
        return "words";
    }

    @Override
    protected Gender getGender() {
        return Gender.NEUTER;
    }
}
