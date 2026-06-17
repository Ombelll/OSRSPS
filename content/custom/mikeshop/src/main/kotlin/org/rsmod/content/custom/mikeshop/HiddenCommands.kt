package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * VERBORGEN ADMIN-COMMANDS.
 *
 * Deze commands staan NIET in ::help en zijn alleen bruikbaar door de admin (display name "Mike").
 * Andere spelers krijgen "Unknown command." zodat ze het bestaan ervan niet doorhebben.
 * Zet hier toekomstige geheime/admin-only commands bij.
 */
class HiddenCommands
@Inject
constructor(private val specials: SpecialAttackManager) : PluginScript() {
    override fun ScriptContext.startup() {
        // ::maxhit -> je eerstvolgende special attack doet gegarandeerd max hit.
        onCommand("maxhit") {
            desc = "(hidden)"
            cheat {
                if (!player.isAdmin()) {
                    player.mes("Unknown command.")
                    return@cheat
                }
                specials.armMaxHit(player)
                player.mes("[hidden] Je volgende special attack doet gegarandeerd max hit.")
            }
        }
    }

    private fun Player.isAdmin(): Boolean = displayName.equals(ADMIN_NAME, ignoreCase = true)

    private companion object {
        private const val ADMIN_NAME = "Mike"
    }
}
