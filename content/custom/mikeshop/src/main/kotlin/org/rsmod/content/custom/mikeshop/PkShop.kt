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
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag
import org.rsmod.routefinder.loc.LocLayerConstants

/** Edgeville-coordinaat van de PvP-world (zelfde als de spawn). */
private val PK_EDGE: CoordGrid = CoordGrid(0, 48, 54, 15, 40)

/** Anker voor de winkel-hub: door Mike gekozen open plek (x=3087, z=3490). Alles bouwt hiervandaan naar het noorden. */
private val PK_HUB: CoordGrid = CoordGrid(0, 48, 54, 15, 34)

/** Zorgt dat de Edgeville PK-NPC's maar een keer worden gespawnd. */
internal object PkShopState {
    var built = false
    /** Werkelijke (mogelijk verschoven) eindcoord van het prikbord, voor de lees-check. */
    var noticeboardCoord: CoordGrid? = null
    var safeFightBoardCoord: CoordGrid? = null
    var wildFightBoardCoord: CoordGrid? = null
}

internal object PkClerkNpcs : NpcReferences() {
    val melee = find("ge_clerk_2")
    val ranged = find("ge_clerk_3")
    val magic = find("ge_clerk_4")
    val supply = find("farming_shopkeeper_1")
    val food = find("warguild_food_shopkeeper")
    val potions = find("warguild_potion_shopkeeper")
    val banker = find("banker1")
}

internal object PkLocs : LocReferences() {
    val bankbooth = find("bankbooth")
    val noticeboard = find("noticeboard")
    val spellbookAltar = find("poh_altar_occult")
}

internal object PkShopInvs : InvReferences() {
    val melee = find("pk_melee_shop")
    val ranged = find("pk_ranged_shop")
    val magic = find("pk_magic_shop")
    val supply = find("pk_supply_shop")
    val food = find("pk_food_shop")
    val potions = find("pk_potion_shop")
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
    val astralrune = find("astralrune")
    // --- Elite Void ---
    val elite_void_top = find("elite_void_knight_top")
    val elite_void_robes = find("elite_void_knight_robes")
    val void_gloves = find("pest_void_knight_gloves")
    val void_melee_helm = find("game_pest_melee_helm")
    val void_ranger_helm = find("game_pest_archer_helm")
    val void_mage_helm = find("game_pest_mage_helm")
    // --- Spec-weapons ---
    val darkbow = find("darkbow")
    val heavy_ballista = find("heavy_ballista")
    val dragon_javelin = find("dragon_javelin")
    val voidwaker = find("voidwaker")
    // --- PvP-armour (Statius/Vesta/Morrigan/Zuriel) ---
    val statius_warhammer = find("statius_warhammer")
    val statius_full_helm = find("statius_full_helm")
    val statius_platebody = find("statius_platebody")
    val statius_platelegs = find("statius_platelegs")
    val vestas_spear = find("vestas_spear")
    val vestas_longsword = find("vestas_longsword")
    val vestas_chainbody = find("vestas_chainbody")
    val vestas_plateskirt = find("vestas_plateskirt")
    val morrigans_thrownaxe = find("morrigans_thrownaxe")
    val morrigans_javelin = find("morrigans_javelin")
    val morrigans_coif = find("morrigans_coif")
    val morrigans_leather_body = find("morrigans_leather_body")
    val morrigans_leather_chaps = find("morrigans_leather_chaps")
    val zuriels_staff = find("zuriels_staff")
    val zuriels_hood = find("zuriels_hood")
    val zuriels_robe_top = find("zuriels_robe_top")
    val zuriels_robe_bottom = find("zuriels_robe_bottom")
    // --- Revenant/wilderness weapons (+50% damage in de wild) ---
    val webweaver_bow = find("wild_cave_webweaver_charged")
    val ursine_chainmace = find("wild_cave_ursine_charged")
    val accursed_sceptre = find("wild_cave_accursed_charged")
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
                    PkObjs.voidwaker,
                    // PvP-armour - Statius + Vesta:
                    PkObjs.statius_warhammer,
                    PkObjs.statius_full_helm,
                    PkObjs.statius_platebody,
                    PkObjs.statius_platelegs,
                    PkObjs.vestas_spear,
                    PkObjs.vestas_longsword,
                    PkObjs.vestas_chainbody,
                    PkObjs.vestas_plateskirt,
                    PkObjs.ursine_chainmace,
                    // Elite Void (volledige set):
                    PkObjs.elite_void_top,
                    PkObjs.elite_void_robes,
                    PkObjs.void_gloves,
                    PkObjs.void_melee_helm,
                    PkObjs.void_ranger_helm,
                    PkObjs.void_mage_helm,
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
            // Spec-weapons:
            stock += stock(PkObjs.darkbow, count = 100, restockCycles = 25)
            stock += stock(PkObjs.heavy_ballista, count = 100, restockCycles = 25)
            stock += stock(PkObjs.dragon_javelin, count = 10000, restockCycles = 5)
            // PvP-armour - Morrigan:
            stock += stock(PkObjs.morrigans_thrownaxe, count = 10000, restockCycles = 5)
            stock += stock(PkObjs.morrigans_javelin, count = 10000, restockCycles = 5)
            stock += stock(PkObjs.morrigans_coif, count = 100, restockCycles = 25)
            stock += stock(PkObjs.morrigans_leather_body, count = 100, restockCycles = 25)
            stock += stock(PkObjs.morrigans_leather_chaps, count = 100, restockCycles = 25)
            stock += stock(PkObjs.webweaver_bow, count = 100, restockCycles = 25)
            // Elite Void (volledige set):
            stock += stock(PkObjs.elite_void_top, count = 100, restockCycles = 25)
            stock += stock(PkObjs.elite_void_robes, count = 100, restockCycles = 25)
            stock += stock(PkObjs.void_gloves, count = 100, restockCycles = 25)
            stock += stock(PkObjs.void_ranger_helm, count = 100, restockCycles = 25)
            stock += stock(PkObjs.void_melee_helm, count = 100, restockCycles = 25)
            stock += stock(PkObjs.void_mage_helm, count = 100, restockCycles = 25)
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
                    // Elite Void (volledige set):
                    PkObjs.elite_void_top,
                    PkObjs.elite_void_robes,
                    PkObjs.void_gloves,
                    PkObjs.void_mage_helm,
                    PkObjs.void_melee_helm,
                    PkObjs.void_ranger_helm,
                    // PvP-armour - Zuriel:
                    PkObjs.zuriels_staff,
                    PkObjs.zuriels_hood,
                    PkObjs.zuriels_robe_top,
                    PkObjs.zuriels_robe_bottom,
                    PkObjs.accursed_sceptre,
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
                    PkObjs.astralrune,
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

internal object PkFoodShopBuilder : InvBuilder() {
    init {
        build("pk_food_shop") {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            stock += stock(PkObjs.shark, count = 1000, restockCycles = 5)
            stock += stock(PkObjs.lobster, count = 1000, restockCycles = 5)
            stock += stock(PkObjs.swordfish, count = 1000, restockCycles = 5)
            stock += stock(PkObjs.monkfish, count = 1000, restockCycles = 5)
            stock += stock(PkObjs.anglerfish, count = 1000, restockCycles = 5)
            stock += stock(PkObjs.dark_crab, count = 1000, restockCycles = 5)
            stock += stock(PkObjs.cooked_karambwan, count = 1000, restockCycles = 5)
        }
    }
}

internal object PkPotionShopBuilder : InvBuilder() {
    init {
        build("pk_potion_shop") {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            stock += stock(PkObjs.saradomin_brew, count = 500, restockCycles = 10)
            stock += stock(PkObjs.super_restore, count = 500, restockCycles = 10)
            stock += stock(PkObjs.prayer_pot4, count = 500, restockCycles = 10)
            stock += stock(PkObjs.super_combat, count = 500, restockCycles = 10)
            stock += stock(PkObjs.divine_combat, count = 500, restockCycles = 10)
            stock += stock(PkObjs.ranging_pot, count = 500, restockCycles = 10)
            stock += stock(PkObjs.magic_pot, count = 500, restockCycles = 10)
            stock += stock(PkObjs.stamina_pot, count = 500, restockCycles = 10)
            stock += stock(PkObjs.super_attack, count = 500, restockCycles = 10)
            stock += stock(PkObjs.super_strength, count = 500, restockCycles = 10)
            stock += stock(PkObjs.attack_pot, count = 500, restockCycles = 10)
            stock += stock(PkObjs.strength_pot, count = 500, restockCycles = 10)
            stock += stock(PkObjs.stat_restore, count = 500, restockCycles = 10)
            stock += stock(PkObjs.prayer_restore, count = 500, restockCycles = 10)
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
    private val collision: CollisionFlagMap,
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
        // Read-only tegel-validatie meteen bij startup loggen (indien de map al geladen is).
        if (pvpWorld) {
            dryRunHubReport()
        }
        // Praat met een PK-shopkeeper -> meteen de bijbehorende gratis winkel.
        onOpNpc1(PkClerkNpcs.melee) { openFree(player, "Free PK Melee Shop", PkShopInvs.melee) }
        onOpNpc1(PkClerkNpcs.ranged) { openFree(player, "Free PK Ranged Shop", PkShopInvs.ranged) }
        onOpNpc1(PkClerkNpcs.magic) { openFree(player, "Free PK Magic Shop", PkShopInvs.magic) }
        onOpNpc1(PkClerkNpcs.supply) {
            openFree(player, "Free PK Supplies Shop", PkShopInvs.supply)
        }
        onOpNpc1(PkClerkNpcs.food) { openFree(player, "Free PK Food Shop", PkShopInvs.food) }
        onOpNpc1(PkClerkNpcs.potions) {
            openFree(player, "Free PK Potions Shop", PkShopInvs.potions)
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
        onCommand("pkfood") {
            desc = "Free PK food shop"
            cheat { openFree(player, "Free PK Food Shop", PkShopInvs.food) }
        }
        onCommand("pkpots") {
            desc = "Free PK potions shop"
            cheat { openFree(player, "Free PK Potions Shop", PkShopInvs.potions) }
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
        when (coords) {
            PkShopState.noticeboardCoord ->
                mesbox(
                    "Edgeville PvP hub<br>" +
                        "::pkready returns here and refreshes Attack.<br>" +
                        "::pkshop opens free gear shops.<br>" +
                        "::brid / ::veng / ::pure / ::tank give fast loadouts.<br>" +
                        "::restock refills supplies after a fight.<br>" +
                        "::resetfight restores HP, prayer, stats, spec, skull and teleblock.<br>" +
                        "::duelarea stages south of the ditch; ::wildfight jumps north."
                )
            PkShopState.safeFightBoardCoord ->
                mesbox(
                    "Quick fights - staging side<br>" +
                        "Use ::resetfight, choose a loadout, then step north over the ditch.<br>" +
                        "::skull toggles a voluntary skull for risk tests.<br>" +
                        "Both players can start from here when testing skull prevention."
                )
            PkShopState.wildFightBoardCoord ->
                mesbox(
                    "Quick fights - wilderness side<br>" +
                        "This side uses wilderness combat range checks.<br>" +
                        "::wildfight brings you here directly.<br>" +
                        "Use ::teleblock or ::tb name to test teleport-blocked escapes."
                )
        }
    }

    // Tegels die als 'bezet/geblokkeerd' tellen voor plaatsing: niet-beloopbaar of een loc erop.
    private val blockedMask = CollisionFlag.BLOCK_WALK or CollisionFlag.LOC

    /**
     * Bouwt de complete PK-hub rond PK_HUB (3087,3490), noordwaarts. Elke spawn wordt eerst
     * gevalideerd: alleen op een geladen, beloopbare, object-vrije en niet-reeds-gebruikte tegel.
     * Geblokkeerde plekken worden automatisch verschoven naar de dichtstbijzijnde vrije tegel.
     * Elke beslissing wordt gelogd ([PK-HUB] object -> coord -> vrij JA/NEE).
     */
    private fun spawnEdgevilleShops() {
        println("[PK-HUB] === Plaatsing PK-hub rond ${PK_HUB.x},${PK_HUB.z} (noordwaarts) met tegel-validatie ===")
        val used = HashSet<CoordGrid>()
        // Twee rijen van 3 winkeliers (de bestaande gratis PK-shops).
        placeNpc("shop:gear/melee", PkClerkNpcs.melee, PK_HUB.translate(-1, 2), used)
        placeNpc("shop:food", PkClerkNpcs.food, PK_HUB.translate(0, 2), used)
        placeNpc("shop:potions", PkClerkNpcs.potions, PK_HUB.translate(1, 2), used)
        placeNpc("shop:runes/magic", PkClerkNpcs.magic, PK_HUB.translate(-1, 3), used)
        placeNpc("shop:ammo/ranged", PkClerkNpcs.ranged, PK_HUB.translate(0, 3), used)
        placeNpc("shop:supplies", PkClerkNpcs.supply, PK_HUB.translate(1, 3), used)
        // Bank: banker + 2 booths achter de winkelrijen (noordkant).
        placeNpc("banker", PkClerkNpcs.banker, PK_HUB.translate(0, 5), used)
        placeLoc("bank booth (west)", PkLocs.bankbooth, PK_HUB.translate(-1, 5), LocAngle.South, used)
        placeLoc("bank booth (oost)", PkLocs.bankbooth, PK_HUB.translate(1, 5), LocAngle.South, used)
        // Prikbord op de LINKER-flank (west), spellbook-altaar op de RECHTER-flank (oost).
        PkShopState.noticeboardCoord =
            placeLoc("noticeboard (links)", PkLocs.noticeboard, PK_HUB.translate(-3, 2), LocAngle.East, used)
        placeLoc("spellbook altar (rechts)", PkLocs.spellbookAltar, PK_HUB.translate(3, 2), LocAngle.West, used)
        // Quick-fight markers bij de Edgeville ditch: zuid = klaarzetten, noord = wilderness-test.
        PkShopState.safeFightBoardCoord =
            placeLoc("quick-fight board (zuid)", PkLocs.noticeboard, CoordGrid(0, 48, 54, 13, 61), LocAngle.East, used)
        PkShopState.wildFightBoardCoord =
            placeLoc("quick-fight board (wild)", PkLocs.noticeboard, CoordGrid(0, 48, 55, 13, 2), LocAngle.East, used)
        println("[PK-HUB] === Plaatsing voltooid ===")
    }

    /** Geladen, beloopbaar, object-vrij en nog niet door ons bezet. */
    private fun tileFree(c: CoordGrid, used: Set<CoordGrid>): Boolean {
        if (c in used) return false
        if (!collision.isZoneAllocated(c.x, c.z, c.level)) return false
        return (collision[c.x, c.z, c.level] and blockedMask) == 0
    }

    /** Gewenste tegel als die vrij is, anders de dichtstbijzijnde vrije tegel (ringen radius 1..4). */
    private fun resolveTile(preferred: CoordGrid, used: Set<CoordGrid>): CoordGrid? {
        if (tileFree(preferred, used)) return preferred
        for (r in 1..4) {
            for (dz in -r..r) {
                for (dx in -r..r) {
                    if (maxOf(kotlin.math.abs(dx), kotlin.math.abs(dz)) != r) continue
                    val c = preferred.translate(dx, dz)
                    if (tileFree(c, used)) return c
                }
            }
        }
        return null
    }

    private fun placeNpc(
        label: String,
        ref: NpcType,
        preferred: CoordGrid,
        used: MutableSet<CoordGrid>,
    ) {
        val coord = resolveTile(preferred, used)
        if (coord == null) {
            println("[PK-HUB] $label -> ${preferred.x},${preferred.z} -> vrij NEE -> geen vrije tegel, OVERGESLAGEN")
            return
        }
        report(label, preferred, coord)
        used += coord
        spawnNpc(ref, coord)
    }

    private fun placeLoc(
        label: String,
        ref: LocType,
        preferred: CoordGrid,
        angle: LocAngle,
        used: MutableSet<CoordGrid>,
    ): CoordGrid? {
        val coord = resolveTile(preferred, used)
        if (coord == null) {
            println("[PK-HUB] $label -> ${preferred.x},${preferred.z} -> vrij NEE -> geen vrije tegel, OVERGESLAGEN")
            return null
        }
        report(label, preferred, coord)
        used += coord
        spawnLoc(ref, coord, angle)
        return coord
    }

    private fun report(label: String, preferred: CoordGrid, coord: CoordGrid) {
        val moved = if (coord != preferred) " (VERSCHOVEN van ${preferred.x},${preferred.z})" else ""
        println("[PK-HUB] $label -> ${coord.x},${coord.z},l${coord.level} -> vrij JA$moved")
    }

    /** Read-only tegel-validatie bij startup (zonder spawnen), zodat het rapport direct in het log staat. */
    private fun dryRunHubReport() {
        if (!collision.isZoneAllocated(PK_HUB.x, PK_HUB.z, PK_HUB.level)) {
            println("[PK-HUB-DRYRUN] Collision-map nog niet geladen bij startup; rapport verschijnt bij de eerste login.")
            return
        }
        println("[PK-HUB-DRYRUN] === Tegel-validatie (read-only) rond ${PK_HUB.x},${PK_HUB.z} ===")
        val used = HashSet<CoordGrid>()
        val plan =
            listOf(
                "shop:gear/melee" to PK_HUB.translate(-1, 2),
                "shop:food" to PK_HUB.translate(0, 2),
                "shop:potions" to PK_HUB.translate(1, 2),
                "shop:runes/magic" to PK_HUB.translate(-1, 3),
                "shop:ammo/ranged" to PK_HUB.translate(0, 3),
                "shop:supplies" to PK_HUB.translate(1, 3),
                "banker" to PK_HUB.translate(0, 5),
                "bank booth (west)" to PK_HUB.translate(-1, 5),
                "bank booth (oost)" to PK_HUB.translate(1, 5),
                "noticeboard (links)" to PK_HUB.translate(-3, 2),
                "spellbook altar (rechts)" to PK_HUB.translate(3, 2),
                "quick-fight board (zuid)" to CoordGrid(0, 48, 54, 13, 61),
                "quick-fight board (wild)" to CoordGrid(0, 48, 55, 13, 2),
            )
        for ((label, pref) in plan) {
            val c = resolveTile(pref, used)
            if (c == null) {
                println("[PK-HUB-DRYRUN] $label -> ${pref.x},${pref.z} -> vrij NEE -> geen vrije tegel")
            } else {
                used += c
                val moved = if (c != pref) " (VERSCHOVEN van ${pref.x},${pref.z})" else ""
                println("[PK-HUB-DRYRUN] $label -> ${c.x},${c.z},l${c.level} -> vrij JA$moved")
            }
        }
        println("[PK-HUB-DRYRUN] === einde ===")
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
