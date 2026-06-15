package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.builders.inv.InvBuilder
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.inv.InvType
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.loc.LocLayerConstants

/** Edgeville-coordinaat van de PvP-world (zelfde als de spawn). */
private val PK_EDGE: CoordGrid = CoordGrid(0, 48, 54, 15, 40)

/** Zorgt dat de Edgeville PK-NPC's maar een keer worden gespawnd. */
internal object PkShopState {
    var built = false
}

internal object PkClerkNpcs : NpcReferences() {
    val melee = find("ge_clerk_2")
    val ranged = find("ge_clerk_3")
    val magic = find("ge_clerk_4")
    val supply = find("farming_shopkeeper_1")
    val banker = find("banker1")
}

internal object PkLocs : LocReferences() {
    val bankbooth = find("bankbooth")
    val noticeboard = find("noticeboard")
}

internal object PkShopInvs : InvReferences() {
    val melee = find("pk_melee_shop")
    val ranged = find("pk_ranged_shop")
    val magic = find("pk_magic_shop")
    val supply = find("pk_supply_shop")
}

internal object PkObjs : ObjReferences() {
    // --- Melee weapons ---
    val abyssal_whip = find("abyssal_whip")
    val dragon_scimitar = find("dragon_scimitar")
    val dragon_dagger_p = find("dragon_dagger_p")
    val dragon_claws = find("dragon_claws")
    val granite_maul = find("granite_maul")
    val dragon_longsword = find("dragon_longsword")
    val rune_scimitar = find("rune_scimitar")
    val dragon_mace = find("dragon_mace")
    val armadyl_godsword = find("ags")
    val osmumtens_fang = find("osmumtens_fang")
    // --- Melee armour ---
    val neitiznot_faceguard = find("neitiznot_faceguard")
    val slayer_helm_i = find("slayer_helm_i")
    val rune_full_helm = find("rune_full_helm")
    val bandos_chestplate = find("bandos_chestplate")
    val rune_platebody = find("rune_platebody")
    val bandos_skirt = find("bandos_skirt")
    val rune_platelegs = find("rune_platelegs")
    val rune_kiteshield = find("rune_kiteshield")
    val dharok_head = find("barrows_dharok_head")
    val dharok_body = find("barrows_dharok_body")
    val dharok_legs = find("barrows_dharok_legs")
    val dharok_weapon = find("barrows_dharok_weapon")
    val torva_helm = find("torva_helm")
    val torva_chest = find("torva_chest")
    val torva_legs = find("torva_legs")
    val inquisitors_helm = find("inquisitors_helm")
    val inquisitors_body = find("inquisitors_body")
    val inquisitors_skirt = find("inquisitors_skirt")
    val inquisitors_mace = find("inquisitors_mace")
    // --- Accessories ---
    val dragon_boots = find("dragon_boots")
    val primordial_boots = find("primordial_boots")
    val ferocious_gloves = find("ferocious_gloves")
    val infernal_cape = find("infernal_cape")
    val imbued_saradomin_cape = find("ma2_saradomin_cape")
    val imbued_guthix_cape = find("ma2_guthix_cape")
    val imbued_zamorak_cape = find("ma2_zamorak_cape")
    val amulet_of_strength = find("amulet_of_strength")
    val amulet_of_glory = find("amulet_of_glory")
    val ultor_ring = find("ultor_ring")
    val ring_of_suffering_ri = find("nzone_zenyte_ring_enchanted_recoil")
    val barrows_gloves = find("hundred_gauntlets_level_10")
    val tormented_bracelet = find("zenyte_bracelet_enchanted")
    val spirit_shield = find("spirit_shield")
    val blessed_spirit_shield = find("blessed_spirit_shield")
    val elysian_spirit_shield = find("elysian")
    val spectral_spirit_shield = find("spectral")
    val arcane_spirit_shield = find("arcane")
    val avernic_treads = find("avernic_treads")
    // --- Ranged ---
    val magic_shortbow = find("magic_shortbow")
    val magic_longbow = find("magic_longbow")
    val toxic_blowpipe = find("toxic_blowpipe")
    val rune_arrow = find("rune_arrow")
    val adamant_arrow = find("adamant_arrow")
    val karil_head = find("barrows_karil_head")
    val karil_body = find("barrows_karil_body")
    val karil_legs = find("barrows_karil_legs")
    val karil_weapon = find("barrows_karil_weapon")
    val masori_mask = find("masori_mask_fortified")
    val masori_body = find("masori_body_fortified")
    val masori_chaps = find("masori_chaps_fortified")
    val dizanas_quiver = find("dizanas_quiver_infinite")
    val zaryte_vambraces = find("zaryte_vambraces")
    // --- Magic ---
    val merfolk_trident = find("merfolk_trident")
    val staff_of_fire = find("staff_of_fire")
    val ancient_staff = find("staff_of_zaros")
    val toxic_staff_of_the_dead = find("toxic_sotd_charged")
    val mystic_hat = find("mystic_hat")
    val mystic_robe_top = find("mystic_robe_top")
    val mystic_robe_bottom = find("mystic_robe_bottom")
    val ahrim_head = find("barrows_ahrim_head")
    val ahrim_body = find("barrows_ahrim_body")
    val ahrim_legs = find("barrows_ahrim_legs")
    val ahrim_weapon = find("barrows_ahrim_weapon")
    val occult_necklace = find("occult_necklace")
    val saturated_heart = find("saturated_heart")
    // --- Runes ---
    val airrune = find("airrune")
    val waterrune = find("waterrune")
    val earthrune = find("earthrune")
    val firerune = find("firerune")
    val mindrune = find("mindrune")
    val bodyrune = find("bodyrune")
    val deathrune = find("deathrune")
    val bloodrune = find("bloodrune")
    val chaosrune = find("chaosrune")
    val naturerune = find("naturerune")
    val lawrune = find("lawrune")
    val soulrune = find("soulrune")
    val cosmicrune = find("cosmicrune")
    // --- Supplies ---
    val shark = find("shark")
    val lobster = find("lobster")
    val swordfish = find("swordfish")
    val monkfish = find("monkfish")
    val super_combat = find("4dose2combat")
    val attack_pot = find("3dose1attack")
    val strength_pot = find("3dose1strength")
    val stat_restore = find("3dosestatrestore")
    val prayer_restore = find("3doseprayerrestore")
    val looting_bag = find("looting_bag")
    val prayer_scroll_rigour = find("raids_prayerscroll")
    val prayer_scroll_preserve = find("raids_prayerscroll_preserve")
    val prayer_scroll_augury = find("raids_prayerscroll_augury")
    val prayer_scroll_deadeye = find("deadeye_prayer_scroll")
    val prayer_scroll_mystic_vigour = find("mystic_vigour_prayer_scroll")

    // === Uitbreiding ===
    // Melee weapons:
    val dragon_warhammer = find("dragon_warhammer")
    val elder_maul = find("elder_maul")
    val ghrazi_rapier = find("ghrazi_rapier")
    val saradomin_sword = find("saradomin_sword")
    val dragon_halberd = find("dragon_halberd")
    val abyssal_dagger = find("abyssal_dagger")
    val verac_weapon = find("barrows_verac_weapon")
    val guthan_weapon = find("barrows_guthan_weapon")
    val torag_weapon = find("barrows_torag_weapon")
    // Melee armour:
    val justiciar_faceguard = find("justiciar_faceguard")
    val justiciar_chestguard = find("justiciar_chestguard")
    val justiciar_leg_guards = find("justiciar_leg_guards")
    val serpentine_helm = find("serpentine_helm")
    val torture = find("br_torture_amulet")
    // Ranged armour + weapons:
    val black_dhide_body = find("black_dragonhide_body")
    val black_dhide_chaps = find("black_dragonhide_chaps")
    val armadyl_helmet = find("armadyl_helmet")
    val armadyl_chestplate = find("armadyl_chestplate")
    val avas_assembler = find("avas_assembler")
    val twisted_bow = find("twisted_bow")
    val crystal_bow = find("crystal_bow")
    val dragon_knife = find("dragon_knife")
    val dragon_dart = find("dragon_dart")
    val dragon_arrow = find("dragon_arrow")
    val anguish = find("br_anguish_necklace")
    // Magic weapons + armour:
    val kodai_wand = find("kodai_wand")
    val master_wand = find("br_master_wand")
    val sanguinesti_staff = find("sanguinesti_staff")
    val ancestral_hat = find("ancestral_hat")
    val ancestral_top = find("ancestral_robe_top")
    val ancestral_bottom = find("ancestral_robe_bottom")
    val infinity_top = find("magictraining_infinitytop")
    val infinity_hat = find("magictraining_infinityhat")
    val infinity_bottom = find("magictraining_infinitybottom")
    val eternal_boots = find("eternal_boots")
    val divine_rune_pouch = find("divine_rune_pouch")
    // Supplies:
    val anglerfish = find("anglerfish")
    val dark_crab = find("dark_crab")
    val cooked_karambwan = find("tbwt_cooked_karambwan")
    val saradomin_brew = find("4dosepotionofsaradomin")
    val super_restore = find("4dose2restore")
    val prayer_pot4 = find("4doseprayerrestore")
    val ranging_pot = find("4doserangerspotion")
    val magic_pot = find("4dose1magic")
    val stamina_pot = find("4dosestamina")
    val divine_combat = find("4dosedivinecombat")
    val super_attack = find("4dose2attack")
    val super_strength = find("4dose2strength")
    // Cosmetics:
    val santa_hat = find("santa_hat")
    val robinhoodhat = find("robinhoodhat")
    val halloweenmask_red = find("halloweenmask_red")
    val black_partyhat = find("black_partyhat")
}

internal object PkMeleeShopBuilder : InvBuilder() {
    init {
        build("pk_melee_shop") {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            val gear =
                listOf(
                    PkObjs.abyssal_whip,
                    PkObjs.dragon_scimitar,
                    PkObjs.dragon_dagger_p,
                    PkObjs.dragon_claws,
                    PkObjs.granite_maul,
                    PkObjs.dragon_longsword,
                    PkObjs.rune_scimitar,
                    PkObjs.dragon_mace,
                    PkObjs.armadyl_godsword,
                    PkObjs.osmumtens_fang,
                    PkObjs.neitiznot_faceguard,
                    PkObjs.slayer_helm_i,
                    PkObjs.rune_full_helm,
                    PkObjs.bandos_chestplate,
                    PkObjs.rune_platebody,
                    PkObjs.bandos_skirt,
                    PkObjs.rune_platelegs,
                    PkObjs.rune_kiteshield,
                    PkObjs.dharok_head,
                    PkObjs.dharok_body,
                    PkObjs.dharok_legs,
                    PkObjs.dharok_weapon,
                    PkObjs.torva_helm,
                    PkObjs.torva_chest,
                    PkObjs.torva_legs,
                    PkObjs.inquisitors_helm,
                    PkObjs.inquisitors_body,
                    PkObjs.inquisitors_skirt,
                    PkObjs.inquisitors_mace,
                    PkObjs.dragon_boots,
                    PkObjs.primordial_boots,
                    PkObjs.ferocious_gloves,
                    PkObjs.infernal_cape,
                    PkObjs.amulet_of_strength,
                    PkObjs.amulet_of_glory,
                    PkObjs.ultor_ring,
                    // Uitbreiding:
                    PkObjs.dragon_warhammer,
                    PkObjs.elder_maul,
                    PkObjs.ghrazi_rapier,
                    PkObjs.saradomin_sword,
                    PkObjs.dragon_halberd,
                    PkObjs.abyssal_dagger,
                    PkObjs.verac_weapon,
                    PkObjs.guthan_weapon,
                    PkObjs.torag_weapon,
                    PkObjs.justiciar_faceguard,
                    PkObjs.justiciar_chestguard,
                    PkObjs.justiciar_leg_guards,
                    PkObjs.serpentine_helm,
                    PkObjs.torture,
                )
            for (obj in gear) {
                stock += stock(obj, count = 100, restockCycles = 25)
            }
        }
    }
}

internal object PkRangedShopBuilder : InvBuilder() {
    init {
        build("pk_ranged_shop") {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            stock += stock(PkObjs.magic_shortbow, count = 100, restockCycles = 25)
            stock += stock(PkObjs.magic_longbow, count = 100, restockCycles = 25)
            stock += stock(PkObjs.toxic_blowpipe, count = 50, restockCycles = 50)
            stock += stock(PkObjs.rune_arrow, count = 10000, restockCycles = 5)
            stock += stock(PkObjs.adamant_arrow, count = 10000, restockCycles = 5)
            stock += stock(PkObjs.karil_head, count = 100, restockCycles = 25)
            stock += stock(PkObjs.karil_body, count = 100, restockCycles = 25)
            stock += stock(PkObjs.karil_legs, count = 100, restockCycles = 25)
            stock += stock(PkObjs.karil_weapon, count = 100, restockCycles = 25)
            stock += stock(PkObjs.masori_mask, count = 100, restockCycles = 25)
            stock += stock(PkObjs.masori_body, count = 100, restockCycles = 25)
            stock += stock(PkObjs.masori_chaps, count = 100, restockCycles = 25)
            stock += stock(PkObjs.dizanas_quiver, count = 100, restockCycles = 25)
            stock += stock(PkObjs.zaryte_vambraces, count = 100, restockCycles = 25)
            // Uitbreiding - ranged armour:
            stock += stock(PkObjs.black_dhide_body, count = 100, restockCycles = 25)
            stock += stock(PkObjs.black_dhide_chaps, count = 100, restockCycles = 25)
            stock += stock(PkObjs.armadyl_helmet, count = 100, restockCycles = 25)
            stock += stock(PkObjs.armadyl_chestplate, count = 100, restockCycles = 25)
            stock += stock(PkObjs.avas_assembler, count = 100, restockCycles = 25)
            stock += stock(PkObjs.anguish, count = 100, restockCycles = 25)
            // Uitbreiding - ranged weapons:
            stock += stock(PkObjs.twisted_bow, count = 50, restockCycles = 50)
            stock += stock(PkObjs.crystal_bow, count = 100, restockCycles = 25)
            stock += stock(PkObjs.dragon_knife, count = 5000, restockCycles = 5)
            stock += stock(PkObjs.dragon_dart, count = 10000, restockCycles = 5)
            stock += stock(PkObjs.dragon_arrow, count = 10000, restockCycles = 5)
        }
    }
}

internal object PkMagicShopBuilder : InvBuilder() {
    init {
        build("pk_magic_shop") {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            val gear =
                listOf(
                    PkObjs.merfolk_trident,
                    PkObjs.staff_of_fire,
                    PkObjs.ancient_staff,
                    PkObjs.toxic_staff_of_the_dead,
                    PkObjs.mystic_hat,
                    PkObjs.mystic_robe_top,
                    PkObjs.mystic_robe_bottom,
                    PkObjs.ahrim_head,
                    PkObjs.ahrim_body,
                    PkObjs.ahrim_legs,
                    PkObjs.ahrim_weapon,
                    PkObjs.occult_necklace,
                    PkObjs.tormented_bracelet,
                    PkObjs.saturated_heart,
                    PkObjs.imbued_saradomin_cape,
                    PkObjs.imbued_guthix_cape,
                    PkObjs.imbued_zamorak_cape,
                    // Uitbreiding:
                    PkObjs.kodai_wand,
                    PkObjs.master_wand,
                    PkObjs.sanguinesti_staff,
                    PkObjs.ancestral_hat,
                    PkObjs.ancestral_top,
                    PkObjs.ancestral_bottom,
                    PkObjs.infinity_hat,
                    PkObjs.infinity_top,
                    PkObjs.infinity_bottom,
                    PkObjs.eternal_boots,
                    PkObjs.divine_rune_pouch,
                    PkObjs.arcane_spirit_shield,
                )
            for (obj in gear) {
                stock += stock(obj, count = 100, restockCycles = 25)
            }
            val runes =
                listOf(
                    PkObjs.airrune,
                    PkObjs.waterrune,
                    PkObjs.earthrune,
                    PkObjs.firerune,
                    PkObjs.mindrune,
                    PkObjs.bodyrune,
                    PkObjs.deathrune,
                    PkObjs.bloodrune,
                    PkObjs.chaosrune,
                    PkObjs.naturerune,
                    PkObjs.lawrune,
                    PkObjs.soulrune,
                    PkObjs.cosmicrune,
                )
            for (rune in runes) {
                stock += stock(rune, count = 25000, restockCycles = 3)
            }
        }
    }
}

internal object PkSupplyShopBuilder : InvBuilder() {
    init {
        build("pk_supply_shop") {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            stock += stock(PkObjs.shark, count = 1000, restockCycles = 5)
            stock += stock(PkObjs.lobster, count = 1000, restockCycles = 5)
            stock += stock(PkObjs.swordfish, count = 1000, restockCycles = 5)
            stock += stock(PkObjs.monkfish, count = 1000, restockCycles = 5)
            stock += stock(PkObjs.super_combat, count = 500, restockCycles = 10)
            stock += stock(PkObjs.attack_pot, count = 500, restockCycles = 10)
            stock += stock(PkObjs.strength_pot, count = 500, restockCycles = 10)
            stock += stock(PkObjs.stat_restore, count = 500, restockCycles = 10)
            stock += stock(PkObjs.prayer_restore, count = 500, restockCycles = 10)
            stock += stock(PkObjs.looting_bag, count = 100, restockCycles = 25)
            stock += stock(PkObjs.barrows_gloves, count = 100, restockCycles = 25)
            stock += stock(PkObjs.ring_of_suffering_ri, count = 100, restockCycles = 25)
            stock += stock(PkObjs.avernic_treads, count = 100, restockCycles = 25)
            stock += stock(PkObjs.spirit_shield, count = 100, restockCycles = 25)
            stock += stock(PkObjs.blessed_spirit_shield, count = 100, restockCycles = 25)
            stock += stock(PkObjs.elysian_spirit_shield, count = 100, restockCycles = 25)
            stock += stock(PkObjs.spectral_spirit_shield, count = 100, restockCycles = 25)
            stock += stock(PkObjs.arcane_spirit_shield, count = 100, restockCycles = 25)
            stock += stock(PkObjs.prayer_scroll_rigour, count = 100, restockCycles = 25)
            stock += stock(PkObjs.prayer_scroll_preserve, count = 100, restockCycles = 25)
            stock += stock(PkObjs.prayer_scroll_augury, count = 100, restockCycles = 25)
            stock += stock(PkObjs.prayer_scroll_deadeye, count = 100, restockCycles = 25)
            stock += stock(PkObjs.prayer_scroll_mystic_vigour, count = 100, restockCycles = 25)
            // Uitbreiding - food:
            stock += stock(PkObjs.anglerfish, count = 1000, restockCycles = 5)
            stock += stock(PkObjs.dark_crab, count = 1000, restockCycles = 5)
            stock += stock(PkObjs.cooked_karambwan, count = 1000, restockCycles = 5)
            // Uitbreiding - potions:
            stock += stock(PkObjs.saradomin_brew, count = 500, restockCycles = 10)
            stock += stock(PkObjs.super_restore, count = 500, restockCycles = 10)
            stock += stock(PkObjs.prayer_pot4, count = 500, restockCycles = 10)
            stock += stock(PkObjs.ranging_pot, count = 500, restockCycles = 10)
            stock += stock(PkObjs.magic_pot, count = 500, restockCycles = 10)
            stock += stock(PkObjs.stamina_pot, count = 500, restockCycles = 10)
            stock += stock(PkObjs.divine_combat, count = 500, restockCycles = 10)
            stock += stock(PkObjs.super_attack, count = 500, restockCycles = 10)
            stock += stock(PkObjs.super_strength, count = 500, restockCycles = 10)
        }
    }
}

/**
 * ::pkshop -> keuzemenu met 4 GRATIS PK-winkels (Melee / Ranged / Magic / Supplies). Alles kost 0
 * coins (buyPercentage = 0.0), zodat spelers op de PvP-world (Edgeville) zich meteen kunnen gearen.
 * Losse commando's: ::pkmelee ::pkranged ::pkmagic ::pksupplies.
 */
class PkShop
@Inject
constructor(
    private val shops: Shops,
    private val protectedAccess: ProtectedAccessLauncher,
    private val locRepo: LocRepository,
    private val locTypes: LocTypeList,
    private val npcRepo: NpcRepository,
    private val npcTypes: NpcTypeList,
) : PluginScript() {
    // Alleen de PvP-world (RSMOD_WORLD=2) krijgt de Edgeville PK-NPC's.
    private val pvpWorld = System.getenv("RSMOD_WORLD") == "2"

    override fun ScriptContext.startup() {
        // Op de PvP-world: spawn eenmalig een PK-shopkeeper + banker in Edgeville.
        onPlayerLogin {
            if (pvpWorld && !PkShopState.built) {
                PkShopState.built = true
                spawnEdgevilleShops()
            }
        }
        // Praat met een PK-shopkeeper -> meteen de bijbehorende gratis winkel.
        onOpNpc1(PkClerkNpcs.melee) { openFree(player, "Free PK Melee Shop", PkShopInvs.melee) }
        onOpNpc1(PkClerkNpcs.ranged) { openFree(player, "Free PK Ranged Shop", PkShopInvs.ranged) }
        onOpNpc1(PkClerkNpcs.magic) { openFree(player, "Free PK Magic Shop", PkShopInvs.magic) }
        onOpNpc1(PkClerkNpcs.supply) {
            openFree(player, "Free PK Supplies Shop", PkShopInvs.supply)
        }
        onOpLoc2(PkLocs.bankbooth) { openEdgevilleBank() }
        onOpLoc1(PkLocs.noticeboard) { readEdgevilleNoticeboard(it.loc.coords) }

        onCommand("pkshop") {
            desc = "Open the free PK shop menu (Melee/Ranged/Magic/Supplies)"
            cheat { protectedAccess.launch(player) { openMenu() } }
        }
        onCommand("pkmelee") {
            desc = "Free PK melee shop"
            cheat { openFree(player, "Free PK Melee Shop", PkShopInvs.melee) }
        }
        onCommand("pkranged") {
            desc = "Free PK ranged shop"
            cheat { openFree(player, "Free PK Ranged Shop", PkShopInvs.ranged) }
        }
        onCommand("pkmagic") {
            desc = "Free PK magic shop"
            cheat { openFree(player, "Free PK Magic Shop", PkShopInvs.magic) }
        }
        onCommand("pksupplies") {
            desc = "Free PK supplies shop"
            cheat { openFree(player, "Free PK Supplies Shop", PkShopInvs.supply) }
        }
    }

    private suspend fun ProtectedAccess.openMenu() {
        val pick =
            choice5(
                "Melee gear",
                1,
                "Ranged gear",
                2,
                "Magic gear + runes",
                3,
                "Food & potions",
                4,
                "Nothing",
                0,
                title = "Free PK Shop - pick a category",
            )
        when (pick) {
            1 -> openFree(player, "Free PK Melee Shop", PkShopInvs.melee)
            2 -> openFree(player, "Free PK Ranged Shop", PkShopInvs.ranged)
            3 -> openFree(player, "Free PK Magic Shop", PkShopInvs.magic)
            4 -> openFree(player, "Free PK Supplies Shop", PkShopInvs.supply)
        }
    }

    private fun openFree(player: Player, title: String, inv: InvType) {
        shops.open(
            player = player,
            title = title,
            shopInv = inv,
            buyPercentage = 0.0,
            sellPercentage = 0.0,
            changePercentage = 0.0,
        )
        player.mes("Everything here is free - gear up and PK!")
    }

    private fun ProtectedAccess.openEdgevilleBank() {
        ifOpenMainSidePair(main = interfaces.bank_main, side = interfaces.bank_side)
    }

    private suspend fun ProtectedAccess.readEdgevilleNoticeboard(coords: CoordGrid) {
        if (coords != PK_EDGE.translate(-1, 2)) {
            return
        }
        mesbox(
            "Edgeville PvP hub<br>" +
                "::pkshop opens free gear shops.<br>" +
                "::pkpoints shows your kills and points.<br>" +
                "::pkspend opens the cosmetics shop.<br>" +
                "Skull prevention is respected; wilderness level ranges apply north of the ditch."
        )
    }

    /** Spawnt de PK-shopkeeper + een banker in Edgeville (PvP-world). NPC-spawns try/catch. */
    private fun spawnEdgevilleShops() {
        spawnNpc(PkClerkNpcs.melee, PK_EDGE.translate(1, 2))
        spawnNpc(PkClerkNpcs.ranged, PK_EDGE.translate(2, 2))
        spawnNpc(PkClerkNpcs.magic, PK_EDGE.translate(3, 2))
        spawnNpc(PkClerkNpcs.supply, PK_EDGE.translate(4, 2))
        spawnNpc(PkClerkNpcs.banker, PK_EDGE.translate(-2, 2))
        spawnLoc(PkLocs.bankbooth, PK_EDGE.translate(-3, 2), LocAngle.East)
        spawnLoc(PkLocs.noticeboard, PK_EDGE.translate(-1, 2), LocAngle.South)
    }

    private fun spawnNpc(ref: NpcType, coords: CoordGrid) {
        try {
            val npc = Npc(npcTypes[ref], coords)
            npc.mode = NpcMode.None
            npcRepo.add(npc, duration = Int.MAX_VALUE)
        } catch (e: Exception) {
            // Tegel geblokkeerd / npc niet plaatsbaar -> sla over, crash de server niet.
        }
    }

    private fun spawnLoc(ref: LocType, coords: CoordGrid, angle: LocAngle) {
        try {
            val type = locTypes[ref]
            val shape = LocShape.CentrepieceStraight.id
            val loc = LocInfo(LocLayerConstants.of(shape), coords, LocEntity(type.id, shape, angle.id))
            locRepo.add(loc, duration = Int.MAX_VALUE)
        } catch (e: Exception) {
            // Tegel geblokkeerd / loc niet plaatsbaar -> sla over, crash de server niet.
        }
    }
}
