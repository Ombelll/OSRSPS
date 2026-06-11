package org.rsmod.content.skills.firemaking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.loc.LocLayerConstants

internal object FireObjs : ObjReferences() {
    val tinderbox = find("tinderbox")
    val logs = find("logs")
    val oak_logs = find("oak_logs")
}

internal object FireLocs : LocReferences() {
    val fire = find("fire")
}

/**
 * FIREMAKING: gebruik een tinderbox op logs -> er verschijnt een vuur op je tegel + Firemaking-XP.
 * Het vuur ('fire'-loc) blijft even staan; je kunt er daarna op koken (zie de cookery-module).
 */
class Firemaking
@Inject
constructor(private val locTypes: LocTypeList, private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(FireObjs.tinderbox, FireObjs.logs) { light(FireObjs.logs, 40.0) }
        onOpHeldU(FireObjs.tinderbox, FireObjs.oak_logs) { light(FireObjs.oak_logs, 60.0) }
    }

    private fun ProtectedAccess.light(log: ObjType, xp: Double) {
        invDel(inv, log, 1)
        val type = locTypes[FireLocs.fire]
        val shape = LocShape.CentrepieceStraight.id
        val angle = LocAngle.West.id
        val loc = LocInfo(LocLayerConstants.of(shape), player.coords, LocEntity(type.id, shape, angle))
        locRepo.add(loc, duration = 100)
        statAdvance(stats.firemaking, PlayerStatMap.toFineXP(xp).toDouble())
        mes("The fire catches and the logs begin to burn.")
    }
}
