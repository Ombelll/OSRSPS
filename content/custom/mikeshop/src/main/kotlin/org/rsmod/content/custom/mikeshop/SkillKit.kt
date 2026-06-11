package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object SkillKitObjs : ObjReferences() {
    // Tools:
    val bronze_pickaxe = find("bronze_pickaxe")
    val bronze_axe = find("bronze_axe")
    val knife = find("knife")
    val needle = find("needle")
    val chisel = find("chisel")
    val hammer = find("hammer")
    val tinderbox = find("tinderbox")
    val net = find("net")
    val fishing_rod = find("fishing_rod")
    val fishing_bait = find("fishing_bait")
    val harpoon = find("harpoon")
    val lobster_pot = find("lobster_pot")
    val feather = find("feather")
    val ring_mould = find("ring_mould")

    // Materialen:
    val copper_ore = find("copper_ore")
    val tin_ore = find("tin_ore")
    val iron_ore = find("iron_ore")
    val coal = find("coal")
    val logs = find("logs")
    val oak_logs = find("oak_logs")
    val flax = find("flax")
    val gold_bar = find("gold_bar")
    val uncut_sapphire = find("uncut_sapphire")
    val raw_beef = find("raw_beef")
    val cow_hide = find("cow_hide")
    val bones = find("bones")
    val potato_seed = find("potato_seed")
    val blankrune = find("blankrune")
    val vial_water = find("vial_water")
    val unidentified_guam = find("unidentified_guam")
    val eye_of_newt = find("eye_of_newt")
}

/** Eén kit-item: het object en hoeveel je ervan krijgt. */
private class KitItem(val obj: ObjType, val count: Int)

/**
 * ::skillkit -> geeft alle gereedschappen + wat materialen om élke skill direct te trainen
 * (pickaxe/axe/knife/needle/chisel/hammer/tinderbox/vis-tools + erts/logs/flax/zaden/runes/
 * kruiden/botten/hide/vlees). Handig om alle skill-systemen meteen uit te proberen.
 */
class SkillKit @Inject constructor() : PluginScript() {
    private val tools =
        listOf(
            KitItem(SkillKitObjs.bronze_pickaxe, 1),
            KitItem(SkillKitObjs.bronze_axe, 1),
            KitItem(SkillKitObjs.knife, 1),
            KitItem(SkillKitObjs.needle, 1),
            KitItem(SkillKitObjs.chisel, 1),
            KitItem(SkillKitObjs.hammer, 1),
            KitItem(SkillKitObjs.tinderbox, 1),
            KitItem(SkillKitObjs.net, 1),
            KitItem(SkillKitObjs.fishing_rod, 1),
            KitItem(SkillKitObjs.fishing_bait, 1000),
            KitItem(SkillKitObjs.harpoon, 1),
            KitItem(SkillKitObjs.lobster_pot, 1),
            KitItem(SkillKitObjs.feather, 1000),
            KitItem(SkillKitObjs.ring_mould, 1),
        )

    private val materials =
        listOf(
            KitItem(SkillKitObjs.copper_ore, 50),
            KitItem(SkillKitObjs.tin_ore, 50),
            KitItem(SkillKitObjs.iron_ore, 50),
            KitItem(SkillKitObjs.coal, 100),
            KitItem(SkillKitObjs.logs, 50),
            KitItem(SkillKitObjs.oak_logs, 50),
            KitItem(SkillKitObjs.flax, 50),
            KitItem(SkillKitObjs.gold_bar, 50),
            KitItem(SkillKitObjs.uncut_sapphire, 50),
            KitItem(SkillKitObjs.raw_beef, 50),
            KitItem(SkillKitObjs.cow_hide, 50),
            KitItem(SkillKitObjs.bones, 100),
            KitItem(SkillKitObjs.potato_seed, 20),
            KitItem(SkillKitObjs.blankrune, 100),
            KitItem(SkillKitObjs.vial_water, 50),
            KitItem(SkillKitObjs.unidentified_guam, 50),
            KitItem(SkillKitObjs.eye_of_newt, 50),
        )

    override fun ScriptContext.startup() {
        onCommand("skillkit") {
            desc = "Get every skilling tool + sample materials"
            cheat {
                give(player, tools)
                player.mes("--- You receive a full skilling kit ---")
                player.mes("Use ::skillkit again or ::store for more materials. Happy training!")
            }
        }
        onCommand("skillmats") {
            desc = "Get a batch of skilling materials"
            cheat {
                give(player, materials)
                player.mes("You receive a batch of skilling materials.")
            }
        }
    }

    private fun give(player: Player, items: List<KitItem>) {
        for (item in items) {
            player.invAdd(player.inv, item.obj, item.count, strict = false)
        }
    }
}
