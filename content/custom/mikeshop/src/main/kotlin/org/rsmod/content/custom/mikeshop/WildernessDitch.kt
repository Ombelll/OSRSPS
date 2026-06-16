package org.rsmod.content.custom.mikeshop

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object WildernessDitchLocs : LocReferences() {
    val ditch = find("ditch_wilderness_cover")
    val membersDitch = find("ditch_wilderness_cover_members")
}

internal object WildernessDitchSeqs : SeqReferences() {
    val jump = find("wild_ditch_jump")
}

class WildernessDitch : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(WildernessDitchLocs.ditch) { jumpDitch(it.loc) }
        onOpLoc1(WildernessDitchLocs.membersDitch) { jumpDitch(it.loc) }
    }

    private suspend fun ProtectedAccess.jumpDitch(loc: BoundLocInfo) {
        arriveDelay()
        val dest = ditchJumpDestination(loc)
        faceSquare(dest)
        anim(WildernessDitchSeqs.jump)
        delay(1)
        telejump(dest)
        mes("You jump over the Wilderness ditch.")
    }

    private fun ProtectedAccess.ditchJumpDestination(loc: BoundLocInfo): CoordGrid {
        val fromSouth = player.coords.z <= loc.coords.z
        val destZ = if (fromSouth) loc.coords.z + 2 else loc.coords.z - 1
        return player.coords.copy(z = destZ)
    }
}
