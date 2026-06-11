package org.rsmod.content.skills.fletching

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object BowObjs : ObjReferences() {
    val knife = find("knife")
    val oak_logs = find("oak_logs")
    val flax = find("flax")
    val bow_string = find("bow_string")
    val unstrung_shortbow = find("unstrung_shortbow")
    val unstrung_oak_shortbow = find("unstrung_oak_shortbow")
    val unstrung_longbow = find("unstrung_longbow")
    val unstrung_oak_longbow = find("unstrung_oak_longbow")
    val shortbow = find("shortbow")
    val oak_shortbow = find("oak_shortbow")
    val longbow = find("longbow")
    val oak_longbow = find("oak_longbow")
}

internal object BowLocs : LocReferences() {
    val spinningwheel = find("spinningwheel")
}

/** Eén string-actie: onbespannen boog -> afgewerkte boog + Fletching-XP. */
private class Stringing(val unstrung: ObjType, val strung: ObjType, val xp: Double, val name: String)

/**
 * FLETCHING — boog-keten (vult de bestaande pijl-keten aan).
 *
 *  - Spinnewiel + vlas -> bow string (Crafting-XP).
 *  - Mes op oak logs   -> onbespannen oak shortbow (Fletching-XP).
 *  - Bow string op een onbespannen boog -> afgewerkte boog (Fletching-XP).
 *    (Onbespannen shortbow/longbow kun je via ::invadd of latere log-tiers krijgen.)
 */
class FletchingBows @Inject constructor() : PluginScript() {
    private val stringings =
        listOf(
            Stringing(BowObjs.unstrung_shortbow, BowObjs.shortbow, 5.0, "shortbow"),
            Stringing(BowObjs.unstrung_oak_shortbow, BowObjs.oak_shortbow, 16.5, "oak shortbow"),
            Stringing(BowObjs.unstrung_longbow, BowObjs.longbow, 10.0, "longbow"),
            Stringing(BowObjs.unstrung_oak_longbow, BowObjs.oak_longbow, 25.0, "oak longbow"),
        )

    override fun ScriptContext.startup() {
        onOpLocU(BowLocs.spinningwheel, BowObjs.flax) { spin() }
        for (s in stringings) {
            onOpHeldU(BowObjs.bow_string, s.unstrung) { stringBow(s) }
        }
    }

    private fun ProtectedAccess.spin() {
        invDel(inv, BowObjs.flax, 1)
        invAdd(inv, BowObjs.bow_string)
        statAdvance(stats.crafting, PlayerStatMap.toFineXP(15.0).toDouble())
        mes("You spin the flax into a bow string.")
    }

    private fun ProtectedAccess.stringBow(s: Stringing) {
        invDel(inv, s.unstrung, 1, BowObjs.bow_string, 1)
        invAdd(inv, s.strung)
        statAdvance(stats.fletching, PlayerStatMap.toFineXP(s.xp).toDouble())
        mes("You add a bow string to make a ${s.name}.")
    }
}
