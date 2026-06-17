package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object FoodObjs : ObjReferences() {
    val bread = find("bread")
    val cooked_meat = find("cooked_meat")
    val cooked_chicken = find("cooked_chicken")
    val shrimp = find("shrimp")
    val trout = find("trout")
    val salmon = find("salmon")
    val tuna = find("tuna")
    val lobster = find("lobster")
    val swordfish = find("swordfish")
    val shark = find("shark")
    val monkfish = find("monkfish")
    val anglerfish = find("anglerfish")
    val dark_crab = find("dark_crab")
    val karambwan = find("tbwt_cooked_karambwan")

    val vial_empty = find("vial_empty")

    // Super combat
    val combat4 = find("4dose2combat")
    val combat3 = find("3dose2combat")
    val combat2 = find("2dose2combat")
    val combat1 = find("1dose2combat")

    // Super restore
    val restore4 = find("4dose2restore")
    val restore3 = find("3dose2restore")
    val restore2 = find("2dose2restore")
    val restore1 = find("1dose2restore")

    // Prayer potion
    val prayer4 = find("4doseprayerrestore")
    val prayer3 = find("3doseprayerrestore")
    val prayer2 = find("2doseprayerrestore")
    val prayer1 = find("1doseprayerrestore")

    // Saradomin brew
    val brew4 = find("4dosepotionofsaradomin")
    val brew3 = find("3dosepotionofsaradomin")
    val brew2 = find("2dosepotionofsaradomin")
    val brew1 = find("1dosepotionofsaradomin")

    // Divine super combat
    val divine4 = find("4dosedivinecombat")
    val divine3 = find("3dosedivinecombat")
    val divine2 = find("2dosedivinecombat")
    val divine1 = find("1dosedivinecombat")

    // Bastion
    val bastion4 = find("4dosebastion")
    val bastion3 = find("3dosebastion")
    val bastion2 = find("2dosebastion")
    val bastion1 = find("1dosebastion")
}

private class FoodItem(val obj: ObjType, val heal: Int, val name: String)

private class Potion(
    val doses: List<ObjType>,
    val message: String,
    val effect: ProtectedAccess.() -> Unit,
)

/**
 * Eten & drinken voor PK. Eet gekookt voedsel om HP te herstellen, drink potions (per dosis,
 * 4 -> 3 -> 2 -> 1 -> lege vial) voor boosts/herstel.
 */
class Eating @Inject constructor() : PluginScript() {
    private val foods =
        listOf(
            FoodItem(FoodObjs.bread, 5, "bread"),
            FoodItem(FoodObjs.cooked_meat, 3, "meat"),
            FoodItem(FoodObjs.cooked_chicken, 3, "chicken"),
            FoodItem(FoodObjs.shrimp, 3, "shrimps"),
            FoodItem(FoodObjs.trout, 7, "trout"),
            FoodItem(FoodObjs.salmon, 9, "salmon"),
            FoodItem(FoodObjs.tuna, 10, "tuna"),
            FoodItem(FoodObjs.lobster, 12, "lobster"),
            FoodItem(FoodObjs.swordfish, 14, "swordfish"),
            FoodItem(FoodObjs.monkfish, 16, "monkfish"),
            FoodItem(FoodObjs.shark, 20, "shark"),
            FoodItem(FoodObjs.dark_crab, 22, "dark crab"),
            FoodItem(FoodObjs.anglerfish, 22, "anglerfish"),
            FoodItem(FoodObjs.karambwan, 18, "karambwan"),
        )

    private val potions =
        listOf(
            Potion(
                listOf(FoodObjs.combat4, FoodObjs.combat3, FoodObjs.combat2, FoodObjs.combat1),
                "You drink some super combat potion. You feel much stronger.",
            ) {
                statBoost(stats.attack, constant = 5, percent = 15)
                statBoost(stats.strength, constant = 5, percent = 15)
                statBoost(stats.defence, constant = 5, percent = 15)
            },
            Potion(
                listOf(FoodObjs.divine4, FoodObjs.divine3, FoodObjs.divine2, FoodObjs.divine1),
                "You drink some divine super combat potion.",
            ) {
                statBoost(stats.attack, constant = 5, percent = 15)
                statBoost(stats.strength, constant = 5, percent = 15)
                statBoost(stats.defence, constant = 5, percent = 15)
            },
            Potion(
                listOf(FoodObjs.bastion4, FoodObjs.bastion3, FoodObjs.bastion2, FoodObjs.bastion1),
                "You drink some bastion potion.",
            ) {
                statBoost(stats.ranged, constant = 4, percent = 13)
                statBoost(stats.defence, constant = 5, percent = 15)
            },
            Potion(
                listOf(FoodObjs.prayer4, FoodObjs.prayer3, FoodObjs.prayer2, FoodObjs.prayer1),
                "You drink some prayer potion.",
            ) {
                statHeal(stats.prayer, constant = 99, percent = 0)
            },
            Potion(
                listOf(FoodObjs.restore4, FoodObjs.restore3, FoodObjs.restore2, FoodObjs.restore1),
                "You drink some super restore.",
            ) {
                statRestoreAll(
                    listOf(stats.attack, stats.strength, stats.defence, stats.ranged, stats.magic)
                )
                statHeal(stats.prayer, constant = 99, percent = 0)
            },
            Potion(
                listOf(FoodObjs.brew4, FoodObjs.brew3, FoodObjs.brew2, FoodObjs.brew1),
                "You drink some Saradomin brew.",
            ) {
                statHeal(stats.hitpoints, constant = 16, percent = 15)
                statBoost(stats.defence, constant = 2, percent = 21)
            },
        )

    override fun ScriptContext.startup() {
        for (food in foods) {
            onOpHeld1(food.obj) { eat(food) }
        }
        for (potion in potions) {
            for (dose in potion.doses) {
                onOpHeld1(dose) { drink(potion, dose) }
            }
        }
    }

    private fun ProtectedAccess.eat(food: FoodItem) {
        invDel(inv, food.obj, 1)
        statHeal(stats.hitpoints, constant = food.heal, percent = 0)
        mes("You eat the ${food.name}. It heals ${food.heal} hitpoints.")
    }

    private fun ProtectedAccess.drink(potion: Potion, dose: ObjType) {
        val index = potion.doses.indexOf(dose)
        invDel(inv, dose, 1)
        val next =
            if (index in 0 until potion.doses.size - 1) potion.doses[index + 1] else FoodObjs.vial_empty
        invAdd(inv, next)
        potion.effect(this)
        mes(potion.message)
    }
}
