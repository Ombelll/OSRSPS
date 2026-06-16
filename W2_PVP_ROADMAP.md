# W2 PvP World Roadmap

Doel: World 2 moet voelen als een snelle, eerlijke PK-testwereld: inloggen, gear pakken, vechten,
punten verdienen, opnieuw klaarstaan. Alles hieronder is gericht op `RSMOD_WORLD=2`,
poort `43595`, DB `.data/saves/game_w2.db`.

## Huidige Staat

- W2 draait los van W1 met eigen DB en Edgeville spawn/respawn.
- PvP is world-gated: spelers aanvallen kan alleen op W2.
- `Attack`, `Follow`, `Trade with` en `Report` player-ops worden bij login gezet.
- `::pvpops` refresht player-ops als `Attack` niet zichtbaar is.
- `::pkready` zet je op de Edgeville PK hub en refresht player-ops.
- Gratis PK-shops bestaan voor melee, ranged, magic, supplies, food en potions.
- `::maxgear` unlockt gear, combat stats, prayers, ancient magicks, quest points en quest gates.
- PK-punten/streaks zijn persistent via varps.
- Wilderness ditch werkt met jump-anim en springt vanaf huidige X over de ditch.
- Kill loot v1: slachtoffer houdt top 3 waardevolle stacks, rest dropt owner-locked voor killer.

## Fase 1 - Directe PvP Betrouwbaarheid

Doel: twee clients kunnen zonder gedoe op W2 vechten.

- Check `Attack` zichtbaar na normale login op W2.
- Check `::pvpops` herstelt `Attack` zonder relog.
- Check `::pkready` teleporteert naar Edgeville en zet `Attack`.
- Check PvP buiten wilderness blijft open test-PvP op W2.
- Check binnen wilderness: beide spelers moeten in wilderness staan en binnen combat-range vallen.
- Check skull prevention: met skull prevention aan moet onveilige attack geblokkeerd worden.
- Check dat W1 veilig blijft: geen `Attack`, en PvP-aanval geeft veilige melding.

Acceptatie:
- Twee clients kunnen binnen 2 minuten na login gear pakken en elkaar aanvallen.
- Geen server error in `world2.err.log`.
- Na kill stijgt killer `::pkpoints`.

## Fase 2 - PK Gear Flow

Doel: minder voorbereiding, meer vechten.

- Maak een compact `::brid` loadout voor hybride NH/tribrid.
- Maak `::veng` loadout voor melee/range veng PK.
- Maak `::pure` loadout voor lage defence tests.
- Maak `::tank` loadout voor tank/anti-PK tests.
- Voeg `::restock` toe: food, karambwans, brews, restores, runes, arrows/bolts.
- Zorg dat alle loadouts free inventory slots respecteren en geen belangrijke items overschrijven.

Acceptatie:
- Elke loadout geeft bruikbare worn gear plus switches/supplies.
- `::restock` werkt tijdens meerdere fights zonder lege inventory-problemen.

## Fase 3 - Wilderness Rules v2

Doel: wilderness voelt herkenbaar en minder exploitable.

- Protect item meenemen in item-retention.
- Untradeables apart behandelen: broken/drop-convert/keep-regels kiezen.
- Speciale PK items waarde geven zodat top-3 retentie klopt.
- Teleblock toevoegen of minimaal voorbereiden als spell/effect.
- PJ-timer/singles-plus basis toevoegen.
- Multiway zones controleren en documenteren.
- Ditch testcases toevoegen voor noord/zuid, lange locs en spam-click.

Acceptatie:
- Loot na death is voorspelbaar.
- Speler kan geen obvious dupe/item-loss veroorzaken door logout, death of full inventory.

## Fase 4 - Rewards En Economy

Doel: kills geven motivatie zonder economie kapot te maken.

- `::pkpoints` duidelijker maken: punten, kills, streak, beste streak.
- PK shop uitbreiden met cosmetics, supplies en risk-items.
- Streak rewards toevoegen met zachte caps.
- Anti-farm regels: zelfde IP/account-pair cooldown of diminishing returns.
- Hiscores W2-tab uitbreiden met kills, deaths, KD, streak en points.

Acceptatie:
- Punten voelen nuttig maar gratis shops blijven bruikbaar voor testen.
- 2-account farmen levert beperkt voordeel op.

## Fase 5 - PvP UX En Hub

Doel: Edgeville als echte PK lobby.

- Noticeboard uitbreiden met duidelijke W2 commands.
- Bank, shops en ditch route visueel controleren.
- Voeg duel-ready area toe net noord/zuid van Edgeville voor quick fights.
- Voeg safe reset command toe: `::resetfight` voor spec, hp, prayer, skull clear of cooldowns.
- Voeg optional `::skull` toe voor vrijwillig skulled testen.
- Voeg broadcast toe bij killstreaks.

Acceptatie:
- Nieuwe speler ziet zonder uitleg waar gear, bank, food en fight-start zijn.

## Fase 6 - Structurele 2-Client Test

Doel: de echte multiplayer-check afvinken.

Testscript:
1. Start W1 en W2 via `start-worlds.ps1`.
2. Login client A en B op W2.
3. Run beide: `::pkready`, daarna `::maxgear` of loadout command.
4. Right-click speler: `Attack` zichtbaar.
5. Fight 1 buiten wilderness: kill A -> B, check `::pkpoints`.
6. Fight 2 binnen wilderness: check combat range en loot drop.
7. Logout beide spelers, restart W2, login opnieuw.
8. Check punten/streak persistent.
9. Check `world2.err.log` leeg.

Acceptatie:
- Roadmap Fase 6 mag op "klaar" als dit script zonder server errors slaagt.

## Fase 7 - Later / Nice To Have

- Bounty target systeem.
- PvP tournament mini-event.
- Risk-fight escrow.
- Real trade UI afronden voor W2.
- Clan war / team capes.
- LMS-achtige test arena.
- Web hiscores met live W2 PvP cards.

## Snelle Commando-Checklist

- Start: `powershell -File C:\Users\Mike\rsmod\start-worlds.ps1`
- W2 poort: `43595`
- W2 target: `RSMod PvP (World 2)`
- Gear: `::maxgear`
- PvP option fix: `::pvpops`
- Hub ready: `::pkready`
- Shops: `::pkshop`, `::pkmelee`, `::pkranged`, `::pkmagic`, `::pksupplies`, `::pkfood`, `::pkpots`
- Progress: `::pkpoints`, `::pkspend`

