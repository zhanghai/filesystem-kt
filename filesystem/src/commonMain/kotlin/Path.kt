/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.zhanghai.kotlin.filesystem

import kotlin.concurrent.Volatile
import kotlin.math.min
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.append
import kotlinx.io.bytestring.buildByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.bytestring.isNotEmpty
import me.zhanghai.kotlin.filesystem.internal.compareTo
import me.zhanghai.kotlin.filesystem.internal.contains
import me.zhanghai.kotlin.filesystem.internal.endsWith
import me.zhanghai.kotlin.filesystem.internal.startsWith

/**
 * A hierarchical identifier for a file in a file system.
 *
 * A path consists of a root URI and a list of names. The root URI identifies the root of the file
 * hierarchy, including the file system which is identified by the scheme of the root URI. The list
 * of names identifies the file within its file hierarchy, and each name is a non-empty byte string
 * that doesn't contain the URI path separator `/`.
 *
 * As such, a path is always absolute, and a relative identifier consisting of only a list of names
 * is represented by a [RelativePath].
 *
 * A path is an immutable data object independent of file systems, and can be converted to/from a
 * URI for presentation and serialization.
 *
 * @see FileSystem
 * @see RelativePath
 */
public class Path
private constructor(
    /** The root URI of this path. */
    public val rootUri: Uri,

    /** The list of names of this path. */
    public val names: List<ByteString>,
) : Comparable<Path> {
    /**
     * A tag for use by the implementation of the file system identified by this path.
     *
     * Code outside of the file system implementation should ignore and never access this property.
     */
    @Volatile public var fileSystemTag: Any? = null

    /** The scheme of the root URI of this path. */
    public val scheme: String
        get() = rootUri.scheme!!

    /** The file name of this path, which is the last name in the list of names. */
    public val fileName: ByteString?
        get() = names.lastOrNull()

    /**
     * Get a path for the root of the file hierarchy this path is identifying.
     *
     * @return a path for the root of the file hierarchy
     */
    public fun getRoot(): Path = if (names.isEmpty()) this else Path(rootUri, emptyList())

    /**
     * Get a path for the parent of the file this path is identifying, or `null` if this path
     * identifies the root of its file hierarchy.
     *
     * @return a path for the parent of the file, or `null`
     */
    public fun getParent(): Path? {
        val lastIndex = names.lastIndex
        return if (lastIndex >= 0) Path(rootUri, names.subList(0, lastIndex)) else null
    }

    /**
     * Get a path consisting of a sublist of names of this path.
     *
     * @param endIndex the index of the last name, exclusive
     * @return a path consisting of a sublist of names of this path
     */
    public fun subPath(endIndex: Int): Path =
        if (endIndex == names.size) {
            this
        } else {
            Path(rootUri, names.subList(0, endIndex))
        }

    /**
     * Get a relative path consisting of a sublist of names of this path.
     *
     * @param startIndex the index of the first name, inclusive
     * @param endIndex the index of the last name, exclusive
     * @return a relative path consisting of a sublist of names of this path
     */
    public fun toRelativePath(startIndex: Int = 0, endIndex: Int = names.size): RelativePath =
        RelativePath(names.subList(startIndex, endIndex))

    /**
     * Check whether this path starts with another path.
     *
     * This path starts with the other path if they have the same root URI, and the list of names of
     * this path starts with the list of names in the other path.
     *
     * @param other the other path
     * @return whether this path starts with another path
     */
    public fun startsWith(other: Path): Boolean =
        rootUri == other.rootUri && names.startsWith(other.names)

    /**
     * Check whether this path ends with a relative path.
     *
     * This path ends with the relative path if the list of names of this path ends with the list of
     * names in the relative path.
     *
     * @param other the relative path
     * @return whether this path ends with the relative path
     */
    public fun endsWith(other: RelativePath): Boolean = names.endsWith(other.names)

    /**
     * Get a path that is this path with redundant names removed.
     *
     * Redundant names include `.` which refers to the current directory and can be removed, and
     * `..` which refers to the parent directory and can be removed together with its preceding name
     * (if any).
     *
     * This method does not access the file system, so the returned path may identify a different
     * file than the this path after removing `..` and its preceding name, for example when the
     * preceding name is a symbolic link.
     *
     * @return a path that is this path with redundant names removed.
     */
    public fun normalize(): Path {
        var newNames: MutableList<ByteString>? = null
        for ((index, name) in names.withIndex()) {
            when (name) {
                NAME_DOT ->
                    if (newNames == null) {
                        newNames = names.subList(0, index).toMutableList()
                    }
                NAME_DOT_DOT ->
                    if (newNames != null) {
                        when (newNames.lastOrNull()) {
                            null -> {}
                            NAME_DOT_DOT -> newNames += name
                            else -> newNames.removeLast()
                        }
                    } else {
                        when (names.getOrNull(index - 1)) {
                            null -> newNames = mutableListOf()
                            NAME_DOT_DOT -> {}
                            else -> newNames = names.subList(0, index - 1).toMutableList()
                        }
                    }
                else ->
                    if (newNames != null) {
                        newNames += name
                    }
            }
        }
        if (newNames == null) {
            return this
        }
        return Path(rootUri, newNames)
    }

    /**
     * Resolve a file name against this path.
     *
     * The file name is resolved by appending it to the list of names of this path.
     *
     * @param fileName the file name
     * @return a path that is the file name resolved against this path
     * @throws IllegalArgumentException if the file name is empty or contains the URI name separator
     *   `/`
     */
    public fun resolve(fileName: ByteString): Path {
        require(fileName.isNotEmpty()) { "Cannot resolve an empty file name" }
        require(NAME_SEPARATOR_BYTE !in fileName) { "Name separator in file name \"$fileName\"" }
        return Path(rootUri, names + fileName)
    }

    /**
     * Resolve a relative path against this path.
     *
     * The relative path is resolved by appending the list of names of it to the list of names of
     * this path.
     *
     * @param other the relative path
     * @return a path that is the relative path resolved against this path
     * @see relativize
     */
    public fun resolve(other: RelativePath): Path =
        if (other.names.isEmpty()) this else Path(rootUri, names + other.names)

    /**
     * Resolve a file name against the parent of this path.
     *
     * The file name is resolved by replacing the last name of this path with it.
     *
     * @param fileName the file name
     * @return a path that is the file name resolved against the parent of this path
     * @throws IllegalStateException if this path identifies the root of its file hierarchy.
     * @throws IllegalArgumentException if the file name is empty or contains the URI name separator
     *   `/`
     * @see getParent
     * @see resolve
     */
    public fun resolveSibling(fileName: ByteString): Path {
        check(names.isNotEmpty()) { "Cannot resolve sibling of a root path" }
        require(fileName.isNotEmpty()) { "Cannot resolve an empty file name" }
        require(NAME_SEPARATOR_BYTE !in fileName) { "Name separator in file name \"$fileName\"" }
        return Path(rootUri, names.toMutableList().apply { set(lastIndex, fileName) })
    }

    /**
     * Get a relative path between this path and another path.
     *
     * The relative path is created by replacing each name in the common prefix of the two paths
     * with `..` in the other path, so that for two normalized paths `p` and `q`,
     * `p.resolve(p.relativize(q)).normalize() == q`.
     *
     * This method does not access the file system, so the path obtained by resolving the return
     * relative path against this path may identify a different file than the other path, for
     * example when a name after the common prefix of the two paths in this path is a symbolic link.
     *
     * @param other the other path
     * @return a relative path between this path and the other path
     * @throws IllegalArgumentException if the other path doesn't have the same root URI as this
     *   path
     * @see resolve
     */
    public fun relativize(other: Path): RelativePath {
        require(rootUri != other.rootUri) { "Cannot relativize paths with different root URIs " }
        if (names.isEmpty()) {
            return RelativePath(other.names)
        }
        val namesSize = names.size
        val otherNamesSize = other.names.size
        val minNamesSize = min(namesSize, otherNamesSize)
        var commonNamesSize = 0
        while (commonNamesSize < minNamesSize) {
            if (names[commonNamesSize] != other.names[commonNamesSize]) {
                break
            }
            ++commonNamesSize
        }
        val newNames =
            buildList<ByteString> {
                repeat(namesSize - commonNamesSize) { this += NAME_DOT_DOT }
                this += other.names.subList(commonNamesSize, otherNamesSize)
            }
        return RelativePath(newNames)
    }

    /**
     * Get a URI representing this path.
     *
     * The URI is created by replacing the path component of the root URI of this path with the list
     * of names of this path joined with the URI path separator `/`.
     *
     * @return the URI representing this path
     */
    public fun toUri(): Uri {
        val decodedPath = buildByteString {
            for (name in names) {
                append(NAME_SEPARATOR_BYTE)
                append(name)
            }
        }
        return rootUri.copyDecoded(decodedPath = decodedPath)
    }

    override fun compareTo(other: Path): Int {
        rootUri.compareTo(other.rootUri).let {
            if (it != 0) {
                return it
            }
        }
        return names.compareTo(other.names)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || this::class != other::class) {
            return false
        }
        other as Path
        return rootUri == other.rootUri && names == other.names
    }

    override fun hashCode(): Int {
        var result = rootUri.hashCode()
        result = 31 * result + names.hashCode()
        return result
    }

    override fun toString(): String = "Path(rootUri=$rootUri, names=$names)"

    public companion object {
        internal val NAME_DOT = ".".encodeToByteString()
        internal val NAME_DOT_DOT = "..".encodeToByteString()
        private const val NAME_SEPARATOR_CHAR = '/'
        internal const val NAME_SEPARATOR_BYTE = NAME_SEPARATOR_CHAR.code.toByte()
        private const val NAME_SEPARATOR_STRING = "/"

        /**
         * Create a new path from a root URI and a list of names.
         *
         * @param rootUri the root URI
         * @param names the list of names
         * @return the new path
         * @throws IllegalArgumentException if the root URI doesn't have a scheme, or the path
         *   component of the root URI isn't `/`, or any of the names is empty or contains the URI
         *   name separator `/`
         */
        public operator fun invoke(rootUri: Uri, names: List<ByteString>): Path {
            requireNotNull(rootUri.scheme) { "Missing scheme in path root URI \"$rootUri\"" }
            require(rootUri.encodedPath == NAME_SEPARATOR_STRING) {
                "Path is not root path in path root URI \"$rootUri\""
            }
            for (name in names) {
                require(name.isNotEmpty()) { "Empty name in path name \"$name\$" }
                require(NAME_SEPARATOR_BYTE !in name) { "Name separator in path name \"$name\"" }
            }
            return Path(rootUri, names)
        }

        /**
         * Create a new path from a URI.
         *
         * @param uri the URI
         * @return the new path
         * @throws IllegalArgumentException if the URI doesn't have a scheme, or the path component
         *   of the URI is empty or relative.
         */
        public fun fromUri(uri: Uri): Path {
            requireNotNull(uri.scheme) { "Missing scheme in path URI \"$uri\"" }
            val encodedPath = uri.encodedPath
            require(encodedPath.isNotEmpty()) { "Empty path in path URI \"$uri\"" }
            require(encodedPath[0] == NAME_SEPARATOR_CHAR) { "Relative path in path URI \"$uri\"" }
            val rootUri = uri.copyEncoded(encodedPath = NAME_SEPARATOR_STRING)
            val names = buildList {
                val decodedPath = uri.decodedPath
                val decodedPathSize = decodedPath.size
                var nameStart = 0
                var nameEnd = nameStart
                while (nameEnd < decodedPathSize) {
                    if (decodedPath[nameEnd] == NAME_SEPARATOR_BYTE) {
                        if (nameEnd != nameStart) {
                            this += decodedPath.substring(nameStart, nameEnd)
                        }
                        nameStart = nameEnd + 1
                        nameEnd = nameStart
                    } else {
                        ++nameEnd
                    }
                }
                if (nameEnd != nameStart) {
                    this += decodedPath.substring(nameStart, nameEnd)
                }
            }
            return Path(rootUri, names)
        }
    }
}
