package info.kgeorgiy.ja.chulkov.crawler;

import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;
import info.kgeorgiy.java.advanced.crawler.URLUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class WebCrawler implements Crawler {

    public static final Result EMPTY_RESULT = new Result(List.of(), Map.of());
    private final ConcurrentHashMap<String, Semaphore> hostLimit = new ConcurrentHashMap<>();

    private final ExecutorService downoloadExecutorService;
    private final ExecutorService extractorExecutorService;
    private final Downloader downloader;
    private final int perHost;
    private final ConcurrentHashMap<String, Future<List<String>>> linkCache = new ConcurrentHashMap<>();

    public WebCrawler(final Downloader downloader,
            final int downloaders,
            final int extractors,
            final int perHost) {
        extractorExecutorService = Executors.newFixedThreadPool(extractors);
        downoloadExecutorService = Executors.newFixedThreadPool(downloaders);
        this.perHost = perHost;
        this.downloader = downloader;
    }


    @Override
    public Result download(final String url, final int depth) {
        final RecursiveDownloader recursiveDownloader = new RecursiveDownloader(url, depth);
        try {
            return recursiveDownloader.get();
        } catch (final InterruptedException | ExecutionException e) {
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

    private class RecursiveDownloader extends RecursiveTask<Result> {

        private final String url;
        private final int depth;

        RecursiveDownloader(final String url, final int depth) {
            this.url = url;
            this.depth = depth;
        }

        private Document download() throws IOException {

            final String host = URLUtils.getHost(url);
            hostLimit.computeIfAbsent(host, (ignore) -> new Semaphore(perHost));
            try {
                hostLimit.get(host).acquire();
                try {
                    return downloader.download(url);
                } finally {
                    hostLimit.get(host).release();
                }
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Result compute() {
            if (depth == 0) {
                return EMPTY_RESULT;
            }
            final List<String> downloaded = new ArrayList<>();
            final Map<String, IOException> errors = new HashMap<>();
            try {
                synchronized (linkCache) {
                    linkCache.computeIfAbsent(url, (ignore) -> CompletableFuture.supplyAsync(() -> {
                        try {
                            final Document document = downoloadExecutorService
                                    .submit(this::download)
                                    .get();
                            return extractorExecutorService
                                    .submit(document::extractLinks)
                                    .get();
                        } catch (final InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    ));
                }

                downloaded.add(url);
                final var recursives = linkCache.get(url).get().stream()
                        .distinct()
                        .map(link -> new RecursiveDownloader(link, depth - 1))
                        .toList();
                if (recursives.size() > 0 && depth > 1) {
                    recursives.subList(1, recursives.size()).forEach(ForkJoinTask::fork);
                    Stream.concat(
                                    Stream.of(recursives.get(0).compute()),
                                    recursives.subList(1, recursives.size()).stream().map(ForkJoinTask::join))
                            .forEach(result -> {
                                downloaded.addAll(result.getDownloaded());
                                errors.putAll(result.getErrors());
                            });
                }
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            } catch (final ExecutionException e) {
                if (e.getCause() instanceof final IOException cause) {
                    errors.put(url, cause);
                } else {
                    throw new RuntimeException(e);
                }
            }
            return new Result(downloaded, errors);
        }

    }
}