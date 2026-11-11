# Farming nav + pest ESP design

Approved 2026-11-11.

## Menu

- Sidebar is aspect-level: **Farming** (active); Mining / Foraging stubs disabled (“Soon”).
- Farming opens a second-level sub-nav: Quick Setup → Macro → Pests → Visitors → Failsafes → Automation → Advanced → All.
- Settings content unchanged; search filters within the active aspect.
- Default: Farming → Quick Setup.

## Pest ESP

- Bare entity types (`bat`, `spider`, …) must not alone pass `isLikelyPest`.
- Require pest **name keyword** and/or **texture signature** (or name+type together).
- Denylist ambient/vanilla-looking bats and non-pest displays/armor stands without pest evidence.
