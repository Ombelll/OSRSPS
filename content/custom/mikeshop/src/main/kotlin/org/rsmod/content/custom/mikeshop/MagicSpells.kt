package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * Maakt de TELEPORT-spells werkend over alle 4 spellbooks. Elke teleport wordt alleen gewired als
 * de bestemmingsregio op de server geladen is (zone-validatie via CollisionFlagMap.isZoneAllocated);
 * bestemmingen die niet geladen zijn worden overgeslagen en gelogd ([MAGIC-TP]). Zo faalt geen enkele
 * teleport stil (zoals eerder met onbekende XTEA-regio's).
 *
 * Home Teleport gaat op ELKE spellbook naar Edgeville (de PK-hub op deze server).
 */
class MagicSpells
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val protectedAccess: ProtectedAccessLauncher,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    private data class Tele(val label: String, val obj: ObjType, val dest: CoordGrid)

    override fun ScriptContext.startup() {
        val edge = CoordGrid(3087, 3496)
        val teleports =
            listOf(
                // Home Teleport (alle 4 books) -> Edgeville.
                Tele("home:standard", objs.spell_hometeleport_lumbridge, edge),
                Tele("home:ancients", objs.spell_hometeleport_edgeville, edge),
                Tele("home:lunar", objs.spell_hometeleport_lunar, edge),
                Tele("home:arceuus", objs.spell_hometeleport_arceuus, edge),
                // Standard teleports.
                Tele("Lumbridge", objs.spell_lumbridge_teleport, CoordGrid(3222, 3218)),
                Tele("Varrock", objs.spell_varrock_teleport, CoordGrid(3213, 3424)),
                Tele("Falador", objs.spell_falador_teleport, CoordGrid(2965, 3379)),
                Tele("Camelot", objs.spell_camelot_teleport, CoordGrid(2757, 3477)),
                Tele("Ardougne", objs.spell_ardougne_teleport, CoordGrid(2662, 3305)),
                Tele("Watchtower", objs.spell_watchtower_teleport, CoordGrid(2549, 3112)),
                Tele("Trollheim", objs.spell_trollheim_teleport, CoordGrid(2891, 3678)),
                // Ancient teleports.
                Tele("Senntisten", objs.spell_senntisten_teleport, CoordGrid(3322, 3336)),
                Tele("Paddewwa", objs.spell_paddewwa_teleport, CoordGrid(3097, 9884)),
                Tele("Lassar", objs.spell_lassar_teleport, CoordGrid(3006, 3471)),
                Tele("Dareeyak", objs.spell_dareeyak_teleport, CoordGrid(2972, 3697)),
                Tele("Annakarl", objs.spell_annakarl_teleport, CoordGrid(3288, 3886)),
                Tele("Ghorrock(anc)", objs.spell_ghorrock_teleport, CoordGrid(2977, 3873)),
                // Lunar teleports.
                Tele("Moonclan", objs.spell_moonclan_teleport, CoordGrid(2111, 3915)),
                Tele("Waterbirth", objs.spell_waterbirth_teleport, CoordGrid(2546, 3760)),
                Tele("Catherby", objs.spell_catherby_teleport, CoordGrid(2802, 3449)),
                Tele("Ghorrock(lun)", objs.spell_lunarghorrock_teleport, CoordGrid(2977, 3873)),
            )

        println("[MAGIC-TP] === Teleport-validatie & wiring (alleen geladen regio's) ===")
        var wired = 0
        var skipped = 0
        for (t in teleports) {
            val spell = spells.getObjSpell(t.obj)
            if (spell == null) {
                println("[MAGIC-TP] ${t.label} -> spell niet gevonden -> OVERGESLAGEN")
                skipped++
                continue
            }
            if (!collision.isZoneAllocated(t.dest.x, t.dest.z, t.dest.level)) {
                println("[MAGIC-TP] ${t.label} -> ${t.dest.x},${t.dest.z} -> regio NIET geladen -> niet gewired")
                skipped++
                continue
            }
            val dest = t.dest
            onIfOverlayButton(spell.component) {
                if (TeleBlockState.isBlocked(player)) {
                    player.mes(
                        "A magical force prevents you from teleporting " +
                            "(${TeleBlockState.remainingSeconds(player)}s left)."
                    )
                } else {
                    protectedAccess.launch(player) { telejump(dest) }
                }
            }
            println("[MAGIC-TP] ${t.label} -> ${t.dest.x},${t.dest.z} -> geladen -> GEWIRED")
            wired++
        }
        println("[MAGIC-TP] === klaar: $wired gewired, $skipped overgeslagen ===")
    }
}
