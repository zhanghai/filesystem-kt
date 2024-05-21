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

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.io.IOException
import me.zhanghai.kotlin.filesystem.io.AsyncCloseable

/**
 * A stream of entries in a directory.
 *
 * @see FileSystem.openDirectoryStream
 */
public interface DirectoryStream : AsyncCloseable {
    /**
     * Read an entry from this directory stream.
     *
     * The special entries `.` and `..` are not returned by this method.
     *
     * @return a directory entry, or {@code null} if this directory stream has reached its end
     */
    @Throws(CancellationException::class, IOException::class)
    public suspend fun read(): DirectoryEntry?
}
