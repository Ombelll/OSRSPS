package org.rsmod.api.combat.manager

import org.rsmod.api.config.refs.walktriggers
import org.rsmod.game.entity.Player

/**
 * Freeze-state voor ice-spells (Ice Rush/Burst/Blitz/Barrage). Houdt per speler bij tot welke
 * game-tick hij bevroren is en tot wanneer hij immuun is (kan dan niet meteen herbevroren worden).
 * De daadwerkelijke bewegingsblokkering loopt via de [walktriggers.pvp_frozen] walk-trigger, die in
 * een PluginScript (PvPCombatScript) geregistreerd is en [isFrozen] checkt.
 *
 * Staat in combat-manager zodat zowel de combat-scripts (walk-trigger) als de spell-attacks
 * (freeze toepassen op een rake hit) erbij kunnen.
 */
public object FreezeState {
    private val frozenUntil = HashMap<Int, Int>()
    private val immuneUntil = HashMap<Int, Int>()

    public fun isFrozen(player: Player): Boolean {
        val until = frozenUntil[player.slotId] ?: return false
        return player.currentMapClock < until
    }

    /** Mag [player] nu bevroren worden? (Niet al bevroren en niet in de immuniteitsperiode.) */
    public fun canFreeze(player: Player): Boolean {
        if (isFrozen(player)) {
            return false
        }
        val immune = immuneUntil[player.slotId] ?: return true
        return player.currentMapClock >= immune
    }

    public fun freeze(target: Player, durationTicks: Int) {
        val now = target.currentMapClock
        frozenUntil[target.slotId] = now + durationTicks
        // Immuniteit = duur van de freeze erna (OSRS-stijl), zodat je niet eindeloos vastgezet wordt.
        immuneUntil[target.slotId] = now + durationTicks + durationTicks
        target.walkTrigger(walktriggers.pvp_frozen)
        target.abortRoute()
    }
}
