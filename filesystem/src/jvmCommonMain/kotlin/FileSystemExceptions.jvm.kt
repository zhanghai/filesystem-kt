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

public actual typealias AccessDeniedException = java.nio.file.AccessDeniedException

public actual fun AccessDeniedException(
    file: String?,
    otherFile: String?,
    reason: String?,
    cause: Throwable?,
): AccessDeniedException =
    AccessDeniedException(file, otherFile, reason).apply { cause?.let { initCause(it) } }

public actual typealias AtomicMoveNotSupportedException =
    java.nio.file.AtomicMoveNotSupportedException

public actual fun AtomicMoveNotSupportedException(
    source: String?,
    target: String?,
    reason: String?,
    cause: Throwable?,
): AtomicMoveNotSupportedException =
    AtomicMoveNotSupportedException(source, target, reason).apply { cause?.let { initCause(it) } }

public actual typealias DirectoryNotEmptyException = java.nio.file.DirectoryNotEmptyException

// The reason parameter isn't supported on JVM.
public actual fun DirectoryNotEmptyException(
    directory: String?,
    reason: String?,
    cause: Throwable?,
): DirectoryNotEmptyException =
    DirectoryNotEmptyException(directory).apply { cause?.let { initCause(it) } }

public actual typealias FileAlreadyExistsException = java.nio.file.FileAlreadyExistsException

public actual fun FileAlreadyExistsException(
    file: String?,
    otherFile: String?,
    reason: String?,
    cause: Throwable?,
): FileAlreadyExistsException =
    FileAlreadyExistsException(file, otherFile, reason).apply { cause?.let { initCause(it) } }

public actual typealias FileSystemLoopException = java.nio.file.FileSystemLoopException

// The reason parameter isn't supported on JVM.
public actual fun FileSystemLoopException(
    file: String?,
    reason: String?,
    cause: Throwable?,
): FileSystemLoopException = FileSystemLoopException(file).apply { cause?.let { initCause(it) } }

public actual typealias NoSuchFileException = java.nio.file.NoSuchFileException

public actual fun NoSuchFileException(
    file: String?,
    otherFile: String?,
    reason: String?,
    cause: Throwable?,
): NoSuchFileException =
    NoSuchFileException(file, otherFile, reason).apply { cause?.let { initCause(it) } }

public actual typealias NotDirectoryException = java.nio.file.NotDirectoryException

// The reason parameter isn't supported on JVM.
public actual fun NotDirectoryException(
    file: String?,
    reason: String?,
    cause: Throwable?,
): NotDirectoryException = NotDirectoryException(file).apply { cause?.let { initCause(it) } }

public actual typealias NotLinkException = java.nio.file.NotLinkException

public actual fun NotLinkException(
    file: String?,
    otherFile: String?,
    reason: String?,
    cause: Throwable?,
): NotLinkException =
    NotLinkException(file, otherFile, reason).apply { cause?.let { initCause(it) } }
