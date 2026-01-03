package team.emptyte.storage.aggregate.domain.repository.builder;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import team.emptyte.storage.aggregate.domain.AggregateRoot;
import team.emptyte.storage.aggregate.domain.repository.AggregateRootRepository;
import team.emptyte.storage.aggregate.domain.repository.AsyncAggregateRootRepository;
import team.emptyte.storage.aggregate.domain.repository.WithFallbackAggregateRootRepository;

import java.util.concurrent.Executor;

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
   * (e.g., File System, Database, Network).
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
