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
 * Basic options for opening content of a file.
 *
 * @see FileSystem.openContent
 */
public enum class BasicFileContentOption : FileContentOption {
    /**
     * Open for reading content of the file.
     *
     * This option is implied if none of [READ], [WRITE] or [APPEND] is specified.
     */
    READ,

    /** Open for writing content of the file. */
    WRITE,

    /**
     * Open for appending to content of the file.
     *
     * This option implies [WRITE] if neither [READ] nor [WRITE] is specified.
     */
    APPEND,

    /** Truncate the file content to empty if it exists and [WRITE] is specified or implied. */
    TRUNCATE_EXISTING,

    /** Create the file if it doesn't exist and [WRITE] is specified or implied. */
    CREATE,

    /**
     * Create the file and fail if it already exists, if [WRITE] is specified or implied.
     *
     * Symbolic links are not followed and considered existent regardless of their targets.
     *
     * This option implies [CREATE].
     */
    CREATE_NEW
}
