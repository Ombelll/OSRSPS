package org.rsmod.content.skills.mining

import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.testing.GameTestState
import org.rsmod.map.CoordGrid

class MiningScriptTest {
    @Test
    fun GameTestState.`mine copper rock gives ore`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), MiningRocks.copperrock1)
            player.teleport(rock.coords.translateX(-1))
            player.clearInv()
            player.stats[stats.mining] = 1

            player.opLoc1(rock)
            advance(ticks = 2)

            assertMessageSent("You manage to mine some copper ore.")
            assertContains(player.inv, MiningObjs.copper_ore)
        }

    @Test
    fun GameTestState.`iron rock requires mining level 15`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), MiningRocks.ironrock1)
            player.teleport(rock.coords.translateX(-1))
            player.clearInv()
            player.stats[stats.mining] = 1

            player.opLoc1(rock)
            advance(ticks = 2)

            assertMessageSent("You need a Mining level of 15 to mine this rock.")
            assertDoesNotContain(player.inv, MiningObjs.iron_ore)
        }
}
