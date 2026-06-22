package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
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
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.spot.SpotanimType

class PkMeleeSpecialAttacks : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee(
            objs.armadyl_godsword,
            SingleHit(
                manager = manager,
                seq = special_seqs.armadyl_godsword,
                spot = special_spots.armadyl_godsword,
                accuracyMultiplier = 2.0,
                maxHitMultiplier = 1.375,
                blockType = MeleeAttackType.Slash,
            ),
        )

        registerMelee(objs.dragon_claws, DragonClaws(manager))

        val graniteMaul = GraniteMaul(manager)
        registerMelee(objs.granite_maul, graniteMaul)
        registerMelee(objs.granite_maul_pretty, graniteMaul)
        registerMelee(objs.granite_maul_plus, graniteMaul)
        registerMelee(objs.granite_maul_pretty_plus, graniteMaul)

        val dragonWarhammer =
            SingleHit(
                manager = manager,
                seq = special_seqs.dragon_warhammer,
                spot = special_spots.dragon_warhammer,
                accuracyMultiplier = 1.5,
                maxHitMultiplier = 1.5,
                blockType = MeleeAttackType.Crush,
            )
        registerMelee(objs.dragon_warhammer, dragonWarhammer)
        registerMelee(objs.dragon_warhammer_ornament, dragonWarhammer)

        val abyssalDagger =
            DoubleHit(
                manager = manager,
                seq = special_seqs.abyssal_dagger,
                spot = special_spots.abyssal_dagger,
                accuracyMultiplier = 1.25,
                maxHitMultiplier = 0.85,
                blockType = MeleeAttackType.Stab,
            )
        registerMelee(objs.abyssal_dagger, abyssalDagger)
        registerMelee(objs.abyssal_dagger_p, abyssalDagger)
        registerMelee(objs.abyssal_dagger_p_plus, abyssalDagger)
        registerMelee(objs.abyssal_dagger_p_plus_plus, abyssalDagger)

        registerMelee(
            objs.osmumtens_fang,
            SingleHit(
                manager = manager,
                seq = special_seqs.osmumtens_fang,
                spot = null,
                accuracyMultiplier = 1.5,
                maxHitMultiplier = 1.25,
                blockType = MeleeAttackType.Stab,
            ),
        )
        registerMelee(
            objs.osmumtens_fang_or,
            SingleHit(
                manager = manager,
                seq = special_seqs.osmumtens_fang,
                spot = null,
                accuracyMultiplier = 1.5,
                maxHitMultiplier = 1.25,
                blockType = MeleeAttackType.Stab,
            ),
        )

        registerMelee(objs.voidwaker, Voidwaker(manager))
    }

    /**
     * Voidwaker-spec: gegarandeerde hit van 50%-150% van de normale max hit, die door
     * protect-prayers heen gaat (alleen tegen spelers). 50% energie (uit de wapen-config).
     */
    private class Voidwaker(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee) =
            slash(target, attack, bypassPrayer = false)

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Melee) =
            slash(target, attack, bypassPrayer = true)

        private fun ProtectedAccess.slash(
            target: PathingEntity,
            attack: CombatAttack.Melee,
            bypassPrayer: Boolean,
        ): Boolean {
            anim(special_seqs.voidwaker)
            spotanim(
                spot = special_spots.voidwaker,
                slot = constants.spotanim_slot_combat,
                height = 96,
            )
            // Gegarandeerde hit (geen accuracy-roll): 50%-150% van de normale max hit.
            val max = manager.calculateMeleeMaxHit(this, target, attack.type, attack.style, 1.0)
            val roll = manager.rollMeleeMaxHit(this, target, attack.type, attack.style, 1.0)
            val damage = max / 2 + roll
            manager.giveCombatXp(this, target, attack, damage)
            if (bypassPrayer && target is Player) {
                // Voidwaker negeert protect-prayers: deal de hit ongemodificeerd.
                target.queueHit(player, 1, HitType.Melee, damage, modifier = NoopPlayerHitModifier)
            } else {
                manager.queueMeleeHit(this, target, damage)
            }
            manager.continueCombat(this, target)
            return true
        }
    }

    private class SingleHit(
        private val manager: SpecialAttackManager,
        private val seq: SeqType,
        private val spot: SpotanimType?,
        private val accuracyMultiplier: Double,
        private val maxHitMultiplier: Double,
        private val blockType: MeleeAttackType,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee) =
            strike(target, attack)

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Melee) =
            strike(target, attack)

        private fun ProtectedAccess.strike(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            playFx(seq, spot)
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = accuracyMultiplier,
                    maxHitMultiplier = maxHitMultiplier,
                    blockType = blockType,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
            return true
        }
    }

    private class DoubleHit(
        private val manager: SpecialAttackManager,
        private val seq: SeqType,
        private val spot: SpotanimType,
        private val accuracyMultiplier: Double,
        private val maxHitMultiplier: Double,
        private val blockType: MeleeAttackType,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee) =
            stab(target, attack)

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Melee) =
            stab(target, attack)

        private fun ProtectedAccess.stab(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            playFx(seq, spot)
            val first = roll(target, attack)
            val second = roll(target, attack)
            manager.giveCombatXp(this, target, attack, first + second)
            manager.queueMeleeHit(this, target, first)
            manager.queueMeleeHit(this, target, second)
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.roll(target: PathingEntity, attack: CombatAttack.Melee): Int =
            manager.rollMeleeDamage(
                source = this,
                target = target,
                attack = attack,
                accuracyMultiplier = accuracyMultiplier,
                maxHitMultiplier = maxHitMultiplier,
                blockType = blockType,
            )
    }

    private class DragonClaws(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee) =
            slice(target, attack)

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Melee) =
            slice(target, attack)

        private fun ProtectedAccess.slice(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            playFx(special_seqs.dragon_claws, special_spots.dragon_claws)
            val damages =
                IntArray(4) {
                    manager.rollMeleeDamage(
                        source = this,
                        target = target,
                        attack = attack,
                        accuracyMultiplier = 1.4,
                        maxHitMultiplier = 0.75,
                        blockType = MeleeAttackType.Slash,
                    )
                }
            manager.giveCombatXp(this, target, attack, damages.sum())
            for (damage in damages) {
                manager.queueMeleeHit(this, target, damage)
            }
            manager.continueCombat(this, target)
            return true
        }
    }

    private class GraniteMaul(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee) =
            pound(target, attack)

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Melee) =
            pound(target, attack)

        private fun ProtectedAccess.pound(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            anim(special_seqs.granite_maul)
            spotanim(spotanims.sp_attackglow_red, slot = constants.spotanim_slot_combat, height = 96)
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.25,
                    maxHitMultiplier = 1.25,
                    blockType = MeleeAttackType.Crush,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
            return true
        }
    }

    private companion object {
        private fun ProtectedAccess.playFx(seq: SeqType, spot: SpotanimType?) {
            anim(seq)
            if (spot != null) {
                spotanim(spot = spot, slot = constants.spotanim_slot_combat, height = 96)
            }
        }
    }
}
