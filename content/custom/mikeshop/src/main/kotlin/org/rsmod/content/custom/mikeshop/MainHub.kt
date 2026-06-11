package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.loc.LocLayerConstants

/**
 * De centrale hub-coordinaat: het Grand Exchange-plein. Open, centraal en de natuurlijke
 * handels-hub. De GE-regio (mapsquare 49,54) wordt wel degelijk geladen (de m/l-cachebestanden
 * + XTEA-sleutel zijn aanwezig), dus telejump hierheen werkt.
 */
internal val HUB: CoordGrid = CoordGrid(0, 49, 54, 28, 30)

/** Zorgt dat de hub-meubels (NPC's + skilling-locs) maar een keer worden gespawnd. */
internal object HubState {
    var built = false
}

internal object HubNpcs : NpcReferences() {
    val clerk = find("ge_clerk_1")
    val banker = find("banker1")
    val wizard = find("wise_old_man")
}

/**
 * MAIN HUB: iedereen teleporteert bij login naar de hub (::hub om terug te keren).
 *
 * Op de hub staan:
 *  - een shop-clerk  -> praat met 'm voor een keuzemenu met alle 4 winkels
 *  - een banker      -> praat met 'm om je bank te openen (vanilla bank-gedrag)
 *  - een skilling-zone (erts-rotsen + oven + aambeeld + altaar) om direct te trainen
 *
 * De meubels worden eenmalig gespawnd bij de eerste login (wereld is dan zeker geladen).
 */
class MainHub
@Inject
constructor(
    private val shops: Shops,
    private val npcRepo: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val locTypes: LocTypeList,
    private val locRepo: LocRepository,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    // World 2 (RSMOD_WORLD=2) is the PvP world: spawns in Edgeville and must NOT be teleported to
    // the GE hub. The GE hub + login-teleport are world-1 only.
    private val pvpWorld = System.getenv("RSMOD_WORLD") == "2"

    override fun ScriptContext.startup() {
        onPlayerLogin {
            if (!HubState.built && !pvpWorld) {
                HubState.built = true
                buildHub()
            }
            if (!pvpWorld) {
                protectedAccess.launch(player) { telejump(HUB) }
                player.mes("Welcome to the Hub, ${player.displayName}!")
                player.mes("Talk to the Shop Clerk for all shops, or the Banker to bank.")
            } else {
                player.mes("Welcome to the PvP world, ${player.displayName}!")
                player.mes("You're in Edgeville - talk to a PK shopkeeper for FREE gear, then hit the Wild!")
            }
            player.mes("Boss kills so far: ${PvmProgress.bossKills(player)}. Type ::help for commands.")
        }

        onOpNpc1(HubNpcs.clerk) { openShopMenu() }

        onCommand("hub") {
            desc = "Teleport to the main hub"
            cheat { protectedAccess.launch(player) { telejump(HUB) } }
        }
    }

    private suspend fun ProtectedAccess.openShopMenu() {
        val pick =
            choice5(
                "Mike's Gear Shop",
                1,
                "Supply Shop",
                2,
                "General Store",
                3,
                "Potion & Food Shop",
                4,
                "Nothing",
                0,
                title = "Which shop would you like, traveller?",
            )
        when (pick) {
            1 ->
                shops.open(
                    player = player,
                    title = "Mike's Custom Shop",
                    shopInv = MikeShopInvs.mike_shop,
                    buyPercentage = 100.0,
                    sellPercentage = 80.0,
                    changePercentage = 2.0,
                )
            2 ->
                shops.open(
                    player = player,
                    title = "Supply Shop",
                    shopInv = SupplyShopInvs.supply_shop,
                    buyPercentage = 100.0,
                    sellPercentage = 60.0,
                    changePercentage = 1.0,
                )
            3 ->
                shops.open(
                    player = player,
                    title = "General Store",
                    shopInv = GeneralStoreInvs.general_store,
                    buyPercentage = 100.0,
                    sellPercentage = 55.0,
                    changePercentage = 2.0,
                )
            4 ->
                shops.open(
                    player = player,
                    title = "Potion & Food Shop",
                    shopInv = PotionShopInvs.potion_shop,
                    buyPercentage = 100.0,
                    sellPercentage = 60.0,
                    changePercentage = 1.0,
                )
        }
    }

    /** Spawnt de hub-meubels rondom HUB. NPC-spawns zijn omhuld met try/catch (geblokkeerde tegel). */
    private fun buildHub() {
        spawnNpc(HubNpcs.clerk, HUB.translate(1, 1))
        spawnNpc(HubNpcs.banker, HUB.translate(-1, 1))
        spawnNpc(HubNpcs.wizard, HUB.translate(0, 3))
        spawnLoc(SkillZoneLocs.furnace, HUB.translate(4, 3))
        spawnLoc(SkillZoneLocs.anvil, HUB.translate(4, 1))
        spawnLoc(SkillZoneLocs.altar, HUB.translate(4, -1))
        spawnLoc(SkillZoneLocs.copperrock1, HUB.translate(-4, 3))
        spawnLoc(SkillZoneLocs.tinrock1, HUB.translate(-4, 1))
        spawnLoc(SkillZoneLocs.ironrock1, HUB.translate(-4, -1))
        spawnLoc(SkillZoneLocs.coalrock1, HUB.translate(-4, -3))
    }

    private fun spawnNpc(ref: NpcType, coords: CoordGrid) {
        try {
            val npc = Npc(npcTypes[ref], coords)
            npc.mode = NpcMode.None
            npcRepo.add(npc, duration = Int.MAX_VALUE)
        } catch (e: Exception) {
            // Tegel geblokkeerd of npc niet plaatsbaar -> sla deze meubel over, crash de server niet.
        }
    }

    private fun spawnLoc(ref: LocType, coords: CoordGrid) {
        val type = locTypes[ref]
        val shape = LocShape.CentrepieceStraight.id
        val angle = LocAngle.West.id
        val loc = LocInfo(LocLayerConstants.of(shape), coords, LocEntity(type.id, shape, angle))
        locRepo.add(loc, duration = Int.MAX_VALUE)
    }
}
