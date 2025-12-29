package dk.skancode.skanmate.data.service

import dk.skancode.skanmate.ImageData
import dk.skancode.skanmate.deleteFile
import dk.skancode.skanmate.loadLocalImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

interface FileService {
    suspend fun loadLocalFile(localFilePath: String): ImageData

    suspend fun deleteLocalFile(localFilePath: String)
    suspend fun deleteLocalFileDeferred(localFilePath: String, start: CoroutineStart = CoroutineStart.DEFAULT, scope: CoroutineScope): Deferred<Unit>

    companion object Companion {
        val instance: FileService = FileServiceImpl
    }
}


private object FileServiceImpl: FileService {
    override suspend fun loadLocalFile(localFilePath: String): ImageData {
        return loadLocalImage(localFilePath)
    }

    override suspend fun deleteLocalFile(localFilePath: String) {
        deleteFile(localFilePath)
    }

    override suspend fun deleteLocalFileDeferred(
        localFilePath: String,
        start: CoroutineStart,
        scope: CoroutineScope,
    ): Deferred<Unit> {
        return scope.async(start = start) {
            deleteLocalFile(localFilePath)
        }
    }
}