package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onCommand
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Keybinds (F-keys). De client leest de stone_*_key-varbits om toetsen aan de gameframe-tabs te
 * binden, maar niks zette ze server-side. `::keybinds` zet de standaard F1-F12-mapping; `::keybinds
 * clear` zet alles terug op None. De waarde is de keybind-index (best-effort: 1=F1 ... 12=F12).
 */
internal object KeybindVarBits : VarBitReferences() {
    val combat = find("stone_combat_key")
    val stats = find("stone_stats_key")
    val journal = find("stone_journal_key")
    val inv = find("stone_inv_key")
    val worn = find("stone_worn_key")
    val prayer = find("stone_prayer_key")
    val magic = find("stone_magic_key")
    val clanchat = find("stone_clanchat_key")
    val friends = find("stone_friends_key")
    val options = find("stone_options1_key")
    val music = find("stone_music_key")
    val logout = find("stone_logout_key")
}

class KeybindCommands @Inject constructor() : PluginScript() {
    private var Player.combatKey by intVarBit(KeybindVarBits.combat)
    private var Player.statsKey by intVarBit(KeybindVarBits.stats)
    private var Player.journalKey by intVarBit(KeybindVarBits.journal)
    private var Player.invKey by intVarBit(KeybindVarBits.inv)
    private var Player.wornKey by intVarBit(KeybindVarBits.worn)
    private var Player.prayerKey by intVarBit(KeybindVarBits.prayer)
    private var Player.magicKey by intVarBit(KeybindVarBits.magic)
    private var Player.clanchatKey by intVarBit(KeybindVarBits.clanchat)
    private var Player.friendsKey by intVarBit(KeybindVarBits.friends)
    private var Player.optionsKey by intVarBit(KeybindVarBits.options)
    private var Player.musicKey by intVarBit(KeybindVarBits.music)
    private var Player.logoutKey by intVarBit(KeybindVarBits.logout)

    override fun ScriptContext.startup() {
        onCommand("keybinds") {
            desc = "Set default F-key tab keybinds (F1=combat ... F12=logout). ::keybinds clear to reset."
            cheat {
                if (args.firstOrNull()?.equals("clear", ignoreCase = true) == true) {
                    player.applyKeybinds(0)
                    player.mes("Keybinds gewist (alles op None).")
                } else {
                    player.applyDefaults()
                    player.mes("Standaard F-key keybinds gezet: F1=combat, F2=skills, F3=quests,")
                    player.mes("F4=inventory, F5=worn, F6=prayer, F7=magic, F8=clan, F9=friends,")
                    player.mes("F10=settings, F11=music, F12=logout. Test met F1 t/m F12.")
                }
            }
        }

        onCommand("keybind") {
            desc = "Bind 1 tab aan een F-toets: ::keybind <tab> <Fnummer 1-12, 0=None>"
            cheat {
                val tab = args.getOrNull(0)?.lowercase()
                val num = args.getOrNull(1)?.toIntOrNull()
                if (tab == null || num == null || num !in 0..12) {
                    player.mes("Gebruik: ::keybind <tab> <0-12>. Tabs: combat, skills, quests,")
                    player.mes("inventory, worn, prayer, magic, clan, friends, settings, music, logout.")
                    return@cheat
                }
                if (player.setKeybind(tab, num)) {
                    val key = if (num == 0) "None" else "F$num"
                    player.mes("Keybind gezet: $tab -> $key.")
                } else {
                    player.mes("Onbekende tab '$tab'. Geldig: combat, skills, quests, inventory,")
                    player.mes("worn, prayer, magic, clan, friends, settings, music, logout.")
                }
            }
        }
    }

    private fun Player.setKeybind(tab: String, value: Int): Boolean {
        when (tab) {
            "combat", "attack" -> combatKey = value
            "skills", "stats" -> statsKey = value
            "quests", "journal", "char" -> journalKey = value
            "inventory", "inv" -> invKey = value
            "worn", "equipment", "equip" -> wornKey = value
            "prayer", "pray" -> prayerKey = value
            "magic", "spellbook", "spells" -> magicKey = value
            "clan", "clanchat" -> clanchatKey = value
            "friends", "friend" -> friendsKey = value
            "settings", "options", "config" -> optionsKey = value
            "music" -> musicKey = value
            "logout" -> logoutKey = value
            else -> return false
        }
        return true
    }

    private fun Player.applyDefaults() {
        combatKey = 1
        statsKey = 2
        journalKey = 3
        invKey = 4
        wornKey = 5
        prayerKey = 6
        magicKey = 7
        clanchatKey = 8
        friendsKey = 9
        optionsKey = 10
        musicKey = 11
        logoutKey = 12
    }

    private fun Player.applyKeybinds(value: Int) {
        combatKey = value
        statsKey = value
        journalKey = value
        invKey = value
        wornKey = value
        prayerKey = value
        magicKey = value
        clanchatKey = value
        friendsKey = value
        optionsKey = value
        musicKey = value
        logoutKey = value
    }
}
