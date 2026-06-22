package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.queues
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerSoftQueue
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object VengeanceComponents : ComponentReferences() {
    val vengeance = find("magic_spellbook:vengeance")
}

internal object VengeanceSeqs : SeqReferences() {
    val cast = find("vengeance_spell_anim_nostalling")
}

internal object VengeanceSpotanims : SpotanimReferences() {
    val cast = find("quest_lunar_spellbook_vengeance_spot_anim")
}

/**
 * Vengeance (lunar self-cast). De client toonde de spell wel (quest-gate is weg), maar er was geen
 * server-handler. Hier: bij klikken laad je Vengeance op (anim + graphic + [varbits.vengeance_rebound]),
 * met 30s cooldown. De daadwerkelijke reflect (75% van de volgende hit terug naar de aanvaller) zit in
 * StandardPlayerHitProcessor, dat [varbits.vengeance_rebound] uitleest en verbruikt.
 *
 * NB: spec-energie/runes zijn vrij op deze testserver, dus we checken alleen de cooldown.
 */
class VengeanceSpell
@Inject
constructor(private val protectedAccess: ProtectedAccessLauncher) : PluginScript() {
    private var Player.vengeanceActive by boolVarBit(varbits.vengeance_rebound)
    private var Player.vengeanceCooldown by boolVarBit(varbits.vengeance_timelimit)

    override fun ScriptContext.startup() {
        onIfOverlayButton(VengeanceComponents.vengeance) {
            when {
                player.vengeanceCooldown ->
                    player.mes("Vengeance is nog op cooldown (max elke 30s).")
                player.vengeanceActive -> player.mes("Je bent al opgeladen met Vengeance.")
                else ->
                    protectedAccess.launch(player) {
                        anim(VengeanceSeqs.cast)
                        spotanim(VengeanceSpotanims.cast)
                        player.vengeanceActive = true
                        player.vengeanceCooldown = true
                        player.mes("You feel charged with energy!")
                        player.softQueue(queues.generic_queue8, COOLDOWN_CYCLES)
                    }
            }
        }

        // Cooldown opheffen na 30s.
        onPlayerSoftQueue(queues.generic_queue8) { player.vengeanceCooldown = false }
    }

    private companion object {
        private const val COOLDOWN_CYCLES = 50 // ~30 seconden (0.6s per tick)
    }
}
