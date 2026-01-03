/**
 * <b>Core definitions and Domain Contracts for the Storage Library.</b>
 * <p>
 * This module ("api") serves as the foundation of the library, establishing the
 * <b>Domain-Driven Design (DDD)</b> building blocks required to persist data.
 * </p>
 *
 * <h2>Module Contents</h2>
 * <ul>
 * <li><b>Domain Models:</b> Base classes for {@code AggregateRoot} and Entities.</li>
 * <li><b>Repository Contracts:</b> Interfaces defining standard CRUD and query operations (Sync & Async).</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>
 * Other modules depend on this package to implement specific storage mechanisms. Applications
 * should use the types defined here to ensure their Domain Layer remains decoupled from infrastructure.
 * </p>
 *
 * @author team.emptyte
 * @since 0.1.0
 */
package team.emptyte.storage;
