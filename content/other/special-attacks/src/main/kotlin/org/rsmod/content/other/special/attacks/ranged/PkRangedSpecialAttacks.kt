package org.rsmod.content.other.special.attacks.ranged

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.content.other.special.attacks.configs.special_seqs
import org.rsmod.content.other.special.attacks.configs.special_spots
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

class PkRangedSpecialAttacks : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val magicShortbow = MagicShortbow(manager)
        registerRanged(objs.magic_shortbow, magicShortbow)
        registerRanged(objs.magic_shortbow_i, magicShortbow)

        val blowpipe = ToxicBlowpipe(manager)
        registerRanged(objs.toxic_blowpipe, blowpipe)
        registerRanged(objs.toxic_blowpipe_loaded, blowpipe)
        registerRanged(objs.toxic_blowpipe_ornament, blowpipe)
        registerRanged(objs.toxic_blowpipe_loaded_ornament, blowpipe)

        registerRanged(objs.heavy_ballista, HeavyBallista(manager))
    }

    /** Heavy ballista "Concentrated Shot": verhoogde accuracy + damage, enkele hit. */
    private class HeavyBallista(private val manager: SpecialAttackManager) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Ranged) =
            shot(target, attack)

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Ranged) =
            shot(target, attack)

        private fun ProtectedAccess.shot(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            anim(seqs.human_bow)
            spotanim(
                spot = spotanims.ballista_special,
                slot = constants.spotanim_slot_combat,
                height = 96,
            )
            val damage =
                manager.rollRangedDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.25,
                    maxHitMultiplier = 1.25,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueRangedHit(this, target, ammo = null, damage = damage, clientDelay = 30)
            manager.continueCombat(this, target)
            return true
        }
    }

    private class MagicShortbow(private val manager: SpecialAttackManager) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Ranged) =
            snapshot(target, attack)

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Ranged) =
            snapshot(target, attack)

        private fun ProtectedAccess.snapshot(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            anim(seqs.human_bow)
            val first =
                manager.rollRangedDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.1,
                    maxHitMultiplier = 1.0,
                )
            val second =
                manager.rollRangedDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.1,
                    maxHitMultiplier = 1.0,
                )
            manager.giveCombatXp(this, target, attack, first + second)
            manager.queueRangedHit(this, target, ammo = null, damage = first, clientDelay = 30)
            manager.queueRangedDamage(this, target, ammo = null, damage = second, hitDelay = 2)
            manager.continueCombat(this, target)
            return true
        }
    }

    private class ToxicBlowpipe(private val manager: SpecialAttackManager) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Ranged) =
            siphon(target, attack)

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Ranged) =
            siphon(target, attack)

        private fun ProtectedAccess.siphon(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            anim(special_seqs.toxic_blowpipe)
            spotanim(
                spot = special_spots.toxic_blowpipe,
                slot = constants.spotanim_slot_combat,
                height = 96,
            )
            val damage =
                manager.rollRangedDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.5,
                    maxHitMultiplier = 1.5,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueRangedHit(this, target, ammo = null, damage = damage, clientDelay = 30)
            manager.continueCombat(this, target)
            return true
        }
    }
}
