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

import team.emptyte.storage.domain.AggregateRoot;
import team.emptyte.storage.domain.repository.AggregateRootRepository;
import team.emptyte.storage.domain.repository.AsyncAggregateRootRepository;
import team.emptyte.storage.infrastructure.codec.AggregateRootDeserializer;
import team.emptyte.storage.infrastructure.codec.AggregateRootSerializer;

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
 * Implementation of {@link AggregateRootRepository} that persists
 * aggregates as <b>JSON files</b> using the Google Gson library.
 * <p>
 * This class acts as a <b>File System Adapter</b> in the Hexagonal Architecture.
 * Each aggregate is stored as a separate file named {@code {id}.json} within the configured {@code folderPath}.
 * </p>
 * <h2>Performance &amp; Limitations:</h2>
 * <ul>
 * <li><b>I/O Bound:</b> All operations involve disk access. Performance depends on the underlying disk speed (SSD vs HDD).</li>
 * <li><b>Scalability:</b> Operations like {@code findAll} or {@code iterator} require scanning the directory.
 * Performance may degrade significantly if the folder contains thousands of files.</li>
 * <li><b>Atomicity:</b> File writes are generally atomic on modern OSs, but this class does not implement transactional locks across multiple files.</li>
 * </ul>
 *
 * @param <T> The type of the Aggregate Root.
 * @author team.emptyte
 * @since 0.1.0
 */
public class GsonAggregateRootRepository<T extends AggregateRoot> extends AsyncAggregateRootRepository<T> {
  private final Path folderPath;
  private final boolean prettyPrint;
  private final AggregateRootSerializer<T, JsonObject> serializer;
  private final AggregateRootDeserializer<T, JsonObject> deserializer;

  /**
   * Creates a new Gson-based repository with a specific Executor for async operations.
   *
   * @param executor     The executor to handle background I/O tasks.
   * @param folderPath   The directory where JSON files will be stored.
   * @param prettyPrint  If {@code true}, the JSON output will be indented for human readability (increases file size).
   * @param serializer   The adapter to convert the domain object to a Gson {@link JsonObject}.
   * @param deserializer The adapter to reconstitute the domain object from a Gson {@link JsonObject}.
   */
  protected GsonAggregateRootRepository(
    final @NotNull Executor executor,
    final @NotNull Path folderPath,
    final boolean prettyPrint,
    final @NotNull AggregateRootSerializer<T, JsonObject> serializer,
    final @NotNull AggregateRootDeserializer<T, JsonObject> deserializer
  ) {
    super(executor);

    this.folderPath = folderPath;
    this.prettyPrint = prettyPrint;
    this.serializer = serializer;
    this.deserializer = deserializer;
  }

  /**
   * Creates a new Gson-based repository using a default single-thread executor.
   * <p>
   * <b>Warning:</b> This relies on the default executor from the parent class.
   * Ensure this fits your concurrency model.
   * </p>
   *
   * @param folderPath   The directory where JSON files will be stored.
   * @param prettyPrint  If {@code true}, format JSON with indentation.
   * @param serializer   The serializer adapter.
   * @param deserializer The deserializer adapter.
   */
  protected GsonAggregateRootRepository(
    final @NotNull Path folderPath,
    final boolean prettyPrint,
    final @NotNull AggregateRootSerializer<T, JsonObject> serializer,
    final @NotNull AggregateRootDeserializer<T, JsonObject> deserializer
  ) {
    super();

    this.folderPath = folderPath;
    this.prettyPrint = prettyPrint;
    this.serializer = serializer;
    this.deserializer = deserializer;
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
      return this.deserializer.deserialize(jsonObject);
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
   * @implNote Writes the aggregate to a file using <b>UTF-8</b> encoding.
   * It is configured to <b>skip null fields</b> during serialization to save space.
   * If {@code prettyPrint} is enabled, the JSON will be indented.
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
        final JsonObject jsonObject = this.serializer.serialize(aggregateRoot);
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
   * @implNote <b>Memory Warning:</b> This implementation relies on {@link DirectoryStream} but currently
   * loads <b>ALL</b> aggregates into an {@link ArrayList} in memory before returning the iterator.
   * It is <b>NOT lazy</b>. Do not use on folders with massive amounts of data.
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
