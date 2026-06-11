package org.rsmod.content.skills.slayer

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.queues
import org.rsmod.api.config.refs.stats
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.npc.NpcType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Slayer-master + speciale slayer-monsters (eigen NPC's, geen conflict met de droptables). */
internal object SlayerNpcs : NpcReferences() {
    val master = find("slayer_master_nieve")
    val crawling_hand = find("slayer_crawling_hand_1")
    val cave_crawler = find("slayer_cave_crawler_1")
    val banshee = find("slayer_banshee_1")
    val rockslug = find("slayer_rockslug")
    val cockatrice = find("slayer_cockatrice")
    val pyrefiend = find("slayer_pyrefiend_1")
    val gargoyle = find("slayer_gargoyle_1")
    val nechryael = find("slayer_nechryael")
    val abyssal = find("slayer_abyssal")
}

/** Beloningen in de Slayer-shop (gekocht met Slayer-punten). */
internal object SlayerRewardObjs : ObjReferences() {
    val super_combat = find("4dose2combat")
    val abyssal_whip = find("abyssal_whip")
}

/** Eén slayer-monster: de NPC, een label voor de taak, en de Slayer-XP per kill. */
private class SlayerMonster(val npc: NpcType, val label: String, val xp: Double)

/** Een toegewezen taak: welk monster en hoeveel er nog over zijn. */
private class Task(val label: String, var remaining: Int)

/**
 * SLAYER.
 *
 * - Praat met een Slayer-master (NPC 'slayer_master_nieve') -> krijgt een taak
 *   (bv. "kill 15 banshees"), bijgehouden per speler.
 * - Het doden van een slayer-monster geeft Slayer-XP (+ botten). Als het monster je
 *   huidige taak is, telt de teller af tot de taak klaar is.
 *
 * (Deze slayer-monsters hebben hun eigen NPC-typen, dus dit botst niet met de bestaande
 *  droptables van de gewone monsters.)
 */
class Slayer
@Inject
constructor(
    private val death: NpcDeath,
    private val objRepo: ObjRepository,
    private val players: PlayerList,
    private val random: GameRandom,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    private val monsters: List<SlayerMonster> by lazy {
        listOf(
            SlayerMonster(SlayerNpcs.crawling_hand, "crawling hands", 12.0),
            SlayerMonster(SlayerNpcs.cave_crawler, "cave crawlers", 23.0),
            SlayerMonster(SlayerNpcs.banshee, "banshees", 22.0),
            SlayerMonster(SlayerNpcs.rockslug, "rockslugs", 25.0),
            SlayerMonster(SlayerNpcs.cockatrice, "cockatrice", 41.0),
            SlayerMonster(SlayerNpcs.pyrefiend, "pyrefiends", 43.0),
            SlayerMonster(SlayerNpcs.gargoyle, "gargoyles", 105.0),
            SlayerMonster(SlayerNpcs.nechryael, "nechryael", 105.0),
            SlayerMonster(SlayerNpcs.abyssal, "abyssal demons", 150.0),
        )
    }

    /** Per-speler toegewezen taak + verdiende punten (in-memory; sessie-gebonden). */
    private val tasks = HashMap<Player, Task>()
    private val points = HashMap<Player, Int>()

    override fun ScriptContext.startup() {
        onOpNpc1(SlayerNpcs.master) { assignTask() }
        for (def in monsters) {
            onNpcQueue(def.npc, queues.death) { processDeath(def) }
        }

        onCommand("slayerpoints") {
            desc = "Check your Slayer points"
            cheat { player.mes("You have ${points[player] ?: 0} Slayer points.") }
        }
        onCommand("slayershop") {
            desc = "Spend Slayer points on rewards"
            cheat { protectedAccess.launch(player) { openShop() } }
        }
    }

    private suspend fun ProtectedAccess.openShop() {
        val pts = points[player] ?: 0
        mesbox("You have $pts Slayer points to spend.")
        val pick =
            choice3(
                "Super combat potion (10 pts)",
                1,
                "Abyssal whip (50 pts)",
                2,
                "Nothing",
                0,
                title = "Slayer Rewards",
            )
        val cost = if (pick == 1) 10 else if (pick == 2) 50 else 0
        if (pick == 0) return
        if (pts < cost) {
            mesbox("You need $cost Slayer points but only have $pts.")
            return
        }
        points[player] = pts - cost
        if (pick == 1) invAdd(inv, SlayerRewardObjs.super_combat) else invAdd(inv, SlayerRewardObjs.abyssal_whip)
        mesbox("Purchased! You have ${points[player]} Slayer points left.")
    }

    private fun ProtectedAccess.assignTask() {
        val def = monsters[random.of(maxExclusive = monsters.size)]
        val amount = 10 + random.of(maxExclusive = 11) // 10..20
        tasks[player] = Task(def.label, amount)
        mes("Excellent. Your new Slayer task is to kill $amount ${def.label}. Good luck!")
    }

    private suspend fun StandardNpcAccess.processDeath(def: SlayerMonster) {
        val coords = npc.coords
        val hero = findHero(players)
        death.deathNoDrops(this)
        objRepo.add(objs.bones, coords, 200, hero, 1)
        hero?.let { award(it, def) }
    }

    private fun award(player: Player, def: SlayerMonster) {
        player.statAdvance(stats.slayer, PlayerStatMap.toFineXP(def.xp).toDouble())
        val task = tasks[player]
        if (task != null && task.label == def.label) {
            task.remaining -= 1
            if (task.remaining <= 0) {
                tasks.remove(player)
                points[player] = (points[player] ?: 0) + 10
                player.mes(
                    "Slayer task complete! +10 Slayer points (total ${points[player]}). " +
                        "Spend them with ::slayershop."
                )
            } else {
                player.mes("Slayer task: ${task.remaining} x ${task.label} remaining.")
            }
        }
    }
}
