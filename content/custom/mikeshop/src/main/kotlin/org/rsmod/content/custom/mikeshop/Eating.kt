package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statHeal
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

    // Super combat potion (per dosis):
    val combat4 = find("4dose2combat")
    val combat3 = find("3dose2combat")
    val combat2 = find("2dose2combat")
    val combat1 = find("1dose2combat")
    val vial_empty = find("vial_empty")
}

private class FoodItem(val obj: ObjType, val heal: Int, val name: String)

private class Dose(val from: ObjType, val to: ObjType)

/**
 * Eten & drinken.
 *
 * - Eet gekookt voedsel (klik -> "Eat") om HP te herstellen.
 * - Drink super combat potion -> boost op Attack/Strength/Defence, dosis loopt af.
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
            FoodItem(FoodObjs.shark, 20, "shark"),
        )

    private val combatDoses =
        listOf(
            Dose(FoodObjs.combat4, FoodObjs.combat3),
            Dose(FoodObjs.combat3, FoodObjs.combat2),
            Dose(FoodObjs.combat2, FoodObjs.combat1),
            Dose(FoodObjs.combat1, FoodObjs.vial_empty),
        )

    override fun ScriptContext.startup() {
        for (food in foods) {
            onOpHeld1(food.obj) { eat(food) }
        }
        for (dose in combatDoses) {
            onOpHeld1(dose.from) { drinkCombat(dose) }
        }
    }

    private fun ProtectedAccess.eat(food: FoodItem) {
        invDel(inv, food.obj, 1)
        statHeal(stats.hitpoints, constant = food.heal, percent = 0)
        mes("You eat the ${food.name}. It heals ${food.heal} hitpoints.")
    }

    private fun ProtectedAccess.drinkCombat(dose: Dose) {
        invDel(inv, dose.from, 1)
        invAdd(inv, dose.to)
        statBoost(stats.attack, constant = 5, percent = 15)
        statBoost(stats.strength, constant = 5, percent = 15)
        statBoost(stats.defence, constant = 5, percent = 15)
        mes("You drink some super combat potion. You feel much stronger.")
    }
}
