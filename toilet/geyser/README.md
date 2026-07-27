# Geyser Toilet Pack

## Custom mappings

Copy [custom_mappings/toilet_blocks.json](/home/tim/minecraft-plugins/toilet/geyser/custom_mappings/toilet_blocks.json:1)
into Geyser's `custom_mappings` directory.

## Bedrock resource pack

Zip the contents of [packs/toilet-bedrock-pack](/home/tim/minecraft-plugins/toilet/geyser/packs/toilet-bedrock-pack/manifest.json:1)
or use the prebuilt archive in `target` after packaging.

Put that archive into Geyser's `packs` directory.

## Geyser config

Ensure this is enabled in Geyser's `config.yml`:

```yml
enable-custom-content: true
```

Then restart Geyser fully.
