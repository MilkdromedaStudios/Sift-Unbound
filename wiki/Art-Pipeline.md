# Art pipeline – how the textures were made

[Home](Home.md) · [The Sift (research)](The-Sift-Research.md) · [Warden Portal](Warden-Portal.md) · [Biomes & Tides](Biomes-and-Tides.md) · [Terrain](Terrain-Blocks.md) · [Soulshale](Soulshale.md) · [Plants](Plants.md) · [Pallid Wood](Pallid-Wood.md) · [Soul Water](Soul-Water.md) · [Mobs](Mobs.md) · [Items](Items.md) · [Advancements](Advancements.md) · [Sounds](Sounds.md) · [Art pipeline](Art-Pipeline.md) · [Credits](Credits.md)

The request for this mod was to "extract the textures from everything we can see". Mojang's art can't legally be copied into a
public, Apache-licensed mod, and screenshots can't be turned into clean 16×16 textures anyway. So the pipeline extracts **colours
and observations** from the reference pictures, then **draws new textures** from them:

1. **Reference collection (private).** 28 reference images and 16 official blub sound clips were downloaded from the Minecraft
   Wiki through its API, into a private folder and a ZIP for the project owner. They are **not** in this repository (`.gitignore`
   blocks `/references/`). The list is on [The Sift – research](The-Sift-Research.md#reference-photos-10-of-the-land-trees-grass-water-and-creatures).
2. **`tools/texture_pipeline/extract_palette.py`.** Crops the regions listed in `regions.json` (vanilla grass, sky, blub renders,
   ichor pools, pale tree canopies, red sculk ground, huskland sand, portal glow…). Runs deterministic k-means colour clustering and
   writes **`palettes.json`** (plain hex colours, committed). It also writes 16×16 / 64×64 "observed swatches" and a contact sheet
   to the private folder, for side-by-side comparison only.
3. **`tools/texture_pipeline/gen_textures.py`.** Draws every block, item, entity, fluid and effect texture **procedurally**: seeded
   value noise, hand-placed pixel patterns, box-UV painting for entity models. It only uses the palette colours. Animated textures
   (soul water, portal) get `.mcmeta` files.
4. **`tools/gen_sounds.py`.** Synthesizes all sounds and music (numpy → WAV → Ogg Vorbis via ffmpeg).
5. **`tools/gen_assets.py`.** Writes blockstates, models, item definitions, loot tables, recipes, tags, advancements and all
   worldgen. It clones vanilla *formats* (cherry → Pallid wood set, deepslate family → Soulshale family) from an extracted 26.3 jar
   and rewrites every ID and texture to the mod's own.
6. **`tools/gen_wiki.py`.** Builds the block/item pages of this wiki from the data above.

| Palette source | Colours used for |
|---|---|
| `sift_grass_vanilla`, `sift_ground_vanilla` | Sift grass block, short/tall Sift grass, mod icon |
| `sift_sky_vanilla` | dimension sky colour (`#60d5c8`) |
| `red_terrain`, `lullaby_rock` | Sift Sculk |
| `teal_foliage` | Teal Sprouts, flower stems |
| `pale_tree_leaves` | Pallid leaves, sapling, bloom petals |
| `huskland_sand` | Sift Sand |
| `ichor`, `ichor_pond` | Soul Water rainbow |
| `portal_glow` | Sift portal, Soul Shards, Soulsick icon |
| `blub_*` | the three blub looks |
| `endure_tide_night` | Endure Tide sky, Sculk Prowler |
