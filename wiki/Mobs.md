# Mobs

[Home](Home.md) · [The Sift (research)](The-Sift-Research.md) · [Warden Portal](Warden-Portal.md) · [Biomes & Tides](Biomes-and-Tides.md) · [Terrain](Terrain-Blocks.md) · [Soulshale](Soulshale.md) · [Plants](Plants.md) · [Pallid Wood](Pallid-Wood.md) · [Soul Water](Soul-Water.md) · [Mobs](Mobs.md) · [Items](Items.md) · [Advancements](Advancements.md) · [Sounds](Sounds.md) · [Art pipeline](Art-Pipeline.md) · [Credits](Credits.md)

![Blubs, a Sculk Prowler and a Soul Wisp](screenshots/sift_creatures.jpg)

## Blub

The Sift's signature creature, announced by Mojang for Minecraft in 2027: a small, squishy, rabbit-like jelly cube that hops around
and **squeaks every few seconds**. This model, its textures and sounds are original.

| Property | Value |
|---|---|
| ID | `sift_unbound:blub` (category: creature) |
| Health | 6 (3 ♥) |
| Size | 0.55 × 0.55 |
| Looks | **Soul-engorged** (glowing light blue, default), **Meadows** (mossy green with an antenna light), **Carapace** (beige shell with red frills – spawns on sand / dark sculk) |
| Spawns | On Sift ground or in Soul Water, groups of 1–6, any light level |
| Behaviour | Wanders and hops, panics when hurt, follows players holding **Lullaby Blooms** or **Teal Sprouts**, babies follow parents |
| Breeding | Lullaby Bloom or Teal Sprouts; babies take a parent's look |
| Soul Water | Immune; swims and never drowns |
| Bucket | Use a **Soul Water Bucket** on it to get a **Bucket of Blub** (keeps look, name, health, age) |
| Drops | 0–1 **Blub Jelly** (+Looting) |

## Soul Wisp

A harmless little soul that drifts out of soul water and ravines. Ambient like a bat: it floats about 1.5–9 blocks above the
ground, chimes softly, glows and leaves a trail of soul particles.

| Property | Value |
|---|---|
| ID | `sift_unbound:soul_wisp` (category: ambient) |
| Health | 4 |
| Flying | yes (no gravity, no fall damage) |
| Spawns | Every Sift biome, most in Soul Wetlands |
| Drops | nothing |

## Sculk Prowler

A low, four-legged sculk hunter with twitching sensor tendrils and glowing cyan veins. It hunts players and its bite makes them Soulsick.

| Property | Value |
|---|---|
| ID | `sift_unbound:sculk_prowler` (category: monster) |
| Health / Armour | 24 / 2 |
| Attack | 5 damage + Soulsick 4 s |
| Speed | 0.28 (chases at ×1.2) |
| Spawns | In the dark – anywhere monsters can spawn, plus on Sift ground below light 4. The **Endure Tide** dims the sky enough for them to come out on the surface. |
| Drops | 0–2 Soul Shards (+Looting), 8 % chance of an **echo shard** when killed by a player, 8 XP |
| Soul Water | Immune |
