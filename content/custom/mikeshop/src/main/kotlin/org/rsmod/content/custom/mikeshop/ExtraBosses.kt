package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.queues
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.output.mes
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Elite-buit van de zwaardere bosses (incl. de BIS-items als zeldzame drops). */
internal object EliteDropObjs : ObjReferences() {
    val abyssal_whip = find("abyssal_whip")
    val bandos_chestplate = find("bandos_chestplate")
    val bandos_tassets = find("bandos_skirt")
    val dragonfire_shield = find("dragonfire_shield")
    val dragon_scimitar = find("dragon_scimitar")
    val dragon_boots = find("dragon_boots")
    val big_bones = find("big_bones")

    // Top-tier drops voor de zwaardere wyrm-bosses:
    val dragon_platebody = find("dragon_platebody")
    val dragon_2h_sword = find("dragon_2h_sword")
    val dragon_claws = find("dragon_claws")
    val dragon_chainbody = find("dragon_chainbody")
    val dragon_dagger = find("dragon_dagger")
}

/**
 * BOSS-GAUNTLET (vervolg op ::mikeboss).
 *
 *  - ::demonboss  -> "Inferno Demon"   : big bones + 8-23k coins + dragon scimitar,
 *                                        1/40 abyssal whip, 1/20 Lucky Cabbage.
 *  - ::finalboss  -> "Mike's Nightmare": big bones + 15-35k coins + dragon boots,
 *                                        1/15 whip, 1/30 bandos chest/tassets,
 *                                        1/40 dragonfire shield, 1/10 Lucky Cabbage.
 */
class ExtraBosses
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val objRepo: ObjRepository,
    private val players: PlayerList,
    private val random: GameRandom,
    private val death: NpcDeath,
) : PluginScript() {
    private val lootDuration = 200

    override fun ScriptContext.startup() {
        onCommand("demonboss") {
            desc = "Spawn the Inferno Demon next to you (stand in open ground!)"
            cheat {
                spawnBoss(player.coords, BossNpcs.greater_demon)
                player.mes("The Inferno Demon awakens beside you!")
            }
        }
        onCommand("finalboss") {
            desc = "Spawn Mike's Nightmare next to you (stand in open ground!)"
            cheat {
                spawnBoss(player.coords, BossNpcs.black_demon)
                player.mes("Mike's Nightmare awakens beside you! Best of luck...")
            }
        }

        onCommand("dragonboss") {
            desc = "Spawn the Frost Wyrm next to you (stand in open ground!)"
            cheat {
                spawnBoss(player.coords, BossNpcs.blue_dragon)
                player.mes("The Frost Wyrm descends beside you!")
            }
        }
        onCommand("infernoboss") {
            desc = "Spawn the Crimson Wyrm next to you (stand in open ground!)"
            cheat {
                spawnBoss(player.coords, BossNpcs.red_dragon)
                player.mes("The Crimson Wyrm descends beside you!")
            }
        }
        onCommand("godboss") {
            desc = "Spawn The Ancient One next to you (the ultimate boss!)"
            cheat {
                spawnBoss(player.coords, BossNpcs.steel_dragon)
                player.mes("The Ancient One awakens... this is the ultimate test.")
            }
        }
        onCommand("bosses") {
            desc = "List all boss commands"
            cheat {
                player.mes("Bosses (weakest -> strongest):")
                player.mes("::mikeboss, ::demonboss, ::finalboss,")
                player.mes("::dragonboss, ::infernoboss, ::godboss")
            }
        }

        onNpcQueue(BossNpcs.greater_demon, queues.death) { demonLoot() }
        onNpcQueue(BossNpcs.black_demon, queues.death) { nightmareLoot() }
        onNpcQueue(BossNpcs.blue_dragon, queues.death) { wyrmLoot(2) }
        onNpcQueue(BossNpcs.red_dragon, queues.death) { wyrmLoot(3) }
        onNpcQueue(BossNpcs.steel_dragon, queues.death) { ancientLoot() }
    }

    private fun spawnBoss(coords: CoordGrid, type: NpcType) {
        val boss = Npc(npcTypes[type], coords.translate(2, 2))
        boss.mode = NpcMode.None
        npcRepo.add(boss, duration = 1000)
    }

    private suspend fun StandardNpcAccess.demonLoot() {
        val coords = npc.coords
        val hero = findHero(players)
        death.deathNoDrops(this)
        hero?.let { awardMilestone(it, coords) }
        drop(EliteDropObjs.big_bones, coords, hero)
        drop(objs.coins, coords, hero, count = 8000 + random.of(maxExclusive = 15001)) // 8k..23k
        drop(EliteDropObjs.dragon_scimitar, coords, hero)
        if (random.of(maxExclusive = 40) == 0) drop(EliteDropObjs.abyssal_whip, coords, hero)
        if (random.of(maxExclusive = 20) == 0) drop(objs.cabbage, coords, hero)
    }

    private suspend fun StandardNpcAccess.nightmareLoot() {
        val coords = npc.coords
        val hero = findHero(players)
        death.deathNoDrops(this)
        hero?.let { awardMilestone(it, coords) }
        drop(EliteDropObjs.big_bones, coords, hero)
        drop(objs.coins, coords, hero, count = 15000 + random.of(maxExclusive = 20001)) // 15k..35k
        drop(EliteDropObjs.dragon_boots, coords, hero)
        if (random.of(maxExclusive = 15) == 0) drop(EliteDropObjs.abyssal_whip, coords, hero)
        if (random.of(maxExclusive = 30) == 0) drop(EliteDropObjs.bandos_chestplate, coords, hero)
        if (random.of(maxExclusive = 30) == 0) drop(EliteDropObjs.bandos_tassets, coords, hero)
        if (random.of(maxExclusive = 40) == 0) drop(EliteDropObjs.dragonfire_shield, coords, hero)
        if (random.of(maxExclusive = 10) == 0) drop(objs.cabbage, coords, hero)
    }

    /** Frost/Crimson Wyrm: schaalt met tier (2 = blauw, 3 = rood). */
    private suspend fun StandardNpcAccess.wyrmLoot(tier: Int) {
        val coords = npc.coords
        val hero = findHero(players)
        death.deathNoDrops(this)
        hero?.let { awardMilestone(it, coords) }
        drop(EliteDropObjs.big_bones, coords, hero)
        drop(objs.coins, coords, hero, count = 10000 * tier + random.of(maxExclusive = 10001))
        drop(EliteDropObjs.dragon_dagger, coords, hero)
        if (random.of(maxExclusive = 8) == 0) drop(EliteDropObjs.dragon_chainbody, coords, hero)
        if (random.of(maxExclusive = (22 - tier * 4)) == 0) {
            drop(EliteDropObjs.abyssal_whip, coords, hero)
        }
        if (random.of(maxExclusive = 20) == 0) drop(EliteDropObjs.dragon_claws, coords, hero)
        if (random.of(maxExclusive = 12) == 0) drop(objs.cabbage, coords, hero)
    }

    /** The Ancient One: de eindbaas met de beste buit. */
    private suspend fun StandardNpcAccess.ancientLoot() {
        val coords = npc.coords
        val hero = findHero(players)
        death.deathNoDrops(this)
        hero?.let { awardMilestone(it, coords) }
        drop(EliteDropObjs.big_bones, coords, hero)
        drop(objs.coins, coords, hero, count = 50000 + random.of(maxExclusive = 50001)) // 50k..100k
        drop(EliteDropObjs.dragon_platebody, coords, hero) // gegarandeerd
        if (random.of(maxExclusive = 4) == 0) drop(EliteDropObjs.dragon_2h_sword, coords, hero)
        if (random.of(maxExclusive = 5) == 0) drop(EliteDropObjs.dragon_claws, coords, hero)
        if (random.of(maxExclusive = 10) == 0) {
            drop(EliteDropObjs.abyssal_whip, coords, hero)
            hero?.let { broadcast("News: ${it.displayName} received an Abyssal whip from The Ancient One!") }
        }
        if (random.of(maxExclusive = 15) == 0) drop(EliteDropObjs.dragonfire_shield, coords, hero)
        if (random.of(maxExclusive = 6) == 0) drop(objs.cabbage, coords, hero)
    }

    private fun drop(type: ObjType, coords: CoordGrid, hero: Player?, count: Int = 1) {
        objRepo.add(type, coords, lootDuration, hero, count)
    }

    /** Stuurt een melding naar alle online spelers (drop-announcement). */
    private fun broadcast(text: String) {
        for (p in players) p.mes(text)
    }

    /** Telt de boss-kill mee voor progressie en geeft elke 10 kills een bonus. */
    private fun awardMilestone(hero: Player, coords: CoordGrid) {
        val kc = PvmProgress.recordBossKill(hero)
        if (kc % 10 == 0) {
            drop(objs.coins, coords, hero, 100_000)
            hero.mes("PvM milestone: $kc boss kills! Bonus 100,000 coins!")
        }
    }
}
