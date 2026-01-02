package team.emptyte.storage.aggregate.infrastructure.gson;

import com.google.gson.JsonObject;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.emptyte.storage.aggregate.domain.AggregateRoot;
import team.emptyte.storage.aggregate.domain.repository.AsyncAggregateRootRepository;
import team.emptyte.storage.aggregate.infrastructure.codec.AggregateRootDeserializer;
import team.emptyte.storage.aggregate.infrastructure.codec.AggregateRootSerializer;

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

/**
 * Implementation of {@link team.emptyte.storage.aggregate.domain.repository.AggregateRootRepository} that persists
 * aggregates as <b>JSON files</b> using the Gson library.
 * <p>
 * Each aggregate is stored as a separate file named {@code {id}.json} within the configured {@code folderPath}.
 * </p>
 * <p>
 * <strong>Performance Note:</strong><br>
 * Since this implementation relies on the file system, operations like {@code iterator()} or {@code findAllSync}
 * involve scanning the directory. Performance may degrade with a very large number of files (thousands) in a single folder.
 * </p>
 *
 * @param <T> The type of the Aggregate Root.
 * @author team.emptyte
 * @since 0.0.1
 */
public class GsonAggregateRootRepository<T extends AggregateRoot> extends AsyncAggregateRootRepository<T> {
  private final Class<T> aggregateRootType;
  private final Path folderPath;
  private final boolean prettyPrint;
  private final AggregateRootSerializer<T, JsonObject> serializer;
  private final AggregateRootDeserializer<T, JsonObject> deserializer;

  /**
   * Creates a new Gson-based repository with a specific Executor for async operations.
   *
   * @param executor          The executor to handle background I/O tasks.
   * @param aggregateRootType The class type of the aggregate (used for reflection/logging if needed).
   * @param folderPath        The directory where JSON files will be stored. Must exist or be creatable.
   * @param prettyPrint       If {@code true}, the JSON output will be indented for human readability (increases file size).
   * @param serializer        The adapter to convert the domain object to a Gson {@link JsonObject}.
   * @param deserializer      The adapter to reconstitute the domain object from a Gson {@link JsonObject}.
   */
  protected GsonAggregateRootRepository(
    final @NotNull Executor executor,
    final @NotNull Class<T> aggregateRootType,
    final @NotNull Path folderPath,
    final boolean prettyPrint,
    final @NotNull AggregateRootSerializer<T, JsonObject> serializer,
    final @NotNull AggregateRootDeserializer<T, JsonObject> deserializer
  ) {
    super(executor);

    this.aggregateRootType = aggregateRootType;
    this.folderPath = folderPath;
    this.prettyPrint = prettyPrint;
    this.serializer = serializer;
    this.deserializer = deserializer;
  }

  /**
   * Creates a new Gson-based repository using a default single-thread executor.
   * <p>
   * <b>Warning:</b> Not recommended for high-throughput production environments.
   * </p>
   *
   * @param aggregateRootType The class type of the aggregate.
   * @param folderPath        The directory where JSON files will be stored.
   * @param prettyPrint       If {@code true}, format JSON with indentation.
   * @param serializer        The serializer adapter.
   * @param deserializer      The deserializer adapter.
   */
  protected GsonAggregateRootRepository(
    final @NotNull Class<T> aggregateRootType,
    final @NotNull Path folderPath,
    final boolean prettyPrint,
    final @NotNull AggregateRootSerializer<T, JsonObject> serializer,
    final @NotNull AggregateRootDeserializer<T, JsonObject> deserializer
  ) {
    super(); // Calls parent constructor which creates a single thread executor

    this.aggregateRootType = aggregateRootType;
    this.folderPath = folderPath;
    this.prettyPrint = prettyPrint;
    this.serializer = serializer;
    this.deserializer = deserializer;
  }

  private @NotNull Path resolveChild(final @NotNull String id) {
    return this.folderPath.resolve(id + ".json");
  }

  /**
   * Extracts the aggregate ID from a given file path.
   * <p>
   * Assumes the filename format is {@code {id}.json}.
   * </p>
   *
   * @param file The path to the file.
   * @return The ID portion of the filename.
   */
  public @NotNull String extractId(final @NotNull Path file) {
    final String fileName = file.getFileName().toString();
    // Safely remove the last 5 characters (".json")
    return fileName.substring(0, fileName.length() - 5);
  }

  private @Nullable T internalFind(final @NotNull Path file) {
    if (!Files.exists(file)) {
      return null;
    }
    try (final JsonReader reader = new JsonReader(Files.newBufferedReader(file))) {
      final JsonObject jsonObject = new JsonObject();
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
   * @implNote This implementation attempts to delete the file {@code {id}.json}.
   * Wraps specific {@link IOException} into a generic {@link RuntimeException}.
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
   * @implNote Uses {@link Files#walk} to traverse the directory and delete all files.
   * This operation is not atomic.
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

  @Override
  public @NotNull <C extends Collection<@NotNull T>> C findAllSync(final @NotNull Consumer<T> postLoadAction, final @NotNull IntFunction<C> factory) {
    final C foundAggregates = factory.apply(1);
    // Reuse the internal iterator logic or directory stream
    this.forEach(aggregateRoot -> {
      postLoadAction.accept(aggregateRoot);
      foundAggregates.add(aggregateRoot);
    });
    return foundAggregates;
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Uses {@link Files#newDirectoryStream(Path)} to list files without loading their content.
   */
  @Override
  public @NotNull <C extends Collection<@NotNull String>> C findIdsSync(final @NotNull IntFunction<C> factory) {
    try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.folderPath)) {
      final C foundIds = factory.apply(1); // Initial size hint
      directoryStream.forEach(path -> foundIds.add(this.extractId(path)));
      return foundIds;
    } catch (IOException e) {
      throw new RuntimeException("Failed to list files in: " + this.folderPath, e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Writes the aggregate to a file using UTF-8 encoding. If {@code prettyPrint} is enabled,
   * the JSON will be indented. Creates the file if it does not exist.
   */
  @Override
  public @NotNull T saveSync(@NotNull final T aggregateRoot) {
    final Path aggregateRootPath = this.resolveChild(aggregateRoot.id());
    try {
      if (Files.notExists(aggregateRootPath)) {
        Files.createFile(aggregateRootPath);
      }
      try (final JsonWriter writer = new JsonWriter(Files.newBufferedWriter(aggregateRootPath, StandardCharsets.UTF_8))) {
        writer.setSerializeNulls(false);
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
   * @implNote <b>Warning:</b> This implementation eagerly loads all files into memory to create the Iterator.
   * It does not stream lazily from the disk.
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
