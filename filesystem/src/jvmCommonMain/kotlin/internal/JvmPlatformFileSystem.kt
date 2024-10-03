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

import java.nio.file.CopyOption
import java.nio.file.FileSystemException as JavaFileSystemException
import java.nio.file.Files
import java.nio.file.LinkOption as JavaLinkOption
import java.nio.file.Path as JavaPath
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.FileAttribute
import java.nio.file.spi.FileSystemProvider
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
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
import me.zhanghai.kotlin.filesystem.FileWatcher
import me.zhanghai.kotlin.filesystem.LinkOption
import me.zhanghai.kotlin.filesystem.Path
import me.zhanghai.kotlin.filesystem.PlatformFileSystem
import me.zhanghai.kotlin.filesystem.internal.JvmFileContent.Companion.toJavaAttribute
import me.zhanghai.kotlin.filesystem.requireSameSchemeAs

internal class JvmPlatformFileSystem : PlatformFileSystem {
    @Throws(CancellationException::class, IOException::class)
    override suspend fun getRealPath(file: Path): Path {
        file.requireSameSchemeAs(this)
        val javaFile = file.toJavaPath()
        val javaRealPath =
            withContext(Dispatchers.IO) {
                try {
                    javaFile.toRealPath()
                } catch (e: JavaFileSystemException) {
                    throw e.toFileSystemException(file)
                }
            }
        return javaRealPath.toPath()
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun checkAccess(file: Path, vararg modes: AccessMode) {
        file.requireSameSchemeAs(this)
        val javaFile = file.toJavaPath()
        withContext(Dispatchers.IO) {
            try {
                javaFile.provider.checkAccess(javaFile, *modes)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(file)
            }
        }
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun openMetadataView(
        file: Path,
        vararg options: FileMetadataOption,
    ): FileMetadataView {
        file.requireSameSchemeAs(this)
        return when {
            JvmPosixFileMetadataView.isSupported -> JvmPosixFileMetadataView(file, *options)
            else -> JvmFileMetadataView(file, *options)
        }
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun openContent(file: Path, vararg options: FileContentOption): FileContent {
        file.requireSameSchemeAs(this)
        return JvmFileContent(file, *options)
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun openDirectoryStream(
        directory: Path,
        vararg options: DirectoryStreamOption,
    ): DirectoryStream {
        directory.requireSameSchemeAs(this)
        return JvmDirectoryStream(directory, *options)
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun createDirectory(directory: Path, vararg options: CreateFileOption) {
        directory.requireSameSchemeAs(this)
        val javaDirectory = directory.toJavaPath()
        val javaAttributes = options.toJavaAttributes()
        withContext(Dispatchers.IO) {
            try {
                Files.createDirectory(javaDirectory, *javaAttributes)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(directory)
            }
        }
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun readSymbolicLink(link: Path): ByteString {
        link.requireSameSchemeAs(this)
        val javaLink = link.toJavaPath()
        val javaTarget =
            withContext(Dispatchers.IO) {
                try {
                    Files.readSymbolicLink(javaLink)
                } catch (e: JavaFileSystemException) {
                    throw e.toFileSystemException(link)
                }
            }
        return javaTarget.toString().encodeToByteString()
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun createSymbolicLink(
        link: Path,
        target: ByteString,
        vararg options: CreateFileOption,
    ) {
        link.requireSameSchemeAs(this)
        val javaLink = link.toJavaPath()
        val javaTarget = Paths.get(target.decodeToString())
        val javaAttributes = options.toJavaAttributes()
        withContext(Dispatchers.IO) {
            try {
                Files.createSymbolicLink(javaLink, javaTarget, *javaAttributes)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(link)
            }
        }
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun createHardLink(link: Path, existing: Path) {
        link.requireSameSchemeAs(this)
        existing.requireSameSchemeAs(this)
        val javaLink = link.toJavaPath()
        val javaExisting = existing.toJavaPath()
        return withContext(Dispatchers.IO) {
            try {
                Files.createLink(javaLink, javaExisting)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(link, existing)
            }
        }
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun delete(file: Path) {
        file.requireSameSchemeAs(this)
        val javaFile = file.toJavaPath()
        withContext(Dispatchers.IO) {
            try {
                Files.delete(javaFile)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(file)
            }
        }
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun isSameFile(file1: Path, file2: Path): Boolean {
        file1.requireSameSchemeAs(this)
        file2.requireSameSchemeAs(this)
        val javaFile1 = file1.toJavaPath()
        val javaFile2 = file2.toJavaPath()
        return withContext(Dispatchers.IO) {
            try {
                Files.isSameFile(javaFile1, javaFile2)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(file1, file2)
            }
        }
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun copy(source: Path, target: Path, vararg options: CopyFileOption) {
        source.requireSameSchemeAs(this)
        target.requireSameSchemeAs(this)
        val javaSource = source.toJavaPath()
        val javaTarget = target.toJavaPath()
        val javaOptions = options.toJavaOptions()
        withContext(Dispatchers.IO) {
            try {
                Files.copy(javaSource, javaTarget, *javaOptions)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(source, target)
            }
        }
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun move(source: Path, target: Path, vararg options: CopyFileOption) {
        source.requireSameSchemeAs(this)
        target.requireSameSchemeAs(this)
        val javaSource = source.toJavaPath()
        val javaTarget = target.toJavaPath()
        val javaOptions = options.toJavaOptions()
        withContext(Dispatchers.IO) {
            try {
                Files.move(javaSource, javaTarget, *javaOptions)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(source, target)
            }
        }
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun openFileStore(file: Path): FileStore {
        file.requireSameSchemeAs(this)
        return JvmFileStore(file)
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun openFileWatcher(): FileWatcher = JvmFileWatcher()

    override fun getPath(platformPath: ByteString): Path =
        Paths.get(platformPath.decodeToString()).toAbsolutePath().toPath()

    override fun toPlatformPath(path: Path): ByteString {
        path.requireSameSchemeAs(this)
        return path.toJavaPath().toString().encodeToByteString()
    }

    companion object {
        private val JavaPath.provider: FileSystemProvider
            get() = fileSystem.provider()

        private fun Array<out CreateFileOption>.toJavaAttributes(): Array<FileAttribute<*>> =
            map { it.toJavaAttribute() }.toTypedArray()

        internal fun Array<out CopyFileOption>.toJavaOptions(): Array<CopyOption> {
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
