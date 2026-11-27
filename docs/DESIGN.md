# Vanilla Extras design rules

## Goal

Vanilla Extras adds features to Minecraft without breaking vanilla clients. A server running the mod must accept unmodded (vanilla) clients, and they must be able to join and play normally. Players who also install the mod on their client may get extra interaction or polish, but nothing may *require* the client mod.

One jar, `vanilla-extras-<version>.jar`, goes in either the server's or the client's `mods/` folder.

## Why one jar works

- `fabric.mod.json` declares `"environment": "*"`, so Fabric Loader loads the mod on both sides.
- Loom's `splitEnvironmentSourceSets()` keeps client-only code in `src/client`. Both source sets are packed into the same jar, but the `client` entrypoint and the client mixin config (`"environment": "client"`) only load on a client. A dedicated server never touches client classes, so nothing crashes there.

## Rules for features

A vanilla client only knows vanilla registries, packets, and assets. Anything the server sends must already be understood by a vanilla client.

| Do | Don't |
|---|---|
| Use vanilla blocks, items, entities, sounds, and particles | Register new blocks, items, entities, block entities, enchantments, or other synced registry entries |
| Use data components on vanilla items (custom name, lore, item model, custom data) to make "custom" items | Add new item or block types (Fabric registry sync kicks vanilla clients) |
| Use datapack-style content the server can define (recipes, loot tables, advancements, tags, functions) | Rely on a resource pack or custom textures being present (offer one as optional only) |
| Run game logic server-side with mixins/events under `src/main` | Send custom payload packets to a client that hasn't announced the mod |
| Use vanilla commands, chat, titles, boss bars, scoreboards, and display entities for UI | Open custom screens or GUIs on a vanilla client |
| Put client-only polish under `src/client`, gated on the server having the mod | Make the server depend on anything in `src/client` |

## Every feature has a config toggle

Each feature must be switchable on and off in `config/vanilla-extras.json`. The file is created on first launch and lists every feature under `"features"` with its current value:

```json
{
  "features": {
    "saturationBeacon": true,
    "pickaxeTorches": true
  }
}
```

Declare each feature once in `ModFeatures` (package `com.kevin2holt.vanillaextras.feature`) and check it with `ModFeatures.<FEATURE>.isEnabled()`. The config loads lazily, so it is safe to call from mixins that run during class initialization. Missing keys are written back with their default; a file that fails to parse is left untouched and defaults are used.

Mark a feature `isRestartRequired` (4th `Feature` argument) when it is wired up once at startup, like the beacon effect list. Its `isEnabled()` keeps the value it had on first read, so the game never runs half-switched; `isEnabledInConfig()` returns the saved value. Other features are live: check `isEnabled()` at the point of use and a change takes effect immediately. Hand-editing the file while the game runs is not picked up; use the in-game menu or restart.

Feature options (sliders, extra switches) go under `"settings"`, keyed by feature, through `IntSetting` and `BooleanSetting` in `com.kevin2holt.vanillaextras.config`. Their `get()` reads memory, so it is cheap enough to call every tick.

```json
{
  "features": { "saturationBeacon": true, "pickaxeTorches": true },
  "settings": {
    "settingsMenu": { "showOptionsScreenButton": false }
  }
}
```

## In-game settings menu

Client-only, under `src/client/.../client/settings`. It opens from Mod Menu's configure button (optional dependency) or, when `settings.settingsMenu.showOptionsScreenButton` is `true`, from an "Extras" button on the vanilla Options screen. The root screen lists "Feature Toggles" first, split into features that apply right away and features that need a restart, then one button per registered page.

A feature with options registers its page once from `onInitializeClient`:

```java
SettingsMenu.registerPage(Component.literal("Auto-Refill"), page -> {
	page.addSlider(AutoRefillSettings.TRIGGER_COUNT, Component.literal("Refill below"));
	page.addToggle(AutoRefillSettings.FROM_CONTAINERS, Component.literal("Use containers"));
});
```

Feature names on the toggles screen come from the lang key `vanilla-extras.feature.<configKey>`, falling back to the config key split into words. Toggling a feature from the menu saves the file and, for client-synced features, re-sends the enabled list to modded players on the running integrated server. On a remote server the toggles still edit the client's own file, and the screen says so.

The server's config decides. When a feature changes what a modded client shows (like a new beacon button), mark it `isSyncedToClient` in `ModFeatures`; the server sends its enabled synced features to modded clients on join, and the client only shows the extra option when the server has it on. A modded client's own toggle only matters in singleplayer and LAN hosting.

## Optional client features

When both sides have the mod, they can exchange custom packets. Use Fabric Networking API payloads and check `ServerPlayNetworking.canSend(player, TYPE)` before sending anything custom, so a vanilla client never receives a packet it can't decode.

## Testing checklist

- Dedicated server with the mod, plus a vanilla 26.3 client: joins, plays, and uses every feature without errors or disconnects.
- Same server with a modded client: optional extras work.
- Singleplayer with the mod: works (the integrated server plus a modded client).
