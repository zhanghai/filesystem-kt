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

package me.zhanghai.kotlin.filesystem.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import me.zhanghai.kotlin.filesystem.FileMetadataOption
import me.zhanghai.kotlin.filesystem.FileTime
import me.zhanghai.kotlin.filesystem.Path
import me.zhanghai.kotlin.filesystem.internal.JvmFileMetadataView.Companion.toJavaOptions
import me.zhanghai.kotlin.filesystem.posix.PosixFileMetadata
import me.zhanghai.kotlin.filesystem.posix.PosixFileMetadataView
import me.zhanghai.kotlin.filesystem.posix.PosixFileType
import me.zhanghai.kotlin.filesystem.posix.PosixModeBit
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.Path as JavaPath

internal class JvmPosixFileMetadataView(
    private val file: JavaPath,
    private vararg val options: LinkOption
) : PosixFileMetadataView {
    constructor(
        file: Path,
        vararg options: FileMetadataOption
    ) : this(file.toJavaPath(), *options.toJavaOptions())

    override suspend fun readMetadata(): PosixFileMetadata {
        val attributes =
            runInterruptible(Dispatchers.IO) { Files.readAttributes(file, ATTRIBUTES, *options) }
        val id = attributes[ATTRIBUTE_NANE_FILE_KEY]!!
        val size = attributes[ATTRIBUTE_NANE_SIZE] as Long
        val lastModificationTime =
            (attributes[ATTRIBUTE_NANE_LAST_MODIFIED_TIME] as FileTime).takeIf {
                it.toMillis() != 0.toLong()
            }
        val lastAccessTime =
            (attributes[ATTRIBUTE_NANE_LAST_ACCESS_TIME] as FileTime).takeIf {
                it.toMillis() != 0.toLong()
            }
        val creationTime =
            (attributes[ATTRIBUTE_NANE_CREATION_TIME] as FileTime).takeIf {
                it.toMillis() != 0.toLong()
            }
        val userId = attributes[ATTRIBUTE_NANE_UID] as Int
        val groupId = attributes[ATTRIBUTE_NANE_GID] as Int
        val modeInt = attributes[ATTRIBUTE_NANE_MODE] as Int
        val posixType =
            when (modeInt and S_IFMT) {
                S_IFIFO -> PosixFileType.FIFO
                S_IFCHR -> PosixFileType.CHARACTER_DEVICE
                S_IFDIR -> PosixFileType.DIRECTORY
                S_IFBLK -> PosixFileType.BLOCK_DEVICE
                S_IFREG -> PosixFileType.REGULAR_FILE
                S_IFLNK -> PosixFileType.SYMBOLIC_LINK
                S_IFSOCK -> PosixFileType.SOCKET
                else -> PosixFileType.OTHER
            }
        val mode = buildSet {
            if (modeInt and S_ISUID != 0) {
                this += PosixModeBit.SET_USER_ID
            }
            if (modeInt and S_ISGID != 0) {
                this += PosixModeBit.SET_GROUP_ID
            }
            if (modeInt and S_ISVTX != 0) {
                this += PosixModeBit.STICKY
            }
            if (modeInt and S_IRUSR != 0) {
                this += PosixModeBit.OWNER_READ
            }
            if (modeInt and S_IWUSR != 0) {
                this += PosixModeBit.OWNER_WRITE
            }
            if (modeInt and S_IXUSR != 0) {
                this += PosixModeBit.OWNER_EXECUTE
            }
            if (modeInt and S_IRGRP != 0) {
                this += PosixModeBit.GROUP_READ
            }
            if (modeInt and S_IWGRP != 0) {
                this += PosixModeBit.GROUP_WRITE
            }
            if (modeInt and S_IXGRP != 0) {
                this += PosixModeBit.GROUP_EXECUTE
            }
            if (modeInt and S_IROTH != 0) {
                this += PosixModeBit.OTHERS_READ
            }
            if (modeInt and S_IWOTH != 0) {
                this += PosixModeBit.OTHERS_WRITE
            }
            if (modeInt and S_IXOTH != 0) {
                this += PosixModeBit.OTHERS_EXECUTE
            }
        }
        val lastStatusChangeTime =
            (attributes[ATTRIBUTE_NAME_CTIME] as FileTime).takeIf { it.toMillis() != 0.toLong() }
        return JvmPosixFileMetadata(
            id,
            size,
            lastModificationTime,
            lastAccessTime,
            creationTime,
            posixType,
            userId,
            groupId,
            mode,
            lastStatusChangeTime
        )
    }

    override suspend fun setTimes(
        lastModificationTime: FileTime?,
        lastAccessTime: FileTime?,
        creationTime: FileTime?
    ) {
        val attributeView =
            Files.getFileAttributeView(file, BasicFileAttributeView::class.java, *options)
        runInterruptible(Dispatchers.IO) {
            attributeView.setTimes(lastModificationTime, lastAccessTime, creationTime)
        }
    }

    override suspend fun setMode(mode: Set<PosixModeBit>) {
        val modeInt =
            mode.fold(0) { modeInt, modeBit ->
                modeInt or
                    when (modeBit) {
                        PosixModeBit.SET_USER_ID -> S_ISUID
                        PosixModeBit.SET_GROUP_ID -> S_ISGID
                        PosixModeBit.STICKY -> S_ISVTX
                        PosixModeBit.OWNER_READ -> S_IRUSR
                        PosixModeBit.OWNER_WRITE -> S_IWUSR
                        PosixModeBit.OWNER_EXECUTE -> S_IXUSR
                        PosixModeBit.GROUP_READ -> S_IRGRP
                        PosixModeBit.GROUP_WRITE -> S_IWGRP
                        PosixModeBit.GROUP_EXECUTE -> S_IXGRP
                        PosixModeBit.OTHERS_READ -> S_IROTH
                        PosixModeBit.OTHERS_WRITE -> S_IWOTH
                        PosixModeBit.OTHERS_EXECUTE -> S_IXOTH
                    }
            }
        runInterruptible(Dispatchers.IO) {
            Files.setAttribute(file, ATTRIBUTE_MODE, modeInt, *options)
        }
    }

    override suspend fun setOwnership(userId: Int?, groupId: Int?) {
        if (userId != null) {
            runInterruptible(Dispatchers.IO) {
                Files.setAttribute(file, ATTRIBUTE_UID, userId, *options)
            }
        }
        if (groupId != null) {
            runInterruptible(Dispatchers.IO) {
                Files.setAttribute(file, ATTRIBUTE_GID, groupId, *options)
            }
        }
    }

    override suspend fun close() {}

    companion object {
        private const val ATTRIBUTE_VIEW_NAME_UNIX = "unix"
        private const val ATTRIBUTE_NANE_FILE_KEY = "fileKey"
        private const val ATTRIBUTE_NANE_SIZE = "size"
        private const val ATTRIBUTE_NANE_LAST_MODIFIED_TIME = "lastModifiedTime"
        private const val ATTRIBUTE_NANE_LAST_ACCESS_TIME = "lastAccessTime"
        private const val ATTRIBUTE_NANE_CREATION_TIME = "creationTime"
        private const val ATTRIBUTE_NANE_UID = "uid"
        private const val ATTRIBUTE_NANE_GID = "gid"
        private const val ATTRIBUTE_NANE_MODE = "mode"
        private const val ATTRIBUTE_NAME_CTIME = "ctime"
        private const val ATTRIBUTES =
            "$ATTRIBUTE_VIEW_NAME_UNIX:$ATTRIBUTE_NANE_FILE_KEY,$ATTRIBUTE_NANE_SIZE" +
                ",$ATTRIBUTE_NANE_LAST_MODIFIED_TIME,$ATTRIBUTE_NANE_LAST_ACCESS_TIME" +
                ",$ATTRIBUTE_NANE_CREATION_TIME,$ATTRIBUTE_NANE_UID,$ATTRIBUTE_NANE_GID" +
                ",$ATTRIBUTE_NANE_MODE,$ATTRIBUTE_NAME_CTIME"
        private const val ATTRIBUTE_MODE = "$ATTRIBUTE_VIEW_NAME_UNIX:$ATTRIBUTE_NANE_MODE"
        private const val ATTRIBUTE_UID = "$ATTRIBUTE_VIEW_NAME_UNIX:$ATTRIBUTE_NANE_UID"
        private const val ATTRIBUTE_GID = "$ATTRIBUTE_VIEW_NAME_UNIX:$ATTRIBUTE_NANE_GID"

        private const val S_IFMT = 0xF000
        private const val S_IFIFO = 0x1000
        private const val S_IFCHR = 0x2000
        private const val S_IFDIR = 0x4000
        private const val S_IFBLK = 0x6000
        private const val S_IFREG = 0x8000
        private const val S_IFLNK = 0xA000
        private const val S_IFSOCK = 0xC000
        private const val S_ISUID = 0x800
        private const val S_ISGID = 0x400
        private const val S_ISVTX = 0x200
        private const val S_IRUSR = 0x100
        private const val S_IWUSR = 0x80
        private const val S_IXUSR = 0x40
        private const val S_IRGRP = 0x20
        private const val S_IWGRP = 0x10
        private const val S_IXGRP = 0x8
        private const val S_IROTH = 0x4
        private const val S_IWOTH = 0x2
        private const val S_IXOTH = 0x1

        val isSupported by
            lazy(LazyThreadSafetyMode.PUBLICATION) {
                try {
                    Files.getFileAttributeView(
                        Paths.get("."),
                        PosixFileAttributeView::class.java,
                        LinkOption.NOFOLLOW_LINKS
                    )
                    true
                } catch (t: Throwable) {
                    false
                }
            }
    }
}

internal class JvmPosixFileMetadata(
    override val id: Any,
    override val size: Long,
    override val lastModificationTime: FileTime?,
    override val lastAccessTime: FileTime?,
    override val creationTime: FileTime?,
    override val posixType: PosixFileType,
    override val userId: Int,
    override val groupId: Int,
    override val mode: Set<PosixModeBit>,
    val lastStatusChangeTime: FileTime?
) : PosixFileMetadata
