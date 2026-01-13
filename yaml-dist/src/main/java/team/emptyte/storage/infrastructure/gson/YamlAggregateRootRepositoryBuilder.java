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
package team.emptyte.storage.infrastructure.gson;

import org.spongepowered.configurate.serialize.TypeSerializer;
import team.emptyte.storage.domain.AggregateRoot;
import team.emptyte.storage.domain.repository.AsyncAggregateRootRepository;
import team.emptyte.storage.domain.repository.builder.AbstractAggregateRootRepositoryBuilder;

import java.nio.file.Path;
import java.util.concurrent.Executor;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A builder for creating instances of {@link YamlAggregateRootRepository}.
 * <p>
 * This class provides a fluent API to configure the necessary parts for the YAML repository,
 * including the storage directory, the aggregate type class, serialization logic, and output formatting.
 *
 * @param <T> the type of the {@link AggregateRoot} managed by the repository
 * @see YamlAggregateRootRepository
 */
public class YamlAggregateRootRepositoryBuilder<T extends AggregateRoot> extends AbstractAggregateRootRepositoryBuilder<T> {
  private Class<T> aggregateType;
  private Path folderPath;
  private boolean prettyPrinting;
  private TypeSerializer<T> typeSerializer;

  /**
   * Sets the class type of the aggregate root.
   * <p>
   * This class token is required by the Configurate library to correctly identify and
   * instantiate the target objects during deserialization.
   *
   * @param aggregateType the class of the aggregate
   * @return this builder instance
   */
  @Contract("_ -> this")
  public YamlAggregateRootRepositoryBuilder<T> aggregateType(final @NotNull Class<T> aggregateType) {
    this.aggregateType = aggregateType;
    return this;
  }

  /**
   * Sets the root directory where the YAML files will be stored.
   *
   * @param folderPath the directory path
   * @return this builder instance
   */
  @Contract("_ -> this")
  public YamlAggregateRootRepositoryBuilder<T> folder(final @NotNull Path folderPath) {
    this.folderPath = folderPath;
    return this;
  }

  /**
   * Configures the output style of the YAML files.
   *
   * @param prettyPrinting if {@code true}, the YAML generator will use <b>Block Style</b>
   * (standard indented YAML, easier to read). If {@code false}, it will
   * use <b>Flow Style</b> (JSON-like syntax, more compact).
   * @return this builder instance
   */
  @Contract("_ -> this")
  public YamlAggregateRootRepositoryBuilder<T> prettyPrinting(final boolean prettyPrinting) {
    this.prettyPrinting = prettyPrinting;
    return this;
  }

  /**
   * Sets the {@link TypeSerializer} responsible for converting the aggregate to/from a configuration node.
   * <p>
   * This serializer bridges the gap between your domain object and the Configurate node structure.
   *
   * @param typeSerializer the serializer implementation for type {@code T}
   * @return this builder instance
   */
  @Contract("_ -> this")
  public YamlAggregateRootRepositoryBuilder<T> typeSerializer(final @NotNull TypeSerializer<T> typeSerializer) {
    this.typeSerializer = typeSerializer;
    return this;
  }

  /**
   * Constructs a new {@link YamlAggregateRootRepository} using the configured parameters.
   *
   * @param executor the executor to be used for asynchronous tasks within the repository
   * @return a new repository instance
   * @throws IllegalArgumentException if required fields (like folder path or aggregate type) are missing,
   * depending on the validation in the repository constructor.
   */
  @Override
  public @NotNull AsyncAggregateRootRepository<T> build(final @NotNull Executor executor) {
    return new YamlAggregateRootRepository<>(
      executor,
      this.aggregateType,
      this.folderPath,
      this.prettyPrinting,
      this.typeSerializer
    );
  }
}
