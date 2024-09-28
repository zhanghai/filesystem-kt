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

import java.nio.file.DirectoryStream as JavaDirectoryStream
import java.nio.file.FileSystemException as JavaFileSystemException
import java.nio.file.LinkOption
import java.nio.file.Path as JavaPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import me.zhanghai.kotlin.filesystem.DirectoryEntry
import me.zhanghai.kotlin.filesystem.DirectoryEntryWithMetadata
import me.zhanghai.kotlin.filesystem.DirectoryStream
import me.zhanghai.kotlin.filesystem.FileMetadata
import me.zhanghai.kotlin.filesystem.Path

internal class JvmDirectoryStream(
    private val directory: Path,
    private val directoryStream: JavaDirectoryStream<JavaPath>,
    private val readMetadata: Boolean,
) : DirectoryStream {
    private val directoryIterator = directoryStream.iterator()

    override suspend fun read(): DirectoryEntry? {
        val hasNext =
            runInterruptible(Dispatchers.IO) {
                try {
                    directoryIterator.hasNext()
                } catch (e: JavaFileSystemException) {
                    throw e.toFileSystemException(directory)
                }
            }
        if (!hasNext) {
            return null
        }
        val javaFile =
            runInterruptible(Dispatchers.IO) {
                try {
                    directoryIterator.next()
                } catch (e: JavaFileSystemException) {
                    throw e.toFileSystemException(directory)
                }
            }
        val name = javaFile.fileName.toString().encodeToByteString()
        if (!readMetadata) {
            return JvmDirectoryEntry(name)
        }
        val file = javaFile.toPath()
        val metadataView =
            when {
                JvmPosixFileMetadataView.isSupported ->
                    JvmPosixFileMetadataView(file, LinkOption.NOFOLLOW_LINKS)
                else -> JvmFileMetadataView(file, LinkOption.NOFOLLOW_LINKS)
            }
        var metadataException: Throwable? = null
        val metadata =
            try {
                metadataView.readMetadata()
            } catch (t: Throwable) {
                metadataException = t
                null
            }
        return JvmDirectoryEntryWithMetadata(name, metadata, metadataException)
    }

    override suspend fun close() {
        runInterruptible(Dispatchers.IO) {
            try {
                directoryStream.close()
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(directory)
            }
        }
    }
}

internal class JvmDirectoryEntry(override val name: ByteString) : DirectoryEntry

internal class JvmDirectoryEntryWithMetadata(
    override val name: ByteString,
    override val metadata: FileMetadata?,
    override val metadataException: Throwable?,
) : DirectoryEntryWithMetadata
