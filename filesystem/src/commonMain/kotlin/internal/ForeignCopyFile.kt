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

import kotlinx.io.IOException
import me.zhanghai.kotlin.filesystem.AtomicMoveNotSupportedException
import me.zhanghai.kotlin.filesystem.BasicCopyFileOption
import me.zhanghai.kotlin.filesystem.BasicFileContentOption
import me.zhanghai.kotlin.filesystem.CopyFileOption
import me.zhanghai.kotlin.filesystem.FileAlreadyExistsException
import me.zhanghai.kotlin.filesystem.FileType
import me.zhanghai.kotlin.filesystem.LinkOption
import me.zhanghai.kotlin.filesystem.NoSuchFileException
import me.zhanghai.kotlin.filesystem.Path
import me.zhanghai.kotlin.filesystem.createDirectory
import me.zhanghai.kotlin.filesystem.createSymbolicLinkTo
import me.zhanghai.kotlin.filesystem.delete
import me.zhanghai.kotlin.filesystem.deleteIfExists
import me.zhanghai.kotlin.filesystem.exists
import me.zhanghai.kotlin.filesystem.io.buffered
import me.zhanghai.kotlin.filesystem.io.use
import me.zhanghai.kotlin.filesystem.openSink
import me.zhanghai.kotlin.filesystem.openSource
import me.zhanghai.kotlin.filesystem.readMetadata
import me.zhanghai.kotlin.filesystem.readSymbolicLink
import me.zhanghai.kotlin.filesystem.setTimes

@PublishedApi
internal object ForeignCopyFile {
    @Throws(IOException::class)
    suspend fun copy(source: Path, target: Path, vararg options: CopyFileOption) {
        val copyFileOptions = options.toCopyFileOptions()
        if (copyFileOptions.atomicMove) {
            throw UnsupportedOperationException(BasicCopyFileOption.ATOMIC_MOVE.toString())
        }
        val linkOptions =
            if (copyFileOptions.noFollowLinks) {
                arrayOf(LinkOption.NO_FOLLOW_LINKS)
            } else {
                emptyArray()
            }
        val sourceMetadata = source.readMetadata(*linkOptions)
        val sourceType = sourceMetadata.type
        if (sourceType == FileType.OTHER) {
            throw IOException("Cannot copy special files to a foreign provider")
        }
        if (!copyFileOptions.replaceExisting && target.exists(followLinks = false)) {
            throw FileAlreadyExistsException(source, target, null)
        }
        when (sourceType) {
            FileType.REGULAR_FILE -> {
                if (copyFileOptions.replaceExisting) {
                    target.deleteIfExists()
                }
                val openOptions =
                    if (copyFileOptions.noFollowLinks) {
                        arrayOf(LinkOption.NO_FOLLOW_LINKS)
                    } else {
                        emptyArray()
                    }
                source.openSource(*openOptions).use { sourceSource ->
                    // Call openSink() outside of the try-catch block.
                    val targetSink =
                        target.openSink(
                            BasicFileContentOption.WRITE,
                            BasicFileContentOption.CREATE_NEW,
                        )
                    try {
                        targetSink.use { sourceSource.buffered().transferTo(targetSink) }
                    } catch (t: Throwable) {
                        try {
                            target.deleteIfExists()
                        } catch (t2: Throwable) {
                            t.addSuppressed(t2)
                        }
                        throw t
                    }
                }
            }
            FileType.DIRECTORY -> {
                if (copyFileOptions.replaceExisting) {
                    target.deleteIfExists()
                }
                target.createDirectory()
            }
            FileType.SYMBOLIC_LINK -> {
                val sourceTarget = source.readSymbolicLink()
                try {
                    // This might throw UnsupportedOperationException, so we cannot delete the
                    // target file beforehand.
                    target.createSymbolicLinkTo(sourceTarget)
                } catch (e: FileAlreadyExistsException) {
                    if (!copyFileOptions.replaceExisting) {
                        throw e
                    }
                    target.deleteIfExists()
                    target.createSymbolicLinkTo(sourceTarget)
                }
            }
            else -> error(sourceType)
        }
        // We don't take failures when copying metadata as fatal.
        try {
            target.setTimes(
                sourceMetadata.lastModificationTime,
                if (copyFileOptions.copyMetadata) sourceMetadata.lastAccessTime else null,
                if (copyFileOptions.copyMetadata) sourceMetadata.creationTime else null,
            )
        } catch (e: IOException) {
            // Ignored.
        } catch (e: UnsupportedOperationException) {
            // Ignored.
        }
    }

    @Throws(IOException::class)
    suspend fun move(source: Path, target: Path, vararg options: CopyFileOption) {
        val copyFileOptions = options.toCopyFileOptions()
        if (copyFileOptions.atomicMove) {
            throw AtomicMoveNotSupportedException(
                source,
                target,
                "Cannot move file atomically to foreign provider",
            )
        }
        val optionsForCopy =
            if (copyFileOptions.copyMetadata && copyFileOptions.noFollowLinks) {
                options
            } else {
                CopyFileOptions(
                        replaceExisting = copyFileOptions.replaceExisting,
                        copyMetadata = true,
                        atomicMove = false,
                        noFollowLinks = true,
                    )
                    .toArray()
            }
        copy(source, target, *optionsForCopy)
        try {
            source.delete()
        } catch (t: Throwable) {
            if (t !is NoSuchFileException) {
                try {
                    target.delete()
                } catch (t2: Throwable) {
                    t.addSuppressed(t2)
                }
            }
            throw t
        }
    }
}
