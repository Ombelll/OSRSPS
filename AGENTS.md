# AGENTS.md — werken aan deze RS Mod-server

Gids voor (AI-)agents/devs die content toevoegen aan deze **lokale** rsmod-server.
Vat de werkwijze, snelheidsregels en patronen samen die in deze repo werken.

> Scope: dit is een **offline** test-/leerserver (revisie 233). Niet bedoeld om publiek
> te hosten. Custom content leeft los van de engine in eigen modules.

---

## 1. Project & paden

- Server: `C:\Users\Mike\rsmod`
- Custom content (zelf toegevoegd):
  - `content/custom/mikeshop/`   — winkel, custom item, commando's (`::mikeshop`, `::combatzone`, `::boss`, `::megaboss`, `::starter`, teleport-hub)
  - `content/custom/cowdrops/`   — droptables voor 117 monsters (`MonsterDrops`) + `BulkMonsters`
  - `content/custom/combatfix/`  — koppelt juiste `attack_anim` per NPC (fix voor "rare" aanvalsanimaties)
  - `content/skills/mining/`     — Mining-skill (9 ertsen)
  - `content/skills/prayer/`     — botten begraven → Prayer-XP
  - `content/skills/smithing/`   — erts → bars (oven) → scimitar (aambeeld)
- Data (niet in git): `.data/` — `cache/`, `saves/game.db`, `symbols/*.sym`, `game.key`/`client.key`.

Modules worden **automatisch ontdekt**: elke map onder `content/` met een `build.gradle.kts`
wordt door `settings.gradle.kts` meegenomen.

---

## 2. Build & run — SNELHEIDSREGELS (belangrijk)

Builds zijn traag; kies de juiste:

| Situatie | Commando | Tijd |
|---|---|---|
| Alleen **scripts** toegevoegd/gewijzigd die naar **bestaande** cache-types verwijzen (commando's, skill-logica, droptables, `onOpLoc`/`onOpHeld` op bestaande objs/locs) | `gradlew run` | ~1-2 min |
| **Nieuwe of gewijzigde TYPES**: nieuw item/inv/loc/npc, of een `*Editor` (reskin, `attack_anim`, contentGroup, params) | `gradlew packCache` daarna `gradlew run` | ~1-3 min |
| Eerste keer / verse vanilla-cache nodig | `gradlew install` (download + pack + rsa) | ~5-8 min |
| Snel syntax checken | `gradlew :content:<pad>:compileKotlin` | ~30s-2min |

**Vuistregels (snelheid):**
- `install` = download vanilla-cache + pack + rsa. De **download (~30s) is bijna nooit nodig** — gebruik daarom **`packCache`** (alleen de packer) bij type-wijzigingen. `install` alleen de allereerste keer.
- Pure **script-content** heeft geen pack nodig → gewoon **`run`** (server resolved references bij opstart tegen de al-gepackte cache).
- Stop de server vóór `packCache`/`install` (file-locks op de cache + poort 43594).
- **LET OP — `configuration-cache` UIT laten.** `server/shared/build.gradle.kts` bouwt z'n content-deps dynamisch via `project(":content").subprojects` op configuratietijd. Config-cache cachet die lijst → **nieuwe content-modules worden dan NIET geladen** (script-count stijgt niet). Met config-cache uit pikt `run` nieuwe script-modules wél op.
- De `install`-flow is: `GameServerInstall` = LogbackCopy → **CacheDownloader** → **CachePacker** → RsaGenerator. `packCache` draait enkel `CachePacker`.

### Daemon & stabiliteit
- **Gebruik de Gradle-daemon** (laat `--no-daemon` weg) voor snelheid (warme JVM, incrementeel).
- Start de **server losgekoppeld** zodat hij niet door andere gradle-commando's wordt gekilld:
  ```powershell
  Start-Process gradlew.bat -ArgumentList "run","--console=plain","--no-daemon" `
    -WorkingDirectory "C:\Users\Mike\rsmod" -RedirectStandardOutput ".data\server-run.log" -WindowStyle Hidden
  ```
  De server draait met `--no-daemon` (geïsoleerd); daemon-builds raken hem dan niet.
- Stop de server vóór een `install` (cache-filelocks + poort 43594).
- `gradle.properties` heeft `parallel`, `caching`, `configureondemand` aan; **`configuration-cache` staat bewust UIT** (zie waarschuwing hierboven).

Server luistert op **poort 43594**. "Bound to ports: 43594" + "Loaded N scripts" in de log = klaar.

---

## 3. Nieuwe TYPES registreren (.sym)

Een **nieuw** type (niet in de vanilla cache) moet een naam→id in een `.sym` hebben, anders faalt
de packer: *"cache builders use names that are not defined in a .sym file"*.

- Bestaand voorbeeld: `mike_shop` is handmatig toegevoegd aan `.data/symbols/inv.sym` als `2000	mike_shop` (tab-gescheiden, hoog vrij id).
- Items/locs/npcs die al in de cache zitten → gewoon `find("naam")`, geen sym-edit nodig.
- `.sym`-bestanden worden niet door code overschreven; handmatige toevoegingen blijven staan.

---

## 4. Content-patronen (met voorbeelden in deze repo)

**Commando** — `onCommand` gebruikt een builder-DSL (NIET de 3-arg helper uit `AdminCommands`):
```kotlin
onCommand("naam") { desc = "uitleg"; cheat { /* this: Cheat, heeft player */ } }
```
Teleport: `protectedAccess.launch(player) { telejump(CoordGrid(...)) }` (inject `ProtectedAccessLauncher`).
Item geven: `player.invAdd(player.inv, obj, count, strict = false)` (import `org.rsmod.api.invtx.invAdd`).

**Custom item** = reskin van bestaand item via `ObjEditor` (model blijft; alleen gezette velden overschrijven):
```kotlin
internal object X : ObjEditor() { init { edit(objs.cabbage) { name="..."; desc="..."; cost=5000 } } }
```
Volledig nieuw item met eigen model vereist cache-editing (model-ids zitten niet in symbols) — vermijd.

**Winkel** — `InvBuilder` bouwt nieuwe voorraad (`autoSize=true` + `stock += stock(obj, count, restockCycles)`;
`count=0` = winkel koopt het van je). Openen zonder NPC: `shops.open(player, title, invType, buy%, sell%, change%)`.

**Monster-drops** — haak op de death-queue:
```kotlin
onNpcQueue(content.cow, queues.death) { death.deathNoDrops(this); objRepo.add(ore, coords, dur, hero) }
```
Per-type: `onNpcQueue(npcType, queues.death) { ... }` (overschrijft de default; doe zelf de death-sequence).
Standaard `NpcDeath.spawnDeathDrops` dropt alleen botten (`// TODO: Drop tables`).

**Gathering-skill** (Mining-patroon) — `onOpLoc1(locType) { ... }` in `ProtectedAccess`:
check `player.<skill>Lvl`, `invAdd(inv, item)`, `statAdvance(stats.<skill>, PlayerStatMap.toFineXP(xp).toDouble())`.

**Inventory-actie** (Prayer/burying) — `onOpHeld1(obj) { invDel(inv, obj, 1); statAdvance(...) }`.

**NPC-eigenschap zetten** (bv. attack-animatie) — `NpcEditor`:
```kotlin
edit(npcType) { param[params.attack_anim] = seqRef }
```

Spawnen vanuit code: `Npc(npcTypes[typeRef], coords)` (resolve ref → `UnpackedNpcType`), `npc.mode = NpcMode.None`, `npcRepo.add(npc, duration)`.

Module-deps: meestal volstaat `implementation(projects.api.pluginCommons)`. Voeg toe waar nodig:
`projects.api.script` (onCommand/onOpHeld), `projects.api.type.typeBuilders` (nieuwe types), `projects.api.death`/`projects.api.repo` (NpcDeath/ObjRepository).

---

## 5. Valkuilen (in deze sessie geleerd)

- **`TypeReferences`-subklassen mogen niet `private`** zijn → gebruik `internal`/`object`.
- **OSRS-chat accepteert geen plak/paste** — bij geautomatiseerd typen: in stukjes van ≤8 tekens, anders pakt de tool de clipboard-fastpath en komt er niets binnen.
- **NPC-aanvalsanimaties zitten niet in de publieke cache** → rsmod default = `human_unarmedpunch`. Fix per NPC met `param[params.attack_anim]` (zie `combatfix`).
- **Server stierf door de gradle-daemon**: als de server op de daemon draait kan een volgend gradle-commando hem killen → draai server met `--no-daemon`.
- **`install` regenereert de RSA-sleutel niet** als `.data/game.key` al bestaat → modulus blijft gelijk → RSProx-config blijft geldig.

---

## 6. Spelen / verbinden (client)

- Client: **RSProx** in `C:\Users\Mike\rsprox` → `java -jar rsprox-launcher.jar` → kies **RSMod (233)** → ▶.
- Proxy-target: `C:\Users\Mike\.rsprox\proxy-targets.yaml` (name=RSMod, `jav_config_url=https://client.blurite.io/jav_local_233.ws`, modulus uit `.data/client.key`, `game_server_port=43594`).
- Inloggen: dev-realm heeft `ignorePasswords=true` + `autoAssignDisplayNames=true` → elke naam/wachtwoord werkt.
- Na een server-herstart: vorige client-sessie is stale → in RSProx opnieuw ▶ voor een verse client.

## 7. Handige dev-commando's (in-game)
`::master` (max stats), `::tele`/`::telezone`, `::npcadd <dur> <npc>`, `::locadd <dur> <loc>`, `::invadd <obj>`,
`::mypos`, `::reboot` — plus custom: zie §10.

---

# DEEL B — Volledige content-inventaris (zodat je niet hoeft te gokken)

> Server-staat: ~144 scripts, poort 43594, revisie 233. Alle **23 OSRS-skills** zijn een echt
> systeem. Combat (melee/ranged/magic PvN) zit in de **engine-core** (`api/combat/*`), niet in
> custom content — niet zelf herbouwen.

## 8. Alle modules & bestanden

| Module (pad onder `content/`) | Inhoud |
|---|---|
| `custom/mikeshop/` | Winkels (`::mikeshop`, `::supplyshop`), `::maxgear`, `::starter`, `::bank`, eten/drinken (`Eating.kt`), teleport-hub, **6 bosses** (`MikeBoss.kt` + `ExtraBosses.kt` + `MikeBossEditor.kt`), supply-shop (`SupplyShopInvs.kt`) |
| `custom/cowdrops/` | Droptables 117 monsters (`MonsterDrops`, `BulkMonsters`) |
| `custom/combatfix/` | `attack_anim` per NPC (CombatAnimEditor) |
| `skills/mining/` | 9 ertsen + **depletion/respawn (in-memory via MapClock) + edelsteen-vondsten** |
| `skills/smithing/` | smelt alle ertsen→baren (oven) + smeed **scimitar/full helm/platelegs/platebody** per tier (aambeeld, prioriteit op baar-aantal) |
| `skills/cookery/` | koken op vuur + **aanbrand-kans** (level-gated, 6 gerechten) |
| `skills/herblore/` | **volledige keten**: schoonmaken→onaffe potion→afgewerkte potion (attack/strength/restore)→drinkbaar (statBoost) |
| `skills/fishing/` | visplekken (op+ap), tool-afhankelijke vangst |
| `skills/hunter/` | vang creatures (chinchompa/butterfly/kebbit) via op+ap |
| `skills/construction/` | hamer-op-plank → meubel + XP (4 plank-tiers) |
| `skills/runecrafting/` | **klik altaar (`onOpLoc1`!)** → essence→runes (NIET `onOpLocU`) |
| `skills/farming/` | zaad op patch → instant oogst |
| `skills/prayer/` | botten begraven |
| `skills/firemaking/` `cookery/` `crafting/` `fletching/` `thieving/` `extra/`(agility + `::train`) | zie bestanden |
| `skills/magic/spell-attacks/` | combat-magic (vanilla rsmod) · `skills/magic/utility/` | enchant + high alchemy |
| `skills/woodcutting/` | vanilla rsmod (heeft al echte depletion via `next_loc_stage`-param) |

## 9. De 6-boss gauntlet (`custom/mikeshop/`)

Bosses = bestaande **normaal-spawnbare** monsters opgevoerd via `NpcEditor` (naam/HP/stats/`attack_anim`).
**LET OP:** unieke quest-NPC's (bv. `elvarg`) renderen NIET als je ze handmatig spawnt — gebruik gewone
monsters (`green_dragon`, `greater_demon`, `black_demon`, `blue/red/steel_dragon`).

| Commando | Basis-NPC | HP | Top-drops |
|---|---|---|---|
| `::mikeboss` | green_dragon | 600 | rune-gear, 1/50 dragon scim |
| `::demonboss` | greater_demon | 800 | 1/40 whip |
| `::finalboss` | black_demon | 1200 | 1/15 whip, 1/30 bandos |
| `::dragonboss` | blue_dragon | 1400 | dragon dagger/chainbody, 1/14 whip |
| `::infernoboss` | red_dragon | 1800 | 1/10 whip, dragon claws |
| `::godboss` | steel_dragon | 2500 | dragon platebody (altijd), 2h/claws/whip |

`::bosses` somt ze op. Spawn-pattern: `spawnBoss(player.coords, ref)` → `Npc(npcTypes[ref], coords.translate(2,2)).also{it.mode=NpcMode.None}` → `npcRepo.add(boss,1000)`. **Spawn bij `player.coords`, NIET een vaste arena-coord** (die kan in water/geblokkeerd vallen → onzichtbaar). Drops via `onNpcQueue(ref, queues.death)`.

## 10. Alle custom commando's

Gear/test: `::maxgear` (echte BIS-melee: scythe of vitur, justiciar faceguard, bandos, ferocious gloves,
primordial boots, amulet of strength, ultor ring, infernal cape + ranged-kit bow/arrows + magic-kit
staff/runes + super combat + 99 combat + alle prayers), `::starter` (10 sharks-kit), `::train <skill> [xp]`, `::bank`.
Winkels: `::mikeshop`, `::supplyshop` (runes/vials/secondaries/vis-tools). Bosses: zie §9.
Combat-zone: `::combatzone`, `::boss`, `::megaboss`. Teleports: `::home/varrock/falador/edge/ge/mine/wild`.

## 11. Veel-gebruikte exacte cache-namen (bespaart grep-werk)

- Runes (1 woord): `firerune waterrune airrune earthrune mindrune bodyrune deathrune naturerune chaosrune lawrune cosmicrune`; essence = `blankrune`.
- Grimy kruid = `unidentified_<herb>`; schoon = `<herb>_leaf`/`<herb>_weed`/`guam_leaf`/`tarromin`/`harralander`. Vial water = `vial_water`, leeg = `vial_empty`. Onaffe potion = `<herb>vial` (bv. `guamvial`). Afgewerkt = `3dose1attack`/`3dose1strength`/`3dosestatrestore`; super combat = `4dose2combat`.
- Bandos tassets = **`bandos_skirt`** (interne naam!). Berserker ring (i) = `nzone_berzerker_ring`. Fire cape = `tzhaar_cape_fire`. Bars = `<tier>_bar` (adamant = `adamantite_bar`, rune = `runite_bar`). Gems = `uncut_sapphire/emerald/ruby/diamond`.
- Visplekken-NPC's: `fishing_spot_aerial` (op1="Catch"), `0_39_54_brut_fishing_spot` (op1="Use-rod"). Slayer-monsters: `slayer_<naam>_1` (bv. `slayer_banshee_1`). Slayer-master: `slayer_master_nieve`.

## 12. Nieuwe patronen & API's (deze sessie geverifieerd)

- **Equipment aantrekken** (in `cheat{}`): `player.<slot> = InvObj(objRef)`. Slots (`org.rsmod.api.player.<slot>`): `hat torso legs righthand lefthand hands feet ring front`(amulet)`back`(cape). 2h-wapen → géén `lefthand` zetten.
- **Heal/boost** (op `ProtectedAccess` én `Player`): `statHeal(stat, constant, percent)` (HP/prayer-restore), `statBoost(stat, constant, percent)` (potion-boost).
- **Huidige game-tick:** `MapClock` injecteren, maar gebruik de **operators** (`mapClock + n`, `mapClock < n`), NIET `.cycle` (resolved niet op het content-classpath). In-memory state (depletion) = gewoon een `HashMap<CoordGrid,Int>` als veld.
- **Loc-coords in handler:** `onOpLoc1(loc) { mine(it.loc.coords) }` — de lambda-param is `LocEvents.Op1` met `.loc.coords`.
- **`op` vs `ap`:** sommige NPC's (visplekken) vuren op afstand → registreer **zowel** `onOpNpc1/2` als `onApNpc1/2` op dezelfde handler.
- **Bank openen:** `player.openBank(eventBus)` (inject `EventBus`; dep `projects.content.interfaces.bank`).
- **NPC-stats opvoeren:** `NpcEditor.edit(ref){ name=..; hitpoints=..; attack=..; strength=..; defence=..; param[params.attack_anim]=seq }` → **packCache nodig**.

## 13. Extra valkuilen (deze sessie)

- **Bewerk geen bron-bestanden tijdens een lopende build** → compile pakt half-bewerkte file → faalt.
- **Redirect-volgorde:** gebruik `> log 2>&1` (NIET `2>&1 > log`) anders mis je compile-fouten (stderr).
- **Integratietests:** de `integration`-suite (plugin `id("integration-test-suite")`, taak heet **`integration`** niet `integrationTest`) is een apart compilatie-doel → `internal` types zijn er NIET zichtbaar (maak ze `object`/public) tenzij friend-path. NPC-`op`-interacties zijn er lastig betrouwbaar te triggeren (reachability); `onOpLoc`-skills testen wél goed. Test-API: `runGameTest(Script::class){ spawnNpc(coord, npcTypes[ref]); player.opNpc1(npc); advance(ticks); assertMessageSent(...) }`.
- **Runecrafting-altaar = `onOpLoc1` ("Craft-rune"), niet `onOpLocU`** (essence-op-altaar vuurt niet).
- **`statAdvance(stat, fineXp)`**: zet altijd `PlayerStatMap.toFineXP(rawXp).toDouble()`.
