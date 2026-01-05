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
import team.emptyte.storage.domain.repository.AggregateRootRepository;
import team.emptyte.storage.domain.repository.AsyncAggregateRootRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * An implementation of {@link AggregateRootRepository} that persists domain aggregates
 * as individual <b>JSON files</b> using the Google Gson library.
 *
 * <p>This implementation functions as a <b>File System Adapter</b> within the
 * Infrastructure layer (Hexagonal Architecture). Each aggregate is stored as a
 * standalone file following the naming convention {@code {id}.json} inside the
 * specified {@code folderPath}.</p>
 *
 * <h3>Technical Considerations:</h3>
 * <ul>
 * <li><b>Performance:</b> Being I/O bound, performance is dictated by disk throughput (SSD recommended).</li>
 * <li><b>Concurrency:</b> While this class is stateless, file system operations are not
 * inherently transactional. External synchronization may be required if multiple
 * processes access the same directory.</li>
 * <li><b>Scalability:</b> Operations like {@link #findAllSync} or {@link #iterator()}
 * perform directory scans. Performance scales linearly $O(n)$ with the number
 * of files, which may become a bottleneck with tens of thousands of entries.</li>
 * </ul>
 *
 * @param <T> The type of the {@link AggregateRoot} being persisted.
 * @author team.emptyte
 * @since 0.1.0
 */
public class GsonAggregateRootRepository<T extends AggregateRoot> extends AsyncAggregateRootRepository<T> {
  private final Path folderPath;
  private final boolean prettyPrint;
  private final TypeAdapter<T, JsonObject> typeAdapter;

  /**
   * Initializes the repository.
   * <p><b>Note:</b> The provided {@code folderPath} should exist and have
   * write permissions for the application.</p>
   *
   * @param executor     The task runner for asynchronous operations.
   * @param folderPath   The root directory for JSON storage.
   * @param prettyPrint  Whether to indent the output JSON (useful for debugging,
   * but increases storage footprint).
   * @param typeAdapter  The bridge used to map domain objects to {@link JsonObject}.
   */
  protected GsonAggregateRootRepository(
    final @NotNull Executor executor,
    final @NotNull Path folderPath,
    final boolean prettyPrint,
    final @NotNull TypeAdapter<T, JsonObject> typeAdapter
  ) {
    super(executor);

    this.folderPath = folderPath;
    this.prettyPrint = prettyPrint;
    this.typeAdapter = typeAdapter;
  }

  /**
   * Creates a builder to configure and instantiate the repository.
   *
   * @param <T> The aggregate type.
   * @return A new Builder instance.
   */
  public static <T extends AggregateRoot> @NotNull GsonAggregateRootRepositoryBuilder<T> builder() {
    return new GsonAggregateRootRepositoryBuilder<>();
  }

  /**
   * Resolves the full path for a specific aggregate ID.
   *
   * @param id The aggregate identifier.
   * @return The absolute path to the {@code .json} file.
   */
  private @NotNull Path resolveChild(final @NotNull String id) {
    return this.folderPath.resolve(id + ".json");
  }

  /**
   * Extracts the aggregate ID from a given file path.
   * <p>
   * Assumes the filename format is exactly {@code {id}.json}.
   * </p>
   *
   * @param file The path to the file.
   * @return The ID portion of the filename (without extension).
   */
  public @NotNull String extractId(final @NotNull Path file) {
    final String fileName = file.getFileName().toString();
    // Safely remove the last 5 characters (".json")
    return fileName.substring(0, fileName.length() - 5);
  }

  /**
   * Internal helper to read and deserialize a file.
   *
   * @param file The file path to read.
   * @return The reconstituted aggregate, or null if the file does not exist.
   * @throws RuntimeException If an I/O error occurs during reading.
   */
  private @Nullable T internalFind(final @NotNull Path file) {
    if (!Files.exists(file)) {
      return null;
    }
    try (final JsonReader reader = new JsonReader(Files.newBufferedReader(file))) {
      final JsonObject jsonObject = new JsonObject();
      // Manual streaming into a JsonObject to avoid loading the full string into memory first
      reader.beginObject();
      while (reader.hasNext()) {
        jsonObject.add(reader.nextName(), TypeAdapters.JSON_ELEMENT.read(reader));
      }
      reader.endObject();
      return this.typeAdapter.read(jsonObject);
    } catch (final IOException e) {
      throw new RuntimeException("Failed to read JSON file: " + file, e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Wraps specific {@link IOException} into a generic {@link RuntimeException}.
   */
  @Override
  public boolean deleteSync(final @NotNull String id) {
    try {
      return Files.deleteIfExists(this.resolveChild(id));
    } catch (final IOException e) {
      throw new RuntimeException("Failed to delete file for ID: " + id, e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Uses {@link Files#walk} to traverse the directory depth-first.
   * This operation stops at the first failure.
   */
  @Override
  public void deleteAllSync() {
    try (final Stream<Path> walk = Files.walk(this.folderPath, 1)) {
      walk.filter(Files::isRegularFile)
        .forEach(path -> {
          try {
            Files.deleteIfExists(path);
          } catch (final IOException e) {
            throw new RuntimeException("Failed to delete file: " + path, e);
          }
        });
    } catch (final IOException e) {
      throw new RuntimeException("Failed to walk directory: " + this.folderPath, e);
    }
  }

  @Override
  public @Nullable T deleteAndRetrieveSync(final @NotNull String id) {
    // Note: This is not atomic. A failure between find and delete could leave the file.
    final T aggregateRoot = this.findSync(id);
    if (aggregateRoot != null) {
      this.deleteSync(id);
    }
    return aggregateRoot;
  }

  @Override
  public boolean existsSync(final @NotNull String id) {
    return Files.exists(this.resolveChild(id));
  }

  /**
   * {@inheritDoc}
   * * @implNote Uses a {@link JsonReader} to stream the file content directly into a
   * {@link JsonObject}. This approach is more memory-efficient than reading the
   * entire file into a String before parsing.
   * @throws RuntimeException wrapping {@link IOException} if the file is corrupted
   * or inaccessible.
   */
  @Override
  public @Nullable T findSync(final @NotNull String id) {
    return this.internalFind(this.resolveChild(id));
  }

  /**
   * {@inheritDoc}
   *
   * @implNote This method reuses the internal iterator logic. It is synchronous and blocking.
   */
  @Override
  public @NotNull <C extends Collection<@NotNull T>> C findAllSync(final @NotNull Consumer<T> postLoadAction, final @NotNull IntFunction<C> factory) {
    final C foundAggregates = factory.apply(1);
    this.forEach(aggregateRoot -> {
      postLoadAction.accept(aggregateRoot);
      foundAggregates.add(aggregateRoot);
    });
    return foundAggregates;
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Uses {@link Files#newDirectoryStream(Path)} to list files efficiently without reading their content.
   */
  @Override
  public @NotNull <C extends Collection<@NotNull String>> C findIdsSync(final @NotNull IntFunction<C> factory) {
    try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.folderPath)) {
      final C foundIds = factory.apply(1); // Starting with small size hint
      directoryStream.forEach(path -> foundIds.add(this.extractId(path)));
      return foundIds;
    } catch (IOException e) {
      throw new RuntimeException("Failed to list files in: " + this.folderPath, e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @implNote This method ensures the file exists before writing. It utilizes
   * <b>UTF-8</b> encoding and is configured via {@link JsonWriter#setSerializeNulls(boolean)}
   * to {@code false} to minimize file size by omitting null fields.
   */
  @Override
  public @NotNull T saveSync(@NotNull final T aggregateRoot) {
    final Path aggregateRootPath = this.resolveChild(aggregateRoot.id());
    try {
      if (Files.notExists(aggregateRootPath)) {
        Files.createFile(aggregateRootPath);
      }
      try (final JsonWriter writer = new JsonWriter(Files.newBufferedWriter(aggregateRootPath, StandardCharsets.UTF_8))) {
        writer.setSerializeNulls(false); // Optimization: Don't write nulls
        if (this.prettyPrint) {
          writer.setIndent("  ");
        }
        final JsonObject jsonObject = this.typeAdapter.write(aggregateRoot);
        TypeAdapters.JSON_ELEMENT.write(writer, jsonObject);
        return aggregateRoot;
      }
    } catch (final IOException e) {
      throw new RuntimeException("Failed to save aggregate: " + aggregateRoot.id(), e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @implNote <b>Warning: Non-Lazy Implementation.</b>
   * This implementation drains the {@link DirectoryStream} into an in-memory
   * {@link List} before returning the iterator. In environments with a massive
   * amount of aggregates, this may trigger an {@link OutOfMemoryError}.
   */
  @Override
  public @NotNull Iterator<T> iterator() {
    try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.folderPath)) {
      final List<T> foundAggregates = new ArrayList<>();
      directoryStream.forEach(path -> foundAggregates.add(this.internalFind(path)));
      return foundAggregates.iterator();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @implNote <b>Memory Warning:</b> Similar to {@link #iterator()}, this loads all IDs into memory.
   */
  @Override
  public @NotNull Iterator<@NotNull String> iteratorIds() {
    try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.folderPath)) {
      final List<String> foundIds = new ArrayList<>();
      directoryStream.forEach(path -> foundIds.add(this.extractId(path)));
      return foundIds.iterator();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
