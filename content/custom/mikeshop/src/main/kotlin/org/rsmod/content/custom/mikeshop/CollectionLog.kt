package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onCommand
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CollectionLog @Inject constructor(private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("collection") {
            desc = "Open your custom collection log"
            cheat { protectedAccess.launch(player) { showCollectionLog() } }
        }
    }

    private fun doneLine(label: String, done: Boolean): String {
        val colour = if (done) "008000" else "990000"
        val status = if (done) "Done" else "Missing"
        return "<col=$colour>$label</col> - $status"
    }

    private fun progressLine(label: String, value: Int, target: Int): String {
        val done = value >= target
        val colour = if (done) "008000" else "b06b00"
        val shown = value.coerceAtMost(target)
        return "<col=$colour>$label</col> - $shown / $target"
    }

    private fun ProtectedAccess.questDone(quest: String): Boolean =
        when (quest) {
            "dragonsheart" -> vars[QuestVarps.dragonsheart] >= 3
            "smithmaster" -> vars[QuestVarps.smithmaster] >= 3
            "gemheist" -> vars[QuestVarps.gemheist] >= 2
            "bossslayer" -> vars[QuestVarps.bossslayer] >= 3
            "alchemistpact" -> vars[QuestVarps.alchemistpact] >= 3
            else -> false
        }

    private fun ProtectedAccess.showCollectionLog() {
        val arenaBest = vars[varps.mike_arena_best_wave]
        val bossKills = PvmProgress.bossKills(player)
        val pkKills = vars[varps.mike_pk_kills]
        val pkPoints = vars[varps.mike_pk_points]
        val questCount =
            listOf(
                    questDone("dragonsheart"),
                    questDone("smithmaster"),
                    questDone("gemheist"),
                    questDone("bossslayer"),
                    questDone("alchemistpact"),
                )
                .count { it }

        val lines =
            buildList {
                add("<col=000080>Mike's Collection Log</col>")
                add("")
                add("<col=555555>Minigames</col>")
                add(progressLine("Combat Arena best wave", arenaBest, 10))
                add(doneLine("Arena Champion", arenaBest >= 10))
                add(doneLine("Fire cape in inventory", invTotal(inv, objs.fire_cape) > 0))
                add("")
                add("<col=555555>Quests</col>")
                add(progressLine("Quest completions", questCount, 5))
                add(doneLine("The Dragon's Heart", questDone("dragonsheart")))
                add(doneLine("The Master Smith", questDone("smithmaster")))
                add(doneLine("The Gem Heist", questDone("gemheist")))
                add(doneLine("Boss Slayer's Trial", questDone("bossslayer")))
                add(doneLine("The Alchemist's Pact", questDone("alchemistpact")))
                add(doneLine("Quest Diary adventurer tier", vars[DiaryVarps.diary] >= 1))
                add(doneLine("Quest Diary veteran tier", vars[DiaryVarps.diary] >= 2))
                add("")
                add("<col=555555>PvM</col>")
                add(progressLine("Boss kill milestone", bossKills, 10))
                add(progressLine("Boss kill veteran", bossKills, 50))
                add("")
                add("<col=555555>PvP</col>")
                add(progressLine("PK kills", pkKills, 10))
                add(progressLine("PK point stack", pkPoints, 200))
                add("")
                add("<col=555555>Commands:</col>")
                add("::arena, ::arenatop, ::fightcaves")
                add("::questlog, ::diary, ::pkpoints")
            }

        ifOpenMainModal(QuestLogInterfaces.questjournal)
        ifSetText(QuestLogComponents.title, "Collection Log")
        QuestLogComponents.lines.forEachIndexed { index, component ->
            ifSetText(component, lines.getOrNull(index) ?: "")
        }
        ifSetEvents(QuestLogComponents.close, 0..0, IfEvent.Op1)
    }
}
