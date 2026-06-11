package org.rsmod.content.skills.prayer

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Botten die je kunt begraven voor Prayer-XP (bestaande cache-items). */
internal object PrayerObjs : ObjReferences() {
    val bones = find("bones")
    val big_bones = find("big_bones")
}

/**
 * PRAYER-skill: begraaf botten (de "Bury"-optie = op1) voor Prayer-XP.
 * Botten komen van zo'n beetje elk monster, dus dit maakt je combat-loot nuttig.
 */
class Prayer @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        bury(PrayerObjs.bones, xp = 4.5)
        bury(PrayerObjs.big_bones, xp = 15.0)
    }

    private fun ScriptContext.bury(bone: ObjType, xp: Double) {
        onOpHeld1(bone) {
            invDel(inv, bone, 1)
            statAdvance(stats.prayer, PlayerStatMap.toFineXP(xp).toDouble())
            mes("You dig a hole in the ground...")
            mes("You bury the bones.")
        }
    }
}
