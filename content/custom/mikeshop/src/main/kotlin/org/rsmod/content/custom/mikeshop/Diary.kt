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
}

/**
 * ACHIEVEMENT DIARY (persistent, Phase 6).
 *
 * `::diary` toont je voortgang over de 3 quests (gelezen uit hun persistente varps). Heb je
 * alle 3 voltooid en de beloning nog niet geclaimd, dan claim je hem direct: 250.000 coins +
 * een dragon scimitar. De claim wordt in een eigen varp bijgehouden zodat het maar 1x kan.
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
        val completed = listOf(q1, q2, q3).count { it }
        val claimed = vars[DiaryVarps.diary] >= 1

        val rewardLine =
            when {
                claimed -> "Reward: already claimed"
                completed == 3 -> "Reward: READY - claiming now!"
                else -> "Reward: $completed / 3 quests done"
            }
        mesbox(
            "--- Quest Diary ---<br>" +
                "The Dragon's Heart: ${mark(q1)}<br>" +
                "The Master Smith: ${mark(q2)}<br>" +
                "The Gem Heist: ${mark(q3)}<br>" +
                rewardLine
        )

        if (completed == 3 && !claimed) {
            vars[DiaryVarps.diary] = 1
            invAdd(inv, objs.coins, 250_000)
            invAdd(inv, DiaryObjs.dragon_scimitar)
            mesbox(
                "QUEST DIARY COMPLETE! You receive 250,000 coins and a dragon scimitar. " +
                    "You are a true Hero of the realm!"
            )
        }
    }
}
