package org.rsmod.content.custom.mikeshop

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Inject
import org.rsmod.api.config.refs.varps
import org.rsmod.api.db.Database
import org.rsmod.api.parsers.jackson.readReifiedValue
import org.rsmod.api.parsers.json.Json
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onCommand
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private data class ArenaScore(val displayName: String, val wave: Int)

class ArenaHiscores
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val database: Database,
    @Json private val objectMapper: ObjectMapper,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("arenatop") {
            desc = "Show the Combat Arena best-wave hiscores"
            cheat { protectedAccess.launch(player) { showArenaTop() } }
        }
    }

    private suspend fun ProtectedAccess.showArenaTop() {
        val scores = loadArenaScores(player.displayName, vars[varps.mike_arena_best_wave])
        val lines =
            buildList {
                add("<col=000080>Combat Arena Hiscores</col>")
                add("")
                if (scores.isEmpty()) {
                    add("No saved Arena records yet.")
                    add("Start with ::arena and clear a wave.")
                } else {
                    scores.take(20).forEachIndexed { index, score ->
                        add("${index + 1}. ${score.displayName} - wave ${score.wave}/10")
                    }
                }
                add("")
                add("<col=555555>Commands:</col>")
                add("::arena")
                add("::arenastats")
                add("::arenatop")
            }

        ifOpenMainModal(QuestLogInterfaces.questjournal)
        ifSetText(QuestLogComponents.title, "Arena Hiscores")
        QuestLogComponents.lines.forEachIndexed { index, component ->
            ifSetText(component, lines.getOrNull(index) ?: "")
        }
        ifSetEvents(QuestLogComponents.close, 0..0, IfEvent.Op1)
    }

    private suspend fun loadArenaScores(currentName: String, currentWave: Int): List<ArenaScore> {
        val saved =
            database.withTransaction { connection ->
                val statement =
                    connection.prepareStatement(
                        """
                            SELECT COALESCE(a.display_name, a.login_username) AS display_name, c.varps
                            FROM characters c
                            JOIN accounts a ON a.id = c.account_id
                        """
                            .trimIndent()
                    )
                statement.use {
                    val rows = mutableListOf<ArenaScore>()
                    val results = it.executeQuery()
                    results.use {
                        while (results.next()) {
                            val varpText = results.getString("varps") ?: "{}"
                            val savedWave = arenaBestWave(varpText)
                            if (savedWave > 0) {
                                rows += ArenaScore(results.getString("display_name"), savedWave)
                            }
                        }
                    }
                    rows
                }
            }

        val merged = saved.filterNot { it.displayName.equals(currentName, ignoreCase = true) }
        val current = if (currentWave > 0) listOf(ArenaScore(currentName, currentWave)) else emptyList()
        return (merged + current).sortedWith(compareByDescending<ArenaScore> { it.wave }.thenBy { it.displayName })
    }

    private fun arenaBestWave(varpText: String): Int {
        val saved = objectMapper.readReifiedValue<Map<Int, Int>>(varpText)
        return saved[varps.mike_arena_best_wave.id] ?: 0
    }
}
