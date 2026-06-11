package org.rsmod.content.skills.prayer

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object AltarLocs : LocReferences() {
    val altar = find("altar")
}

internal object AltarBones : ObjReferences() {
    val bones = find("bones")
    val big_bones = find("big_bones")
    val dragon_bones = find("dragon_bones")
}

/** Eén offerbaar bot: het item en de (dubbele) Prayer-XP bij offeren op het altaar. */
private class Offering(val bone: ObjType, val xp: Double, val name: String)

/**
 * PRAYER-ALTAAR.
 *
 *  - Klik het altaar aan -> je prayer points worden volledig hersteld.
 *  - Gebruik botten op het altaar -> 2x de normale Prayer-XP (i.p.v. begraven).
 */
class PrayerAltar @Inject constructor() : PluginScript() {
    private val offerings =
        listOf(
            Offering(AltarBones.bones, 9.0, "bones"),
            Offering(AltarBones.big_bones, 30.0, "big bones"),
            Offering(AltarBones.dragon_bones, 144.0, "dragon bones"),
        )

    override fun ScriptContext.startup() {
        onOpLoc1(AltarLocs.altar) { recharge() }
        for (o in offerings) {
            onOpLocU(AltarLocs.altar, o.bone) { offer(o) }
        }
    }

    private fun ProtectedAccess.recharge() {
        statHeal(stats.prayer, constant = 999, percent = 0)
        mes("You pray at the altar; your prayer points are restored.")
    }

    private fun ProtectedAccess.offer(o: Offering) {
        invDel(inv, o.bone, 1)
        statAdvance(stats.prayer, PlayerStatMap.toFineXP(o.xp).toDouble())
        mes("You offer the ${o.name} on the altar. The gods are pleased.")
    }
}
