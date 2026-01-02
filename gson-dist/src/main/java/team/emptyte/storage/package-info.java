/**
 * <b>Gson-based Infrastructure Implementation.</b>
 * <p>
 * This module ("gson-dist") provides a concrete implementation of the storage system,
 * using <b>Google Gson</b> to persist aggregates as JSON files on the local file system.
 * </p>
 *
 * <h2>Module Contents</h2>
 * <ul>
 * <li><b>Gson Repository:</b> A file-system backed repository implementation.</li>
 * <li><b>File Management:</b> Logic for reading, writing, and locking JSON files.</li>
 * </ul>
 *
 * <h2>Integration</h2>
 * <p>
 * This module combines the contracts from {@code common} and the codecs from {@code codec}
 * to provide a ready-to-use storage solution. It is intended to be injected as a dependency
 * in the Infrastructure Layer of your application.
 * </p>
 *
 * @author team.emptyte
 * @since 0.0.1
 * @see com.google.gson.Gson
 */
package team.emptyte.storage;
