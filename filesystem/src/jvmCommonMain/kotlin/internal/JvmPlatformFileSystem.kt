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

import java.nio.channels.FileChannel
import java.nio.file.CopyOption
import java.nio.file.FileSystemException as JavaFileSystemException
import java.nio.file.Files
import java.nio.file.LinkOption as JavaLinkOption
import java.nio.file.OpenOption
import java.nio.file.Path as JavaPath
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.spi.FileSystemProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import me.zhanghai.kotlin.filesystem.AccessMode
import me.zhanghai.kotlin.filesystem.BasicCopyFileOption
import me.zhanghai.kotlin.filesystem.BasicDirectoryStreamOption
import me.zhanghai.kotlin.filesystem.BasicFileContentOption
import me.zhanghai.kotlin.filesystem.CopyFileOption
import me.zhanghai.kotlin.filesystem.CreateFileOption
import me.zhanghai.kotlin.filesystem.DirectoryStream
import me.zhanghai.kotlin.filesystem.DirectoryStreamOption
import me.zhanghai.kotlin.filesystem.FileContent
import me.zhanghai.kotlin.filesystem.FileContentOption
import me.zhanghai.kotlin.filesystem.FileMetadataOption
import me.zhanghai.kotlin.filesystem.FileMetadataView
import me.zhanghai.kotlin.filesystem.FileStore
import me.zhanghai.kotlin.filesystem.LinkOption
import me.zhanghai.kotlin.filesystem.Path
import me.zhanghai.kotlin.filesystem.PlatformFileSystem
import me.zhanghai.kotlin.filesystem.internal.JvmFileMetadataView.Companion.toJavaOptions
import me.zhanghai.kotlin.filesystem.posix.PosixModeBit
import me.zhanghai.kotlin.filesystem.posix.PosixModeOption
import me.zhanghai.kotlin.filesystem.requireSameSchemeAs

internal class JvmPlatformFileSystem : PlatformFileSystem {
    override suspend fun getRealPath(file: Path): Path {
        file.requireSameSchemeAs(this)
        val javaFile = file.toJavaPath()
        val javaRealPath =
            runInterruptible(Dispatchers.IO) {
                try {
                    javaFile.toRealPath()
                } catch (e: JavaFileSystemException) {
                    throw e.toFileSystemException(file)
                }
            }
        return javaRealPath.toPath()
    }

    override suspend fun checkAccess(file: Path, vararg modes: AccessMode) {
        file.requireSameSchemeAs(this)
        val javaFile = file.toJavaPath()
        runInterruptible(Dispatchers.IO) {
            try {
                javaFile.provider.checkAccess(javaFile, *modes)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(file)
            }
        }
    }

    override suspend fun openMetadataView(
        file: Path,
        vararg options: FileMetadataOption,
    ): FileMetadataView {
        file.requireSameSchemeAs(this)
        val javaOptions = options.toJavaOptions()
        return when {
            JvmPosixFileMetadataView.isSupported -> JvmPosixFileMetadataView(file, *javaOptions)
            else -> JvmFileMetadataView(file, *javaOptions)
        }
    }

    override suspend fun openContent(file: Path, vararg options: FileContentOption): FileContent {
        file.requireSameSchemeAs(this)
        val javaFile = file.toJavaPath()
        val javaOptions = mutableSetOf<OpenOption>()
        val javaAttributeList = mutableListOf<FileAttribute<*>>()
        for (option in options) {
            when (option) {
                BasicFileContentOption.READ -> javaOptions += StandardOpenOption.READ
                BasicFileContentOption.WRITE -> javaOptions += StandardOpenOption.WRITE
                BasicFileContentOption.APPEND -> javaOptions += StandardOpenOption.APPEND
                BasicFileContentOption.TRUNCATE_EXISTING ->
                    javaOptions += StandardOpenOption.TRUNCATE_EXISTING
                BasicFileContentOption.CREATE -> javaOptions += StandardOpenOption.CREATE
                BasicFileContentOption.CREATE_NEW -> javaOptions += StandardOpenOption.CREATE_NEW
                LinkOption.NO_FOLLOW_LINKS -> javaOptions += JavaLinkOption.NOFOLLOW_LINKS
                is CreateFileOption -> javaAttributeList += option.toJavaAttribute()
                else -> throw UnsupportedOperationException("Unsupported option $option")
            }
        }
        val javaAttributes = javaAttributeList.toTypedArray()
        val channel =
            runInterruptible(Dispatchers.IO) {
                try {
                    FileChannel.open(javaFile, javaOptions, *javaAttributes)
                } catch (e: JavaFileSystemException) {
                    throw e.toFileSystemException(file)
                }
            }
        return JvmFileContent(file, channel)
    }

    override suspend fun openDirectoryStream(
        directory: Path,
        vararg options: DirectoryStreamOption,
    ): DirectoryStream {
        directory.requireSameSchemeAs(this)
        val javaDirectory = directory.toJavaPath()
        val javaDirectoryStream =
            runInterruptible(Dispatchers.IO) {
                try {
                    Files.newDirectoryStream(javaDirectory)
                } catch (e: JavaFileSystemException) {
                    throw e.toFileSystemException(directory)
                }
            }
        var readMetadata = false
        for (option in options) {
            when (option) {
                BasicDirectoryStreamOption.READ_TYPE,
                BasicDirectoryStreamOption.READ_METADATA -> readMetadata = true
                else -> throw UnsupportedOperationException("Unsupported option $option")
            }
        }
        return JvmDirectoryStream(directory, javaDirectoryStream, readMetadata)
    }

    override suspend fun createDirectory(directory: Path, vararg options: CreateFileOption) {
        directory.requireSameSchemeAs(this)
        val javaDirectory = directory.toJavaPath()
        val javaAttributes = options.toJavaAttributes()
        runInterruptible(Dispatchers.IO) {
            try {
                Files.createDirectory(javaDirectory, *javaAttributes)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(directory)
            }
        }
    }

    override suspend fun readSymbolicLink(link: Path): ByteString {
        link.requireSameSchemeAs(this)
        val javaLink = link.toJavaPath()
        val javaTarget =
            runInterruptible(Dispatchers.IO) {
                try {
                    Files.readSymbolicLink(javaLink)
                } catch (e: JavaFileSystemException) {
                    throw e.toFileSystemException(link)
                }
            }
        return javaTarget.toString().encodeToByteString()
    }

    override suspend fun createSymbolicLink(
        link: Path,
        target: ByteString,
        vararg options: CreateFileOption,
    ) {
        link.requireSameSchemeAs(this)
        val javaLink = link.toJavaPath()
        val javaTarget = Paths.get(target.decodeToString())
        val javaAttributes = options.toJavaAttributes()
        runInterruptible(Dispatchers.IO) {
            try {
                Files.createSymbolicLink(javaLink, javaTarget, *javaAttributes)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(link)
            }
        }
    }

    override suspend fun createHardLink(link: Path, existing: Path) {
        link.requireSameSchemeAs(this)
        existing.requireSameSchemeAs(this)
        val javaLink = link.toJavaPath()
        val javaExisting = existing.toJavaPath()
        return runInterruptible(Dispatchers.IO) {
            try {
                Files.createLink(javaLink, javaExisting)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(link, existing)
            }
        }
    }

    override suspend fun delete(file: Path) {
        file.requireSameSchemeAs(this)
        val javaFile = file.toJavaPath()
        runInterruptible(Dispatchers.IO) {
            try {
                Files.delete(javaFile)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(file)
            }
        }
    }

    override suspend fun isSameFile(file1: Path, file2: Path): Boolean {
        file1.requireSameSchemeAs(this)
        file2.requireSameSchemeAs(this)
        val javaFile1 = file1.toJavaPath()
        val javaFile2 = file2.toJavaPath()
        return runInterruptible(Dispatchers.IO) {
            try {
                Files.isSameFile(javaFile1, javaFile2)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(file1, file2)
            }
        }
    }

    override suspend fun copy(source: Path, target: Path, vararg options: CopyFileOption) {
        source.requireSameSchemeAs(this)
        target.requireSameSchemeAs(this)
        val javaSource = source.toJavaPath()
        val javaTarget = target.toJavaPath()
        val javaOptions = options.toJavaOptions()
        runInterruptible(Dispatchers.IO) {
            try {
                Files.copy(javaSource, javaTarget, *javaOptions)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(source, target)
            }
        }
    }

    override suspend fun move(source: Path, target: Path, vararg options: CopyFileOption) {
        source.requireSameSchemeAs(this)
        target.requireSameSchemeAs(this)
        val javaSource = source.toJavaPath()
        val javaTarget = target.toJavaPath()
        val javaOptions = options.toJavaOptions()
        runInterruptible(Dispatchers.IO) {
            try {
                Files.move(javaSource, javaTarget, *javaOptions)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(source, target)
            }
        }
    }

    override suspend fun openFileStore(file: Path): FileStore {
        file.requireSameSchemeAs(this)
        val javaFile = file.toJavaPath()
        val javaFileStore =
            runInterruptible(Dispatchers.IO) {
                try {
                    Files.getFileStore(javaFile)
                } catch (e: JavaFileSystemException) {
                    throw e.toFileSystemException(file)
                }
            }
        return JvmFileStore(file, javaFileStore)
    }

    override fun getPath(platformPath: ByteString): Path =
        Paths.get(platformPath.decodeToString()).toAbsolutePath().toPath()

    override fun toPlatformPath(path: Path): ByteString {
        path.requireSameSchemeAs(this)
        return path.toJavaPath().toString().encodeToByteString()
    }

    private val JavaPath.provider: FileSystemProvider
        get() = fileSystem.provider()

    companion object {
        private fun Array<out CreateFileOption>.toJavaAttributes(): Array<FileAttribute<*>> =
            map { it.toJavaAttribute() }.toTypedArray()

        fun CreateFileOption.toJavaAttribute(): FileAttribute<*> =
            when (this) {
                is PosixModeOption -> {
                    val permissions = mutableSetOf<PosixFilePermission>()
                    for (modeBit in mode) {
                        when (modeBit) {
                            PosixModeBit.SET_USER_ID,
                            PosixModeBit.SET_GROUP_ID,
                            PosixModeBit.STICKY ->
                                throw UnsupportedOperationException(
                                    "Unsupported POSIX mode bit $modeBit"
                                )
                            PosixModeBit.OWNER_READ -> permissions += PosixFilePermission.OWNER_READ
                            PosixModeBit.OWNER_WRITE ->
                                permissions += PosixFilePermission.OWNER_WRITE
                            PosixModeBit.OWNER_EXECUTE ->
                                permissions += PosixFilePermission.OWNER_EXECUTE
                            PosixModeBit.GROUP_READ -> permissions += PosixFilePermission.GROUP_READ
                            PosixModeBit.GROUP_WRITE ->
                                permissions += PosixFilePermission.GROUP_WRITE
                            PosixModeBit.GROUP_EXECUTE ->
                                permissions += PosixFilePermission.GROUP_EXECUTE
                            PosixModeBit.OTHERS_READ ->
                                permissions += PosixFilePermission.OTHERS_READ
                            PosixModeBit.OTHERS_WRITE ->
                                permissions += PosixFilePermission.OTHERS_WRITE
                            PosixModeBit.OTHERS_EXECUTE ->
                                permissions += PosixFilePermission.OTHERS_EXECUTE
                        }
                    }
                    PosixFilePermissions.asFileAttribute(permissions)
                }
                else -> throw UnsupportedOperationException("Unsupported option $this")
            }

        private fun Array<out CopyFileOption>.toJavaOptions(): Array<CopyOption> {
            val javaOptions = mutableSetOf<CopyOption>()
            for (option in this) {
                javaOptions +=
                    when (option) {
                        BasicCopyFileOption.REPLACE_EXISTING -> StandardCopyOption.REPLACE_EXISTING
                        BasicCopyFileOption.COPY_METADATA -> StandardCopyOption.COPY_ATTRIBUTES
                        BasicCopyFileOption.ATOMIC_MOVE -> StandardCopyOption.ATOMIC_MOVE
                        LinkOption.NO_FOLLOW_LINKS -> JavaLinkOption.NOFOLLOW_LINKS
                        else -> throw UnsupportedOperationException("Unsupported option $option")
                    }
            }
            return javaOptions.toTypedArray()
        }
    }
}
