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

package me.zhanghai.kotlin.filesystem.io

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.io.Buffer
import kotlinx.io.EOFException
import kotlinx.io.IOException
import kotlinx.io.InternalIoApi
import kotlinx.io.RawSink

/** @see kotlinx.io.Source */
public sealed interface AsyncSource : RawAsyncSource {
    /** @see kotlinx.io.Source.buffer */
    @InternalIoApi public val buffer: Buffer

    /** @see kotlinx.io.Source.exhausted */
    @Throws(CancellationException::class, IOException::class)
    public suspend fun exhausted(): Boolean

    /** @see kotlinx.io.Source.require */
    @Throws(CancellationException::class, EOFException::class, IOException::class)
    public suspend fun require(byteCount: Long)

    /** @see kotlinx.io.Source.request */
    @Throws(CancellationException::class, IOException::class)
    public suspend fun request(byteCount: Long): Boolean

    /** @see kotlinx.io.Source.readByte */
    @Throws(CancellationException::class, EOFException::class, IOException::class)
    public suspend fun readByte(): Byte

    /** @see kotlinx.io.Source.readShort */
    @Throws(CancellationException::class, EOFException::class, IOException::class)
    public suspend fun readShort(): Short

    /** @see kotlinx.io.Source.readInt */
    @Throws(CancellationException::class, EOFException::class, IOException::class)
    public suspend fun readInt(): Int

    /** @see kotlinx.io.Source.readLong */
    @Throws(CancellationException::class, EOFException::class, IOException::class)
    public suspend fun readLong(): Long

    /** @see kotlinx.io.Source.skip */
    @Throws(CancellationException::class, EOFException::class, IOException::class)
    public suspend fun skip(byteCount: Long)

    /** @see kotlinx.io.Source.readAtMostTo */
    @Throws(CancellationException::class, IOException::class)
    public suspend fun readAtMostTo(
        sink: ByteArray,
        startIndex: Int = 0,
        endIndex: Int = sink.size,
    ): Int

    /** @see kotlinx.io.Source.readTo */
    @Throws(CancellationException::class, EOFException::class, IOException::class)
    public suspend fun readTo(sink: RawAsyncSink, byteCount: Long)

    /** @see kotlinx.io.Source.readTo */
    @Throws(CancellationException::class, EOFException::class, IOException::class)
    public suspend fun readTo(sink: RawSink, byteCount: Long)

    /** @see kotlinx.io.Source.transferTo */
    @Throws(CancellationException::class, IOException::class)
    public suspend fun transferTo(sink: RawAsyncSink): Long

    /** @see kotlinx.io.Source.transferTo */
    @Throws(CancellationException::class, IOException::class)
    public suspend fun transferTo(sink: RawSink): Long

    /** @see kotlinx.io.Source.peek */
    public fun peek(): AsyncSource
}
