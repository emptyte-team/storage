package team.emptyte.storage.infrastructure.gson;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import team.emptyte.storage.domain.AggregateRoot;
import team.emptyte.storage.domain.repository.AsyncAggregateRootRepository;
import team.emptyte.storage.domain.repository.builder.AbstractAggregateRootRepositoryBuilder;
import team.emptyte.storage.infrastructure.codec.AggregateRootDeserializer;
import team.emptyte.storage.infrastructure.codec.AggregateRootSerializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;

/**
 * A builder for creating instances of {@link GsonAggregateRootRepository}.
 * <p>
 * This builder simplifies the configuration of the JSON storage engine, allowing you to specify:
 * <ul>
 * <li><b>Storage Location:</b> The folder where JSON files will be kept.</li>
 * <li><b>Formatting:</b> Whether to use pretty printing (indentation) or minified JSON.</li>
 * <li><b>Serialization Logic:</b> How to convert your specific domain objects to/from Gson's {@link JsonObject}.</li>
 * </ul>
 *
 * @param <T> The type of AggregateRoot to be built.
 */
public class GsonAggregateRootRepositoryBuilder<T extends AggregateRoot> extends AbstractAggregateRootRepositoryBuilder<T> {
  private Path folderPath;
  private boolean prettyPrinting;
  private AggregateRootSerializer<T, JsonObject> writer;
  private AggregateRootDeserializer<T, JsonObject> reader;

  /**
   * Package-private constructor to enforce the use of the static factory method
   * {@link GsonAggregateRootRepository#builder()}.
   */
  GsonAggregateRootRepositoryBuilder() {
  }

  /**
   * Sets the root directory where the JSON files will be stored.
   *
   * @param folderPath The directory path.
   * @return This builder instance for chaining.
   */
  @Contract("_ -> this")
  public @NotNull GsonAggregateRootRepositoryBuilder<T> folder(final @NotNull Path folderPath) {
    this.folderPath = folderPath;
    return this;
  }

  /**
   * Configures the JSON output format.
   *
   * @param prettyPrinting If {@code true}, the JSON will be indented (easier to read/debug).
   *                       If {@code false}, the JSON will be minified (saves space).
   * @return This builder instance for chaining.
   */
  @Contract("_ -> this")
  public @NotNull GsonAggregateRootRepositoryBuilder<T> prettyPrinting(final boolean prettyPrinting) {
    this.prettyPrinting = prettyPrinting;
    return this;
  }

  /**
   * Sets the serializer responsible for converting the Aggregate Root into a Gson {@link JsonObject}.
   *
   * @param writer The serializer implementation.
   * @return This builder instance for chaining.
   */
  @Contract("_ -> this")
  public @NotNull GsonAggregateRootRepositoryBuilder<T> aggregateRootSerializer(
    final @NotNull AggregateRootSerializer<T, JsonObject> writer
  ) {
    this.writer = writer;
    return this;
  }

  /**
   * Sets the deserializer responsible for reconstructing the Aggregate Root from a Gson {@link JsonObject}.
   *
   * @param reader The deserializer implementation.
   * @return This builder instance for chaining.
   */
  @Contract("_ -> this")
  public @NotNull GsonAggregateRootRepositoryBuilder<T> aggregateRootDeserializer(
    final @NotNull AggregateRootDeserializer<T, JsonObject> reader
  ) {
    this.reader = reader;
    return this;
  }

  /**
   * Builds the final {@link GsonAggregateRootRepository}.
   * <p>
   * <b>Side Effect:</b> This method checks if the configured {@code folderPath} exists.
   * If it does not, it attempts to create the directory (including any necessary parent directories).
   *
   * @param executor The executor to use for async operations in the repository.
   * @return The fully configured repository.
   * @throws RuntimeException If the directory cannot be created.
   */
  @Override
  @Contract("_ -> new")
  public @NotNull AsyncAggregateRootRepository<T> build(final @NotNull Executor executor) {
    // Infrastructure initialization logic
    if (Files.notExists(this.folderPath)) {
      try {
        Files.createDirectories(this.folderPath);
      } catch (final IOException e) {
        throw new RuntimeException("Could not create storage directory: " + this.folderPath, e);
      }
    }

    return new GsonAggregateRootRepository<>(
      executor,
      this.folderPath,
      this.prettyPrinting,
      this.writer,
      this.reader
    );
  }
}
