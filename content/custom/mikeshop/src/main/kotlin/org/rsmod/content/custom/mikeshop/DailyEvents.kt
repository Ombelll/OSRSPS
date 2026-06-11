package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import java.sql.Types
import org.rsmod.api.db.Database
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.realm.Realm
import org.rsmod.api.realm.RealmConfig
import org.rsmod.api.realm.config.updater.RealmConfigUpdater
import org.rsmod.api.script.onCommand
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private data class DailyEvent(val label: String, val globalXpRate: Double, val broadcast: String?)

class DailyEvents
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val database: Database,
    private val realm: Realm,
    private val realmUpdater: RealmConfigUpdater,
    private val players: PlayerList,
) : PluginScript() {
    private val normal = DailyEvent("Normal rates", 1.0, null)
    private val doubleXp =
        DailyEvent("Double XP", 2.0, "Daily Event: Double XP is now active.")
    private val skillingBoost =
        DailyEvent("Skilling Boost", 3.0, "Daily Event: Skilling Boost is now active.")
    private val pvpWeekend =
        DailyEvent("PvP Weekend", 1.5, "Daily Event: PvP Weekend is now active.")

    override fun ScriptContext.startup() {
        onCommand("daily") {
            desc = "Set the daily server event and global XP rate"
            cheat { protectedAccess.launch(player) { selectDailyEvent() } }
        }
        onCommand("event") {
            desc = "Show the current daily server event"
            cheat { showCurrentEvent(player) }
        }
    }

    private fun showCurrentEvent(player: Player) {
        val config = realm.config
        val event = config.loginBroadcast ?: "No daily event broadcast set."
        player.mes("Global XP rate: ${config.globalXpRate}x.")
        player.mes(event)
    }

    private suspend fun ProtectedAccess.selectDailyEvent() {
        val pick =
            choice5(
                "Normal rates (1x)",
                1,
                "Double XP (2x)",
                2,
                "Skilling Boost (3x)",
                3,
                "PvP Weekend (1.5x)",
                4,
                "Cancel",
                0,
                title = "Daily Event - current ${realm.config.globalXpRate}x",
            )
        val event =
            when (pick) {
                1 -> normal
                2 -> doubleXp
                3 -> skillingBoost
                4 -> pvpWeekend
                else -> return
            }
        applyEvent(event)
    }

    private suspend fun ProtectedAccess.applyEvent(event: DailyEvent) {
        val updated = realm.config.copy(globalXpRate = event.globalXpRate, loginBroadcast = event.broadcast)
        saveEvent(updated)
        applyLive(updated)
        mes("Daily event set: ${event.label} (${event.globalXpRate}x global XP).")
    }

    private suspend fun saveEvent(config: RealmConfig) {
        database.withTransaction { connection ->
            val statement =
                connection.prepareStatement(
                    """
                        UPDATE realms
                        SET login_broadcast = ?, global_xp_rate_in_hundreds = ?
                        WHERE name = ?
                    """
                        .trimIndent()
                )
            statement.use {
                val broadcast = config.loginBroadcast
                if (broadcast == null) {
                    it.setNull(1, Types.VARCHAR)
                } else {
                    it.setString(1, broadcast)
                }
                it.setInt(2, (config.globalXpRate * 100).toInt())
                it.setString(3, realm.name)
                it.executeUpdate()
            }
        }
    }

    private fun applyLive(updated: RealmConfig) {
        val previous = realm.config
        realmUpdater.update(updated)
        if (previous.globalXpRate != updated.globalXpRate) {
            for (player in players) {
                player.globalXpRate = updated.globalXpRate
            }
        }
        val broadcast = updated.loginBroadcast
        if (broadcast != null) {
            for (player in players) {
                player.mes(broadcast, ChatType.Broadcast)
            }
        }
    }
}
