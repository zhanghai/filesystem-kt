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
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path as JavaPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.io.bytestring.encodeToByteString
import me.zhanghai.kotlin.filesystem.BasicDirectoryStreamOption
import me.zhanghai.kotlin.filesystem.DirectoryEntry
import me.zhanghai.kotlin.filesystem.DirectoryStream
import me.zhanghai.kotlin.filesystem.DirectoryStreamOption
import me.zhanghai.kotlin.filesystem.Path

internal class JvmDirectoryStream
private constructor(
    private val directoryStream: JavaDirectoryStream<JavaPath>,
    private val readMetadata: Boolean
) : DirectoryStream {
    private val directoryIterator = directoryStream.iterator()

    override suspend fun read(): DirectoryEntry? {
        if (!directoryIterator.hasNext()) {
            return null
        }
        val javaPath = directoryIterator.next()
        val name = javaPath.fileName.toString().encodeToByteString()
        if (!readMetadata) {
            return JvmDirectoryEntry(name)
        }
        val metadataView =
            when {
                JvmPosixFileMetadataView.isSupported ->
                    JvmPosixFileMetadataView(javaPath, LinkOption.NOFOLLOW_LINKS)
                else -> JvmFileMetadataView(javaPath, LinkOption.NOFOLLOW_LINKS)
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
        runInterruptible(Dispatchers.IO) { directoryStream.close() }
    }

    companion object {
        suspend operator fun invoke(
            directory: Path,
            vararg options: DirectoryStreamOption
        ): JvmDirectoryStream {
            val javaDirectory = directory.toJavaPath()
            val javaDirectoryStream =
                runInterruptible(Dispatchers.IO) { Files.newDirectoryStream(javaDirectory) }
            var readMetadata = false
            for (option in options) {
                when (option) {
                    BasicDirectoryStreamOption.READ_TYPE,
                    BasicDirectoryStreamOption.READ_METADATA -> readMetadata = true
                    else -> throw UnsupportedOperationException("Unsupported option $option")
                }
            }
            return JvmDirectoryStream(javaDirectoryStream, readMetadata)
        }
    }
}
