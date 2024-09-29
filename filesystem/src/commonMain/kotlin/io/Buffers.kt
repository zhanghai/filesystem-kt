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
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
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

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.Buffer
import kotlinx.io.EOFException
import kotlinx.io.Segment
import kotlinx.io.checkByteCount

/** @see Buffer.readTo */
public suspend fun Buffer.readTo(sink: RawAsyncSink, byteCount: Long) {
    checkByteCount(byteCount)
    if (size < byteCount) {
        sink.write(this, size) // Exhaust ourselves.
        throw EOFException(
            "Buffer exhausted before writing $byteCount bytes. Only $size bytes were written."
        )
    }
    sink.write(this, byteCount)
}

/** @see Buffer.transferTo */
public suspend fun Buffer.transferTo(sink: RawAsyncSink): Long {
    val byteCount = size
    if (byteCount > 0L) {
        sink.write(this, byteCount)
    }
    return byteCount
}

/** @see Buffer.write */
public suspend fun Buffer.write(source: RawAsyncSource, byteCount: Long) {
    checkByteCount(byteCount)
    var remainingByteCount = byteCount
    while (remainingByteCount > 0L) {
        coroutineContext.ensureActive()
        val read = source.readAtMostTo(this, remainingByteCount)
        if (read == -1L) {
            throw EOFException(
                "Source exhausted before reading $byteCount bytes. " +
                    "Only ${byteCount - remainingByteCount} were read."
            )
        }
        remainingByteCount -= read
    }
}

/** @see Buffer.transferFrom */
public suspend fun Buffer.transferFrom(source: RawAsyncSource): Long {
    var totalBytesRead = 0L
    while (true) {
        coroutineContext.ensureActive()
        val readCount = source.readAtMostTo(this, Segment.SIZE.toLong())
        if (readCount == -1L) break
        totalBytesRead += readCount
    }
    return totalBytesRead
}
