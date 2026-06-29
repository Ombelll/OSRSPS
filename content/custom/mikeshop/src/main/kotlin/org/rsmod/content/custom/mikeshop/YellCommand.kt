package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.PvpKillTracker
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onCommand
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * ::yell <bericht> - stuurt een bericht naar alle online spelers met je PK-rang ervoor, bv.
 * "[Yell] [Warlord] Mike: ez clap". Globale trash-talk/chat tussen spelers.
 */
class YellCommand
@Inject
constructor(private val players: PlayerList) : PluginScript() {
    private var Player.pkKills by intVarp(varps.mike_pk_kills)

    override fun ScriptContext.startup() {
        onCommand("yell") {
            desc = "Stuur een bericht naar alle online spelers (met je PK-rang ervoor)"
            cheat {
                val msg = args.joinToString(" ").trim()
                if (msg.isEmpty()) {
                    player.mes("Gebruik: ::yell <bericht>")
                    return@cheat
                }
                val rank = PvpKillTracker.pkTitle(player.pkKills)
                for (online in players) {
                    online.mes("[Yell] [$rank] ${player.displayName}: $msg")
                }
            }
        }
    }
}
