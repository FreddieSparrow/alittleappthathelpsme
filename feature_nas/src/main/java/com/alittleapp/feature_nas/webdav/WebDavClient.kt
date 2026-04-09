package com.alittleapp.feature_nas.webdav

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Minimal WebDAV client for UGREEN NAS (and any standards-compliant WebDAV server).
 *
 * All calls are made on the local network — no external servers.
 * Supports Basic Auth (HTTPS recommended on LAN, HTTP fallback for setup).
 *
 * Core operations implemented:
 *  - PROPFIND  → list directory contents
 *  - GET       → download file
 *  - PUT       → upload file
 *  - MKCOL     → create directory
 *  - DELETE    → delete file or directory
 */
class WebDavClient(private val config: NasConfig) {

    private val http = HttpClient(OkHttp) {
        engine {
            preconfigured = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        }
    }

    private fun buildUrl(path: String): String {
        val base = config.baseUrl.trimEnd('/')
        val p = if (path.startsWith("/")) path else "/$path"
        return "$base$p"
    }

    private fun HttpRequestBuilder.auth() {
        headers {
            val credentials = "${config.username}:${config.password}"
            val encoded = android.util.Base64.encodeToString(credentials.toByteArray(), android.util.Base64.NO_WRAP)
            append(HttpHeaders.Authorization, "Basic $encoded")
        }
    }

    /** List directory contents via PROPFIND. Returns parsed [NasEntry] list. */
    suspend fun listDirectory(path: String): Result<List<NasEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = http.request(buildUrl(path)) {
                method = HttpMethod("PROPFIND")
                auth()
                headers { append("Depth", "1") }
                setBody(PROPFIND_BODY)
                contentType(ContentType.Application.Xml)
            }
            if (!response.status.isSuccess()) throw WebDavException("PROPFIND failed: ${response.status}")
            parsePropfindResponse(response.bodyAsText(), path)
        }
    }

    /** Download a file from [remotePath] and save to [localFile]. Reports progress via [onProgress]. */
    suspend fun downloadFile(
        remotePath: String,
        localFile: File,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val response = http.get(buildUrl(remotePath)) { auth() }
            if (!response.status.isSuccess()) throw WebDavException("GET failed: ${response.status}")
            val total = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
            var downloaded = 0L
            val bytes = response.readRawBytes()
            localFile.writeBytes(bytes)
            onProgress(1f)
            localFile
        }
    }

    /** Upload [localFile] to [remotePath]. Reports progress via [onProgress]. */
    suspend fun uploadFile(
        localFile: File,
        remotePath: String,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = localFile.readBytes()
            val response = http.put(buildUrl(remotePath)) {
                auth()
                setBody(bytes)
                contentType(ContentType.Application.OctetStream)
            }
            if (!response.status.isSuccess() && response.status.value != 201) {
                throw WebDavException("PUT failed: ${response.status}")
            }
            onProgress(1f)
        }
    }

    /** Create a directory at [path]. */
    suspend fun createDirectory(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = http.request(buildUrl(path)) {
                method = HttpMethod("MKCOL")
                auth()
            }
            if (!response.status.isSuccess() && response.status.value != 405) {
                throw WebDavException("MKCOL failed: ${response.status}")
            }
        }
    }

    /** Delete a file or directory at [path]. */
    suspend fun delete(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = http.delete(buildUrl(path)) { auth() }
            if (!response.status.isSuccess()) throw WebDavException("DELETE failed: ${response.status}")
        }
    }

    fun close() = http.close()

    companion object {
        private val PROPFIND_BODY = """
            <?xml version="1.0" encoding="utf-8"?>
            <D:propfind xmlns:D="DAV:">
              <D:prop>
                <D:displayname/>
                <D:getcontentlength/>
                <D:getlastmodified/>
                <D:resourcetype/>
              </D:prop>
            </D:propfind>
        """.trimIndent()

        /** Very simple PROPFIND XML parser — no external XML library required. */
        fun parsePropfindResponse(xml: String, basePath: String): List<NasEntry> {
            val entries = mutableListOf<NasEntry>()
            val hrefRegex = Regex("<D:href>(.*?)</D:href>", RegexOption.IGNORE_CASE)
            val nameRegex = Regex("<D:displayname>(.*?)</D:displayname>", RegexOption.IGNORE_CASE)
            val sizeRegex = Regex("<D:getcontentlength>(.*?)</D:getcontentlength>", RegexOption.IGNORE_CASE)
            val collectionRegex = Regex("<D:collection\\s*/?>", RegexOption.IGNORE_CASE)
            val modifiedRegex = Regex("<D:getlastmodified>(.*?)</D:getlastmodified>", RegexOption.IGNORE_CASE)

            // Split by <D:response>
            val responses = xml.split(Regex("<D:response[^>]*>", RegexOption.IGNORE_CASE)).drop(1)
            for (response in responses) {
                val href = hrefRegex.find(response)?.groupValues?.get(1)?.trim() ?: continue
                val decodedHref = java.net.URLDecoder.decode(href, "UTF-8")
                val name = nameRegex.find(response)?.groupValues?.get(1)?.trim()
                    ?: decodedHref.trimEnd('/').substringAfterLast('/')
                if (name.isBlank()) continue
                val isDir = collectionRegex.containsMatchIn(response)
                val size = sizeRegex.find(response)?.groupValues?.get(1)?.trim()?.toLongOrNull() ?: 0L
                val modified = modifiedRegex.find(response)?.groupValues?.get(1)?.trim() ?: ""
                entries.add(NasEntry(name = name, path = decodedHref, isDirectory = isDir, size = size, lastModified = modified))
            }
            // Skip the first entry if it represents the requested directory itself
            return if (entries.firstOrNull()?.path?.trimEnd('/') == basePath.trimEnd('/'))
                entries.drop(1) else entries
        }
    }
}

data class NasConfig(
    val baseUrl: String,      // e.g. "http://192.168.1.100:5005/dav"
    val username: String,
    val password: String
)

data class NasEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: String
)

class WebDavException(message: String) : Exception(message)
