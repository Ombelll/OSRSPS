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
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** De buit die Mike the Destroyer laat vallen (bestaande cache-items). */
internal object BossDropObjs : ObjReferences() {
    val rune_scimitar = find("rune_scimitar")
    val rune_platebody = find("rune_platebody")
    val rune_full_helm = find("rune_full_helm")
    val rune_kiteshield = find("rune_kiteshield")
    val dragon_scimitar = find("dragon_scimitar")
    val big_bones = find("big_bones")
}

/**
 * MIKE THE DESTROYER (custom boss).
 *
 * - `::mikeboss`  : teleporteert je naar de arena en spawnt de boss naast je.
 * - Drop-tabel    : gegarandeerd big bones + 5k..15k coins + 1 (soms 2) rune-gear stuk;
 *                   zeldzaam een dragon scimitar (1/50) of Mike's Lucky Cabbage (1/25).
 */
class MikeBoss
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

    private val gear =
        listOf(
            BossDropObjs.rune_scimitar,
            BossDropObjs.rune_platebody,
            BossDropObjs.rune_full_helm,
            BossDropObjs.rune_kiteshield,
        )

    override fun ScriptContext.startup() {
        onCommand("mikeboss") {
            desc = "Spawn Mike the Destroyer right next to you (stand in open ground!)"
            cheat {
                val boss = Npc(npcTypes[BossNpcs.green_dragon], player.coords.translate(2, 2))
                boss.mode = NpcMode.None
                npcRepo.add(boss, duration = 1000)
                boss.startBossMechanics()
                player.mes("Mike the Destroyer awakens beside you! Attack to begin the fight.")
            }
        }

        onNpcQueue(BossNpcs.green_dragon, queues.death) { dropLoot() }
    }

    private suspend fun StandardNpcAccess.dropLoot() {
        val coords = npc.coords
        val hero = findHero(players)
        death.deathNoDrops(this)

        // Gegarandeerd:
        drop(BossDropObjs.big_bones, coords, hero)
        drop(objs.coins, coords, hero, count = 5000 + random.of(maxExclusive = 10001)) // 5000..15000

        // 1 rune-gear stuk, met kans op een tweede:
        drop(gear[random.of(maxExclusive = gear.size)], coords, hero)
        if (random.of(maxExclusive = 3) == 0) {
            drop(gear[random.of(maxExclusive = gear.size)], coords, hero)
        }

        // Zeldzame drops:
        if (random.of(maxExclusive = 50) == 0) {
            drop(BossDropObjs.dragon_scimitar, coords, hero) // 1/50
        }
        if (random.of(maxExclusive = 25) == 0) {
            drop(objs.cabbage, coords, hero) // Mike's Lucky Cabbage (1/25)
        }
        hero?.let { awardMilestone(it, coords) }
    }

    private fun drop(type: ObjType, coords: CoordGrid, hero: Player?, count: Int = 1) {
        objRepo.add(type, coords, lootDuration, hero, count)
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
