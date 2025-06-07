package org.itstory.imageview.service

import org.itstory.imageview.domain.OnePage
import org.itstory.imageview.domain.Thumbnail
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Base64
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.notExists

@Service
class MainService(
    @Value("\${base-path}")
    private val basePath: String,
    @Value("\${placeholder}")
    private val placeholder: String,
) {
    fun encode(path: Path): String {
        val bytes = path.name.toByteArray()
        val encoder = Base64.getUrlEncoder()
        return encoder.encodeToString(bytes)
    }

    fun decode(name: String): String {
        val decoder = Base64.getUrlDecoder()
        val bytes = decoder.decode(name)
        return bytes.decodeToString()
    }

    fun artistsThumb(): List<Thumbnail> {
        val list = mutableListOf<Thumbnail>()
        Paths.get(basePath).listDirectoryEntries()
            .filter { it.isDirectory() }
            .forEach { list += artistThumb(it) }
        return list
    }

    fun artistThumb(artist: Path): Thumbnail {
        val a = encode(artist)
        val thumb = Paths.get("$artist/_.webp")
        val url = if (thumb.exists()) {
            "/image/$a/_.webp"
        } else {
            try { artistThumbUrl(artist) }
            catch (_: Exception) { placeholder }
        }
        return Thumbnail(artist.name, "/artist/$a", url)
    }

    fun artistThumbUrl(artist: Path): String {
        val book = artist.listDirectoryEntries()
            .first { it.isDirectory() }
        val cover = book.listDirectoryEntries()
            .first { it.isRegularFile() }
        val a = encode(artist)
        val b = encode(book)
        return "/image/$a/$b/${cover.name}"
    }

    fun worksThumb(artist: Path): List<Thumbnail> {
        val list = mutableListOf<Thumbnail>()
        val a = encode(artist)
        artist.listDirectoryEntries()
            .filter { it.isDirectory() }
            .forEach { list += workThumb(a, it) }
        return list
    }

    fun workThumb(a: String, work: Path): Thumbnail {
        val b = encode(work)
        val url = try {
            val thumb = workThumbName(work)
            "/image/$a/$b/$thumb"
        } catch (_: Exception) { placeholder }
        return Thumbnail(work.name, "/artist/$a/$b", url)
    }

    fun workThumbName(work: Path): String {
        val thumb = Paths.get("$work/_.webp")
        return if (thumb.exists()) {
            "_.webp"
        } else {
            work.listDirectoryEntries()
                .first { it.isRegularFile() }
                .name
        }
    }

    fun pagesThumb(a: String, b: String, work: Path): List<Thumbnail> {
        val list = mutableListOf<Thumbnail>()
        work.listDirectoryEntries()
            .filter { it.isRegularFile() }
            .forEach { list += Thumbnail(
                name = it.name,
                href = "/artist/$a/$b/${it.name}",
                url = "/image/$a/$b/${it.name}",
            ) }
        return list
    }

    fun onePage(a: String, b: String, page: String): OnePage {
        val artist = decode(a)
        val work = decode(b)
        val pages = Paths.get(basePath, artist, work).listDirectoryEntries()
        val idx = pages.indexOfFirst { it.name == page }
        val prev = pages.getOrNull(idx-1)?.name ?: page
        val next = pages.getOrNull(idx+1)?.name ?: page
        return OnePage(
            prev = "/artist/$a/$b/$prev",
            next = "/artist/$a/$b/$next",
            src = "/image/$a/$b/$page",
        )
    }

    fun responseEntity(path: Path): ResponseEntity<Resource> {
        if (path.notExists())
            return ResponseEntity.notFound().build()
        val resource = InputStreamResource(path.inputStream())
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${path.name}\"")
            .header(HttpHeaders.CONTENT_LENGTH, path.fileSize().toString())
            .body(resource)
    }
}
