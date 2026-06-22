package org.rsmod.content.custom.mikeshop

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.resyncVar
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.game.type.varp.VarpType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object QuestUnlockVarps : VarpReferences() {
    val qpTotal = find("qp_total")
    val qpTotal2 = find("qp_total2")
    val qpTotal3 = find("qp_total3")

    // Spellbook-gating quests: de client greyt spells (bv. Vengeance) tot de quest-progress-varp op
    // z'n endstate (voltooid) staat. Endstate-waarden uit de quest-dbtable gehaald.
    val lunarDiplomacy = find("lunar_quest") // endstate 190 -> lunar spells incl. Vengeance
    val desertTreasure = find("deserttreasuremain") // endstate 15 -> Ancient Magicks
    val dreamMentor = find("dream_main") // endstate 28 -> hogere lunar combat spells
}

internal object QuestUnlockVarBits : VarBitReferences() {
    val qpMax = find("qp_max")
    val questsCompletedCount = find("quests_completed_count")
    val subquestsCompletedCount = find("subquests_completed_count")
    val miniquestsCompletedCount = find("miniquests_completed_count")

    val arceuusSpellbookUnlocked = find("arceuus_spellbook_unlocked")
    val edgevilleSpawnUnlocked = find("edgeville_spawn_unlocked")
    val wildernessSpawnUnlocked = find("wilderness_spawn_unlocked")
    val lostTribeQuest = find("lost_tribe_quest")
    val knightWavesState = find("kr_knightwaves_state")

    val diaryCompletions =
        listOf(
            find("ardougne_diary_easy_complete"),
            find("ardougne_diary_medium_complete"),
            find("ardougne_diary_hard_complete"),
            find("ardougne_diary_elite_complete"),
            find("falador_diary_easy_complete"),
            find("falador_diary_medium_complete"),
            find("falador_diary_hard_complete"),
            find("falador_diary_elite_complete"),
            find("wilderness_diary_easy_complete"),
            find("wilderness_diary_medium_complete"),
            find("wilderness_diary_hard_complete"),
            find("wilderness_diary_elite_complete"),
            find("wilderness_diary_any_complete"),
            find("western_diary_easy_complete"),
            find("western_diary_medium_complete"),
            find("western_diary_hard_complete"),
            find("western_diary_elite_complete"),
            find("kandarin_diary_easy_complete"),
            find("kandarin_diary_medium_complete"),
            find("kandarin_diary_hard_complete"),
            find("kandarin_diary_elite_complete"),
            find("varrock_diary_easy_complete"),
            find("varrock_diary_medium_complete"),
            find("varrock_diary_hard_complete"),
            find("varrock_diary_elite_complete"),
            find("desert_diary_easy_complete"),
            find("desert_diary_medium_complete"),
            find("desert_diary_hard_complete"),
            find("desert_diary_elite_complete"),
            find("morytania_diary_easy_complete"),
            find("morytania_diary_medium_complete"),
            find("morytania_diary_hard_complete"),
            find("morytania_diary_elite_complete"),
            find("fremennik_diary_easy_complete"),
            find("fremennik_diary_medium_complete"),
            find("fremennik_diary_hard_complete"),
            find("fremennik_diary_elite_complete"),
            find("lumbridge_diary_easy_complete"),
            find("lumbridge_diary_medium_complete"),
            find("lumbridge_diary_hard_complete"),
            find("lumbridge_diary_elite_complete"),
            find("karamja_diary_elite_complete"),
            find("kourend_diary_easy_complete"),
            find("kourend_diary_medium_complete"),
            find("kourend_diary_hard_complete"),
            find("kourend_diary_elite_complete"),
        )

    val slayerUnlocks =
        listOf(
            find("slayer_unlock_fossilwyvernblock"),
            find("slayer_unlock_reddragons"),
            find("slayer_unlock_mithrildragons"),
            find("slayer_unlock_aviansies"),
            find("slayer_unlock_notedmithrilbars"),
            find("slayer_unlock_tzhaar"),
            find("slayer_unlock_bosses"),
            find("slayer_unlock_lizardmen"),
            find("slayer_unlock_helm_black"),
            find("slayer_unlock_helm_green"),
            find("slayer_unlock_helm_red"),
            find("slayer_unlock_superiormobs"),
            find("slayer_unlock_helm_purple"),
            find("slayer_unlock_helm_turquoise"),
            find("slayer_unlock_grotesquekills"),
            find("slayer_unlock_helm_hydra"),
            find("slayer_unlock_basilisk"),
            find("slayer_unlock_helm_twisted"),
            find("slayer_unlock_vampyres"),
            find("slayer_unlock_helm_araxyte"),
            find("slayer_unlock_storage"),
            find("slayer_unlock_wildy_extratasks"),
            find("slayer_unlock_warped_creatures"),
        )
}

class QuestUnlocks : PluginScript() {
    override fun ScriptContext.startup() {
        // Quest-requirement van spells (Vengeance/Ancients/lunar) weghalen: bij elke login de
        // spellbook-gating quests op voltooid zetten, zodat de client die spells niet meer greyt -
        // ook zonder ::maxgear.
        onPlayerLogin { player.unlockSpellbookQuests() }

        onCommand("unlockquests") {
            desc = "Unlock custom quests, quest points, diaries, spellbook gates and slayer gates"
            cheat {
                player.unlockQuestProgress()
                player.mes("Quest progress unlocked: max quest points, custom quests, diaries and gates.")
            }
        }

        onCommand("qpdebug") {
            desc = "(hidden)"
            cheat {
                player.mes("--- QP debug (opgeslagen waarden) ---")
                player.mes("qp_total=${player.vars[QuestUnlockVarps.qpTotal]} " +
                    "qp_total2=${player.vars[QuestUnlockVarps.qpTotal2]} " +
                    "qp_total3=${player.vars[QuestUnlockVarps.qpTotal3]}")
                player.mes("qp_max(varbit)=${player.vars[QuestUnlockVarBits.qpMax]} " +
                    "quests_completed_count=${player.vars[QuestUnlockVarBits.questsCompletedCount]}")
            }
        }
    }
}

internal fun Player.unlockQuestProgress() {
    setVarp(QuestUnlockVarps.qpTotal, MAX_QUEST_POINTS)
    setVarp(QuestUnlockVarps.qpTotal2, MAX_QUEST_POINTS)
    setVarp(QuestUnlockVarps.qpTotal3, MAX_QUEST_POINTS)

    setVarBitClamped(QuestUnlockVarBits.qpMax, MAX_QUEST_POINTS)
    setVarBitClamped(QuestUnlockVarBits.questsCompletedCount, MAX_COMPLETED_QUESTS)
    setVarBitClamped(QuestUnlockVarBits.subquestsCompletedCount, MAX_COMPLETED_SUBQUESTS)
    setVarBitClamped(QuestUnlockVarBits.miniquestsCompletedCount, MAX_COMPLETED_MINIQUESTS)

    setVarBit(QuestUnlockVarBits.arceuusSpellbookUnlocked, 1)
    setVarBit(QuestUnlockVarBits.edgevilleSpawnUnlocked, 1)
    setVarBit(QuestUnlockVarBits.wildernessSpawnUnlocked, 1)
    setVarBit(QuestUnlockVarBits.lostTribeQuest, 1)
    setVarBit(QuestUnlockVarBits.knightWavesState, 8)

    setCustomQuestVarps()
    unlockSpellbookQuests()
    for (diaryBit in QuestUnlockVarBits.diaryCompletions) {
        setVarBit(diaryBit, 1)
    }
    for (unlock in QuestUnlockVarBits.slayerUnlocks) {
        setVarBit(unlock, 1)
    }

    PvmProgress.setQuestsDone(this, CUSTOM_QUEST_COMPLETIONS)
}

/**
 * Zet de spellbook-gating quests op "voltooid" zodat de client de bijbehorende spells niet meer
 * greyt: Vengeance (Lunar Diplomacy), Ancient Magicks (Desert Treasure) en de hogere lunar combat
 * spells (Dream Mentor). Endstate-waarden komen uit de quest-dbtable.
 */
internal fun Player.unlockSpellbookQuests() {
    setVarp(QuestUnlockVarps.lunarDiplomacy, 190)
    setVarp(QuestUnlockVarps.desertTreasure, 15)
    setVarp(QuestUnlockVarps.dreamMentor, 28)
}

private fun Player.setCustomQuestVarps() {
    setVarp(QuestVarps.dragonsheart, 3)
    setVarp(QuestVarps.smithmaster, 3)
    setVarp(QuestVarps.gemheist, 2)
    setVarp(QuestVarps.bossslayer, 3)
    setVarp(QuestVarps.alchemistpact, 3)
}

private fun Player.setVarp(varp: VarpType, value: Int) {
    VarPlayerIntMapSetter.set(this, varp, value)
    resyncVar(varp)
}

private fun Player.setVarBit(varbit: VarBitType, value: Int) {
    VarPlayerIntMapSetter.set(this, varbit, value)
    resyncVar(varbit)
}

private fun Player.setVarBitClamped(varbit: VarBitType, requestedValue: Int) {
    var value = requestedValue
    while (value > 0) {
        try {
            setVarBit(varbit, value)
            return
        } catch (_: IllegalArgumentException) {
            value /= 2
        }
    }
    setVarBit(varbit, 0)
}

private const val MAX_QUEST_POINTS = 320
// "Quests Completed: X/320" in het character-tabblad leest quests_completed_count als X en
// qp_max als noemer. Gelijk aan 320 zodat de teller vol (320/320) staat na ::maxgear/::unlockquests.
private const val MAX_COMPLETED_QUESTS = 320
private const val MAX_COMPLETED_SUBQUESTS = 20
private const val MAX_COMPLETED_MINIQUESTS = 30
private const val CUSTOM_QUEST_COMPLETIONS = 10
