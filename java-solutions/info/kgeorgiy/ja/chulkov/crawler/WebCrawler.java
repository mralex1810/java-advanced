package info.kgeorgiy.ja.chulkov.crawler;

import info.kgeorgiy.java.advanced.crawler.AdvancedCrawler;
import info.kgeorgiy.java.advanced.crawler.CachingDownloader;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;
import info.kgeorgiy.java.advanced.crawler.URLUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class WebCrawler implements AdvancedCrawler {

    private final ConcurrentHashMap<String, Semaphore> hostLimit = new ConcurrentHashMap<>();
    private final ExecutorService downoloadExecutorService;
    private final ExecutorService extractorExecutorService;
    private final Downloader downloader;
    private final int perHost;

    public WebCrawler(final Downloader downloader,
            final int downloaders,
            final int extractors,
            final int perHost) {
        extractorExecutorService = Executors.newFixedThreadPool(extractors);
        downoloadExecutorService = Executors.newFixedThreadPool(downloaders);
        this.perHost = perHost;
        this.downloader = downloader;
    }

    private static int getPositiveInt(final String[] args, final int index, final int defaultValue, final String name) {
        if (index < args.length) {
            try {
                final int res = Integer.parseInt(args[index]);
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
                    new WebCrawler(new CachingDownloader(0), downloaders, extractors, perHost)) {
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


    @Override
    public Result download(final String url, final int depth) {
        return new DownloadAction().download(url, depth);
    }

    public Result download(final String url, final int depth, final List<String> hosts) {
        Objects.requireNonNull(hosts);
        hosts.forEach(Objects::requireNonNull);
        return new DownloadAction(hosts).download(url, depth);
    }

    @Override
    public void close() {
        final var services = List.of(extractorExecutorService, downoloadExecutorService);
        services.forEach(ExecutorService::shutdownNow);
        for (final ExecutorService executorService : services) {
            boolean terminated = false;
            while (!terminated) {
                try {
                    terminated = executorService.awaitTermination(1, TimeUnit.DAYS);
                } catch (final InterruptedException ignored) {
                }
            }
        }
    }

    private record UrlDocument(String url, Document document) {

    }

    private class DownloadAction {

        private final List<String> hosts;
        private final Set<String> downloaded = new HashSet<>();
        private final Map<String, IOException> errors = new ConcurrentHashMap<>();

        private DownloadAction() {
            this.hosts = null;
        }

        private DownloadAction(final List<String> hosts) {
            this.hosts = hosts;
        }

        private Optional<UrlDocument> downloadDocument(final String url) {
            try {
                final String host = URLUtils.getHost(url);
                if (hosts != null && !hosts.contains(host)) {
                    return Optional.empty();
                }
                hostLimit.computeIfAbsent(host, (ignore) -> new Semaphore(perHost));
                hostLimit.get(host).acquire();
                try {
                    final var res = downloader.download(url);
                    synchronized (downloaded) {
                        downloaded.add(url);
                    }
                    return Optional.of(new UrlDocument(url, res));
                } finally {
                    hostLimit.get(host).release();
                }
            } catch (final IOException e) {
                errors.put(url, e);
                return Optional.empty();
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<List<String>> parseDocument(final Optional<UrlDocument> urlDocumentOpt) {
            return urlDocumentOpt.map(urlDocument -> {
                try {
                    return urlDocument.document.extractLinks();
                } catch (final IOException e) {
                    errors.put(urlDocument.url, e);
                    return null;
                }
            });
        }

        private void download(final Set<String> urls, final int depth) throws InterruptedException {
            if (depth == 0) {
                return;
            }
            final Set<String> nextUrls = urls.stream()
                    .<Supplier<Optional<UrlDocument>>>map((url) -> () -> downloadDocument(url))
                    .map(it -> CompletableFuture.supplyAsync(it, downoloadExecutorService))
                    .map(it -> it.thenApplyAsync(this::parseDocument, extractorExecutorService))
                    .toList()
                    .stream()
                    .map(it -> {
                        try {
                            return it.get();
                        } catch (final InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList()
                    .stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .flatMap(Collection::stream)
                    .filter(o -> !downloaded.contains(o) && !errors.containsKey(o))
                    .collect(Collectors.toSet());

            download(nextUrls, depth - 1);
        }

        private Result download(final String url, final int depth) {
            try {
                download(Set.of(url), depth);
                return new Result(List.copyOf(downloaded), errors);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

}