package info.kgeorgiy.ja.chulkov.crawler;

import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;
import info.kgeorgiy.java.advanced.crawler.URLUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class WebCrawler implements Crawler {

    public static final Result EMPTY_RESULT = new Result(List.of(), Map.of());
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

    private static Result concatResults(final Result a, final Result b) {
        return new Result(
                Stream.concat(a.getDownloaded().stream(), b.getDownloaded().stream()).toList(),
                Stream.concat(a.getErrors().entrySet().stream(), b.getErrors().entrySet().stream())
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue))
        );
    }

    private UrlDocument downloadDocument(final String url, final Map<String, IOException> errors) {
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

    private List<String> parseDocument(final UrlDocument urlDocument, final Map<String, IOException> errors) {
        try {
            return urlDocument.document.extractLinks();
        } catch (final IOException e) {
            errors.put(urlDocument.url, e);
            return null;
        }
    }

    private Result download(final List<String> urls, final int depth) throws InterruptedException {
        if (depth == 0) {
            return EMPTY_RESULT;
        }
        final List<String> downloaded = new ArrayList<>();
        final Map<String, IOException> errors = new ConcurrentHashMap<>();
        final List<UrlDocument> urlDocuments = executeAll(downoloadExecutorService, urls,
                (url) -> () -> downloadDocument(url, errors));
        urlDocuments.stream().map(UrlDocument::url).forEach(downloaded::add);
        final List<String> nextUrls = executeAll(extractorExecutorService, urlDocuments,
                (urlDocument) -> () -> parseDocument(urlDocument, errors))
                .stream()
                .flatMap(Collection::stream)
                .distinct()
                .filter(o -> !used.contains(o))
                .toList();
        return concatResults(new Result(downloaded, errors), download(nextUrls, depth - 1));
    }

    @Override
    public Result download(final String url, final int depth) {
        try {
            return download(List.of(url), depth);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        extractorExecutorService.shutdownNow();
        downoloadExecutorService.shutdownNow();
    }

    private Result getResult(final Future<Result> result) {
        try {
            return result.get();
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private record UrlDocument(String url, Document document) {

    }

}