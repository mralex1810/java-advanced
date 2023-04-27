package info.kgeorgiy.ja.chulkov.crawler;

import info.kgeorgiy.java.advanced.crawler.CachingDownloader;
import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;
import info.kgeorgiy.java.advanced.crawler.URLUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

public class WebCrawler implements Crawler {

    private static int getPositiveInt(final String[] args, final int index, final int defaultValue, final String name) {
        if (index < args.length) {
            try {
                final int res =  Integer.parseInt(args[index]);
                if (res <= 0) {
                    throw new RuntimeException(name + " must be positive");
                }
                return res;
            } catch (final NumberFormatException e) {
                throw new RuntimeException(name + " must be integer");
            }
        } else {
            return defaultValue;
        }
    }

    public static void main(final String[] args) {
        Objects.requireNonNull(args);
        Arrays.stream(args).forEach(Objects::requireNonNull);
        if (args.length < 1) {
            printUsage();
            return;
        }
        final var url = args[0];
        try {
            final var depth = getPositiveInt(args, 1, 1, "depth");
            final var downloaders = getPositiveInt(args, 2, Integer.MAX_VALUE, "downloaders");
            final var extractors = getPositiveInt(args, 3, Integer.MAX_VALUE, "extractors");
            final var perHost = getPositiveInt(args, 4, Integer.MAX_VALUE, "perHost");
            try (final WebCrawler webCrawler =
                    new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost)) {
                try {
                    printResult(webCrawler.download(url, depth));
                } catch (final RuntimeException e) {
                    System.err.println("Error on executing process: " + e.getMessage());
                }
            } catch (final IOException e) {
                System.err.println("Error on creating CachingDownloader " + e.getMessage());
            }
        } catch (final RuntimeException e) {
            System.err.println(e.getMessage());
            printUsage();
        }
    }

    private static void printResult(final Result download) {
        System.out.println("Downloaded:");
        download.getDownloaded().forEach(System.out::println);
        System.out.println("Errors:");
        download.getErrors().forEach((url, exception) -> System.out.println(url + ": " + exception.getMessage()));
    }

    private static void printUsage() {
        System.out.println("WebCrawler url [depth [downloads [extractors [perHost]]]]");
    }

    private final ConcurrentHashMap<String, Semaphore> hostLimit = new ConcurrentHashMap<>();

    private final ExecutorService downoloadExecutorService;
    private final ExecutorService extractorExecutorService;
    private final Downloader downloader;
    private final int perHost;
    private final HashSet<String> used = new HashSet<>();

    public WebCrawler(final Downloader downloader,
            final int downloaders,
            final int extractors,
            final int perHost) {
        extractorExecutorService = Executors.newFixedThreadPool(extractors);
        downoloadExecutorService = Executors.newFixedThreadPool(downloaders);
        this.perHost = perHost;
        this.downloader = downloader;
    }

    private static <T, R> List<R> executeAll(
            final ExecutorService executorService,
            final List<T> src,
            final Function<T, Callable<R>> callableGen)
            throws InterruptedException {
        return executorService.invokeAll(src.stream()
                        .map(callableGen)
                        .toList())
                .stream()
                .map(it -> {
                            try {
                                return it.get();
                            } catch (final InterruptedException | ExecutionException e) {
                                throw new RuntimeException(e);
                            }
                        }
                )
                .filter(obj -> !Objects.isNull(obj))
                .toList();
    }


    @Override
    public Result download(final String url, final int depth) {
        return new DownloadAction().download(url, depth);
    }

    @Override
    public void close() {
        extractorExecutorService.shutdownNow();
        downoloadExecutorService.shutdownNow();
    }

    private class DownloadAction {
        private final List<String> downloaded = new ArrayList<>();
        private final Map<String, IOException> errors = new ConcurrentHashMap<>();

        private UrlDocument downloadDocument(final String url) {
            synchronized (used) {
                used.add(url);
            }
            try {
                final String host = URLUtils.getHost(url);
                hostLimit.computeIfAbsent(host, (ignore) -> new Semaphore(perHost));
                hostLimit.get(host).acquire();
                try {
                    return new UrlDocument(url, downloader.download(url));
                } finally {
                    hostLimit.get(host).release();
                }
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            } catch (final IOException e) {
                errors.put(url, e);
                return null;
            }
        }

        private List<String> parseDocument(final UrlDocument urlDocument) {
            try {
                return urlDocument.document.extractLinks();
            } catch (final IOException e) {
                errors.put(urlDocument.url, e);
                return null;
            }
        }

        private void download(final List<String> urls, final int depth) throws InterruptedException {
            if (depth == 0) {
                return;
            }
            final List<UrlDocument> urlDocuments = executeAll(downoloadExecutorService, urls,
                    (url) -> () -> downloadDocument(url));
            urlDocuments.stream().map(UrlDocument::url).forEach(downloaded::add);
            final List<String> nextUrls = executeAll(extractorExecutorService, urlDocuments,
                    (urlDocument) -> () -> parseDocument(urlDocument))
                    .stream()
                    .flatMap(Collection::stream)
                    .distinct()
                    .filter(o -> !used.contains(o))
                    .toList();
            download(nextUrls, depth - 1);
        }

        private Result download(final String url, final int depth) {
            try {
                download(List.of(url), depth);
                return new Result(downloaded, errors);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private record UrlDocument(String url, Document document) {

    }

}