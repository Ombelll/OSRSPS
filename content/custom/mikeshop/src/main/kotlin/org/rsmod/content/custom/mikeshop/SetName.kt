package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * ::setname <naam> -> zet je display name direct. Alternatief voor de "Configure Display name"-knop
 * (die geen server-handler heeft op deze rev). De naam is meteen in-game zichtbaar; bij volgende
 * login toont de hub-MOTD 'm ook.
 */
class SetName @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("setname") {
            desc = "Set your display name: ::setname <name>"
            cheat {
                val name = args.joinToString(" ").trim()
                if (name.isBlank()) {
                    player.mes("Usage: ::setname <name>")
                    return@cheat
                }
                if (name.length > 12) {
                    player.mes("Display name max 12 characters.")
                    return@cheat
                }
                player.displayName = name
                player.mes("Your display name is now: $name")
            }
        }
    }
}
