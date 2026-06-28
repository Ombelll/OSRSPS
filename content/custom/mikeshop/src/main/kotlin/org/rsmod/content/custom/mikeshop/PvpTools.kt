package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.combatClearQueue
import org.rsmod.api.player.deathResetTimers
import org.rsmod.api.player.disablePrayers
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statRestoreAll
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.stat.StatTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private val PK_READY_TILE: CoordGrid = CoordGrid(0, 48, 54, 15, 40)
private val PK_DUEL_SAFE_TILE: CoordGrid = CoordGrid(0, 48, 54, 15, 61)
private val FIGHT_PIT_TILE: CoordGrid = CoordGrid(0, 48, 55, 20, 6) // 3092,3526 - lage wild bij Edge

// Wilderness-obelisk bestemmingen: spots in de geladen Edgeville-wild (::obelisk hopt random rond).
private val OBELISK_SPOTS: List<CoordGrid> =
    listOf(
        CoordGrid(3087, 3527),
        CoordGrid(3095, 3540),
        CoordGrid(3075, 3545),
        CoordGrid(3100, 3555),
        CoordGrid(3070, 3535),
    )
private val PK_DUEL_WILD_TILE: CoordGrid = CoordGrid(0, 48, 55, 15, 2)

class PvpTools
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val statTypes: StatTypeList,
    private val objTypes: ObjTypeList,
) : PluginScript() {
    private val pvpWorld = System.getenv("RSMOD_WORLD") == "2"
    private var Player.specialAttackType by intVarp(varps.sa_attack)
    private var Player.specialAttackEnergy by intVarp(varps.sa_energy)

    override fun ScriptContext.startup() {
        // Zet de Attack-optie (player-op) meteen bij login op de PvP-wereld, zodat er overal -
        // inclusief in de Wilderness - direct een werkende Attack-knop op andere spelers staat,
        // zonder dat je eerst ::pvpops of ::pkready hoeft te draaien. Op W1 worden de ops juist
        // veilig gezet (geen Attack).
        onPlayerLogin { player.sendPvpPlayerOps(pvpWorld) }

        onCommand("pvpops") {
            desc = "Refresh player right-click options for PvP testing"
            cheat {
                player.sendPvpPlayerOps(pvpWorld)
                if (pvpWorld) {
                    player.mes("PvP options refreshed. Right-click players should show Attack.")
                } else {
                    player.mes("PvP options refreshed for safe world. Attack is only shown on World 2.")
                }
            }
        }

        onCommand("pkready") {
            desc = "W2 test setup: refresh PvP options and move to Edgeville PK hub"
            cheat {
                player.sendPvpPlayerOps(pvpWorld)
                if (!pvpWorld) {
                    player.mes("Use World 2 for PvP. Your W1 player options were left safe.")
                    return@cheat
                }
                protectedAccess.launch(player) {
                    telejump(PK_READY_TILE)
                    mes("PK ready: Edgeville hub, Attack option refreshed.")
                }
            }
        }

        onCommand("resetfight") {
            desc = "Reset HP, prayer, spec, skull and teleblock for safe PK testing"
            cheat {
                if (!player.requirePvpWorld()) return@cheat
                player.resetFightState()
                player.mes("Fight reset: HP, prayer, stats, spec, skull and teleblock restored.")
            }
        }

        onCommand("risk") {
            desc = "Toon de geschatte waarde van je gedragen + inventory items (wat je riskeert)"
            cheat {
                var total = 0L
                for (slot in player.inv.indices) {
                    val obj = player.inv[slot] ?: continue
                    total += objTypes[obj].cost.toLong() * obj.count.coerceAtLeast(1)
                }
                for (slot in player.worn.indices) {
                    val obj = player.worn[slot] ?: continue
                    total += objTypes[obj].cost.toLong() * obj.count.coerceAtLeast(1)
                }
                player.mes("Je riskeert momenteel ongeveer ${"%,d".format(total)} gp aan items.")
            }
        }

        onCommand("obelisk") {
            desc = "Wilderness-obelisk: hop naar een willekeurige andere wild-locatie"
            cheat {
                if (!player.requirePvpWorld()) return@cheat
                val dest = OBELISK_SPOTS.random()
                protectedAccess.launch(player) {
                    telejump(dest)
                    mes("De obelisk verplaatst je naar een andere plek in de Wilderness!")
                }
            }
        }

        onCommand("duel") {
            desc = "Teleporteer naar de Edgeville fight-pit voor een directe 1v1 (beiden ::duel)"
            cheat {
                if (!player.requirePvpWorld()) return@cheat
                player.sendPvpPlayerOps(pvpWorld)
                protectedAccess.launch(player) {
                    telejump(FIGHT_PIT_TILE)
                    mes("Fight-pit (lage wild): zodra je maat ook ::duel doet, sla erop los!")
                }
            }
        }

        onCommand("wildlevel") {
            desc = "Toon je huidige Wilderness-level"
            cheat {
                val x = player.coords.x
                val z = player.coords.z
                val level =
                    if (x in 2944..3391 && z in 3520..4351) {
                        (((z - 3520) / 8) + 1).coerceIn(1, 56)
                    } else {
                        0
                    }
                if (level == 0) {
                    player.mes("Je bent niet in de Wilderness (veilig gebied).")
                } else {
                    player.mes("Wilderness level: $level.")
                }
            }
        }

        onCommand("dharok") {
            desc = "Zet je HP op 1 (Dharok-test: lage HP = hogere max hit)"
            cheat {
                protectedAccess.launch(player) {
                    val current = player.hitpoints
                    if (current > 1) {
                        statSub(stats.hitpoints, constant = current - 1, percent = 0)
                        player.mes("HP op 1 gezet. Dharok's hitst nu het hardst!")
                    } else {
                        player.mes("Je HP is al 1.")
                    }
                }
            }
        }

        onCommand("skull") {
            desc = "Toggle a voluntary PvP skull for risk testing"
            cheat {
                if (!player.requirePvpWorld()) return@cheat
                if (player.skullIcon == null) {
                    player.skullIcon = constants.skullicon_default
                    player.mes("You are now skulled for PK testing.")
                } else {
                    player.skullIcon = null
                    player.mes("Your skull has been cleared.")
                }
            }
        }

        onCommand("duelarea") {
            desc = "Teleport to the south Edgeville quick-fight staging tile"
            cheat {
                if (!player.requirePvpWorld()) return@cheat
                player.sendPvpPlayerOps(pvpWorld)
                protectedAccess.launch(player) {
                    telejump(PK_DUEL_SAFE_TILE)
                    mes("Quick-fight staging: step north over the ditch when both players are ready.")
                }
            }
        }

        onCommand("wildfight") {
            desc = "Teleport just north of Edgeville ditch for immediate wilderness tests"
            cheat {
                if (!player.requirePvpWorld()) return@cheat
                player.sendPvpPlayerOps(pvpWorld)
                protectedAccess.launch(player) {
                    telejump(PK_DUEL_WILD_TILE)
                    mes("Wilderness quick-fight tile. Combat range rules apply here.")
                }
            }
        }
    }

    private fun Player.requirePvpWorld(): Boolean {
        if (pvpWorld) return true
        sendPvpPlayerOps(pvpWorld)
        mes("Use World 2 for PvP testing.")
        return false
    }

    private fun Player.resetFightState() {
        combatClearQueue()
        disablePrayers()
        deathResetTimers()
        statRestoreAll(statTypes.values)
        specialAttackType = 0
        specialAttackEnergy = constants.sa_max_energy
        skullIcon = null
        TeleBlockState.clear(this)
    }
}

private fun Player.sendPvpPlayerOps(pvpWorld: Boolean) {
    MiscOutput.setPlayerOp(this, slot = 2, op = if (pvpWorld) "Attack" else null, priority = pvpWorld)
    MiscOutput.setPlayerOp(this, slot = 3, op = "Follow")
    MiscOutput.setPlayerOp(this, slot = 4, op = "Trade with")
    MiscOutput.setPlayerOp(this, slot = 5, op = null)
    MiscOutput.setPlayerOp(this, slot = 8, op = "Report")
}
