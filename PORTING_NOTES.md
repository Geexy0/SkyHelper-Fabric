# Porting notes (clean 26.3 fork — `26.3.0-clean.13`)

## Target

- Minecraft **26.3**
- Fabric Loader **0.20.1**
- Fabric API **0.166.x+26.3**
- Java **25** runtime
- Mojmap / Loom 1.19

## Privacy strip

Removed / neutralized versus older FarmHelper builds:

- Discord webhook network posting (`LocalDebugLogService`; deprecated alias `DiscordWebhookService`)
- Analytics / phone-home endpoints
- Remote-control websocket / bot integrations

## Input / UI notes for 26.3

- Keyboard bindings use **SDL scancodes** (not GLFW keycodes)
- Left mouse button is `InputConstants.MOUSE_BUTTON_LEFT` (**1**), not GLFW `0`
- Camera freelook hooks `Camera.alignWithEntity` on this mapping set

## Presets

- `S_WHEAT` / `S_POTATO` / `S_CARROT` — S-shape vertical with forced yaw **-90** and pitch **0**
- `S_NETHER_WART` — S-shape with pitch **~3.0**
- `S_SUGAR_CANE` — S-shape, pitch **0**, yaw locked to current cardinal on Apply Preset
- `S_COCOA_BEANS*` / `S_COCOA_BEANS_LEFT_RIGHT` — pitch **~-70**, yaw ±90 on Apply Preset
- `S_MUSHROOM*` — pitch **-45** / **-30** defaults on Apply Preset
- Pest ESP / hunt requires an explicit pest species name or texture hash (bare bats rejected)
- Vertical lanes: row-end water/slab hop before anti-stuck; post-switch yaw relock for camera-locked crops
- Post-warp camera: dual-pass realign after `/warp garden` with HUD “realigning” status
- Jacob HUD match requires **jacob + contest** together (bare “contest” ignored)

## Still WIP

Expect edge-case bugs on unusual garden layouts. Core lane farming + visitors/sell/pests menu retries are the best-supported path.
