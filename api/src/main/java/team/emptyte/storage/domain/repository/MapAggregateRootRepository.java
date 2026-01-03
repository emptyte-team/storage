package team.emptyte.storage.domain.repository;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.emptyte.storage.domain.AggregateRoot;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * An in-memory implementation of {@link AggregateRootRepository} backed by a {@link Map}.
 * <p>
 * This class serves as an <b>Infrastructure Adapter</b> within the Hexagonal Architecture.
 * It is highly recommended for:
 * <ul>
 * <li><b>Unit Testing:</b> Validating domain logic without the overhead of a real database or complex mocks.</li>
 * <li><b>Prototyping:</b> Rapid development phases before selecting a final persistence engine.</li>
 * <li><b>Local Caching:</b> High-speed temporary storage.</li>
 * </ul>
 * <p>
 * <b>Note:</b> While the project follows an "Async-first" philosophy, this implementation is
 * inherently synchronous as in-memory operations are immediate.
 *
 * @param <T> The type of AggregateRoot managed by this repository.
 */
public class MapAggregateRootRepository<T extends AggregateRoot> implements AggregateRootRepository<T> {
  private final Map<String, T> cache;

  /**
   * Protected constructor to allow inheritance or controlled instantiation.
   *
   * @param cache The underlying map to be used as storage.
   */
  protected MapAggregateRootRepository(final @NotNull Map<String, T> cache) {
    this.cache = cache;
  }

  /**
   * Creates a repository backed by a standard {@link HashMap}.
   * <p>
   * <b>Warning:</b> This implementation is <b>NOT thread-safe</b>.
   * It should only be used in single-threaded contexts (like simple unit tests)
   * or where external synchronization is guaranteed.
   *
   * @param <T> The aggregate type.
   * @return A new repository instance.
   */
  @Contract(" -> new")
  public static <T extends AggregateRoot> @NotNull MapAggregateRootRepository<T> hashMap() {
    return MapAggregateRootRepository.create(new HashMap<>());
  }

  /**
   * Creates a repository backed by a {@link ConcurrentHashMap}.
   * <p>
   * This implementation <b>IS thread-safe</b> and suitable for production environments
   * or multi-threaded testing where concurrent access to the storage is expected.
   *
   * @param <T> The aggregate type.
   * @return A new concurrent repository instance.
   */
  @Contract(" -> new")
  public static <T extends AggregateRoot> @NotNull MapAggregateRootRepository<T> concurrentHashMap() {
    return MapAggregateRootRepository.create(new ConcurrentHashMap<>());
  }

  /**
   * Creates a repository with a custom backing map.
   *
   * @param cache The specific map implementation to use (e.g., WeakHashMap, TreeMap).
   * @param <T>   The aggregate type.
   * @return A new repository instance.
   */
  @Contract("_ -> new")
  public static <T extends AggregateRoot> @NotNull MapAggregateRootRepository<T> create(final @NotNull Map<String, T> cache) {
    return new MapAggregateRootRepository<>(cache);
  }

  /**
   * Exposes the underlying backing map.
   * Useful for debugging, inspection during tests, or advanced map operations not covered by the repository interface.
   *
   * @return The direct storage map.
   */
  public @NotNull Map<String, T> cache() {
    return this.cache;
  }

  @Override
  public boolean deleteSync(final @NotNull String id) {
    return this.cache.remove(id) != null;
  }

  @Override
  public void deleteAllSync() {
    this.cache.clear();
  }

  @Override
  public @Nullable T deleteAndRetrieveSync(final @NotNull String id) {
    return this.cache.remove(id);
  }

  @Override
  public boolean existsSync(final @NotNull String id) {
    return this.cache.containsKey(id);
  }

  @Override
  public @Nullable T findSync(final @NotNull String id) {
    return this.cache.get(id);
  }

  /**
   * Synchronously retrieves all stored aggregates.
   *
   * @param postLoadAction An action to execute for each loaded element (e.g., for late dependency injection).
   * @param factory        A function to create the return collection with the exact required size.
   * This optimizes memory usage by avoiding internal array resizing.
   * @param <C>            The type of the returned collection.
   * @return A collection containing all stored aggregates.
   */
  @Override
  public @NotNull <C extends Collection<@NotNull T>> C findAllSync(final @NotNull Consumer<T> postLoadAction, final @NotNull IntFunction<C> factory) {
    final Collection<T> values = this.cache.values();
    if (values.isEmpty()) {
      return factory.apply(0);
    }
    // Optimization: Instantiate the collection with the exact size of the map
    final var collection = factory.apply(values.size());
    for (final T value : values) {
      postLoadAction.accept(value);
      collection.add(value);
    }
    return collection;
  }

  /**
   * Synchronously retrieves all stored IDs.
   *
   * @param factory A function to create the return collection with the optimal size.
   * @param <C>     The type of the returned collection.
   * @return A collection containing all the keys (IDs) in the map.
   */
  @Override
  public @NotNull <C extends Collection<@NotNull String>> C findIdsSync(final @NotNull IntFunction<C> factory) {
    final Collection<String> ids = this.cache.keySet();
    if (ids.isEmpty()) {
      return factory.apply(0);
    }
    final var collection = factory.apply(ids.size());
    collection.addAll(ids);
    return collection;
  }

  @Override
  public @NotNull T saveSync(@NotNull final T aggregateRoot) {
    this.cache.put(aggregateRoot.id(), aggregateRoot);
    return aggregateRoot;
  }

  @Override
  public @NotNull Iterator<T> iterator() {
    return this.cache.values().iterator();
  }

  @Override
  public @NotNull Iterator<@NotNull String> iteratorIds() {
    return this.cache.keySet().iterator();
  }
}
