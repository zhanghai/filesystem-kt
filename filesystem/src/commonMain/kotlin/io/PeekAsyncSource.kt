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
 * Copyright (C) 2018 Square, Inc.
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

import kotlinx.io.Buffer
import kotlinx.io.InternalIoApi
import kotlinx.io.checkByteCount

/** @see kotlinx.io.PeekSource */
internal class PeekAsyncSource(private val upstream: AsyncSource) : RawAsyncSource {
    @OptIn(InternalIoApi::class) private val buffer = upstream.buffer
    private var expectedSegment = buffer.head
    private var expectedPos = buffer.head?.pos ?: -1

    private var closed = false
    private var pos = 0L

    override suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        check(!closed) { "Source is closed." }
        checkByteCount(byteCount)
        // Source becomes invalid if there is an expected Segment and it and the expected position
        // do not match the current head and head position of the upstream buffer
        check(
            expectedSegment == null ||
                expectedSegment === buffer.head && expectedPos == buffer.head!!.pos
        ) {
            "Peek source is invalid because upstream source was used"
        }
        if (byteCount == 0L) return 0L
        if (!upstream.request(pos + 1)) return -1L

        if (expectedSegment == null && buffer.head != null) {
            // Only once the buffer actually holds data should an expected Segment and position be
            // recorded. This allows reads from the peek source to repeatedly return -1 and for data
            // to be added later. Unit tests depend on this behavior.
            expectedSegment = buffer.head
            expectedPos = buffer.head!!.pos
        }

        val toCopy = minOf(byteCount, buffer.size - pos)
        buffer.copyTo(sink, pos, pos + toCopy)
        pos += toCopy
        return toCopy
    }

    override suspend fun close() {
        closed = true
    }
}
