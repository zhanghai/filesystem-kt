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

/**
 * A registry for file systems.
 *
 * Each file system is identified by its URI scheme. For example, the platform file system (if any)
 * is identified by the URI scheme `file`.
 *
 * This class is safe for use by multiple concurrent threads.
 *
 * @see FileSystem
 * @see PlatformFileSystem
 */
public object FileSystemRegistry {
    @Volatile private var fileSystems: Map<String, FileSystem> = emptyMap()

    init {
        platformFileSystem?.let { addFileSystem(it) }
    }

    /**
     * Get a mapping of all the registered file systems, keyed by their URI schemes.
     *
     * @return a mapping of all the registered file systems
     */
    public fun getFileSystems(): Map<String, FileSystem> = fileSystems.toMap()

    /**
     * Add a file system to this registry.
     *
     * @param fileSystem the file system
     */
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

    /**
     * Get a registered file system for a URI scheme, or `null` if none is registered for the URI
     * scheme.
     *
     * @param scheme the URI scheme of the file system
     * @return the registered file system or `null`
     */
    public fun getFileSystem(scheme: String): FileSystem? = fileSystems[scheme]

    /**
     * Remove a registered file system for a URI scheme.
     *
     * @param scheme the URI scheme of the file system
     */
    public fun removeFileSystem(scheme: String) {
        fileSystems -= scheme
    }
}

/**
 * Get a registered file system for a URI scheme, or throw an exception if none is registered.
 *
 * @return the registered file system
 * @throws IllegalStateException if no file system is registered for the URI scheme
 */
public fun FileSystemRegistry.requireFileSystem(scheme: String): FileSystem =
    checkNotNull(getFileSystem(scheme)) { "No file system registered for scheme \"$scheme\"" }

internal expect val platformFileSystem: PlatformFileSystem?

/** The registered platform file system, or `null` if none is registered. */
public val FileSystemRegistry.platformFileSystem: PlatformFileSystem?
    get() = getFileSystem(PlatformFileSystem.SCHEME) as PlatformFileSystem?

/**
 * Get the registered platform file system, or throw an exception if none is registered.
 *
 * @return the registered platform file system
 * @throws IllegalStateException if no platform file system is registered
 */
public fun FileSystemRegistry.requirePlatformFileSystem(): PlatformFileSystem =
    checkNotNull(platformFileSystem) { "No platform file system registered" }
