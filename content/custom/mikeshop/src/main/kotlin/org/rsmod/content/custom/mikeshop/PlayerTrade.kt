package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.config.refs.invs
import org.rsmod.api.invtx.invMoveAll
import org.rsmod.api.invtx.invTransfer
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.advanced.onOpPlayer4
import org.rsmod.api.script.onCommand
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.objtx.TransactionResultList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PlayerTrade
@Inject
constructor(
    private val players: PlayerList,
    private val mapClock: MapClock,
    private val protectedAccess: ProtectedAccessLauncher,
    private val objTypes: ObjTypeList,
) : PluginScript() {
    private val requests = mutableMapOf<String, TradeRequest>()
    private val sessions = mutableMapOf<String, TradeSession>()

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
        onCommand("tradeoffer") {
            desc = "Offer an inventory item into the active trade"
            cheat {
                protectedAccess.launch(player) {
                    val slot = args.getOrNull(0)?.toIntOrNull()?.minus(1)
                    val count = args.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE
                    offerItem(slot, count)
                }
            }
        }
        onCommand("traderemove") {
            desc = "Remove an offered item from the active trade"
            cheat {
                protectedAccess.launch(player) {
                    val slot = args.getOrNull(0)?.toIntOrNull()?.minus(1)
                    val count = args.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE
                    removeOffer(slot, count)
                }
            }
        }
        onCommand("tradestatus") {
            desc = "Show the active trade status"
            cheat { protectedAccess.launch(player) { showTradeStatus() } }
        }
        onCommand("tradeaccept") {
            desc = "Accept the active trade"
            cheat { protectedAccess.launch(player) { acceptTrade() } }
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
            openTradeSession(player, target)
            return
        }
        requests[requestKey(from, to)] = TradeRequest(from, to, mapClock + REQUEST_TIMEOUT)
        mes("Sending trade request...")
        target.mes("${player.displayName} wishes to trade with you.")
        target.mes("Right-click them and choose 'Trade with', or type ::trade ${player.displayName}.")
    }

    private fun ProtectedAccess.cancelTradeRequests() {
        pruneExpiredRequests()
        val session = sessions.remove(sessionKey(player))
        if (session != null) {
            sessions.remove(session.key)
            returnOffers(session.first)
            returnOffers(session.second)
            player.mes("Trade cancelled.")
            session.partnerOf(player)?.mes("${player.displayName} cancelled the trade.")
            return
        }
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

    private fun ProtectedAccess.openTradeSession(first: Player, second: Player) {
        cancelSession(first)
        cancelSession(second)
        if (!returnOffers(first) || !returnOffers(second)) {
            first.mes("Could not start trade; make room in your inventory first.")
            second.mes("Could not start trade; make room in your inventory first.")
            return
        }
        val session = TradeSession(tradeSessionKey(first, second), first, second)
        sessions[session.key] = session
        first.mes("Trade started with ${second.displayName}.")
        second.mes("Trade started with ${first.displayName}.")
        first.mes("Use ::tradeoffer slot amount, ::traderemove slot amount, ::tradestatus and ::tradeaccept.")
        second.mes("Use ::tradeoffer slot amount, ::traderemove slot amount, ::tradestatus and ::tradeaccept.")
    }

    private fun ProtectedAccess.offerItem(slot: Int?, count: Int) {
        val session = activeSession() ?: return
        if (slot == null || slot !in player.inv.indices || count <= 0) {
            mes("Usage: ::tradeoffer inventory-slot amount. Slots are 1-28.")
            return
        }
        val obj = player.inv[slot]
        if (obj == null) {
            mes("There is no item in that inventory slot.")
            return
        }
        val type = objTypes[obj]
        if (!type.tradeable) {
            mes("You cannot trade that item.")
            return
        }
        val offer = player.tradeOffer()
        val moveCount = min(count, obj.count)
        val result = player.invTransfer(from = player.inv, fromSlot = slot, count = moveCount, into = offer)
        if (!result.success) {
            mes("Could not add that item to your offer.")
            return
        }
        session.resetAccepts()
        val partner = session.partnerOf(player)
        mes("Offered ${type.name} x $moveCount.")
        partner?.mes("${player.displayName} changed their offer.")
    }

    private fun ProtectedAccess.removeOffer(slot: Int?, count: Int) {
        val session = activeSession() ?: return
        val offer = player.tradeOffer()
        if (slot == null || slot !in offer.indices || count <= 0) {
            mes("Usage: ::traderemove offer-slot amount. Slots are 1-28.")
            return
        }
        val obj = offer[slot]
        if (obj == null) {
            mes("There is no item in that offer slot.")
            return
        }
        val type = objTypes[obj]
        val moveCount = min(count, obj.count)
        val result = player.invTransfer(from = offer, fromSlot = slot, count = moveCount, into = player.inv)
        if (!result.success) {
            mes("Could not remove that item; make room in your inventory first.")
            return
        }
        session.resetAccepts()
        val partner = session.partnerOf(player)
        mes("Removed ${type.name} x $moveCount from your offer.")
        partner?.mes("${player.displayName} changed their offer.")
    }

    private fun ProtectedAccess.showTradeStatus() {
        val session = activeSession() ?: return
        val partner = session.partnerOf(player) ?: return
        val myOffer = player.tradeOffer().offerSummary()
        val theirOffer = partner.tradeOffer().offerSummary()
        mes("Trading with ${partner.displayName}. Your offer: $myOffer")
        mes("${partner.displayName}'s offer: $theirOffer")
        mes("Accepted: you=${session.accepted(player)}, them=${session.accepted(partner)}.")
    }

    private fun ProtectedAccess.acceptTrade() {
        val session = activeSession() ?: return
        val partner = session.partnerOf(player) ?: return
        if (!player.isWithinDistance(partner, distance = 2)) {
            mes("You need to stand closer to accept the trade.")
            return
        }
        session.setAccepted(player, accepted = true)
        mes("You accept the trade.")
        partner.mes("${player.displayName} accepts the trade.")
        if (!session.accepted(player) || !session.accepted(partner)) {
            return
        }
        completeTrade(session)
    }

    private fun ProtectedAccess.completeTrade(session: TradeSession) {
        val first = session.first
        val second = session.second
        val firstOffer = first.tradeOffer()
        val secondOffer = second.tradeOffer()
        if (firstOffer.isEmpty() && secondOffer.isEmpty()) {
            mes("Neither player has offered any items.")
            session.resetAccepts()
            return
        }
        val firstMove = prepareMoveAll(first, firstOffer, second.inv)
        val secondMove = prepareMoveAll(second, secondOffer, first.inv)
        if (firstMove == null && secondMove == null) {
            session.resetAccepts()
            return
        }
        if (firstMove?.failure == true || secondMove?.failure == true) {
            first.mes("Trade failed; one player does not have enough inventory space.")
            second.mes("Trade failed; one player does not have enough inventory space.")
            session.resetAccepts()
            return
        }
        firstMove?.commitAll()
        secondMove?.commitAll()
        sessions.remove(session.key)
        first.mes("Trade completed with ${second.displayName}.")
        second.mes("Trade completed with ${first.displayName}.")
    }

    private fun prepareMoveAll(
        player: Player,
        from: Inventory,
        into: Inventory,
    ): TransactionResultList<*>? {
        if (from.isEmpty()) {
            return null
        }
        return player.invMoveAll(from = from, into = into, autoCommit = false)
    }

    private fun ProtectedAccess.activeSession(): TradeSession? {
        val session = sessions[sessionKey(player)]
        if (session == null) {
            mes("You are not in a trade.")
            return null
        }
        val partner = session.partnerOf(player)
        if (partner == null || players.none { it === partner }) {
            cancelSession(player)
            mes("The other player is no longer online.")
            return null
        }
        return session
    }

    private fun cancelSession(player: Player) {
        val session = sessions.remove(sessionKey(player)) ?: return
        sessions.remove(session.key)
        returnOffers(session.first)
        returnOffers(session.second)
    }

    private fun returnOffers(player: Player): Boolean {
        val offer = player.tradeOffer()
        if (offer.isEmpty()) {
            return true
        }
        val result = player.invMoveAll(from = offer, into = player.inv)
        if (!result.success) {
            player.mes("Make room in your inventory before closing this trade.")
            return false
        }
        return true
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

    private fun sessionKey(player: Player): String {
        val playerKey = player.tradeKey()
        return sessions.keys.firstOrNull { key ->
            key.startsWith("$playerKey|") || key.endsWith("|$playerKey")
        } ?: playerKey
    }

    private fun tradeSessionKey(first: Player, second: Player): String =
        listOf(first.tradeKey(), second.tradeKey()).sorted().joinToString("|")

    private fun Player.tradeKey(): String = displayName.lowercase()

    private fun Player.tradeOffer(): Inventory =
        checkNotNull(invMap[invs.tradeoffer]) { "Player has no tradeoffer inventory: $this" }

    private fun Inventory.offerSummary(): String {
        val items =
            filterNotNull().joinToString(", ") { obj ->
                "${objTypes[obj].name} x ${obj.count}"
            }
        return items.ifBlank { "nothing" }
    }

    private data class TradeRequest(val from: String, val to: String, val expiresAt: Int)

    private data class TradeSession(
        val key: String,
        val first: Player,
        val second: Player,
        var firstAccepted: Boolean = false,
        var secondAccepted: Boolean = false,
    ) {
        fun partnerOf(player: Player): Player? =
            when (player) {
                first -> second
                second -> first
                else -> null
            }

        fun accepted(player: Player): Boolean =
            when (player) {
                first -> firstAccepted
                second -> secondAccepted
                else -> false
            }

        fun setAccepted(player: Player, accepted: Boolean) {
            when (player) {
                first -> firstAccepted = accepted
                second -> secondAccepted = accepted
            }
        }

        fun resetAccepts() {
            firstAccepted = false
            secondAccepted = false
        }
    }

    private companion object {
        const val REQUEST_TIMEOUT = 100
    }
}
