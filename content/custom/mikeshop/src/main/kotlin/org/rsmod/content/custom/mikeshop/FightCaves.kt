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
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
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
import org.rsmod.game.region.Region
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
    class Run(val region: Region, val leader: Player, var wave: Int = 0) {
        val members = LinkedHashSet<Player>()
        val npcs = LinkedHashSet<Npc>()
    }

    val runs = HashMap<Player, Run>()
    val npcRuns = HashMap<Npc, Run>()
}

class FightCaves
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val npcRepo: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val objRepo: ObjRepository,
    private val regionRepo: RegionRepository,
    private val players: PlayerList,
    private val death: NpcDeath,
) : PluginScript() {
    private val start = CoordGrid(0, 37, 79, 45, 61)
    private val outside = CoordGrid(0, 49, 54, 28, 30)
    private val maxWave = 6
    private val entranceFee = 100_000

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
                if (FightCaveState.runs.containsKey(player)) {
                    player.mes("You're already in the Fight Caves.")
                    return@cheat
                }
                protectedAccess.launch(player) { startPaidCaves() }
            }
        }

        onCommand("fightjoin") {
            desc = "Join an active Fight Caves party"
            cheat {
                if (FightCaveState.runs.containsKey(player)) {
                    player.mes("You're already in the Fight Caves.")
                    return@cheat
                }
                val run = findJoinRun(args.joinToString(" "))
                if (run == null) {
                    player.mes("No Fight Caves party found. Use ::fightjoin name if multiple parties are active.")
                    return@cheat
                }
                protectedAccess.launch(player) { joinPaidCaves(run) }
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
        val run = FightCaveState.runs[player] ?: return
        if (run.wave == 0 && run.members.size == 1) {
            mes("The heat of the Fight Caves surrounds you. Type ::fightquit to leave.")
            spawnWave(run, 1)
        }
    }

    private suspend fun ProtectedAccess.startPaidCaves() {
        if (invCoinTotal() < entranceFee) {
            mes("The Fight Caves entrance fee is $entranceFee coins.")
            return
        }
        if (!invTakeFee(entranceFee)) {
            mes("The Fight Caves entrance fee is $entranceFee coins.")
            return
        }
        mes("You pay $entranceFee coins to enter the Fight Caves.")
        val region = createRegion()
        if (region == null) {
            invAdd(player.inv, objs.coins, entranceFee, strict = false)
            mes("Could not create a Fight Caves instance. Your fee has been refunded.")
            return
        }
        val run = FightCaveState.Run(region, player)
        addMember(run, player)
        telejump(runStart(run))
    }

    private suspend fun ProtectedAccess.joinPaidCaves(run: FightCaveState.Run) {
        if (invCoinTotal() < entranceFee) {
            mes("The Fight Caves entrance fee is $entranceFee coins.")
            return
        }
        if (!invTakeFee(entranceFee)) {
            mes("The Fight Caves entrance fee is $entranceFee coins.")
            return
        }
        mes("You pay $entranceFee coins to join ${run.leader.displayName}'s Fight Caves party.")
        addMember(run, player)
        run.message("${player.displayName} joined the Fight Caves party. Next wave will scale up.")
        telejump(runStart(run))
    }

    private suspend fun ProtectedAccess.exitArena() {
        val run = FightCaveState.runs[player] ?: return
        run.members.remove(player)
        FightCaveState.runs.remove(player)
        mes("Your Fight Caves run has ended.")
        if (run.members.isEmpty()) {
            cleanupRun(run)
        } else {
            run.message("${player.displayName} left the Fight Caves party.")
        }
    }

    private fun spawnWave(run: FightCaveState.Run, wave: Int) {
        run.wave = wave
        val waveTypes = scaledWaveTypes(wave, run.members.size.coerceAtLeast(1))
        var spawned = 0
        for ((index, type) in waveTypes.withIndex()) {
            val off = spawnOffsets[index % spawnOffsets.size]
            try {
                val npc = Npc(npcTypes[type], runStart(run).translate(off.first, off.second))
                npc.mode = NpcMode.None
                npcRepo.add(npc, duration = 2000)
                run.npcs += npc
                FightCaveState.npcRuns[npc] = run
                spawned++
            } catch (e: Exception) {
                // Blocked spawn tile: skip it; the wave can still continue with the rest.
            }
        }
        if (spawned == 0) {
            run.message("The cave refuses to spawn this wave. Your run has been reset.")
            cleanupRun(run)
            return
        }
        run.message("Fight Caves wave $wave/$maxWave: $spawned enemies for ${run.members.size} player(s)!")
    }

    private fun scaledWaveTypes(wave: Int, memberCount: Int): List<NpcType> {
        val base = waveTypes(wave)
        val extraCount = (memberCount - 1).coerceAtLeast(0)
        if (extraCount == 0) {
            return base
        }
        val extra = List(extraCount) { base.last() }
        return base + extra
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
        val run = FightCaveState.npcRuns.remove(npc)
        if (run == null || !run.npcs.remove(npc)) {
            death.deathWithDrops(this)
            return
        }
        val coords = npc.coords
        death.deathNoDrops(this)
        for (member in run.members) {
            objRepo.add(objs.coins, coords, 200, member, count = run.wave * 10_000)
        }
        if (run.npcs.isNotEmpty()) {
            return
        }
        val cleared = run.wave
        if (cleared >= maxWave) {
            for (member in run.members) {
                member.invAdd(member.inv, objs.fire_cape, 1, strict = false)
                objRepo.add(objs.coins, coords, 300, member, count = 1_000_000)
                member.mes("You conquered the Fight Caves! A fire cape is yours.")
            }
            val names = run.members.joinToString { it.displayName }
            for (online in players) {
                if (online !in run.members) {
                    online.mes("[Fight Caves] $names defeated Mike's Jad!")
                }
            }
            cleanupRun(run)
            return
        }
        run.message("Wave $cleared cleared. Prepare yourself...")
        spawnWave(run, cleared + 1)
    }

    private fun cleanupRun(run: FightCaveState.Run) {
        for (member in run.members.toList()) {
            FightCaveState.runs.remove(member)
        }
        run.members.clear()
        for (npc in run.npcs.toList()) {
            FightCaveState.npcRuns.remove(npc)
            try {
                npcRepo.del(npc, Int.MAX_VALUE)
            } catch (e: Exception) {
                // Already gone through death cleanup.
            }
        }
        run.npcs.clear()
    }

    private fun addMember(run: FightCaveState.Run, player: Player) {
        run.members += player
        FightCaveState.runs[player] = run
    }

    private fun findJoinRun(name: String): FightCaveState.Run? {
        val runs = FightCaveState.runs.values.toSet()
        if (runs.isEmpty()) {
            return null
        }
        if (name.isBlank()) {
            return runs.singleOrNull()
        }
        return runs.firstOrNull { run ->
            run.members.any { it.displayName.equals(name, ignoreCase = true) }
        }
    }

    private fun createRegion(): Region? = regionRepo.add(fightCaveTemplate)

    private fun runStart(run: FightCaveState.Run): CoordGrid {
        return run.region.normal[0, 37, 79, 45, 61]
    }

    private fun FightCaveState.Run.message(text: String) {
        for (member in members) {
            member.mes(text)
        }
    }

    private companion object {
        val fightCaveTemplate =
            RegionTemplate.create {
                copyAllLevels(296, 632) {
                    zoneWidth = 8
                    zoneLength = 8
                }
            }
    }
}
