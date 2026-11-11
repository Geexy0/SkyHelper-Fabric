# FarmHelper Fabric (Minecraft 26.3)

Client-side Fabric farming helper for **Minecraft 26.3**, ported from the legacy FarmHelper / BetterFarmHelper lineage and retargeted for the modern Fabric stack.

This **clean** build ships **without Discord webhooks, remote control, or analytics phone-home**. Status and failsafe messages stay in local debug logs under `.minecraft/logs/farmhelper/`.

## What it does

Automates Hypixel SkyBlock garden farming macros:

- **Crop macros** — S-shape / vertical lanes, sugar cane, cocoa, cactus, mushroom variants, circular crops
- **Crop presets** — `S_WHEAT` / `S_POTATO` / `S_CARROT` lock camera to **yaw -90 / pitch 0**; `S_NETHER_WART` uses pitch **~3.0**
- **Failsafes** — disconnect, teleport, rotation, knockback, inventory, chat keywords, low BPS, and related detectors
- **Automation modules** — visitors, pests, auto-sell, reconnect, cookie/god pot/repellent, bazaar helpers, and more
- **In-game config UI** — `/fh ui` (Print Screen by default on this build)
- **Rewarp / spawn tools** — save recovery points from your current position

## Requirements

| Piece | Version |
| --- | --- |
| Minecraft | **26.3** |
| Fabric Loader | **0.20.1+** |
| Fabric API | **0.166.x+26.3** |
| Java | **25+** (client runtime) |

Install like any Fabric client mod: drop the release JAR into your `mods` folder (OneClient / Prism / official launcher).

## Quick start

1. Join SkyBlock and warp to your **Garden**.
2. Open settings: `/fh ui` or the Open Menu keybind.
3. Pick a **Macro Type** (e.g. wheat/potato/carrot/nether wart) and tap **Apply Preset** in Quick Setup if needed.
4. Optional: stand on a pad → `/fh rewarp add`. Set spawn with `/fh spawn set`.
5. Start: `/fh start` or the Start / Toggle keybind.
6. Stop: `/fh stop`.

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

### Manual UI checklist (after SDL mouse fix)

1. `/fh ui` opens; sidebar section switching works (Quick Setup → Macro → …).
2. Left-click toggles, dropdowns, and text fields respond.
3. Search filters setting rows.
4. Escape / close button dismisses the screen.
5. With macro running and a rewarp/pest marker active, HUD shows projected box/tracer/text overlays.

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

## Disclaimer

Automated macros may violate Hypixel or other server rules. Use at your own risk. This project is provided as-is for personal / educational use. The authors are not responsible for bans, losses, or account actions.

## Credits

- Original FarmHelper / JellyLabScripts lineage
- [fruvs/BetterFarmHelper](https://github.com/fruvs/BetterFarmHelper) Fabric port work
- This repository: Minecraft **26.3** retarget + privacy clean-up

## License

CC0 1.0 (see `LICENSE`).
