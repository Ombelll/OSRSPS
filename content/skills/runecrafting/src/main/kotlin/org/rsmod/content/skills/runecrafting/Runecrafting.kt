package org.rsmod.content.skills.runecrafting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object RcObjs : ObjReferences() {
    val essence = find("blankrune") // rune essence
    val airrune = find("airrune")
    val mindrune = find("mindrune")
    val waterrune = find("waterrune")
    val earthrune = find("earthrune")
    val firerune = find("firerune")
}

internal object RcAltars : LocReferences() {
    val air = find("air_altar")
    val mind = find("mind_altar")
    val water = find("water_altar")
    val earth = find("earth_altar")
    val fire = find("fire_altar")
}

/** RUNECRAFTING: gebruik rune essence ('blankrune') op een altaar -> runes + RC-XP. */
class Runecrafting @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        craft(RcAltars.air, RcObjs.airrune, 5.0)
        craft(RcAltars.mind, RcObjs.mindrune, 5.5)
        craft(RcAltars.water, RcObjs.waterrune, 6.0)
        craft(RcAltars.earth, RcObjs.earthrune, 6.5)
        craft(RcAltars.fire, RcObjs.firerune, 7.0)
    }

    private fun ScriptContext.craft(altar: LocType, rune: ObjType, xpEach: Double) {
        onOpLoc1(altar) {
            val n = invTotal(inv, RcObjs.essence)
            if (n <= 0) {
                mes("You have no rune essence to bind.")
            } else {
                invDel(inv, RcObjs.essence, n)
                invAdd(inv, rune, n)
                statAdvance(stats.runecrafting, PlayerStatMap.toFineXP(xpEach * n).toDouble())
                mes("You bind the altar's power into $n rune(s).")
            }
        }
    }
}
