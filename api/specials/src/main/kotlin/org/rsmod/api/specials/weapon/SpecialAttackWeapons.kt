package org.rsmod.api.specials.weapon

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.specials.configs.energy_enums
import org.rsmod.api.specials.energy.SpecialAttackEnergy
import org.rsmod.game.enums.EnumTypeMapResolver
import org.rsmod.game.type.obj.ObjType

public class SpecialAttackWeapons
@Inject
constructor(private val enumResolver: EnumTypeMapResolver) {
    private lateinit var energyRequirements: Map<Int, Int>
    private lateinit var descriptions: Map<Int, String>

    /**
     * Returns the special attack energy requirement for [objType] from the `energy_requirements`
     * enum.
     *
     * @return the special attack energy requirement for [objType] in the range of `1` to
     *   [MAX_ENERGY] (`1000`), or `null` if [objType] does not have an associated special attack.
     * @see [loadEnergyRequirements]
     */
    public fun getSpecialEnergy(objType: ObjType): Int? = energyRequirements[objType.id]

    public fun getSpecialDescription(objType: ObjType): String? = descriptions[objType.id]

    internal fun startup() {
        val energyRequirements = loadEnergyRequirements()
        this.energyRequirements = energyRequirements

        val descriptions = loadDescriptions()
        this.descriptions = descriptions
    }

    private fun loadEnergyRequirements(): Map<Int, Int> {
        val requirements = mutableMapOf<Int, Int>()

        val enum = enumResolver[energy_enums.energy_requirements].filterValuesNotNull()
        for ((obj, energy) in enum) {
            check(energy in 0..MAX_ENERGY) {
                "Expected `energy` values to be within range of [0..$MAX_ENERGY]: actual=$energy"
            }
            requirements[obj.id] = energy
        }
        requirements.addCustomEnergyRequirements()

        return requirements
    }

    private fun loadDescriptions(): Map<Int, String> {
        val descriptions = mutableMapOf<Int, String>()

        val enum = enumResolver[energy_enums.descriptions].filterValuesNotNull()
        for ((obj, description) in enum) {
            descriptions[obj.id] = description
        }
        descriptions.addCustomDescriptions()

        return descriptions
    }

    private fun MutableMap<Int, Int>.addCustomEnergyRequirements() {
        putIfAbsent(objs.armadyl_godsword.id, 500)
        putIfAbsent(objs.dragon_claws.id, 500)
        putIfAbsent(objs.granite_maul.id, 500)
        putIfAbsent(objs.granite_maul_pretty.id, 500)
        putIfAbsent(objs.granite_maul_plus.id, 500)
        putIfAbsent(objs.granite_maul_pretty_plus.id, 500)
        putIfAbsent(objs.dragon_warhammer.id, 500)
        putIfAbsent(objs.dragon_warhammer_ornament.id, 500)
        putIfAbsent(objs.abyssal_dagger.id, 500)
        putIfAbsent(objs.abyssal_dagger_p.id, 500)
        putIfAbsent(objs.abyssal_dagger_p_plus.id, 500)
        putIfAbsent(objs.abyssal_dagger_p_plus_plus.id, 500)
        putIfAbsent(objs.osmumtens_fang.id, 250)
        putIfAbsent(objs.osmumtens_fang_or.id, 250)
        putIfAbsent(objs.magic_shortbow.id, 550)
        putIfAbsent(objs.magic_shortbow_i.id, 500)
        putIfAbsent(objs.toxic_blowpipe.id, 500)
        putIfAbsent(objs.toxic_blowpipe_loaded.id, 500)
        putIfAbsent(objs.toxic_blowpipe_ornament.id, 500)
        putIfAbsent(objs.toxic_blowpipe_loaded_ornament.id, 500)
    }

    private fun MutableMap<Int, String>.addCustomDescriptions() {
        putIfAbsent(objs.armadyl_godsword.id, "The Judgement")
        putIfAbsent(objs.dragon_claws.id, "Slice and Dice")
        putIfAbsent(objs.granite_maul.id, "Quick Smash")
        putIfAbsent(objs.granite_maul_pretty.id, "Quick Smash")
        putIfAbsent(objs.granite_maul_plus.id, "Quick Smash")
        putIfAbsent(objs.granite_maul_pretty_plus.id, "Quick Smash")
        putIfAbsent(objs.dragon_warhammer.id, "Smash")
        putIfAbsent(objs.dragon_warhammer_ornament.id, "Smash")
        putIfAbsent(objs.abyssal_dagger.id, "Abyssal Puncture")
        putIfAbsent(objs.abyssal_dagger_p.id, "Abyssal Puncture")
        putIfAbsent(objs.abyssal_dagger_p_plus.id, "Abyssal Puncture")
        putIfAbsent(objs.abyssal_dagger_p_plus_plus.id, "Abyssal Puncture")
        putIfAbsent(objs.osmumtens_fang.id, "Eviscerate")
        putIfAbsent(objs.osmumtens_fang_or.id, "Eviscerate")
        putIfAbsent(objs.magic_shortbow.id, "Snapshot")
        putIfAbsent(objs.magic_shortbow_i.id, "Snapshot")
        putIfAbsent(objs.toxic_blowpipe.id, "Toxic Siphon")
        putIfAbsent(objs.toxic_blowpipe_loaded.id, "Toxic Siphon")
        putIfAbsent(objs.toxic_blowpipe_ornament.id, "Toxic Siphon")
        putIfAbsent(objs.toxic_blowpipe_loaded_ornament.id, "Toxic Siphon")
    }

    private companion object {
        private const val MAX_ENERGY = SpecialAttackEnergy.MAX_ENERGY
    }
}
