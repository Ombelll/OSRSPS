package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * ::help-overzicht van alle custom commando's (discoverability).
 *
 * De login-MOTD + hub-teleport zit in MainHub.kt (een enkele onPlayerLogin-handler).
 */
class WelcomeMessage @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("help") {
            desc = "List all custom commands"
            cheat {
                player.mes("--- Mike's RSPS commands ---")
                player.mes("Hub: ::hub (clerk = shops, banker = bank, skilling-zone)")
                player.mes("Gear/test: ::maxgear, ::heal, ::starter, ::train <skill> [xp]")
                player.mes("Shops/bank: ::mikeshop, ::supplyshop, ::potionshop, ::store, ::bank")
                player.mes("Skilling: ::skillkit, ::skillmats, ::skillzone")
                player.mes("Bosses: ::bosses (lists ::mikeboss..::godboss)")
                player.mes("Quests: ::quests, ::questlog, ::dragonsheart, ::smithmaster, ::gemheist")
                player.mes("Quests+: ::bossslayer, ::alchemistpact")
                player.mes("Minigames: ::arena (wave survival), ::dice, ::flip, ::slots, ::mystery")
                player.mes("Hub NPCs: Shop Clerk, Banker, Teleport Wizard (talk to them)")
                player.mes("Travel: ::teleport, ::home, ::varrock, ::falador, ::ge, ::mine")
                player.mes("Progress: ::achievements, ::slayerpoints, ::slayershop")
                player.mes("PvP (W2): ::pkshop, ::pkpoints, ::pkspend - kills geven PK-punten!")
                player.mes("Social: ::online, ::pm, ::reply, ::friendadd, ::friends, ::clancreate")
            }
        }
    }
}
