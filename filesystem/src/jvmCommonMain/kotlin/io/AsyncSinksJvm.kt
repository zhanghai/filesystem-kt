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

package me.zhanghai.kotlin.filesystem.io

import java.nio.ByteBuffer
import kotlinx.io.Buffer
import kotlinx.io.InternalIoApi
import kotlinx.io.UnsafeIoApi
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
public fun Buffer.transferFrom(source: ByteBuffer): Buffer {
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
