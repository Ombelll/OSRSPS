package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.queues
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Wilderness-boss (W2): Callisto spawnt in de Edgeville-wild als PvM-target met z'n eigen loot.
 * Andere spelers kunnen je tijdens het fighten verrassen (open wild). Respawnt na death zodat 'ie
 * permanent beschikbaar blijft. ::boss teleporteert naar de buurt.
 */
class WildernessBoss
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val players: PlayerList,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    private val pvpWorld = System.getenv("RSMOD_WORLD") == "2"

    override fun ScriptContext.startup() {
        if (!pvpWorld) {
            return
        }
        onPlayerLogin {
            if (!WildernessBossState.spawned) {
                WildernessBossState.spawned = true
                spawnBoss()
            }
        }
        onNpcQueue(npcs.callisto, queues.death) {
            for (online in players) {
                online.mes("[Boss] Callisto has been slain! It will return to the Wilderness shortly.")
            }
            spawnBoss()
        }
        onCommand("wildboss") {
            desc = "Teleporteer naar de Wilderness-boss (Callisto)"
            cheat {
                if (!pvpWorld) {
                    player.mes("De Wilderness-boss staat alleen op World 2.")
                    return@cheat
                }
                protectedAccess.launch(player) {
                    telejump(BOSS_APPROACH)
                    mes("Callisto is vlakbij in de Wilderness. Succes - en pas op voor PK'ers!")
                }
            }
        }
    }

    private fun spawnBoss() {
        try {
            val npc = Npc(npcTypes[npcs.callisto], BOSS_COORD)
            npcRepo.add(npc, duration = Int.MAX_VALUE)
            for (online in players) {
                online.mes("[Boss] Callisto stalks the Wilderness near Edgeville! Hunt it with ::boss.")
            }
        } catch (e: Exception) {
            // Tegel geblokkeerd / npc niet plaatsbaar -> sla over, crash de server niet.
        }
    }

    private companion object {
        private val BOSS_COORD = CoordGrid(3100, 3555)
        private val BOSS_APPROACH = CoordGrid(3097, 3550)
    }
}

internal object WildernessBossState {
    var spawned = false
}
