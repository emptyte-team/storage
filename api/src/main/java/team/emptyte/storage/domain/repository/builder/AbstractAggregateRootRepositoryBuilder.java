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
package team.emptyte.storage.domain.repository.builder;

import team.emptyte.storage.domain.AggregateRoot;
import team.emptyte.storage.domain.repository.AggregateRootRepository;
import team.emptyte.storage.domain.repository.AsyncAggregateRootRepository;
import team.emptyte.storage.domain.repository.WithFallbackAggregateRootRepository;

import java.util.concurrent.Executor;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A skeletal builder implementation that standardizes the creation of {@link AsyncAggregateRootRepository}.
 * <p>
 * This class provides a fluent API to:
 * <ol>
 * <li>Construct a specific repository implementation (defined by subclasses).</li>
 * <li><b>Compose</b> that repository with a fallback mechanism (caching) seamlessly.</li>
 * </ol>
 * <p>
 * By extending this class, concrete builders (like {@code GsonRepositoryBuilder} or {@code SqlRepositoryBuilder})
 * automatically gain the ability to create cached repositories without writing extra logic.
 *
 * @param <T> The type of AggregateRoot.
 */
@SuppressWarnings("unused")
public abstract class AbstractAggregateRootRepositoryBuilder<T extends AggregateRoot> {
  /**
   * Constructs the concrete implementation of the <b>Main Repository</b> (Source of Truth).
   * <p>
   * Subclasses must implement this method to instantiate their specific storage engine
   * (e.g., File System, Database, Network) using the provided {@link Executor} for
   * asynchronous operations.
   *
   * @param executor The executor to be used for asynchronous operations.
   * @return A new instance of the specific repository.
   */
  @Contract("_ -> new")
  public abstract @NotNull AsyncAggregateRootRepository<@NotNull T> build(final @NotNull Executor executor);

  /**
   * Constructs a <b>Hybrid Repository</b> that wraps the Main Repository with a Fallback (Cache).
   * <p>
   * This method applies the <b>Decorator Pattern</b>. It creates the main repository using {@link #build(Executor)}
   * and then wraps it inside a {@link WithFallbackAggregateRootRepository}.
   *
   * @param executor                        The executor for async tasks.
   * @param fallbackAggregateRootRepository The fast/volatile repository to use as a cache (e.g., In-Memory Map).
   * @return A repository that automatically coordinates between the Cache and the Main storage.
   */
  @Contract("_, _ -> new")
  public @NotNull WithFallbackAggregateRootRepository<T> buildWithFallback(
    final @NotNull Executor executor,
    final @NotNull AggregateRootRepository<T> fallbackAggregateRootRepository
  ) {
    return new WithFallbackAggregateRootRepository<>(
      executor,
      fallbackAggregateRootRepository, // The Cache
      this.build(executor)             // The Database (created by the subclass)
    );
  }
}
