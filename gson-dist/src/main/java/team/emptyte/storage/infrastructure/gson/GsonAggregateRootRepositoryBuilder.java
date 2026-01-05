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

import team.emptyte.storage.codec.TypeAdapter;
import team.emptyte.storage.domain.AggregateRoot;
import team.emptyte.storage.domain.repository.AsyncAggregateRootRepository;
import team.emptyte.storage.domain.repository.builder.AbstractAggregateRootRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonObject;

/**
 * A fluent builder implementation for creating {@link GsonAggregateRootRepository} instances.
 *
 * <p>This builder follows the <b>Fluent API</b> pattern, allowing for a descriptive
 * configuration of the JSON storage engine. It centralizes the validation of required
 * parameters and ensures the underlying file system is prepared before the repository
 * starts its operations.</p>
 *
 * <h3>Key Configurations:</h3>
 * <ul>
 * <li><b>Location:</b> Defines the {@link Path} where the JSON documents will reside.</li>
 * <li><b>Format:</b> Controls whether the output should be human-readable (pretty-printed)
 * or optimized for size (minified).</li>
 * <li><b>Codec:</b> Assigns a {@link TypeAdapter} to handle the domain-to-JSON mapping.</li>
 * </ul>
 *
 * @param <T> The type of {@link AggregateRoot} this builder will produce a repository for.
 * @author team.emptyte
 * @since 0.1.0
 */
public class GsonAggregateRootRepositoryBuilder<T extends AggregateRoot> extends AbstractAggregateRootRepositoryBuilder<T> {
  private Path folderPath;
  private boolean prettyPrinting;
  private TypeAdapter<T, JsonObject> typeAdapter;

  /**
   * Package-private constructor.
   * <p>Use {@link GsonAggregateRootRepository#builder()} to obtain a new instance.</p>
   */
  GsonAggregateRootRepositoryBuilder() {
  }

  /**
   * Defines the root directory for the JSON storage.
   *
   * @param folderPath The {@link Path} to the directory.
   * @return This builder instance for method chaining.
   */
  @Contract("_ -> this")
  public @NotNull GsonAggregateRootRepositoryBuilder<T> folder(final @NotNull Path folderPath) {
    this.folderPath = folderPath;
    return this;
  }

  /**
   * Sets the visual format of the generated JSON files.
   *
   * @param prettyPrinting If {@code true}, applies indentation and line breaks.
   *                       Useful for development and manual debugging.
   * @return This builder instance for method chaining.
   */
  @Contract("_ -> this")
  public @NotNull GsonAggregateRootRepositoryBuilder<T> prettyPrinting(final boolean prettyPrinting) {
    this.prettyPrinting = prettyPrinting;
    return this;
  }

  /**
   * Sets the adapter responsible for converting the domain object into a {@link JsonObject}.
   *
   * @param typeAdapter The codec implementation.
   * @return This builder instance for method chaining.
   */
  @Contract("_ -> this")
  public @NotNull GsonAggregateRootRepositoryBuilder<T> typeAdapter(final @NotNull TypeAdapter<T, JsonObject> typeAdapter) {
    this.typeAdapter = typeAdapter;
    return this;
  }

  /**
   * Validates the configuration and constructs the {@link GsonAggregateRootRepository}.
   *
   * <p><b>Side Effects:</b> This method checks if the specified {@code folderPath} exists.
   * If it does not, it attempts to create the directory and all necessary parent
   * directories using {@link Files#createDirectories}.</p>
   *
   * @param executor The {@link Executor} used to run asynchronous repository tasks.
   * @return A fully initialized {@link AsyncAggregateRootRepository}.
   * @throws NullPointerException if any mandatory field (folder, adapter) is missing.
   * @throws RuntimeException if the storage directory is inaccessible or cannot be created.
   */
  @Override
  @Contract("_ -> new")
  public @NotNull AsyncAggregateRootRepository<T> build(final @NotNull Executor executor) {
    if (Files.notExists(this.folderPath)) {
      try {
        Files.createDirectories(this.folderPath);
      } catch (IOException e) {
        // Fail fast: We cannot proceed if we can't write to the disk.
        throw new RuntimeException("Failed to initialize storage directory at: " + this.folderPath, e);
      }
    }
    return new GsonAggregateRootRepository<>(
      executor,
      this.folderPath,
      this.prettyPrinting,
      this.typeAdapter
    );
  }
}
