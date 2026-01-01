package team.emptyte.storage.aggregate.infrastructure.codec;

import org.jetbrains.annotations.NotNull;

/**
 * Functional interface responsible for transforming a domain <b>Aggregate Root</b> into a
 * data format suitable for storage.
 * <p>
 * In the context of the Hexagonal Architecture (Ports and Adapters), this interface acts as a
 * <b>Data Mapper</b> component. It prevents the domain entities from knowing about the
 * underlying persistence details (like JSON structures, SQL statements, or Byte arrays).
 * </p>
 * <p>
 * <strong>Generic Types:</strong>
 * <ul>
 * <li>{@code AggregateRootType}: The high-level Domain Entity to be serialized.</li>
 * <li>{@code ReadType}: The low-level data format (e.g., {@code String}, {@code Document}, {@code byte[]})
 * that the storage adapter understands.</li>
 * </ul>
 * </p>
 *
 * @param <AggregateRootType> The type of the domain aggregate.
 * @param <ReadType>          The type of the target data structure.
 * @author team.emptyte
 * @since 0.0.1
 * @see AggregateRootDeserializer
 */
@FunctionalInterface
public interface AggregateRootSerializer<AggregateRootType, ReadType> {

  /**
   * Converts the provided Aggregate Root into a low-level data structure.
   * <p>
   * This method should be a <b>pure function</b>: it must generate the output based solely
   * on the input, without modifying the state of the {@code aggregateRoot} or causing side effects.
   * </p>
   *
   * @param aggregateRoot The domain entity to serialize. Must not be {@code null}.
   * @return The data representation of the entity (e.g., a JSON String or Map).
   */
  @NotNull ReadType serialize(final @NotNull AggregateRootType aggregateRoot);
}
