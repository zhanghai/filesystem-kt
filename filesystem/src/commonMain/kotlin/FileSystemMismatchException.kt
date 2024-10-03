package me.zhanghai.kotlin.filesystem

/**
 * Exception thrown when an attempt is made to invoke a file system function on a path with a scheme
 * identifying a different file system.
 */
public class FileSystemMismatchException(public val path: Path, public val expectedScheme: String) :
    IllegalArgumentException("Path \"$path\" doesn't have the expected scheme \"$expectedScheme\"")

public fun Path.requireScheme(scheme: String) {
    if (this.scheme != scheme) {
        throw FileSystemMismatchException(this, scheme)
    }
}

public fun Path.requireSameSchemeAs(fileSystem: FileSystem) {
    requireScheme(fileSystem.scheme)
}
