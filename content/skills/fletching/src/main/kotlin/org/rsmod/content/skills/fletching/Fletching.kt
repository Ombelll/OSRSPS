package org.rsmod.content.skills.fletching

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object FletchObjs : ObjReferences() {
    val knife = find("knife")
    val logs = find("logs")
    val oak_logs = find("oak_logs")
    val arrow_shaft = find("arrow_shaft")
    val feather = find("feather")
    val headless_arrow = find("headless_arrow")
    val bronze_arrowheads = find("bronze_arrowheads")
    val bronze_arrow = find("bronze_arrow")
}

/** FLETCHING: gebruik een mes op logs -> pijlschachten + Fletching-XP. */
class Fletching @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(FletchObjs.knife, FletchObjs.logs) { fletch(FletchObjs.logs, 5.0) }
        onOpHeldU(FletchObjs.knife, FletchObjs.oak_logs) { fletch(FletchObjs.oak_logs, 10.0) }
        // Arrow-chain: feather + shaft -> headless arrow -> (+arrowheads) bronze arrow
        onOpHeldU(FletchObjs.feather, FletchObjs.arrow_shaft) {
            combine(FletchObjs.feather, FletchObjs.arrow_shaft, FletchObjs.headless_arrow, 1.5, "You attach a feather to the arrow shaft.")
        }
        onOpHeldU(FletchObjs.headless_arrow, FletchObjs.bronze_arrowheads) {
            combine(FletchObjs.headless_arrow, FletchObjs.bronze_arrowheads, FletchObjs.bronze_arrow, 1.3, "You attach an arrowhead to the arrow.")
        }
    }

    private fun ProtectedAccess.combine(a: ObjType, b: ObjType, result: ObjType, xp: Double, msg: String) {
        invDel(inv, a, 1, b, 1)
        invAdd(inv, result)
        statAdvance(stats.fletching, PlayerStatMap.toFineXP(xp).toDouble())
        mes(msg)
    }

    private fun ProtectedAccess.fletch(log: ObjType, xp: Double) {
        invDel(inv, log, 1)
        invAdd(inv, FletchObjs.arrow_shaft, 15)
        statAdvance(stats.fletching, PlayerStatMap.toFineXP(xp).toDouble())
        mes("You carefully cut the logs into 15 arrow shafts.")
    }
}
