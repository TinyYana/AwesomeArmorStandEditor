package com.tinyyana.awesomeArmorStandEditor.edit

import com.tinyyana.awesomeArmorStandEditor.AaseKeys
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/** The editor tool: an ordinary item PDC-marked so we can recognise it. Legacy ItemMeta = cross-platform. */
object ToolItem {

    @Suppress("DEPRECATION")
    fun create(keys: AaseKeys, material: Material, nameLegacy: String, loreLegacy: List<String>): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(nameLegacy)
        meta.lore = loreLegacy
        meta.persistentDataContainer.set(keys.tool, PersistentDataType.BYTE, 1)
        item.itemMeta = meta
        return item
    }

    fun isTool(keys: AaseKeys, item: ItemStack?): Boolean {
        val meta = item?.itemMeta ?: return false
        return meta.persistentDataContainer.has(keys.tool, PersistentDataType.BYTE)
    }
}
