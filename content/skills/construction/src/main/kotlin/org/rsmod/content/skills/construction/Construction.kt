package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.constructionLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ConObjs : ObjReferences() {
    val hammer = find("hammer")
    val woodplank = find("woodplank")
    val plank_oak = find("plank_oak")
    val plank_teak = find("plank_teak")
    val plank_mahogany = find("plank_mahogany")
}

/** Eén bouwactie: het type plank, vereist Construction-level, XP en weergavenaam. */
private class Build(val plank: ObjType, val level: Int, val xp: Double, val name: String)

/**
 * CONSTRUCTION (vereenvoudigd).
 *
 * Gebruik een hamer op planken om meubels te bouwen -> Construction-XP (level-gated).
 * (Een volledige POH met bouwplekken is logisch vervolgwerk; dit maakt de skill trainbaar
 * en verwerkt de planken die je via Woodcutting/Sawmill maakt.)
 */
class Construction @Inject constructor() : PluginScript() {
    private val builds =
        listOf(
            Build(ConObjs.woodplank, 1, 29.0, "wooden"),
            Build(ConObjs.plank_oak, 15, 60.0, "oak"),
            Build(ConObjs.plank_teak, 30, 90.0, "teak"),
            Build(ConObjs.plank_mahogany, 50, 140.0, "mahogany"),
        )

    override fun ScriptContext.startup() {
        for (b in builds) {
            onOpHeldU(ConObjs.hammer, b.plank) { build(b) }
        }
    }

    private fun ProtectedAccess.build(b: Build) {
        if (player.constructionLvl < b.level) {
            mes("You need a Construction level of ${b.level} to build with these planks.")
            return
        }
        invDel(inv, b.plank, 1)
        statAdvance(stats.construction, PlayerStatMap.toFineXP(b.xp).toDouble())
        mes("You build a piece of ${b.name} furniture.")
    }
}
