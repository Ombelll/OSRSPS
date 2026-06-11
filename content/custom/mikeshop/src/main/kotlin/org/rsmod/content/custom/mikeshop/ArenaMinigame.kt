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

/** In-memory state per speler die in de arena zit. */
internal object ArenaState {
    class Run(var wave: Int, var alive: Int)

    val runs = HashMap<Player, Run>()
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
                ArenaState.runs[player] = ArenaState.Run(wave = 0, alive = 0)
                player.mes("=== COMBAT ARENA ===")
                player.mes("Your best wave: ${player.arenaBestWave}/$maxWave.")
                player.mes("Survive 10 waves! Stand in OPEN ground. Type ::arenaquit to leave.")
                spawnWave(player, 1)
            }
        }

        onCommand("arenastats") {
            desc = "Show your best Combat Arena wave"
            cheat { player.mes("Combat Arena best wave: ${player.arenaBestWave}/$maxWave.") }
        }

        onCommand("arenaquit") {
            desc = "Leave the arena"
            cheat {
                if (ArenaState.runs.remove(player) != null) {
                    player.mes("You leave the arena. (Remaining monsters despawn shortly.)")
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

    private fun spawnWave(player: Player, wave: Int) {
        val run = ArenaState.runs[player] ?: return
        run.wave = wave
        val count = (2 + wave).coerceAtMost(spawnOffsets.size)
        val type = waveMonster(wave)
        var spawned = 0
        for (off in spawnOffsets.take(count)) {
            try {
                val npc = Npc(npcTypes[type], player.coords.translate(off.first, off.second))
                npc.mode = NpcMode.None
                npcRepo.add(npc, duration = 2000)
                spawned++
            } catch (e: Exception) {
                // tegel geblokkeerd -> sla over
            }
        }
        run.alive = spawned
        if (spawned == 0) {
            player.mes("No room to spawn monsters - move to open ground and ::arena again.")
            ArenaState.runs.remove(player)
            return
        }
        player.mes("Wave $wave/$maxWave: $spawned enemies incoming - kill them all!")
    }

    private suspend fun StandardNpcAccess.arenaKill() {
        val coords = npc.coords
        val hero = findHero(players)
        val run = hero?.let { ArenaState.runs[it] }
        if (hero == null || run == null) {
            // Geen arena-kill (bv. dit monster ergens anders gedood) -> normale loot.
            death.deathWithDrops(this)
            return
        }
        // Arena-kill: geen normale loot, wel een kleine coin-beloning per kill.
        death.deathNoDrops(this)
        objRepo.add(objs.coins, coords, 100, hero, count = run.wave * 500)
        run.alive--
        if (run.alive > 0) {
            return
        }
        // Golf geklaard:
        val cleared = run.wave
        objRepo.add(objs.coins, coords, 200, hero, count = cleared * 5000)
        hero.mes("Wave $cleared cleared! +${cleared * 5000} coins.")
        if (cleared > hero.arenaBestWave) {
            hero.arenaBestWave = cleared
            hero.mes("New Combat Arena record: wave $cleared/$maxWave!")
        }
        if (cleared >= maxWave) {
            objRepo.add(objs.coins, coords, 300, hero, count = 1_000_000)
            hero.mes("=== ARENA CHAMPION! All $maxWave waves cleared! Bonus 1,000,000 coins! ===")
            ArenaState.runs.remove(hero)
        } else {
            spawnWave(hero, cleared + 1)
        }
    }
}
