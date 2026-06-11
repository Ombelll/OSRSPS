package org.rsmod.content.skills.hunter

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hunterLvl
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onApNpc2
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Huntbare creatures (bestaande cache-NPC's). Spawn er een met bv. `::npc hunting_chinchompa`. */
internal object HuntNpcs : NpcReferences() {
    val butterfly = find("butterfly")
    val chinchompa = find("hunting_chinchompa")
    val chinchompa_big = find("hunting_chinchompa_big")
    val kebbit = find("eaglepeak_uber_kebbit")
}

internal object HuntObjs : ObjReferences() {
    val butterfly_jar = find("butterfly_jar")
    val chinchompa = find("chinchompa_captured")
    val chinchompa_big = find("chinchompa_big_captured")
    val fur = find("fur")
}

/** Eén vangst: het dier, de beloning, vereist Hunter-level, XP en weergavenaam. */
private class Catch(
    val npc: NpcType,
    val reward: ObjType,
    val level: Int,
    val xp: Double,
    val name: String,
)

/**
 * HUNTER.
 *
 * Klik een huntbaar dier aan om het te vangen -> beloning + Hunter-XP (level-gated).
 * Zowel "op" (ernaast) als "ap" (op afstand) routes worden geregistreerd.
 */
class Hunter @Inject constructor() : PluginScript() {
    private val catches =
        listOf(
            Catch(HuntNpcs.butterfly, HuntObjs.butterfly_jar, 1, 30.0, "butterfly"),
            Catch(HuntNpcs.chinchompa, HuntObjs.chinchompa, 10, 50.0, "chinchompa"),
            Catch(HuntNpcs.kebbit, HuntObjs.fur, 20, 60.0, "kebbit"),
            Catch(
                HuntNpcs.chinchompa_big,
                HuntObjs.chinchompa_big,
                30,
                80.0,
                "carnivorous chinchompa",
            ),
        )

    override fun ScriptContext.startup() {
        for (c in catches) {
            onOpNpc1(c.npc) { catch(c) }
            onApNpc1(c.npc) { catch(c) }
            onApNpc2(c.npc) { catch(c) }
        }
    }

    private fun ProtectedAccess.catch(c: Catch) {
        if (player.hunterLvl < c.level) {
            mes("You need a Hunter level of ${c.level} to catch this.")
            return
        }
        if (inv.isFull()) {
            mes("Your inventory is too full to hold your catch.")
            return
        }
        invAdd(inv, c.reward)
        statAdvance(stats.hunter, PlayerStatMap.toFineXP(c.xp).toDouble())
        spam("You catch the ${c.name}.")
    }
}
