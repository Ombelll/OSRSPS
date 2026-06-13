package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onCommand
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.stat.StatType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object QuestObjs : ObjReferences() {
    val runite_bar = find("runite_bar")
    val diamond = find("diamond")
    val dragon_bones = find("dragon_bones")
    val rune_platebody = find("rune_platebody")
    val raw_shark = find("raw_shark")
    val chinchompa = find("chinchompa_captured")
}

/**
 * QUEST-PACK — fetch-quests die aansluiten op de skill-/boss-systemen. Volledig stateless:
 * elke quest kijkt of je het gevraagde item bij je hebt en beloont je dan.
 *
 *  - ::smithquest  : 5 runite bars  -> rune platebody + 20k coins + Smithing-XP
 *  - ::gemquest    : 1 diamond      -> 25k coins + Crafting-XP
 *  - ::dragonquest : 5 dragon bones -> 30k coins + Prayer-XP
 *  - ::quests      : overzicht
 */
class QuestPack @Inject constructor(private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("smithquest") {
            desc = "Quest: The Blacksmith's Order"
            cheat {
                protectedAccess.launch(player) {
                    fetchQuest(
                        intro = "The Blacksmith says: 'Forge me 5 runite bars and I'll reward you!'",
                        item = QuestObjs.runite_bar,
                        itemCount = 5,
                        rewardItem = QuestObjs.rune_platebody,
                        rewardCount = 1,
                        coins = 20_000,
                        xpStat = stats.smithing,
                        xp = 5_000.0,
                        complete = "'Fine work! Take this rune platebody and 20,000 coins.' QUEST COMPLETE!",
                        incomplete = "'Come back with 5 runite bars. Smith them at an anvil... or smelt ore first.'",
                    )
                }
            }
        }
        onCommand("gemquest") {
            desc = "Quest: The Gem Collector"
            cheat {
                protectedAccess.launch(player) {
                    fetchQuest(
                        intro = "The Gem Collector says: 'Bring me a cut diamond and I'll pay handsomely!'",
                        item = QuestObjs.diamond,
                        itemCount = 1,
                        rewardItem = null,
                        rewardCount = 0,
                        coins = 25_000,
                        xpStat = stats.crafting,
                        xp = 4_000.0,
                        complete = "'A flawless diamond! Here's 25,000 coins.' QUEST COMPLETE!",
                        incomplete = "'Mine a gem, cut it with a chisel, then bring me the diamond.'",
                    )
                }
            }
        }
        onCommand("dragonquest") {
            desc = "Quest: The Dragon Hunter"
            cheat {
                protectedAccess.launch(player) {
                    fetchQuest(
                        intro = "The Hunter says: 'Slay dragons and bring me 5 dragon bones for glory!'",
                        item = QuestObjs.dragon_bones,
                        itemCount = 5,
                        rewardItem = null,
                        rewardCount = 0,
                        coins = 30_000,
                        xpStat = stats.prayer,
                        xp = 8_000.0,
                        complete = "'A true dragon slayer! Take 30,000 coins and a prayer blessing.' QUEST COMPLETE!",
                        incomplete = "'Bring me 5 dragon bones. The wyrm-bosses (::dragonboss) drop them.'",
                    )
                }
            }
        }
        onCommand("fishquest") {
            desc = "Quest: The Hungry Fisherman"
            cheat {
                protectedAccess.launch(player) {
                    fetchQuest(
                        intro = "The Fisherman says: 'I'm starving! Catch me 5 raw sharks.'",
                        item = QuestObjs.raw_shark,
                        itemCount = 5,
                        rewardItem = null,
                        rewardCount = 0,
                        coins = 20_000,
                        xpStat = stats.fishing,
                        xp = 4_000.0,
                        complete = "'Magnificent sharks! Here's 20,000 coins.' QUEST COMPLETE!",
                        incomplete = "'Bring me 5 raw sharks - fish them at a fishing spot.'",
                    )
                }
            }
        }
        onCommand("huntquest") {
            desc = "Quest: The Chinchompa Collector"
            cheat {
                protectedAccess.launch(player) {
                    fetchQuest(
                        intro = "The Hunter says: 'Catch me 3 chinchompas and I'll pay you well!'",
                        item = QuestObjs.chinchompa,
                        itemCount = 3,
                        rewardItem = null,
                        rewardCount = 0,
                        coins = 25_000,
                        xpStat = stats.hunter,
                        xp = 4_000.0,
                        complete = "'Perfect little critters! Take 25,000 coins.' QUEST COMPLETE!",
                        incomplete = "'Bring me 3 chinchompas - hunt them with ::npc hunting_chinchompa.'",
                    )
                }
            }
        }
        onCommand("quests") {
            desc = "List all quests"
            cheat {
                player.mes("Quests:")
                player.mes("::quest (Cabbage of Power), ::smithquest, ::gemquest,")
                player.mes("::dragonquest, ::fishquest, ::huntquest, ::alchemistpact")
            }
        }
    }

    private suspend fun ProtectedAccess.fetchQuest(
        intro: String,
        item: ObjType,
        itemCount: Int,
        rewardItem: ObjType?,
        rewardCount: Int,
        coins: Int,
        xpStat: StatType,
        xp: Double,
        complete: String,
        incomplete: String,
    ) {
        mesbox(intro)
        val accept = choice2("Yes, I accept.", true, "Not now.", false, title = "Accept the task?")
        if (!accept) {
            mesbox("'Very well, perhaps another time.'")
            return
        }
        if (invTotal(inv, item) < itemCount) {
            mesbox(incomplete)
            return
        }
        invDel(inv, item, itemCount)
        if (rewardItem != null && rewardCount > 0) {
            invAdd(inv, rewardItem, rewardCount)
        }
        if (coins > 0) {
            invAdd(inv, objs.coins, coins)
        }
        statAdvance(xpStat, PlayerStatMap.toFineXP(xp).toDouble())
        PvmProgress.recordQuest(player)
        mesbox(complete)
    }
}
