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

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.FileSystemException as JavaFileSystemException
import java.nio.file.LinkOption as JavaLinkOption
import java.nio.file.OpenOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.asSource
import kotlinx.io.readTo
import me.zhanghai.kotlin.filesystem.BasicFileContentOption
import me.zhanghai.kotlin.filesystem.CreateFileOption
import me.zhanghai.kotlin.filesystem.FileContent
import me.zhanghai.kotlin.filesystem.FileContentOption
import me.zhanghai.kotlin.filesystem.LinkOption
import me.zhanghai.kotlin.filesystem.Path
import me.zhanghai.kotlin.filesystem.posix.PosixModeBit
import me.zhanghai.kotlin.filesystem.posix.PosixModeOption

internal class JvmFileContent
private constructor(private val file: Path, private val fileChannel: FileChannel) : FileContent {
    @Throws(CancellationException::class, IOException::class)
    override suspend fun readAtMostTo(position: Long, sink: Buffer, byteCount: Long): Long {
        require(position >= 0) { "position ($position) < 0" }
        require(byteCount >= 0) { "byteCount ($byteCount) < 0" }
        return runInterruptible(Dispatchers.IO) {
            object : InputStream() {
                    var currentPosition = position

                    override fun read(): Int {
                        throw UnsupportedOperationException()
                    }

                    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                        if (Thread.interrupted()) {
                            throw CancellationException("Job was cancelled by interrupt")
                        }
                        val byteBuffer = ByteBuffer.wrap(bytes, offset, length)
                        val readByteCount =
                            try {
                                fileChannel.read(byteBuffer, currentPosition)
                            } catch (e: JavaFileSystemException) {
                                throw e.toFileSystemException(file)
                            }
                        if (readByteCount == -1) {
                            return -1
                        }
                        currentPosition += readByteCount
                        return readByteCount
                    }
                }
                .asSource()
                .readAtMostTo(sink, byteCount)
        }
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun write(position: Long, source: Buffer, byteCount: Long) {
        require(position >= 0) { "position ($position) < 0" }
        require(byteCount >= 0) { "byteCount ($byteCount) < 0" }
        require(byteCount <= source.size) {
            "byteCount ($byteCount) > source.size (${source.size})"
        }
        runInterruptible(Dispatchers.IO) {
            source.readTo(
                object : OutputStream() {
                    var currentPosition = position

                    override fun write(byte: Int) {
                        throw UnsupportedOperationException()
                    }

                    override fun write(bytes: ByteArray, offset: Int, length: Int) {
                        val byteBuffer = ByteBuffer.wrap(bytes, offset, length)
                        while (byteBuffer.hasRemaining()) {
                            if (Thread.interrupted()) {
                                throw CancellationException("Job was cancelled by interrupt")
                            }
                            val writtenByteCount =
                                try {
                                    fileChannel.write(byteBuffer, currentPosition)
                                } catch (e: JavaFileSystemException) {
                                    throw e.toFileSystemException(file)
                                }
                            currentPosition += writtenByteCount
                        }
                    }
                },
                byteCount,
            )
        }
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun getSize(): Long =
        runInterruptible(Dispatchers.IO) {
            try {
                fileChannel.size()
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(file)
            }
        }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun setSize(size: Long) {
        runInterruptible(Dispatchers.IO) {
            try {
                val channelSize = fileChannel.size()
                when {
                    size < channelSize -> fileChannel.truncate(size)
                    size > channelSize -> fileChannel.write(ByteBuffer.allocate(1), size - 1)
                    else -> {}
                }
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(file)
            }
        }
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun sync() {
        runInterruptible(Dispatchers.IO) {
            try {
                fileChannel.force(true)
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(file)
            }
        }
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun close() {
        withContext(Dispatchers.IO) {
            try {
                fileChannel.close()
            } catch (e: JavaFileSystemException) {
                throw e.toFileSystemException(file)
            }
        }
    }

    companion object {
        @Throws(CancellationException::class, IOException::class)
        suspend operator fun invoke(file: Path, vararg options: FileContentOption): JvmFileContent {
            val javaFile = file.toJavaPath()
            val (javaOptions, javaAttributes) = options.toJavaOptionsAndAttributes()
            val channel =
                withContext(Dispatchers.IO) {
                    try {
                        FileChannel.open(javaFile, javaOptions, *javaAttributes)
                    } catch (e: JavaFileSystemException) {
                        throw e.toFileSystemException(file)
                    }
                }
            return JvmFileContent(file, channel)
        }

        private fun Array<out FileContentOption>.toJavaOptionsAndAttributes():
            Pair<Set<OpenOption>, Array<out FileAttribute<*>>> {
            val javaOptions = mutableSetOf<OpenOption>()
            val javaAttributeList = mutableListOf<FileAttribute<*>>()
            for (option in this) {
                when (option) {
                    BasicFileContentOption.READ -> javaOptions += StandardOpenOption.READ
                    BasicFileContentOption.WRITE -> javaOptions += StandardOpenOption.WRITE
                    BasicFileContentOption.APPEND -> javaOptions += StandardOpenOption.APPEND
                    BasicFileContentOption.TRUNCATE_EXISTING ->
                        javaOptions += StandardOpenOption.TRUNCATE_EXISTING
                    BasicFileContentOption.CREATE -> javaOptions += StandardOpenOption.CREATE
                    BasicFileContentOption.CREATE_NEW ->
                        javaOptions += StandardOpenOption.CREATE_NEW
                    LinkOption.NO_FOLLOW_LINKS -> javaOptions += JavaLinkOption.NOFOLLOW_LINKS
                    is CreateFileOption -> javaAttributeList += option.toJavaAttribute()
                    else -> throw UnsupportedOperationException("Unsupported option $option")
                }
            }
            val javaAttributes = javaAttributeList.toTypedArray()
            return javaOptions to javaAttributes
        }

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
    }
}
