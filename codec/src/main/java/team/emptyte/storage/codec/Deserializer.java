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
package team.emptyte.storage.codec;

import org.jetbrains.annotations.NotNull;

/**
 * Functional interface responsible for <b>converting</b> raw data into a specific object instance.
 * <p>
 * This component acts as a generic translator that maps input data (such as a JSON string,
 * a Map, a SQL ResultSet, or a BSON document) into a concrete Java object (POJO, Record, DTO, etc.).
 * </p>
 * <p>
 * <strong>State Reconstruction:</strong><br>
 * Its primary purpose is to hydrate an object's state from a stored or serialized format.
 * Depending on the implementation, this process may involve direct field assignment,
 * constructor invocation, or the use of builders to ensure the object is correctly populated.
 * </p>
 *
 * @param <Type>     The type of the target object to create/reconstruct.
 * @param <ReadType> The type of the raw input data (e.g., {@code String}, {@code Document}, {@code Map}).
 * @author team.emptyte
 * @see Serializer
 * @since 0.1.0
 */
@FunctionalInterface
public interface Deserializer<Type, ReadType> {

  /**
   * Transforms the provided serialized data into an instance of the target type.
   * <p>
   * This method reads the input data and maps it to the structure of the target object.
   * Implementations should handle the logic necessary to parse the raw data and
   * return a valid, fully populated instance.
   * </p>
   *
   * @param serialized The raw data source. Must not be {@code null}.
   * @return A fully reconstructed instance of type {@code Type}.
   * @throws RuntimeException if the data is malformed or cannot be converted to the target type.
   */
  @NotNull Type deserialize(final @NotNull ReadType serialized);
}
