package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.advanced.onOpPlayer4
import org.rsmod.api.script.onCommand
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PlayerTrade
@Inject
constructor(
    private val players: PlayerList,
    private val mapClock: MapClock,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    private val requests = mutableMapOf<String, TradeRequest>()

    override fun ScriptContext.startup() {
        onOpPlayer4 { requestTrade(it.target) }
        onCommand("trade") {
            desc = "Send a trade request to an online player"
            cheat {
                val name = args.joinToString(" ").trim()
                if (name.isBlank()) {
                    player.mes("Usage: ::trade player name")
                    return@cheat
                }
                val target = players.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
                if (target == null) {
                    player.mes("Could not find an online player named '$name'.")
                    return@cheat
                }
                protectedAccess.launch(player) { requestTrade(target) }
            }
        }
        onCommand("tradecancel") {
            desc = "Cancel pending trade requests"
            cheat { protectedAccess.launch(player) { cancelTradeRequests() } }
        }
    }

    private fun ProtectedAccess.requestTrade(target: Player) {
        pruneExpiredRequests()
        if (target === player) {
            mes("You cannot trade yourself.")
            return
        }
        if (!player.isWithinDistance(target, distance = 2)) {
            mes("You need to stand closer to trade ${target.displayName}.")
            return
        }
        val from = player.tradeKey()
        val to = target.tradeKey()
        val reciprocal = requests[requestKey(to, from)]
        if (reciprocal != null && mapClock <= reciprocal.expiresAt) {
            clearRequests(player, target)
            mes("Trade request accepted with ${target.displayName}.")
            target.mes("${player.displayName} accepted your trade request.")
            mes("Offer screens are the next Trade Plan A step; this handshake is now live.")
            target.mes("Offer screens are the next Trade Plan A step; this handshake is now live.")
            return
        }
        requests[requestKey(from, to)] = TradeRequest(from, to, mapClock + REQUEST_TIMEOUT)
        mes("Sending trade request...")
        target.mes("${player.displayName} wishes to trade with you.")
        target.mes("Right-click them and choose 'Trade with', or type ::trade ${player.displayName}.")
    }

    private fun ProtectedAccess.cancelTradeRequests() {
        pruneExpiredRequests()
        val before = requests.size
        requests.entries.removeAll { (_, request) ->
            request.from == player.tradeKey() || request.to == player.tradeKey()
        }
        if (requests.size == before) {
            mes("You have no pending trade requests.")
        } else {
            mes("Pending trade requests cancelled.")
        }
    }

    private fun clearRequests(first: Player, second: Player) {
        val firstKey = first.tradeKey()
        val secondKey = second.tradeKey()
        requests.remove(requestKey(firstKey, secondKey))
        requests.remove(requestKey(secondKey, firstKey))
    }

    private fun pruneExpiredRequests() {
        val now = mapClock - 0
        requests.entries.removeAll { it.value.expiresAt < now }
    }

    private fun requestKey(from: String, to: String): String = "$from->$to"

    private fun Player.tradeKey(): String = displayName.lowercase()

    private data class TradeRequest(val from: String, val to: String, val expiresAt: Int)

    private companion object {
        const val REQUEST_TIMEOUT = 100
    }
}
