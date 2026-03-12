# Runeterra — Minecraft NeoForge Mod (1.21.11)

A Minecraft NeoForge mod that adds League of Legends-inspired content, starting with the **Elder Drake** — a massive boss mob that shoots exploding fireballs.

---

## Features

- **Elder Drake** — a large custom mob (8x scale) that spawns in mountain biomes during thunderstorms
- **Elder Drake Fireball** — a projectile with flame/smoke/lava particle trail that creates a TNT-style explosion on impact
- **Elder Drake Spawn Egg** — available in a custom Runeterra creative tab
- **Custom loot table** — drops on death
- **Biome modifier** — spawns naturally in mountain biomes
- **GeckoLib animations** — custom model, texture, and animations

---

## Project Structure

```
src/main/java/com/eilai/runeterra/
├── Runeterra.java                        # Mod entry point
├── RuneterraClient.java                  # Client-side setup (renderers)
├── Config.java                           # Mod config
├── entity/
│   ├── ElderDrakeEntity.java             # Elder Drake mob logic
│   ├── ElderDrakeFireball.java           # Fireball projectile
│   └── client/
│       ├── ElderDrakeModel.java          # GeckoLib model
│       ├── ElderDrakeRenderer.java       # GeckoLib renderer
│       └── ElderDrakeFireballRenderer.java
└── init/
    ├── ModEntities.java                  # Entity type registration + spawn rules
    └── ModItems.java                     # Spawn egg + creative tab

src/main/resources/assets/runeterra/
├── geckolib/
│   ├── animations/elder_drake.animation.json
│   └── models/elder_drake.geo.json
├── models/item/elder_drake_spawn_egg.json
├── neoforge.spawn_egg_colors/elder_drake_spawn_egg.json
└── textures/entity/elder_drake.png

src/main/resources/data/runeterra/
├── loot_tables/entities/elder_drake.json
└── neoforge.biome_modifiers/elder_drake_spawns.json
```

---

## Elder Drake Stats

| Property | Value |
|----------|-------|
| Hitbox | 2.5 × 2.0 blocks |
| Visual scale | 8× |
| Shadow radius | 2.5 |
| Spawn biome | `#minecraft:is_mountain` |
| Fireball lifetime | 5 seconds (100 ticks) |
| Explosion radius | 3.5 (TNT-style, sets fire) |
| Tracking range | 12 chunks |

---

## Setup & Building

### Prerequisites
- Java 21
- IntelliJ IDEA or Eclipse
- NeoForge MDK 1.21.11
- GeckoLib 5

### Getting Started

1. Clone the repository
2. Open in IntelliJ IDEA or Eclipse
3. Run Gradle sync:
   ```bash
   ./gradlew --refresh-dependencies
   ```
4. Run the game:
   ```bash
   ./gradlew runClient
   ```
5. Build the mod jar:
   ```bash
   ./gradlew build
   ```

If you run into issues, reset with:
```bash
./gradlew clean
./gradlew --refresh-dependencies
```

---

## Dependencies

- **NeoForge** 1.21.11
- **GeckoLib 5** — for animated entity model/renderer

---

## Mapping Names

This mod uses the official Mojang mapping names for Minecraft methods and fields.
See the license: https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

---

## Additional Resources

- NeoForge Docs: https://docs.neoforged.net/
- NeoForge Discord: https://discord.neoforged.net/
- GeckoLib Wiki: https://wiki.geckolib.com/
