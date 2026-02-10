# Echo Regions

Echo Regions tracks local chunk memory (mining, combat, death) and derives a regional state
without changing biomes or worldgen. Effects and visuals are added via events only.

## Status
Current: v0.1 (MVP)

## Features (v0.1)
- Server-side SavedData: ChunkPos -> RegionMemory
- Mining/combat/death tracking
- Score decay every 5 minutes with pruning
- Debug command: `/echoregion here`

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
- v0.2: config + Pebble/Ectoplasm + tooltips
- v0.3: region radius + ambient cues + basic effects

## Non-goals
- No biome/worldgen rewrites
- No custom dimensions
- No GUI
