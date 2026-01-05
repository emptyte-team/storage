# 📦 Storage

[![License](https://img.shields.io/github/license/emptyte-team/storage?style=flat-square)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange?style=flat-square)](https://www.java.com/)
[![Architecture](https://img.shields.io/badge/Architecture-DDD-blueviolet?style=flat-square)](https://martinfowler.com/tags/domain%20driven%20design.html)

> **The bridge between your Domain and your Data.**

**Storage** is a Java library built to simplify the implementation of the **Repository Pattern**. It allows developers to integrate persistence into **Domain-Driven Design (DDD)** architectures without coupling the business logic to specific database technologies.

With **Storage**, you define *what* needs to be saved in your Domain Layer and let this library handle *how* it is saved in the Infrastructure Layer.

## 🎯 Why use this library?

* **DDD Friendly:** Keeps your Domain Entities pure and your Repository Interfaces clean.
* **Infrastructure Agnostic:** Switch between Flat Files (JSON/YAML) or InMemory (Map/Caffeine) storage without changing a single line of business logic.
* **Decoupled:** Acts as an adapter layer, preventing database dependencies from leaking into your application core.
* **Simple API:** Provides a fluent and consistent API for standard CRUD operations (Sync and Async).

## 📦 Installation

Currently, this library is in development. To use it, you must select the **distribution module** that fits your needs (e.g., Gson, Caffeine).

### 1. Clone and Install (Local Mode)

Since it is not yet on Maven Central, you must build it locally:

```bash
# 1. Clone the repository
git clone https://github.com/emptyte-team/storage.git

# 2. Navigate to the folder
cd storage

# 3. Publish to your local Maven repository
./gradlew publishToMavenLocal
```

### 2. Add to your Project

Once installed locally, add `mavenLocal()` to your repositories and include the dependency for the specific implementation you want to use.

> **⚠️ Important:** Do not depend on the core directly. Choose a distribution artifact ending in `-dist`.

#### Gradle (Kotlin DSL)

```kotlin
repositories {
  mavenLocal() // Important: Look in the local .m2 folder first
  mavenCentral()
}

dependencies {
  // Replace 'TYPE' with the storage implementation you need (e.g., gson-dist)
  // Replace 'VERSION' with the current version (e.g., 0.1.0)

  implementation("team.emptyte:storage-gson-dist:VERSION")
  // or
  implementation("team.emptyte:storage-caffeine-dist:VERSION")
}
```

#### Maven

```maven
<repositories>
    <repository>
        <id>local-maven</id>
        <url>file://${user.home}/.m2/repository</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>team.emptyte</groupId>
        <artifactId>storage-gson-dist</artifactId> 
        <version>VERSION</version>
    </dependency>
</dependencies>
```

# 🛠️ Usage with DDD

Here is how Storage fits into a layered architecture:

## 1. Domain Layer

Defines the rules. No libraries or frameworks here (except simple annotations if needed).

```java
import team.emptyte.storage.domain.AggregateRoot;

public class UserAggregateRoot extends AggregateRoot {
  private final String name;

  public UserAggregateRoot(final String id, final String name) {
    super(id);
    this.name = name;
  }

  public String name() {
    return this.name;
  }
}
```

## 2. Service Layer

Orchestrates logic using the Repository Interface provided by Storage.

```java
import repository.team.emptyte.storage.domain.AsyncAggregateRootRepository;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UserService {
  private final AsyncAggregateRootRepository<UserAggregateRoot> repository;

  // Dependency Injection via Constructor
  public UserService(final AsyncAggregateRootRepository<UserAggregateRoot> repository) {
    this.repository = repository;
  }

  public CompletableFuture<Void> create(final String name) {
    final String id = UUID.randomUUID().toString();
    final UserAggregateRoot user = new UserAggregateRoot(id, name);

    return this.repository.saveAsync(user)
      .thenAccept(savedUser -> System.out.println("User created: " + savedUser.name()))
      .exceptionally(ex -> {
        System.err.println("Error creating user: " + ex.getMessage());
        return null;
      });
  }
}
```

## 3. Entry Point (Main / Infrastructure)

This is where you decide which implementation to use (Gson, YAML, etc.) and inject dependencies.

```java
// Example import assuming you used 'storage-gson-dist'

import repository.team.emptyte.storage.domain.AsyncAggregateRootRepository;
import team.emptyte.storage.infrastructure.gson.GsonAggregateRootRepository;
import codec.team.emptyte.storage.codec.TypeAdapter;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executor;

public class Main {
  static void main(final String[] args) {
    // 1. Instantiate the concrete repository (depends on the -dist library you imported)
    // Hypothetical example using Gson:
    final Executor executor = Executors.newSingleThreadExecutor();
    final Path folder = Paths.get("data");

    final AsyncAggergateRootRespository<UserAggregateRoot> repository = GsonAggregateRootRepository.<UserAggregateRoot>builder()
      .folder(folder)
      .prettyPrinting(true)
      .typeAdapter(new TypeAdapter<UserAggregateRoot, JsonObject>() {
        public @NotNull JsonObject write(final @NotNull UserAggregateRoot value) {
          final JsonObject serialized = new JsonObject();
          serialized.addProperty("id", value.id());
          serialized.addProperty("name", value.name());
          return serialized;
        }

        public @NotNull UserAggregateRoot read(final @NotNull JsonObject value) {
          final String id = value.get("id").getAsString();
          final String name = value.get("name").getAsString();
          return new UserAggregateRoot(id, name);
        }
      })
      .build(executor);

    // 2. Inject a repository into the service
    final UserService userService = new UserService(repository);

    // 3. Run logic
    userService.create("Nelson Rodriguez");
  }
}
```

# 🤝 Contribution

Contributions are welcome! If you want to add support for a new database type (e.g., storage-redis-dist) or improve the core logic:

1. Fork the repository.
2. Create your feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature`').
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

# 📄 License

Distributed under the MIT License. See [LICENSE](https://github.com/emptyte-team/storage/blob/main/LICENSE) for more information.
