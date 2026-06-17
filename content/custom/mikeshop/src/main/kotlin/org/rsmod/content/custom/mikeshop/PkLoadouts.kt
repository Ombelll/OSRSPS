package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.back
import org.rsmod.api.player.feet
import org.rsmod.api.player.front
import org.rsmod.api.player.hands
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.righthand
import org.rsmod.api.player.ring
import org.rsmod.api.player.stat.statBoost
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.torso
import org.rsmod.api.script.onCommand
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * W2 PvP Roadmap - Fase 2: PK Gear Flow. Snelle loadout-commands die worn gear zetten + switches en
 * supplies in je inventory leggen. invAdd gebruikt strict=false zodat het vrije slots respecteert en
 * bestaande items niet overschrijft (worn gear wordt wel vervangen, dat is de bedoeling).
 *
 * Alle items hergebruiken de bestaande, bevestigde MaxGearObjs-refs.
 */
class PkLoadouts @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("brid") {
            desc = "PK tribrid/NH loadout (mage worn + range/melee switches + supplies)"
            cheat {
                // Worn: mage primair.
                player.hat = InvObj(MaxGearObjs.ancestral_hat)
                player.torso = InvObj(MaxGearObjs.ancestral_top)
                player.legs = InvObj(MaxGearObjs.ancestral_bottom)
                player.righthand = InvObj(MaxGearObjs.kodai_wand)
                player.hands = InvObj(MaxGearObjs.tormented_bracelet)
                player.feet = InvObj(MaxGearObjs.primordial_boots)
                player.front = InvObj(MaxGearObjs.occult_necklace)
                player.ring = InvObj(MaxGearObjs.ultor_ring)
                player.back = InvObj(MaxGearObjs.imbued_saradomin_cape)
                // Range switch.
                player.give(MaxGearObjs.masori_mask, MaxGearObjs.masori_body, MaxGearObjs.masori_chaps)
                player.give(MaxGearObjs.twisted_bow, MaxGearObjs.dizanas_quiver, MaxGearObjs.zaryte_vambraces)
                player.invAdd(player.inv, MaxGearObjs.rune_arrow, 1000, strict = false)
                // Melee switch.
                player.give(MaxGearObjs.torva_helm, MaxGearObjs.torva_chest, MaxGearObjs.torva_legs)
                player.give(MaxGearObjs.osmumtens_fang, MaxGearObjs.armadyl_godsword, MaxGearObjs.dragon_claws)
                player.give(MaxGearObjs.ferocious_gloves, MaxGearObjs.amulet_of_strength)
                player.giveRunes()
                player.giveSupplies(brews = 6, restores = 6, karam = 6, angler = 4)
                player.readyStats()
                player.mes("Tribrid/NH loadout klaar: mage worn + range/melee switches + supplies.")
            }
        }

        onCommand("veng") {
            desc = "PK melee/range veng loadout (cast Vengeance op Lunar: ::lunar)"
            cheat {
                player.hat = InvObj(MaxGearObjs.torva_helm)
                player.torso = InvObj(MaxGearObjs.torva_chest)
                player.legs = InvObj(MaxGearObjs.torva_legs)
                player.righthand = InvObj(MaxGearObjs.osmumtens_fang)
                player.hands = InvObj(MaxGearObjs.ferocious_gloves)
                player.feet = InvObj(MaxGearObjs.primordial_boots)
                player.front = InvObj(MaxGearObjs.amulet_of_strength)
                player.ring = InvObj(MaxGearObjs.ultor_ring)
                player.back = InvObj(MaxGearObjs.infernal_cape)
                // Range switch + spec.
                player.give(MaxGearObjs.magic_shortbow, MaxGearObjs.armadyl_godsword, MaxGearObjs.dragon_claws)
                player.give(MaxGearObjs.masori_mask, MaxGearObjs.masori_body, MaxGearObjs.masori_chaps)
                player.invAdd(player.inv, MaxGearObjs.rune_arrow, 1000, strict = false)
                player.giveSupplies(brews = 6, restores = 4, karam = 6, angler = 4)
                player.readyStats()
                player.mes("Veng loadout klaar. Tip: ga op Lunar (::lunar) voor Vengeance.")
            }
        }

        onCommand("pure") {
            desc = "PK pure-style loadout (low-prep offense: claws/MSB + str)"
            cheat {
                player.righthand = InvObj(MaxGearObjs.dragon_claws)
                player.hands = InvObj(MaxGearObjs.barrows_gloves)
                player.front = InvObj(MaxGearObjs.amulet_of_strength)
                player.ring = InvObj(MaxGearObjs.ultor_ring)
                player.back = InvObj(MaxGearObjs.infernal_cape)
                player.give(MaxGearObjs.magic_shortbow, MaxGearObjs.armadyl_godsword)
                player.invAdd(player.inv, MaxGearObjs.rune_arrow, 1000, strict = false)
                player.giveSupplies(brews = 2, restores = 2, karam = 4, angler = 6)
                player.invAdd(player.inv, MaxGearObjs.super_combat, 1, strict = false)
                player.readyStats()
                player.mes("Pure-style loadout klaar (vereenvoudigd; dedicated pure-gear volgt).")
            }
        }

        onCommand("tank") {
            desc = "PK tank/anti-PK loadout (Dharok + DFS + brews om te outlasten)"
            cheat {
                player.hat = InvObj(MaxGearObjs.dharok_head)
                player.torso = InvObj(MaxGearObjs.dharok_body)
                player.legs = InvObj(MaxGearObjs.dharok_legs)
                player.righthand = InvObj(MaxGearObjs.dharok_weapon)
                player.hands = InvObj(MaxGearObjs.barrows_gloves)
                player.feet = InvObj(MaxGearObjs.avernic_treads)
                player.front = InvObj(MaxGearObjs.amulet_of_strength)
                player.ring = InvObj(MaxGearObjs.ring_of_suffering_ri)
                player.back = InvObj(MaxGearObjs.imbued_saradomin_cape)
                player.give(MaxGearObjs.dragonfire_shield, MaxGearObjs.osmumtens_fang)
                player.giveSupplies(brews = 8, restores = 4, karam = 4, angler = 6)
                player.readyStats()
                player.mes("Tank/anti-PK loadout klaar: Dharok + DFS + brews.")
            }
        }

        onCommand("restock") {
            desc = "Vul food, karambwans, brews, restores, runes en arrows aan"
            cheat {
                player.giveSupplies(brews = 6, restores = 6, karam = 6, angler = 8)
                player.invAdd(player.inv, MaxGearObjs.super_combat, 2, strict = false)
                player.invAdd(player.inv, MaxGearObjs.rune_arrow, 1000, strict = false)
                player.giveRunes()
                player.statHeal(stats.hitpoints, constant = 999, percent = 0)
                player.statHeal(stats.prayer, constant = 999, percent = 0)
                player.mes("Restocked: food, karambwans, brews, restores, runes en arrows.")
            }
        }
    }

    private fun Player.give(vararg objs: ObjType) {
        for (obj in objs) {
            invAdd(inv, obj, 1, strict = false)
        }
    }

    private fun Player.giveRunes() {
        invAdd(inv, MaxGearObjs.waterrune, 1000, strict = false)
        invAdd(inv, MaxGearObjs.chaosrune, 1000, strict = false)
        invAdd(inv, MaxGearObjs.deathrune, 1000, strict = false)
        invAdd(inv, MaxGearObjs.bloodrune, 1000, strict = false)
        invAdd(inv, MaxGearObjs.soulrune, 1000, strict = false)
    }

    private fun Player.giveSupplies(brews: Int, restores: Int, karam: Int, angler: Int) {
        invAdd(inv, MaxGearObjs.saradomin_brew, brews, strict = false)
        invAdd(inv, MaxGearObjs.super_restore, restores, strict = false)
        invAdd(inv, MaxGearObjs.cooked_karambwan, karam, strict = false)
        invAdd(inv, MaxGearObjs.anglerfish, angler, strict = false)
    }

    private fun Player.readyStats() {
        statHeal(stats.hitpoints, constant = 999, percent = 0)
        statHeal(stats.prayer, constant = 999, percent = 0)
        statBoost(stats.attack, constant = 5, percent = 15)
        statBoost(stats.strength, constant = 5, percent = 15)
        statBoost(stats.defence, constant = 5, percent = 15)
        statBoost(stats.ranged, constant = 4, percent = 13)
        statBoost(stats.magic, constant = 4, percent = 13)
    }
}
