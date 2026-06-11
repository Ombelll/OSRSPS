package org.rsmod.content.custom.mikeshop

import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.script.dsl.NpcPluginBuilder

/**
 * De NPC die we ombouwen tot onze boss. We gebruiken green_dragon: een normaal spawnbaar
 * monster dat gegarandeerd rendert (elvarg is een unieke quest-NPC die manueel gespawnd
 * niet verschijnt). Past bovendien mooi bij de groene-draak-art.
 */
internal object BossNpcs : NpcReferences() {
    val green_dragon = find("green_dragon")
    val greater_demon = find("greater_demon")
    val black_demon = find("black_demon")
    val blue_dragon = find("blue_dragon")
    val red_dragon = find("red_dragon")
    val steel_dragon = find("steel_dragon")
}

internal object BossSeqs : SeqReferences() {
    val dragon = find("dragon_attack")
    val demon = find("demon_attack")
}

/**
 * Bouwt onze 3 bosses door bestaande, normaal spawnbare monsters op te voeren (dikke HP,
 * hoge combat-stats, juiste aanvalanimatie). (Type-wijziging -> vereist packCache.)
 *
 *  - green_dragon  -> "Mike the Destroyer" (instap-boss)
 *  - greater_demon -> "Inferno Demon"      (midden-boss)
 *  - black_demon   -> "Mike's Nightmare"   (eind-boss)
 */
internal object MikeBossEditor : NpcEditor() {
    init {
        edit(BossNpcs.green_dragon) {
            name = "Mike the Destroyer"
            applyBossAggro()
            hitpoints = 600
            attack = 200
            strength = 200
            defence = 180
            ranged = 150
            magic = 150
            param[params.attack_anim] = BossSeqs.dragon
        }
        edit(BossNpcs.greater_demon) {
            name = "Inferno Demon"
            applyBossAggro()
            hitpoints = 800
            attack = 220
            strength = 220
            defence = 200
            magic = 180
            param[params.attack_anim] = BossSeqs.demon
        }
        edit(BossNpcs.black_demon) {
            name = "Mike's Nightmare"
            applyBossAggro()
            hitpoints = 1200
            attack = 280
            strength = 280
            defence = 250
            magic = 220
            param[params.attack_anim] = BossSeqs.demon
        }
        edit(BossNpcs.blue_dragon) {
            name = "Frost Wyrm"
            applyBossAggro()
            hitpoints = 1400
            attack = 300
            strength = 300
            defence = 270
            magic = 240
            param[params.attack_anim] = BossSeqs.dragon
        }
        edit(BossNpcs.red_dragon) {
            name = "Crimson Wyrm"
            applyBossAggro()
            hitpoints = 1800
            attack = 340
            strength = 340
            defence = 300
            magic = 260
            param[params.attack_anim] = BossSeqs.dragon
        }
        edit(BossNpcs.steel_dragon) {
            name = "The Ancient One"
            applyBossAggro()
            hitpoints = 2500
            attack = 400
            strength = 400
            defence = 350
            magic = 300
            param[params.attack_anim] = BossSeqs.dragon
        }
    }

    private fun NpcPluginBuilder.applyBossAggro() {
        huntMode = huntmodes.mike_boss_aggro
        huntRange = 8
    }
}

internal object ArenaMonsterEditor : NpcEditor() {
    init {
        edit(ArenaMonsters.skeleton) { applyArenaAggro() }
        edit(ArenaMonsters.zombie) { applyArenaAggro() }
        edit(ArenaMonsters.black_knight) { applyArenaAggro() }
        edit(ArenaMonsters.lesser_demon) { applyArenaAggro() }
    }

    private fun NpcPluginBuilder.applyArenaAggro() {
        defaultMode = none
        huntMode = huntmodes.mike_boss_aggro
        huntRange = 8
    }
}
