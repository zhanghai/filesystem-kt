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
 * Copyright (C) 2014 Square, Inc.
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

import java.io.OutputStream
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.InternalIoApi
import kotlinx.io.UnsafeIoApi
import kotlinx.io.checkOffsetAndCount
import kotlinx.io.minOf
import kotlinx.io.unsafe.UnsafeBufferOperations

/** @see kotlinx.io.Sink.write */
@OptIn(InternalIoApi::class)
public suspend fun AsyncSink.write(source: ByteBuffer): Int {
    val sizeBefore = buffer.size
    buffer.transferFrom(source)
    val bytesRead = buffer.size - sizeBefore
    hintEmit()
    return bytesRead.toInt()
}

/** @see kotlinx.io.Buffer.transferFrom */
@OptIn(UnsafeIoApi::class)
private fun Buffer.transferFrom(source: ByteBuffer): Buffer {
    val byteCount = source.remaining()
    var remaining = byteCount

    while (remaining > 0) {
        UnsafeBufferOperations.writeToTail(this, 1) { data, pos, limit ->
            val toCopy = minOf(remaining, limit - pos)
            source.get(data, pos, toCopy)
            remaining -= toCopy
            toCopy
        }
    }

    return this
}

/** @see kotlinx.io.asSink */
public fun OutputStream.asAsyncSink(): RawAsyncSink = OutputStreamAsyncSink(this)

private open class OutputStreamAsyncSink(private val out: OutputStream) : RawAsyncSink {
    @OptIn(UnsafeIoApi::class)
    override suspend fun write(source: Buffer, byteCount: Long) {
        withContext(Dispatchers.IO) {
            checkOffsetAndCount(source.size, 0, byteCount)
            var remaining = byteCount
            while (remaining > 0) {
                // kotlinx.io TODO: detect Interruption.
                UnsafeBufferOperations.readFromHead(source) { data, pos, limit ->
                    val toCopy = minOf(remaining, limit - pos).toInt()
                    out.write(data, pos, toCopy)
                    remaining -= toCopy
                    toCopy
                }
            }
        }
    }

    override suspend fun flush() {
        withContext(Dispatchers.IO) { out.flush() }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) { out.close() }
    }

    override fun toString() = "RawAsyncSink($out)"
}
