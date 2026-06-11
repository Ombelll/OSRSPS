package org.rsmod.content.skills.cookery

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.cookingLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object CookObjs : ObjReferences() {
    val raw_beef = find("raw_beef")
    val cooked_meat = find("cooked_meat")
    val burnt_meat = find("burnt_meat")
    val raw_chicken = find("raw_chicken")
    val cooked_chicken = find("cooked_chicken")
    val burnt_chicken = find("burnt_chicken")
    val raw_shrimp = find("raw_shrimp")
    val shrimp = find("shrimp")
    val burnt_shrimp = find("burnt_shrimp")
    val raw_lobster = find("raw_lobster")
    val lobster = find("lobster")
    val burnt_lobster = find("burnt_lobster")
    val raw_swordfish = find("raw_swordfish")
    val swordfish = find("swordfish")
    val burnt_swordfish = find("burnt_swordfish")
    val raw_shark = find("raw_shark")
    val shark = find("shark")
    val burnt_shark = find("burnt_shark")
}

internal object CookLocs : LocReferences() {
    val fire = find("fire")
}

/**
 * Eén gerecht: rauw -> gekookt of (bij mislukken) verbrand.
 *
 * @param cookLevel minimale Cooking-level om het te bereiden.
 * @param xp Cooking-XP bij succes.
 * @param stopBurn vanaf dit level brandt het nooit meer aan.
 */
private class Food(
    val raw: ObjType,
    val cooked: ObjType,
    val burnt: ObjType,
    val cookLevel: Int,
    val xp: Double,
    val stopBurn: Int,
    val name: String,
)

/**
 * COOKERY (met aanbrand-kans).
 *
 * Gebruik rauw eten op een vuur. Onder je 'stopBurn'-level is er kans dat het aanbrandt
 * (verbrand eten, geen XP); die kans daalt naarmate je Cooking-level stijgt en verdwijnt
 * op het stopBurn-level. Verbindt mooi met Fishing (rauwe vis) en combat-loot (beef/chicken).
 */
class Cookery @Inject constructor(private val random: GameRandom) : PluginScript() {
    private val foods =
        listOf(
            Food(CookObjs.raw_beef, CookObjs.cooked_meat, CookObjs.burnt_meat, 1, 30.0, 30, "meat"),
            Food(
                CookObjs.raw_chicken,
                CookObjs.cooked_chicken,
                CookObjs.burnt_chicken,
                1,
                30.0,
                30,
                "chicken",
            ),
            Food(CookObjs.raw_shrimp, CookObjs.shrimp, CookObjs.burnt_shrimp, 1, 30.0, 34, "shrimps"),
            Food(
                CookObjs.raw_lobster,
                CookObjs.lobster,
                CookObjs.burnt_lobster,
                40,
                120.0,
                74,
                "lobster",
            ),
            Food(
                CookObjs.raw_swordfish,
                CookObjs.swordfish,
                CookObjs.burnt_swordfish,
                45,
                140.0,
                86,
                "swordfish",
            ),
            Food(CookObjs.raw_shark, CookObjs.shark, CookObjs.burnt_shark, 80, 210.0, 94, "shark"),
        )

    override fun ScriptContext.startup() {
        for (food in foods) {
            onOpLocU(CookLocs.fire, food.raw) { cook(food) }
        }
    }

    private fun ProtectedAccess.cook(food: Food) {
        if (player.cookingLvl < food.cookLevel) {
            mes("You need a Cooking level of ${food.cookLevel} to cook this.")
            return
        }
        invDel(inv, food.raw, 1)
        if (burns(food)) {
            invAdd(inv, food.burnt)
            mes("Oops! You accidentally burn the ${food.name}.")
        } else {
            invAdd(inv, food.cooked)
            statAdvance(stats.cooking, PlayerStatMap.toFineXP(food.xp).toDouble())
            mes("You cook the ${food.name} over the fire.")
        }
    }

    /** Aanbrand-kans: ~50% op het kook-level, lineair aflopend naar 0% op stopBurn. */
    private fun ProtectedAccess.burns(food: Food): Boolean {
        val level = player.cookingLvl
        if (level >= food.stopBurn) return false
        val span = (food.stopBurn - food.cookLevel).coerceAtLeast(1)
        val pct = (50 * (food.stopBurn - level) / span).coerceIn(0, 90)
        return random.of(maxExclusive = 100) < pct
    }
}
