package com.tinyyana.awesomeArmorStandEditor

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.configuration.file.YamlConfiguration
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Guards the shipped lang files. A missing key renders as `<red>the.key` in-game and a dropped
 * `{placeholder}` silently swallows a number, so both are checked here rather than in QA.
 */
class LangFilesTest {

    private fun load(code: String): YamlConfiguration {
        val stream = javaClass.getResourceAsStream("/lang/$code.yml") ?: fail("lang/$code.yml is not on the classpath")
        return YamlConfiguration.loadConfiguration(InputStreamReader(stream, StandardCharsets.UTF_8))
    }

    private fun strings(cfg: YamlConfiguration): Map<String, String> =
        cfg.getKeys(true).filter { cfg.isString(it) }.associateWith { cfg.getString(it)!! }

    private fun placeholders(text: String): Set<String> =
        Regex("\\{[a-z]+}").findAll(text).map { it.value }.toSet()

    @Test
    fun everyLanguageHasTheSameKeys() {
        val reference = strings(load("zh_TW"))
        for (code in listOf("en")) {
            val other = strings(load(code))
            assertEquals(reference.keys.sorted(), other.keys.sorted(), "lang/$code.yml key set differs from zh_TW")
        }
    }

    @Test
    fun everyLanguageKeepsThePlaceholders() {
        val reference = strings(load("zh_TW"))
        for (code in listOf("en")) {
            for ((key, text) in strings(load(code))) {
                val expected = reference[key]?.let { placeholders(it) } ?: continue
                assertEquals(expected, placeholders(text), "lang/$code.yml `$key` lost or invented a placeholder")
            }
        }
    }

    @Test
    fun everyStringAndGuidePageParsesAsMiniMessage() {
        val mm = MiniMessage.miniMessage()
        for (code in listOf("zh_TW", "en")) {
            val cfg = load(code)
            val pages = cfg.getStringList("guide.pages")
            assertTrue(pages.size >= 10, "lang/$code.yml should still have the full guide book")
            for (text in strings(cfg).values + pages) {
                runCatching { mm.deserialize(text) }
                    .onFailure { fail("lang/$code.yml: MiniMessage rejected \"$text\" — ${it.message}") }
            }
        }
    }
}
