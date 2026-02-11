# Echo Regions

Echo Regions tracks local region memory (8x8 chunks) and derives a regional state
without changing biomes or worldgen. Effects and visuals are added via events only.

## Status
Current: v0.4

## Features (v0.4)
- Server-side SavedData: RegionPos (8x8 chunks) -> RegionMemory
- Mining/combat/death tracking
- Farming tracking (bone meal, planting, harvest)
- Building, fire, travel, and rare-mining tracking
- Configurable thresholds + decay mode/interval
- Asymmetric decay (negative scores linger longer than positive)
- Score decay with pruning
- Debug command: `/echoregion here` (local region + 3x3 region area)
- Pebble + Ectoplasm items with tooltips
- Region states: SCARRED, HAUNTED, WAR_TORN, CULTIVATED, SETTLED, BLIGHTED, TRAVELLED, EXPLOITED
- Multi-layer history: multiple active tags with intensity levels
- Headline hysteresis for more stable state transitions

## Commands
All commands are server-side. Config/debug/decay commands require gamemaster permissions.

### Debug
- `/echoregion here`
  - Shows region position, dimension, headline state, active tags (with intensity), thresholds, local region scores, 3x3 region area scores, and last update tick.
- `/echoregion debug on`
  - Enables full debug output for `/echoregion here`.
- `/echoregion debug off`
  - Limits `/echoregion here` to basic info (chunk/dimension/state).

### Config
- `/echoregion config get <key>`
  - Prints the current value for a config key.
- `/echoregion config set <key> <value>`
  - Sets a config key at runtime and saves the config file.
- `/echoregion config reload`
  - Reloads the config from disk.
- `/echoregion config reset [key]`
  - Resets one key or all keys to defaults and saves the config file.

Valid keys:
`thresholdMining`, `thresholdCombat`, `thresholdDeath`, `thresholdFarm`,
`thresholdBuild`, `thresholdFire`, `thresholdTravel`, `thresholdExploit`,
`decayIntervalMinutes`, `decayMode`,
`negativeDecayFlatAmount`, `negativeDecayPercent`,
`positiveDecayFlatAmount`, `positiveDecayPercent`,
`headlineKeepThresholdFactor`, `headlineSwitchRatio`,
`scarredPebbleChance`, `hauntedEctoplasmChance`, `warTornStrengthChance`

Notes:
- `decayMode` accepts `FLAT` or `PERCENT`.

### Decay
- `/echoregion decay now`
  - Immediately applies decay across all loaded levels using current config.

## Testing checklist
Minimal sanity checks after changes:
1) Migration: open an old world and verify a single migration log entry appears once.
2) Region mapping: cross a region border (every 8 chunks) and confirm scores write to a new region.
3) Headline hysteresis: force a state above threshold, then reduce it below `threshold * keepFactor` and confirm the headline changes only when rules trigger.
4) Active tags: verify top 3 are shown with intensity and the remainder is summarized as `...+N`.
5) Asymmetric decay: set extreme negative/positive decay values, run `/echoregion decay now`, and confirm positive scores drop faster.

## Build and run
```bash
./gradlew runClient
./gradlew runServer
./gradlew build
```

## Tech
- Minecraft 1.21.11
- NeoForge 21.11.38-beta
- ModDevGradle MDK
- Package: `cz.stofiiis.echoregions`

## Roadmap
- v0.5: ambient cues
- v0.6: basic effects

## Backlog ideas
- Ambient cues per region state
- Small, safe gameplay effects tied to states

## Non-goals
- No biome/worldgen rewrites
- No custom dimensions
- No GUI
