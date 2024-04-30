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

package me.zhanghai.kotlin.filesystem

import kotlinx.io.Buffer
import kotlinx.io.IOException
import me.zhanghai.kotlin.filesystem.io.AsyncCloseable
import me.zhanghai.kotlin.filesystem.io.AsyncSink
import me.zhanghai.kotlin.filesystem.io.AsyncSource
import kotlin.concurrent.Volatile
import kotlin.coroutines.cancellation.CancellationException

public interface FileContent : AsyncCloseable {
    @Throws(CancellationException::class, IOException::class)
    public suspend fun readAtMostTo(position: Long, sink: Buffer, byteCount: Long): Long

    @Throws(CancellationException::class, IOException::class)
    public suspend fun write(position: Long, source: Buffer, byteCount: Long)

    @Throws(CancellationException::class, IOException::class) public suspend fun getSize(): Long

    @Throws(CancellationException::class, IOException::class) public suspend fun setSize(size: Long)

    @Throws(CancellationException::class, IOException::class) public suspend fun sync()
}

public fun FileContent.openSource(position: Long = 0): AsyncSource =
    FileContentSource(this, position)

private class FileContentSource(private val fileContent: FileContent, private var position: Long) :
    AsyncSource {
    @Volatile private var closed: Boolean = false

    @Throws(CancellationException::class, IOException::class)
    override suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        checkNotClosed()
        val read = fileContent.readAtMostTo(position, sink, byteCount)
        if (read != -1L) {
            position += read
        }
        return read
    }

    private fun checkNotClosed() {
        if (!closed) {
            throw IOException("Source is closed")
        }
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun close() {
        closed = true
    }
}

public fun FileContent.openSink(position: Long = 0): AsyncSink = FileContentSink(this, position)

private class FileContentSink(private val fileContent: FileContent, private var position: Long) :
    AsyncSink {
    @Volatile private var closed: Boolean = false

    @Throws(CancellationException::class, IOException::class)
    override suspend fun write(source: Buffer, byteCount: Long) {
        checkNotClosed()
        fileContent.write(position, source, byteCount)
        position += byteCount
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun flush() {
        checkNotClosed()
    }

    private fun checkNotClosed() {
        if (!closed) {
            throw IOException("Sink is closed")
        }
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun close() {
        closed = true
    }
}
