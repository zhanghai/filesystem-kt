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
import kotlinx.io.IOException
import kotlinx.io.InternalIoApi
import kotlinx.io.RawSource

/** @see kotlinx.io.Sink */
public sealed interface AsyncSink : RawAsyncSink {
    /** @see kotlinx.io.Sink.buffer */
    @InternalIoApi public val buffer: Buffer

    /** @see kotlinx.io.Sink.write */
    @Throws(CancellationException::class, IOException::class)
    public suspend fun write(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size)

    /** @see kotlinx.io.Sink.transferFrom */
    @Throws(CancellationException::class, IOException::class)
    public suspend fun transferFrom(source: RawAsyncSource): Long

    /** @see kotlinx.io.Sink.transferFrom */
    @Throws(CancellationException::class, IOException::class)
    public suspend fun transferFrom(source: RawSource): Long

    /** @see kotlinx.io.Sink.write */
    @Throws(CancellationException::class, IOException::class)
    public suspend fun write(source: RawAsyncSource, byteCount: Long)

    /** @see kotlinx.io.Sink.write */
    @Throws(CancellationException::class, IOException::class)
    public suspend fun write(source: RawSource, byteCount: Long)

    /** @see kotlinx.io.Sink.writeByte */
    @Throws(CancellationException::class, IOException::class)
    public suspend fun writeByte(byte: Byte)

    /** @see kotlinx.io.Sink.writeShort */
    @Throws(CancellationException::class, IOException::class)
    public suspend fun writeShort(short: Short)

    /** @see kotlinx.io.Sink.writeInt */
    @Throws(CancellationException::class, IOException::class) public suspend fun writeInt(int: Int)

    /** @see kotlinx.io.Sink.writeLong */
    @Throws(CancellationException::class, IOException::class)
    public suspend fun writeLong(long: Long)

    /** @see kotlinx.io.Sink.flush */
    @Throws(CancellationException::class, IOException::class) override suspend fun flush()

    /** @see kotlinx.io.Sink.emit */
    @Throws(CancellationException::class, IOException::class) public suspend fun emit()

    /** @see kotlinx.io.Sink.hintEmit */
    @InternalIoApi
    @Throws(CancellationException::class, IOException::class)
    public suspend fun hintEmit()
}
