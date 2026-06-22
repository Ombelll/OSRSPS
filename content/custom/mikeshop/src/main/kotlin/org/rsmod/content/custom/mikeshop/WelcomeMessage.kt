package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Command-overzicht (discoverability).
 *
 * Dynamisch: leest de volledige command-registry (ScriptContext.cheatCommandMap - dezelfde map waar
 * alle onCommand's in registreren), sorteert alfabetisch en print alles - behalve verborgen admin-/
 * debug-commands (die met desc "(hidden)", o.a. ::maxhit en ::qpdebug).
 *
 * Geregistreerd onder ::commands, ::cmds en ::help. LET OP: veel clients (RuneLite/blurite) vangen
 * ::help lokaal af en sturen 'm niet naar de server, dus gebruik ::commands. Zo blijft de lijst
 * vanzelf compleet als er later commands bijkomen, zonder dat hier iets aangepast hoeft.
 *
 * De login-MOTD + hub-teleport zit in MainHub.kt (een enkele onPlayerLogin-handler).
 */
class WelcomeMessage @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        // De échte registratie-map vastpakken (niet injecteren: dat kan een lege instance zijn).
        val registry = cheatCommandMap
        for (alias in ALIASES) {
            onCommand(alias) {
                desc = "List all available commands"
                cheat { listCommands(registry) }
            }
        }
    }

    private fun Cheat.listCommands(registry: CheatCommandMap) {
        val names =
            registry.commands
                .filterValues { it.desc != HIDDEN_MARKER }
                .keys
                .sorted()
        player.mes("--- Commands (${names.size}) - typ ::<naam> ---")
        for (line in names.chunked(COMMANDS_PER_LINE)) {
            player.mes(line.joinToString("  ") { "::$it" })
        }
        player.mes("Tip: ::bosses en ::quests geven hun eigen sub-overzichten.")
    }

    private companion object {
        private val ALIASES = listOf("commands", "cmds", "help")
        private const val HIDDEN_MARKER = "(hidden)"
        private const val COMMANDS_PER_LINE = 6
    }
}
