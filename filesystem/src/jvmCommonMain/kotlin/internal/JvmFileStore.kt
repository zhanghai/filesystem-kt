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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import me.zhanghai.kotlin.filesystem.FileStore
import me.zhanghai.kotlin.filesystem.FileStoreMetadata

internal class JvmFileStore(private val fileStore: JavaFileStore) : FileStore {
    override suspend fun readMetadata(): FileStoreMetadata {
        val type = fileStore.type().encodeToByteString()
        var blockSize = 0L
        var totalSpace = 0L
        var freeSpace = 0L
        var availableSpace = 0L
        runInterruptible(Dispatchers.IO) {
            blockSize = fileStore.blockSize
            totalSpace = fileStore.totalSpace
            freeSpace = fileStore.unallocatedSpace
            availableSpace = fileStore.usableSpace
        }
        return JvmFileStoreMetadata(type, blockSize, totalSpace, freeSpace, availableSpace)
    }

    override suspend fun close() {}
}

internal class JvmFileStoreMetadata(
    override val type: ByteString,
    override val blockSize: Long,
    override val totalSpace: Long,
    override val freeSpace: Long,
    override val availableSpace: Long,
) : FileStoreMetadata
