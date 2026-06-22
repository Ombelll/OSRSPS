package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Wilderness Rules v2 (Fase 3) - Tele Block.
 *
 * Een tele-geblokkeerde speler kan zijn teleport-SPELLS niet gebruiken (de PK-escape) tot het
 * blok afloopt. Utility/admin-commands (::edge, ::ge, ::pkready) blijven werken voor testgemak.
 * Toepassen via `::teleblock <naam>` / `::tb <naam>` (5 minuten).
 */
internal object TeleBlockState {
    private const val DURATION_MS = 5 * 60 * 1000L
    private val until = HashMap<Int, Long>()

    fun apply(player: Player) {
        until[player.slotId] = System.currentTimeMillis() + DURATION_MS
    }

    fun isBlocked(player: Player): Boolean {
        val expiry = until[player.slotId] ?: return false
        if (System.currentTimeMillis() >= expiry) {
            until.remove(player.slotId)
            return false
        }
        return true
    }

    fun remainingSeconds(player: Player): Long {
        val expiry = until[player.slotId] ?: return 0
        return ((expiry - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
    }

    fun clear(player: Player) {
        until.remove(player.slotId)
    }
}

class WildernessRules @Inject constructor(private val players: PlayerList) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("teleblock") {
            desc = "Tele-block a player for 5 min (blocks their teleport spells): ::teleblock name"
            cheat { player.teleBlock(args.joinToString(" ").trim()) }
        }
        onCommand("tb") {
            desc = "Tele-block a player: ::tb name"
            cheat { player.teleBlock(args.joinToString(" ").trim()) }
        }
    }

    private fun Player.teleBlock(name: String) {
        if (name.isBlank()) {
            mes("Usage: ::teleblock player")
            return
        }
        val target = players.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
        if (target == null) {
            mes("Could not find an online player named '$name'.")
            return
        }
        TeleBlockState.apply(target)
        mes("You tele-block ${target.displayName} for 5 minutes.")
        target.mes("You have been tele-blocked! Your teleport spells are disabled for a while.")
    }
}
