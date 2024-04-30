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

public expect class AccessDeniedException : FileSystemException

public expect fun AccessDeniedException(
    file: String? = null,
    otherFile: String? = null,
    reason: String? = null,
    cause: Throwable? = null
): AccessDeniedException

public expect class AtomicMoveNotSupportedException : FileSystemException

public expect fun AtomicMoveNotSupportedException(
    source: String? = null,
    target: String? = null,
    reason: String? = null,
    cause: Throwable? = null
): AtomicMoveNotSupportedException

public expect class DirectoryNotEmptyException : FileSystemException

public expect fun DirectoryNotEmptyException(
    directory: String? = null,
    reason: String? = null,
    cause: Throwable? = null
): DirectoryNotEmptyException

public expect class FileAlreadyExistsException : FileSystemException

public expect fun FileAlreadyExistsException(
    file: String? = null,
    otherFile: String? = null,
    reason: String? = null,
    cause: Throwable? = null
): FileAlreadyExistsException

public expect class FileSystemLoopException : FileSystemException

public expect fun FileSystemLoopException(
    file: String? = null,
    reason: String? = null,
    cause: Throwable? = null
): FileSystemLoopException

public expect class NoSuchFileException : FileSystemException

public expect fun NoSuchFileException(
    file: String? = null,
    otherFile: String? = null,
    reason: String? = null,
    cause: Throwable? = null
): NoSuchFileException

public expect class NotDirectoryException : FileSystemException

public expect fun NotDirectoryException(
    file: String? = null,
    reason: String? = null,
    cause: Throwable? = null
): NotDirectoryException

public expect class NotLinkException : FileSystemException

public expect fun NotLinkException(
    file: String? = null,
    otherFile: String? = null,
    reason: String? = null,
    cause: Throwable? = null
): NotLinkException
