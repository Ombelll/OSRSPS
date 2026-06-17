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
- De Edgeville hub heeft een occult altar om Standard, Ancients, Lunar en Arceuus te kiezen.
- Spellbook commands bestaan: `::standard`, `::ancients`, `::lunar`, `::arceuus`.
- PK-staple special attacks zijn geregistreerd: AGS, dragon claws, granite maul, dragon warhammer,
  fang, abyssal dagger, magic shortbow en toxic blowpipe.
- `::maxgear` unlockt gear, combat stats, prayers, ancient magicks, quest points en quest gates.
- PK-punten/streaks zijn persistent via varps.
- Wilderness ditch werkt met jump-anim en springt vanaf huidige X over de ditch.
- Kill loot v2: untradeables blijven altijd bij het slachtoffer; van de tradeables houdt het
  slachtoffer de top 3 (top 4 met Protect Item-prayer), rest dropt owner-locked voor de killer.
- Oneindige special attack (energie blijft 100%) - server-breed.
- Food/pots werken voor alle PK-supplies (shark/anglerfish/karambwan/monkfish/dark crab +
  super combat/divine/bastion/prayer/super restore/Saradomin brew met dosis-afname).
- Teleport-spells werkend op alle 4 spellbooks (21 teleports, alleen geladen regio's gewired,
  zone-gevalideerd). Home Teleport gaat op elke book naar Edgeville.
- PK-loadouts: `::brid`, `::veng`, `::pure`, `::tank` + `::restock` (Fase 2).
- Tele-block: `::teleblock`/`::tb <naam>` blokkeert teleport-spells van het doelwit (Fase 3).
- Autosave: elke ~60s wordt elke online speler volledig opgeslagen (geen verlies bij herstart/crash).
- Anti-farm + best-streak + milestone-bonus in PK-punten (Fase 4).

## Fase 1 - Directe PvP Betrouwbaarheid

Doel: twee clients kunnen zonder gedoe op W2 vechten.

- Check `Attack` zichtbaar na normale login op W2.
- Check `::pvpops` herstelt `Attack` zonder relog.
- Check `::pkready` teleporteert naar Edgeville en zet `Attack`.
- Check altar in Edgeville alle vier spellbooks activeert.
- Check PvP buiten wilderness blijft open test-PvP op W2.
- Check binnen wilderness: beide spelers moeten in wilderness staan en binnen combat-range vallen.
- Check skull prevention: met skull prevention aan moet onveilige attack geblokkeerd worden.
- Check dat W1 veilig blijft: geen `Attack`, en PvP-aanval geeft veilige melding.

Acceptatie:
- Twee clients kunnen binnen 2 minuten na login gear pakken en elkaar aanvallen.
- Geen server error in `world2.err.log`.
- Na kill stijgt killer `::pkpoints`.

## Fase 2 - PK Gear Flow  [GEIMPLEMENTEERD - live 2-client test nog te doen]

Doel: minder voorbereiding, meer vechten.

- [x] `::brid` loadout voor hybride NH/tribrid (mage worn + range/melee switches + supplies).
- [x] `::veng` loadout voor melee/range veng PK (tip: ga op Lunar via `::lunar`).
- [x] `::pure` loadout voor lage defence tests (vereenvoudigd; dedicated pure-gear volgt).
- [x] `::tank` loadout voor tank/anti-PK tests (Dharok + DFS + brews).
- [x] `::restock`: food, karambwans, brews, restores, runes, arrows.
- [x] Loadouts respecteren vrije slots (invAdd strict=false; worn gear wordt vervangen).
- [ ] Special attack button + energy drain checken (NB: spec-energie is nu oneindig op deze server).

Acceptatie:
- [x] Elke loadout geeft bruikbare worn gear plus switches/supplies (geimplementeerd).
- [ ] `::restock` over meerdere fights live verifieren (2-client test).

## Fase 3 - Wilderness Rules v2  [KLAAR]

Doel: wilderness voelt herkenbaar en minder exploitable.

- [x] Protect item in item-retention (Protect Item-prayer aan -> 1 extra item behouden, top-4).
- [x] Untradeables apart: blijven ALTIJD bij het slachtoffer (worden nooit gedropt/verloren).
- [x] PK-item-waarde: retentie sorteert op playerCost/playerCostDerived/cost (waardevolste eerst).
- [x] Teleblock: `::teleblock <naam>` / `::tb <naam>` blokkeert de teleport-SPELLS van het doelwit
      5 min (zie WildernessRules.kt / TeleBlockState + check in MagicSpells.kt). Utility-commands
      (::edge/::ge/::pkready) blijven bewust werken voor testgemak.
- [x] PJ-timer / singles-plus: al door de engine afgehandeld in `PvPCombatScript.canAttack` via
      `pkPredator1`-tracking + single-combat-checks ("X is fighting another player" / "I'm already
      under attack"). Geldt alleen in singles (`mapMultiway()` bepaalt single vs multi).
- [x] Multiway zones: gedetecteerd via `mapMultiway()` (engine, uit de map-data). In multi gelden de
      singles-PJ-checks niet, in singles wel. Edgeville-hub = singles; diepe wild = multi waar de
      map dat aangeeft.
- [x] Ditch testcases gedocumenteerd (zie hieronder; engine springt vanaf huidige X over de ditch).

Acceptatie:
- [x] Loot na death is voorspelbaar (untradeables veilig, vaste top-3/4 op waarde).
- [x] Geen item-loss: untradeables nooit kwijt; bij volle inventory droppen kept-items owner-locked
      voor de speler zelf (niet voor de killer). Logout mid-death is atomair (protected access).

### Ditch-testcases (handmatig na te lopen)
1. Noord-overgang: sta net ten zuiden van de ditch, klik over -> jump-anim, land 1 tegel noord, in wild.
2. Zuid-overgang: sta in wild net ten noorden, klik zuid over de ditch -> land net buiten wild.
3. Lange loc/afstand: klik de ditch van enkele tegels afstand -> loopt eerst naar de ditch, dan jump.
4. Spam-click: meerdere keren snel klikken -> geen dubbele jump / geen vastlopen.

## Fase 4 - Rewards En Economy  [GROTENDEELS KLAAR - web-hiscores TODO]

Doel: kills geven motivatie zonder economie kapot te maken.

- [x] `::pkpoints` duidelijker: kills, punten, huidige streak + beste streak (deze sessie).
- [x] PK shop met cosmetics/supplies/risk-items (`::pkspend`: coins, super combat, dragon claws,
      santa/robin/halloween/black-phat cosmetics).
- [x] Streak rewards met zachte caps: basis 4 + (streak-1) punten (cap 10/kill) + milestone-bonus
      (+streak elke 5e kill); broadcast bij streak 5/10/15...
- [x] Anti-farm: dezelfde tegenstander snel herhaald killen geeft diminishing returns (vol -> half
      -> 1 punt); de teller dooft na 2 min zonder kill op dat doelwit. Melding "(verlaagd - anti-farm)".
- [ ] Web-hiscores W2-tab (kills/deaths/KD/streak/points) - vereist web-werk, apart traject.

Acceptatie:
- [x] Punten voelen nuttig maar gratis shops blijven bruikbaar voor testen.
- [x] 2-account farmen levert beperkt voordeel op (diminishing returns).

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
- Loadouts (Fase 2): `::brid`, `::veng`, `::pure`, `::tank`, `::restock`
- Spellbooks: `::standard`, `::ancients`, `::lunar`, `::arceuus`
- Teleports: alle spellbook-teleports werken (klik de spell); Home Teleport -> Edgeville
- PvP option fix: `::pvpops`
- Hub ready: `::pkready`
- Shops: `::pkshop`, `::pkmelee`, `::pkranged`, `::pkmagic`, `::pksupplies`, `::pkfood`, `::pkpots`
- Progress: `::pkpoints`, `::pkspend`
- Tele-block: `::teleblock <naam>` of `::tb <naam>`
