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

public actual class AccessDeniedException
internal constructor(
    file: String?,
    otherFile: String?,
    reason: String?,
    cause: Throwable?,
    @Suppress("UNUSED_PARAMETER") any: Any?,
) : FileSystemException(file, otherFile, reason, cause, null)

public actual fun AccessDeniedException(
    file: String?,
    otherFile: String?,
    reason: String?,
    cause: Throwable?,
): AccessDeniedException = AccessDeniedException(file, otherFile, reason, cause, null)

public actual class AtomicMoveNotSupportedException
internal constructor(
    source: String?,
    target: String?,
    reason: String?,
    cause: Throwable?,
    @Suppress("UNUSED_PARAMETER") any: Any?,
) : FileSystemException(source, target, reason, cause, null)

public actual fun AtomicMoveNotSupportedException(
    source: String?,
    target: String?,
    reason: String?,
    cause: Throwable?,
): AtomicMoveNotSupportedException =
    AtomicMoveNotSupportedException(source, target, reason, cause, null)

public actual class DirectoryNotEmptyException
internal constructor(
    directory: String?,
    reason: String?,
    cause: Throwable?,
    @Suppress("UNUSED_PARAMETER") any: Any?,
) : FileSystemException(directory, null, reason, cause, null)

public actual fun DirectoryNotEmptyException(
    directory: String?,
    reason: String?,
    cause: Throwable?,
): DirectoryNotEmptyException = DirectoryNotEmptyException(directory, reason, cause, null)

public actual class FileAlreadyExistsException
internal constructor(
    file: String?,
    otherFile: String?,
    reason: String?,
    cause: Throwable?,
    @Suppress("UNUSED_PARAMETER") any: Any?,
) : FileSystemException(file, otherFile, reason, cause, null)

public actual fun FileAlreadyExistsException(
    file: String?,
    otherFile: String?,
    reason: String?,
    cause: Throwable?,
): FileAlreadyExistsException = FileAlreadyExistsException(file, otherFile, reason, cause, null)

public actual class FileSystemLoopException
internal constructor(
    file: String?,
    reason: String?,
    cause: Throwable?,
    @Suppress("UNUSED_PARAMETER") any: Any?,
) : FileSystemException(file, null, reason, cause, null)

public actual fun FileSystemLoopException(
    file: String?,
    reason: String?,
    cause: Throwable?,
): FileSystemLoopException = FileSystemLoopException(file, reason, cause, null)

public actual class NoSuchFileException
internal constructor(
    file: String?,
    otherFile: String?,
    reason: String?,
    cause: Throwable?,
    @Suppress("UNUSED_PARAMETER") any: Any?,
) : FileSystemException(file, otherFile, reason, cause, null)

public actual fun NoSuchFileException(
    file: String?,
    otherFile: String?,
    reason: String?,
    cause: Throwable?,
): NoSuchFileException = NoSuchFileException(file, otherFile, reason, cause, null)

public actual class NotDirectoryException
internal constructor(
    file: String?,
    reason: String?,
    cause: Throwable?,
    @Suppress("UNUSED_PARAMETER") any: Any?,
) : FileSystemException(file, null, reason, cause, null)

public actual fun NotDirectoryException(
    file: String?,
    reason: String?,
    cause: Throwable?,
): NotDirectoryException = NotDirectoryException(file, reason, cause, null)

public actual class NotLinkException
internal constructor(
    file: String?,
    otherFile: String?,
    reason: String?,
    cause: Throwable?,
    @Suppress("UNUSED_PARAMETER") any: Any?,
) : FileSystemException(file, otherFile, reason, cause, null)

public actual fun NotLinkException(
    file: String?,
    otherFile: String?,
    reason: String?,
    cause: Throwable?,
): NotLinkException = NotLinkException(file, otherFile, reason, cause, null)
