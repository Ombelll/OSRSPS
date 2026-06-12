# ROADMAP v4 — Mike's OSRS Private Server (uitvoeringsgids)

Doel: **elk open punt is uitvoerbaar zonder eigen onderzoek** — exacte paden, kopieerbare code,
build-stappen en verificatie. Paden relatief aan `C:\Users\Mike\rsmod`. ✅ klaar · ◐ deels · ☐ te doen.

> **Huidige staat (v4, na de grote content-batch):** 2 werelden (W1 GE-hub/PvM 43594, W2
> PvP/Edgeville 43595, eigen DB's), **172 scripts**. Nieuw sinds v3: NPC-aggressie via custom
> hunt-mode, Fight Caves-gauntlet op de echte map (area-lifecycle), persistente PK-punten
> (varps), NPC-Grand Exchange (orderboek), quest-log/collection/hiscore-UI's (questjournal-
> interface), boss-fases, daily events, PK-cosmetics, arena best-wave, **watchdog + backups +
> log-rotatie** (start-worlds.ps1 / backup-saves.ps1 / rotate-logs.ps1, logs in `.data/logs/`),
> player following (engine), Gradle-geheugen 4G. Alles gecommit (zie `git log`).

---

## A. OPERATIONEEL — bouwen, draaien, herstarten

### Wereld-matrix
| | World 1 | World 2 (PvP) |
|---|---|---|
| Poort | 43594 | 43595 |
| DB | `.data/saves/game.db` | `.data/saves/game_w2.db` |
| Spawn=respawn | GE `0_49_54_28_30` (3164,3486) | Edgeville `0_48_54_15_40` (3087,3496) |
| RSProx-target | "RSMod" | "RSMod PvP (World 2)" (`C:\Users\Mike\.rsprox\proxy-targets.yaml`) |

### Draaien (voorkeursroute: de ops-scripts)
```powershell
# Watchdog: start ontbrekende werelden, roteert logs, herstart bij crash (60s-loop):
powershell -File C:\Users\Mike\rsmod\start-worlds.ps1
# Backups van beide DB's:
powershell -File C:\Users\Mike\rsmod\backup-saves.ps1
```
Server-logs: `.data/logs/world1.out.log` / `world2.out.log` (rotatie >10MB → `.data/logs/archive/`,
14 dagen bewaard). Succes-check: `Loaded NNN scripts` + `Bound to ports`, geen `Exception in thread "main"`.

### Bouwen (Git Bash, repo-root)
```bash
./gradlew :server:app:installDist --no-daemon   # script-only wijziging (~1 min)
./gradlew packCache --no-daemon                 # EERST bij nieuwe/gewijzigde TYPES
                                                # (varp/inv/NpcEditor/hunt/area), dan installDist
```
Handmatig starten kan ook: `./server/app/build/install/app/bin/app` (W1) en met
`RSMOD_WORLD=2 RSMOD_PORT=43595 RSMOD_DB=.data/saves/game_w2.db` ervoor (W2).
Stoppen: `Get-NetTCPConnection -LocalPort 4359X -State Listen | % { Stop-Process -Id $_.OwningProcess -Force }`
— maar denk aan de watchdog: die herstart binnen 60s (watchdog zelf stoppen om écht te stoppen).

### IJzeren regels
1. **packCache → ALTIJD beide werelden herstarten** (gedeelde cache; oude server + nieuwe cache
   = client "Unexpected server response" door CRC-mismatch).
2. Saven gebeurt **alleen bij logout** (geen autosave). Force-kill = ingelogde spelers verliezen
   alles sinds login; login zelf blijft werken (devMode-guard).
3. Geen bronbestanden bewerken tijdens een lopende build; configuration-cache staat bewust UIT
   (`gradle.properties`: content-modules worden dynamisch ontdekt).
4. Eén RSProx-client per wereld; wisselen via target-dropdown in de RSProx-launcher.
5. Render-glitch (Intel Iris Xe): loginscherm → "Force disable new renderer?" aan.

### Engine-patches (bij upstream-update opnieuw aanbrengen)
1. `api/net/.../player/AccountLoadResponseHook.kt` — auto display-name bij lege naam (vóór de
   `!newAccount`-return) + partial-save-guard alleen buiten devMode.
2. Multi-world env-overrides: `api/net/.../NetworkFactory.kt` (`RSMOD_PORT`),
   `api/db/.../DatabaseConfig.kt` (`RSMOD_DB`), `api/server-config/.../ServerConfigLoader.kt`
   (`RSMOD_WORLD`/`RSMOD_REALM`).
3. `api/shops/.../cost/StandardGpCostCalculations.kt` — buy-floor `cap = 0.0, minCost = 0`
   (shops met buyPercentage 0.0 zijn écht gratis).
4. `api/combat/combat-scripts/.../PvPCombatScript.kt` — `pvpWorld`-gate in `canAttack`
   (PvP alleen op W2).
5. `api/death/.../PlayerDeath.kt` + `PvpKillTracker.kt` — respawn = realm-respawnCoord;
   PK-kill-detectie → punten/streaks (dep `api/realm` in `api/death/build.gradle.kts`).
6. Engine-extra's uit de batch: player following (`PlayerInteractionProcessor` e.o.),
   NPC line-of-sight-fixes (src/dest-dimensies). Gecommit; alleen relevant bij rebase op upstream.

### Sym-inventaris (bezette custom ids → eerstvolgende vrij)
| sym | bezet | volgende vrij |
|---|---|---|
| `varp.sym` | 9000-9003 quests/diary · 9004 pk_points · 9005 pk_kills · 9006 bossslayer · 9007 arena_best_wave | **9008** |
| `inv.sym` | 2000-2007 (4 hub-shops + 4 PK-shops) | **2008** |
| `hunt.sym` | 23 `mike_boss_aggro` | **24** |
| `area.sym` | 32763 `fight_cave_arena` | 32762 (omlaag tellen) |

### Database-spiekbrief (SQLite; lezen met `mode=ro`, schrijven alleen met gestopte server)
```sql
UPDATE realms SET spawn_coord='0_49_54_28_30', respawn_coord='0_49_54_28_30';
-- accounts: login_username, display_name, modlevel · characters: x,z,varps(JSON),last_logout
-- stats: character_id, stat_id, base_level, fine_xp (xp*10)
```
Hiscores: `SELECT a.display_name, s.stat_id, s.base_level, s.fine_xp/10 xp FROM stats s
JOIN characters c ON c.id=s.character_id JOIN accounts a ON a.id=c.account_id ORDER BY xp DESC;`

### Commando-index (voor tests; `::help` in-game toont de spelerslijst)
Hub/reis: `::hub ::teleport` + Teleport Wizard-NPC · Gear/test: `::maxgear ::maxxp ::setname
::skillkit ::skillmats ::skillzone` · Winkels: `::pkshop ::pkmelee/ranged/magic/supplies
::potionshop ::store ::mikeshop ::supplyshop ::npcge` · PvM: `::mikeboss …::godboss ::bosses
::bossslayer` · Minigames: `::arena ::arenajoin ::arenaquit ::arenatop ::fightcaves ::fightjoin
::fightquit ::dice
::flip ::slots ::mystery` · Progressie: `::questlog ::quests ::diary ::achievements
::collection ::pkpoints ::pkspend ::daily ::event`.

---

## B. RECEPTENBOEK — bewezen patronen (kopieer letterlijk)

Content in `content/custom/mikeshop/src/main/kotlin/org/rsmod/content/custom/mikeshop/`
(auto-ontdekt; refs/builders = **niet-private Kotlin `object`s**).

1. **Commando**: `onCommand("x") { desc = "..."; cheat { ... } }`; args via `args.getOrNull(0)`;
   dialogen/telejump → injecteer `ProtectedAccessLauncher`, `protectedAccess.launch(player) { ... }`.
2. **Dialogen**: `choice2..5(label, waarde, ..., title=)`, `mesbox`, `chatNpc(mesanims.x, ...)`,
   `countDialog("How many?")`, `objDialog("Choose an item.", stockMarketRestriction = true)`
   (ingebouwde GE-item-zoeker — bewezen in `NpcGrandExchange.kt`).
3. **Persistente varp**: `9008<TAB>naam` in `.data/symbols/varp.sym` → `VarpBuilder.build("naam")`
   (kaal = Perm) → `VarpReferences.find("naam")` (géén hash) → `vars[ref] = x` (ProtectedAccess)
   of `private var Player.x: Int by intVarp(ref)`. **packCache!** Zonder packCache worden writes
   bij logout stil gedropt. Live voorbeeld: `PkPoints.kt` (varps 9004/9005).
4. **Shop-inv**: `2008<TAB>naam` in inv.sym → `InvBuilder.build("naam") { scope=Shared;
   stack=Always; autoSize=true; restock=true; stock += stock(obj,count,cycles) }` →
   `shops.open(player, titel, ref, buy%, sell%, change%)` (0.0 = gratis). **packCache!**
5. **NpcEditor**: `edit(ref) { name=...; hitpoints=...; contentGroup=content.banker;
   huntMode=huntmodes.mike_boss_aggro; huntRange=8 }`. **packCache!** `contentGroup =
   content.banker` = werkende bank-NPC.
6. **Aggressie (bewezen!)**: hunt-mode `huntmodes.mike_boss_aggro` bestaat al (elke combat-level,
   re-aggro, melee/OpPlayer2) — hergebruik 'm via NpcEditor (zie 5). Eigen variant: kopieer de
   builder in `api/config/builders/HuntBuilds.kt` + regel in `hunt.sym` (id 24+). Valkuilen:
   `huntRange` NIET exact 5 (geldt als unset); ranged-aggro = `findNewMode = NpcMode.ApPlayer2`
   + `attackRange` in de editor; `npc.setHunt()/setHuntMode()` NIET als primair mechanisme
   (wordt door `resetHunt()` teruggezet).
7. **NPC/loc spawnen**: `Npc(npcTypes[ref], coords); npc.mode = NpcMode.None; npcRepo.add(npc,dur)`
   in try/catch, relatief aan bekende vrije tegel. Loc: `LocInfo(LocLayerConstants.of(shape),
   coords, LocEntity(type.id, shape, angle))` → `locRepo.add`.
8. **Death-hook**: `onNpcQueue(ref, queues.death) { findHero(players); death.deathNoDrops(this); ... }`
   — **single-handler per type+queue; duplicate = startup-crash.** Claim-check verplicht:
   `grep -rn "\"<npcnaam>\"" content/ | grep -v mikeshop` + bekende claims: CowDrops (cow/
   chicken/goblin/giant), BulkMonsters (~44 types, o.a. hellhound), Slayer (`slayer_*`),
   mikeshop (6 bosses + skeleton_unarmed/zombie_unarmed/black_knight/lesser_demon + FightCaves-types).
9. **Area-lifecycle (bewezen in FightCaves)**: area-id in `area.sym` + `AreaBuilds.kt` +
   `BaseAreas.kt` + `MapAreaBuilder` met `mapSquare(MapSquareKey(x,z))` → packCache →
   `onArea(ref) { start }` / `onAreaExit(ref) { cleanup }` — exit vuurt óók bij dood/teleport/
   logout = één cleanup-punt. NIET `onPlayerQueue(queues.death)` binden (al geclaimd).
10. **Tekst-UI (bewezen 3×: QuestEngine/CollectionLog/ArenaHiscores)**: cache-interface
    `questjournal` (119): refs zonder hash, alle `ifSetText` VÓÓR `ifOpenMainModal`, daarna
    `ifSetEvents(close, -1..-1, IfEvent.Op1)` + `onIfModalButton(close) { ifClose() }`.
    Alternatieven: `journalscroll` (741), `longscroll` (625, één component + `<br>`).
11. **World-gating**: `private val pvpWorld = System.getenv("RSMOD_WORLD") == "2"`.
12. **Naam-validatie**: `awk -v n="naam" '$2==n{f=1} END{exit !f}' .data/symbols/obj.sym && echo OK`.
    Runes = `airrune`-stijl, tassets = `bandos_skirt`, barrows = `barrows_dharok_weapon`,
    potions = `4dose2combat`-stijl.

---

## C. FASES

### FASE 0 — Fundament & beheer ✅
Multi-world, login-robuustheid, RSProx-targets, **watchdog (start-worlds.ps1) + backups
(backup-saves.ps1) + log-rotatie (rotate-logs.ps1)** — afgerond.

### FASE 1 — Werelden & identiteit ◐
- ✅ W1 veilig (PvP-gate) · PK-punten/streaks **persistent** (varps 9004/9005) · PK-cosmetics ·
  gratis PK-shops als NPC's · world-gating
- ✅ **Edgeville-polish**: PK-hub spawnt shop-NPC's, banker, bank booth en noticeboard. De spawned
  bank booth opent bank op op2; het noticeboard toont PK-commands, skull-prevention en wildy-range
  uitleg.

### FASE 2 — Combat-diepte ◐
- ✅ PvN+PvP · protect prayers (werkten al) · **aggressie live** (`mike_boss_aggro` op de bosses,
  huntRange 8) · **boss-fases** (`BossMechanics.kt`)
- ✅ **Dragon dagger special attack**: refs + anim/spotanim + registratie voor normale, p, p+ en p++
  dagger-varianten; `DragonDaggerSpecialAttack.kt` doet 2x stab-hit met 1.15 accuracy/max-hit en
  gebruikt de standaard 25% special-energy route.
- ✅ **Wilderness-regels v1**: skull-on-attack + skull-prevention + wildy-level-ranges gebouwd voor
  W2. W2 blijft buiten wilderness bruikbaar als open PvP-testwereld; binnen wilderness moeten beide
  spelers in wildy staan en binnen `min(wildy-levels)` combat difference vallen. Bij een geldige
  player-kill houdt het slachtoffer de top 3 waardevolle item-stacks; overige inventory+worn items
  droppen owner-locked voor de killer. Open voor later: protect-item/prayer-modifier en speciale
  untradeable-regels.

### FASE 3 — Minigames & endgame ◐
- ✅ Arena (waves, best-wave persistent varp 9007, `::arenatop`-hiscores) · **Fight Caves** op de
  echte map (area 32763 + `mike_boss_aggro`; `::fightcaves`/`::fightquit`) · custom **collection
  log** (`::collection`) · **daily events** (`::daily`/`::event`) · dice/flip/slots/mystery ·
  progressie/diary/achievements
- ✅ **Multi-speler-waves v1**: Arena gebruikt party-runs met `::arenajoin`, deelnemerslijst,
  wave-scaling en per-speler owner-locked coin drops/records. Fight Caves laat spelers betaald
  bij dezelfde run joinen, schaalt waves op deelnemersaantal en deelt coins/fire cape per deelnemer.
  Open voor later: echte per-party instances zodat meerdere groepen tegelijk Fight Caves kunnen doen.
- ✅ **Instancing-upgrade Fight Caves v1**: `::fightcaves` maakt per party een eigen
  `RegionTemplate`/`RegionRepository` instance van de TzHaar-map; `::fightjoin [naam]` laat extra
  spelers betaald in dezelfde instance joinen. NPC-spawns en drops worden per `Run`/`npcRuns`
  gevolgd, waardoor meerdere Fight Caves groepen tegelijk kunnen draaien.

### FASE 4 — Quests & UI ✅ (basis)
Quest-engine + **echte quest-log-UI** (questjournal 119) + questreeks 2.0 met boss-kill-stages
(`::bossslayer`, varp 9006). Open: meer questlijnen schrijven (puur content, recepten B2/B3/B10).

### FASE 5 — Economie ◐
- ✅ 8 winkels · PK-puntenwinkel/cosmetics · geld-sinks (instance-fees, cosmetica) ·
  **NPC-Grand Exchange** (`::npcge`: orderboek met escrow, prijs-matching, collect — in-memory)
- ✅ **NPC-GE v2 (persistentie)**: Flyway-migratie `api/db/src/main/resources/db/migration/
  V7__ge_orders.sql` + lazy load/save van `NpcExchangeBook`. Open buy/sell-orders, collect-coins
  en collect-items blijven nu in de world-DB staan na restart.
- ☐ **Echte speler-trade (Plan A — ná de 2-client-test).** Server-kant bestaat: Trade =
  `onOpPlayer4` (`api/script-advanced`; labels in `content/other/login/LoginScript.kt:136`),
  offer-inv `invs.tradeoffer` (90), interfaces `trademain`(335)/`tradeside`(336)/`tradeconfirm`(334),
  atomaire swap via `invTransaction` + `select(beide offers/invs)` + 2× `moveAll` (niets commit
  bij falen). UI-template: `content/interfaces/equipment/.../GuidePriceScript.kt`. Client-
  onbekenden: trademain-cs2-init + partner-offer spiegelen (`UpdateInvFull(-(combined), 32858, ...)`).
  Anti-switch-scam: accept-flags resetten bij élke offer-mutatie. `tradeoffer` is Perm →
  login-restore toevoegen.

### FASE 6 — Multiplayer, verificatie & sociaal ☐ ← **HIER STAAN WE**
- ☐ **Live verificatie van de nieuwe batch** (gebouwd + gecommit + servers draaien, maar nog
  niet in-game getest): per feature de verificatiestap draaien —
  `::mikeboss` → boss valt ZELF aan binnen 1-2 ticks (aggressie); `::fightcaves` → waves multiway-
  aggressief, dood/wegloop = nette cleanup, fee geïnd; `::npcge` → buy/sell/collect-cyclus met 2
  accounts; `::collection ::arenatop ::questlog` → UI's openen + close-knop werkt; `::daily` →
  event activeert; boss-fases zichtbaar bij HP-drempels; `::pkspend` cosmetics.
- ☐ **Structurele 2-client-test** (W2): PvP-duel → `::pkpoints` klopt bij beide; samen arena;
  uitloggen/herstarten → punten persistent.
- ✅ **Hiscores-webpagina**: `hiscores-web.ps1` start een lokale read-only pagina op
  `http://127.0.0.1:8088`; leest W1 `game.db` en W2 `game_w2.db`, met world/skill/search/top-filter.
  Data blijft logout-vers.
- ☐ PM/friends/clanchat — engine-onderzoek nodig (nog niet gedaan).

### FASE 7 — Polish & beheer ◐
- ✅ Watchdog/backups/log-rotatie
- ☐ Performance-test 3+ clients · ☐ **Niet publiek hosten** (Jagex-IP; lokaal leer-/testproject).

---

## D. AANBEVOLEN VOLGORDE
1. **Live verificatie nieuwe batch** (Fase 6) — alles is gebouwd maar ongetest in-game; vind de
   bugs vóór er verder gestapeld wordt. Checklist staat hierboven.
2. **2-client-test** (Fase 6) — ontgrendelt trade Plan A en multi-speler-waves.
3. **Trade Plan A** (Fase 5) — medium; client-onbekenden eerst met 2 clients verifiëren.
4. **Wilderness item-on-death/keep-3** — eerst death-inv-drop API afronden.

## E. CHECKLIST VOOR ELKE WIJZIGING (dwingend)
1. Item/npc/loc-namen exact gevalideerd in de .sym (B12)?
2. Death-hook-claimcheck gedaan (B8)?
3. Nieuwe types → packCache vóór installDist → BEIDE werelden herstart (watchdog doet dit
   binnen 60s zelf als je de processen stopt)?
4. Geen dubbele claim op `onPlayerQueue(queues.death)` / engine-queues?
5. NPC-spawns try/catch + bekende vrije tegel; world-gating nodig (B11)?
6. Log toont `Loaded NNN scripts` (+1 per nieuw script) + `Bound to ports`?
7. In-game verificatiestap uitgevoerd én beschreven in de commit?
