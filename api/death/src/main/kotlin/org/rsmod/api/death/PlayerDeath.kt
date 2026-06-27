package org.rsmod.api.death

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.jingles
import org.rsmod.api.config.refs.midis
import org.rsmod.api.config.refs.queues
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.deathResetTimers
import org.rsmod.api.player.disablePrayers
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.realm.Realm
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.stat.StatTypeList
import org.rsmod.map.CoordGrid

@Singleton
public class PlayerDeath
@Inject
constructor(
    private val statTypes: StatTypeList,
    private val realm: Realm,
    private val players: PlayerList,
    private val pvpKills: PvpKillTracker,
    private val objRepo: ObjRepository,
    private val objTypes: ObjTypeList,
) {
    private var Player.specialAttackType by intVarp(varps.sa_attack)
    private var Player.pkPoints by intVarp(varps.mike_pk_points)
    private var Player.pkKills by intVarp(varps.mike_pk_kills)
    private var Player.pkDeaths by intVarp(varps.mike_pk_deaths)
    private var Player.pkBestStreak by intVarp(varps.mike_pk_best_streak)
    private val Player.protectItemActive by boolVarBit(varbits.protect_item)

    // PK-punten/killstreaks gelden alleen op de PvP-wereld (RSMOD_WORLD=2).
    private val pvpWorld = System.getenv("RSMOD_WORLD") == "2"

    public suspend fun death(access: ProtectedAccess) {
        access.deathSequence()
    }

    private suspend fun ProtectedAccess.deathSequence() {
        // Respawn op de realm-respawnCoord (bv. Edgeville op de PvP-wereld) i.p.v. hardcoded.
        val respawn = realm.config.respawnCoord
        val randomRespawn = mapFindSquareLineOfWalk(respawn, minRadius = 0, maxRadius = 2)
        val deathCoords = player.coords
        val killer = if (pvpWorld) awardPvpKill() else null
        stopAction()
        delay(2)
        anim(seqs.human_death)
        delay(4)
        combatClearQueue()
        clearQueue(queues.death)
        midiSong(midis.stop_music)
        midiJingle(jingles.death_jingle_2)
        mes("Oh dear, you are dead!")
        if (killer != null) {
            dropPvpItems(killer, deathCoords)
        }
        telejump(randomRespawn ?: respawn)
        resetAnim()
        resetPlayerState(statTypes)
        restoreToplevelTabs(
            components.toplevel_target_pvp_icons,
            components.toplevel_target_side1,
            components.toplevel_target_side2,
            components.toplevel_target_side4,
            components.toplevel_target_side5,
            components.toplevel_target_side6,
            components.toplevel_target_side9,
            components.toplevel_target_side8,
            components.toplevel_target_side7,
            components.toplevel_target_side10,
            components.toplevel_target_side11,
            components.toplevel_target_side12,
            components.toplevel_target_side13,
        )
    }

    /** Kent PK-punten toe aan de killer en beeindigt de streak van het slachtoffer. */
    private fun ProtectedAccess.awardPvpKill(): Player? {
        val victim = player
        val killer = findHero() ?: return null
        if (killer === victim) {
            return null
        }
        victim.pkDeaths += 1
        val victimStreakLost = pvpKills.endStreak(victim.displayName)
        val result =
            pvpKills.recordKill(killer.displayName, victim.displayName, killer.pkKills, killer.pkPoints)
        killer.pkKills = result.kills
        killer.pkPoints = result.points
        killer.pkBestStreak = maxOf(killer.pkBestStreak, result.bestStreak)
        val farmNote = if (result.farmed) " (verlaagd - anti-farm)" else ""
        killer.mes("You killed ${victim.displayName}! +${result.gained} PK points$farmNote.")
        killer.mes(
            "Streak: ${result.streak} (best: ${killer.pkBestStreak}). " +
                "Total: ${result.points} PK points (::pkspend)."
        )
        victim.mes(
            "You were killed by ${killer.displayName}." +
                if (victimStreakLost > 1) " Your streak of $victimStreakLost is over!" else ""
        )
        // Kill-feed: elke kill naar alle online spelers (extra melding bij een bounty-claim).
        val bountyNote = if (result.bountyClaimed) " [BOUNTY CLAIMED! +bonus]" else ""
        for (online in players) {
            online.mes("[PK] ${killer.displayName} killed ${victim.displayName}!$bountyNote")
        }
        // Broadcast bij streak-mijlpalen (5, 10, 15, ...).
        if (result.streak >= 5 && result.streak % 5 == 0) {
            for (online in players) {
                online.mes(
                    "[PK] ${killer.displayName} is on a ${result.streak} killstreak! " +
                        "Take them down for bonus glory!"
                )
            }
        }
        return killer
    }

    private fun ProtectedAccess.dropPvpItems(killer: Player, deathCoords: CoordGrid) {
        val items = deathItems()
        if (items.isEmpty()) {
            return
        }
        // Wilderness Rules v2 (Fase 3):
        //  - Untradeables blijven ALTIJD bij het slachtoffer (geen item-loss, voorspelbaar).
        //  - Van de tradeables behoud je de 3 waardevolste (4 met Protect Item-prayer aan).
        //  - De rest dropt owner-locked voor de killer.
        val untradeables = items.filter { !objTypes[it.obj].tradeable }
        val tradeables = items.filter { objTypes[it.obj].tradeable }
        val keepCount = if (player.protectItemActive) 4 else 3
        val keptTradeables =
            tradeables
                .sortedWith(
                    compareByDescending<DeathItem> { deathValue(it.obj) }
                        .thenBy { if (it.worn) 0 else 1 }
                        .thenBy { it.slot }
                )
                .take(keepCount)
                .toSet()
        val kept = untradeables.toSet() + keptTradeables
        val dropped = tradeables.filter { it !in keptTradeables }

        clearDeathItems(items)
        restoreKeptItems(kept, deathCoords)

        val duration = killer.lootDropDuration ?: constants.lootdrop_duration
        for (entry in dropped) {
            objRepo.add(entry.obj, deathCoords, duration, killer)
        }
        val extras = buildString {
            if (untradeables.isNotEmpty()) append(" (incl. ${untradeables.size} untradeable behouden)")
            if (player.protectItemActive) append(" [Protect Item: +1]")
        }
        mes(
            "You kept ${kept.size} item(s)$extras. " +
                "${dropped.size} item stack(s) dropped for ${killer.displayName}."
        )
        if (dropped.isNotEmpty()) {
            killer.mes("${player.displayName} dropped ${dropped.size} item stack(s).")
        }
    }

    private fun ProtectedAccess.deathItems(): List<DeathItem> = buildList {
        collectDeathItems(player.worn, worn = true)
        collectDeathItems(player.inv, worn = false)
    }

    private fun MutableList<DeathItem>.collectDeathItems(inventory: Inventory, worn: Boolean) {
        for (slot in inventory.indices) {
            val obj = inventory[slot] ?: continue
            add(DeathItem(inventory, slot, obj, worn))
        }
    }

    private fun clearDeathItems(items: List<DeathItem>) {
        for (entry in items) {
            entry.inventory[entry.slot] = null
        }
    }

    private fun ProtectedAccess.restoreKeptItems(
        items: Collection<DeathItem>,
        fallbackCoords: CoordGrid,
    ) {
        val duration = constants.lootdrop_duration
        val sortedItems = items.sortedWith(compareBy<DeathItem> { if (it.worn) 0 else 1 }.thenBy { it.slot })
        for (entry in sortedItems) {
            if (entry.worn && player.worn[entry.slot] == null) {
                player.worn[entry.slot] = entry.obj
                continue
            }
            val invSlot = firstFreeSlot(player.inv)
            if (invSlot != null) {
                player.inv[invSlot] = entry.obj
            } else {
                objRepo.add(entry.obj, fallbackCoords, duration, player)
            }
        }
    }

    private fun firstFreeSlot(inventory: Inventory): Int? {
        return inventory.indices.firstOrNull { inventory[it] == null }
    }

    private fun deathValue(obj: InvObj): Long {
        val type = objTypes[obj]
        val each = maxOf(type.playerCost, type.playerCostDerived, type.cost, 0)
        return each.toLong() * obj.count.toLong().coerceAtLeast(1L)
    }

    private fun ProtectedAccess.resetPlayerState(stats: StatTypeList) {
        player.disablePrayers()
        player.deathResetTimers()

        player.specialAttackType = 0
        player.skullIcon = null

        rebuildAppearance()

        camReset()
        statRestoreAll(stats.values)
        minimapReset()
    }

    private data class DeathItem(
        val inventory: Inventory,
        val slot: Int,
        val obj: InvObj,
        val worn: Boolean,
    )
}
