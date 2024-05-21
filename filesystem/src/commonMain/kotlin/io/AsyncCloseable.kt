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

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.io.IOException

public interface AsyncCloseable {
    @Throws(CancellationException::class, IOException::class) public suspend fun close()
}

@OptIn(ExperimentalContracts::class)
public suspend inline fun <T : AsyncCloseable?, R> T.use(block: (T) -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    var throwable: Throwable? = null
    try {
        return block(this)
    } catch (t: Throwable) {
        throwable = t
        throw t
    } finally {
        // Work around compiler error about smart cast and "captured by a changing closure"
        @Suppress("NAME_SHADOWING") val throwable = throwable
        when {
            this == null -> {}
            throwable == null -> close()
            else ->
                try {
                    close()
                } catch (closeThrowable: Throwable) {
                    throwable.addSuppressed(closeThrowable)
                }
        }
    }
}
