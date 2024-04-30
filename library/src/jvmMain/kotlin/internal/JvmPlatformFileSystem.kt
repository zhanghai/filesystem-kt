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
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import me.zhanghai.kotlin.filesystem.AccessMode
import me.zhanghai.kotlin.filesystem.BasicCopyFileOption
import me.zhanghai.kotlin.filesystem.CopyFileOption
import me.zhanghai.kotlin.filesystem.CreateFileOption
import me.zhanghai.kotlin.filesystem.DirectoryStream
import me.zhanghai.kotlin.filesystem.DirectoryStreamOption
import me.zhanghai.kotlin.filesystem.FileContent
import me.zhanghai.kotlin.filesystem.FileContentOption
import me.zhanghai.kotlin.filesystem.FileMetadataOption
import me.zhanghai.kotlin.filesystem.FileMetadataView
import me.zhanghai.kotlin.filesystem.FileStore
import me.zhanghai.kotlin.filesystem.Path
import me.zhanghai.kotlin.filesystem.PlatformFileSystem
import me.zhanghai.kotlin.filesystem.posix.PosixModeBit
import me.zhanghai.kotlin.filesystem.posix.PosixModeOption
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions

internal class JvmPlatformFileSystem : PlatformFileSystem {
    override fun getPath(platformPath: ByteString): Path =
        Paths.get(platformPath.decodeToString()).toAbsolutePath().toPath()

    override fun toPlatformPath(path: Path): ByteString =
        path.toJavaPath().toString().encodeToByteString()

    override suspend fun getRealPath(path: Path): Path {
        val javaPath = path.toJavaPath()
        val javaRealPath = runInterruptible(Dispatchers.IO) { javaPath.toRealPath() }
        return javaRealPath.toPath()
    }

    override suspend fun checkAccess(path: Path, vararg modes: AccessMode) {
        val javaPath = path.toJavaPath()
        runInterruptible(Dispatchers.IO) { javaPath.provider.checkAccess(javaPath, *modes) }
    }

    override suspend fun openMetadataView(
        file: Path,
        vararg options: FileMetadataOption
    ): FileMetadataView =
        when {
            JvmPosixFileMetadataView.isSupported -> JvmPosixFileMetadataView(file, *options)
            else -> JvmFileMetadataView(file, *options)
        }

    override suspend fun openContent(file: Path, vararg options: FileContentOption): FileContent =
        JvmFileContent(file, *options)

    override suspend fun openDirectoryStream(
        directory: Path,
        vararg options: DirectoryStreamOption
    ): DirectoryStream = JvmDirectoryStream(directory, *options)

    override suspend fun createDirectory(directory: Path, vararg options: CreateFileOption) {
        val javaDirectory = directory.toJavaPath()
        val javaAttributes = options.toJavaAttributes()
        runInterruptible(Dispatchers.IO) { Files.createDirectory(javaDirectory, *javaAttributes) }
    }

    override suspend fun readSymbolicLink(link: Path): ByteString {
        val javaLink = link.toJavaPath()
        val javaTarget = runInterruptible(Dispatchers.IO) { Files.readSymbolicLink(javaLink) }
        return javaTarget.toString().encodeToByteString()
    }

    override suspend fun createSymbolicLink(
        link: Path,
        target: ByteString,
        vararg options: CreateFileOption
    ) {
        val javaLink = link.toJavaPath()
        val javaTarget = Paths.get(target.decodeToString())
        val javaAttributes = options.toJavaAttributes()
        runInterruptible(Dispatchers.IO) {
            Files.createSymbolicLink(javaLink, javaTarget, *javaAttributes)
        }
    }

    override suspend fun createHardLink(link: Path, existing: Path) {
        val javaLink = link.toJavaPath()
        val javaExisting = existing.toJavaPath()
        return runInterruptible(Dispatchers.IO) { Files.createLink(javaLink, javaExisting) }
    }

    override suspend fun delete(path: Path) {
        val javaPath = path.toJavaPath()
        runInterruptible(Dispatchers.IO) { Files.delete(javaPath) }
    }

    override suspend fun isSameFile(path1: Path, path2: Path): Boolean {
        val javaPath1 = path1.toJavaPath()
        val javaPath2 = path2.toJavaPath()
        return runInterruptible(Dispatchers.IO) { Files.isSameFile(javaPath1, javaPath2) }
    }

    override suspend fun copy(source: Path, target: Path, vararg options: CopyFileOption) {
        val javaSource = source.toJavaPath()
        val javaTarget = target.toJavaPath()
        val javaOptions = options.toJavaOptions()
        runInterruptible(Dispatchers.IO) { Files.copy(javaSource, javaTarget, *javaOptions) }
    }

    override suspend fun move(source: Path, target: Path, vararg options: CopyFileOption) {
        val javaSource = source.toJavaPath()
        val javaTarget = target.toJavaPath()
        val javaOptions = options.toJavaOptions()
        runInterruptible(Dispatchers.IO) { Files.move(javaSource, javaTarget, *javaOptions) }
    }

    override suspend fun openFileStore(path: Path): FileStore = JvmFileStore(path)

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
                        else -> throw UnsupportedOperationException("Unsupported option $option")
                    }
            }
            return javaOptions.toTypedArray()
        }
    }
}
