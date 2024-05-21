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

/**
 * A directory entry with additional file type information.
 *
 * @see DirectoryStream
 * @see BasicDirectoryStreamOption.READ_TYPE
 */
public interface DirectoryEntryWithType : DirectoryEntry {
    /** The type of the file, or `null` if it couldn't be read. */
    public val type: FileType?

    /**
     * The exception occurred when reading type of the file, or `null` when the type is successfully
     * read.
     */
    public val typeException: Throwable?
}

/**
 * A directory entry with additional file metadata information.
 *
 * @see DirectoryStream
 * @see BasicDirectoryStreamOption.READ_METADATA
 */
public interface DirectoryEntryWithMetadata : DirectoryEntryWithType {
    override val type: FileType?
        get() = metadata?.type

    override val typeException: Throwable?
        get() = metadataException

    /** The metadata of the file, or `null` if it couldn't be read. */
    public val metadata: FileMetadata?

    /**
     * The exception occurred when reading metadata of the file, or `null` when the metadata is
     * successfully read.
     */
    public val metadataException: Throwable?
}
