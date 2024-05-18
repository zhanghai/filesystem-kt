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

import kotlin.concurrent.Volatile

public object FileSystemRegistry {
    @Volatile private var fileSystems: Map<String, FileSystem> = emptyMap()

    init {
        platformFileSystem?.let { addFileSystem(it) }
    }

    public fun getFileSystems(): Map<String, FileSystem> = fileSystems

    public fun addFileSystem(fileSystem: FileSystem) {
        val scheme = fileSystem.scheme
        if (scheme == PlatformFileSystem.SCHEME) {
            require(fileSystem is PlatformFileSystem) {
                "File system with scheme \"${PlatformFileSystem.SCHEME}\" must be a" +
                    " ${PlatformFileSystem::class.simpleName}"
            }
        }
        fileSystems += scheme to fileSystem
    }

    public fun getFileSystem(scheme: String): FileSystem? = fileSystems[scheme]

    public fun removeFileSystem(scheme: String) {
        fileSystems -= scheme
    }
}

internal expect val platformFileSystem: PlatformFileSystem?

public val FileSystemRegistry.platformFileSystem: PlatformFileSystem?
    get() = getFileSystem(PlatformFileSystem.SCHEME) as PlatformFileSystem?

public fun FileSystemRegistry.requirePlatformFileSystem(): PlatformFileSystem =
    requireNotNull(platformFileSystem) { "No platform file system registered" }
