package team.emptyte.storage.domain.repository;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.emptyte.storage.domain.AggregateRoot;

/**
 * A hybrid repository implementation that orchestrates data flow between two distinct storage sources.
 * <p>
 * This class implements the <b>Decorator Pattern</b> to combine a fast, volatile storage (Fallback)
 * with a slow, persistent storage (Main).
 * <p>
 * <h3>Architecture Definitions:</h3>
 * <ul>
 * <li><b>Main Repository:</b> The "Source of Truth". Usually a persistent database (MySQL, Mongo, file system).
 * Operations here are assumed to be slower but durable.</li>
 * <li><b>Fallback Repository:</b> The "Cache" or "Buffer". Usually in-memory (Map) or low-latency (Redis).
 * Operations here are fast but potentially volatile.</li>
 * </ul>
 * <p>
 * <h3>Supported Caching Strategies:</h3>
 * This class allows you to implement various patterns depending on which methods you call:
 * <ul>
 * <li><b>Read-Through / Cache-Aside:</b> Use {@link #findInBothAndSaveToFallbackSync(String)}.
 * (Try Cache -> If missed, try DB -> Populate Cache).</li>
 * <li><b>Write-Through:</b> Use {@link #saveInBothSync(AggregateRoot)}.
 * (Write to Cache AND DB immediately).</li>
 * <li><b>Write-Behind (Batching):</b> Use {@link #saveInFallbackSync(AggregateRoot)} for high speed,
 * then periodically call {@link #uploadAllSync(Consumer)} to flush to DB.</li>
 * <li><b>Cache Warming:</b> Use {@link #loadAllSync(Consumer, IntFunction)} to preload DB data into Cache on startup.</li>
 * </ul>
 * <p>
 * <b>Important Note on Default Behavior:</b> The standard methods overridden from {@link AggregateRootRepository}
 * (like {@code saveSync}, {@code findSync}) delegate <b>ONLY to the Main Repository</b> by default.
 * You must explicitly call the hybrid methods (e.g., {@code saveInBoth}) to involve the fallback.
 *
 * @param <T> The type of AggregateRoot managed by this repository.
 */
@SuppressWarnings("unused")
public class WithFallbackAggregateRootRepository<T extends AggregateRoot> extends AsyncAggregateRootRepository<T> {
  protected final AggregateRootRepository<T> fallbackRepository;
  protected final AggregateRootRepository<T> mainRepository;

  /**
   * Constructs a new hybrid repository.
   *
   * @param executor           The executor service used to offload async operations.
   * @param fallbackRepository The fast/volatile repository (e.g., In-Memory).
   * @param mainRepository     The slow/persistent repository (e.g., SQL/JSON).
   */
  public WithFallbackAggregateRootRepository(
    final @NotNull Executor executor,
    final @NotNull AggregateRootRepository<T> fallbackRepository,
    final @NotNull AggregateRootRepository<T> mainRepository
  ) {
    super(executor);
    this.fallbackRepository = fallbackRepository;
    this.mainRepository = mainRepository;
  }

  /**
   * Deletes the aggregate from the <b>Main Repository only</b>.
   * <p>
   * To delete from both layers (ensuring consistency), use {@link #deleteInBothSync(String)}.
   *
   * @param id The aggregate identifier.
   * @return True if deleted from main.
   */
  @Override
  public boolean deleteSync(final @NotNull String id) {
    return this.mainRepository.deleteSync(id);
  }

  @Override
  public void deleteAllSync() {
    this.mainRepository.deleteAllSync();
  }

  @Override
  public @Nullable T deleteAndRetrieveSync(final @NotNull String id) {
    return this.mainRepository.deleteAndRetrieveSync(id);
  }

  @Override
  public boolean existsSync(final @NotNull String id) {
    return this.mainRepository.existsSync(id);
  }

  /**
   * Retrieves data from the <b>Main Repository only</b>, bypassing the cache.
   *
   * @param id The aggregate identifier.
   * @return The aggregate if found in the persistent storage.
   */
  @Override
  public @Nullable T findSync(final @NotNull String id) {
    return this.mainRepository.findSync(id);
  }

  @Override
  public <C extends Collection<@NotNull T>> @NotNull C findAllSync(final @NotNull Consumer<T> postLoadAction, final @NotNull IntFunction<@NotNull C> factory) {
    return this.mainRepository.findAllSync(postLoadAction, factory);
  }

  @Override
  public @NotNull Collection<@NotNull String> findIdsSync() {
    return this.mainRepository.findIdsSync();
  }

  @Override
  public <C extends Collection<@NotNull String>> @NotNull C findIdsSync(final @NotNull IntFunction<@NotNull C> factory) {
    return this.mainRepository.findIdsSync(factory);
  }

  /**
   * Saves the aggregate to the <b>Main Repository only</b>.
   * <p>
   * <b>Warning:</b> This leaves the fallback/cache outdated. If you want to keep them in sync,
   * consider using {@link #saveInBothSync(AggregateRoot)}.
   *
   * @param aggregate The entity to save.
   * @return The saved entity.
   */
  @Override
  public @NotNull T saveSync(final @NotNull T aggregate) {
    return this.mainRepository.saveSync(aggregate);
  }

  @Override
  public @NotNull Iterator<T> iterator() {
    return this.mainRepository.iterator();
  }

  @Override
  public @NotNull Iterator<String> iteratorIds() {
    return this.mainRepository.iteratorIds();
  }

  public void deleteAllInFallbackSync() {
    this.fallbackRepository.deleteAllSync();
  }

  public @NotNull CompletableFuture<@NotNull Void> deleteAllInFallbackAsync() {
    return CompletableFuture.runAsync(this::deleteAllInFallbackSync, this.executor);
  }

  public @Nullable T deleteAndRetrieveInFallbackSync(final @NotNull String id) {
    return this.fallbackRepository.deleteAndRetrieveSync(id);
  }

  public @NotNull CompletableFuture<@Nullable T> deleteAndRetrieveInFallbackAsync(final @NotNull String id) {
    return CompletableFuture.supplyAsync(() -> this.deleteAndRetrieveInFallbackSync(id), this.executor);
  }

  public boolean deleteInFallbackSync(final @NotNull String id) {
    return this.fallbackRepository.deleteSync(id);
  }

  public @NotNull CompletableFuture<@NotNull Boolean> deleteInFallbackAsync(final @NotNull String id) {
    return CompletableFuture.supplyAsync(() -> this.deleteInFallbackSync(id), this.executor);
  }

  public boolean existsInFallbackSync(final @NotNull String id) {
    return this.fallbackRepository.existsSync(id);
  }

  public @NotNull CompletableFuture<@NotNull Boolean> existsInFallbackAsync(final @NotNull String id) {
    return CompletableFuture.supplyAsync(() -> this.existsInFallbackSync(id), this.executor);
  }

  public @Nullable Collection<@NotNull String> findAllIdsInFallbackSync() {
    return this.fallbackRepository.findIdsSync();
  }

  public @NotNull CompletableFuture<@Nullable Collection<@NotNull String>> findAllIdsInFallbackAsync() {
    return CompletableFuture.supplyAsync(this::findAllIdsInFallbackSync, this.executor);
  }

  public <C extends Collection<@NotNull T>> @Nullable C findAllInFallbackSync(final @NotNull IntFunction<@NotNull C> factory) {
    return this.fallbackRepository.findAllSync(factory);
  }

  public <C extends Collection<@NotNull T>> @NotNull CompletableFuture<@Nullable C> findAllInFallbackAsync(final @NotNull IntFunction<@NotNull C> factory) {
    return CompletableFuture.supplyAsync(() -> this.findAllInFallbackSync(factory), this.executor);
  }

  public <C extends Collection<@NotNull T>> @Nullable C findAllInFallbackSync(final @NotNull Consumer<@NotNull T> postLoadAction, final @NotNull IntFunction<@NotNull C> factory) {
    return this.fallbackRepository.findAllSync(postLoadAction, factory);
  }

  public <C extends Collection<@NotNull T>> @NotNull CompletableFuture<@Nullable C> findAllInFallbackAsync(final @NotNull Consumer<@NotNull T> postLoadAction, final @NotNull IntFunction<@NotNull C> factory) {
    return CompletableFuture.supplyAsync(() -> this.findAllInFallbackSync(postLoadAction, factory), this.executor);
  }

  public @Nullable T findInFallbackSync(final @NotNull String id) {
    return this.fallbackRepository.findSync(id);
  }

  public @NotNull CompletableFuture<@Nullable T> findInFallbackAsync(final @NotNull String id) {
    return CompletableFuture.supplyAsync(() -> this.findInFallbackSync(id), this.executor);
  }

  public void forEachIdsInFallbackSync(final @NotNull Consumer<@NotNull String> action) {
    this.fallbackRepository.forEachIds(action);
  }

  public @NotNull CompletableFuture<@NotNull Void> forEachIdsInFallbackAsync(final @NotNull Consumer<@NotNull String> action) {
    return CompletableFuture.runAsync(() -> this.forEachIdsInFallbackSync(action), this.executor);
  }

  public void forEachInFallbackSync(final @NotNull Consumer<@NotNull T> action) {
    this.fallbackRepository.forEach(action);
  }

  public @NotNull CompletableFuture<@NotNull Void> forEachInFallbackAsync(final @NotNull Consumer<@NotNull T> action) {
    return CompletableFuture.runAsync(() -> this.forEachInFallbackSync(action), this.executor);
  }

  public @NotNull Iterator<T> iteratorInFallback() {
    return this.fallbackRepository.iterator();
  }

  public @NotNull Iterator<String> iteratorIdsInFallback() {
    return this.fallbackRepository.iteratorIds();
  }

  /**
   * Saves data directly to the fallback repository.
   * <p>
   * Useful for temporary data or "Dirty Writes" that will be synced to the database later via {@link #uploadAllSync}.
   *
   * @param aggregate The entity to save.
   * @return The saved entity.
   */
  @Contract("_ -> param1")
  public @NotNull T saveInFallbackSync(final @NotNull T aggregate) {
    this.fallbackRepository.saveSync(aggregate);
    return aggregate;
  }

  public @NotNull CompletableFuture<@NotNull T> saveInFallbackAsync(final @NotNull T aggregate) {
    return CompletableFuture.supplyAsync(() -> this.saveInFallbackSync(aggregate), this.executor);
  }

  /**
   * Deletes the entity from both repositories.
   * <p>
   * This is an atomic-like operation in concept, but not transactionally atomic.
   * If the fallback deletion succeeds but the main deletion fails, the data may be lost in cache but remain in DB.
   *
   * @param id The identifier to delete.
   * @return {@code true} only if BOTH deletions were successful.
   */
  public boolean deleteInBothSync(final @NotNull String id) {
    return this.fallbackRepository.deleteSync(id) && this.mainRepository.deleteSync(id);
  }

  public @NotNull CompletableFuture<@NotNull Boolean> deleteInBothAsync(final @NotNull String id) {
    return CompletableFuture.supplyAsync(() -> this.deleteInBothSync(id), this.executor);
  }

  /**
   * Checks if the entity exists in EITHER the fallback OR the main repository.
   */
  public boolean existsInAnySync(final @NotNull String id) {
    return this.existsInFallbackSync(id) || this.mainRepository.existsSync(id);
  }

  public @NotNull CompletableFuture<@NotNull Boolean> existsInAnyAsync(final @NotNull String id) {
    return CompletableFuture.supplyAsync(() -> this.existsInAnySync(id), this.executor);
  }

  /**
   * Checks if the entity exists in BOTH the fallback AND the main repository.
   * This effectively checks if the cache is "in-sync" regarding the existence for this specific ID.
   */
  public boolean existsInBothSync(final @NotNull String id) {
    return this.existsInFallbackSync(id) && this.mainRepository.existsSync(id);
  }

  public @NotNull CompletableFuture<@NotNull Boolean> existsInBothAsync(final @NotNull String id) {
    return CompletableFuture.supplyAsync(() -> this.existsInBothSync(id), this.executor);
  }

  /**
   * <b>Read-Through Pattern (Half):</b>
   * Finds the entity in the Main repository. If found, it saves a copy to the Fallback repository.
   * <p>
   * This is useful to refresh the cache with the latest data from the database.
   *
   * @param id The ID to search for.
   * @return The found entity or null.
   */
  public @Nullable T findAndSaveToFallbackSync(final @NotNull String id) {
    final var aggregate = this.findSync(id);
    if (aggregate == null) {
      return null;
    }
    this.fallbackRepository.saveSync(aggregate);
    return aggregate;
  }

  public @NotNull CompletableFuture<@Nullable T> findAndSaveToFallbackAsync(final @NotNull String id) {
    return CompletableFuture.supplyAsync(() -> this.findAndSaveToFallbackSync(id), this.executor);
  }

  /**
   * <b>Basic Fallback Pattern:</b>
   * 1. Checks the Fallback (Cache).
   * 2. If missing, check the Main (DB).
   * <p>
   * Note: This does <b>NOT</b> populate the cache if data is found in DB.
   * For that, use {@link #findInBothAndSaveToFallbackSync(String)}.
   *
   * @param id The ID to search for.
   * @return The found entity.
   */
  public @Nullable T findInBothSync(final @NotNull String id) {
    final var aggregate = this.findInFallbackSync(id);
    if (aggregate != null) {
      return aggregate;
    }
    return this.findSync(id);
  }

  public @NotNull CompletableFuture<@Nullable T> findInBothAsync(final @NotNull String id) {
    return CompletableFuture.supplyAsync(() -> this.findInBothSync(id), this.executor);
  }

  /**
   * <b>Cache-Aside / Read-Through Pattern (Full):</b>
   * 1. Checks Fallback. Returns immediately if found (Cache Hit).
   * 2. If missing (Cache Miss), queries Main.
   * 3. If found in Main, saves to Fallback for future requests.
   *
   * @param id The ID to search for.
   * @return The entity found.
   */
  public @Nullable T findInBothAndSaveToFallbackSync(final @NotNull String id) {
    final var cachedAggregate = this.findInFallbackSync(id);
    if (cachedAggregate != null) {
      return cachedAggregate;
    }
    final var foundAggregate = this.findSync(id);
    if (foundAggregate == null) {
      return null;
    }
    this.fallbackRepository.saveSync(foundAggregate);
    return foundAggregate;
  }

  public @NotNull CompletableFuture<@Nullable T> findInBothAndSaveToFallbackAsync(final @NotNull String id) {
    return CompletableFuture.supplyAsync(() -> this.findInBothAndSaveToFallbackSync(id), this.executor);
  }

  /**
   * <b>Cache Warming Pattern:</b>
   * Loads ALL data from Main (DB) and saves it into Fallback (Memory).
   * <p>
   * High-cost operation. Recommended to be run only during application startup.
   *
   * @param postLoadAction Action to perform on each loaded element.
   * @param factory        Collection factory.
   * @param <C>            Collection type.
   * @return The collection of all loaded aggregates.
   */
  public <C extends Collection<@NotNull T>> @Nullable C loadAllSync(final @NotNull Consumer<@NotNull T> postLoadAction, final @NotNull IntFunction<@NotNull C> factory) {
    final var aggregates = this.mainRepository.findAllSync(postLoadAction, factory);
    if (aggregates.isEmpty()) {
      return null;
    }
    for (final var aggregate : aggregates) {
      this.fallbackRepository.saveSync(aggregate);
    }
    return aggregates;
  }

  public <C extends Collection<@NotNull T>> @NotNull CompletableFuture<@Nullable C> loadAllAsync(final @NotNull Consumer<@NotNull T> postLoadAction, final @NotNull IntFunction<@NotNull C> factory) {
    return CompletableFuture.supplyAsync(() -> this.loadAllSync(postLoadAction, factory), this.executor);
  }

  /**
   * Helper method to iterate over Fallback and save everything to Main.
   * Does not clear the fallback.
   */
  public void saveAllSync(final @NotNull Consumer<T> preSaveAction) {
    for (final var aggregate : this.fallbackRepository) {
      preSaveAction.accept(aggregate);
      this.mainRepository.saveSync(aggregate);
    }
  }

  public @NotNull CompletableFuture<@NotNull Void> saveAllAsync(final @NotNull Consumer<T> preSaveAction) {
    return CompletableFuture.runAsync(() -> this.saveAllSync(preSaveAction), this.executor);
  }

  /**
   * <b>Write-Through Pattern:</b>
   * Saves the entity to BOTH the Fallback and the Main repository immediately.
   * Ensures high consistency between Cache and DB.
   *
   * @param aggregate The entity to save.
   * @return The saved entity.
   */
  @Contract("_ -> param1")
  public @NotNull T saveInBothSync(final @NotNull T aggregate) {
    this.fallbackRepository.saveSync(aggregate);
    this.mainRepository.saveSync(aggregate);
    return aggregate;
  }

  public @NotNull CompletableFuture<@NotNull T> saveInBothAsync(final @NotNull T aggregate) {
    return CompletableFuture.supplyAsync(() -> this.saveInBothSync(aggregate), this.executor);
  }

  /**
   * <b>Single Item Flush:</b>
   * Moves a specific entity from Fallback to Main, and then removes it from Fallback.
   * Useful for freeing up memory for a specific object after persisting it.
   *
   * @param id The ID to migrate.
   * @return The migrated entity, or null if it wasn't in the fallback.
   */
  public @Nullable T uploadSync(final @NotNull String id) {
    final var aggregate = this.fallbackRepository.deleteAndRetrieveSync(id);
    if (aggregate == null) {
      return null;
    }
    this.mainRepository.saveSync(aggregate);
    return aggregate;
  }

  public @NotNull CompletableFuture<@Nullable T> uploadAsync(final @NotNull String id) {
    return CompletableFuture.supplyAsync(() -> this.uploadSync(id), this.executor);
  }

  /**
   * <b>Write-Behind / Flush Pattern:</b>
   * Iterates through ALL items in the Fallback, saves them to Main, and then clears the Fallback.
   * <p>
   * Ideal for periodic tasks (e.g., auto-save every 5 minutes) where data is kept in memory
   * and bulk-saved to DB.
   *
   * @param preUploadAction Action to perform before saving each element (e.g., update "lastSaved" timestamp).
   */
  public void uploadAllSync(final @NotNull Consumer<@NotNull T> preUploadAction) {
    for (final var aggregate : this.fallbackRepository) {
      preUploadAction.accept(aggregate);
      this.mainRepository.saveSync(aggregate);
    }
    this.fallbackRepository.deleteAllSync();
  }

  public @NotNull CompletableFuture<Void> uploadAllAsync(final @NotNull Consumer<@NotNull T> preUploadAction) {
    return CompletableFuture.runAsync(() -> this.uploadAllSync(preUploadAction), this.executor);
  }

  /**
   * @return The underlying Fallback repository (Cache).
   */
  public @NotNull AggregateRootRepository<T> fallbackRepository() {
    return this.fallbackRepository;
  }

  /**
   * @return The underlying Main repository (Database).
   */
  public @NotNull AggregateRootRepository<T> mainRepository() {
    return this.mainRepository;
  }
}
