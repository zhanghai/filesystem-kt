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

@JvmInline
public value class RelativePath private constructor(public val names: List<ByteString>) :
    Comparable<RelativePath> {
    public val fileName: ByteString?
        get() = names.lastOrNull()

    public fun getParent(): RelativePath? {
        val lastIndex = names.lastIndex
        return if (lastIndex >= 0) RelativePath(names.subList(0, lastIndex)) else null
    }

    public fun subPath(startIndex: Int, endIndex: Int): RelativePath =
        if (startIndex == 0 && endIndex == names.size) {
            this
        } else {
            RelativePath(names.subList(startIndex, endIndex))
        }

    public fun startsWith(other: RelativePath): Boolean = names.startsWith(other.names)

    public fun endsWith(other: RelativePath): Boolean = names.endsWith(other.names)

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

    public fun resolve(fileName: ByteString): RelativePath {
        require(fileName.isNotEmpty()) { "Cannot resolve an empty file name" }
        return RelativePath(names + fileName)
    }

    public fun resolve(other: RelativePath): RelativePath =
        if (other.names.isEmpty()) this else RelativePath(names + other.names)

    public fun resolveSibling(fileName: ByteString): RelativePath {
        require(fileName.isNotEmpty()) { "Cannot resolve an empty file name" }
        check(names.isNotEmpty()) { "Cannot resolve sibling of an empty relative path" }
        return RelativePath(names.toMutableList().apply { set(lastIndex, fileName) })
    }

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
        val newNames = names.subList(0, commonNamesSize).toMutableList()
        repeat(namesSize - commonNamesSize) { newNames += Path.NAME_DOT_DOT }
        newNames += other.names.subList(commonNamesSize, otherNamesSize)
        return RelativePath(newNames)
    }

    public fun toByteString(): ByteString = buildByteString {
        for (name in names) {
            append(Path.NAME_SEPARATOR_BYTE)
            append(name)
        }
    }

    override fun compareTo(other: RelativePath): Int = names.compareTo(other.names)

    override fun toString(): String = "RelativePath(names=$names)"

    public companion object {
        public operator fun invoke(names: List<ByteString>): RelativePath {
            for (name in names) {
                require(name.isNotEmpty()) { "Empty name in relative path name \"$name\$" }
                require(Path.NAME_SEPARATOR_BYTE !in name) {
                    "Name separator in relative path name \"$name\""
                }
            }
            return RelativePath(names)
        }

        public fun fromByteString(path: ByteString): RelativePath {
            val names = buildList {
                val pathSize = path.size
                var nameStart = 0
                var nameEnd = nameStart
                while (nameEnd < pathSize) {
                    if (path[nameEnd] == Path.NAME_SEPARATOR_BYTE) {
                        if (nameEnd != nameStart) {
                            this += path.substring(nameStart, nameEnd)
                        }
                        nameStart = nameEnd + 1
                        nameEnd = nameStart
                    } else {
                        ++nameEnd
                    }
                }
                if (nameEnd != nameStart) {
                    this += path.substring(nameStart, nameEnd)
                }
            }
            return RelativePath(names)
        }
    }
}
