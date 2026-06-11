package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.content.other.special.attacks.configs.special_seqs
import org.rsmod.content.other.special.attacks.configs.special_spots
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

class DragonDaggerSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val special = DragonDagger(manager)
        registerMelee(objs.dragon_dagger, special)
        registerMelee(objs.dragon_dagger_p, special)
        registerMelee(objs.dragon_dagger_p_plus, special)
        registerMelee(objs.dragon_dagger_p_plus_plus, special)
    }

    private class DragonDagger(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            puncture(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            puncture(target, attack)
            return true
        }

        private fun ProtectedAccess.puncture(target: PathingEntity, attack: CombatAttack.Melee) {
            anim(special_seqs.dragon_dagger)
            spotanim(
                spot = special_spots.dragon_dagger,
                slot = constants.spotanim_slot_combat,
                height = 96,
            )

            val first =
                rollDamage(
                    target = target,
                    attack = attack,
                )
            val second =
                rollDamage(
                    target = target,
                    attack = attack,
                )
            manager.giveCombatXp(this, target, attack, first + second)
            manager.queueMeleeHit(this, target, first)
            manager.queueMeleeHit(this, target, second)
            manager.continueCombat(this, target)
        }

        private fun ProtectedAccess.rollDamage(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Int =
            manager.rollMeleeDamage(
                source = this,
                target = target,
                attack = attack,
                accuracyMultiplier = 1.15,
                maxHitMultiplier = 1.15,
                blockType = MeleeAttackType.Stab,
            )
    }
}
