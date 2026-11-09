# Sounds

[Home](Home.md) · [The Sift (research)](The-Sift-Research.md) · [Warden Portal](Warden-Portal.md) · [Biomes & Tides](Biomes-and-Tides.md) · [Terrain](Terrain-Blocks.md) · [Soulshale](Soulshale.md) · [Plants](Plants.md) · [Pallid Wood](Pallid-Wood.md) · [Soul Water](Soul-Water.md) · [Mobs](Mobs.md) · [Items](Items.md) · [Advancements](Advancements.md) · [Sounds](Sounds.md) · [Art pipeline](Art-Pipeline.md) · [Credits](Credits.md)

Every sound in the mod is **synthesized from scratch** by `tools/gen_sounds.py` (sine chirps, bells, filtered noise). No game or
wiki audio is used. To hear the *official* blub, see [The Sift – research](The-Sift-Research.md#where-to-hear-the-creatures-sounds).

| Event | Files | What it sounds like |
|---|---|---|
| `the_rift:entity.blub.ambient` | squeak1-3 | jelly squeaks with a wobble, every ~5 s |
| `the_rift:entity.blub.hurt` / `.death` | hurt1-2, death | sharp falling squeaks |
| `the_rift:entity.blub.jump` | hop1-2 | soft rubbery boing |
| `the_rift:entity.soul_wisp.*` | chime1-2, hurt, death | glassy two-note bells |
| `the_rift:entity.sculk_prowler.*` | click1-2, hurt, death | sculk clicks over a low growl |
| `the_rift:block.soul_water.ambient` | bubble1-2 | thick bubbles |
| `the_rift:block.soul_water.burn` | burn | soul hiss when you get Soulsick |
| `the_rift:block.sift_portal.ambient` | hum | low layered hum |
| `the_rift:block.sift_portal.travel` | travel | rising sweep |
| `the_rift:block.sift_portal.activate` | activate | the "melody": a rising C-major bell arpeggio over a swell |
| `the_rift:ambient.sift.loop` / `.additions` / `.mood` | loop, addition1-3, mood | the dimension's drone, distant bells and deep sculk clicks |
| `the_rift:music.sift` | sift_lullaby | "Sift Lullaby", a 64-second pentatonic bell piece over soft pads |
