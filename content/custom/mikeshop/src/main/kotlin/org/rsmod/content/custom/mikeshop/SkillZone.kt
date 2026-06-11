package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onCommand
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.loc.LocLayerConstants

internal object SkillZoneLocs : LocReferences() {
    val copperrock1 = find("copperrock1")
    val tinrock1 = find("tinrock1")
    val ironrock1 = find("ironrock1")
    val coalrock1 = find("coalrock1")
    val furnace = find("furnace")
    val anvil = find("anvil")
    val altar = find("altar")
}

/**
 * ::skillzone -> spawnt een complete skilling-zone rond je: erts-rotsen (Mining), een oven +
 * aambeeld (Smithing) en een altaar (Prayer). Combineer met ::skillkit/::skillmats om ter
 * plekke te mijnen -> smelten -> smeden -> bidden zonder te reizen.
 */
class SkillZone
@Inject
constructor(private val locTypes: LocTypeList, private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("skillzone") {
            desc = "Spawn a skilling zone (rocks, furnace, anvil, altar) around you"
            cheat {
                val base = player.coords
                spawnLoc(SkillZoneLocs.copperrock1, base.translate(2, 2))
                spawnLoc(SkillZoneLocs.tinrock1, base.translate(3, 2))
                spawnLoc(SkillZoneLocs.ironrock1, base.translate(2, -2))
                spawnLoc(SkillZoneLocs.coalrock1, base.translate(3, -2))
                spawnLoc(SkillZoneLocs.furnace, base.translate(-3, 2))
                spawnLoc(SkillZoneLocs.anvil, base.translate(-3, 0))
                spawnLoc(SkillZoneLocs.altar, base.translate(-3, -2))
                player.mes("A skilling zone appears around you! Mine -> smelt -> smith -> pray.")
            }
        }
    }

    private fun spawnLoc(ref: LocType, coords: CoordGrid) {
        val type = locTypes[ref]
        val shape = LocShape.CentrepieceStraight.id
        val angle = LocAngle.West.id
        val loc = LocInfo(LocLayerConstants.of(shape), coords, LocEntity(type.id, shape, angle))
        locRepo.add(loc, duration = 500)
    }
}
