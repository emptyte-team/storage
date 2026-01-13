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
 * A contract for serializing and deserializing objects between different types.
 *
 * <p>A TypeAdapter acts as a bidirectional bridge, allowing a domain object
 * of type {@code T} to be converted into a persistent or transportable format
 * of type {@code R}, and vice versa.</p>
 *
 * @param <T> The domain object type (e.g., User, Player, Location).
 * @param <R> The representation type (e.g., String, JsonElement, byte[]).
 */
public class TypeAdapter<T, R> {

  /**
   * Converts the given domain object into its storage representation.
   *
   * @param value The object of type {@code T} to be encoded. Must not be null.
   * @return The resulting representation of type {@code R}.
   */
  public @NotNull R write(final @NotNull T value) {

  }

  /**
   * Reconstructs the original domain object from its representation.
   *
   * @param value The representation of type {@code R} to be decoded. Must not be null.
   * @return The reconstructed object of type {@code T}.
   */
  public @NotNull T read(final @NotNull R value) {
    return null;
  }
}
