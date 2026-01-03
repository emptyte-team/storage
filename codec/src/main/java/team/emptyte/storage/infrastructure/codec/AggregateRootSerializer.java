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
 *
 * @param <AggregateRootType> The type of the domain aggregate.
 * @param <ReadType>          The type of the target data structure.
 * @author team.emptyte
 * @since 0.1.0
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
