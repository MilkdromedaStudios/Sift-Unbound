# Soul Water

[Home](Home.md) · [The Sift (research)](The-Sift-Research.md) · [Warden Portal](Warden-Portal.md) · [Biomes & Tides](Biomes-and-Tides.md) · [Terrain](Terrain-Blocks.md) · [Soulshale](Soulshale.md) · [Plants](Plants.md) · [Pallid Wood](Pallid-Wood.md) · [Soul Water](Soul-Water.md) · [Mobs](Mobs.md) · [Items](Items.md) · [Advancements](Advancements.md) · [Sounds](Sounds.md) · [Art pipeline](Art-Pipeline.md) · [Credits](Credits.md)

<img src="../src/main/resources/assets/the_rift/textures/block/soul_water_still.png" width="64" alt="Soul water texture (animated strip)">

The Sift's liquid: a thick, shimmering, **oil-on-water** rainbow of teal, blue, pink, gold and green, with sparks of souls
rising from it. It's the mod's take on *Dungeons II*'s **ichor**, which devs nicknamed "ouch water".

| Property | Value |
|---|---|
| IDs | `the_rift:soul_water` (source), `the_rift:flowing_soul_water`, block `the_rift:soul_water`, fluid tag `#the_rift:soul_water` |
| Flow | Like water: 4-block slope search, drops 1 level per block, but slower (ticks every 10 game ticks) |
| Renewable? | **No** – two sources don't make a new one |
| Light | 5 |
| Swimming | Swims like water (Fabric fluid behaviour `WATER_LIKE`); you can drown in it; it puts out fire |
| Bucket | Soul Water Bucket; also used to scoop up blubs |
| Where | Seas below Y 52, lakes, and springs in every Sift biome |

## Soulsick

Touching soul water gives Overworld creatures **Soulsick** for 5 seconds, refreshed while you stay in it.

| | |
|---|---|
| Effect ID | `the_rift:soulsick` (harmful, teal) |
| Damage | 1 ♥/2 every 25 ticks at level I (twice as fast at II…), **ignores armour, no knock-back**, can kill |
| Extra | Players also lose 1 XP point per hit – it drains your soul |
| Death message | "*Player* had their soul drained away" |
| Immune | Blubs, Soul Wisps, Sculk Prowlers and the Warden (tag `#the_rift:soul_water_immune`) |
| Also caused by | Sculk Prowler bites (4 s) |
| Creative / spectator | not affected |
| Cure | Milk, or wait it out |
