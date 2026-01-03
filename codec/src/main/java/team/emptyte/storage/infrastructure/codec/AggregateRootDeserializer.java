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
package team.emptyte.storage.infrastructure.codec;

import org.jetbrains.annotations.NotNull;

/**
 * Functional interface responsible for <b>reconstituting</b> a domain Aggregate Root from its
 * raw stored representation.
 * <p>
 * This component acts as the bridge that translates infrastructure-specific data (like a JSON string,
 * a SQL ResultSet row, or a BSON document) back into a rich Domain Entity.
 * </p>
 * <p>
 * <strong>Reconstitution vs. Creation:</strong><br>
 * Unlike a factory that creates a brand new entity, this deserializer restores an existing entity
 * to a specific state. Implementations should ensure that the entity's Identity (ID) is correctly
 * preserved from the storage.
 * </p>
 *
 * @param <AggregateRootType> The type of the domain entity to create.
 * @param <ReadType>          The type of the raw data input (e.g., {@code String}, {@code Document}).
 * @author team.emptyte
 * @since 0.1.0
 * @see AggregateRootSerializer
 */
@FunctionalInterface
public interface AggregateRootDeserializer<AggregateRootType, ReadType> {

  /**
   * Reconstructs an Aggregate Root instance from the provided serialized data.
   * <p>
   * This method acts as a factory, hydrating the object's state. Implementations may need to use
   * reflection, package-private constructors, or static factory methods to instantiate the
   * aggregate without triggering domain events intended only for new objects.
   * </p>
   *
   * @param serialized The raw data retrieved from storage. Must not be {@code null}.
   * @return A fully reconstructed instance of the Aggregate Root.
   * @throws RuntimeException if the data is malformed or cannot be mapped to the entity.
   */
  @NotNull AggregateRootType deserialize(final @NotNull ReadType serialized);
}
