package com.tinyyana.awesomeArmorStandEditor.integration

import org.bukkit.plugin.PluginManager
import java.lang.reflect.Method

/**
 * Optional soft integration with LycoLib's AuditLog, reached purely by reflection so the plugin
 * neither compiles against nor hard-depends on LycoLib. No-op when LycoLib is absent (the SpigotMC
 * public case). Never throws into game logic.
 */
object LycoLibHook {
    private var logMethod: Method? = null

    fun init(pluginManager: PluginManager) {
        if (pluginManager.getPlugin("LycoLib") == null) return
        runCatching {
            val cls = Class.forName("com.tinyyana.lycoLib.audit.AuditLog")
            // @JvmStatic log(plugin, player, action, detail)
            logMethod = cls.getMethod(
                "log", String::class.java, String::class.java, String::class.java, String::class.java,
            )
        }
    }

    val available: Boolean get() = logMethod != null

    fun audit(plugin: String, player: String, action: String, detail: String = "") {
        val m = logMethod ?: return
        runCatching { m.invoke(null, plugin, player, action, detail) }
    }
}
