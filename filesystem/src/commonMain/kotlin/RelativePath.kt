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

import kotlin.jvm.JvmInline
import kotlin.math.min
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.append
import kotlinx.io.bytestring.buildByteString
import kotlinx.io.bytestring.isNotEmpty
import me.zhanghai.kotlin.filesystem.internal.compareTo
import me.zhanghai.kotlin.filesystem.internal.contains
import me.zhanghai.kotlin.filesystem.internal.endsWith
import me.zhanghai.kotlin.filesystem.internal.startsWith

/**
 * A relative hierarchical identifier that can be resolved against a path.
 *
 * A relative path consists of a list of names, and each name is a non-empty byte string that
 * doesn't contain the URI path separator `/`.
 *
 * A relative path is an immutable data object independent of file systems, and can be converted
 * to/from a byte string for presentation and serialization.
 *
 * @see Path
 */
@JvmInline
public value class RelativePath
private constructor(
    /** The list of names of this relative path. */
    public val names: List<ByteString>
) : Comparable<RelativePath> {
    /** The file name of this relative path, which is the last name in the list of names. */
    public val fileName: ByteString?
        get() = names.lastOrNull()

    /**
     * Get a relative path for the parent of this relative path, or `null` if this relative path is
     * empty.
     *
     * @return a relative path for the parent of this relative path, or `null`
     */
    public fun getParent(): RelativePath? {
        val lastIndex = names.lastIndex
        return if (lastIndex >= 0) RelativePath(names.subList(0, lastIndex)) else null
    }

    /**
     * Get a relative path consisting of a sublist of names of this relative path.
     *
     * @param startIndex the index of the first name, inclusive
     * @param endIndex the index of the last name, exclusive
     * @return a relative path consisting of a sublist of names of this relative path
     */
    public fun subPath(startIndex: Int, endIndex: Int): RelativePath =
        if (startIndex == 0 && endIndex == names.size) {
            this
        } else {
            RelativePath(names.subList(startIndex, endIndex))
        }

    /**
     * Check whether this relative path starts with another relative path.
     *
     * This relative path starts with the other relative path if the list of names of this relative
     * path starts with the list of names in the other relative path.
     *
     * @param other the other relative path
     * @return whether this relative path starts with another relative path
     */
    public fun startsWith(other: RelativePath): Boolean = names.startsWith(other.names)

    /**
     * Check whether this relative path ends with another relative path.
     *
     * This relative path ends with the other relative path if the list of names of this relative
     * path ends with the list of names in the other relative path.
     *
     * @param other the other relative path
     * @return whether this relative path ends with another relative path
     */
    public fun endsWith(other: RelativePath): Boolean = names.endsWith(other.names)

    /**
     * Get a relative path that is this relative path with redundant names removed.
     *
     * Redundant names include `.` which refers to the current directory and can be removed, and
     * `..` which refers to the parent directory and can be removed together with its preceding name
     * when there is one.
     *
     * @return a relative path that is this relative path with redundant names removed.
     */
    public fun normalize(): RelativePath {
        var newNames: MutableList<ByteString>? = null
        for ((index, name) in names.withIndex()) {
            when (name) {
                Path.NAME_DOT ->
                    if (newNames == null) {
                        newNames = names.subList(0, index).toMutableList()
                    }
                Path.NAME_DOT_DOT ->
                    if (newNames != null) {
                        when (newNames.lastOrNull()) {
                            null,
                            Path.NAME_DOT_DOT -> newNames += name
                            else -> newNames.removeLast()
                        }
                    } else {
                        when (names.getOrNull(index - 1)) {
                            null,
                            Path.NAME_DOT_DOT -> {}
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
        return RelativePath(newNames)
    }

    /**
     * Resolve a file name against this relative path.
     *
     * The file name is resolved by appending it to the list of names of this relative path.
     *
     * @param fileName the file name
     * @return a relative path that is the file name resolved against this relative path
     * @throws IllegalArgumentException if the file name is empty or contains the URI name separator
     *   `/`
     */
    public fun resolve(fileName: ByteString): RelativePath {
        require(fileName.isNotEmpty()) { "Cannot resolve an empty file name" }
        require(Path.NAME_SEPARATOR_BYTE !in fileName) {
            "Name separator in file name \"$fileName\""
        }
        return RelativePath(names + fileName)
    }

    /**
     * Resolve another relative path against this relative path.
     *
     * The other relative path is resolved by appending the list of names of it to the list of names
     * of this relative path.
     *
     * @param other the other relative path
     * @return a relative path that is the other relative path resolved against this relative path
     * @see relativize
     */
    public fun resolve(other: RelativePath): RelativePath =
        if (other.names.isEmpty()) this else RelativePath(names + other.names)

    /**
     * Resolve a file name against the parent of this relative path.
     *
     * The file name is resolved by replacing the last name of this relative path with it.
     *
     * @param fileName the file name
     * @return a path that is the file name resolved against the parent of this relative path
     * @throws IllegalStateException if this relative path is empty.
     * @throws IllegalArgumentException if the file name is empty or contains the URI name separator
     *   `/`
     * @see getParent
     * @see resolve
     */
    public fun resolveSibling(fileName: ByteString): RelativePath {
        check(names.isNotEmpty()) { "Cannot resolve sibling of an empty relative path" }
        require(fileName.isNotEmpty()) { "Cannot resolve an empty file name" }
        require(Path.NAME_SEPARATOR_BYTE !in fileName) {
            "Name separator in file name \"$fileName\""
        }
        return RelativePath(names.toMutableList().apply { set(lastIndex, fileName) })
    }

    /**
     * Get a relative path between this relative path and another relative path.
     *
     * The relative path is created by replacing each name in the common prefix of the two relative
     * paths with `..` in the other relative path, so that for two normalized relative paths `p` and
     * `q`, `p.resolve(p.relativize(q)).normalize() == q`.
     *
     * @param other the other relative path
     * @return a relative path between this relative path and the other relative path
     * @see resolve
     */
    public fun relativize(other: RelativePath): RelativePath {
        if (names.isEmpty()) {
            return other
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
                repeat(namesSize - commonNamesSize) { this += Path.NAME_DOT_DOT }
                this += other.names.subList(commonNamesSize, otherNamesSize)
            }
        return RelativePath(newNames)
    }

    /**
     * Get a byte string representing this relative path.
     *
     * The byte string is created by joining the list of names of this relative path with the URI
     * path separator `/`.
     *
     * @return the byte string representing this relative path
     */
    public fun toByteString(): ByteString {
        var decodedLength = 0
        var isFirst = true
        for (name in names) {
            if (isFirst) {
                isFirst = false
            } else {
                decodedLength += 1
            }
            decodedLength += name.size
        }
        return buildByteString(decodedLength) {
            isFirst = true
            for (name in names) {
                if (isFirst) {
                    isFirst = false
                } else {
                    append(Path.NAME_SEPARATOR_BYTE)
                }
                append(name)
            }
        }
    }

    override fun compareTo(other: RelativePath): Int = names.compareTo(other.names)

    override fun toString(): String = "RelativePath(names=$names)"

    public companion object {
        /**
         * Create a new relative path from a list of names.
         *
         * @param names the list of names
         * @return the new relative path
         * @throws IllegalArgumentException if any of the names is empty or contains the URI name
         *   separator `/`
         */
        public operator fun invoke(names: List<ByteString>): RelativePath {
            for (name in names) {
                require(name.isNotEmpty()) { "Empty name in relative path name \"$name\$" }
                require(Path.NAME_SEPARATOR_BYTE !in name) {
                    "Name separator in relative path name \"$name\""
                }
            }
            return RelativePath(names)
        }

        /**
         * Create a new relative path from a byte string.
         *
         * @param byteString the byte string
         * @return the new relative path
         */
        public fun fromByteString(byteString: ByteString): RelativePath {
            val names = buildList {
                val pathSize = byteString.size
                var nameStart = 0
                var nameEnd = nameStart
                while (nameEnd < pathSize) {
                    if (byteString[nameEnd] == Path.NAME_SEPARATOR_BYTE) {
                        if (nameEnd != nameStart) {
                            this += byteString.substring(nameStart, nameEnd)
                        }
                        nameStart = nameEnd + 1
                        nameEnd = nameStart
                    } else {
                        ++nameEnd
                    }
                }
                if (nameEnd != nameStart) {
                    this += byteString.substring(nameStart, nameEnd)
                }
            }
            return RelativePath(names)
        }
    }
}
