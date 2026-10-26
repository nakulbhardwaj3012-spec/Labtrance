# LabTrace Stage 1

A passive Fabric client telemetry foundation for a private anti-cheat test lab. This stage does not automate gameplay, change movement/combat, synthesize clicks, select targets, or implement evasion profiles.

## Target/toolchain

- Minecraft Java Edition 26.2
- Java 25
- Fabric Loader 0.19.3
- Fabric API 0.155.2+26.2
- Fabric Loom 1.17
- Mojang official mappings (26.1+ are unobfuscated; Yarn is not supported for this target)

Minecraft 1.20.x requires a separate legacy Loom/Yarn build and different Mixin targets. This project intentionally targets 26.2 only.

## Features

- `Module` base class with id/name/category/enabled state/keybind/settings
- `ModuleManager` with registration, lookup, toggle-by-name, keybind dispatch, and JSON persistence
- Thread-safe typed event bus: tick, packet-send metadata, packet-receive metadata, and render-start/render-end
- Right Shift opens a small diagnostic screen
- `.toggle telemetry` chat fallback
- Per-module JSON settings in `config/labtrace/config.json`
- Timestamped JSONL session logs in `logs/labtrace/`
- Optional per-tick position/rotation CSV; packet logs record packet class name and thread only, not payloads

The only bundled module is **Passive Telemetry**. It observes; it does not alter packets or player state. The CSV excludes click timing and target data.

## Build

Install a JDK 25 and Gradle 9.5.1. From this directory run:

```bash
gradle build
```

The mod JAR will be in `build/libs/`. Copy it into the `mods` folder of a Fabric 26.2 profile that has Fabric API installed. This source archive does not contain a Gradle wrapper.

## Run and configure

Launch Minecraft 26.2 with Fabric. Press **Right Shift** to open the diagnostic screen. Enable the recorder, then enable CSV export if you want movement samples. The fallback chat command is:

```text
.toggle telemetry
```

Edit `config/labtrace/config.json` to set `sessionLabel`, module keybinds, and settings. Module keybind values are GLFW key codes; `-1` means unbound. Each run creates a timestamped JSONL file. The CSV uses a stable session-labelled filename and appends rows if the same session label is reused.

## Log data

JSONL records have `timestamp`, `session`, `event`, `module`, `tick`, and `data`. Packet records include only `packet_class` and `thread`; they do not store packet contents. Module toggle records include the module's current setting values.

CSV columns: `timestamp,tick,x,y,z,yaw,pitch,on_ground`.

## Version notes

The Mixin hooks here target the unobfuscated 26.2 class/method names. Do not copy those selectors into a 1.20.x mod. The current environment used to prepare this archive has Java 21, while this project requires Java 25, so the project could not be compiled locally in this session; run `gradle build` under JDK 25 to validate against your installed Fabric dependencies.
