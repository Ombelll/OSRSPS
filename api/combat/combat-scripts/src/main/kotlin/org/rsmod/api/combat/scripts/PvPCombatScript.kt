package org.rsmod.api.combat.scripts

import jakarta.inject.Inject
import kotlin.math.abs
import kotlin.math.min
import org.rsmod.api.combat.PvPCombat
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.commons.styles.AttackStyle
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.player.aggressiveNpc
import org.rsmod.api.combat.player.attackRange
import org.rsmod.api.combat.player.pkPredator1
import org.rsmod.api.combat.player.resolveAutocastSpell
import org.rsmod.api.combat.player.resolveCombatAttack
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.queues
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.walktriggers
import org.rsmod.api.player.isInPvpCombat
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.advanced.onApPlayer2
import org.rsmod.api.script.advanced.onOpPlayer2
import org.rsmod.api.script.onApPlayerT
import org.rsmod.api.script.onPlayerWalkTrigger
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.spells.autocast.AutocastWeapons
import org.rsmod.game.entity.Player
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Freeze-state voor ice-spells (Ice Rush/Burst/Blitz/Barrage). Houdt per speler bij tot welke
 * game-tick hij bevroren is en tot wanneer hij immuun is (kan dan niet meteen herbevroren worden).
 * Bewegingsblokkering loopt via de [walktriggers.pvp_frozen] walk-trigger in [PvPCombatScript].
 */
internal object FreezeState {
    private val frozenUntil = HashMap<Int, Int>()
    private val immuneUntil = HashMap<Int, Int>()

    fun isFrozen(player: Player): Boolean {
        val until = frozenUntil[player.slotId] ?: return false
        return player.currentMapClock < until
    }

    /** Mag [player] nu bevroren worden? (Niet al bevroren en niet in de immuniteitsperiode.) */
    fun canFreeze(player: Player): Boolean {
        if (isFrozen(player)) {
            return false
        }
        val immune = immuneUntil[player.slotId] ?: return true
        return player.currentMapClock >= immune
    }

    fun freeze(target: Player, durationTicks: Int) {
        val now = target.currentMapClock
        frozenUntil[target.slotId] = now + durationTicks
        // Immuniteit = duur van de freeze erna (OSRS-stijl), zodat je niet eindeloos vastgezet wordt.
        immuneUntil[target.slotId] = now + durationTicks + durationTicks
        target.walkTrigger(walktriggers.pvp_frozen)
        target.abortRoute()
    }
}

internal class PvPCombatScript
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val styles: AttackStyles,
    private val types: AttackTypes,
    private val combat: PvPCombat,
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val autocast: AutocastWeapons,
) : PluginScript() {
    // PvP staat alleen aan op de PvP-wereld (env RSMOD_WORLD=2); andere werelden zijn veilig.
    private val pvpWorld = System.getenv("RSMOD_WORLD") == "2"
    private var Player.skullPrevention by boolVarBit(varbits.skull_prevent)

    // Ice-spell-component -> freeze-duur in ticks. Opgebouwd bij startup uit de spell-registry.
    private var iceFreezeDurations: Map<ComponentType, Int> = emptyMap()

    override fun ScriptContext.startup() {
        onApPlayer2 { attemptCombatAp(it.target) }
        onOpPlayer2 { attemptCombatOp(it.target) }
        for (spell in spells.combatSpells()) {
            onApPlayerT(spell.component) { attemptCombatSpell(it.target, spell) }
        }
        iceFreezeDurations = buildIceFreezeDurations()
        // Blokkeer beweging zolang bevroren; de walk-trigger self-cleart na afloop (eerste move).
        onPlayerWalkTrigger(walktriggers.pvp_frozen) {
            if (FreezeState.isFrozen(player)) {
                player.abortRoute()
            } else {
                player.clearWalkTrigger()
            }
        }
    }

    private fun buildIceFreezeDurations(): Map<ComponentType, Int> {
        val tiers =
            listOf(
                objs.spell_ice_rush to 8, // ~5s
                objs.spell_ice_burst to 16, // ~10s
                objs.spell_ice_blitz to 24, // ~15s
                objs.spell_ice_barrage to 32, // ~20s
            )
        return tiers
            .mapNotNull { (obj, ticks) -> spells.getObjSpell(obj)?.component?.let { it to ticks } }
            .toMap()
    }

    /** Bevriest [target] als [spell] een ice-spell is (en het doelwit niet al bevroren/immuun is). */
    private fun applyIceFreeze(spell: MagicSpell, target: Player) {
        val duration = iceFreezeDurations[spell.component] ?: return
        if (!FreezeState.canFreeze(target)) {
            return
        }
        FreezeState.freeze(target, duration)
        target.mes("You have been frozen!")
    }

    private suspend fun ProtectedAccess.attemptCombatAp(target: Player) {
        val type = types.get(player)
        val style = styles.get(player)
        val attackRange = attackRange(style)
        val canAttack = canAttack(target)

        // Weapons such as salamanders have an attack range of `1` but can attack with both ranged
        // and magic. These attacks should be treated as ap range, not op.
        val isMeleeAttackType = type == null || type.isMelee
        if (attackRange == 1 && isMeleeAttackType) {
            apRange(-1)
            return
        }

        if (!canAttack) {
            return
        }

        if (!isWithinDistance(target, attackRange)) {
            apRange(attackRange)
            return
        }

        val spell = resolveAutocastSpell(objTypes, spells, runes, autocast)
        val attack = resolveCombatAttack(player.righthand, type, style, spell)
        combat.attack(this, target, attack)
        spell?.let { applyIceFreeze(it, target) }
    }

    private suspend fun ProtectedAccess.attemptCombatOp(target: Player) {
        if (!canAttack(target)) {
            return
        }
        val type = types.get(player)
        val style = styles.get(player)

        val spell = resolveAutocastSpell(objTypes, spells, runes, autocast)
        val attack = resolveCombatAttack(player.righthand, type, style, spell)
        combat.attack(this, target, attack)
        spell?.let { applyIceFreeze(it, target) }
    }

    private suspend fun ProtectedAccess.attemptCombatSpell(target: Player, spell: MagicSpell) {
        val canCast = runes.canCastSpell(player, spell)
        if (!canCast) {
            return
        }
        // Official behavior: `canAttack` checks occur _after_ `canCastSpell` checks.
        val canAttack = canAttack(target)
        if (!canAttack) {
            return
        }
        // Note: Ap range condition is not necessary as magic spells can be cast from `10` tiles
        // away, which is the same as the default engine valid-ap range.
        val attack = resolveCombatAttack(player.righthand, null, null, spell)
        combat.attack(this, target, attack)
        applyIceFreeze(spell, target)
    }

    private fun ProtectedAccess.canAttack(target: Player): Boolean {
        // Spelers aanvallen kan alleen op de PvP-wereld (RSMOD_WORLD=2); wereld 1 is veilig.
        if (!pvpWorld) {
            mes("You can only attack other players on the PvP world (World 2).")
            clearPendingAction()
            return false
        }
        // Veilige zone: geen PvP bij de Edgeville-bank/hub (banken/gear pakken zonder gevaar).
        if (inBankSafeZone(player) || inBankSafeZone(target)) {
            mes("You can't fight here - the bank area is a safe zone. Head north into the Wilderness.")
            clearPendingAction()
            return false
        }
        if (!isWithinWildernessRange(target)) {
            clearPendingAction()
            return false
        }
        if (wouldSkullOn(target) && player.skullPrevention) {
            mes("Your skull prevention stops you from attacking ${target.displayName}.")
            clearPendingAction()
            return false
        }
        val weapon = objTypes.getOrNull(player.righthand)
        if (weapon != null && weapon.isCategoryType(categories.dinhs_bulwark)) {
            val attackStyle = styles.get(player)
            // Dinh's "Block" attack style uses `AggressiveMelee` as its "dummy" attack style.
            if (attackStyle == AttackStyle.AggressiveMelee) {
                mes("Your bulwark gets in the way.")
                clearPendingAction()
                return false
            }
        }

        // Dinh's bulwark style-switching delay is added to a queue and is applied globally during
        // this condition check. This means even if you quickly change to another melee weapon and
        // re-interact with a target, you will _not_ move into op range.
        if (queues.dinhs_combat_delay in player.queueList) {
            clearPendingAction()
            return false
        }

        // TODO(combat): Updated multiway logic.
        // TODO(combat): Add singles plus support.
        val singleCombat = !mapMultiway()
        if (singleCombat) {
            if (isInCombat()) {
                if (pkPredator1 != null && pkPredator1 != target.uid) {
                    spam("I'm already under attack.")
                    return false
                }

                val aggressiveNpc = aggressiveNpc
                if (aggressiveNpc != null && findUid(aggressiveNpc) != null) {
                    spam("I'm already under attack.")
                    return false
                }
            }

            if (target.isInPvpCombat()) {
                if (target.pkPredator1 != null && target.pkPredator1 != player.uid) {
                    mes("${target.displayName} is fighting another player.")
                    return false
                }
            }
        }
        return true
    }

    private fun ProtectedAccess.wouldSkullOn(target: Player): Boolean =
        player.skullIcon == null && pkPredator1 != target.uid

    private fun ProtectedAccess.isWithinWildernessRange(target: Player): Boolean {
        val sourceLevel = player.wildernessLevel()
        val targetLevel = target.wildernessLevel()
        if (sourceLevel == null && targetLevel == null) {
            return true
        }
        if (sourceLevel == null || targetLevel == null) {
            mes("You can only attack players who are also in the Wilderness.")
            return false
        }

        val allowedDifference = min(sourceLevel, targetLevel)
        val combatDifference = abs(player.combatLevel - target.combatLevel)
        if (combatDifference <= allowedDifference) {
            return true
        }

        mes(
            "You need to move deeper into the Wilderness to attack ${target.displayName}. " +
                "Your level range is $allowedDifference."
        )
        return false
    }

    // Edgeville PK-hub/bank veilige zone (bank + shops). Ruim ten zuiden van de wildernis-ditch
    // (z 3520), zodat je veilig kunt banken/gearen; noordwaarts de wild in geldt PvP weer.
    private fun inBankSafeZone(player: Player): Boolean {
        val x = player.coords.x
        val z = player.coords.z
        return x in 3078..3099 && z in 3486..3512
    }

    private fun Player.wildernessLevel(): Int? {
        val x = coords.x
        val z = coords.z
        if (x !in 2944..3391 || z !in 3520..4351) {
            return null
        }
        return (((z - 3520) / 8) + 1).coerceIn(1, 56)
    }
}
