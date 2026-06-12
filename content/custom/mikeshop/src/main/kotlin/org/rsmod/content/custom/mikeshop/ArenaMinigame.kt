package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.queues
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Arena-monsters: bewust types die NERGENS anders een death-hook hebben (geen conflict). */
internal object ArenaMonsters : NpcReferences() {
    val skeleton = find("skeleton_unarmed")
    val zombie = find("zombie_unarmed")
    val black_knight = find("black_knight")
    val lesser_demon = find("lesser_demon")
}

/** In-memory state voor party-runs in de arena. */
internal object ArenaState {
    class Run(var leader: Player, var wave: Int = 0, var alive: Int = 0) {
        val members = LinkedHashSet<Player>()
        val npcs = LinkedHashSet<Npc>()
    }

    val runs = HashMap<Player, Run>()
    val npcRuns = HashMap<Npc, Run>()
}

private var Player.arenaBestWave: Int by intVarp(varps.mike_arena_best_wave)

/**
 * COMBAT ARENA (minigame) - ::arena
 *
 * Wave-survival: elke golf spawnt steeds meer & sterkere monsters rond je. Maak een golf helemaal
 * af -> coins-beloning + de volgende (zwaardere) golf. Overleef alle 10 golven voor een grote bonus.
 * Monsters zijn (nog) passief - aggressie KAN wel via hunt-modes (zie ROADMAP.md Fase 2:
 * NpcEditor huntMode/huntRange + custom HuntModeBuilder; vereist packCache).
 */
class ArenaMinigame
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val objRepo: ObjRepository,
    private val players: PlayerList,
    private val death: NpcDeath,
) : PluginScript() {
    private val maxWave = 10

    private val spawnOffsets =
        listOf(2 to 2, -2 to 2, 2 to -2, -2 to -2, 3 to 0, -3 to 0, 0 to 3, 0 to -3)

    private val arenaTypes =
        listOf(
            ArenaMonsters.skeleton,
            ArenaMonsters.zombie,
            ArenaMonsters.black_knight,
            ArenaMonsters.lesser_demon,
        )

    override fun ScriptContext.startup() {
        onCommand("arena") {
            desc = "Start the Combat Arena wave-survival minigame"
            cheat {
                if (ArenaState.runs.containsKey(player)) {
                    player.mes("You're already fighting in the arena!")
                    return@cheat
                }
                val run = ArenaState.Run(player)
                run.members += player
                ArenaState.runs[player] = run
                player.mes("=== COMBAT ARENA ===")
                player.mes("Your best wave: ${player.arenaBestWave}/$maxWave.")
                player.mes("Survive 10 waves! Friends nearby can type ::arenajoin.")
                player.mes("Stand in OPEN ground. Type ::arenaquit to leave.")
                spawnWave(run, 1)
            }
        }

        onCommand("arenajoin") {
            desc = "Join a nearby Combat Arena party run"
            cheat {
                if (ArenaState.runs.containsKey(player)) {
                    player.mes("You're already in an arena party.")
                    return@cheat
                }
                val run = nearbyJoinableRun(player)
                if (run == null) {
                    player.mes("No nearby arena party found. Stand near the leader and try again.")
                    return@cheat
                }
                run.members += player
                ArenaState.runs[player] = run
                run.message("${player.displayName} joined the arena party. Next wave will scale up.")
            }
        }

        onCommand("arenastats") {
            desc = "Show your best Combat Arena wave"
            cheat { player.mes("Combat Arena best wave: ${player.arenaBestWave}/$maxWave.") }
        }

        onCommand("arenaquit") {
            desc = "Leave the arena"
            cheat {
                val run = ArenaState.runs[player]
                if (run != null) {
                    leaveRun(player, run)
                } else {
                    player.mes("You're not in the arena.")
                }
            }
        }

        for (type in arenaTypes) {
            onNpcQueue(type, queues.death) { arenaKill() }
        }
    }

    private fun waveMonster(wave: Int): NpcType =
        when {
            wave <= 3 -> ArenaMonsters.skeleton
            wave <= 6 -> ArenaMonsters.zombie
            wave <= 8 -> ArenaMonsters.black_knight
            else -> ArenaMonsters.lesser_demon
        }

    private fun spawnWave(run: ArenaState.Run, wave: Int) {
        run.wave = wave
        val memberCount = run.members.size.coerceAtLeast(1)
        val count = (2 + wave + (memberCount - 1) * 2).coerceAtMost(spawnOffsets.size)
        val type = waveMonster(wave)
        var spawned = 0
        for (off in spawnOffsets.take(count)) {
            try {
                val npc = Npc(npcTypes[type], run.leader.coords.translate(off.first, off.second))
                npc.mode = NpcMode.None
                npcRepo.add(npc, duration = 2000)
                run.npcs += npc
                ArenaState.npcRuns[npc] = run
                spawned++
            } catch (e: Exception) {
                // tegel geblokkeerd -> sla over
            }
        }
        run.alive = spawned
        if (spawned == 0) {
            run.message("No room to spawn monsters - move to open ground and ::arena again.")
            cleanupRun(run)
            return
        }
        run.message("Wave $wave/$maxWave: $spawned enemies incoming for ${run.members.size} player(s)!")
    }

    private fun nearbyJoinableRun(player: Player): ArenaState.Run? {
        val runs = ArenaState.runs.values.toSet()
        return runs.firstOrNull { run ->
            run.members.any { member -> member.distanceTo(player) <= 8 }
        }
    }

    private fun leaveRun(player: Player, run: ArenaState.Run) {
        ArenaState.runs.remove(player)
        run.members.remove(player)
        player.mes("You leave the arena party.")
        if (run.members.isEmpty()) {
            cleanupRun(run)
        } else {
            if (run.leader === player) {
                run.leader = run.members.first()
            }
            run.message("${player.displayName} left the arena party.")
        }
    }

    private fun cleanupRun(run: ArenaState.Run) {
        for (member in run.members.toList()) {
            ArenaState.runs.remove(member)
        }
        run.members.clear()
        for (npc in run.npcs.toList()) {
            ArenaState.npcRuns.remove(npc)
            try {
                npcRepo.del(npc, Int.MAX_VALUE)
            } catch (e: Exception) {
                // Already gone through death cleanup.
            }
        }
        run.npcs.clear()
        run.alive = 0
    }

    private fun ArenaState.Run.message(text: String) {
        for (member in members) {
            member.mes(text)
        }
    }

    private suspend fun StandardNpcAccess.arenaKill() {
        val coords = npc.coords
        val hero = findHero(players)
        val run = ArenaState.npcRuns.remove(npc) ?: hero?.let { ArenaState.runs[it] }
        if (run == null) {
            // Geen arena-kill (bv. dit monster ergens anders gedood) -> normale loot.
            death.deathWithDrops(this)
            return
        }
        run.npcs.remove(npc)
        // Arena-kill: geen normale loot, wel een kleine coin-beloning per kill.
        death.deathNoDrops(this)
        for (member in run.members) {
            objRepo.add(objs.coins, coords, 100, member, count = run.wave * 500)
        }
        run.alive--
        if (run.alive > 0) {
            return
        }
        // Golf geklaard:
        val cleared = run.wave
        for (member in run.members) {
            objRepo.add(objs.coins, coords, 200, member, count = cleared * 5000)
            member.mes("Wave $cleared cleared! +${cleared * 5000} coins.")
            if (cleared > member.arenaBestWave) {
                member.arenaBestWave = cleared
                member.mes("New Combat Arena record: wave $cleared/$maxWave!")
            }
        }
        if (cleared >= maxWave) {
            for (member in run.members) {
                objRepo.add(objs.coins, coords, 300, member, count = 1_000_000)
                member.mes("=== ARENA CHAMPION! All $maxWave waves cleared! Bonus 1,000,000 coins! ===")
            }
            cleanupRun(run)
        } else {
            spawnWave(run, cleared + 1)
        }
    }
}
