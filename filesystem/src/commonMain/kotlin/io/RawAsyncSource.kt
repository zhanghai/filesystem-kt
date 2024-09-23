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

/** @see kotlinx.io.RawSource */
public interface RawAsyncSource : AsyncCloseable {
    /** @see kotlinx.io.RawSource.readAtMostTo */
    @Throws(CancellationException::class, IOException::class)
    public suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long
}

/** @see kotlinx.io.buffered */
public fun RawAsyncSource.buffered(): AsyncSource = RealAsyncSource(this)

internal fun RawAsyncSource.withCloseable(closeable: AsyncCloseable): RawAsyncSource =
    CloseableRawAsyncSource(this, closeable)

private class CloseableRawAsyncSource(
    private val source: RawAsyncSource,
    private val closeable: AsyncCloseable,
) : RawAsyncSource {
    override suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long =
        source.readAtMostTo(sink, byteCount)

    override suspend fun close() {
        source.close()
        closeable.close()
    }
}