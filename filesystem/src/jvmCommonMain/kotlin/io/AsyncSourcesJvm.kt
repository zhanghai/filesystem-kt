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

import java.nio.ByteBuffer
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.InternalIoApi
import kotlinx.io.Segment
import kotlinx.io.UnsafeIoApi
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
