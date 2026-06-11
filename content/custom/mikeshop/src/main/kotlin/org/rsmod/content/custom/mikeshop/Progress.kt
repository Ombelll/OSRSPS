package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Gedeelde, sessie-gebonden progressie-state. Boss-death-hooks en quests schrijven hierheen;
 * `::achievements` leest het. (Eén singleton zodat alle scripts dezelfde teller delen.)
 */
object PvmProgress {
    private val bossKills = HashMap<Player, Int>()
    private val questsDone = HashMap<Player, Int>()

    /** Registreer een boss-kill; geeft het nieuwe totaal terug. */
    fun recordBossKill(player: Player): Int {
        val kc = (bossKills[player] ?: 0) + 1
        bossKills[player] = kc
        return kc
    }

    fun recordQuest(player: Player) {
        questsDone[player] = (questsDone[player] ?: 0) + 1
    }

    fun bossKills(player: Player): Int = bossKills[player] ?: 0

    fun questsDone(player: Player): Int = questsDone[player] ?: 0
}

/** ::achievements -> toont je progressie (boss-kills + quests + volgende mijlpaal). */
class Achievements @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("achievements") {
            desc = "View your progression (boss kills, quests, milestones)"
            cheat {
                val kc = PvmProgress.bossKills(player)
                val next = ((kc / 10) + 1) * 10
                player.mes("--- Your Progression ---")
                player.mes("Boss kills: $kc (next milestone at $next -> +100k bonus)")
                player.mes("Quests completed: ${PvmProgress.questsDone(player)} / 6")
            }
        }
    }
}
