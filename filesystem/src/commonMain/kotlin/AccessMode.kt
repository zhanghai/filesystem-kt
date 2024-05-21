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
 * Access modes for checking the accessibility of a file.
 *
 * @see FileSystem.checkAccess
 */
public expect enum class AccessMode {
    /** Check read access of the file. */
    READ,

    /** Check read access of the file. */
    WRITE,

    /** Check execute access of the file. */
    EXECUTE
}
