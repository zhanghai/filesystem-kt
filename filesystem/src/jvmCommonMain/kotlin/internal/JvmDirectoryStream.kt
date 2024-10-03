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
import java.nio.file.Files
import java.nio.file.Path as JavaPath
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import me.zhanghai.kotlin.filesystem.BasicDirectoryStreamOption
import me.zhanghai.kotlin.filesystem.DirectoryEntry
import me.zhanghai.kotlin.filesystem.DirectoryEntryWithMetadata
import me.zhanghai.kotlin.filesystem.DirectoryStream
import me.zhanghai.kotlin.filesystem.DirectoryStreamOption
import me.zhanghai.kotlin.filesystem.FileMetadata
import me.zhanghai.kotlin.filesystem.LinkOption
import me.zhanghai.kotlin.filesystem.Path

internal class JvmDirectoryStream
private constructor(
    private val directory: Path,
    private val directoryStream: JavaDirectoryStream<JavaPath>,
    private val readMetadata: Boolean,
) : DirectoryStream {
    private val directoryIterator = directoryStream.iterator()

    @Throws(CancellationException::class, IOException::class)
    override suspend fun read(): DirectoryEntry? {
        val hasNext =
            withContext(Dispatchers.IO) {
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
            withContext(Dispatchers.IO) {
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
                    JvmPosixFileMetadataView(file, LinkOption.NO_FOLLOW_LINKS)
                else -> JvmFileMetadataView(file, LinkOption.NO_FOLLOW_LINKS)
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

    @Throws(CancellationException::class, IOException::class)
    override suspend fun close() {
        withContext(Dispatchers.IO) {
            try {
                directoryStream.close()
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(directory)
            }
        }
    }

    companion object {
        @Throws(CancellationException::class, IOException::class)
        suspend operator fun invoke(
            directory: Path,
            vararg options: DirectoryStreamOption,
        ): JvmDirectoryStream {
            val javaDirectory = directory.toJavaPath()
            val javaDirectoryStream =
                withContext(Dispatchers.IO) {
                    try {
                        Files.newDirectoryStream(javaDirectory)
                    } catch (e: JavaFileSystemException) {
                        throw e.toFileSystemException(directory)
                    }
                }
            var readMetadata = false
            for (option in options) {
                when (option) {
                    BasicDirectoryStreamOption.READ_TYPE,
                    BasicDirectoryStreamOption.READ_METADATA -> readMetadata = true
                    else -> throw UnsupportedOperationException("Unsupported option $option")
                }
            }
            return JvmDirectoryStream(directory, javaDirectoryStream, readMetadata)
        }
    }
}

internal class JvmDirectoryEntry(override val name: ByteString) : DirectoryEntry

internal class JvmDirectoryEntryWithMetadata(
    override val name: ByteString,
    override val metadata: FileMetadata?,
    override val metadataException: Throwable?,
) : DirectoryEntryWithMetadata
