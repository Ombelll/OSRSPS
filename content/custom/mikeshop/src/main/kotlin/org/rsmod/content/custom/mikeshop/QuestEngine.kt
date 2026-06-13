package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Persistente quest-state-varp (Perm-scope = saved naar game.db). */
internal object QuestVarpBuilder : VarpBuilder() {
    init {
        build("mikeq_dragonsheart")
        build("mikeq_smithmaster")
        build("mikeq_gemheist")
        build("mikeq_bossslayer")
        build("mikeq_alchemistpact")
    }
}

internal object QuestVarps : VarpReferences() {
    val dragonsheart = find("mikeq_dragonsheart")
    val smithmaster = find("mikeq_smithmaster")
    val gemheist = find("mikeq_gemheist")
    val bossslayer = find("mikeq_bossslayer")
    val alchemistpact = find("mikeq_alchemistpact")
}

internal object QuestEngineObjs : ObjReferences() {
    val dragon_bones = find("dragon_bones")
    val bronze_bar = find("bronze_bar")
    val runite_bar = find("runite_bar")
    val diamond = find("diamond")
    val rune_platebody = find("rune_platebody")
    val snapdragon = find("snapdragon")
    val super_combat = find("4dose2combat")
}

internal object QuestLogInterfaces : InterfaceReferences() {
    val questjournal = find("questjournal")
}

internal object QuestLogComponents : ComponentReferences() {
    val title = find("questjournal:title")
    val close = find("questjournal:close")
    val lines = (1..200).map { find("questjournal:qj$it") }
}

/**
 * ECHTE QUEST-ENGINE (persistent, multi-stage).
 *
 * Quest-voortgang staat in een varp (`mikeq_dragonsheart`, stage 0..3) die persistent is en
 * meegaat over uitloggen heen. De dialoog vertakt op basis van de huidige stage.
 *
 *  - ::dragonsheart : speelt de quest af op je huidige stage.
 *  - ::questlog     : toont je voortgang.
 *
 * Stages: 0 = niet gestart, 1 = geaccepteerd, 2 = botten geleverd, 3 = voltooid.
 */
class QuestEngine @Inject constructor(private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("dragonsheart") {
            desc = "Quest: The Dragon's Heart (multi-stage, persistent)"
            cheat { protectedAccess.launch(player) { runDragonsHeart() } }
        }
        onCommand("smithmaster") {
            desc = "Quest: The Master Smith (multi-stage, persistent)"
            cheat { protectedAccess.launch(player) { runSmithMaster() } }
        }
        onCommand("gemheist") {
            desc = "Quest: The Gem Heist (multi-stage, persistent)"
            cheat { protectedAccess.launch(player) { runGemHeist() } }
        }
        onCommand("bossslayer") {
            desc = "Quest: Boss Slayer's Trial (boss-kill stages, persistent)"
            cheat { protectedAccess.launch(player) { runBossSlayer() } }
        }
        onCommand("alchemistpact") {
            desc = "Quest: The Alchemist's Pact (multi-stage, persistent)"
            cheat { protectedAccess.launch(player) { runAlchemistPact() } }
        }
        onCommand("questlog") {
            desc = "View your quest journal"
            cheat { protectedAccess.launch(player) { showLog() } }
        }
        onIfModalButton(QuestLogComponents.close) { ifClose() }
    }

    private fun status(stage: Int, maxStage: Int): String =
        when {
            stage <= 0 -> "Not started"
            stage >= maxStage -> "COMPLETE"
            else -> "In progress (stage $stage of $maxStage)"
        }

    private fun questLine(name: String, stage: Int, maxStage: Int): String {
        val colour =
            when {
                stage <= 0 -> "990000"
                stage >= maxStage -> "008000"
                else -> "b06b00"
            }
        return "<col=$colour>$name</col> - ${status(stage, maxStage)}"
    }

    private fun ProtectedAccess.showLog() {
        val lines =
            listOf(
                "<col=000080>Mike's Quest Journal</col>",
                "",
                questLine("The Dragon's Heart", vars[QuestVarps.dragonsheart], 3),
                questLine("The Master Smith", vars[QuestVarps.smithmaster], 3),
                questLine("The Gem Heist", vars[QuestVarps.gemheist], 2),
                questLine("Boss Slayer's Trial", vars[QuestVarps.bossslayer], 3),
                questLine("The Alchemist's Pact", vars[QuestVarps.alchemistpact], 3),
                "",
                "<col=555555>Commands:</col>",
                "::dragonsheart",
                "::smithmaster",
                "::gemheist",
                "::bossslayer",
                "::alchemistpact",
            )

        ifOpenMainModal(QuestLogInterfaces.questjournal)
        ifSetText(QuestLogComponents.title, "Quest Journal")
        QuestLogComponents.lines.forEachIndexed { index, component ->
            ifSetText(component, lines.getOrNull(index) ?: "")
        }
        ifSetEvents(QuestLogComponents.close, 0..0, IfEvent.Op1)
    }

    private suspend fun ProtectedAccess.runDragonsHeart() {
        when (vars[QuestVarps.dragonsheart]) {
            0 -> {
                mesbox("The Dragon Sage says: 'A dragon stole my heartstone! Will you help me reclaim it?'")
                val accept =
                    choice2("Yes, I'll help.", true, "Not now.", false, title = "The Dragon's Heart")
                if (accept) {
                    vars[QuestVarps.dragonsheart] = 1
                    mesbox("'Wonderful! First, bring me 5 dragon bones to forge a blade.'")
                } else {
                    mesbox("'Come back when you're ready, adventurer.'")
                }
            }
            1 -> {
                if (invTotal(inv, QuestEngineObjs.dragon_bones) >= 5) {
                    invDel(inv, QuestEngineObjs.dragon_bones, 5)
                    vars[QuestVarps.dragonsheart] = 2
                    mesbox("'Perfect! Now slay a mighty dragon and bring me its heart - a Lucky Cabbage.'")
                } else {
                    mesbox("'Bring me 5 dragon bones. The wyrm-bosses (::dragonboss) drop them.'")
                }
            }
            2 -> {
                if (invTotal(inv, objs.cabbage) >= 1) {
                    invDel(inv, objs.cabbage, 1)
                    invAdd(inv, objs.coins, 100_000)
                    vars[QuestVarps.dragonsheart] = 3
                    mesbox(
                        "'The heartstone, at last! You've saved us. Take 100,000 coins. " +
                            "THE DRAGON'S HEART - COMPLETE!'"
                    )
                } else {
                    mesbox("'I still need the dragon's heart - a Lucky Cabbage from a boss. Keep hunting!'")
                }
            }
            else -> {
                mesbox("'Thank you again, hero. The Dragon's Heart is safe thanks to you.'")
            }
        }
    }

    private suspend fun ProtectedAccess.runSmithMaster() {
        when (vars[QuestVarps.smithmaster]) {
            0 -> {
                mesbox("The Master Smith says: 'I need a worthy apprentice. Will you prove yourself?'")
                val accept =
                    choice2("Yes, I will.", true, "Not now.", false, title = "The Master Smith")
                if (accept) {
                    vars[QuestVarps.smithmaster] = 1
                    mesbox("'Good. First, smith and bring me 10 bronze bars.'")
                } else {
                    mesbox("'Hmph. Come back when you have the will.'")
                }
            }
            1 -> {
                if (invTotal(inv, QuestEngineObjs.bronze_bar) >= 10) {
                    invDel(inv, QuestEngineObjs.bronze_bar, 10)
                    vars[QuestVarps.smithmaster] = 2
                    mesbox("'Solid work! Now the real test: bring me 5 runite bars.'")
                } else {
                    mesbox("'Bring me 10 bronze bars. Smelt copper + tin at a furnace.'")
                }
            }
            2 -> {
                if (invTotal(inv, QuestEngineObjs.runite_bar) >= 5) {
                    invDel(inv, QuestEngineObjs.runite_bar, 5)
                    invAdd(inv, QuestEngineObjs.rune_platebody)
                    invAdd(inv, objs.coins, 30_000)
                    vars[QuestVarps.smithmaster] = 3
                    mesbox(
                        "'You are a true smith! Take this rune platebody and 30,000 coins. " +
                            "THE MASTER SMITH - COMPLETE!'"
                    )
                } else {
                    mesbox("'I still need 5 runite bars. Mine runite and smelt it with coal.'")
                }
            }
            else -> mesbox("'Welcome back, master smith. Our forge thanks you.'")
        }
    }

    private suspend fun ProtectedAccess.runGemHeist() {
        when (vars[QuestVarps.gemheist]) {
            0 -> {
                mesbox("The Gem Collector whispers: 'I need 3 flawless diamonds... discreetly. Interested?'")
                val accept =
                    choice2("I'm in.", true, "No thanks.", false, title = "The Gem Heist")
                if (accept) {
                    vars[QuestVarps.gemheist] = 1
                    mesbox("'Excellent. Cut me 3 diamonds and bring them here.'")
                } else {
                    mesbox("'Your loss. The offer stands.'")
                }
            }
            1 -> {
                if (invTotal(inv, QuestEngineObjs.diamond) >= 3) {
                    invDel(inv, QuestEngineObjs.diamond, 3)
                    invAdd(inv, objs.coins, 75_000)
                    vars[QuestVarps.gemheist] = 2
                    mesbox("'Flawless! Here's 75,000 coins. THE GEM HEIST - COMPLETE!'")
                } else {
                    mesbox("'Bring me 3 cut diamonds. Mine the gems, then cut them with a chisel.'")
                }
            }
            else -> mesbox("'Pleasure doing business, friend.'")
        }
    }

    private suspend fun ProtectedAccess.runBossSlayer() {
        val kills = PvmProgress.bossKills(player)
        when (vars[QuestVarps.bossslayer]) {
            0 -> {
                mesbox("The Slayer Captain says: 'Your name is new to me. Prove it in blood and steel.'")
                val accept =
                    choice2("Give me the trial.", true, "Not now.", false, title = "Boss Slayer's Trial")
                if (accept) {
                    vars[QuestVarps.bossslayer] = 1
                    mesbox("'First mark: defeat 3 bosses. Your current boss kill count is $kills.'")
                } else {
                    mesbox("'Then keep your blade sheathed until you're ready.'")
                }
            }
            1 -> {
                if (kills >= 3) {
                    vars[QuestVarps.bossslayer] = 2
                    invAdd(inv, objs.coins, 50_000)
                    mesbox("'Three marks confirmed. Take 50,000 coins. Final mark: reach 10 boss kills.'")
                } else {
                    mesbox("'You have $kills / 3 boss kills. Try ::mikeboss, ::demonboss, or ::dragonboss.'")
                }
            }
            2 -> {
                if (kills >= 10) {
                    vars[QuestVarps.bossslayer] = 3
                    invAdd(inv, objs.coins, 150_000)
                    PvmProgress.recordQuest(player)
                    mesbox(
                        "'Ten marks. You are no pretender. Take 150,000 coins. " +
                            "BOSS SLAYER'S TRIAL - COMPLETE!'"
                    )
                } else {
                    mesbox("'You have $kills / 10 boss kills. Return when your kill count reaches 10.'")
                }
            }
            else -> mesbox("'Boss Slayer. The title suits you.'")
        }
    }

    private suspend fun ProtectedAccess.runAlchemistPact() {
        when (vars[QuestVarps.alchemistpact]) {
            0 -> {
                mesbox("The Battle Alchemist says: 'A good warrior wins before the first swing. Care to learn?'")
                val accept =
                    choice2("Teach me.", true, "Not now.", false, title = "The Alchemist's Pact")
                if (accept) {
                    vars[QuestVarps.alchemistpact] = 1
                    mesbox("'Bring me 3 snapdragons. Potency begins with clean ingredients.'")
                } else {
                    mesbox("'Then return when your flask is empty and your curiosity is full.'")
                }
            }
            1 -> {
                if (invTotal(inv, QuestEngineObjs.snapdragon) >= 3) {
                    invDel(inv, QuestEngineObjs.snapdragon, 3)
                    vars[QuestVarps.alchemistpact] = 2
                    mesbox("'Fine herbs. Now bring me a 4-dose super combat potion for the final binding.'")
                } else {
                    mesbox("'I need 3 snapdragons. Try skilling supplies, farming, or monster drops.'")
                }
            }
            2 -> {
                if (invTotal(inv, QuestEngineObjs.super_combat) >= 1) {
                    invDel(inv, QuestEngineObjs.super_combat, 1)
                    invAdd(inv, QuestEngineObjs.super_combat, 3)
                    invAdd(inv, objs.coins, 80_000)
                    vars[QuestVarps.alchemistpact] = 3
                    PvmProgress.recordQuest(player)
                    mesbox(
                        "'Pact sealed. Take three refined super combats and 80,000 coins. " +
                            "THE ALCHEMIST'S PACT - COMPLETE!'"
                    )
                } else {
                    mesbox("'Bring me one 4-dose super combat potion. ::potionshop can help.'")
                }
            }
            else -> mesbox("'The pact holds. Keep your potions stocked and your blade steady.'")
        }
    }
}
