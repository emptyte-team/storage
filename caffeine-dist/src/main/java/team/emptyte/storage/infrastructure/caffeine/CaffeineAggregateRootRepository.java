/*
 * This file is part of storage, licensed under the MIT License
 *
 * Copyright (c) 2025 Emptyte Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package team.emptyte.storage.infrastructure.caffeine;

import org.jetbrains.annotations.Contract;
import team.emptyte.storage.domain.AggregateRoot;
import team.emptyte.storage.domain.repository.AggregateRootRepository;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.jetbrains.annotations.NotNull;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;

/**
 * An implementation of {@link AggregateRootRepository} that stores aggregates in memory
 * using a Caffeine {@link Cache}.
 * <p>
 * This repository acts as a high-performance, volatile storage system. Data stored here
 * is held in the JVM heap and will be lost if the application restarts, unless a
 * {@link RemovalListener} is configured to persist data upon eviction, or this repository
 * is used as a caching layer in front of a persistent repository.
 * <p>
 * Key features depend on the injected {@link Cache} configuration, but typically include:
 * <ul>
 * <li><b>In-Memory:</b> Extremely fast read/write operations (microsecond latency).</li>
 * <li><b>Eviction Policies:</b> Support for size-based, time-based, or reference-based eviction.</li>
 * <li><b>Concurrency:</b> Thread-safe operations suitable for high-throughput environments.</li>
 * </ul>
 *
 * @param <T> The type of the aggregate root stored in this repository.
 * @see Caffeine
 * @see Cache
 */
public class CaffeineAggregateRootRepository<T extends AggregateRoot> implements AggregateRootRepository<T> {
  private final Cache<String, T> cache;

  /**
   * Constructs a new repository backed by the provided Caffeine cache instance.
   * <p>
   * The behavior of this repository (expiration, size limits, stats recording) is entirely
   * determined by the configuration of the injected {@code cache}.
   *
   * @param cache The underlying Caffeine cache instance where aggregates are stored.
   */
  protected CaffeineAggregateRootRepository(
    final @NotNull Cache<String, T> cache
  ) {
    this.cache = cache;
  }

  @Contract(value = "_ -> new")
  public static <T extends AggregateRoot> @NotNull CaffeineAggregateRootRepository<T> create(final @NotNull Cache<String, T> cache) {
    return new CaffeineAggregateRootRepository<>(cache);
  }

  /**
   * Exposes the underlying Caffeine {@link Cache} instance.
   * <p>
   * This is useful for advanced operations not covered by the repository interface,
   * such as retrieving cache statistics via {@code cache.stats()}, explicit invalidation,
   * or accessing the map view directly for atomic computations.
   *
   * @return The underlying Caffeine cache.
   */
  public @NotNull Cache<String, T> cache() {
    return this.cache;
  }

  /**
   * {@inheritDoc}
   *
   * @implNote This operation delegates to the underlying cache map's remove method.
   * It returns {@code true} if the entry existed and was removed, or {@code false} otherwise.
   */
  @Override
  public boolean deleteSync(final @NotNull String id) {
    return this.deleteAndRetrieveSync(id) != null;
  }

  /**
   * {@inheritDoc}
   *
   * @implNote This clears all entries from the internal cache using {@link Cache#invalidateAll()}.
   *
   */
  @Override
  public void deleteAllSync() {
    this.cache.invalidateAll();
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Removes the entry from the cache map and returns the value that was associated with it,
   * or {@code null} if no such mapping existed.
   */
  @Override
  public @Nullable T deleteAndRetrieveSync(final @NotNull String id) {
    return this.cache.asMap().remove(id);
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Checks for the presence of the key in the cache map (O(1) operation).
   * Note that if the entry has expired but hasn't been evicted yet by the maintenance cycle,
   * this might still return false depending on Caffeine's internal state.
   */
  @Override
  public boolean existsSync(final @NotNull String id) {
    return this.cache.asMap().containsKey(id);
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Uses {@link Cache#getIfPresent(Object)} to retrieve the value.
   * Does not trigger a load or computation if the value is missing; strictly returns what is currently in memory.
   */
  @Override
  public @Nullable T findSync(final @NotNull String id) {
    return this.cache.getIfPresent(id);
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Iterates over the {@code values()} view of the underlying concurrent map.
   * <p>
   * <b>Consistency Note:</b> The returned collection is a weakly consistent snapshot. Modifications
   * to the cache that occur during iteration may or may not be reflected in the result.
   */
  @Override
  public @NonNull <C extends Collection<@NotNull T>> C findAllSync(final @NotNull Consumer<T> postLoadAction, final @NotNull IntFunction<C> factory) {
    final Collection<T> values = this.cache.asMap().values();
    final C result = factory.apply(values.size());
    for (final T value : values) {
      postLoadAction.accept(value);
      result.add(value);
    }
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Returns a snapshot of the keys currently in the cache.
   * Like {@link #findAllSync}, this view is weakly consistent.
   */
  @Override
  public @NonNull <C extends Collection<@NotNull String>> C findIdsSync(final @NotNull IntFunction<C> factory) {
    final Collection<String> keys = this.cache.asMap().keySet();
    final C foundIds = factory.apply(keys.size());
    foundIds.addAll(keys);
    return foundIds;
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Inserts or updates the aggregate in the cache using {@link Cache#put(Object, Object)}.
   * This operation overwrites any previous value associated with the ID immediately (Last-Write-Wins).
   */
  @Override
  public @NonNull T saveSync(@NonNull final T aggregateRoot) {
    this.cache.put(aggregateRoot.id(), aggregateRoot);
    return aggregateRoot;
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Returns a weakly consistent iterator over the elements in the cache.
   * Updates to the cache map while iterating may or may not be reflected in the iterator.
   */
  @Override
  public @NotNull Iterator<T> iterator() {
    return this.cache.asMap()
      .values()
      .iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Returns a weakly consistent iterator over the keys in the cache.
   */
  @Override
  public @NotNull Iterator<@NotNull String> iteratorIds() {
    return this.cache.asMap()
      .keySet()
      .iterator();
  }
}
