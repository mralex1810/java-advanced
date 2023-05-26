package info.kgeorgiy.ja.chulkov.i18n.statistics;

import java.util.List;

/**
 * A contract for retrieving formatted statistics.
 */
public interface FormattedStatistic {

    /**
     * Parses the given text to extract relevant information for the statistic.
     *
     * @param text the text to parse
     */
    void parseText(String text);

    /**
     * Retrieves the formatted title of the statistic.
     *
     * @return the title of the statistic
     */
    String getTitle();

    /**
     * Retrieves the formatted summary of objects.
     *
     * @return the formatted summary of objects
     */
    String formattedSummary();

    /**
     * Retrieves the list of formatted statistics.
     *
     * @return the list of formatted statistics
     */
    List<String> getStats();

}
