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

package me.zhanghai.kotlin.filesystem

import kotlinx.io.IOException

public open class FileSystemException(
    public val file: Path? = null,
    public val otherFile: Path? = null,
    public val reason: String? = null,
    cause: Throwable? = null,
) : IOException(buildMessage(file, otherFile, reason), cause) {
    public companion object {
        private fun buildMessage(file: Path?, otherFile: Path?, reason: String?): String? =
            if (file == null && otherFile == null) {
                reason
            } else {
                buildString {
                    file?.let { append(it.toUri().toString()) }
                    otherFile?.let {
                        append(" -> ")
                        append(it.toUri().toString())
                    }
                    reason?.let {
                        append(": ")
                        append(it)
                    }
                }
            }
    }
}
