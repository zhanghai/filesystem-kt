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

import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystemException as JavaFileSystemException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.LinkOption as JavaLinkOption
import java.nio.file.Path as JavaPath
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.io.bytestring.encodeToByteString
import me.zhanghai.kotlin.filesystem.BasicWatchFileOption
import me.zhanghai.kotlin.filesystem.FileWatcher
import me.zhanghai.kotlin.filesystem.LinkOption
import me.zhanghai.kotlin.filesystem.NotDirectoryException
import me.zhanghai.kotlin.filesystem.Path
import me.zhanghai.kotlin.filesystem.PlatformFileSystem
import me.zhanghai.kotlin.filesystem.WatchFileEvent
import me.zhanghai.kotlin.filesystem.WatchFileOption
import me.zhanghai.kotlin.filesystem.io.AsyncCloseableFlow
import me.zhanghai.kotlin.filesystem.requireScheme

internal class JvmFileWatcher private constructor(private val watchService: WatchService) :
    FileWatcher {
    private val mutex = Mutex()

    private var isClosed = false

    private val keyToFlowsMutex = Mutex()
    private val keyToFlows = mutableMapOf<WatchKey, MutableList<WatchFileFlow>>()

    private val poller = Poller()

    @Throws(CancellationException::class, IOException::class)
    override suspend fun watch(
        file: Path,
        vararg options: WatchFileOption,
    ): AsyncCloseableFlow<WatchFileEvent> {
        mutex.withLock {
            check(!isClosed) { "FileWatcher is closed" }
            file.requireScheme(PlatformFileSystem.SCHEME)
            val watchFileOptions = options.toWatchFileOptions()
            val javaFile = file.toJavaPath()
            val attributesNoFollowLinks =
                withContext(Dispatchers.IO) {
                    try {
                        Files.readAttributes(
                            javaFile,
                            BasicFileAttributes::class.java,
                            JavaLinkOption.NOFOLLOW_LINKS,
                        )
                    } catch (e: JavaFileSystemException) {
                        throw e.toFileSystemException()
                    }
                }
            val attributes =
                if (!watchFileOptions.noFollowLinks && attributesNoFollowLinks.isSymbolicLink) {
                    withContext(Dispatchers.IO) {
                        try {
                            Files.readAttributes(javaFile, BasicFileAttributes::class.java)
                        } catch (e: JavaFileSystemException) {
                            throw e.toFileSystemException()
                        }
                    }
                } else {
                    attributesNoFollowLinks
                }
            if (watchFileOptions.watchDirectoryEntries && !attributes.isDirectory) {
                throw NotDirectoryException(file)
            }
            val watchParentDirectory =
                !attributes.isDirectory || !watchFileOptions.watchDirectoryEntries
            val watchable: JavaPath
            val context: String?
            if (watchParentDirectory) {
                watchable =
                    javaFile.parent
                        ?: throw UnsupportedOperationException(
                            "Cannot watch file \"${file.toUri()}\" because it does not have a" +
                                " parent"
                        )
                context = javaFile.name
            } else {
                watchable = javaFile
                context = null
            }
            return keyToFlowsMutex.withLock {
                val watchKey =
                    withContext(Dispatchers.IO) {
                        try {
                            watchable.register(watchService, *EVENT_KINDS)
                        } catch (e: JavaFileSystemException) {
                            throw e.toFileSystemException()
                        }
                    }
                WatchFileFlow(watchKey, context, file).also {
                    keyToFlows.getOrPut(watchKey) { mutableListOf() } += it
                }
            }
        }
    }

    @Throws(CancellationException::class, IOException::class)
    override suspend fun close() {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                poller.interrupt()
                poller.join()
                try {
                    watchService.close()
                } catch (e: JavaFileSystemException) {
                    throw e.toFileSystemException()
                }
            }
        }
    }

    companion object {
        private val EVENT_KINDS =
            arrayOf(
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY,
            )

        private val pollerId = AtomicInteger()

        @Throws(CancellationException::class, IOException::class)
        suspend operator fun invoke(): JvmFileWatcher {
            val fileSystem = FileSystems.getDefault()
            val watchService =
                withContext(Dispatchers.IO) {
                    try {
                        fileSystem.newWatchService()
                    } catch (e: JavaFileSystemException) {
                        throw e.toFileSystemException()
                    }
                }
            return JvmFileWatcher(watchService)
        }
    }

    private inner class Poller : Thread("JvmFileWatcher.Poller-${pollerId.getAndIncrement()}") {
        init {
            isDaemon = true
            start()
        }

        override fun run() {
            while (true) {
                if (interrupted()) {
                    break
                }
                val key =
                    try {
                        watchService.take()
                    } catch (e: ClosedWatchServiceException) {
                        break
                    } catch (e: InterruptedException) {
                        break
                    }
                val javaEvents = key.pollEvents()
                if (javaEvents.isNotEmpty()) {
                    runBlocking {
                        keyToFlowsMutex.withLock {
                            // Skip if we no longer have any flows watching this key.
                            val flows = keyToFlows[key] ?: return@runBlocking
                            for (javaEvent in javaEvents) {
                                val eventContext = (javaEvent.context() as JavaPath?)?.toString()
                                for (flow in flows) {
                                    if (isInterrupted) {
                                        return@runBlocking
                                    }
                                    val flowContext = flow.context
                                    if (
                                        eventContext != null &&
                                            flowContext != null &&
                                            eventContext != flowContext
                                    ) {
                                        continue
                                    }
                                    val path =
                                        if (flowContext != null) {
                                            // We need to match a certain context, so this is
                                            // watching the parent directory for the file itself.
                                            flow.file
                                        } else {
                                            // We don't have a context to match against, so this is
                                            // for watching directory entries, and we should provide
                                            // the changed entry path.
                                            eventContext?.let {
                                                flow.file.resolve(it.encodeToByteString())
                                            }
                                        }
                                    val event = JvmWatchFileEvent(path)
                                    flow.emit(event)
                                }
                            }
                        }
                    }
                    // Check if we just returned from runBlocking() due to interruption.
                    if (interrupted()) {
                        break
                    }
                }
                key.reset()
            }
        }
    }

    private inner class WatchFileFlow(
        private val key: WatchKey,
        val context: String?,
        val file: Path,
    ) : AsyncCloseableFlow<WatchFileEvent> {
        private val flow = MutableSharedFlow<WatchFileEvent>()

        private var isClosed = false

        suspend fun emit(value: WatchFileEvent) {
            flow.emit(value)
        }

        override suspend fun collect(collector: FlowCollector<WatchFileEvent>) {
            flow.collect(collector)
        }

        override suspend fun close() {
            keyToFlowsMutex.withLock {
                if (isClosed) {
                    return
                }
                val flows = keyToFlows[key]!!
                if (flows.size > 1) {
                    return
                }
                check(flows.single() == this)
                withContext(Dispatchers.IO) { key.cancel() }
                keyToFlows -= key
                isClosed = true
            }
        }
    }
}

internal class WatchFileOptions(val watchDirectoryEntries: Boolean, val noFollowLinks: Boolean)

internal fun Array<out WatchFileOption>.toWatchFileOptions(): WatchFileOptions {
    var watchDirectoryEntries = false
    var noFollowLinks = false
    for (option in this) {
        when (option) {
            BasicWatchFileOption.WATCH_DIRECTORY_ENTRIES -> watchDirectoryEntries = true
            LinkOption.NO_FOLLOW_LINKS -> noFollowLinks = true
            else -> throw UnsupportedOperationException("Unsupported option $option")
        }
    }
    return WatchFileOptions(watchDirectoryEntries, noFollowLinks)
}

internal class JvmWatchFileEvent(override val file: Path?) : WatchFileEvent
