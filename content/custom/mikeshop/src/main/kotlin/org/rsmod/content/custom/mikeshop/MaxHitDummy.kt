package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.queues
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Max-hit dummy op de Edgeville PK-hub (W2). Elke melee/ranged hit erop doet gegarandeerd je max hit
 * (geen miss, geen variatie) - de always-max-logica zit in PlayerAttackManager (target = deze
 * dummy). Handig om je max te checken met verschillende gear/specs. Spawnt eenmalig bij de eerste
 * login en respawnt na death zodat hij permanent blijft staan.
 *
 * NB: magic-max wordt (nog) niet geforceerd - alleen melee/ranged.
 */
class MaxHitDummy
@Inject
constructor(private val npcRepo: NpcRepository, private val npcTypes: NpcTypeList) : PluginScript() {
    private val pvpWorld = System.getenv("RSMOD_WORLD") == "2"
    private val dummyCoord = CoordGrid(3085, 3489) // open tegel net ten zuiden van de PK-shops

    override fun ScriptContext.startup() {
        if (!pvpWorld) {
            return
        }
        onPlayerLogin {
            if (!MaxHitDummyState.spawned) {
                MaxHitDummyState.spawned = true
                spawnDummy()
            }
        }
        // Respawn na death zodat de dummy permanent blijft (hij sneuvelt door je max hits).
        onNpcQueue(npcs.test_combat_dummy_maxhit, queues.death) { spawnDummy() }
    }

    private fun spawnDummy() {
        try {
            val npc = Npc(npcTypes[npcs.test_combat_dummy_maxhit], dummyCoord)
            npc.mode = NpcMode.None
            npcRepo.add(npc, duration = Int.MAX_VALUE)
        } catch (e: Exception) {
            // Tegel geblokkeerd / npc niet plaatsbaar -> sla over, crash de server niet.
        }
    }
}

internal object MaxHitDummyState {
    var spawned = false
}
