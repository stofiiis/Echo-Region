# Echo Regions

Echo Regions tracks region memory (8x8 chunks) and derives a regional state
without changing biomes or worldgen. Effects and visuals are added via events only.

## Status
Current: v0.6

## Features (v0.6)
- Server-side SavedData: RegionPos (8x8 chunks) -> RegionMemory
- Mining/combat/death tracking
- Farming tracking (bone meal, planting, harvest)
- Building, fire, travel, and rare-mining tracking
- Configurable thresholds + decay mode/interval
- Asymmetric decay (negative scores linger longer than positive)
- Residual memory (historical maxima + decay floors)
- Stable headline state (switch ratio + keep factor + min duration)
- Multi-layer history: multiple active tags with intensity levels
- Debug command: `/echoregion here` (local region + 3x3 region area)
- Debug HUD + region map overlay
- Pebble + Ectoplasm items with tooltips
- Region states: SCARRED, HAUNTED, WAR_TORN, CULTIVATED, SETTLED, BLIGHTED, TRAVELLED, EXPLOITED
- Ambient cues (particles/sounds) for SCARRED/HAUNTED/WAR_TORN

## Commands
All commands are server-side. Config/debug/decay commands require gamemaster permissions.

### Debug
- `/echoregion here`
  - Debug ON: headline with reason, top tags (current/max/floor), thresholds, residual config, local region scores, 3x3 region area scores, and last update tick.
  - Debug OFF: region + headline + intensity only.
- `/echoregion debug on`
  - Enables full debug output for `/echoregion here`.
- `/echoregion debug off`
  - Limits `/echoregion here` to basic info.

### HUD / Map
- `/echoregion hud on`
  - Enables the debug HUD overlay.
- `/echoregion hud off`
  - Disables the debug HUD overlay.
- `/echoregion map <radius>`
  - Opens a client-side region map overlay. ESC closes.

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
`headlineKeepThresholdFactor`, `headlineSwitchRatio`, `headlineMinDurationMinutes`,
`residualEnabled`, `residualNegativePercent`, `residualPositivePercent`, `residualMinFloor`, `residualAffectsHeadline`,
`scarredPebbleChance`, `hauntedEctoplasmChance`, `warTornStrengthChance`,
`ambientEnabled`, `ambientCheckIntervalTicks`, `ambientCooldownTicks`,
`ambientSoundCooldownTicks`, `ambientParticleBurstCap`,
`ambientEntryEnabled`, `ambientEntryChanceMultiplier`, `ambientEntryParticleMultiplier`, `ambientEntrySoundMultiplier`,
`scarredAmbientChanceLow`, `scarredAmbientChanceMed`, `scarredAmbientChanceHigh`,
`scarredAmbientParticlesLow`, `scarredAmbientParticlesMed`, `scarredAmbientParticlesHigh`,
`scarredSoundChanceLow`, `scarredSoundChanceMed`, `scarredSoundChanceHigh`,
`scarredSoundVolume`, `scarredSoundPitch`,
`scarredMiningChanceLow`, `scarredMiningChanceMed`, `scarredMiningChanceHigh`,
`scarredMiningParticlesLow`, `scarredMiningParticlesMed`, `scarredMiningParticlesHigh`,
`hauntedAmbientChanceLow`, `hauntedAmbientChanceMed`, `hauntedAmbientChanceHigh`,
`hauntedAmbientParticlesLow`, `hauntedAmbientParticlesMed`, `hauntedAmbientParticlesHigh`,
`hauntedSoundChanceLow`, `hauntedSoundChanceMed`, `hauntedSoundChanceHigh`,
`hauntedSoundVolume`, `hauntedSoundPitch`,
`warTornAmbientChanceLow`, `warTornAmbientChanceMed`, `warTornAmbientChanceHigh`,
`warTornAmbientParticlesLow`, `warTornAmbientParticlesMed`, `warTornAmbientParticlesHigh`,
`warTornSoundChanceLow`, `warTornSoundChanceMed`, `warTornSoundChanceHigh`,
`warTornSoundVolume`, `warTornSoundPitch`,
`warTornCombatChanceLow`, `warTornCombatChanceMed`, `warTornCombatChanceHigh`,
`warTornCombatParticlesLow`, `warTornCombatParticlesMed`, `warTornCombatParticlesHigh`

Notes:
- `decayMode` accepts `FLAT` or `PERCENT`.

### Decay
- `/echoregion decay now`
  - Immediately applies decay across all loaded levels using current config.

## Residual Memory
- Region remembers historical maxima.
- Decay never goes below a configurable floor.
- Negative traces persist longer than positive.
- World calms down but does not fully forget.

## Debug HUD
- `/echoregion hud on/off` shows a compact overlay.
- `/echoregion map <radius>` shows a 2D region grid (ESC to close).

## Testing checklist
Minimal sanity checks after changes:
1) Migration: open an old world and verify a single migration log entry appears once.
2) Region mapping: cross a region border (every 8 chunks) and confirm scores write to a new region.
3) Residual floors: raise a score, apply decay, and verify it never drops below floor.
4) Headline stability: verify keep/switch logic + min duration before headline changes.
5) HUD overlay: enable HUD and confirm updates every ~20 ticks.
6) Map overlay: open map and verify center region and labels are correct.
7) Ambient cues: in SCARRED/HAUNTED/WAR_TORN regions, verify particles/sounds are noticeable (night for HAUNTED).
8) Anti-spam: verify repeated ambient sounds respect cooldown and particle bursts stay capped.

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
- v0.7: gameplay effects

## Backlog ideas
- More ambient cues per region state
- Small, safe gameplay effects tied to states

## Non-goals
- No biome/worldgen rewrites
- No custom dimensions
- No gameplay GUI (debug overlays only)
