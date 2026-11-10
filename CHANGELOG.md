# Changelog

All notable changes to The Sift Unbound. Versions follow `MAJOR.MINOR.PATCH`; the release workflow reads the section for each version.

## [Unreleased]

### Added
- **Sift splash texts** on the title screen (*"The Sift!"*, *"Blub!"*, *"Mind the tide!"*…). They replace Minecraft's
  while the mod is installed.

### Changed
- The repository is now **[MilkdromedaStudios/Sift-Unbound](https://github.com/MilkdromedaStudios/Sift-Unbound)** (was `The-Rift`).
  The mod's homepage, sources, issues and credits links point there.
- Built against **Fabric API 0.166.0+26.3** (still Minecraft Java 26.3 only).
- README and wiki screenshots retaken with shaders (Iris + Complementary Unbound).
- `tools/photoshoot.sh` downloads the Sodium build that Iris asks for, uses Complementary Unbound by default
  (`SHADER=<modrinth slug>` to change it) and forces the OpenGL backend that Iris needs.
- Trailer builder (`tools/make_trailer.py`): a slower, more cinematic teaser. Every shot glides the same way at one
  constant speed, with no zoom-ins or shake, and the end card only credits the music.

## [0.1.0] - 2026-11-09

First public release of **The Sift Unbound** (developed as "The Rift"; mod ID `sift_unbound`, commands `/sift credits` and `/sift tide`), for Minecraft Java **26.3** (Fabric Loader 0.20.1+, Fabric API 0.165.2+26.3, Java 25).

### Added
- **Warden Portal**: the Echo Tuner awakens any complete reinforced-deepslate frame (2×3 up to 23×23, including the 20×6 ancient
  city frame). Portals link the Overworld and the Sift 1:1 and build a return portal when needed.
- **The Sift** dimension with 5 biomes (Sift Meadows, Pallid Grove, Soul Wetlands, Husklands, Lullaby Hills), soul-water seas,
  Pallid trees, ores, lakes, springs and husk boulders.
- **Tides**: a data-driven sky/fog/light timeline (Flow, Endure, Thrive) and `/sift tide`.
- **Soul Water** fluid + bucket, and the **Soulsick** effect (drains health and XP; ignores armour).
- Mobs: **Blub** (soul / meadows / carapace, breedable, bucketable), **Soul Wisp**, **Sculk Prowler**.
- 45 blocks (Sift terrain, Soulshale family, Pallid wood set, plants, Soul Shard ore/block), 8 items, 7 advancements,
  19 sound events, original music, an ancient-city loot entry for the Echo Tuner, and `/sift credits`.
- Automated tests: 9 server GameTests, a dedicated-server smoke test and a client GameTest that travels through a portal and takes screenshots.
