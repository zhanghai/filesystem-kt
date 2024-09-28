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

public open class AccessDeniedException(
    file: Path? = null,
    otherFile: Path? = null,
    reason: String? = null,
    cause: Throwable? = null,
) : FileSystemException(file, otherFile, reason, cause)

public open class AtomicMoveNotSupportedException(
    file: Path? = null,
    otherFile: Path? = null,
    reason: String? = null,
    cause: Throwable? = null,
) : FileSystemException(file, otherFile, reason, cause)

public open class DirectoryNotEmptyException(
    file: Path? = null,
    otherFile: Path? = null,
    reason: String? = null,
    cause: Throwable? = null,
) : FileSystemException(file, otherFile, reason, cause)

public open class FileAlreadyExistsException(
    file: Path? = null,
    otherFile: Path? = null,
    reason: String? = null,
    cause: Throwable? = null,
) : FileSystemException(file, otherFile, reason, cause)

public open class FileSystemLoopException(
    file: Path? = null,
    otherFile: Path? = null,
    reason: String? = null,
    cause: Throwable? = null,
) : FileSystemException(file, otherFile, reason, cause)

public open class IsDirectoryException(
    file: Path? = null,
    otherFile: Path? = null,
    reason: String? = null,
    cause: Throwable? = null,
) : FileSystemException(file, otherFile, reason, cause)

public open class NoSuchFileException(
    file: Path? = null,
    otherFile: Path? = null,
    reason: String? = null,
    cause: Throwable? = null,
) : FileSystemException(file, otherFile, reason, cause)

public open class NotDirectoryException(
    file: Path? = null,
    otherFile: Path? = null,
    reason: String? = null,
    cause: Throwable? = null,
) : FileSystemException(file, otherFile, reason, cause)

public open class NotLinkException(
    file: Path? = null,
    otherFile: Path? = null,
    reason: String? = null,
    cause: Throwable? = null,
) : FileSystemException(file, otherFile, reason, cause)
