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

package me.zhanghai.kotlin.filesystem.internal

import kotlin.math.min

internal fun <T : Comparable<T>> List<T>.compareTo(other: List<T>): Int {
    val size = size
    val otherSize = other.size
    val commonSize = min(size, otherSize)
    for (i in 0 ..< commonSize) {
        this[i].compareTo(other[i]).let {
            if (it != 0) {
                return it
            }
        }
    }
    return size.compareTo(otherSize)
}

internal fun <T> List<T>.startsWith(other: List<T>): Boolean {
    val size = size
    val otherSize = other.size
    return when {
        size == otherSize -> this == other
        size > otherSize -> subList(0, otherSize) == other
        else -> false
    }
}

internal fun <T> List<T>.endsWith(other: List<T>): Boolean {
    val size = size
    val otherSize = other.size
    return when {
        size == otherSize -> this == other
        size > otherSize -> subList(size - otherSize, size) == other
        else -> false
    }
}
