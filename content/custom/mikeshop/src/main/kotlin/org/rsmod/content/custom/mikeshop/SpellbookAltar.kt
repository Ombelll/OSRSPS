package org.rsmod.content.custom.mikeshop

import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.resyncVar
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object SpellbookAltarLocs : LocReferences() {
    val occultAltar = find("poh_altar_occult")
}

class SpellbookAltar : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(SpellbookAltarLocs.occultAltar) { openSpellbookMenu() }
        onOpLoc2(SpellbookAltarLocs.occultAltar) { openSpellbookMenu() }

        onCommand("standard") {
            desc = "Switch to the standard spellbook"
            cheat { player.setSpellbook(Spellbook.Standard) }
        }
        onCommand("ancients") {
            desc = "Switch to the Ancient Magicks spellbook"
            cheat { player.setSpellbook(Spellbook.Ancients) }
        }
        onCommand("lunar") {
            desc = "Switch to the Lunar spellbook"
            cheat { player.setSpellbook(Spellbook.Lunars) }
        }
        onCommand("arceuus") {
            desc = "Switch to the Arceuus spellbook"
            cheat { player.setSpellbook(Spellbook.Arceuus) }
        }
    }

    private suspend fun ProtectedAccess.openSpellbookMenu() {
        val pick =
            choice5(
                "Standard",
                Spellbook.Standard.varValue,
                "Ancient Magicks",
                Spellbook.Ancients.varValue,
                "Lunar",
                Spellbook.Lunars.varValue,
                "Arceuus",
                Spellbook.Arceuus.varValue,
                "Cancel",
                -1,
                title = "Choose a spellbook",
            )
        val spellbook = Spellbook[pick] ?: return
        player.setSpellbook(spellbook)
    }
}

internal fun org.rsmod.game.entity.Player.setSpellbook(spellbook: Spellbook) {
    if (spellbook == Spellbook.Arceuus) {
        VarPlayerIntMapSetter.set(this, QuestUnlockVarBits.arceuusSpellbookUnlocked, 1)
        resyncVar(QuestUnlockVarBits.arceuusSpellbookUnlocked)
    }
    VarPlayerIntMapSetter.set(this, varbits.spellbook, spellbook.varValue)
    resyncVar(varbits.spellbook)
    mes("${spellbook.displayName()} spellbook activated.")
}

internal fun org.rsmod.game.entity.Player.enableAncientMagicks() {
    setSpellbook(Spellbook.Ancients)
}

private fun Spellbook.displayName(): String =
    when (this) {
        Spellbook.Standard -> "Standard"
        Spellbook.Ancients -> "Ancient Magicks"
        Spellbook.Lunars -> "Lunar"
        Spellbook.Arceuus -> "Arceuus"
    }
