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

import org.jspecify.annotations.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import team.emptyte.storage.domain.AggregateRoot;
import team.emptyte.storage.domain.repository.AsyncAggregateRootRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An implementation of {@link AsyncAggregateRootRepository} that persists domain aggregates
 * as individual <b>YAML files</b> using the Configurate library.
 *
 * <p>This implementation functions as a <b>File System Adapter</b> within the
 * Infrastructure layer (Hexagonal Architecture). Each aggregate is stored as a
 * standalone file following the naming convention {@code {id}.yml} inside the
 * specified {@code folderPath}.</p>
 *
 * <h2>Technical Considerations:</h2>
 * <ul>
 * <li><b>Performance:</b> Being I/O bound, performance is dictated by disk throughput.
 * YAML parsing (via Configurate) is generally more computationally expensive than JSON due to
 * the complexity of the format, though negligible for typical aggregate sizes.</li>
 * <li><b>Concurrency:</b> While this class is stateless, file system operations are not
 * inherently transactional. External synchronization may be required if multiple
 * processes access the same directory.</li>
 * <li><b>Scalability:</b> Operations like {@link #findAllSync} or {@link #iterator()}
 * rely on {@link Files#list(Path)}, scanning the entire directory. Performance scales
 * linearly <i>O(n)</i> and may degrade with tens of thousands of files.</li>
 * </ul>
 *
 * @param <T> the type of the {@link AggregateRoot} being persisted.
 * @author team.emptyte
 * @since 0.1.0
 */
public class YamlAggregateRootRepository<T extends AggregateRoot> extends AsyncAggregateRootRepository<T> {
  private static final String FILE_EXTENSION = ".yml";
  private static final String FILE_FORMAT = "%s" + FILE_EXTENSION;

  private final Class<T> aggregateType;
  private final Path folderPath;
  private final boolean prettyPrinting;
  private final UnaryOperator<ConfigurationOptions> defaultOptions;

  /**
   * Initializes the repository.
   * <p>
   * If the specified {@code folderPath} does not exist, this constructor attempts to create it.
   * </p>
   *
   * @param executor       The task runner for asynchronous operations.
   * @param aggregateType  The class of the aggregate root (required for Configurate mapping).
   * @param folderPath     The root directory for YAML storage.
   * @param prettyPrinting If {@code true}, the YAML will be formatted with {@link NodeStyle#BLOCK};
   *                       otherwise, it uses {@link NodeStyle#FLOW} (more compact but harder to read).
   * @param typeSerializer The Configurate serializer used to map domain objects to nodes.
   * @throws RuntimeException if the directory creation fails.
   */
  YamlAggregateRootRepository(
    final @NotNull Executor executor,
    final @NotNull Class<T> aggregateType,
    final @NotNull Path folderPath,
    final boolean prettyPrinting,
    final @NotNull TypeSerializer<T> typeSerializer
  ) {
    super(executor);
    this.aggregateType = aggregateType;
    this.prettyPrinting = prettyPrinting;
    this.folderPath = folderPath;
    this.defaultOptions = configurationOptions -> configurationOptions.serializers(
      TypeSerializerCollection.builder()
        .register(aggregateType, typeSerializer)
        .build()
    );

    if (Files.notExists(folderPath)) {
      try {
        Files.createDirectories(folderPath);
      } catch (final Exception exception) {
        throw new RuntimeException("Failed to create directory: " + folderPath, exception);
      }
    }
  }

  /**
   * Creates a builder to configure and instantiate the repository.
   *
   * @return a new instance of {@link YamlAggregateRootRepositoryBuilder}.
   */
  public @NotNull YamlAggregateRootRepositoryBuilder<T> builder() {
    return new YamlAggregateRootRepositoryBuilder<>();
  }

  /**
   * Formats the file name for a given aggregate ID by appending the YAML extension.
   *
   * @param id the identifier of the aggregate.
   * @return the formatted file name (e.g., "id.yml").
   */
  public @NotNull String formatFileName(final @NotNull String id) {
    return String.format(FILE_FORMAT, id);
  }

  /**
   * Resolves the full file path for a given aggregate ID within the repository's folder.
   *
   * @param id the identifier of the aggregate.
   * @return the absolute path to the YAML file.
   */
  public @NotNull Path filePath(final @NotNull String id) {
    return this.folderPath.resolve(this.formatFileName(id));
  }

  /**
   * Creates a {@link YamlConfigurationLoader} for the specified aggregate ID.
   *
   * @param id the identifier of the aggregate.
   * @return a configured loader for the aggregate's file.
   */
  public @NotNull YamlConfigurationLoader loader(final @NotNull String id) {
    return this.loader(this.filePath(id));
  }

  /**
   * Creates a {@link YamlConfigurationLoader} for the specified file path.
   *
   * @param path the path to the YAML file.
   * @return a configured loader with the repository's default options and node style.
   */
  public @NotNull YamlConfigurationLoader loader(final @NotNull Path path) {
    return YamlConfigurationLoader.builder()
      .path(path)
      .defaultOptions(this.defaultOptions)
      .nodeStyle(this.prettyPrinting ? NodeStyle.BLOCK : NodeStyle.FLOW)
      .build();
  }

  /**
   * Loads the raw configuration node for a given aggregate ID.
   *
   * @param id the identifier of the aggregate.
   * @return the loaded {@link CommentedConfigurationNode}.
   * @throws RuntimeException if an error occurs during the load operation.
   */
  public @NotNull CommentedConfigurationNode loadNode(final @NotNull String id) {
    try {
      return this.loader(id).load();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Loads the raw configuration node for a given file path.
   *
   * @param path the path to the YAML file.
   * @return the loaded {@link CommentedConfigurationNode}.
   * @throws RuntimeException if an error occurs during the load operation.
   */
  public @NotNull CommentedConfigurationNode loadNode(final @NotNull Path path) {
    try {
      return this.loader(path).load();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Uses Configurate's object mapping to deserialize the node into type {@code T}.
   */
  public @Nullable T loadSync(final @NotNull String id) {
    try {
      return this.loadNode(id)
        .get(this.aggregateType);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Synchronously loads and deserializes an aggregate root from a specific path.
   *
   * @param path the path to the YAML file.
   * @return the loaded aggregate root, or {@code null} if deserialization fails.
   * @throws RuntimeException if an error occurs during loading or deserialization.
   */
  public @Nullable T loadSync(final @NotNull Path path) {
    try {
      return this.loadNode(path)
        .get(this.aggregateType);
    } catch (final Exception e) {
      throw new RuntimeException(e);
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
      return Files.deleteIfExists(this.filePath(id));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Uses {@link Files#list(Path)} to iterate over the directory.
   * Note that this operation may throw a runtime exception if a file cannot be deleted.
   */
  @Override
  public void deleteAllSync() {
    try (final Stream<Path> paths = Files.list(this.folderPath)) {
      paths.filter(path -> path.toString().endsWith(FILE_EXTENSION))
        .forEach(path -> {
          try {
            Files.deleteIfExists(path);
          } catch (final IOException e) {
            throw new RuntimeException(e);
          }
        });
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public @Nullable T deleteAndRetrieveSync(final @NotNull String id) {
    final T aggregateType = this.loadSync(id);
    if (aggregateType != null) {
      this.deleteSync(id);
    }
    return aggregateType;
  }

  @Override
  public boolean existsSync(final @NotNull String id) {
    return Files.exists(this.filePath(id));
  }

  @Override
  public @Nullable T findSync(final @NotNull String id) {
    return this.loadSync(id);
  }

  @Override
  public <C extends Collection<@NotNull T>> @NotNull C findAllSync(final @NotNull Consumer<@NotNull T> postLoadAction, final @NotNull IntFunction<@NotNull C> factory) {
    final C foundModels = factory.apply(1);
    this.forEach(model -> {
      postLoadAction.accept(model);
      foundModels.add(model);
    });
    return foundModels;
  }

  /**
   * {@inheritDoc}
   *
   * @implNote This method iterates physically over the files in the directory.
   */
  @Override
  public void forEach(final @NotNull Consumer<? super T> action) {
    try (final Stream<Path> paths = Files.list(this.folderPath)) {
      paths.filter(path -> path.toString().endsWith(FILE_EXTENSION))
        .forEach(path -> {
          final T aggregateType = this.loadSync(path);
          if (aggregateType != null) {
            action.accept(aggregateType);
          }
        });
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public @NonNull <C extends Collection<@NotNull String>> C findIdsSync(final @NotNull IntFunction<C> factory) {
    try (final Stream<Path> paths = Files.list(this.folderPath)) {
      final C foundIds = factory.apply(1);
      paths.filter(path -> path.toString().endsWith(FILE_EXTENSION))
        .map(path -> path.getFileName().toString().replace(FILE_EXTENSION, ""))
        .forEach(foundIds::add);
      return foundIds;
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @implNote <b>Memory Warning:</b> This implementation first loads all aggregate IDs
   * into a {@link HashSet} in memory using {@link #findIdsSync}, then iterates over them.
   * In environments with a massive number of files, this could impact memory usage.
   */
  @Override
  public @NotNull Iterator<T> iterator() {
    return new Iterator<>() {
      private final Iterator<String> ids = findIdsSync(HashSet::new).iterator();

      @Override
      public boolean hasNext() {
        return this.ids.hasNext();
      }

      @Override
      public T next() {
        return findSync(this.ids.next());
      }
    };
  }

  @Override
  public @NotNull Iterator<@NotNull String> iteratorIds() {
    return this.findIdsSync()
      .iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Uses {@link YamlConfigurationLoader#save(org.spongepowered.configurate.ConfigurationNode)}
   * to write the aggregate. The output style (Block vs Flow) is determined by the
   * {@code prettyPrinting} flag set in the constructor.
   */
  @Override
  public @NotNull T saveSync(@NotNull final T aggregateType) {
    try {
      final YamlConfigurationLoader loader = this.loader(aggregateType.id());
      loader.save(loader.createNode().set(this.aggregateType, aggregateType));
      return aggregateType;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
