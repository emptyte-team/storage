package team.emptyte.storage.domain.repository;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.emptyte.storage.domain.AggregateRoot;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * Abstract base implementation of an Asynchronous Repository using the <b>Decorator/Wrapper Pattern</b>.
 * <p>
 * This class wraps synchronous blocking operations defined in {@link AggregateRootRepository}
 * and offloads them to a specified {@link Executor}. This allows the main thread (e.g., the UI thread
 * or a server event loop) to remain non-blocking while I/O operations occur in the background.
 * </p>
 * <p>
 * <strong>Usage Note:</strong><br>
 * Concrete implementations must implement the synchronous methods from {@link AggregateRootRepository}.
 * This class automatically provides the asynchronous counterparts by wrapping calls in {@link CompletableFuture}.
 * </p>
 *
 * @param <T> The type of the Aggregate Root.
 * @author team.emptyte
 * @since 0.1.0
 * @see java.util.concurrent.CompletableFuture
 */
public abstract class AsyncAggregateRootRepository<T extends AggregateRoot> implements AggregateRootRepository<T> {

  /**
   * The executor service responsible for running the blocking synchronous operations.
   */
  protected final Executor executor;

  /**
   * Creates an asynchronous repository using a specific {@link Executor}.
   * <p>
   * This is the recommended constructor for production environments, allowing you to pass
   * a shared thread pool (e.g., {@code ForkJoinPool} or a cached thread pool) to manage resources efficiently.
   * </p>
   *
   * @param executor The executor to use for background tasks. Must not be {@code null}.
   */
  protected AsyncAggregateRootRepository(final @NotNull Executor executor) {
    this.executor = executor;
  }

  /**
   * Creates an asynchronous repository using a default single-thread executor.
   * <p>
   * <b>Warning:</b> This constructor creates a new {@link Executors#newSingleThreadExecutor()}
   * for <i>each</i> instance of the repository. In a production environment with many repositories,
   * this could lead to excessive thread creation. Use only for testing or simple prototypes.
   * </p>
   */
  protected AsyncAggregateRootRepository() {
    this(Executors.newSingleThreadExecutor());
  }

  /**
   * Exposes the underlying executor used by this repository.
   *
   * @return The {@link Executor} instance.
   */
  public @NotNull Executor executor() {
    return this.executor;
  }

  /**
   * Asynchronously removes an aggregate by its identifier.
   *
   * @param id The unique identifier.
   * @return A {@link CompletableFuture} that completes with {@code true} if removed, {@code false} otherwise.
   */
  public @NotNull CompletableFuture<@NotNull Boolean> deleteAsync(final @NotNull String id) {
    return CompletableFuture.supplyAsync(() -> this.deleteSync(id), this.executor);
  }

  /**
   * Asynchronously removes all aggregates.
   *
   * @return A {@link CompletableFuture} that completes when the operation is finished.
   */
  public @NotNull CompletableFuture<@NotNull Void> deleteAllAsync() {
    return CompletableFuture.runAsync(this::deleteAllSync, this.executor);
  }

  /**
   * Asynchronously retrieves and removes an aggregate.
   *
   * @param id The unique identifier.
   * @return A {@link CompletableFuture} containing the removed aggregate, or {@code null} if not found.
   */
  public @NotNull CompletableFuture<@Nullable T> deleteAndRetrieveAsync(final @NotNull String id) {
    return CompletableFuture.supplyAsync(() -> this.deleteAndRetrieveSync(id), this.executor);
  }

  /**
   * Asynchronously checks if an aggregate exists.
   *
   * @param id The unique identifier.
   * @return A {@link CompletableFuture} completing with {@code true} if found.
   */
  public @NotNull CompletableFuture<@NotNull Boolean> existsAsync(final @NotNull String id) {
    return CompletableFuture.supplyAsync(() -> this.existsSync(id), this.executor);
  }

  /**
   * Asynchronously finds an aggregate by its identifier.
   *
   * @param id The unique identifier.
   * @return A {@link CompletableFuture} containing the found aggregate, or {@code null}.
   */
  public @NotNull CompletableFuture<@Nullable T> findAsync(final @NotNull String id) {
    return CompletableFuture.supplyAsync(() -> this.findSync(id), this.executor);
  }

  /**
   * Asynchronously retrieves all aggregates using a collection factory.
   *
   * @param factory The factory to create the collection.
   * @param <C>     The type of Collection.
   * @return A {@link CompletableFuture} containing the collection of aggregates.
   */
  public <C extends Collection<@NotNull T>> @NotNull CompletableFuture<C> findAllAsync(final @NotNull IntFunction<@NotNull C> factory) {
    return CompletableFuture.supplyAsync(() -> this.findAllSync(factory), this.executor);
  }

  /**
   * Asynchronously retrieves all aggregates, executes a post-load action, and collects them.
   *
   * @param postLoadAction The action to execute on each loaded entity.
   * @param factory        The factory to create the collection.
   * @param <C>            The type of Collection.
   * @return A {@link CompletableFuture} containing the collection of aggregates.
   */
  public <C extends Collection<@NotNull T>> @NotNull CompletableFuture<C> findAllAsync(final @NotNull Consumer<@NotNull T> postLoadAction, final @NotNull IntFunction<@NotNull C> factory) {
    return CompletableFuture.supplyAsync(() -> this.findAllSync(postLoadAction, factory), this.executor);
  }

  /**
   * Asynchronously retrieves all aggregate identifiers.
   *
   * @return A {@link CompletableFuture} containing the collection of IDs.
   */
  public @NotNull CompletableFuture<@Nullable Collection<@NotNull String>> findIdsAsync() {
    return CompletableFuture.supplyAsync(this::findIdsSync, this.executor);
  }

  /**
   * Asynchronously retrieves all aggregate identifiers and collects them into a provided collection.
   * <p>
   * This overload allows controlling the specific type of collection returned (e.g., Set vs List).
   * </p>
   *
   * @param factory The factory to create the collection (e.g., {@code ArrayList::new}).
   * @param <C>     The specific type of the Collection containing Strings.
   * @return A {@link CompletableFuture} containing the collection of IDs.
   */
  public <C extends Collection<@NotNull String>> @NotNull CompletableFuture<@Nullable C> findIdsAsync(final @NotNull IntFunction<@NotNull C> factory) {
    return CompletableFuture.supplyAsync(() -> this.findIdsSync(factory), this.executor);
  }

  /**
   * Asynchronously persists or updates an aggregate.
   *
   * @param aggregateRoot The entity to save.
   * @return A {@link CompletableFuture} containing the saved entity instance.
   */
  public @NotNull CompletableFuture<@NotNull T> saveAsync(final @NotNull T aggregateRoot) {
    return CompletableFuture.supplyAsync(() -> this.saveSync(aggregateRoot), this.executor);
  }
}
