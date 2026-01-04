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
 * Functional interface responsible for <b>converting</b> an in-memory object into a
 * raw data format suitable for storage or transmission.
 * <p>
 * This component acts as a generic translator that turns rich Java objects (POJOs, Records,
 * DTOs, etc.) into a lower-level representation (like a JSON string, a Map, a Byte array,
 * or a BSON document).
 * </p>
 * <p>
 * <strong>Generic Types:</strong>
 * <ul>
 * <li>{@code Type}: The type of the object to be serialized.</li>
 * <li>{@code ReadType}: The target data format (e.g., {@code String}, {@code Document}, {@code byte[]})
 * that represents the serialized object.</li>
 * </ul>
 *
 * @param <Type>     The type of the object to serialize.
 * @param <ReadType> The type of the resulting data structure.
 * @author team.emptyte
 * @see Serializer
 * @since 0.1.0
 */
@FunctionalInterface
public interface Serializer<Type, ReadType> {

  /**
   * Converts the provided object instance into a raw data structure.
   * <p>
   * This method should be a <b>pure function</b>: it must generate the output based solely
   * on the input object, without modifying the state of the {@code input} or causing side effects.
   * </p>
   *
   * @param input The object instance to serialize. Must not be {@code null}.
   * @return The serialized representation of the object (e.g., a JSON String or Map).
   */
  @NotNull ReadType serialize(final @NotNull Type input);
}
