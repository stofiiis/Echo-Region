# Echo Regions

Echo Regions tracks local chunk memory (mining, combat, death) and derives a regional state
without changing biomes or worldgen. Effects and visuals are added via events only.

## Status
Current: v0.3

## Features (v0.3)
- Server-side SavedData: ChunkPos -> RegionMemory
- Mining/combat/death tracking
- Farming tracking (bone meal, planting, harvest)
- Building, fire, travel, and rare-mining tracking
- Configurable thresholds + decay mode/interval
- Score decay with pruning
- Debug command: `/echoregion here` (local + 3x3 region sums)
- Pebble + Ectoplasm items with tooltips
- Region states: SCARRED, HAUNTED, WAR_TORN, CULTIVATED, SETTLED, BLIGHTED, TRAVELLED, EXPLOITED

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
- v0.4: ambient cues + basic effects

## Backlog ideas
- Ambient cues per region state
- Small, safe gameplay effects tied to states

## Non-goals
- No biome/worldgen rewrites
- No custom dimensions
- No GUI
