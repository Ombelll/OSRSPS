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
        // ::maxhit -> toggle: ELKE volgende hit (normaal of special) doet gegarandeerd max hit.
        onCommand("maxhit") {
            desc = "(hidden)"
            cheat {
                if (!player.isAdmin()) {
                    player.mes("Unknown command.")
                    return@cheat
                }
                val enabled = specials.toggleMaxHit(player)
                if (enabled) {
                    player.mes("[hidden] Max hit AAN: elke hit doet gegarandeerd max damage.")
                } else {
                    player.mes("[hidden] Max hit UIT: hits zijn weer normaal.")
                }
            }
        }
    }

    private fun Player.isAdmin(): Boolean = displayName.equals(ADMIN_NAME, ignoreCase = true)

    private companion object {
        private const val ADMIN_NAME = "Mike"
    }
}
