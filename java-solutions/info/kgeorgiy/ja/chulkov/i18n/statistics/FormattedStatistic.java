package info.kgeorgiy.ja.chulkov.i18n.statistics;

import java.util.List;

public interface FormattedStatistic {
    void parseText(String text);
    String getTitle();
    String formattedCount();
    List<String> getStats();

}
