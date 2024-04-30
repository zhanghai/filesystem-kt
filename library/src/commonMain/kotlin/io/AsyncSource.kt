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

import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlin.coroutines.cancellation.CancellationException

public interface AsyncSource : AsyncCloseable {
    @Throws(CancellationException::class, IOException::class)
    public suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long
}

internal fun AsyncSource.withCloseable(closeable: AsyncCloseable): AsyncSource =
    CloseableAsyncSource(this, closeable)

private class CloseableAsyncSource(
    private val source: AsyncSource,
    private val closeable: AsyncCloseable
) : AsyncSource {
    override suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long =
        source.readAtMostTo(sink, byteCount)

    override suspend fun close() {
        source.close()
        closeable.close()
    }
}
