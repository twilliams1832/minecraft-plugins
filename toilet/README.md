# ToiletPlugin

`ToiletPlugin` is a Paper plugin that gives players a custom toilet item, lets them place a usable toilet in the world, and supports custom visuals for:

- Java Edition clients through a Java resource pack
- Bedrock clients through Geyser custom block mappings plus a Bedrock resource pack

## Current Behavior

- `/placetoilet` gives the player a custom toilet item
- right-clicking a block with that item places a toilet on top of the clicked block
- right-clicking the bowl with an empty hand sits down
- right-clicking the bowl while seated flushes
- sneak + right-click while seated stands up
- right-clicking the seat toggles it up/down

## Important Design Note

The placed toilet does not use visible vanilla `quartz_stairs` and `iron_trapdoor` anymore.

Instead, it uses reserved placeholder blocks internally so Java and Bedrock can each render toilet-specific visuals without globally reskinning ordinary vanilla blocks:

- bowl: `minecraft:jigsaw[orientation=up_north]`
- seat down: `minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]`
- seat up: `minecraft:lightning_rod[facing=up,powered=true,waterlogged=false]`

That placeholder design is the key to the cross-platform setup.

## Project Layout

```text
.
├── geyser
│   ├── custom_mappings
│   │   └── toilet_blocks.json
│   └── packs
│       └── toilet-bedrock-pack
├── pom.xml
├── build.gradle
├── resource-pack
│   ├── pack.mcmeta
│   └── assets
├── src
│   └── main
│       ├── java
│       └── resources
└── target
    ├── ToiletPlugin-1.1.0.jar
    ├── toilet-resource-pack.zip
    └── toilet-bedrock-pack.zip
```

## Build

Prerequisites:

- Java 17+
- Maven 3.8+ or Gradle 8+

### Maven

```bash
mvn clean package
```

Artifacts:

- plugin jar: [target/ToiletPlugin-1.1.0.jar](/home/tim/minecraft-plugins/toilet/target/ToiletPlugin-1.1.0.jar)
- Java resource pack: [target/toilet-resource-pack.zip](/home/tim/minecraft-plugins/toilet/target/toilet-resource-pack.zip)
- Bedrock resource pack: [target/toilet-bedrock-pack.zip](/home/tim/minecraft-plugins/toilet/target/toilet-bedrock-pack.zip)

### Gradle

```bash
gradle build
```

## Install On Paper

1. Build the plugin.
2. Copy [target/ToiletPlugin-1.1.0.jar](/home/tim/minecraft-plugins/toilet/target/ToiletPlugin-1.1.0.jar) into your Paper server `plugins/` directory.
3. Restart the server.

## Java Client Resource Pack

The plugin tags the toilet item with `custom_model_data: 1001` and also relies on Java resource-pack block overrides so the placed placeholder blocks render like a toilet.

Use the prebuilt Java pack:

- [target/toilet-resource-pack.zip](/home/tim/minecraft-plugins/toilet/target/toilet-resource-pack.zip)

### Server setup

Host the zip at an HTTPS URL and point `server.properties` at it:

```properties
resource-pack=https://your-domain/path/to/toilet-resource-pack.zip
require-resource-pack=false
resource-pack-prompt=ToiletPlugin custom visuals
```

If you later add more models, keep extending this same Java pack and replace the hosted zip. Do not try to list multiple Java resource packs in `server.properties`.

## Geyser / Bedrock Setup

This repo includes the two pieces Geyser needs:

1. a custom block mappings file
2. a Bedrock resource pack

### Files

Custom mappings file:

- [geyser/custom_mappings/toilet_blocks.json](/home/tim/minecraft-plugins/toilet/geyser/custom_mappings/toilet_blocks.json:1)

Bedrock pack archive:

- [target/toilet-bedrock-pack.zip](/home/tim/minecraft-plugins/toilet/target/toilet-bedrock-pack.zip)

### Where they go

On a Paper server with Geyser installed as a plugin, the folders are typically under:

```text
/data/plugins/Geyser-Spigot/
```

or similar, such as `Geyser-Paper`.

Put the files here:

- `toilet_blocks.json` -> `plugins/Geyser-*/custom_mappings/`
- `toilet-bedrock-pack.zip` -> `plugins/Geyser-*/packs/`

### Required Geyser config

In Geyser `config.yml`, make sure:

```yml
enable-custom-content: true
```

Then fully restart the server.

## Quick Deploy Checklist

For a full cross-platform deploy:

1. Build:
   ```bash
   mvn clean package
   ```
2. Copy plugin jar to Paper:
   - [target/ToiletPlugin-1.1.0.jar](/home/tim/minecraft-plugins/toilet/target/ToiletPlugin-1.1.0.jar)
3. Host the Java resource pack:
   - [target/toilet-resource-pack.zip](/home/tim/minecraft-plugins/toilet/target/toilet-resource-pack.zip)
4. Update `server.properties` with the Java resource-pack URL
5. Copy Geyser custom mappings:
   - [geyser/custom_mappings/toilet_blocks.json](/home/tim/minecraft-plugins/toilet/geyser/custom_mappings/toilet_blocks.json:1)
6. Copy Geyser Bedrock pack:
   - [target/toilet-bedrock-pack.zip](/home/tim/minecraft-plugins/toilet/target/toilet-bedrock-pack.zip)
7. Confirm `enable-custom-content: true`
8. Restart Paper/Geyser fully

## Commands And Permissions

| Command | Permission | Description |
|---|---|---|
| `/placetoilet` | `toiletplugin.place` | Gives the player a custom toilet item |

Current permission default:

- `toiletplugin.place`: `default: true`

That means normal players can use `/placetoilet` without being op.

## In-Game Flow

1. Run `/placetoilet`
2. Receive a custom toilet item
3. Right-click a block with the item to place the toilet
4. Right-click the bowl with an empty hand to sit
5. Right-click again while seated to flush
6. Sneak + right-click while seated to stand up
7. Right-click the seat to toggle it

## Troubleshooting

### `/placetoilet` does not appear

Check:

- the latest jar was copied to the server
- the plugin is enabled
- `/plugins` shows `ToiletPlugin`

### Java clients do not see the custom model

Check:

- the resource-pack URL is reachable over HTTPS
- the zip has `pack.mcmeta` at the top level
- the client accepted the pack
- the server was restarted after updating `server.properties`

### Bedrock clients do not see the custom toilet

Check:

- `enable-custom-content: true` in Geyser
- `toilet_blocks.json` is in `custom_mappings`
- `toilet-bedrock-pack.zip` is in `packs`
- Geyser was fully restarted
- the Bedrock client reconnected after restart

### The server says the pack is available but Bedrock does not prompt

That is expected for the Java `server.properties` resource pack. Bedrock clients do not use the Java pack. Bedrock visuals come from Geyser’s `packs` folder instead.

## Notes

- Versioning follows semantic versioning:
  - patch for fixes only
  - minor for backward-compatible features
  - major for breaking changes
- The current Java and Bedrock visuals are proof-of-concept scaffolding, not polished final art.
- The logic and packaging path are in place so future custom textures/models can be dropped in without redesigning the whole flow.
- Geyser custom block support can be sensitive to Bedrock/Geyser version changes, so if a future upgrade breaks visuals, check Geyser’s custom block documentation first.

## References

- Geyser custom blocks: https://geysermc.org/wiki/geyser/custom-blocks/
- Geyser packs: https://geysermc.org/wiki/geyser/packs/
- Geyser extensions: https://geysermc.org/wiki/geyser/extensions/
