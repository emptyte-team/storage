package team.emptyte.storage.codec;

import org.jetbrains.annotations.NotNull;

/**
 * A contract for serializing and deserializing objects between different types.
 *
 * <p>A TypeAdapter acts as a bidirectional bridge, allowing a domain object
 * of type {@code T} to be converted into a persistent or transportable format
 * of type {@code R}, and vice versa.</p>
 *
 * @param <T> The domain object type (e.g., User, Player, Location).
 * @param <R> The representation type (e.g., String, JsonElement, byte[]).
 */
public interface TypeAdapter<T, R> {

  /**
   * Converts the given domain object into its storage representation.
   *
   * @param value The object of type {@code T} to be encoded. Must not be null.
   * @return The resulting representation of type {@code R}.
   */
  @NotNull R write(final @NotNull T value);

  /**
   * Reconstructs the original domain object from its representation.
   *
   * @param value The representation of type {@code R} to be decoded. Must not be null.
   * @return The reconstructed object of type {@code T}.
   */
  @NotNull T read(final @NotNull R value);
}
