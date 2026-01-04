/**
 * <b>Data Transformation and Serialization Abstractions.</b>
 * <p>
 * This module ("codec") provides the functional interfaces required to bridge the gap between
 * high-level Domain Objects and low-level Data Formats.
 * </p>
 *
 * <h2>Module Contents</h2>
 * <ul>
 * <li><b>Serializers:</b> Interfaces to transform an Aggregate Root into a raw format (e.g., JSON, Bytes).</li>
 * <li><b>Deserializers:</b> Interfaces to reconstitute an Aggregate Root from stored data.</li>
 * </ul>
 *
 * <h2>Architectural Role</h2>
 * <p>
 * Acts as a <b>Data Mapper</b> definition layer. It allows the storage infrastructure to function
 * without knowing the specific internal structure of your business objects, promoting encapsulation.
 * </p>
 *
 * @author team.emptyte
 * @since 0.1.0
 */
package team.emptyte.storage.codec;
