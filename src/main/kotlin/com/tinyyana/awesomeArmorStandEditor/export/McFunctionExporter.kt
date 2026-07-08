package com.tinyyana.awesomeArmorStandEditor.export

import com.tinyyana.awesomeArmorStandEditor.edit.InterpolationOps
import com.tinyyana.awesomeArmorStandEditor.model.Animation
import com.tinyyana.awesomeArmorStandEditor.model.ArmorStandElement
import com.tinyyana.awesomeArmorStandEditor.model.DisplayElement
import com.tinyyana.awesomeArmorStandEditor.model.Scene

/**
 * Best-effort datapack export. Produces a file tree (relative path -> content):
 *  - pack.mcmeta
 *  - data/aase/function/summon.mcfunction         (place the whole scene, entities Tag'd)
 *  - if animated: load / tick / frames/frame_N     (scoreboard-driven keyframe playback)
 *
 * NBT/format caveat: same as SummonExporter (Pose in degrees, display transformation stable since
 * 1.19.4). `pack_format` and the `function` folder name are version-sensitive — adjust for your MC.
 */
object McFunctionExporter {

    private const val NS = "aase"
    private const val FRAME_STEP = 2

    fun export(scene: Scene): Map<String, String> {
        val files = LinkedHashMap<String, String>()
        val prefix = "aase_" + scene.id.replace("-", "").take(8) + "_"

        files["data/$NS/function/summon.mcfunction"] =
            "# Summon scene: ${scene.name}\n" +
                SummonExporter.export(scene, prefix).lineSequence().joinToString("\n") { it.removePrefix("/") } + "\n"

        val anim = scene.animation
        if (anim != null && anim.tracks.isNotEmpty() && anim.lengthTicks > 0) buildAnimation(files, scene, anim, prefix)

        files["pack.mcmeta"] = "{\"pack\":{\"pack_format\":57,\"description\":\"AASE export: ${scene.name}\"}}\n"
        files["README.txt"] = readme(anim != null)
        return files
    }

    private fun buildAnimation(files: MutableMap<String, String>, scene: Scene, anim: Animation, prefix: String) {
        val byId = scene.elements.associateBy { it.localId }
        val frameTicks = (0..anim.lengthTicks step FRAME_STEP).toList()

        frameTicks.forEachIndexed { idx, t ->
            val sb = StringBuilder("# frame at tick $t\n")
            for (track in anim.tracks) {
                val el = byId[track.elementLocalId] ?: continue
                val kf = InterpolationOps.sample(track.sorted(), t)
                val selector = "@e[tag=$prefix${track.elementLocalId},limit=1]"
                when {
                    el is ArmorStandElement && kf.pose != null ->
                        sb.append("data merge entity $selector {Pose:${SummonExporter.poseNbt(kf.pose)}}\n")
                    el is DisplayElement && kf.transform != null ->
                        sb.append(
                            "data merge entity $selector {start_interpolation:0," +
                                "interpolation_duration:$FRAME_STEP,transformation:${SummonExporter.transformNbt(kf.transform)}}\n",
                        )
                }
            }
            files["data/$NS/function/frames/frame_$idx.mcfunction"] = sb.toString()
        }

        val last = frameTicks.last()
        val tick = StringBuilder("# animation driver\n")
        tick.append("scoreboard players add #t aase_t 1\n")
        frameTicks.forEachIndexed { idx, t ->
            tick.append("execute if score #t aase_t matches $t run function $NS:frames/frame_$idx\n")
        }
        if (anim.loop) {
            tick.append("execute if score #t aase_t matches ${last + FRAME_STEP}.. run scoreboard players set #t aase_t 0\n")
            tick.append("schedule function $NS:tick 1t\n")
        } else {
            tick.append("execute if score #t aase_t matches ..$last run schedule function $NS:tick 1t\n")
        }
        files["data/$NS/function/tick.mcfunction"] = tick.toString()
        files["data/$NS/function/load.mcfunction"] =
            "scoreboard objectives add aase_t dummy\nscoreboard players set #t aase_t 0\n" +
                "# then run:  function $NS:summon   and   function $NS:tick\n"
    }

    private fun readme(animated: Boolean): String = buildString {
        append("AwesomeArmorStandEditor 匯出的資料包(最佳努力)\n\n")
        append("放進世界的 datapacks/<名稱>/,或 .minecraft 的 datapack 目錄。\n")
        append("- function $NS:summon  在你站的位置召喚整個場景\n")
        if (animated) {
            append("- function $NS:load    初始化計時器(播動畫前先跑一次)\n")
            append("- function $NS:tick    啟動關鍵影格播放(每 tick 自我排程)\n")
        }
        append("\n注意:pack_format 與 function 資料夾名稱依 MC 版本可能要調整;NBT 為最佳努力。\n")
    }
}
