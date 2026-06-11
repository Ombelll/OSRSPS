package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onCommand
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object MiniObjs : ObjReferences() {
    val uncut_ruby = find("uncut_ruby")
    val uncut_diamond = find("uncut_diamond")
    val dragon_scimitar = find("dragon_scimitar")
    val abyssal_whip = find("abyssal_whip")
}

/**
 * MINIGAMES — maken de coins van de bosses bruikbaar.
 *
 *  - ::dice <bedrag>  : gok coins, 48% kans om te verdubbelen (lichte house-edge).
 *  - ::mystery        : open een mystery box voor een willekeurige beloning.
 */
class Minigames
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("dice") {
            desc = "Gamble coins: ::dice <amount> (48% to double)"
            cheat {
                val bet = args.getOrNull(0)?.toIntOrNull()
                if (bet == null || bet <= 0) {
                    player.mes("Usage: ::dice <amount>")
                    return@cheat
                }
                protectedAccess.launch(player) {
                    if (invTotal(inv, objs.coins) < bet) {
                        mes("You don't have $bet coins to gamble.")
                        return@launch
                    }
                    invDel(inv, objs.coins, bet)
                    if (random.of(maxExclusive = 100) < 48) {
                        invAdd(inv, objs.coins, bet * 2)
                        mes("The dice roll in your favour - you win $bet coins!")
                    } else {
                        mes("Unlucky! You lose $bet coins.")
                    }
                }
            }
        }

        onCommand("flip") {
            desc = "Coinflip: ::flip <amount> (49% to double)"
            cheat {
                val bet = args.getOrNull(0)?.toIntOrNull()
                if (bet == null || bet <= 0) {
                    player.mes("Usage: ::flip <amount>")
                    return@cheat
                }
                protectedAccess.launch(player) {
                    if (invTotal(inv, objs.coins) < bet) {
                        mes("You don't have $bet coins.")
                        return@launch
                    }
                    invDel(inv, objs.coins, bet)
                    if (random.of(maxExclusive = 100) < 49) {
                        invAdd(inv, objs.coins, bet * 2)
                        mes("Heads - you win $bet coins!")
                    } else {
                        mes("Tails - you lose $bet coins.")
                    }
                }
            }
        }

        onCommand("slots") {
            desc = "Slot machine: ::slots <amount>"
            cheat {
                val bet = args.getOrNull(0)?.toIntOrNull()
                if (bet == null || bet <= 0) {
                    player.mes("Usage: ::slots <amount>")
                    return@cheat
                }
                protectedAccess.launch(player) {
                    if (invTotal(inv, objs.coins) < bet) {
                        mes("You don't have $bet coins.")
                        return@launch
                    }
                    invDel(inv, objs.coins, bet)
                    when (random.of(maxExclusive = 100)) {
                        in 0 until 2 -> {
                            invAdd(inv, objs.coins, bet * 10)
                            mes("JACKPOT! Three 7s - you win ${bet * 10} coins!")
                        }
                        in 2 until 12 -> {
                            invAdd(inv, objs.coins, bet * 3)
                            mes("Three of a kind! You win ${bet * 3} coins.")
                        }
                        in 12 until 35 -> {
                            invAdd(inv, objs.coins, bet * 2)
                            mes("A matching pair! You win ${bet * 2} coins.")
                        }
                        else -> mes("No match - you lose $bet coins.")
                    }
                }
            }
        }

        onCommand("mystery") {
            desc = "Open a mystery box for a random reward"
            cheat {
                protectedAccess.launch(player) {
                    when (random.of(maxExclusive = 100)) {
                        in 0 until 50 -> {
                            val amount = 5_000 + random.of(maxExclusive = 10_001)
                            invAdd(inv, objs.coins, amount)
                            mes("Mystery box: a pile of $amount coins!")
                        }
                        in 50 until 75 -> {
                            invAdd(inv, MiniObjs.uncut_ruby)
                            mes("Mystery box: an uncut ruby!")
                        }
                        in 75 until 90 -> {
                            invAdd(inv, MiniObjs.uncut_diamond)
                            mes("Mystery box: an uncut diamond!")
                        }
                        in 90 until 98 -> {
                            invAdd(inv, MiniObjs.dragon_scimitar)
                            mes("Mystery box: a dragon scimitar - nice!")
                        }
                        else -> {
                            invAdd(inv, MiniObjs.abyssal_whip)
                            mes("Mystery box: JACKPOT - an abyssal whip!")
                        }
                    }
                }
            }
        }
    }
}
