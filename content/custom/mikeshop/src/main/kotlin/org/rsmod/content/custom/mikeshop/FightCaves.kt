package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.areas
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.queues
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.script.dsl.NpcPluginBuilder
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object FightCaveNpcs : NpcReferences() {
    val nibbler = find("tzhaar_fightcave_swarm_1a")
    val bat = find("tzhaar_fightcave_swarm_2a")
    val ranger = find("tzhaar_fightcave_swarm_3a")
    val mage = find("tzhaar_fightcave_swarm_4a")
    val melee = find("tzhaar_fightcave_swarm_5a")
    val jad = find("tzhaar_fightcave_swarm_boss")
}

internal object FightCaveNpcEditor : NpcEditor() {
    init {
        edit(FightCaveNpcs.nibbler) {
            name = "Fight Cave Nibbler"
            fightCaveStats(hitpoints = 120, attack = 90, strength = 90, defence = 70)
        }
        edit(FightCaveNpcs.bat) {
            name = "Fight Cave Bat"
            fightCaveStats(hitpoints = 180, attack = 120, strength = 120, defence = 100)
        }
        edit(FightCaveNpcs.ranger) {
            name = "Fight Cave Ranger"
            fightCaveStats(hitpoints = 300, attack = 170, strength = 170, defence = 150)
            ranged = 220
        }
        edit(FightCaveNpcs.mage) {
            name = "Fight Cave Mage"
            fightCaveStats(hitpoints = 420, attack = 210, strength = 210, defence = 190)
            magic = 260
        }
        edit(FightCaveNpcs.melee) {
            name = "Fight Cave Champion"
            fightCaveStats(hitpoints = 650, attack = 260, strength = 260, defence = 230)
        }
        edit(FightCaveNpcs.jad) {
            name = "Mike's Jad"
            fightCaveStats(hitpoints = 1200, attack = 360, strength = 360, defence = 300)
            ranged = 320
            magic = 320
        }
    }

    private fun NpcPluginBuilder.fightCaveStats(
        hitpoints: Int,
        attack: Int,
        strength: Int,
        defence: Int,
    ) {
        defaultMode = none
        huntMode = huntmodes.mike_boss_aggro
        huntRange = 16
        this.hitpoints = hitpoints
        this.attack = attack
        this.strength = strength
        this.defence = defence
    }
}

internal object FightCaveState {
    class Run(val player: Player, var wave: Int = 0) {
        val npcs = LinkedHashSet<Npc>()
    }

    var run: Run? = null
}

class FightCaves
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val npcRepo: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val objRepo: ObjRepository,
    private val players: PlayerList,
    private val death: NpcDeath,
) : PluginScript() {
    private val start = CoordGrid(0, 37, 79, 45, 61)
    private val outside = CoordGrid(0, 49, 54, 28, 30)
    private val maxWave = 6

    private val spawnOffsets =
        listOf(5 to 5, -5 to 5, 5 to -5, -5 to -5, 0 to 7, 7 to 0, 0 to -7, -7 to 0)

    private val fightCaveTypes =
        listOf(
            FightCaveNpcs.nibbler,
            FightCaveNpcs.bat,
            FightCaveNpcs.ranger,
            FightCaveNpcs.mage,
            FightCaveNpcs.melee,
            FightCaveNpcs.jad,
        )

    override fun ScriptContext.startup() {
        onCommand("fightcaves") {
            desc = "Enter the Fight Caves gauntlet"
            cheat {
                val run = FightCaveState.run
                if (run != null && run.player !== player) {
                    player.mes("The Fight Caves are occupied by ${run.player.displayName}.")
                    return@cheat
                }
                protectedAccess.launch(player) { telejump(start) }
            }
        }

        onCommand("fightquit") {
            desc = "Leave the Fight Caves"
            cheat { protectedAccess.launch(player) { telejump(outside) } }
        }

        onArea(areas.fight_cave_arena) { enterArena() }
        onAreaExit(areas.fight_cave_arena) { exitArena() }

        for (type in fightCaveTypes) {
            onNpcQueue(type, queues.death) { fightCaveKill() }
        }
    }

    private suspend fun ProtectedAccess.enterArena() {
        val run = FightCaveState.run
        if (run != null) {
            if (run.player === player) {
                return
            }
            mes("The Fight Caves are already occupied. Try again shortly.")
            telejump(outside)
            return
        }
        FightCaveState.run = FightCaveState.Run(player)
        mes("The heat of the Fight Caves surrounds you. Type ::fightquit to leave.")
        spawnWave(1)
    }

    private suspend fun ProtectedAccess.exitArena() {
        val run = FightCaveState.run ?: return
        if (run.player !== player) {
            return
        }
        cleanupRun(run)
        FightCaveState.run = null
        mes("Your Fight Caves run has ended.")
    }

    private fun spawnWave(wave: Int) {
        val run = FightCaveState.run ?: return
        run.wave = wave
        val waveTypes = waveTypes(wave)
        var spawned = 0
        for ((index, type) in waveTypes.withIndex()) {
            val off = spawnOffsets[index % spawnOffsets.size]
            try {
                val npc = Npc(npcTypes[type], start.translate(off.first, off.second))
                npc.mode = NpcMode.None
                npcRepo.add(npc, duration = 2000)
                run.npcs += npc
                spawned++
            } catch (e: Exception) {
                // Blocked spawn tile: skip it; the wave can still continue with the rest.
            }
        }
        if (spawned == 0) {
            run.player.mes("The cave refuses to spawn this wave. Your run has been reset.")
            cleanupRun(run)
            FightCaveState.run = null
            return
        }
        run.player.mes("Fight Caves wave $wave/$maxWave: $spawned enemies incoming!")
    }

    private fun waveTypes(wave: Int): List<NpcType> =
        when (wave) {
            1 -> listOf(FightCaveNpcs.nibbler, FightCaveNpcs.nibbler)
            2 -> listOf(FightCaveNpcs.bat, FightCaveNpcs.nibbler, FightCaveNpcs.nibbler)
            3 -> listOf(FightCaveNpcs.ranger, FightCaveNpcs.bat, FightCaveNpcs.nibbler)
            4 -> listOf(FightCaveNpcs.mage, FightCaveNpcs.ranger, FightCaveNpcs.bat)
            5 -> listOf(FightCaveNpcs.melee, FightCaveNpcs.mage, FightCaveNpcs.ranger)
            else -> listOf(FightCaveNpcs.jad, FightCaveNpcs.melee, FightCaveNpcs.mage)
        }

    private suspend fun StandardNpcAccess.fightCaveKill() {
        val run = FightCaveState.run
        if (run == null || !run.npcs.remove(npc)) {
            death.deathWithDrops(this)
            return
        }
        val coords = npc.coords
        death.deathNoDrops(this)
        objRepo.add(objs.coins, coords, 200, run.player, count = run.wave * 10_000)
        if (run.npcs.isNotEmpty()) {
            return
        }
        val cleared = run.wave
        if (cleared >= maxWave) {
            run.player.invAdd(run.player.inv, objs.fire_cape, 1, strict = false)
            objRepo.add(objs.coins, coords, 300, run.player, count = 1_000_000)
            run.player.mes("You conquered the Fight Caves! A fire cape is yours.")
            for (online in players) {
                if (online !== run.player) {
                    online.mes("[Fight Caves] ${run.player.displayName} has defeated Mike's Jad!")
                }
            }
            FightCaveState.run = null
            return
        }
        run.player.mes("Wave $cleared cleared. Prepare yourself...")
        spawnWave(cleared + 1)
    }

    private fun cleanupRun(run: FightCaveState.Run) {
        for (npc in run.npcs.toList()) {
            try {
                npcRepo.del(npc, Int.MAX_VALUE)
            } catch (e: Exception) {
                // Already gone through death cleanup.
            }
        }
        run.npcs.clear()
    }
}
