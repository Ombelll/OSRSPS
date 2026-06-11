package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onCommand
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Monsters die de combat-zone spawnt. */
object ZoneNpcs : NpcReferences() {
    val cow = find("cow")
    val chicken = find("chicken")
    val goblin = find("goblin")
    val giant = find("giant") // hill giant mini-boss
}

/**
 * Custom commando's: combat-zone, mini-boss, teleport-hub, starter-kit en mega-boss.
 */
class CombatZone
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        // --- Combat ---
        onCommand("combatzone") {
            desc = "Spawn a pack of monsters around you"
            cheat { spawnPack(player) }
        }
        onCommand("boss") {
            desc = "Spawn a Hill Giant mini-boss"
            cheat {
                spawn(ZoneNpcs.giant, player.coords.translate(1, 1))
                player.mes("A Hill Giant has spawned! (tip: ::master first to stand a chance)")
            }
        }
        onCommand("megaboss") {
            desc = "Spawn a horde of Hill Giants"
            cheat {
                val base = player.coords
                spawn(ZoneNpcs.giant, base.translate(2, 0))
                spawn(ZoneNpcs.giant, base.translate(-2, 0))
                spawn(ZoneNpcs.giant, base.translate(0, 2))
                spawn(ZoneNpcs.giant, base.translate(0, -2))
                spawn(ZoneNpcs.giant, base.translate(2, 2))
                player.mes("5 Hill Giants have spawned! (::master strongly recommended)")
            }
        }

        // --- Starter-kit ---
        onCommand("starter") {
            desc = "Get a starter kit"
            cheat {
                player.invAdd(player.inv, MikeShopObjs.bronze_pickaxe, 1, strict = false)
                player.invAdd(player.inv, MikeShopObjs.bronze_scimitar, 1, strict = false)
                player.invAdd(player.inv, MikeShopObjs.wooden_shield, 1, strict = false)
                player.invAdd(player.inv, MikeShopObjs.leather_armour, 1, strict = false)
                player.invAdd(player.inv, MikeShopObjs.tinderbox, 1, strict = false)
                player.invAdd(player.inv, MikeShopObjs.shark, 10, strict = false)
                player.mes("You receive a starter kit! (pickaxe, scimitar, shield, armour, 10 sharks)")
            }
        }

        // --- Teleport-hub ---
        teleCmd("home", CoordGrid(0, 50, 50, 21, 18))
        teleCmd("varrock", CoordGrid(0, 50, 53, 12, 30))
        teleCmd("falador", CoordGrid(0, 46, 52, 21, 50))
        teleCmd("edge", CoordGrid(0, 48, 54, 15, 44))
        teleCmd("ge", CoordGrid(0, 49, 54, 28, 30))
        teleCmd("mine", CoordGrid(0, 50, 49, 28, 12))
        teleCmd("wild", CoordGrid(0, 48, 56, 15, 0))
    }

    private fun ScriptContext.teleCmd(name: String, dest: CoordGrid) {
        onCommand(name) {
            desc = "Teleport to $name"
            cheat {
                protectedAccess.launch(player) {
                    telejump(dest)
                    player.mes("Teleported to $name.")
                }
            }
        }
    }

    private fun spawnPack(player: Player) {
        val base = player.coords
        spawn(ZoneNpcs.cow, base.translate(1, 1))
        spawn(ZoneNpcs.cow, base.translate(-1, 1))
        spawn(ZoneNpcs.chicken, base.translate(2, 0))
        spawn(ZoneNpcs.chicken, base.translate(-2, 0))
        spawn(ZoneNpcs.goblin, base.translate(0, 2))
        spawn(ZoneNpcs.goblin, base.translate(1, -1))
        player.mes("Spawned a combat zone around you! (2 cows, 2 chickens, 2 goblins)")
    }

    private fun spawn(type: NpcType, coords: CoordGrid) {
        val npc = Npc(npcTypes[type], coords) // ref -> UnpackedNpcType
        npc.mode = NpcMode.None
        npcRepo.add(npc, duration = 500)
    }
}
