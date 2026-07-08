package com.tinyyana.awesomeArmorStandEditor.store

import com.tinyyana.awesomeArmorStandEditor.model.Scene
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Portable share code: gzip the scene JSON and URL-safe Base64 it, with a versioned prefix so future
 * formats can be told apart. A code is copy-pasteable in chat and imported anywhere the plugin runs.
 *
 * Decode is a trust boundary (arbitrary player-supplied text): the Base64 length and the decompressed
 * size are both capped to stop oversized/decompression-bomb inputs, and any malformed data returns
 * null instead of throwing into game logic.
 */
object ShareCode {

    private const val PREFIX = "AASE1:"
    private const val MAX_CODE_LEN = 60_000        // a code longer than this is rejected outright
    private const val MAX_JSON_BYTES = 1024 * 1024 // cap the decompressed JSON at 1 MiB

    fun encode(scene: Scene): String {
        val json = SceneCodec.toJson(scene).toByteArray(Charsets.UTF_8)
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(json) }
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bos.toByteArray())
    }

    /** Returns null on any malformed/oversized code. */
    fun decode(code: String): Scene? {
        val body = code.trim().removePrefix(PREFIX)
        if (body.isEmpty() || body.length > MAX_CODE_LEN) return null
        return runCatching {
            val gz = Base64.getUrlDecoder().decode(body)
            val json = GZIPInputStream(ByteArrayInputStream(gz)).use { readBounded(it, MAX_JSON_BYTES) }
                ?: return null
            SceneCodec.fromJson(String(json, Charsets.UTF_8))
        }.getOrNull()
    }

    /** Read at most [max] bytes; null if the stream would exceed it (avoids decompression bombs). */
    private fun readBounded(input: InputStream, max: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > max) return null
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}
