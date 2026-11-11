# FarmHelper Fabric release checklist

Match the [Geexy0/FarmHelper-Fabric](https://github.com/Geexy0/FarmHelper-Fabric) publish flow.

## Before tagging

1. **Secret scan** — no API keys, tokens, Discord URLs, or personal `options.txt` / `farmhelper-fabric.json` in the tree or commit.
2. **Version** — bump `mod_version` in `gradle.properties` (e.g. `26.3.0-clean.N`).
3. **Java** — CI / local build on **Java 25**.
4. **Assemble**

```bash
./gradlew clean test build
```

5. Confirm artifact: `build/libs/farmhelper-fabric-<version>.jar` (exclude `-sources`).

## Publish

1. Commit + push the release branch.
2. Tag: `v26.3.0-clean.N`
3. Create GitHub Release for that tag.
4. Attach the release JAR (not sources).
5. **Install into OneClient mods** (deletes old FarmHelper JARs, copies the new one):

```bash
./scripts/install-to-oneclient.sh
```

Target: `/home/geexy/.local/share/Polyfrost/OneClient/.minecraft/mods/26.3 Fabric/`

6. Smoke: launch 26.3 Fabric, open `/fh ui`, start/stop once.

## Privacy reminders

- No Discord webhooks / analytics / remote control.
- `LocalDebugLogService` writes only under `.minecraft/logs/farmhelper/`.
- Do not advertise `ban_info_ws` or other analytics stubs.
