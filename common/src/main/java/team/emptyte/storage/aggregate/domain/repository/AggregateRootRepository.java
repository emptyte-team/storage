package team.emptyte.storage.aggregate.domain.repository;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.emptyte.storage.aggregate.domain.AggregateRoot;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * Interface representing a repository for managing the persistence and retrieval of
 * {@link AggregateRoot} entities.
 * <p>
 * In Domain-Driven Design, a Repository mediates between the domain and data mapping layers,
 * acting like an in-memory collection of domain objects.
 * </p>
 * <p>
 * <strong>Key Features:</strong>
 * <ul>
 * <li><b>Synchronous Operations:</b> Methods suffixed with 'Sync' imply blocking I/O operations.</li>
 * <li><b>Collection Factories:</b> Retrieval methods accept a factory to allow the caller to determine the specific implementation of the returned {@link Collection} (e.g., List, Set).</li>
 * <li><b>ID-only access:</b> Specific methods allow iterating or retrieving only identifiers to minimize memory overhead when the full object is not needed.</li>
 * </ul>
 * </p>
 *
 * @param <T> The type of the Aggregate Root this repository manages.
 * @author team.emptyte
 * @since 0.0.1
 */
public interface AggregateRootRepository<T extends AggregateRoot> extends Iterable<T> {

  /**
   * Synchronously removes an aggregate from the storage by its identifier.
   *
   * @param id The unique identifier of the aggregate to remove.
   * @return {@code true} if the aggregate existed and was removed; {@code false} otherwise.
   */
  boolean deleteSync(final @NotNull String id);

  /**
   * Removes all aggregates managed by this repository.
   * <p>
   * <b>Warning:</b> This is a destructive operation that clears the entire storage for this type.
   * </p>
   */
  void deleteAll();

  /**
   * Atomically retrieves and removes an aggregate from the storage.
   * <p>
   * This is useful for processing queues or one-time-use tokens where the object
   * must be consumed and deleted simultaneously.
   * </p>
   *
   * @param id The unique identifier of the aggregate.
   * @return The removed aggregate, or {@code null} if it did not exist.
   */
  @Nullable T deleteAndRetrieve(final @NotNull String id);

  /**
   * Checks if an aggregate exists in the storage without loading its full state.
   *
   * @param id The unique identifier to check.
   * @return {@code true} if the aggregate exists; {@code false} otherwise.
   */
  boolean exists(final @NotNull String id);

  /**
   * Synchronously retrieves an aggregate by its identifier.
   *
   * @param id The unique identifier of the aggregate.
   * @return The found aggregate, or {@code null} if no match is found.
   */
  @Nullable T findSync(final @NotNull String id);

  /**
   * Retrieves all aggregates and collects them into a collection provided by the factory.
   * <p>
   * This is a convenience method that delegates to {@link #findAll(Consumer, IntFunction)}
   * with an empty post-load action.
   * </p>
   *
   * @param factory A function that accepts the expected size and creates the target Collection (e.g., {@code ArrayList::new}).
   * @param <C>     The type of the Collection.
   * @return A collection containing all aggregates, or {@code null} if the operation fails internally (depending on impl).
   */
  default <C extends Collection<@NotNull T>> @Nullable C findAll(final @NotNull IntFunction<@NotNull C> factory) {
    return this.findAll(modelType -> {}, factory);
  }

  /**
   * Retrieves all aggregates, executes a specific action on each, and collects them.
   *
   * @param postLoadAction A consumer to be executed on each aggregate immediately after loading (e.g., for dependency injection or logging).
   * @param factory        A function that accepts the expected size and creates the target Collection.
   * @param <C>            The type of the Collection.
   * @return A collection containing all loaded aggregates.
   */
  <C extends Collection<@NotNull T>> @NotNull C findAll(final @NotNull Consumer<@NotNull T> postLoadAction, final @NotNull IntFunction<@NotNull C> factory);

  /**
   * Retrieves all unique identifiers (IDs) currently stored in the repository.
   * <p>
   * This method is optimized for performance as it avoids deserializing the full aggregate objects.
   * </p>
   *
   * @return A collection of ID strings, or {@code null} if the storage is empty or fails.
   */
  @Nullable Collection<@NotNull String> findIds();

  /**
   * Retrieves all unique identifiers and collects them into a collection provided by the factory.
   *
   * @param factory A function that accepts the expected size and creates the target Collection.
   * @param <C>     The type of the Collection.
   * @return A collection of ID strings.
   */
  <C extends Collection<@NotNull String>> @Nullable C findIds(final @NotNull IntFunction<@NotNull C> factory);

  /**
   * Synchronously persists or updates an aggregate in the storage.
   *
   * @param aggregateRoot The aggregate root to save.
   * @return The same instance that was passed, allowing for fluent chaining.
   */
  @Contract("_ -> param1")
  @NotNull T saveSync(final @NotNull T aggregateRoot);

  /**
   * Returns an iterator over all aggregates in the repository.
   * <p>
   * <b>Note:</b> Depending on the implementation, this might load all objects into memory
   * or stream them lazily.
   * </p>
   *
   * @return An iterator for type {@code T}.
   */
  @Override
  @NotNull Iterator<@NotNull T> iterator();

  /**
   * Returns an iterator over the identifiers (IDs) only.
   * <p>
   * This is generally lighter on memory than {@link #iterator()} as it does not require
   * deserializing the full objects.
   * </p>
   *
   * @return An iterator for the ID strings.
   */
  @NotNull Iterator<@NotNull String> iteratorIds();

  /**
   * Performs the given action for each element in the repository.
   *
   * @param action The action to be performed for each element.
   */
  @Override
  default void forEach(Consumer<? super T> action) {
    for (final T aggregateRoot : this) {
      action.accept(aggregateRoot);
    }
  }

  /**
   * Performs the given action for each identifier (ID) in the repository.
   * <p>
   * Useful for bulk operations where only the ID is needed (e.g., checking existence or linking).
   * </p>
   *
   * @param action The action to be performed for each ID.
   */
  default void forEachIds(final @NotNull Consumer<@NotNull String> action) {
    final Iterator<String> iterator = this.iteratorIds();
    while (iterator.hasNext()) {
      action.accept(iterator.next());
    }
  }
}
