package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import java.util.Locale
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * De PK-hub banker becommentarieert spelers in de algemene chat op basis van hun K/D-ratio
 * (PvP-wereld). Negatief (meer deaths dan kills) -> hij trasht je bij naam; positief -> respect.
 * Triggert bij login, zodat beide spelers de roast/props in de chat zien.
 */
class BankerChat
@Inject
constructor(private val players: PlayerList) : PluginScript() {
    private val pvpWorld = System.getenv("RSMOD_WORLD") == "2"
    private var Player.pkKills by intVarp(varps.mike_pk_kills)
    private var Player.pkDeaths by intVarp(varps.mike_pk_deaths)

    override fun ScriptContext.startup() {
        if (!pvpWorld) {
            return
        }
        onPlayerLogin { player.bankerGreeting() }
    }

    private fun Player.bankerGreeting() {
        val kills = pkKills
        val deaths = pkDeaths
        val kd = if (deaths == 0) kills.toDouble() else kills.toDouble() / deaths
        val kdText = String.format(Locale.ROOT, "%.2f", kd)
        val line =
            when {
                kills == 0 && deaths == 0 ->
                    "[Banker] A fresh face: $displayName. Let's see if you can PK or just feed."
                kd < 1.0 ->
                    NEGATIVE_LINES.random().replace("{name}", displayName).replace("{kd}", kdText)
                kd >= 2.0 ->
                    RESPECT_LINES.random().replace("{name}", displayName).replace("{kd}", kdText)
                else ->
                    NEUTRAL_LINES.random().replace("{name}", displayName).replace("{kd}", kdText)
            }
        for (online in players) {
            online.mes(line)
        }
    }

    private companion object {
        private val NEGATIVE_LINES =
            listOf(
                "[Banker] Look who crawled back - {name} with a pathetic {kd} KD. " +
                    "Did you forget how to click?",
                "[Banker] Make room, {name} ({kd} KD) is here to feed the Wilderness again.",
                "[Banker] {name}, a {kd} KD? My grandmother PKs better, and she's been dead for years.",
                "[Banker] Careful everyone, {name} drops loot just by existing. ({kd} KD)",
                "[Banker] Welcome back {name}. With that {kd} KD, maybe try fishing instead?",
            )
        private val RESPECT_LINES =
            listOf(
                "[Banker] Bow your heads - {name} walks in with a deadly {kd} KD!",
                "[Banker] The Wilderness fears {name}. ({kd} KD)",
                "[Banker] {name} is on the warpath with a {kd} KD. Stay clear.",
            )
        private val NEUTRAL_LINES =
            listOf(
                "[Banker] {name} keeps it even at {kd} KD. Prove yourself out there.",
                "[Banker] {name} ({kd} KD) - decent, but the hill won't claim itself.",
            )
    }
}
