package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.feet
import org.rsmod.api.player.front
import org.rsmod.api.player.hands
import org.rsmod.api.player.hat
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.legs
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.back
import org.rsmod.api.player.righthand
import org.rsmod.api.player.ring
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBoost
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.torso
import org.rsmod.api.script.onCommand
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.inv.InvObj
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object MaxGearObjs : ObjReferences() {
    // Echte BIS-melee (beste items in deze cache-revisie):
    val justiciar_faceguard = find("justiciar_faceguard")
    val bandos_chestplate = find("bandos_chestplate")
    val bandos_tassets = find("bandos_skirt") // = Bandos tassets (interne cache-naam)
    val scythe_of_vitur = find("scythe_of_vitur") // beste melee-wapen (2h)
    val ferocious_gloves = find("ferocious_gloves")
    val primordial_boots = find("primordial_boots")
    val amulet_of_strength = find("amulet_of_strength")
    val ultor_ring = find("ultor_ring") // beste melee-ring
    val infernal_cape = find("infernal_cape") // beste cape
    val super_combat = find("4dose2combat")

    // Anti-fire 1h-swap voor de draken-bosses (rapier + dragonfire shield):
    val ghrazi_rapier = find("ghrazi_rapier")
    val dragonfire_shield = find("dragonfire_shield")

    // Ranged-kit:
    val magic_shortbow = find("magic_shortbow")
    val rune_arrow = find("rune_arrow")

    // Magic-kit (fire staff = oneindig vuur-runes + losse runes voor combat-spells):
    val staff_of_fire = find("staff_of_fire")
    val airrune = find("airrune")
    val mindrune = find("mindrune")
    val chaosrune = find("chaosrune")
    val deathrune = find("deathrune")
}

/**
 * ::maxgear -> testfase-loadout:
 *  - trekt vol rune + dragon scimitar + glory-amulet aan,
 *  - zet alle combat-stats op 99 (dus alle prayers ontgrendeld),
 *  - vult HP + prayer points helemaal aan,
 *  - geeft een super combat potion én activeert meteen de super-combat-boost.
 */
class MaxGear @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("maxgear") {
            desc = "Test: best-in-slot melee gear, 99 combat, all prayers + super combat"
            cheat {
                // Gear aantrekken (echte BIS melee; scythe = 2h dus geen shield):
                player.hat = InvObj(MaxGearObjs.justiciar_faceguard)
                player.torso = InvObj(MaxGearObjs.bandos_chestplate)
                player.legs = InvObj(MaxGearObjs.bandos_tassets)
                player.righthand = InvObj(MaxGearObjs.scythe_of_vitur)
                player.hands = InvObj(MaxGearObjs.ferocious_gloves)
                player.feet = InvObj(MaxGearObjs.primordial_boots)
                player.front = InvObj(MaxGearObjs.amulet_of_strength)
                player.ring = InvObj(MaxGearObjs.ultor_ring)
                player.back = InvObj(MaxGearObjs.infernal_cape)

                // Anti-fire 1h-swap (rapier + dragonfire shield) voor de draken-bosses:
                player.invAdd(player.inv, MaxGearObjs.ghrazi_rapier, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.dragonfire_shield, 1, strict = false)

                // Combat-stats naar 99 (ontgrendelt alle prayers):
                val maxXp = PlayerStatMap.toFineXP(13_034_431.0).toDouble()
                val combat =
                    listOf(
                        stats.attack,
                        stats.strength,
                        stats.defence,
                        stats.hitpoints,
                        stats.ranged,
                        stats.magic,
                        stats.prayer,
                    )
                for (stat in combat) {
                    player.statAdvance(stat, maxXp)
                }

                // HP + prayer points helemaal vol:
                player.statHeal(stats.hitpoints, constant = 999, percent = 0)
                player.statHeal(stats.prayer, constant = 999, percent = 0)

                // Ranged-kit (wissel naar de bow voor ranged-combat):
                player.invAdd(player.inv, MaxGearObjs.magic_shortbow, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.rune_arrow, 1000, strict = false)

                // Magic-kit (wield de fire staff + cast combat-spells):
                player.invAdd(player.inv, MaxGearObjs.staff_of_fire, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.airrune, 1000, strict = false)
                player.invAdd(player.inv, MaxGearObjs.mindrune, 1000, strict = false)
                player.invAdd(player.inv, MaxGearObjs.chaosrune, 1000, strict = false)
                player.invAdd(player.inv, MaxGearObjs.deathrune, 1000, strict = false)

                // Super combat potion + meteen de boost:
                player.invAdd(player.inv, MaxGearObjs.super_combat, 1, strict = false)
                player.statBoost(stats.attack, constant = 5, percent = 15)
                player.statBoost(stats.strength, constant = 5, percent = 15)
                player.statBoost(stats.defence, constant = 5, percent = 15)

                player.mes("Maxed: gear, 99 combat, all prayers + full points, super combat. ::mikeboss!")
            }
        }
    }
}
