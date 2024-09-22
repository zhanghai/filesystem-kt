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

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.io.IOException
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import me.zhanghai.kotlin.filesystem.io.AsyncSink
import me.zhanghai.kotlin.filesystem.io.AsyncSource

public fun Path.Companion.fromPlatformPath(platformPath: ByteString): Path =
    FileSystemRegistry.requirePlatformFileSystem().getPath(platformPath)

public fun Path.Companion.fromPlatformPath(platformPath: String): Path =
    fromPlatformPath(platformPath.encodeToByteString())

public fun Path.toPlatformPath(): ByteString =
    FileSystemRegistry.requirePlatformFileSystem().toPlatformPath(this)

public fun Path.toPlatformPathString(): String = toPlatformPath().toString()

public fun Path.requireFileSystem(): FileSystem = FileSystemRegistry.requireFileSystem(scheme)

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.toRealPath(): Path = requireFileSystem().getRealPath(this)

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.checkAccess(vararg modes: AccessMode) {
    requireFileSystem().checkAccess(this, *modes)
}

@Throws(CancellationException::class)
public suspend inline fun Path.isAccessible(vararg modes: AccessMode): Boolean =
    try {
        checkAccess(*modes)
        true
    } catch (e: IOException) {
        false
    }

@Throws(CancellationException::class)
public suspend inline fun Path.exists(followLinks: Boolean = true): Boolean =
    try {
        if (followLinks) {
            checkAccess()
        } else {
            readMetadata(LinkOption.NO_FOLLOW_LINKS)
        }
        true
    } catch (e: IOException) {
        false
    }

@Throws(CancellationException::class)
public suspend inline fun Path.notExists(followLinks: Boolean = true): Boolean =
    try {
        if (followLinks) {
            checkAccess()
        } else {
            readMetadata(LinkOption.NO_FOLLOW_LINKS)
        }
        false
    } catch (e: NoSuchFileException) {
        true
    } catch (e: IOException) {
        false
    }

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.openMetadataView(
    vararg options: FileMetadataOption
): FileMetadataView = requireFileSystem().openMetadataView(this, *options)

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.readMetadata(vararg options: FileMetadataOption): FileMetadata =
    requireFileSystem().readMetadata(this, *options)

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.openContent(vararg options: FileContentOption): FileContent =
    requireFileSystem().openContent(this, *options)

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.openSource(vararg options: FileContentOption): AsyncSource =
    requireFileSystem().openSource(this, *options)

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.openSink(
    vararg options: FileContentOption = FileSystem.OPEN_SINK_OPTIONS_DEFAULT
): AsyncSink = requireFileSystem().openSink(this, *options)

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.openDirectoryStream(
    vararg options: DirectoryStreamOption
): DirectoryStream = requireFileSystem().openDirectoryStream(this, *options)

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.readDirectory(vararg options: DirectoryStreamOption): List<Path> =
    requireFileSystem().readDirectory(this, *options)

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.createDirectory(vararg options: CreateFileOption) {
    requireFileSystem().createDirectory(this, *options)
}

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.readSymbolicLink(): ByteString =
    requireFileSystem().readSymbolicLink(this)

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.createSymbolicLinkTo(
    target: ByteString,
    vararg options: CreateFileOption,
) {
    requireFileSystem().createSymbolicLink(this, target, *options)
}

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.createHardLinkTo(existing: Path) {
    requireFileSystem().createHardLink(this, existing)
}

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.delete() {
    requireFileSystem().delete(this)
}

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.deleteIfExists(): Boolean =
    try {
        delete()
        true
    } catch (e: NoSuchFileException) {
        false
    }

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.isSameFileAs(other: Path): Boolean =
    requireFileSystem().isSameFile(this, other)

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.copyTo(target: Path, vararg options: CopyFileOption) {
    requireFileSystem().copy(this, target, *options)
}

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.moveTo(target: Path, vararg options: CopyFileOption) {
    requireFileSystem().move(this, target, *options)
}

@Throws(CancellationException::class, IOException::class)
public suspend inline fun Path.openFileStore(): FileStore = requireFileSystem().openFileStore(this)
