package org.rsmod.content.custom.mikeshop

import org.rsmod.api.config.refs.areas
import org.rsmod.api.type.builders.map.area.MapAreaBuilder
import org.rsmod.map.square.MapSquareKey

object FightCaveMapAreas : MapAreaBuilder() {
    override fun onPackMapTask() {
        area(areas.fight_cave_arena) { mapSquare(MapSquareKey(37, 79)) }
        area(areas.fight_cave_arena) { mapSquare(MapSquareKey(38, 79)) }
        area(areas.fight_cave_arena) { mapSquare(MapSquareKey(37, 80)) }
        area(areas.fight_cave_arena) { mapSquare(MapSquareKey(38, 80)) }
    }
}
