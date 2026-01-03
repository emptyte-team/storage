package team.emptyte.storage.domain;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class representing an <b>Aggregate Root</b> within the Domain-Driven Design (DDD) context.
 * <p>
 * In DDD, an Aggregate is a cluster of domain objects that can be treated as a single unit.
 * The {@code AggregateRoot} is the only entity within the aggregate that external objects
 * (including Repositories and Storage Services) are allowed to reference directly.
 * </p>
 * <p>
 * <strong>Role in the Storage Library:</strong><br>
 * This class ensures that any persistable entity possesses a unique <b>Identity</b> (ID).
 * This identity is fundamental for the storage mechanism to:
 * <ul>
 * <li>Index the object efficiently.</li>
 * <li>Retrieve the current state from the underlying persistence layer.</li>
 * <li>Maintain data consistency boundaries.</li>
 * </ul>
 * </p>
 *
 * @author team.emptyte
 * @since 0.1.0
 */
public abstract class AggregateRoot {

  /**
   * The unique and immutable identifier for this aggregate.
   * <p>
   * It defines the entity's identity regardless of changes to its internal state.
   * </p>
   */
  private final String id;

  /**
   * Initializes the Aggregate Root.
   * <p>
   * A non-null ID is mandatory to ensure the entity has a valid identity
   * from the moment of its creation.
   * </p>
   *
   * @param id The unique identifier for this aggregate. Must not be {@code null}.
   */
  public AggregateRoot(final @NotNull String id) {
    this.id = id;
  }

  /**
   * Retrieves the unique identifier of this aggregate.
   * <p>
   * This method uses a fluent style (no "get" prefix) to denote direct access to the
   * identity property.
   * </p>
   *
   * @return The ID of the aggregate as a {@link String}. Never returns {@code null}.
   */
  public @NotNull String id() {
    return this.id;
  }
}
