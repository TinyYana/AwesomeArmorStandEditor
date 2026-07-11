<div align="center">

# AwesomeArmorStandEditor

**A friendly armor stand / display-entity scene editor for survival & creative servers.**

No angle math. No trial-and-error commands. Pick a pose from a menu, click, done.

</div>

## What it does

Pose armor stands and item/block/text display entities with a hybrid GUI + in-world tool. Save your work as a portable scene, share it with a copy-paste code, animate it with keyframes, or export it as a summon command / mcfunction datapack for your own map or server.

## Features

- **Preset library (zero-skill-required)** — can't pose anything? Open the graphical preset menu, click "wave / cheer / sit…" to apply instantly. One-click **Mirror** for left-right symmetry. One-click particle effect presets. Save your own pose back into the library for reuse.
- **Unified editing** for armor stands and display entities (item / block / text) — pose, transform, scale, rotation, equipment, flags, all through the same workflow.
- **Hybrid interaction** — a GUI control panel *and* a handheld tool for direct in-world nudging, with a live actionbar readout.
- **Particle effects** — budgeted, only fire near a player in a loaded chunk; never scans the world.
- **Keyframe animation** — a timeline with keyframes and live preview playback. Displays use client-side interpolation (near-zero server cost); armor stands animate tick-by-tick during editing.
- **Save / share / export** — every scene is a portable JSON blueprint you can place multiple times. Generate a share code (safe to paste in chat) so another player can import your build with one command. Export a one-click-copy `/summon` command, or a full mcfunction datapack (animation-driven) for a build that runs without the plugin installed.
- **Equipment menu** — click an item onto a slot to equip it, click with an empty cursor to unequip. Items are never consumed or duplicated.
- **Survival-safe** — element ownership tags (nobody can break or steal your build), per-player / per-chunk / global element caps, and automatic region-protection awareness (GriefPrevention / WorldGuard / Towny / Lands…). Anything that writes to server files (export, saving to the shared preset library) is admin/builder-only by default.
- **Zero hard dependencies** — runs on Spigot *or* Paper, no other plugin required.

## Compatibility

| | |
|---|---|
| Minecraft / Paper / Spigot | **1.26.2** |
| Java | **25** |
| API surface | Bukkit/Spigot only — runs identically on plain Spigot, not just Paper |
| Region protection | GriefPrevention / WorldGuard / Towny / Lands respected automatically via an event probe, no hard dependency |

## Quick start

```
/aase new MyBuild        start a new scene
/aase addstand           spawn an armor stand
(right-click it)         select it
/aase presets            open the preset library — click a pose, click an effect
/aase save               done
```

You never see a single angle number.

## Commands

| Command | Description |
|---|---|
| `/aase` | Open the control panel |
| `/aase tool` | Get the editing tool |
| `/aase presets` | Preset library GUI (poses / effects / mirror) |
| `/aase new <name>` / `addstand` / `adddisplay <item\|block\|text>` | Build a scene |
| `/aase equip` | Equipment menu |
| `/aase particle add <type>` | Attach particle effects |
| `/aase anim key/length/loop/play/stop/clear` | Keyframe animation |
| `/aase save` / `load <name>` / `list` / `edit` | Save / place / list / continue editing |
| `/aase share` / `import <code>` | Share codes |
| `/aase export command` / `export function` | Export a summon command or mcfunction datapack (admin/builder-only by default) |

Full command & permission reference: see the [Wiki](https://github.com/TinyYana/AwesomeArmorStandEditor/wiki).

## Permissions

Granted to every player by default: `aase.use`, `aase.create.armorstand`, `aase.create.display`, `aase.scene.save`, `aase.scene.share`, `aase.animate`.

Admin/builder-only by default (these write server files or shared data): `aase.export.command`, `aase.preset.save`, `aase.admin`, `aase.bypass.region`, `aase.bypass.limit`.

## Documentation

- [Full manual — English & 繁體中文, with diagrams](https://github.com/TinyYana/AwesomeArmorStandEditor/wiki)
- [Source code](https://github.com/TinyYana/AwesomeArmorStandEditor)

## License

**GNU AGPL-3.0.** Free to use, including on commercial servers. If you distribute a modified jar or its source to anyone — including running it as a network service others interact with — you must release that modified source under the same license. Simply running it (even for profit) never triggers this.

## FAQ

<details>
<summary>I don't want to learn angles — can I still use this?</summary>

Yes. `/aase presets` is built for exactly that: pick a pose, click, done.
</details>

<details>
<summary>Does this need Paper?</summary>

No. It only uses the Bukkit/Spigot API, so it runs on plain Spigot too.
</details>

<details>
<summary>Will this crash without other plugins installed?</summary>

No hard dependencies at all. Region-plugin integration and an optional audit-log hook activate automatically only if those plugins are present.
</details>

<details>
<summary>Can players grief each other's builds?</summary>

No — every element is ownership-tagged; only its creator can edit or remove it, and per-player/per-chunk/global caps stop spam.
</details>
