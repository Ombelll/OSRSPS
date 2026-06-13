package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onCommand
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Persistente diary-claim-varp (Perm = saved naar game.db). */
internal object DiaryVarpBuilder : VarpBuilder() {
    init {
        build("mikeq_diary")
    }
}

internal object DiaryVarps : VarpReferences() {
    val diary = find("mikeq_diary")
}

internal object DiaryObjs : ObjReferences() {
    val dragon_scimitar = find("dragon_scimitar")
    val super_combat = find("4dose2combat")
}

/**
 * ACHIEVEMENT DIARY (persistent, Phase 6).
 *
 * `::diary` toont je voortgang over de persistente quest-engine quests. Dezelfde diary-varp houdt
 * het hoogste geclaimde reward-tier bij: 1 = adventurer, 2 = veteran.
 */
class Diary @Inject constructor(private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("diary") {
            desc = "View your quest diary and claim the reward"
            cheat { protectedAccess.launch(player) { showDiary() } }
        }
    }

    private fun mark(done: Boolean): String = if (done) "Done" else "Not done"

    private suspend fun ProtectedAccess.showDiary() {
        val q1 = vars[QuestVarps.dragonsheart] >= 3
        val q2 = vars[QuestVarps.smithmaster] >= 3
        val q3 = vars[QuestVarps.gemheist] >= 2
        val q4 = vars[QuestVarps.bossslayer] >= 3
        val q5 = vars[QuestVarps.alchemistpact] >= 3
        val completed = listOf(q1, q2, q3, q4, q5).count { it }
        val tier = vars[DiaryVarps.diary]

        val rewardLine =
            when {
                tier >= 2 -> "Rewards: all tiers claimed"
                completed >= 5 && tier < 2 -> "Veteran reward: READY - claiming now!"
                completed >= 3 && tier < 1 -> "Adventurer reward: READY - claiming now!"
                tier >= 1 -> "Veteran reward: $completed / 5 quests done"
                else -> "Adventurer reward: $completed / 3 quests done"
            }
        mesbox(
            "--- Quest Diary ---<br>" +
                "The Dragon's Heart: ${mark(q1)}<br>" +
                "The Master Smith: ${mark(q2)}<br>" +
                "The Gem Heist: ${mark(q3)}<br>" +
                "Boss Slayer's Trial: ${mark(q4)}<br>" +
                "The Alchemist's Pact: ${mark(q5)}<br>" +
                rewardLine
        )

        if (completed >= 3 && tier < 1) {
            vars[DiaryVarps.diary] = 1
            invAdd(inv, objs.coins, 250_000)
            invAdd(inv, DiaryObjs.dragon_scimitar)
            mesbox(
                "QUEST DIARY COMPLETE! You receive 250,000 coins and a dragon scimitar. " +
                    "You are a true Hero of the realm!"
            )
            return
        }

        if (completed >= 5 && tier < 2) {
            vars[DiaryVarps.diary] = 2
            invAdd(inv, objs.coins, 500_000)
            invAdd(inv, DiaryObjs.super_combat, 5)
            mesbox(
                "QUEST DIARY VETERAN COMPLETE! You receive 500,000 coins and 5 super combat potions. " +
                    "The realm knows your name now."
            )
        }
    }
}
