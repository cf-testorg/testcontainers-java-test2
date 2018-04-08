package org.testcontainers.lifecycle;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@UtilityClass
public class Startables {

    public CompletableFuture<Void> deepStart(Collection<Startable> startables) {
        return deepStart(new ConcurrentHashMap<>(), startables.stream());
    }

    public CompletableFuture<Void> deepStart(Stream<Startable> startables) {
        return deepStart(new ConcurrentHashMap<>(), startables);
    }

    private CompletableFuture<Void> deepStart(ConcurrentMap<Startable, CompletableFuture<Void>> startProcess, Stream<Startable> startables) {
        return CompletableFuture.allOf(
            startables
                .map(it -> startProcess.computeIfAbsent(it, __ -> deepStart(startProcess, it.getDependencies().stream()).thenRunAsync(it::start)))
                .toArray(CompletableFuture[]::new)
        );
    }
}