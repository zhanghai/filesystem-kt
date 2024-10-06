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
import me.zhanghai.kotlin.filesystem.io.AsyncCloseableFlow

/**
 * A watcher for changes to specific files in the file system.
 *
 * Note that due to the differences among the native facilities for monitoring file system changes,
 * many details like the accuracy, timeliness, ordering and performance implications will be
 * implementation specific.
 *
 * @see FileSystem.openFileWatcher
 */
public interface FileWatcher : AsyncCloseable {
    /**
     * Start watching for changes to a file.
     *
     * This method watches for changes to the file itself by default, even if the file is a
     * directory. [BasicWatchFileOption.WATCH_DIRECTORY_ENTRIES] can be specified to watch for
     * changes to the entries of a directory instead of the directory itself.
     *
     * If a file does not a parent directory (e.g. it is a root of the underlying file system), it
     * is implementation specific whether the file itself can be watched, in which case a
     * [UnsupportedOperationException] will be thrown to indicate such a failure.
     *
     * If the file points to an underlying file system object that has already been watched by this
     * file watcher, it is implementation specific if the same [AsyncCloseableFlow] instance will be
     * returned.
     *
     * @param file the file to watch for changes to
     * @param options the options for watching this file
     * @return a [flow][AsyncCloseableFlow] that emits [events][WatchFileEvent] for changes to this
     *   file, and can be [closed][AsyncCloseableFlow.close] to stop watching
     * @see WatchFileOption
     */
    @Throws(CancellationException::class, IOException::class)
    public suspend fun watch(
        file: Path,
        vararg options: WatchFileOption,
    ): AsyncCloseableFlow<out WatchFileEvent>

    /**
     * Close this file watcher.
     *
     * Existing [flows][AsyncCloseableFlow] returned by [watch] will also be closed and stop
     * emitting new events.
     */
    @Throws(CancellationException::class, IOException::class) override suspend fun close()
}
