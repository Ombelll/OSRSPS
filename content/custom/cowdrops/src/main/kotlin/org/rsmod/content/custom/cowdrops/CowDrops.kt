package org.rsmod.content.custom.cowdrops

import jakarta.inject.Inject
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.queues
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Bestaande cache-items die monsters droppen (geen nieuwe types). */
internal object MonsterDropObjs : ObjReferences() {
    val raw_beef = find("raw_beef")
    val cow_hide = find("cow_hide")
    val raw_chicken = find("raw_chicken")
    val feather = find("feather")
    // Hill Giant loot:
    val big_bones = find("big_bones")
    val limpwurt_root = find("limpwurt_root")
    val iron_scimitar = find("iron_scimitar")
    val steel_scimitar = find("steel_scimitar")
}

/** Monsters die we per type aanhaken (cow gaat via content-groep). */
internal object MonsterDropNpcs : NpcReferences() {
    val chicken = find("chicken")
    val goblin = find("goblin")
    val giant = find("giant") // hill giant (mini-boss)

    // Extra monsters (drop bones/coins + gedeelde zeldzame drops):
    val spider = find("spider")
    val scorpion = find("scorpion")
    val wolf = find("wolf")
    val imp = find("imp")
    val man = find("man")
    val woman = find("woman")
    val barbarian = find("barbarian")
    val unicorn = find("unicorn")
    val monk = find("monk")
    val ghost = find("ghost")
    val mugger = find("mugger")
    val giant_frog = find("giant_frog")
    val ogre = find("ogre")
}

/**
 * Geeft meerdere monsters een echte DROPTABLE i.p.v. alleen botten.
 *
 * - cow     : altijd raw beef + cowhide   (hele content-groep 'cow' = alle koe-varianten)
 * - chicken : altijd raw chicken + 5..15 feathers
 * - goblin  : altijd 3..20 coins
 *
 * Gedeelde ZELDZAME drops voor elk monster:
 *   - 1/20 : coins-jackpot (100..500)
 *   - 1/50 : Mike's Lucky Cabbage (het gereskinde item)
 */
class MonsterDrops
@Inject
constructor(
    private val death: NpcDeath,
    private val objRepo: ObjRepository,
    private val players: PlayerList,
    private val random: GameRandom,
) : PluginScript() {
    private val lootDuration = 200

    override fun ScriptContext.startup() {
        onNpcQueue(content.cow, queues.death) {
            processDeath { c, h ->
                drop(MonsterDropObjs.raw_beef, c, h)
                drop(MonsterDropObjs.cow_hide, c, h)
            }
        }
        onNpcQueue(MonsterDropNpcs.chicken, queues.death) {
            processDeath { c, h ->
                drop(MonsterDropObjs.raw_chicken, c, h)
                drop(MonsterDropObjs.feather, c, h, count = 5 + random.of(maxExclusive = 11))
            }
        }
        onNpcQueue(MonsterDropNpcs.goblin, queues.death) {
            processDeath { c, h -> drop(objs.coins, c, h, count = 3 + random.of(maxExclusive = 18)) }
        }
        // Hill Giant (mini-boss): grotere/betere loot.
        onNpcQueue(MonsterDropNpcs.giant, queues.death) {
            processDeath { c, h ->
                drop(MonsterDropObjs.big_bones, c, h)
                drop(MonsterDropObjs.limpwurt_root, c, h)
                drop(objs.coins, c, h, count = 20 + random.of(maxExclusive = 41)) // 20..60
                if (random.of(maxExclusive = 8) == 0) { // 1/8: een scimitar
                    val weapon =
                        if (random.of(maxExclusive = 2) == 0) MonsterDropObjs.iron_scimitar
                        else MonsterDropObjs.steel_scimitar
                    drop(weapon, c, h)
                }
            }
        }

        // ---- Extra monsters: bones/coins + (soms) een thematische drop ----
        monster(MonsterDropNpcs.spider, bones = null, coins = 1..5)
        monster(MonsterDropNpcs.scorpion, bones = null, coins = 1..8)
        monster(MonsterDropNpcs.wolf, coins = 1..10)
        monster(MonsterDropNpcs.imp, bones = null, coins = 2..15)
        monster(MonsterDropNpcs.man, coins = 3..25)
        monster(MonsterDropNpcs.woman, coins = 3..25)
        monster(MonsterDropNpcs.barbarian, coins = 5..30)
        monster(MonsterDropNpcs.monk, coins = 5..20)
        monster(MonsterDropNpcs.ghost, bones = null, coins = 5..25)
        monster(MonsterDropNpcs.mugger, coins = 10..40)
        monster(MonsterDropNpcs.unicorn, coins = 5..20)
        monster(MonsterDropNpcs.giant_frog, bones = MonsterDropObjs.big_bones, coins = 5..20)
        monster(
            MonsterDropNpcs.ogre,
            bones = MonsterDropObjs.big_bones,
            coins = 20..60,
            extra = MonsterDropObjs.limpwurt_root,
        )

        // ---- 100 extra monsters, allemaal met een generieke droptable ----
        BulkMonsters.all.forEach { monster(it, coins = 1..20) }
    }

    /**
     * Generieke registratie voor 'simpele' monsters: optioneel botten, optioneel een
     * bereik aan coins, en optioneel één thematisch item. De gedeelde zeldzame drops
     * (via processDeath) gelden ook hier.
     */
    private fun ScriptContext.monster(
        npc: NpcType,
        bones: ObjType? = objs.bones,
        coins: IntRange? = null,
        extra: ObjType? = null,
    ) {
        onNpcQueue(npc, queues.death) {
            processDeath { c, h ->
                bones?.let { drop(it, c, h) }
                coins?.let {
                    val amount = it.first + random.of(maxExclusive = (it.last - it.first + 1))
                    drop(objs.coins, c, h, count = amount)
                }
                extra?.let { drop(it, c, h) }
            }
        }
    }

    /** Voert de death-sequence uit, bepaalt killer + plek, draait de monster-specifieke
     *  loot en daarna de gedeelde zeldzame drops. */
    private suspend fun StandardNpcAccess.processDeath(common: (CoordGrid, Player?) -> Unit) {
        val coords = npc.coords
        val hero = findHero(players)
        death.deathNoDrops(this)
        common(coords, hero)
        rollRareDrops(coords, hero)
    }

    private fun rollRareDrops(coords: CoordGrid, hero: Player?) {
        if (random.of(maxExclusive = 20) == 0) {
            drop(objs.coins, coords, hero, count = 100 + random.of(maxExclusive = 401)) // 100..500
        }
        if (random.of(maxExclusive = 50) == 0) {
            drop(objs.cabbage, coords, hero) // = Mike's Lucky Cabbage (gereskind)
        }
    }

    private fun drop(type: ObjType, coords: CoordGrid, hero: Player?, count: Int = 1) {
        objRepo.add(type, coords, lootDuration, hero, count)
    }
}
