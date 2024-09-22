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

public actual open class FileSystemException
internal constructor(
    private val file: String?,
    private val otherFile: String?,
    private val reason: String?,
    cause: Throwable?,
    @Suppress("UNUSED_PARAMETER") any: Any?,
) : IOException(reason, cause) {
    public actual open fun getFile(): String? = file

    public actual open fun getOtherFile(): String? = otherFile

    public actual open fun getReason(): String? = reason
}

public actual fun FileSystemException(
    file: String?,
    otherFile: String?,
    reason: String?,
    cause: Throwable?,
): FileSystemException = FileSystemException(file, otherFile, reason, cause, null)
