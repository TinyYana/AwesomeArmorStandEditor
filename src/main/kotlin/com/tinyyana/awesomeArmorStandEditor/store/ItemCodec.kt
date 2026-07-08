package com.tinyyana.awesomeArmorStandEditor.store

import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64

/**
 * ItemStack <-> Base64 via Bukkit's ConfigurationSerializable stream. This is the stable,
 * cross-platform (Spigot + Paper) path — avoids Paper-only ItemStack.serializeAsBytes().
 */
object ItemCodec {

    fun encode(item: ItemStack): String {
        ByteArrayOutputStream().use { bos ->
            BukkitObjectOutputStream(bos).use { it.writeObject(item) }
            return Base64.getEncoder().encodeToString(bos.toByteArray())
        }
    }

    /** Returns null on any malformed/incompatible data rather than throwing into game logic. */
    fun decode(data: String): ItemStack? = try {
        val bytes = Base64.getDecoder().decode(data)
        ByteArrayInputStream(bytes).use { bis ->
            BukkitObjectInputStream(bis).use { it.readObject() as? ItemStack }
        }
    } catch (e: Exception) {
        null
    }
}
