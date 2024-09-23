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

/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package me.zhanghai.kotlin.filesystem.io

import kotlin.jvm.JvmField
import kotlinx.io.Buffer
import kotlinx.io.EOFException
import kotlinx.io.InternalIoApi
import kotlinx.io.RawSource
import kotlinx.io.Segment
import kotlinx.io.checkBounds

@OptIn(InternalIoApi::class)
internal class RealAsyncSink(val sink: RawAsyncSink) : AsyncSink {
    @JvmField var closed: Boolean = false
    private val bufferField = Buffer()

    @InternalIoApi
    override val buffer: Buffer
        get() = bufferField

    override suspend fun write(source: Buffer, byteCount: Long) {
        checkNotClosed()
        require(byteCount >= 0) { "byteCount: $byteCount" }
        bufferField.write(source, byteCount)
        hintEmit()
    }

    override suspend fun write(source: ByteArray, startIndex: Int, endIndex: Int) {
        checkNotClosed()
        checkBounds(source.size, startIndex, endIndex)
        bufferField.write(source, startIndex, endIndex)
        hintEmit()
    }

    override suspend fun transferFrom(source: RawAsyncSource): Long {
        checkNotClosed()
        var totalBytesRead = 0L
        while (true) {
            val readCount: Long = source.readAtMostTo(bufferField, Segment.SIZE.toLong())
            if (readCount == -1L) break
            totalBytesRead += readCount
            hintEmit()
        }
        return totalBytesRead
    }

    override suspend fun transferFrom(source: RawSource): Long {
        checkNotClosed()
        var totalBytesRead = 0L
        while (true) {
            val readCount: Long = source.readAtMostTo(bufferField, Segment.SIZE.toLong())
            if (readCount == -1L) break
            totalBytesRead += readCount
            hintEmit()
        }
        return totalBytesRead
    }

    override suspend fun write(source: RawAsyncSource, byteCount: Long) {
        checkNotClosed()
        require(byteCount >= 0) { "byteCount: $byteCount" }
        var remainingByteCount = byteCount
        while (remainingByteCount > 0L) {
            val read = source.readAtMostTo(bufferField, remainingByteCount)
            if (read == -1L) {
                val bytesRead = byteCount - remainingByteCount
                throw EOFException(
                    "Source exhausted before reading $byteCount bytes from it (number of bytes read: $bytesRead)."
                )
            }
            remainingByteCount -= read
            hintEmit()
        }
    }

    override suspend fun write(source: RawSource, byteCount: Long) {
        checkNotClosed()
        require(byteCount >= 0) { "byteCount: $byteCount" }
        var remainingByteCount = byteCount
        while (remainingByteCount > 0L) {
            val read = source.readAtMostTo(bufferField, remainingByteCount)
            if (read == -1L) {
                val bytesRead = byteCount - remainingByteCount
                throw EOFException(
                    "Source exhausted before reading $byteCount bytes from it (number of bytes read: $bytesRead)."
                )
            }
            remainingByteCount -= read
            hintEmit()
        }
    }

    override suspend fun writeByte(byte: Byte) {
        checkNotClosed()
        bufferField.writeByte(byte)
        hintEmit()
    }

    override suspend fun writeShort(short: Short) {
        checkNotClosed()
        bufferField.writeShort(short)
        hintEmit()
    }

    override suspend fun writeInt(int: Int) {
        checkNotClosed()
        bufferField.writeInt(int)
        hintEmit()
    }

    override suspend fun writeLong(long: Long) {
        checkNotClosed()
        bufferField.writeLong(long)
        hintEmit()
    }

    @InternalIoApi
    override suspend fun hintEmit() {
        checkNotClosed()
        val byteCount = bufferField.completeSegmentByteCount()
        if (byteCount > 0L) sink.write(bufferField, byteCount)
    }

    override suspend fun emit() {
        checkNotClosed()
        val byteCount = bufferField.size
        if (byteCount > 0L) sink.write(bufferField, byteCount)
    }

    override suspend fun flush() {
        checkNotClosed()
        if (bufferField.size > 0L) {
            sink.write(bufferField, bufferField.size)
        }
        sink.flush()
    }

    override suspend fun close() {
        if (closed) return

        // Emit buffered data to the underlying sink. If this fails, we still need
        // to close the sink; otherwise we risk leaking resources.
        var thrown: Throwable? = null
        try {
            if (bufferField.size > 0) {
                sink.write(bufferField, bufferField.size)
            }
        } catch (e: Throwable) {
            thrown = e
        }

        try {
            sink.close()
        } catch (e: Throwable) {
            if (thrown == null) thrown = e
        }

        closed = true

        if (thrown != null) throw thrown
    }

    override fun toString() = "buffered($sink)"

    @Suppress("NOTHING_TO_INLINE")
    private inline fun checkNotClosed() {
        check(!closed) { "Sink is closed." }
    }
}
