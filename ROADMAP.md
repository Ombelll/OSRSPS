# ROADMAP v3 — Mike's OSRS Private Server (uitvoeringsgids)

Doel van dit document: **elk open punt is uitvoerbaar zonder eigen onderzoek.** Exacte
bestandspaden, kopieerbare code, build-stappen en verificatie staan erbij. Paden zijn relatief
aan `C:\Users\Mike\rsmod`. Legenda: ✅ klaar · ◐ deels · ☐ te doen.

> **Huidige staat:** 2 werelden naast elkaar (W1 = GE-hub/PvM op 43594, W2 = PvP/Edgeville op
> 43595, elk eigen SQLite-DB). 23 skills, 6-boss gauntlet, 6 quests (persistente varps),
> Combat Arena (10 waves), 4 gratis PK-shops als NPC's in Edgeville, PK-punten + killstreaks,
> PvP alleen op W2, werkende bank op beide werelden, GE-hub met Shop Clerk / Banker / Teleport
> Wizard / skilling-zone. ~165 scripts.

---

## A. OPERATIONEEL — bouwen, draaien, herstarten

### Wereld-matrix
| | World 1 | World 2 (PvP) |
|---|---|---|
| Poort | 43594 | 43595 |
| DB | `.data/saves/game.db` | `.data/saves/game_w2.db` |
| Spawn=respawn | GE `0_49_54_28_30` (3164,3486) | Edgeville `0_48_54_15_40` (3087,3496) |
| RSProx-target | "RSMod" | "RSMod PvP (World 2)" (in `C:\Users\Mike\.rsprox\proxy-targets.yaml`, id:2, `game_server_port: 43595`) |

### Commando's (Git Bash, cwd = repo-root)
```bash
# Script-only wijziging (geen nieuwe types):
./gradlew :server:app:installDist --no-daemon          # ~1 min

# Nieuwe/gewijzigde TYPES (varp/inv/NpcEditor/hunt/area/obj-param):
./gradlew packCache --no-daemon                        # ~2-3 min, EERST
./gradlew :server:app:installDist --no-daemon          # DAARNA

# Starten (elk in eigen achtergrond-shell):
./server/app/build/install/app/bin/app                                                # W1
RSMOD_WORLD=2 RSMOD_PORT=43595 RSMOD_DB=.data/saves/game_w2.db \
  ./server/app/build/install/app/bin/app                                              # W2
```
Stoppen (PowerShell): `Get-NetTCPConnection -LocalPort 43594 -State Listen | % { Stop-Process -Id $_.OwningProcess -Force }` (idem 43595).
Succes-check in log: `Loaded NNN scripts` + `Bound to ports: 4359x`, geen `Exception in thread "main"`.

### IJzeren regels
1. **packCache → ALTIJD beide werelden herstarten** (zelfde cache; een draaiende server op een
   oude cache geeft client-side "Unexpected server response" door CRC-mismatch).
2. Force-kill is veilig voor login (devMode-guard, zie patch 1) maar **ingelogde spelers
   verliezen alles sinds hun login** — saven gebeurt ALLEEN bij logout
   (`api/account/.../saver/AccountSavingService.kt`; geen autosave, geen emergency backup).
3. Bewerk geen bronbestanden tijdens een lopende Gradle-build.
4. Eén RSProx-client per wereld; wisselen via de target-dropdown in de RSProx-launcher
   (in-client world switcher werkt niet door het proxy-ontwerp).
5. Render-glitch op Intel Iris Xe: op het loginscherm "Force disable new renderer?" aanzetten.

### Engine-patches (bij rsmod-upstream-update opnieuw aanbrengen!)
1. `api/net/src/main/kotlin/org/rsmod/api/net/rsprot/player/AccountLoadResponseHook.kt`
   — (a) in `applyConfigTransforms`: auto display-name voor élk account met lege naam
   (`if (config.autoAssignDisplayNames && displayName.isBlank()) displayName = username.toDisplayName()`
   VÓÓR de `if (!newAccount) return`); (b) partial-save-guard: `if (isPartialSave && !config.devMode)`.
2. Multi-world env-overrides: `api/net/.../NetworkFactory.kt` (`ports = listOf(System.getenv("RSMOD_PORT")?.toIntOrNull() ?: 43594)`),
   `api/db/.../DatabaseConfig.kt` (`System.getenv("RSMOD_DB") ?: ".data/saves/game.db"`),
   `api/server-config/.../ServerConfigLoader.kt` (`RSMOD_WORLD`/`RSMOD_REALM` override op `load()`).
3. `api/shops/src/main/kotlin/org/rsmod/api/shops/cost/StandardGpCostCalculations.kt`
   — in `calculateShopBuy*` (regel ~63): `cap = 0.0, minCost = 0` (was 0.3 / 1). Hierdoor zijn
   shops met `buyPercentage = 0.0` écht gratis; normale shops onveranderd.
4. `api/combat/combat-scripts/.../scripts/PvPCombatScript.kt` — veld
   `private val pvpWorld = System.getenv("RSMOD_WORLD") == "2"` + gate bovenin `canAttack`.
5. `api/death/src/main/kotlin/org/rsmod/api/death/PlayerDeath.kt` — respawn =
   `realm.config.respawnCoord` (was hardcoded Lumbridge); PK-kill-detectie via `findHero()` →
   `PvpKillTracker` (zelfde module) op de PvP-wereld. Dep toegevoegd: `api/realm` in
   `api/death/build.gradle.kts`.

### Database-spiekbrief (SQLite; server stoppen vóór schrijven, of `mode=ro` voor lezen)
```sql
-- realms: spawn aanpassen (formaat level_mapX_mapZ_localX_localZ)
UPDATE realms SET spawn_coord='0_49_54_28_30', respawn_coord='0_49_54_28_30';
-- accounts: id, login_username, display_name, password_hash, modlevel
-- characters: id, account_id, x, z, level, varps (JSON {"varpId": int}), last_logout
-- stats: character_id, stat_id, vis_level, base_level, fine_xp (xp*10)
```
Hiscores-query (read-only): `SELECT a.display_name, s.stat_id, s.base_level, s.fine_xp/10 AS xp FROM stats s JOIN characters c ON c.id=s.character_id JOIN accounts a ON a.id=c.account_id ORDER BY xp DESC;`

---

## B. RECEPTENBOEK — bewezen patronen (kopieer deze letterlijk)

Alle content staat in `content/custom/mikeshop/src/main/kotlin/org/rsmod/content/custom/mikeshop/`
(module wordt auto-ontdekt; refs/builders moeten **niet-private Kotlin `object`s** zijn).

1. **Commando**: `onCommand("naam") { desc = "..."; cheat { player.mes("...") } }`. Args:
   `args.getOrNull(0)`. ProtectedAccess nodig (dialogen/telejump)? Injecteer
   `ProtectedAccessLauncher` → `protectedAccess.launch(player) { ... }` (returnt `false` als bezig).
2. **Dialoog-menu**: `choice5("Label A", 1, "Label B", 2, ..., title = "...")` (ook choice2/3/4);
   `mesbox("...")`, `chatNpc(mesanims.x, "...")`, `countDialog("How many?")`,
   `objDialog("Choose an item.", stockMarketRestriction = true)` (= GE-item-zoeker!).
3. **Custom varp (persistent)**: regel `9004<TAB>naam` in `.data/symbols/varp.sym` (9000-9003
   bezet; TAB-gescheiden!) → `object XBuilder : VarpBuilder() { init { build("naam") } }` (kale
   build = Perm = gesaved) → `object XVarps : VarpReferences() { val x = find("naam") }` (géén
   hash) → lezen/schrijven: in ProtectedAccess `vars[XVarps.x] = ...`; op een rauwe Player:
   `private var Player.teller: Int by intVarp(XVarps.x)` (import `org.rsmod.api.player.vars.intVarp`).
   **packCache vereist**; zonder packCache worden writes bij logout STIL gedropt.
   Verifiëren: `SELECT varps FROM characters` bevat `"9004": waarde`.
4. **Custom shop-inv**: regel `2008<TAB>naam` in `.data/symbols/inv.sym` (2000-2007 bezet) →
   `InvBuilder`: `build("naam") { scope = InvScope.Shared; stack = InvStackType.Always;
   autoSize = true; restock = true; stock += stock(obj, count, restockCycles) }` →
   `shops.open(player, "Titel", invRef, buyPercentage, sellPercentage, changePercentage)`
   (inject `Shops`; 0.0 = gratis). **packCache vereist.**
5. **NpcEditor** (naam/stats/groep/hunt): `object X : NpcEditor() { init { edit(ref) {
   name = "..."; hitpoints = 600; contentGroup = content.banker; huntMode = ...; huntRange = 8 } } }`.
   **packCache vereist.** `contentGroup = content.banker` geeft een NPC werkende bank-opties.
6. **NPC spawnen**: `val npc = Npc(npcTypes[ref], coords); npc.mode = NpcMode.None;
   npcRepo.add(npc, duration)` — altijd in try/catch (geblokkeerde tegel), spawn relatief aan
   een bekende vrije tegel (`HUB.translate(dx,dz)`).
7. **Loc spawnen**: `LocInfo(LocLayerConstants.of(shape), coords, LocEntity(type.id, shape, angle))`
   → `locRepo.add(loc, duration)` met `shape = LocShape.CentrepieceStraight.id`, `angle = LocAngle.West.id`.
8. **Death-hook (kill-detectie)**: `onNpcQueue(npcRef, queues.death) { val hero =
   findHero(players); death.deathNoDrops(this); ... }`. **Single-handler per npc-type+queue —
   duplicate = crash bij startup** ("Event with id already registered"). CHECK vóór gebruik:
   - `content/custom/cowdrops/CowDrops.kt` claimt: cow, chicken, goblin, giant (+lijst in file)
   - `content/custom/cowdrops/BulkMonsters.kt` claimt ~44 types (o.a. **hellhound**!) — grep de file
   - `content/skills/slayer/Slayer.kt` claimt alleen `slayer_*`-types
   - mikeshop zelf: 6 boss-types + skeleton_unarmed/zombie_unarmed/black_knight/lesser_demon (arena)
   Vrij-check: `grep -rn "\"<npcnaam>\"" content/ | grep -v mikeshop`.
9. **World-gating** (content alleen op één wereld): veld
   `private val pvpWorld = System.getenv("RSMOD_WORLD") == "2"` in de PluginScript.
10. **Naam-validatie items/npcs**: exact matchen in `.data/symbols/obj.sym` / `npc.sym`:
    `awk -v n="naam" '$2==n{f=1} END{exit !f}' .data/symbols/obj.sym && echo OK`. Let op:
    runes heten `airrune`/`deathrune` (geen underscore), tassets = `bandos_skirt`,
    barrows = `barrows_dharok_weapon` etc., potions = `4dose2combat`-stijl.

---

## C. FASES

### FASE 0 — Fundament ✅ (rest: beheer-gemak)
- ✅ Multi-world, login-robuustheid, save-persistentie, RSProx-targets
- ✅ **Launch-script + watchdog.** `C:\Users\Mike\rsmod\start-worlds.ps1`:
  ```powershell
  # Start beide werelden als ze niet draaien; herhaal elke 60s (watchdog).
  while ($true) {
    foreach ($w in @(
      @{port=43594; env=@{}},
      @{port=43595; env=@{RSMOD_WORLD='2'; RSMOD_PORT='43595'; RSMOD_DB='.data/saves/game_w2.db'}}
    )) {
      if (-not (Get-NetTCPConnection -LocalPort $w.port -State Listen -ErrorAction SilentlyContinue)) {
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = "C:\Users\Mike\rsmod\server\app\build\install\app\bin\app.bat"
        $psi.WorkingDirectory = "C:\Users\Mike\rsmod"
        foreach ($k in $w.env.Keys) { $psi.EnvironmentVariables[$k] = $w.env[$k] }
        $psi.UseShellExecute = $false
        [System.Diagnostics.Process]::Start($psi) | Out-Null
        Write-Host "started world on $($w.port)"
      }
    }
    Start-Sleep -Seconds 60
  }
  ```
  Verificatie: W2-proces gekilled → `start-worlds.ps1 -Once` startte W2 terug op poort 43595.
- ✅ **Backups.** `C:\Users\Mike\rsmod\backup-saves.ps1`: kopieert `.data/saves/*.db` naar
  `.data/saves/backup/<datum>/` (alleen veilig als servers gestopt zijn óf via SQLite
  `.backup`-API; pragmatisch: kopieer bij server-stop in het launch-script).
  Status: script gebruikt `sqlite3 .backup` als `sqlite3` beschikbaar is; anders kopieert het
  `.db` + `-wal` + `-shm`. Test-backup gemaakt onder `.data/saves/backup/`.

### FASE 1 — Werelden & identiteit ◐
- ✅ W1 veilig (PvP-gate, patch 4) · ✅ PK-punten/killstreaks/broadcasts (patch 5 + `PkPoints.kt`)
  · ✅ respawn per wereld (patch 5) · ✅ PK-shops als NPC's + gratis · ✅ world-gating hub/PK-NPC's
- ✅ **PK-punten persistent maken.** Was in-memory (`api/death/PvpKillTracker.kt`, keyed op
  displayName). Migratie = recept B3: varps `9004 mike_pk_points`, `9005 mike_pk_kills` in
  varp.sym + VarpBuilder/References in mikeshop + in `PkPoints.kt` en `PlayerDeath.awardPvpKill`
  de tracker-waarden spiegelen naar `Player.intVarp`-delegates (streak mag in-memory blijven).
  packCache + beide werelden herstarten. Verificatie: punten verdienen → uitloggen → herstart →
  `::pkpoints` toont ze nog.
  - Status 2026-06-10: uitgevoerd via `.data/symbols/varp.sym` ids 9004/9005 +
    `BaseVarps`/`VarpBuilds`; `PlayerDeath` schrijft kills/punten naar player-varps,
    `PkPoints` leest en besteedt die varps. Streak blijft in-memory. `packCache` + `installDist`
    groen; W1/W2 herstart.
- ☐ **Edgeville-polish**: bank booth-loc bij de PK-NPC's (recept B7, loc `bank_booth` —
  naam verifiëren via B10), uitleg-bord, evt. lederboard (zie Fase 4 quest-log-UI-techniek).
  Status 2026-06-10: uitgevoerd in `PkShop.kt`: W2 spawnt naast de PK-NPC's nu ook een
  `bankbooth` en `noticeboard`, met dezelfde try/catch-spawnstijl als de NPC's. `compileKotlin`
  + `:server:app:installDist` groen; W1/W2 herstart en luisteren op 43594/43595.

### FASE 2 — Combat-diepte ◐
- ✅ Melee/Ranged/Magic PvN + PvP · ✅ **Protect prayers werken al** (onderzocht: 100% reductie
  vs NPC's, 40% vs spelers, via `api/player/.../hit/modifier/StandardPlayerHitModifier.kt`,
  toegepast in elke `queueHit`) — NIETS te doen, hooguit een regressietest.
- ✅ **AGGRESSIE (prioriteit 1, content-level!).** Het hunt-systeem bestaat al; vanilla types
  hebben alleen `huntMode = null`. Recept:
  1. Nieuw bestand `content/custom/mikeshop/MikeHunt.kt`:
     ```kotlin
     internal object MikeHunt : HuntModeReferences() { val boss_aggro = find("mike_boss_aggro") }
     internal object MikeHuntBuilder : HuntModeBuilder() {
         init {
             build("mike_boss_aggro") {
                 type = HuntType.Player
                 checkVis = HuntVis.LineOfSight          // HuntVis.Off = door muren
                 checkNotTooStrong = HuntCheckNotTooStrong.Off  // elke combat-level
                 checkNotCombat = varps.lastcombat
                 checkNotCombatSelf = varns.lastcombat
                 findKeepHunting = true                  // re-aggro na combat
                 findNewMode = NpcMode.OpPlayer2         // melee; ApPlayer2 = ranged/mage
             }
         }
     }
     ```
     (imports: `org.rsmod.api.type.builders.hunt.HuntModeBuilder`, `...refs.hunt.HuntModeReferences`,
     `org.rsmod.game.type.hunt.{HuntType,HuntVis,HuntCheckNotTooStrong}`,
     `org.rsmod.api.config.refs.{varps,varns}`, `org.rsmod.game.entity.npc.NpcMode`)
  2. In `MikeBossEditor.kt` per boss: `huntMode = MikeHunt.boss_aggro; huntRange = 8` —
     **NIET 5** (waarde 5 = "unset" door default-merge in `NpcTypeBuilder`). Voor arena-monsters:
     eigen NpcEditor + `defaultMode = none` zodat ze na gevecht niet gaan zwerven.
  3. packCache → installDist → beide werelden herstarten.
  4. Verificatie: `::mikeboss`, blijf staan ZONDER te klikken → boss loopt naar je toe en slaat
     binnen 1-2 ticks. Let op: buiten multiway valt max 1 NPC tegelijk aan (8-tick lastcombat-
     regel); in multiway-gebied stormt de hele wave.
  - Status 2026-06-10: uitgevoerd via `.data/symbols/hunt.sym` id 23 +
    `api/config/builders/HuntBuilds.kt` / `api/config/refs/BaseHuntModes.kt`, gekoppeld in
    `MikeBossEditor.kt` aan bosses en arena-monsters. `packCache` + `installDist` groen.
  - Valkuilen: gebruik GEEN `npc.setHunt()/setHuntMode()` als primair mechanisme (wordt door
    `resetHunt()` teruggezet na het eerste doelwit); prebuilt `huntmodes.aggressive_melee`
    werkt maar jaagt maar één keer per leven (`findKeepHunting=false`); `constant_*` negeert
    spelers met combat > 2×vislevel; `ApPlayer2` gebruikt `attackRange` uit de editor.
- ☐ **Dragon dagger special attack** (framework bestaat volledig; ~4 kleine bestanden):
  1. `api/config/refs/BaseObjs.kt` (bij regel ~795): refs `dragon_dagger`, `dragon_dagger_p`,
     `dragon_dagger_p_plus = find("dragon_dagger_p+")`, `..._p_plus_plus = find("dragon_dagger_p++")`.
  2. `content/other/special-attacks/.../configs/SpecialAttackSeqs.kt`: `val dragon_dagger = find("puncture")`;
     `SpecialAttackSpotanims.kt`: `find("sp_attack_puncture_spotanim")`.
  3. Nieuw `melee/DragonDaggerSpecialAttack.kt` — kopieer `DragonLongswordSpecialAttack.kt`:
     2× `manager.rollMeleeDamage(this, target, attack, accuracyMultiplier = 1.15, maxHitMultiplier = 1.15)`,
     2× `queueMeleeHit`, `giveCombatXp(dmg1+dmg2)`, `manager.continueCombat(...)`, return `true`
     (= energie-aftrek). GEEN eigen attack-delay zetten.
  4. Registreer in `content/other/special-attacks/.../SpecialAttackModule.kt`:
     `addSetBinding<SpecialAttackMap>(DragonDaggerSpecialAttack::class.java)`.
  5. Script-only → installDist. Faalt de startup met "not found in the required enums": die
     variant uit de registratie halen (energie-kosten komen uit cache-enum 906).
  Verificatie: dd uitrusten, spec-bar aanklikken (werkt al), aanvallen → dubbele hitsplat, -25% energie.
  Status 2026-06-10: Dragon dagger special v1 uitgevoerd: refs voor alle normale dagger-varianten,
  `puncture` anim/spotanim, nieuwe `DragonDaggerSpecialAttack.kt` met dubbele melee-hit en registratie
  in `SpecialAttackModule`. `compileKotlin` + `:server:app:installDist` groen; W1/W2 startup groen.
- ☐ Wilderness-regels (skull, item-on-death keep-3, wildy-levels) — groter; eerst bovenstaande.
- ☐ Boss-mechanics v2 (fases/AoE) — bouwen op aggressie zodra die werkt.

### FASE 3 — Minigames & endgame ◐
- ✅ Arena (10 waves) · ✅ dice/flip/slots/mystery · ✅ progressie/achievements/diary
- ✅ **Fight Caves-gauntlet op de ECHTE Fight Cave-map** (onderzocht; geen instancing nodig):
  - Locatie: TzHaar Fight Cave, spawn `CoordGrid(0, 37, 79, 45, 61)` (= 2413,5117,0); de
    mapsquares (37,79)-(38,80) zitten in de cache én zijn al **multiway** (Tzhaar-polygon) —
    hele waves vallen dus tegelijk aan (combineer met aggressie hierboven!).
  - Area-lifecycle (de elegante kern): regel `32763<TAB>fight_cave_arena` in
    `.data/symbols/area.sym` + `build("fight_cave_arena")` in `api/config/builders/AreaBuilds.kt`
    + `val fight_cave_arena = find(...)` in `api/config/refs/BaseAreas.kt` + in mikeshop een
    `object FightCaveMapAreas : MapAreaBuilder() { override fun onPackMapTask() {
    area(areas.fight_cave_arena) { mapSquare(MapSquareKey(37,79)); mapSquare(MapSquareKey(38,79));
    mapSquare(MapSquareKey(37,80)); mapSquare(MapSquareKey(38,80)) } } }` → packCache.
  - Script: `onArea(areas.fight_cave_arena) { startRun() }`, `onAreaExit(...) { endRun() }` —
    exit vuurt bij weglopen, teleport, DOOD (respawn-teleport) én logout → één cleanup-punt,
    "1 leven" gratis. Code-skelet = `ArenaMinigame.kt`, maar: vaste absolute spawn-coords i.p.v.
    speler-relatief; npcs in `run.npcs` bijhouden; opruimen met `npcRepo.del(npc, Int.MAX_VALUE)`;
    één speler tegelijk (`var occupant: Player?`); kill-detectie via `occupant` i.p.v. findHero.
    NPC-types: tzhaar-namen verifiëren via B10, óf NpcEditor-getunede vrije types — death-hook-
    conflictcheck B8 verplicht! Beloning: fire cape bestaat niet als obj — gebruik
    `infernal_cape` of coins + titel-broadcast.
  - Status 2026-06-10: v1 gebouwd in `FightCaves.kt` + `FightCaveMapAreas.kt`; `::fightcaves`
    teleporteert naar de echte Fight Cave, area-enter start 6 waves, area-exit ruimt NPC's op,
    beloning = fire cape + coins. `packCache` + `installDist` groen; W1/W2 herstart. Nog niet
    handmatig in-game uitgespeeld.
  - NIET `onPlayerQueue(queues.death)` binden (al geclaimd door `PlayerDeathScript`).
  - Later upgraden naar echte instance: `RegionTemplate.create { copyAllLevels(296, 632) {
    zoneWidth = 8; zoneLength = 8 } }` → `regionRepo.add(template)` → `telejump(region.normal[0,37,79,45,61])`
    (voorbeeld: `content/travel/canoe/.../CanoeTravelling.kt:193-508`). Let op: speler die in een
    instance uitlogt wordt op rauwe x/z gesaved → onAreaExit-teleport dekt dit (vuurt vóór save).
- ☐ **Arena v2**: hoogste-wave persistent (varp-recept B3), hiscore-bord op de hub (quest-log-
  UI-techniek, Fase 4), multi-speler-waves (occupant-lijst i.p.v. één Run per speler).
  Status 2026-06-11: hoogste-wave persistentie uitgevoerd via varp `9007 mike_arena_best_wave`;
  `::arenastats` toont je record en `::arena` meldt/updated nieuwe records. `compileKotlin`,
  `packCache` en `:server:app:installDist` groen; W1/W2 herstart op 43594/43595 zonder errors.
  Status 2026-06-12: hiscore-bord v1 uitgevoerd met `::arenatop`: leest `characters.varps`
  uit de huidige world-DB via server Database, parse't JSON met Jackson en toont top 20 in de
  bestaande `questjournal` UI. Online spelerrecord wordt mee gemerged zodat je laatste wave
  zichtbaar is voor logout.
- done **Collection log**: eigen simpele variant via `::collection` met questjournal-UI.
  Status 2026-06-12: uitgevoerd in `CollectionLog.kt`: toont Arena best wave/champion, fire cape
  in inventory, quest-completions, diary reward, boss kill milestones en PK kills/points. Gebruikt
  bestaande varps + `PvmProgress`; geen packCache nodig.
- done Daily events: MOTD/global XP preset-menu via `::daily`, status via `::event`.
  Status 2026-06-12: uitgevoerd in `DailyEvents.kt`: presets Normal 1x, Double XP 2x,
  Skilling Boost 3x en PvP Weekend 1.5x. Schrijft `login_broadcast` +
  `global_xp_rate_in_hundreds` naar `realms`, update live realm config en zet online players
  direct op de nieuwe global XP-rate. Geen packCache nodig.

### FASE 4 — Quests & UI ◐
- ✅ Quest-engine (varps, multi-stage, ::questlog chat-versie)
- ✅ **Echte quest-log-INTERFACE** (geïmplementeerd): cache-interface
  `questjournal` (id 119) heeft titel + 200 tekstregels + scrollbar + close. Recept:
  ```kotlin
  internal object QuestLogInterfaces : InterfaceReferences() { val questjournal = find("questjournal") }
  internal object QuestLogComponents : ComponentReferences() {
      val title = find("questjournal:title"); val close = find("questjournal:close")
      val lines = (1..200).map { find("questjournal:qj$it") }
  }
  // in ProtectedAccess (via protectedAccess.launch):
  ifSetText(QuestLogComponents.title, "Quest Journal")
  regels.forEachIndexed { i, r -> ifSetText(QuestLogComponents.lines[i], r) }   // VÓÓR open!
  (regels.size until 200).forEach { ifSetText(QuestLogComponents.lines[it], "") }
  ifOpenMainModal(QuestLogInterfaces.questjournal)
  ifSetEvents(QuestLogComponents.close, -1..-1, IfEvent.Op1)   // knop anders dood
  // + onIfModalButton(QuestLogComponents.close) { ifClose() }
  ```
  Geen hash in `find()` (auto-resolve op naam); geen packCache (refs zijn geen nieuwe types);
  kleurtags: `<col=000080>`, doorstrepen: `<str>`. Scrollbar raar? Alternatieven:
  `journalscroll` (741) of `longscroll` (625, één `scroll_text`-component met `<br>`).
  Zelfde techniek = leaderboard/diary/collection-UI.
  Status: `::questlog` opent nu `questjournal` met titel, quest-statusregels, kleurstatus en
  werkende close-knop.
- ✅ Questreeks 2.0 met boss-kill-stages: `::bossslayer` gebruikt
  `PvmProgress.bossKills(player)` voor 3/10-kill stages, persistente quest-varp en quest-log-regel.

### FASE 5 — Economie ☐
- ✅ 8 winkels + bank beide werelden + PK-puntenwinkel
- ☐ **"GE" als NPC-orderboek (Plan B — laag risico, DOE DEZE EERST).** Alle bouwstenen bestaan:
  `menu(...)`, `objDialog("Choose an item.", stockMarketRestriction = true)` (ingebouwde GE-
  item-zoeker!), `countDialog(...)`, `invDel/invAdd` (check `.success`), `MarketPrices`
  (inject; default = item-cost) voor richtprijs. Orderboek = singleton met buy/sellOrders
  `(accountId, objId, remaining, price)`; canonicaliseer met `objTypes.uncert(type)`; escrow bij
  plaatsing (items/coins direct innemen); match op kruisende prijs (handel op prijs van de
  staande order); "Collect"-optie keert uit (overloop → `invAddOrDrop`). Koppel aan de Hub-clerk
  (`HubNpcs.clerk`) als extra menu-optie. v1 in-memory; v2 = Flyway-migratie
  `api/db/src/main/resources/db/migration/V7__ge_orders.sql`.
  Status 2026-06-10: v1 uitgevoerd in `NpcGrandExchange.kt` met in-memory buy/sell-orderboek,
  escrow bij plaatsen, matching op kruisende prijs, collect-uitbetaling en itemzoeker via
  `objDialog(..., stockMarketRestriction = true)`. Toegang via `::npcge` en `HubNpcs.clerk`
  option 2. `compileKotlin` + `:server:app:installDist` groen; W1/W2 herstart en luisteren op
  43594/43595.
- ☐ **Echte speler-trade (Plan A — medium risico, ná multiplayer-test).** Server-kant is er:
  Trade = **onOpPlayer4** (`api/script-advanced`; labels gezet in `content/other/login/LoginScript.kt:136`),
  offer-inv = `invs.tradeoffer` (id 90, = `ProtectedAccess.tempInv`), interfaces `trademain`(335)/
  `tradeside`(336)/`tradeconfirm`(334) in de cache, **atomaire twee-speler-swap werkt**:
  ```kotlin
  val result = playerA.invTransaction(from = aOffer, into = playerB.inv) {
      val fromA = select(aOffer); val intoB = select(playerB.inv)
      val fromB = select(bOffer); val intoA = select(playerA.inv)
      moveAll(fromA, intoB); moveAll(fromB, intoA)
  } // niets committed als íéts faalt → "not enough space"
  ```
  Template voor de hele UI-flow: `content/interfaces/equipment/.../prices/GuidePriceScript.kt`
  (zelfde inv + ifOpenMainSidePair + interfaceInvInit + knoppen + close-restore).
  Onbekenden (client-side, eerst testen): initialiseert trademain zich op if_open; partner-offer
  spiegelen vereist handmatige `UpdateInvFull(-(combined), 32858, ...)`-write. Altijd accept-
  flags resetten bij elke offer-mutatie (anti-switch-scam). `tradeoffer` is Perm → login-restore
  toevoegen (gestrande items terug naar inv).
- ✅ Geld-sinks: instance-fees (Fight Caves entree), cosmetica in PK-puntenwinkel.
  Status 2026-06-11: Fight Caves entree uitgevoerd in `FightCaves.kt`: `::fightcaves`
  vraagt nu 100.000 coins via `invCoinTotal`/`invTakeFee` voordat de teleport naar de cave
  gebeurt. Geen betaling als de cave bezet is of als dezelfde speler al binnen is.
  `compileKotlin` + `:server:app:installDist` groen; W1/W2 herstart en luisteren op 43594/43595.
  Status 2026-06-12: `::pkspend` uitgebreid met categorie-menu's en cosmetic rewards
  (Santa hat, Robin hood hat, red halloween mask, black partyhat) als PK-punten sink. Rewards
  gebruiken nu strict inventory-add; bij volle inventory worden geen PK-punten afgeschreven.
  `compileKotlin` + `:server:app:installDist` groen; W1/W2 herstart en luisteren op 43594/43595.

### FASE 6 — Multiplayer & sociaal ☐
- ☐ **Structurele 2-client-test** (W2): PvP-duel → PK-punten checken; samen arena; daarna pas trade.
- ☐ **Hiscores-webpagina**: lees `game.db` read-only (`mode=ro`) met de SQL uit sectie A;
  data is logout-vers (geen autosave). Koppelbaar aan bestaande dashboard-kennis.
- ☐ PM/friends/clanchat — engine-onderzoek nodig (nog niet gedaan).

### FASE 7 — Polish & beheer ☐
- ☐ Watchdog + backups (Fase 0-items) eerst; daarna log-rotatie.
- ☐ Performance-test met 3+ clients.
- ☐ **Niet publiek hosten** (Jagex-IP; lokale leer-/testserver).

---

## D. AANBEVOLEN VOLGORDE (met geschatte moeilijkheid)
1. **Aggressie** (Fase 2) — makkelijk, content-only, grootste gameplay-effect. ½ recept staat hierboven.
2. **PK-punten persistent** (Fase 1) — makkelijk, bewezen varp-recept.
3. **Fight Caves** (Fase 3) — middel; hergebruikt arena-code + aggressie + area-systeem.
4. **Quest-log-UI** (Fase 4) — makkelijk-middel; opent de deur naar alle andere UI's.
5. **Watchdog/backups** (Fase 0) — makkelijk, puur PowerShell.
6. **NPC-GE Plan B** (Fase 5) — middel; geen client-onbekenden.
7. **2-client-test** (Fase 6) → daarna trade Plan A en de rest.

## E. CHECKLIST VOOR ELKE WIJZIGING (dwingend)
1. Nieuwe item/npc/loc-namen exact gevalideerd in de .sym? (B10)
2. Death-hook-conflictcheck gedaan? (B8)
3. Nieuwe types → packCache vóór installDist; daarna BEIDE werelden herstart?
4. `onPlayerQueue(queues.death)` / bestaande engine-queues NIET dubbel geclaimd?
5. NPC-spawns in try/catch + relatief aan bekende vrije tegel?
6. World-gating nodig (alleen W1/W2)? → env-check (B9).
7. Na start: log toont `Loaded NNN scripts` (aantal +1 per nieuw script) + `Bound to ports`?
8. In-game verificatiestap uitgevoerd en beschreven?
