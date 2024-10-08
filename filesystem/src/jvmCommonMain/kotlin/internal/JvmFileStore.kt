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

import java.nio.file.FileStore as JavaFileStore
import java.nio.file.FileSystemException as JavaFileSystemException
import java.nio.file.Files
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import me.zhanghai.kotlin.filesystem.FileStore
import me.zhanghai.kotlin.filesystem.FileStoreMetadata
import me.zhanghai.kotlin.filesystem.Path

internal class JvmFileStore
private constructor(private val file: Path, private val fileStore: JavaFileStore) : FileStore {
    @Throws(CancellationException::class, IOException::class)
    override suspend fun readMetadata(): FileStoreMetadata {
        val type = fileStore.type().encodeToByteString()
        var blockSize = 0L
        var totalSpace = 0L
        var freeSpace = 0L
        var availableSpace = 0L
        withContext(Dispatchers.IO) {
            try {
                blockSize = fileStore.blockSize
                totalSpace = fileStore.totalSpace
                freeSpace = fileStore.unallocatedSpace
                availableSpace = fileStore.usableSpace
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(file)
            }
        }
        return JvmFileStoreMetadata(type, blockSize, totalSpace, freeSpace, availableSpace)
    }

    @Throws(CancellationException::class, IOException::class) override suspend fun close() {}

    companion object {
        @Throws(CancellationException::class, IOException::class)
        suspend operator fun invoke(file: Path): JvmFileStore {
            val javaFile = file.toJavaPath()
            val javaFileStore =
                withContext(Dispatchers.IO) {
                    try {
                        Files.getFileStore(javaFile)
                    } catch (e: JavaFileSystemException) {
                        throw e.toFileSystemException(file)
                    }
                }
            return JvmFileStore(file, javaFileStore)
        }
    }
}

internal class JvmFileStoreMetadata(
    override val type: ByteString,
    override val blockSize: Long,
    override val totalSpace: Long,
    override val freeSpace: Long,
    override val availableSpace: Long,
) : FileStoreMetadata
