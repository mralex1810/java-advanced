package info.kgeorgiy.ja.chulkov.stat.statistics;

import java.util.List;

public interface FormattedStatistic {
    void parseText(String text);
    String getTitle();
    String formattedCount();
    List<String> getStats();

}
