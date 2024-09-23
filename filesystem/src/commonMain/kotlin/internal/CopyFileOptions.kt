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

import me.zhanghai.kotlin.filesystem.BasicCopyFileOption
import me.zhanghai.kotlin.filesystem.CopyFileOption
import me.zhanghai.kotlin.filesystem.LinkOption

internal class CopyFileOptions(
    val replaceExisting: Boolean,
    val copyMetadata: Boolean,
    val atomicMove: Boolean,
    val noFollowLinks: Boolean,
) {
    fun toArray(): Array<CopyFileOption> {
        val options = mutableListOf<CopyFileOption>()
        if (replaceExisting) {
            options += BasicCopyFileOption.REPLACE_EXISTING
        }
        if (copyMetadata) {
            options += BasicCopyFileOption.COPY_METADATA
        }
        if (atomicMove) {
            options += BasicCopyFileOption.ATOMIC_MOVE
        }
        if (noFollowLinks) {
            options += LinkOption.NO_FOLLOW_LINKS
        }
        return options.toTypedArray()
    }
}

internal fun Array<out CopyFileOption>.toCopyFileOptions(): CopyFileOptions {
    var replaceExisting = false
    var copyMetadata = false
    var atomicMove = false
    var noFollowLinks = false
    for (option in this) {
        when {
            option is BasicCopyFileOption ->
                when (option) {
                    BasicCopyFileOption.REPLACE_EXISTING -> replaceExisting = true
                    BasicCopyFileOption.COPY_METADATA -> copyMetadata = true
                    BasicCopyFileOption.ATOMIC_MOVE -> atomicMove = true
                }
            option === LinkOption.NO_FOLLOW_LINKS -> noFollowLinks = true
            else -> throw UnsupportedOperationException(option.toString())
        }
    }
    return CopyFileOptions(replaceExisting, copyMetadata, atomicMove, noFollowLinks)
}
