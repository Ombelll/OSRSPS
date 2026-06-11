package org.rsmod.content.custom.combatfix

import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.seq.SeqType

/**
 * Repareert de "rare" aanval-animaties van NPC's.
 *
 * Probleem: OSRS bewaart NPC-aanvalsanimaties server-side (niet in de publieke cache),
 * dus rsmod valt voor ELKE npc terug op de default `attack_anim` = human_unarmedpunch
 * (een mens die slaat). Hier koppelen we per monster de juiste, bestaande attack-seq.
 */
internal object FixSeqs : SeqReferences() {
    val cow = find("cow_attack")
    val chicken = find("chicken_attack")
    val goblin = find("goblin_attack_unarmed")
    val giant = find("giant_attack")
    val rat = find("rat_attack")
    val spider = find("spider_attack")
    val unicorn = find("unicorn_attack")
    val imp = find("imp_attack")
    val ghost = find("ghost_attack")
    val ogre = find("ogre_attack")
    val dog = find("dog_attack")
}

internal object FixNpcs : NpcReferences() {
    val cow = find("cow")
    val cow2 = find("cow2")
    val cow3 = find("cow3")
    val cow_beef = find("cow_beef")
    val chicken = find("chicken")
    val goblin = find("goblin")
    val giant = find("giant")
    val rat = find("rat")
    val spider = find("spider")
    val unicorn = find("unicorn")
    val imp = find("imp")
    val ghost = find("ghost")
    val ogre = find("ogre")
    val wolf = find("wolf")
}

internal object CombatAnimEditor : NpcEditor() {
    init {
        fix(FixNpcs.cow, FixSeqs.cow)
        fix(FixNpcs.cow2, FixSeqs.cow)
        fix(FixNpcs.cow3, FixSeqs.cow)
        fix(FixNpcs.cow_beef, FixSeqs.cow)
        fix(FixNpcs.chicken, FixSeqs.chicken)
        fix(FixNpcs.goblin, FixSeqs.goblin)
        fix(FixNpcs.giant, FixSeqs.giant)
        fix(FixNpcs.rat, FixSeqs.rat)
        fix(FixNpcs.spider, FixSeqs.spider)
        fix(FixNpcs.unicorn, FixSeqs.unicorn)
        fix(FixNpcs.imp, FixSeqs.imp)
        fix(FixNpcs.ghost, FixSeqs.ghost)
        fix(FixNpcs.ogre, FixSeqs.ogre)
        fix(FixNpcs.wolf, FixSeqs.dog) // wolf gebruikt de hond-aanval als beste match
    }

    private fun fix(npc: NpcType, seq: SeqType) {
        edit(npc) { param[params.attack_anim] = seq }
    }
}
