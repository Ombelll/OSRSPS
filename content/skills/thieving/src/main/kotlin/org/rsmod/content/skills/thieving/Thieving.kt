package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpNpc2
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.npc.NpcType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ThiefNpcs : NpcReferences() {
    val man = find("man")
    val woman = find("woman")
    val barbarian = find("barbarian")
}

internal object ThiefObjs : ObjReferences() {
    val coins = find("coins")
}

/**
 * THIEVING: zakkenrollen (npc-optie 2 = "Pickpocket") -> coins + Thieving-XP.
 */
class Thieving @Inject constructor(private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        pickpocket(ThiefNpcs.man, "man", xp = 8.0, min = 3, max = 15)
        pickpocket(ThiefNpcs.woman, "woman", xp = 8.0, min = 3, max = 15)
        pickpocket(ThiefNpcs.barbarian, "barbarian", xp = 15.0, min = 5, max = 25)
    }

    private fun ScriptContext.pickpocket(npc: NpcType, name: String, xp: Double, min: Int, max: Int) {
        onOpNpc2(npc) {
            val amount = min + random.of(maxExclusive = (max - min + 1))
            invAdd(inv, ThiefObjs.coins, amount)
            statAdvance(stats.thieving, PlayerStatMap.toFineXP(xp).toDouble())
            mes("You pick the $name's pocket. ($amount coins)")
        }
    }
}
