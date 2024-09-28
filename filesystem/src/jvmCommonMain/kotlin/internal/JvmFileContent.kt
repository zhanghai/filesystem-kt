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

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.FileSystemException as JavaFileSystemException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.io.Buffer
import kotlinx.io.asSource
import kotlinx.io.readTo
import me.zhanghai.kotlin.filesystem.FileContent
import me.zhanghai.kotlin.filesystem.Path

internal class JvmFileContent(private val file: Path, private val fileChannel: FileChannel) :
    FileContent {
    override suspend fun readAtMostTo(position: Long, sink: Buffer, byteCount: Long): Long {
        require(position >= 0) { "position ($position) < 0" }
        require(byteCount >= 0) { "byteCount ($byteCount) < 0" }
        return runInterruptible(Dispatchers.IO) {
            object : InputStream() {
                    var currentPosition = position

                    override fun read(): Int {
                        throw UnsupportedOperationException()
                    }

                    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                        val byteBuffer = ByteBuffer.wrap(bytes, offset, length)
                        val readByteCount =
                            try {
                                fileChannel.read(byteBuffer, currentPosition)
                            } catch (e: JavaFileSystemException) {
                                throw e.toFileSystemException(file)
                            }
                        if (readByteCount == -1) {
                            return -1
                        }
                        currentPosition += readByteCount
                        return readByteCount
                    }
                }
                .asSource()
                .readAtMostTo(sink, byteCount)
        }
    }

    override suspend fun write(position: Long, source: Buffer, byteCount: Long) {
        require(position >= 0) { "position ($position) < 0" }
        require(byteCount >= 0) { "byteCount ($byteCount) < 0" }
        require(byteCount <= source.size) {
            "byteCount ($byteCount) > source.size (${source.size})"
        }
        runInterruptible(Dispatchers.IO) {
            source.readTo(
                object : OutputStream() {
                    var currentPosition = position

                    override fun write(byte: Int) {
                        throw UnsupportedOperationException()
                    }

                    override fun write(bytes: ByteArray, offset: Int, length: Int) {
                        val byteBuffer = ByteBuffer.wrap(bytes, offset, length)
                        while (byteBuffer.hasRemaining()) {
                            val writtenByteCount =
                                try {
                                    fileChannel.write(byteBuffer, currentPosition)
                                } catch (e: JavaFileSystemException) {
                                    throw e.toFileSystemException(file)
                                }
                            currentPosition += writtenByteCount
                        }
                    }
                },
                byteCount,
            )
        }
    }

    override suspend fun getSize(): Long =
        runInterruptible(Dispatchers.IO) {
            try {
                fileChannel.size()
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(file)
            }
        }

    override suspend fun setSize(size: Long) {
        runInterruptible(Dispatchers.IO) {
            try {
                val channelSize = fileChannel.size()
                when {
                    size < channelSize -> fileChannel.truncate(size)
                    size > channelSize -> fileChannel.write(ByteBuffer.allocate(1), size - 1)
                    else -> {}
                }
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(file)
            }
        }
    }

    override suspend fun sync() {
        runInterruptible(Dispatchers.IO) {
            try {
                fileChannel.force(true)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(file)
            }
        }
    }

    override suspend fun close() {
        runInterruptible(Dispatchers.IO) {
            try {
                fileChannel.close()
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(file)
            }
        }
    }
}
