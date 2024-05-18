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
import me.zhanghai.kotlin.filesystem.internal.startsWith

public class Path private constructor(public val rootUri: Uri, public val names: List<ByteString>) :
    Comparable<Path> {
    @Volatile public var fileSystemTag: Any? = null

    public val scheme: String
        get() = rootUri.scheme!!

    public val fileName: ByteString?
        get() = names.lastOrNull()

    public fun getRoot(): Path = if (names.isEmpty()) this else Path(rootUri, emptyList())

    public fun getParent(): Path? {
        val lastIndex = names.lastIndex
        return if (lastIndex >= 0) Path(rootUri, names.subList(0, lastIndex)) else null
    }

    public fun subPath(startIndex: Int, endIndex: Int): Path =
        if (startIndex == 0 && endIndex == names.size) {
            this
        } else {
            Path(rootUri, names.subList(startIndex, endIndex))
        }

    public fun startsWith(other: Path): Boolean {
        if (rootUri != other.rootUri) {
            return false
        }
        return names.startsWith(other.names)
    }

    public fun endsWith(other: Path): Boolean {
        if (rootUri != other.rootUri) {
            return false
        }
        return names == other.names
    }

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

    public fun resolve(fileName: ByteString): Path {
        require(fileName.isNotEmpty()) { "Cannot resolve an empty file name" }
        return Path(rootUri, names + fileName)
    }

    public fun resolve(other: RelativePath): Path =
        if (other.names.isEmpty()) this else Path(rootUri, names + other.names)

    public fun resolveSibling(fileName: ByteString): Path {
        require(fileName.isNotEmpty()) { "Cannot resolve an empty file name" }
        check(names.isNotEmpty()) { "Cannot resolve sibling of a root path" }
        return Path(rootUri, names.toMutableList().apply { set(lastIndex, fileName) })
    }

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
        val newNames = names.subList(0, commonNamesSize).toMutableList()
        repeat(namesSize - commonNamesSize) { newNames += NAME_DOT_DOT }
        newNames += other.names.subList(commonNamesSize, otherNamesSize)
        return RelativePath(newNames)
    }

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
