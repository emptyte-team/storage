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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import team.emptyte.storage.domain.AggregateRoot;
import team.emptyte.storage.domain.repository.AsyncAggregateRootRepository;
import team.emptyte.storage.domain.repository.builder.AbstractAggregateRootRepositoryBuilder;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * A builder for creating instances of {@link CaffeineAggregateRootRepository}.
 * <p>
 * This builder provides a fluent API to configure the underlying Caffeine cache properties,
 * such as eviction policies (size-based, time-based), memory reference types, and
 * event listeners.
 *
 * @param <T> The type of the aggregate root.
 * @see Caffeine
 */
public class CaffeineAggregateRootRepositoryBuilder<T extends AggregateRoot> extends AbstractAggregateRootRepositoryBuilder<T> {
  private Executor executor;
  private long maximumSize;
  private long expireAfterWrite;
  private TimeUnit timeUnit;
  private boolean recordStats;
  private boolean useSoftValues;
  private RemovalListener<String, T> removalListener;

  /**
   * Internal constructor. Use {@link CaffeineAggregateRootRepository#builder()} to get an instance.
   */
  CaffeineAggregateRootRepositoryBuilder() {

  }

  /**
   * Sets the executor to be used by Caffeine for asynchronous maintenance tasks.
   * <p>
   * If not configured, Caffeine uses {@link ForkJoinPool#commonPool()} by default.
   * Note that this executor is distinct from the one passed to the {@link #build(Executor)}
   * method, although the same instance can be used for both.
   *
   * @param executor The executor for cache maintenance (eviction, notifications, etc.).
   * @return This builder instance.
   */
  @Contract("_ -> this")
  public @NotNull CaffeineAggregateRootRepositoryBuilder<T> executor(final @NotNull Executor executor) {
    this.executor = executor;
    return this;
  }

  /**
   * Specifies the maximum number of entries the cache may contain.
   * <p>
   * When the cache size grows close to the maximum, the cache will evict entries
   * that are less likely to be used again.
   *
   * @param maximumSize The maximum size of the cache.
   * @return This builder instance.
   */
  @Contract("_ -> this")
  public @NotNull CaffeineAggregateRootRepositoryBuilder<T> maximumSize(final long maximumSize) {
    this.maximumSize = maximumSize;
    return this;
  }

  /**
   * Specifies the duration after which an entry should be automatically removed
   * from the cache once it has been created or the value replaced.
   *
   * @param expireAfterWrite The length of time after an entry is created that it should be automatically removed.
   * @return This builder instance.
   * @see #timeUnit(TimeUnit)
   */
  @Contract("_ -> this")
  public @NotNull CaffeineAggregateRootRepositoryBuilder<T> expireAfterWrite(final long expireAfterWrite) {
    this.expireAfterWrite = expireAfterWrite;
    return this;
  }

  /**
   * Specifies the time unit for the {@link #expireAfterWrite(long)} configuration.
   *
   * @param timeUnit The unit of time.
   * @return This builder instance.
   */
  @Contract("_ -> this")
  public @NotNull CaffeineAggregateRootRepositoryBuilder<T> timeUnit(final @NotNull TimeUnit timeUnit) {
    this.timeUnit = timeUnit;
    return this;
  }

  /**
   * Enables the accumulation of {@link com.github.benmanes.caffeine.cache.stats.CacheStats}
   * during the operation of the cache.
   * <p>
   * Useful for monitoring performance, hit rates, and eviction counts.
   *
   * @param recordStats True to enable statistics recording.
   * @return This builder instance.
   */
  @Contract("_ -> this")
  public @NotNull CaffeineAggregateRootRepositoryBuilder<T> recordStats(final boolean recordStats) {
    this.recordStats = recordStats;
    return this;
  }

  /**
   * Specifies that values stored in the cache should be wrapped in {@link java.lang.ref.SoftReference}s.
   * <p>
   * Softly-referenced objects are garbage-collected in a globally least-recently-used manner,
   * in response to memory demand. This helps avoid {@link OutOfMemoryError} at the cost
   * of potentially earlier eviction.
   *
   * @param useSoftValues True to enable soft references for values.
   * @return This builder instance.
   */
  @Contract("_ -> this")
  public @NotNull CaffeineAggregateRootRepositoryBuilder<T> useSoftValues(final boolean useSoftValues) {
    this.useSoftValues = useSoftValues;
    return this;
  }

  /**
   * Specifies a listener that is notified each time an entry is removed from the cache.
   * <p>
   * This can be used to perform cleanup operations or to persist data to a permanent
   * storage when it is evicted from memory.
   *
   * @param removalListener The listener to receive removal notifications.
   * @return This builder instance.
   */
  @Contract("_ -> this")
  public @NotNull CaffeineAggregateRootRepositoryBuilder<T> removalListener(final @NotNull RemovalListener<String, T> removalListener) {
    this.removalListener = removalListener;
    return this;
  }

  /**
   * Builds the {@link CaffeineAggregateRootRepository} using the configured settings.
   *
   * @param executor The executor to be used by the repository for its asynchronous operations.
   * @return A new instance of the repository.
   */
  @Override
  @Contract("_ -> new")
  public @NotNull AsyncAggregateRootRepository<T> build(final @NotNull Executor executor) {
    final var builder = Caffeine.newBuilder();

    // Configures Caffeine internal executor if specified
    if (this.executor != null) builder.executor(this.executor);

    if (this.maximumSize > 0) builder.maximumSize(this.maximumSize);
    if (this.expireAfterWrite > 0 && this.timeUnit != null) builder.expireAfterWrite(this.expireAfterWrite, this.timeUnit);
    if (this.recordStats) builder.recordStats();
    if (this.useSoftValues) builder.softValues();
    if (this.removalListener != null) builder.removalListener(this.removalListener);

    // Note: This assumes CaffeineAggregateRootRepository has a constructor (Executor, Cache)
    return new CaffeineAggregateRootRepository<>(executor, builder.build());
  }
}
