package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.herbloreLvl
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.stat.StatType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object HerbObjs : ObjReferences() {
    // Vies (grimy) -> schoon (clean):
    val unidentified_guam = find("unidentified_guam")
    val guam_leaf = find("guam_leaf")
    val unidentified_tarromin = find("unidentified_tarromin")
    val tarromin = find("tarromin")
    val unidentified_harralander = find("unidentified_harralander")
    val harralander = find("harralander")
    val unidentified_ranarr = find("unidentified_ranarr")
    val ranarr_weed = find("ranarr_weed")
    val unidentified_irit = find("unidentified_irit")
    val irit_leaf = find("irit_leaf")
    val unidentified_avantoe = find("unidentified_avantoe")
    val avantoe = find("avantoe")
    val unidentified_kwuarm = find("unidentified_kwuarm")
    val kwuarm = find("kwuarm")
    val unidentified_cadantine = find("unidentified_cadantine")
    val cadantine = find("cadantine")
    val unidentified_dwarf_weed = find("unidentified_dwarf_weed")
    val dwarf_weed = find("dwarf_weed")
    val unidentified_torstol = find("unidentified_torstol")
    val torstol = find("torstol")

    // Vials + secondaries:
    val vial_water = find("vial_water")
    val vial_empty = find("vial_empty")
    val eye_of_newt = find("eye_of_newt")
    val limpwurt_root = find("limpwurt_root")
    val red_spiders_eggs = find("red_spiders_eggs")

    // Onaffe potions:
    val guamvial = find("guamvial")
    val tarrominvial = find("tarrominvial")
    val harralandervial = find("harralandervial")

    // Afgewerkte potions (per dosis):
    val attack3 = find("3dose1attack")
    val attack2 = find("2dose1attack")
    val attack1 = find("1dose1attack")
    val strength3 = find("3dose1strength")
    val strength2 = find("2dose1strength")
    val strength1 = find("1dose1strength")
    val restore3 = find("3dosestatrestore")
}

private class Herb(
    val grimy: ObjType,
    val clean: ObjType,
    val level: Int,
    val xp: Double,
    val name: String,
)

private class Unf(val herb: ObjType, val unf: ObjType, val level: Int, val name: String)

private class Potion(
    val unf: ObjType,
    val secondary: ObjType,
    val result: ObjType,
    val level: Int,
    val xp: Double,
    val name: String,
)

private class Dose(val from: ObjType, val to: ObjType, val stat: StatType, val name: String)

/**
 * HERBLORE (volledige keten).
 *
 *  1. Schoonmaken : klik een vies kruid ('unidentified_...') aan -> schoon kruid + XP.
 *  2. Onaffe potion: schoon kruid op 'vial of water' -> onaffe potion.
 *  3. Afgewerkte potion: onaffe potion + secondary -> drinkbare potion + (grote) XP.
 *  4. Drinken     : klik de potion aan -> tijdelijke stat-boost, dosis loopt af tot lege vial.
 */
class Herblore @Inject constructor() : PluginScript() {
    private val herbs =
        listOf(
            Herb(HerbObjs.unidentified_guam, HerbObjs.guam_leaf, 3, 2.5, "guam leaf"),
            Herb(HerbObjs.unidentified_tarromin, HerbObjs.tarromin, 11, 5.0, "tarromin"),
            Herb(HerbObjs.unidentified_harralander, HerbObjs.harralander, 20, 6.3, "harralander"),
            Herb(HerbObjs.unidentified_ranarr, HerbObjs.ranarr_weed, 25, 7.5, "ranarr weed"),
            Herb(HerbObjs.unidentified_irit, HerbObjs.irit_leaf, 40, 8.8, "irit leaf"),
            Herb(HerbObjs.unidentified_avantoe, HerbObjs.avantoe, 48, 10.0, "avantoe"),
            Herb(HerbObjs.unidentified_kwuarm, HerbObjs.kwuarm, 54, 11.3, "kwuarm"),
            Herb(HerbObjs.unidentified_cadantine, HerbObjs.cadantine, 65, 12.5, "cadantine"),
            Herb(HerbObjs.unidentified_dwarf_weed, HerbObjs.dwarf_weed, 70, 13.8, "dwarf weed"),
            Herb(HerbObjs.unidentified_torstol, HerbObjs.torstol, 75, 15.0, "torstol"),
        )

    private val unfs =
        listOf(
            Unf(HerbObjs.guam_leaf, HerbObjs.guamvial, 3, "guam"),
            Unf(HerbObjs.tarromin, HerbObjs.tarrominvial, 12, "tarromin"),
            Unf(HerbObjs.harralander, HerbObjs.harralandervial, 22, "harralander"),
        )

    private val potions =
        listOf(
            Potion(HerbObjs.guamvial, HerbObjs.eye_of_newt, HerbObjs.attack3, 3, 25.0, "Attack potion"),
            Potion(
                HerbObjs.tarrominvial,
                HerbObjs.limpwurt_root,
                HerbObjs.strength3,
                12,
                50.0,
                "Strength potion",
            ),
            Potion(
                HerbObjs.harralandervial,
                HerbObjs.red_spiders_eggs,
                HerbObjs.restore3,
                22,
                62.5,
                "Restore potion",
            ),
        )

    // Drinken: dosis-afbouw + boost (Attack & Strength).
    private val doses =
        listOf(
            Dose(HerbObjs.attack3, HerbObjs.attack2, stats.attack, "attack"),
            Dose(HerbObjs.attack2, HerbObjs.attack1, stats.attack, "attack"),
            Dose(HerbObjs.attack1, HerbObjs.vial_empty, stats.attack, "attack"),
            Dose(HerbObjs.strength3, HerbObjs.strength2, stats.strength, "strength"),
            Dose(HerbObjs.strength2, HerbObjs.strength1, stats.strength, "strength"),
            Dose(HerbObjs.strength1, HerbObjs.vial_empty, stats.strength, "strength"),
        )

    override fun ScriptContext.startup() {
        for (herb in herbs) {
            onOpHeld1(herb.grimy) { clean(herb) }
        }
        for (u in unfs) {
            onOpHeldU(u.herb, HerbObjs.vial_water) { makeUnf(u) }
        }
        for (p in potions) {
            onOpHeldU(p.unf, p.secondary) { brew(p) }
        }
        for (d in doses) {
            onOpHeld1(d.from) { drink(d) }
        }
    }

    private fun ProtectedAccess.clean(herb: Herb) {
        if (player.herbloreLvl < herb.level) {
            mes("You need a Herblore level of ${herb.level} to clean this herb.")
            return
        }
        invDel(inv, herb.grimy, 1)
        invAdd(inv, herb.clean)
        statAdvance(stats.herblore, PlayerStatMap.toFineXP(herb.xp).toDouble())
        mes("You clean the dirt off the ${herb.name}.")
    }

    private fun ProtectedAccess.makeUnf(u: Unf) {
        if (player.herbloreLvl < u.level) {
            mes("You need a Herblore level of ${u.level} to make this unfinished potion.")
            return
        }
        invDel(inv, u.herb, 1)
        invDel(inv, HerbObjs.vial_water, 1)
        invAdd(inv, u.unf)
        mes("You put the ${u.name} into the vial of water.")
    }

    private fun ProtectedAccess.brew(p: Potion) {
        if (player.herbloreLvl < p.level) {
            mes("You need a Herblore level of ${p.level} to make a ${p.name}.")
            return
        }
        invDel(inv, p.unf, 1)
        invDel(inv, p.secondary, 1)
        invAdd(inv, p.result)
        statAdvance(stats.herblore, PlayerStatMap.toFineXP(p.xp).toDouble())
        mes("You mix the ingredients and make a ${p.name}.")
    }

    private fun ProtectedAccess.drink(d: Dose) {
        invDel(inv, d.from, 1)
        invAdd(inv, d.to)
        statBoost(d.stat, constant = 3, percent = 10)
        mes("You drink some of your potion. Your ${d.name} feels stronger.")
    }
}
