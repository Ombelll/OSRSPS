package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import java.util.IdentityHashMap
import org.rsmod.api.config.refs.queues
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.NpcType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal fun Npc.startBossMechanics() {
    if (type.id !in BossMechanics.MECHANIC_TYPE_IDS) {
        return
    }
    queue(queues.generic_queue10, BossMechanics.PULSE_CYCLES)
}

internal object BossMechanics {
    const val PULSE_CYCLES = 10
    val MECHANIC_TYPE_IDS =
        setOf(
            BossNpcs.black_demon.id,
            BossNpcs.blue_dragon.id,
            BossNpcs.red_dragon.id,
            BossNpcs.steel_dragon.id,
        )
}

private data class BossMechanic(
    val type: NpcType,
    val name: String,
    val radius: Int,
    val damage: Int,
    val warning: String,
    val phase50: String,
    val phase25: String,
)

class BossMechanicsScript
@Inject
constructor(private val players: PlayerList) : PluginScript() {
    private val phases = IdentityHashMap<Npc, Int>()

    private val mechanics =
        listOf(
            BossMechanic(
                BossNpcs.black_demon,
                "Mike's Nightmare",
                radius = 3,
                damage = 8,
                warning = "Darkness erupts around Mike's Nightmare.",
                phase50 = "Mike's Nightmare enrages at half health!",
                phase25 = "Mike's Nightmare is close to collapse and lashes out wildly!",
            ),
            BossMechanic(
                BossNpcs.blue_dragon,
                "Frost Wyrm",
                radius = 4,
                damage = 9,
                warning = "Freezing shards burst from the Frost Wyrm.",
                phase50 = "The Frost Wyrm's scales crack with cold energy.",
                phase25 = "The Frost Wyrm releases a desperate blizzard!",
            ),
            BossMechanic(
                BossNpcs.red_dragon,
                "Crimson Wyrm",
                radius = 4,
                damage = 11,
                warning = "A wave of heat rolls off the Crimson Wyrm.",
                phase50 = "The Crimson Wyrm burns hotter at half health.",
                phase25 = "The Crimson Wyrm floods the arena with flame!",
            ),
            BossMechanic(
                BossNpcs.steel_dragon,
                "The Ancient One",
                radius = 5,
                damage = 14,
                warning = "Ancient power detonates around The Ancient One.",
                phase50 = "The Ancient One hardens its scales and accelerates.",
                phase25 = "The Ancient One begins its final annihilation pattern!",
            ),
        )

    override fun ScriptContext.startup() {
        for (mechanic in mechanics) {
            onNpcQueue(mechanic.type, queues.generic_queue10) { bossPulse(mechanic) }
            onNpcHit(mechanic.type) { announcePhase(mechanic, npc) }
        }
    }

    private fun announcePhase(mechanic: BossMechanic, npc: Npc) {
        val nextPhase =
            when {
                npc.hitpoints <= 0 -> return
                npc.hitpoints <= npc.baseHitpointsLvl / 4 -> 25
                npc.hitpoints <= npc.baseHitpointsLvl / 2 -> 50
                else -> return
            }
        val previous = phases[npc] ?: 100
        if (nextPhase >= previous) {
            return
        }
        phases[npc] = nextPhase
        val text = if (nextPhase == 25) mechanic.phase25 else mechanic.phase50
        nearbyPlayers(npc, radius = 10).forEach { it.mes(text, ChatType.Broadcast) }
    }

    private fun StandardNpcAccess.bossPulse(mechanic: BossMechanic) {
        if (npc.hitpoints <= 0) {
            phases.remove(npc)
            return
        }
        val targets = nearbyPlayers(npc, mechanic.radius)
        if (targets.isNotEmpty()) {
            for (target in targets) {
                target.mes(mechanic.warning)
                target.queueHit(npc, delay = 1, type = HitType.Typeless, damage = mechanic.damage)
            }
        }
        queue(queues.generic_queue10, BossMechanics.PULSE_CYCLES)
    }

    private fun nearbyPlayers(npc: Npc, radius: Int): List<Player> {
        return players.filter { player ->
            player.coords.level == npc.coords.level &&
                player.coords.chebyshevDistance(npc.coords) <= radius
        }
    }
}
