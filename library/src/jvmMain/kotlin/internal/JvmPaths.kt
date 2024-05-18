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

import java.net.URI
import java.nio.file.Path as JavaPath
import java.nio.file.Paths
import java.nio.file.spi.FileSystemProvider
import kotlinx.io.bytestring.encodeToByteString
import me.zhanghai.kotlin.filesystem.Path
import me.zhanghai.kotlin.filesystem.PlatformFileSystem
import me.zhanghai.kotlin.filesystem.Uri

internal fun JavaPath.toPath(): Path {
    require(isAbsolute) { "Path \"$this\" is not absolute" }
    val uri = toUri()
    require(uri.scheme == PlatformFileSystem.SCHEME) {
        "Expecting path scheme \"${PlatformFileSystem.SCHEME}\" but found \"${uri.scheme}\""
    }
    val rootUri =
        Uri.ofEncoded(scheme = PlatformFileSystem.SCHEME, encodedHost = uri.host, encodedPath = "/")
    return Path(rootUri, map { it.toString().encodeToByteString() })
}

internal fun Path.toJavaPath(): JavaPath {
    require(scheme == PlatformFileSystem.SCHEME) {
        "Expecting path scheme \"${PlatformFileSystem.SCHEME}\" but found \"$scheme\""
    }
    return fileSystemTag as JavaPath?
        ?: Paths.get(URI(toUri().toString())).also { fileSystemTag = it }
}

internal val JavaPath.provider: FileSystemProvider
    get() = fileSystem.provider()
