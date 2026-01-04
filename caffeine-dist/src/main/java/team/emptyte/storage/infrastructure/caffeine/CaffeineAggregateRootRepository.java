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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import team.emptyte.storage.domain.AggregateRoot;
import team.emptyte.storage.domain.repository.AsyncAggregateRootRepository;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * An implementation of {@link AsyncAggregateRootRepository} that stores aggregates in memory
 * using a Caffeine {@link Cache}.
 * <p>
 * This repository acts as a high-performance, volatile storage system. Data stored here
 * is held in the JVM heap and will be lost if the application restarts, unless a
 * {@link RemovalListener} is configured to persist data upon eviction, or this repository
 * is used as a caching layer in front of a persistent repository.
 * <p>
 * Key features of this implementation:
 * <ul>
 * <li><b>In-Memory:</b> Extremely fast read/write operations.</li>
 * <li><b>Eviction Policies:</b> Supports size-based and time-based eviction.</li>
 * <li><b>Memory Safety:</b> Optional support for {@code softValues} to allow garbage collection of entries when memory is low.</li>
 * </ul>
 *
 * @param <T> The type of the aggregate root stored in this repository.
 * @see Caffeine
 * @see Cache
 */
public class CaffeineAggregateRootRepository<T extends AggregateRoot> extends AsyncAggregateRootRepository<T> {
  private final Cache<String, T> cache;

  /**
   * Constructs a new repository with a specific executor for asynchronous operations.
   *
   * @param executor The executor to be used by Caffeine for async tasks (e.g., eviction maintenance).
   */
  protected CaffeineAggregateRootRepository(
    final @NotNull Executor executor,
    final @NotNull Cache<String, T> cache
  ) {
    super(executor);
    this.cache = cache;
  }

  /**
   * Constructs a new repository using the default executor.
   *
   * @param maximumSize      The maximum number of entries the cache can hold before evicting the oldest ones.
   * @param expireAfterWrite The duration after which an entry should be automatically removed after being created or replaced.
   * @param timeUnit         The unit of time for {@code expireAfterWrite}.
   * @param recordStats      Whether to enable the recording of cache statistics.
   * @param useSoftValues    If true, enables soft reference collection for values.
   * @param removalListener  An optional listener to be notified when an entry is removed.
   */
  protected CaffeineAggregateRootRepository(
    final @NotNull Cache<String, T> cache
  ) {
    super();
    this.cache = cache;
  }

  /**
   * Creates a builder to configure and instantiate a {@link CaffeineAggregateRootRepository}.
   *
   * @param <T> The type of aggregate root.
   * @return A new builder instance.
   */
  public static <T extends AggregateRoot> @NotNull CaffeineAggregateRootRepositoryBuilder<T> builder() {
    return new CaffeineAggregateRootRepositoryBuilder<>();
  }

  /**
   * Exposes the underlying Caffeine {@link Cache} instance.
   * <p>
   * This is useful for advanced operations not covered by the repository interface,
   * such as retrieving cache statistics (`cache.stats()`), explicit invalidation,
   * or accessing the map view directly.
   *
   * @return The underlying Caffeine cache.
   */
  public @NotNull Cache<String, T> cache() {
    return this.cache;
  }

  /**
   * {@inheritDoc}
   *
   * @implNote This operation is performed directly on the underlying cache map.
   * It returns {@code true} if the entry existed and was removed, or {@code false} otherwise.
   */
  @Override
  public boolean deleteSync(final @NotNull String id) {
    return this.cache.asMap().remove(id) != null;
  }

  /**
   * {@inheritDoc}
   *
   * @implNote This clears all entries from the internal cache using {@link Cache#invalidateAll()}.
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
   * Note that if the entry has expired but hasn't been evicted yet, this might still return false
   * depending on Caffeine's cleanup cycle.
   */
  @Override
  public boolean existsSync(final @NotNull String id) {
    return this.cache.asMap().containsKey(id);
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Uses {@link Cache#getIfPresent(Object)} to retrieve the value.
   * Does not trigger a load or computation if the value is missing.
   */
  @Override
  public @Nullable T findSync(final @NotNull String id) {
    return this.cache.getIfPresent(id);
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Iterates over the {@code values()} view of the underlying concurrent map.
   * This operation is thread-safe but weakly consistent; it reflects the state of the cache
   * at the time of iteration.
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
   * This operation overwrites any previous value associated with the ID.
   */
  @Override
  public @NonNull T saveSync(@NonNull final T aggregateRoot) {
    this.cache.put(aggregateRoot.id(), aggregateRoot);
    return aggregateRoot;
  }

  @Override
  public @NotNull Iterator<T> iterator() {
    return this.cache.asMap()
      .values()
      .iterator();
  }

  @Override
  public @NotNull Iterator<@NotNull String> iteratorIds() {
    return this.cache.asMap()
      .keySet()
      .iterator();
  }
}
