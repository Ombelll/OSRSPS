package org.rsmod.content.custom.mikeshop

import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.resyncVar
import org.rsmod.api.script.onCommand
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PrayerUnlocks : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("unlockprayers") {
            desc = "Unlock scroll/quest gated prayers and switch to Ancient Magicks"
            cheat {
                player.unlockAllPrayers()
                player.enableAncientMagicks()
                player.mes("All prayer unlocks enabled. Ancient Magicks spellbook activated.")
            }
        }
    }
}

internal fun Player.unlockAllPrayers() {
    VarPlayerIntMapSetter.set(this, varbits.preserve_unlocked, 1)
    VarPlayerIntMapSetter.set(this, varbits.rigour_unlocked, 1)
    VarPlayerIntMapSetter.set(this, varbits.augury_unlocked, 1)
    VarPlayerIntMapSetter.set(this, varbits.prayer_deadeye_unlocked, 1)
    VarPlayerIntMapSetter.set(this, varbits.prayer_mystic_vigour_unlocked, 1)
    VarPlayerIntMapSetter.set(this, varbits.kr_knightwaves_state, 8)

    resyncVar(varbits.preserve_unlocked)
    resyncVar(varbits.rigour_unlocked)
    resyncVar(varbits.augury_unlocked)
    resyncVar(varbits.prayer_deadeye_unlocked)
    resyncVar(varbits.prayer_mystic_vigour_unlocked)
    resyncVar(varbits.kr_knightwaves_state)
}
