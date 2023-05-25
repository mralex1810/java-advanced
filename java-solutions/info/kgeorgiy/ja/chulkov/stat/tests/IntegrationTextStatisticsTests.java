package info.kgeorgiy.ja.chulkov.stat.tests;

import info.kgeorgiy.ja.chulkov.stat.TextStatistics;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class IntegrationTextStatisticsTests {

    public static final String TESTS_RESOURCES = "info/kgeorgiy/ja/chulkov/stat/tests/resources/";

    public void test(final String fileName, final String inLocale, final String outLocale) throws IOException {
        final var inputPath = Path.of(TESTS_RESOURCES + fileName + ".in");
        final var outPath = Path.of(TESTS_RESOURCES + fileName + "_" + outLocale + ".out");
        final var tmpPath = Path.of(TESTS_RESOURCES + fileName + "_" + outLocale + ".tmp");
        TextStatistics.main(new String[] {inLocale, outLocale, inputPath.toString(), tmpPath.toString()});

        try {
            Assert.assertEquals(Files.readString(outPath), Files.readString(tmpPath));
        } finally {
            Files.deleteIfExists(tmpPath);
        }

    }

    public void test(final String fileName, final String inLocale) throws IOException {
        for (final String outLocale : List.of("ru_RU", "en_US")) {
            test(fileName, inLocale, outLocale);
        }
    }

    @Test
    public void exampleTest() throws IOException {
        test("example", "ru_RU");
    }

    @Test
    public void loremTest() throws IOException {
        test("lorem", "en_US");
    }

    @Test
    public void humourTest() throws IOException {
        test("humour", "ru_RU");
    }

    @Test
    public void bigTest() throws IOException {
        test("big", "en_US");
    }
}
