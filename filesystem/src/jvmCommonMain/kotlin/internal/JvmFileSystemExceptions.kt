package me.zhanghai.kotlin.filesystem.internal

import java.nio.file.AccessDeniedException as JavaAccessDeniedException
import java.nio.file.AtomicMoveNotSupportedException as JavaAtomicMoveNotSupportedException
import java.nio.file.DirectoryNotEmptyException as JavaDirectoryNotEmptyException
import java.nio.file.FileAlreadyExistsException as JavaFileAlreadyExistsException
import java.nio.file.FileSystemException as JavaFileSystemException
import java.nio.file.FileSystemLoopException as JavaFileSystemLoopException
import java.nio.file.NoSuchFileException as JavaNoSuchFileException
import java.nio.file.NotDirectoryException as JavaNotDirectoryException
import java.nio.file.NotLinkException as JavaNotLinkException
import me.zhanghai.kotlin.filesystem.AccessDeniedException
import me.zhanghai.kotlin.filesystem.AtomicMoveNotSupportedException
import me.zhanghai.kotlin.filesystem.DirectoryNotEmptyException
import me.zhanghai.kotlin.filesystem.FileAlreadyExistsException
import me.zhanghai.kotlin.filesystem.FileSystemException
import me.zhanghai.kotlin.filesystem.FileSystemLoopException
import me.zhanghai.kotlin.filesystem.NoSuchFileException
import me.zhanghai.kotlin.filesystem.NotDirectoryException
import me.zhanghai.kotlin.filesystem.NotLinkException
import me.zhanghai.kotlin.filesystem.Path

internal fun JavaFileSystemException.toFileSystemException(
    file: Path? = null,
    otherFile: Path? = null,
): FileSystemException {
    return when (this) {
        is JavaAccessDeniedException -> AccessDeniedException(file, otherFile, reason, this)
        is JavaAtomicMoveNotSupportedException ->
            AtomicMoveNotSupportedException(file, otherFile, reason, this)
        is JavaDirectoryNotEmptyException ->
            DirectoryNotEmptyException(file, otherFile, reason, this)
        is JavaFileAlreadyExistsException ->
            FileAlreadyExistsException(file, otherFile, reason, this)
        is JavaFileSystemLoopException -> FileSystemLoopException(file, otherFile, reason, this)
        is JavaNoSuchFileException -> NoSuchFileException(file, otherFile, reason, this)
        is JavaNotDirectoryException -> NotDirectoryException(file, otherFile, reason, this)
        is JavaNotLinkException -> NotLinkException(file, otherFile, reason, this)
        else -> FileSystemException(file, otherFile, reason, this)
    }
}
