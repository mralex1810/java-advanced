package info.kgeorgiy.ja.chulkov.stat.tests;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TestsData {


    public static List<Number> NUMBERS = List.of(42, 0, 3.14d, -10L, 23123233123L);
    public static List<Integer> FORMATS = List.of(DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL);
    public static List<Calendar> DATES = List.of(get26May(), getDate(1975, Calendar.JANUARY, 0), getDate(2003, Calendar.OCTOBER, 5));
    public static List<Locale> LOCALES = List.of(Locale.US, Locale.of("ru", "RU"), Locale.JAPANESE, Locale.CHINESE, Locale.of("ar"));

    public static Calendar get26May() {
        return getDate(2023, Calendar.MAY, 26);
    }

    public static Calendar getDate(final int year, final int month, final int date) {
        final var calendar = Calendar.getInstance();
        calendar.set(year, month, date, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }
}
