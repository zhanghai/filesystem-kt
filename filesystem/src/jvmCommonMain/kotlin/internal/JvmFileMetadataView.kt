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

import java.nio.file.FileSystemException as JavaFileSystemException
import java.nio.file.Files
import java.nio.file.LinkOption as JavaLinkOption
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.BasicFileAttributes
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.io.IOException
import me.zhanghai.kotlin.filesystem.FileMetadata
import me.zhanghai.kotlin.filesystem.FileMetadataOption
import me.zhanghai.kotlin.filesystem.FileMetadataView
import me.zhanghai.kotlin.filesystem.FileTime
import me.zhanghai.kotlin.filesystem.FileType
import me.zhanghai.kotlin.filesystem.LinkOption
import me.zhanghai.kotlin.filesystem.Path

internal open class JvmFileMetadataView(private val file: Path, vararg options: JavaLinkOption) :
    FileMetadataView {
    private val fileAttributeView =
        Files.getFileAttributeView(
            file.toJavaPath(),
            BasicFileAttributeView::class.java,
            *options,
        )!!

    @Throws(CancellationException::class, IOException::class)
    override suspend fun readMetadata(): FileMetadata =
        runInterruptible(Dispatchers.IO) {
            try {
                JvmFileMetadata(fileAttributeView.readAttributes())
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(file)
            }
        }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun setTimes(
        lastModificationTime: FileTime?,
        lastAccessTime: FileTime?,
        creationTime: FileTime?,
    ) {
        runInterruptible(Dispatchers.IO) {
            try {
                fileAttributeView.setTimes(lastModificationTime, lastAccessTime, creationTime)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(file)
            }
        }
    }

    override suspend fun close() {}

    companion object {
        fun Array<out FileMetadataOption>.toJavaOptions(): Array<JavaLinkOption> {
            val javaOptionSet = mutableListOf<JavaLinkOption>()
            for (option in this) {
                when (option) {
                    LinkOption.NO_FOLLOW_LINKS -> javaOptionSet += JavaLinkOption.NOFOLLOW_LINKS
                    else -> throw UnsupportedOperationException("Unsupported option $option")
                }
            }
            return javaOptionSet.toTypedArray()
        }
    }
}

internal class JvmFileMetadata(private val fileAttributes: BasicFileAttributes) : FileMetadata {
    override val id: Any
        get() = fileAttributes.fileKey()!!

    override val type: FileType
        get() =
            when {
                fileAttributes.isRegularFile -> FileType.REGULAR_FILE
                fileAttributes.isDirectory -> FileType.DIRECTORY
                fileAttributes.isSymbolicLink -> FileType.SYMBOLIC_LINK
                else -> FileType.OTHER
            }

    override val size: Long
        get() = fileAttributes.size()

    override val lastModificationTime: FileTime?
        get() = fileAttributes.lastModifiedTime().takeIf { it.toMillis() != 0.toLong() }

    override val lastAccessTime: FileTime?
        get() = fileAttributes.lastAccessTime().takeIf { it.toMillis() != 0.toLong() }

    override val creationTime: FileTime?
        get() = fileAttributes.creationTime().takeIf { it.toMillis() != 0.toLong() }
}
