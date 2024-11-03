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

import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.InternalIoApi
import kotlinx.io.Segment
import kotlinx.io.UnsafeIoApi
import kotlinx.io.checkByteCount
import kotlinx.io.minOf
import kotlinx.io.unsafe.UnsafeBufferOperations

/** @see kotlinx.io.Source.readAtMostTo */
@OptIn(InternalIoApi::class)
@Throws(CancellationException::class, IOException::class)
public suspend fun AsyncSource.readAtMostTo(sink: ByteBuffer): Int {
    if (buffer.size == 0L) {
        request(Segment.SIZE.toLong())
        if (buffer.size == 0L) return -1
    }

    return buffer.readAtMostTo(sink)
}

/** @see kotlinx.io.Buffer.readAtMostTo */
@OptIn(UnsafeIoApi::class)
private fun Buffer.readAtMostTo(sink: ByteBuffer): Int {
    if (exhausted()) return -1
    var toCopy = 0
    UnsafeBufferOperations.readFromHead(this) { data, pos, limit ->
        toCopy = minOf(sink.remaining(), limit - pos)
        sink.put(data, pos, toCopy)
        toCopy
    }

    return toCopy
}

/** @see kotlinx.io.asSource */
public fun InputStream.asAsyncSource(): RawAsyncSource = InputStreamAsyncSource(this)

private open class InputStreamAsyncSource(private val input: InputStream) : RawAsyncSource {
    @OptIn(UnsafeIoApi::class)
    override suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long =
        withContext(Dispatchers.IO) {
            if (byteCount == 0L) return@withContext 0L
            checkByteCount(byteCount)
            var readTotal = 0L
            UnsafeBufferOperations.writeToTail(sink, 1) { data, pos, limit ->
                val maxToCopy = minOf(byteCount, limit - pos).toInt()
                readTotal = input.read(data, pos, maxToCopy).toLong()
                if (readTotal == -1L) {
                    0
                } else {
                    readTotal.toInt()
                }
            }
            readTotal
        }

    override suspend fun close() {
        withContext(Dispatchers.IO) { input.close() }
    }

    override fun toString() = "RawAsyncSource($input)"
}
