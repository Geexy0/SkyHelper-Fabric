# SkyHelper Fabric (Minecraft 26.3)

Multi-aspect Hypixel SkyBlock helper for **Minecraft 26.3** (Farming + Fishing today; Mining / Foraging next). Ported from the legacy FarmHelper / BetterFarmHelper lineage and retargeted for the modern Fabric stack.

In-game commands and keybinds still use the **`/fh`** / FarmHelper control category for continuity.

This **clean** build ships **without Discord webhooks, remote control, or analytics phone-home**. Status and failsafe messages stay in local debug logs under `.minecraft/logs/farmhelper/`.

## Aspects

| Aspect | Status |
| --- | --- |
| **Farming** | Supported — garden crop macros, pests, visitors, sell, failsafes |
| **Fishing** | Supported — auto cast/reel, anti-detect, sea creatures, trophy / golden fish |
| **Mining** | Soon (UI stub) |
| **Foraging** | Soon (UI stub) |

Switch aspects in `/fh ui` (left sidebar).

## Farming

- **Crop macros** — S-shape / vertical lanes, sugar cane, cocoa, cactus, mushroom variants, circular crops
- **Crop presets** — `S_WHEAT` / `S_POTATO` / `S_CARROT` lock camera to **yaw -90 / pitch 0**; `S_NETHER_WART` uses pitch **~3.0**
- **Failsafes** — disconnect, teleport, rotation, knockback, inventory, chat keywords, low BPS, and related detectors
- **Automation modules** — visitors, pests, auto-sell, reconnect, cookie/god pot/repellent, bazaar helpers, and more
- **Rewarp / spawn tools** — save recovery points from your current position

## Fishing

- **Auto fish** — cast → bite detect → reel → recast
- **Anti-detect** — randomized timings, idle look, AFK breaks, session limit, failsafe/staff pause
- **Sea creatures** — optional kill with Hyperion / Fire Veil / Frozen Scythe / sword auto-select
- **Trophy** — golden fish ESP/tracer and optional priority track (Crimson Isle–oriented extras)

## Requirements

| Piece | Version |
| --- | --- |
| Minecraft | **26.3** |
| Fabric Loader | **0.20.1+** |
| Fabric API | **0.166.x+26.3** |
| Java | **25+** (client runtime) |

Install like any Fabric client mod: drop the release JAR into your `mods` folder (OneClient / Prism / official launcher), or run `./scripts/install-to-oneclient.sh` after a local build.

## Quick start

### Farming

1. Join SkyBlock and warp to your **Garden**.
2. Open settings: `/fh ui` → **Farming**.
3. Pick a **Macro Type** and tap **Apply Preset** in Quick Setup if needed.
4. Optional: stand on a pad → `/fh rewarp add`. Set spawn with `/fh spawn set`.
5. Start: `/fh start` or the Start / Toggle keybind. Stop: `/fh stop`.

### Fishing

1. Stand at your fishing spot (water or Crimson Isle lava).
2. Open `/fh ui` → **Fishing** → enable **Auto Fishing** (and combat/trophy options as needed).
3. Hold a fishing rod in hotbar; keep combat weapons available if sea-creature kill is on.

### Useful commands

```text
/fh status
/fh macro list
/fh macro type S_WHEAT
/fh feature list
/fh feature <id> on|off
/fh rewarp add|list|clear
/fh spawn set
/fh failsafe clear
/fh config save|reload
```

Keybinds live under **Options → Controls → FarmHelper** (authoritative). Advanced settings show SDL scancodes with key names; prefer rebinding in Controls rather than editing the numbers.

### Manual UI checklist

1. `/fh ui` opens; aspect sidebar switches Farming / Fishing / Soon stubs.
2. Left-click toggles, dropdowns, and text fields respond.
3. Search filters setting rows within the active aspect.
4. Escape / close button dismisses the screen.
5. Farming: with macro running and a rewarp/pest marker active, HUD shows projected box/tracer/text overlays.
6. Farming: Apply Preset sets camera defaults; pest ESP labels confirmed pests only (no bare bats).
7. Fishing: golden fish ESP draws when trophy/ESP toggles are on and a golden fish is in world.

## Build from source

```bash
./gradlew build
```

Output JAR:

```text
build/libs/farmhelper-fabric-<version>.jar
```

## Privacy / clean fork notes

- No Discord webhook posting
- No analytics / telemetry endpoints
- No remote-control websocket client
- Proxy username/password fields exist for optional SOCKS/HTTP proxy use; leave them blank unless you configure a proxy yourself
- Your personal `farmhelper-fabric.json` and Minecraft `options.txt` are **local only** — not part of this repository

## Status (`26.3.0-clean.17`)

- **Farming** — lanes, pests, visitors/sell, HUD (baseline from clean.16)
- **Fishing** — auto fish, anti-detect, sea combat, trophy/golden (clean.17)
- **Mining / Foraging** — UI stubs only (“Soon”)

## Disclaimer

Automated macros may violate Hypixel or other server rules. Use at your own risk. This project is provided as-is for personal / educational use. The authors are not responsible for bans, losses, or account actions.

## Credits

- Original FarmHelper / JellyLabScripts lineage
- [fruvs/BetterFarmHelper](https://github.com/fruvs/BetterFarmHelper) Fabric port work
- This repository: Minecraft **26.3** retarget, privacy clean-up, multi-aspect UI (Farming + Fishing)

## License

MIT (see `LICENSE`).
