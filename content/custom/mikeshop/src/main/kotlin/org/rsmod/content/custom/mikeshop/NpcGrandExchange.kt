package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onOpNpc2
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private enum class ExchangeSide {
    Buy,
    Sell,
}

private data class ExchangeOrder(
    val id: Int,
    val owner: String,
    val side: ExchangeSide,
    val obj: ObjType,
    val name: String,
    val price: Int,
    var remaining: Int,
)

private data class ExchangeCollect(
    var coins: Int = 0,
    val items: MutableMap<ObjType, Int> = linkedMapOf(),
)

private object NpcExchangeBook {
    private val buyOrders = ArrayList<ExchangeOrder>()
    private val sellOrders = ArrayList<ExchangeOrder>()
    private val collect = HashMap<String, ExchangeCollect>()
    private var nextOrderId = 1

    @Synchronized
    fun placeBuy(owner: String, type: UnpackedObjType, price: Int, count: Int): String {
        val obj = type.toHashedType()
        val order = ExchangeOrder(nextOrderId++, owner, ExchangeSide.Buy, obj, type.name, price, count)
        buyOrders += order
        val bought = match()
        return "Buy order placed: ${type.name} x$count at $price gp each. Matched $bought item(s)."
    }

    @Synchronized
    fun placeSell(owner: String, type: UnpackedObjType, price: Int, count: Int): String {
        val obj = type.toHashedType()
        val order = ExchangeOrder(nextOrderId++, owner, ExchangeSide.Sell, obj, type.name, price, count)
        sellOrders += order
        val sold = match()
        return "Sell order placed: ${type.name} x$count at $price gp each. Matched $sold item(s)."
    }

    @Synchronized
    fun collect(owner: String): ExchangeCollect {
        return collect.remove(owner) ?: ExchangeCollect()
    }

    @Synchronized
    fun summary(owner: String): List<String> {
        val ownBuys = buyOrders.filter { it.owner == owner && it.remaining > 0 }
        val ownSells = sellOrders.filter { it.owner == owner && it.remaining > 0 }
        val pending = collect[owner]
        return buildList {
            add("Open buy orders: ${ownBuys.size}")
            ownBuys.take(3).forEach { add("#${it.id}: ${it.name} x${it.remaining} at ${it.price} gp") }
            add("Open sell orders: ${ownSells.size}")
            ownSells.take(3).forEach { add("#${it.id}: ${it.name} x${it.remaining} at ${it.price} gp") }
            add("Collect: ${pending?.coins ?: 0} gp, ${pending?.items?.values?.sum() ?: 0} item(s)")
        }
    }

    private fun match(): Int {
        var matched = 0
        val buys = buyOrders.sortedWith(compareByDescending<ExchangeOrder> { it.price }.thenBy { it.id })
        val sells = sellOrders.sortedWith(compareBy<ExchangeOrder> { it.price }.thenBy { it.id })

        for (buy in buys) {
            if (buy.remaining <= 0) continue
            for (sell in sells) {
                if (sell.remaining <= 0) continue
                if (buy.obj.id != sell.obj.id || buy.price < sell.price) continue

                val count = minOf(buy.remaining, sell.remaining)
                val tradePrice = sell.price
                buy.remaining -= count
                sell.remaining -= count
                matched += count

                collectFor(buy.owner).items.merge(buy.obj, count, Int::plus)
                collectFor(sell.owner).coins += tradePrice * count
                val refund = (buy.price - tradePrice) * count
                if (refund > 0) {
                    collectFor(buy.owner).coins += refund
                }
            }
        }

        buyOrders.removeAll { it.remaining <= 0 }
        sellOrders.removeAll { it.remaining <= 0 }
        return matched
    }

    private fun collectFor(owner: String): ExchangeCollect = collect.getOrPut(owner) { ExchangeCollect() }
}

class NpcGrandExchange
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val marketPrices: MarketPrices,
    private val objTypes: ObjTypeList,
    private val objRepo: ObjRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("npcge") {
            desc = "Open the simple NPC Grand Exchange"
            cheat { protectedAccess.launch(player) { openExchange() } }
        }
        onOpNpc2(HubNpcs.clerk) { protectedAccess.launch(player) { openExchange() } }
    }

    private suspend fun ProtectedAccess.openExchange() {
        when (
            choice5(
                "Place buy order",
                1,
                "Place sell order",
                2,
                "Collect",
                3,
                "View my orders",
                4,
                "Nothing",
                0,
                title = "Grand Exchange Clerk",
            )
        ) {
            1 -> placeBuyOrder()
            2 -> placeSellOrder()
            3 -> collectExchange()
            4 -> showOrders()
        }
    }

    private suspend fun ProtectedAccess.placeBuyOrder() {
        val type = objTypes.uncert(objDialog("Choose an item to buy.", stockMarketRestriction = true))
        val price = positiveCount("Price per item:", defaultPrice(type))
        val count = positiveCount("Quantity:", 1)
        val total = safeMultiply(price, count)
        if (total == null) {
            mesbox("That order is too large.")
            return
        }
        if (!invDel(inv, objs.coins, total).success) {
            mesbox("You need $total coins to place that buy order.")
            return
        }
        mesbox(NpcExchangeBook.placeBuy(ownerKey(player), type, price, count))
    }

    private suspend fun ProtectedAccess.placeSellOrder() {
        val type = objTypes.uncert(objDialog("Choose an item to sell.", stockMarketRestriction = true))
        val obj = type.toHashedType()
        val price = positiveCount("Price per item:", defaultPrice(type))
        val count = positiveCount("Quantity:", 1)
        if (!invDel(inv, obj, count).success) {
            mesbox("You do not have ${type.name} x$count to sell.")
            return
        }
        mesbox(NpcExchangeBook.placeSell(ownerKey(player), type, price, count))
    }

    private suspend fun ProtectedAccess.collectExchange() {
        val collect = NpcExchangeBook.collect(ownerKey(player))
        if (collect.coins <= 0 && collect.items.isEmpty()) {
            mesbox("You have nothing to collect.")
            return
        }
        if (collect.coins > 0) {
            invAddOrDrop(objRepo, objs.coins, collect.coins)
        }
        for ((obj, count) in collect.items) {
            invAddOrDrop(objRepo, obj, count)
        }
        mesbox("Collected ${collect.coins} coins and ${collect.items.values.sum()} item(s).")
    }

    private suspend fun ProtectedAccess.showOrders() {
        mesbox(NpcExchangeBook.summary(ownerKey(player)).joinToString("<br>"))
    }

    private suspend fun ProtectedAccess.positiveCount(title: String, fallback: Int): Int {
        val input = countDialog("$title Suggested: $fallback")
        return input.coerceAtLeast(1)
    }

    private fun defaultPrice(type: UnpackedObjType): Int = (marketPrices[type] ?: type.cost).coerceAtLeast(1)

    private fun safeMultiply(price: Int, count: Int): Int? {
        val total = price.toLong() * count.toLong()
        return if (total in 1..Int.MAX_VALUE) total.toInt() else null
    }

    private fun ownerKey(player: Player): String = player.displayName.lowercase()
}
