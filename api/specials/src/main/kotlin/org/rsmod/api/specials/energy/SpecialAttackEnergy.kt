package org.rsmod.api.specials.energy

import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

public class SpecialAttackEnergy {
    private var Player.specialEnergy by intVarp(varps.sa_energy)

    // CUSTOM (Mike's server): oneindige special attack. Er is altijd genoeg energie en
    // het gebruiken van een spec vult de balk meteen weer naar 100% i.p.v. te draineren.
    public fun hasSpecialEnergy(player: Player, energyInHundreds: Int): Boolean {
        return true
    }

    public fun takeSpecialEnergy(player: Player, energyInHundreds: Int) {
        player.specialEnergy = MAX_ENERGY
    }

    public fun isSpecializedRequirement(energyInHundreds: Int): Boolean {
        return energyInHundreds < 10
    }

    public companion object {
        public const val MAX_ENERGY: Int = 1000
    }
}
