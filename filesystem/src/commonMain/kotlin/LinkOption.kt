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

/**
 * Options for handling a symbolic link.
 *
 * @see FileSystem.copy
 * @see FileSystem.move
 * @see FileSystem.openContent
 * @see FileSystem.openMetadataView
 * @see FileSystem.readMetadata
 * @see FileWatcher.watch
 */
public enum class LinkOption :
    CopyFileOption, FileContentOption, FileMetadataOption, WatchFileOption {
    /**
     * Do not follow symbolic links.
     *
     * This option is ignored on file systems where symbolic links are not supported.
     */
    NO_FOLLOW_LINKS
}
