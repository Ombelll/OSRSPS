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
    val torva_helm = find("torva_helm")
    val torva_chest = find("torva_chest")
    val torva_legs = find("torva_legs")
    val osmumtens_fang = find("osmumtens_fang")
    val armadyl_godsword = find("ags")
    val dragon_claws = find("dragon_claws")
    val scythe_of_vitur = find("scythe_of_vitur")
    val ghrazi_rapier = find("ghrazi_rapier")
    val dragonfire_shield = find("dragonfire_shield")
    val ferocious_gloves = find("ferocious_gloves")
    val primordial_boots = find("primordial_boots")
    val amulet_of_strength = find("amulet_of_strength")
    val ultor_ring = find("ultor_ring")
    val infernal_cape = find("infernal_cape")
    val imbued_saradomin_cape = find("ma2_saradomin_cape")
    val ring_of_suffering_ri = find("nzone_zenyte_ring_enchanted_recoil")
    val barrows_gloves = find("hundred_gauntlets_level_10")
    val tormented_bracelet = find("zenyte_bracelet_enchanted")
    val zaryte_vambraces = find("zaryte_vambraces")
    val avernic_treads = find("avernic_treads")
    val spirit_shield = find("spirit_shield")
    val blessed_spirit_shield = find("blessed_spirit_shield")
    val elysian_spirit_shield = find("elysian")
    val spectral_spirit_shield = find("spectral")
    val arcane_spirit_shield = find("arcane")
    val super_combat = find("4dose2combat")
    val anglerfish = find("anglerfish")
    val cooked_karambwan = find("tbwt_cooked_karambwan")
    val saradomin_brew = find("4dosepotionofsaradomin")
    val super_restore = find("4dose2restore")

    val dharok_head = find("barrows_dharok_head")
    val dharok_body = find("barrows_dharok_body")
    val dharok_legs = find("barrows_dharok_legs")
    val dharok_weapon = find("barrows_dharok_weapon")
    val inquisitors_helm = find("inquisitors_helm")
    val inquisitors_body = find("inquisitors_body")
    val inquisitors_skirt = find("inquisitors_skirt")
    val inquisitors_mace = find("inquisitors_mace")

    val magic_shortbow = find("magic_shortbow")
    val rune_arrow = find("rune_arrow")
    val twisted_bow = find("twisted_bow")
    val masori_mask = find("masori_mask_fortified")
    val masori_body = find("masori_body_fortified")
    val masori_chaps = find("masori_chaps_fortified")
    val dizanas_quiver = find("dizanas_quiver_infinite")

    val ancient_staff = find("staff_of_zaros")
    val kodai_wand = find("kodai_wand")
    val toxic_staff_of_the_dead = find("toxic_sotd_charged")
    val ancestral_hat = find("ancestral_hat")
    val ancestral_top = find("ancestral_robe_top")
    val ancestral_bottom = find("ancestral_robe_bottom")
    val occult_necklace = find("occult_necklace")
    val saturated_heart = find("saturated_heart")
    val waterrune = find("waterrune")
    val chaosrune = find("chaosrune")
    val deathrune = find("deathrune")
    val bloodrune = find("bloodrune")
    val soulrune = find("soulrune")
}

/** ::maxgear -> PK-testloadout met max stats, prayer unlocks, Ancients en veel switches. */
class MaxGear @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("maxgear") {
            desc = "Test: Torva/Fang, switches, 99 combat, prayer unlocks + Ancient Magicks"
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
                player.unlockAllPrayers()
                player.enableAncientMagicks()

                player.statHeal(stats.hitpoints, constant = 999, percent = 0)
                player.statHeal(stats.prayer, constant = 999, percent = 0)

                player.invAdd(player.inv, MaxGearObjs.armadyl_godsword, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.dragon_claws, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.scythe_of_vitur, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.ghrazi_rapier, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.dragonfire_shield, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.spirit_shield, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.blessed_spirit_shield, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.elysian_spirit_shield, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.spectral_spirit_shield, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.arcane_spirit_shield, 1, strict = false)

                player.invAdd(player.inv, MaxGearObjs.dharok_head, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.dharok_body, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.dharok_legs, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.dharok_weapon, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.inquisitors_helm, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.inquisitors_body, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.inquisitors_skirt, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.inquisitors_mace, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.barrows_gloves, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.avernic_treads, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.ring_of_suffering_ri, 1, strict = false)

                player.invAdd(player.inv, MaxGearObjs.magic_shortbow, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.twisted_bow, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.rune_arrow, 1000, strict = false)
                player.invAdd(player.inv, MaxGearObjs.masori_mask, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.masori_body, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.masori_chaps, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.dizanas_quiver, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.zaryte_vambraces, 1, strict = false)

                player.invAdd(player.inv, MaxGearObjs.ancient_staff, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.kodai_wand, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.toxic_staff_of_the_dead, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.ancestral_hat, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.ancestral_top, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.ancestral_bottom, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.tormented_bracelet, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.occult_necklace, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.imbued_saradomin_cape, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.saturated_heart, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.waterrune, 1000, strict = false)
                player.invAdd(player.inv, MaxGearObjs.chaosrune, 1000, strict = false)
                player.invAdd(player.inv, MaxGearObjs.deathrune, 1000, strict = false)
                player.invAdd(player.inv, MaxGearObjs.bloodrune, 1000, strict = false)
                player.invAdd(player.inv, MaxGearObjs.soulrune, 1000, strict = false)

                player.invAdd(player.inv, MaxGearObjs.super_combat, 1, strict = false)
                player.invAdd(player.inv, MaxGearObjs.anglerfish, 4, strict = false)
                player.invAdd(player.inv, MaxGearObjs.cooked_karambwan, 4, strict = false)
                player.invAdd(player.inv, MaxGearObjs.saradomin_brew, 4, strict = false)
                player.invAdd(player.inv, MaxGearObjs.super_restore, 4, strict = false)
                player.statBoost(stats.attack, constant = 5, percent = 15)
                player.statBoost(stats.strength, constant = 5, percent = 15)
                player.statBoost(stats.defence, constant = 5, percent = 15)

                player.mes("Maxed PK: Torva/Fang, switches, all prayer unlocks, Ancient Magicks.")
            }
        }
    }
}
