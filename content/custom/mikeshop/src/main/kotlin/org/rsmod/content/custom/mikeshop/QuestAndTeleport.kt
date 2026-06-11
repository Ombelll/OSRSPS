package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onCommand
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * ::teleport -> opent een echt keuze-menu (choice5) en teleporteert je naar de gekozen plek.
 */
class TeleportMenu @Inject constructor(private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("teleport") {
            desc = "Open the teleport menu"
            cheat {
                protectedAccess.launch(player) {
                    val dest =
                        choice5(
                            "Lumbridge (home)",
                            CoordGrid(0, 50, 50, 21, 18),
                            "Varrock",
                            CoordGrid(0, 50, 53, 12, 30),
                            "Falador",
                            CoordGrid(0, 46, 52, 21, 50),
                            "Grand Exchange",
                            CoordGrid(0, 49, 54, 28, 30),
                            "Mining site",
                            CoordGrid(0, 50, 49, 28, 12),
                            title = "Where would you like to teleport?",
                        )
                    telejump(dest)
                    mes("You teleport away.")
                }
            }
        }
    }
}

/**
 * MINI-QUEST: "The Cabbage of Power".
 *
 * ::quest -> de oude tovenaar vraagt je Mike's Lucky Cabbage (zeldzame boss-drop) terug te brengen.
 * Heb je 'm in je inventory, dan lever je 'm in voor 50.000 coins + een zegen (Prayer-XP).
 * Volledig stateless: het kijkt simpelweg of je het item bij je hebt.
 */
class CabbageQuest @Inject constructor(private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("quest") {
            desc = "Start the mini-quest: The Cabbage of Power"
            cheat { protectedAccess.launch(player) { runQuest() } }
        }
    }

    private suspend fun ProtectedAccess.runQuest() {
        mesbox("The Old Wizard says: 'A mighty dragon stole my Lucky Cabbage of Power!'")
        val help =
            choice2(
                "I'll find it for you.",
                true,
                "Not right now.",
                false,
                title = "Help the Old Wizard?",
            )
        if (!help) {
            mesbox("'Suit yourself,' the wizard grumbles.")
            return
        }
        if (invTotal(inv, objs.cabbage) >= 1) {
            invDel(inv, objs.cabbage, 1)
            invAdd(inv, objs.coins, 50_000)
            statAdvance(stats.prayer, PlayerStatMap.toFineXP(10_000.0).toDouble())
            PvmProgress.recordQuest(player)
            mesbox(
                "You hand over Mike's Lucky Cabbage. 'Incredible! Take 50,000 coins " +
                    "and my blessing!' QUEST COMPLETE!"
            )
        } else {
            mesbox(
                "'Bring me Mike's Lucky Cabbage. The bosses drop it rarely - try ::godboss. " +
                    "Come back when you have it!'"
            )
        }
    }
}
