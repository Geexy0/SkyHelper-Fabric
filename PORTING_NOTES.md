# Porting notes (clean 26.3 fork)

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
- Pest ESP requires an explicit pest species name or texture hash (bare bats rejected)

## Still WIP

Expect edge-case bugs on unusual garden layouts, Jacob contests, and menu automation. Core lane farming is the best-supported path.
