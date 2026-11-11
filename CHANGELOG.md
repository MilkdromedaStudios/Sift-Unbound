# Changelog

All notable changes to The Sift Unbound. Versions follow `MAJOR.MINOR.PATCH`; the release workflow reads the section for each version.

## [Unreleased]

### Changed
- **Sift Unbound Shaders** now comes with every release: the zip of the Iris shader pack is attached to each GitHub
  release next to the mod jar, with the same version number, and each CI build uploads it as an artifact. Changing
  the pack releases a new version like any change to the mod.

## [0.3.0] - 2026-11-11

The Sift, as it really is: this release brings the mod in line with the Sift that Minecraft Dungeons II shipped
(names, mechanics and creatures from the Minecraft Wiki), still with 100% original textures and sounds.

### Added
- **The note block machine**: as in Dungeons II, eight note blocks in front of the Warden Portal play the Sift's melody
  (F♯ A C♯ B A E C♯ F♯) to wake it, by hand or by redstone. Each correct note sends up a soul. The **Echo Tuner** still
  lights a frame directly, and now hums the melody (and shows its notes) when used while sneaking.
- **Tides change gameplay**: during the **Endure Tide** wounds don't heal on their own in the Sift and its monsters spawn
  stronger; during the **Thrive Tide** wounds heal 50% faster. Players in the Sift are told when the tide turns.
- **Chiseled Soulshale depicts a singer** (as in the 26.4 snapshot), is teal on maps, and turns up in 23% of ancient city
  chests.
- Blubs **bathe in ichor**: they wander to nearby pools for a soak and shed **Blub Jelly** when they climb out, and a
  baby blub drops one when it grows up.
- **The Sift's wildlife**:
  - **Nuzzle**: the sheep-like grazer of Singer's Meadow. Shear its salmon fleece for pink wool; it grows back when the
    nuzzle grazes Sift grass. Bred with Sift grass.
  - **Slabber**: a slow, armoured, turtle-like grazer whose shell is a slab of mossy soulshale. It sleeps through the
    Endure Tide (the night outside the Sift) and looks just like the ground while it does. Bred with Teal Sprouts.
  - **Echo Golem**: the Sift's caretaker, powered by the soul block on its head. It runs down slowly and recharges by
    bathing in ichor. Hand one an Echo Shard near a dormant Warden Portal and it plays the melody and wakes it.
  - **The Singer**: a healthy warden, keeper of the Singer's Portal. It can't be hurt, sings the Sift's melody now and
    then (or when you ask), and creates echo golems.
- **The Singer's Portal**: the first return portal in the Sift is ringed with red sculk and tall grass, with the Singer
  and two echo golems standing by.
- **Soulshale monoliths**: old carved steles on polished plinths, a Chiseled Soulshale singer set into both faces, in
  Singer's Meadow and the Lullaby Hills (rarer elsewhere). Some have toppled and lie broken among rubble and creeping
  sculk.
- **Sculk formations**: mounds of red sculk, darkest at the heart, with curved horn-like spires, common in the Humbler
  Huskland and rarer in the Pallid Grove and Ichor Ponds.
- **The Sifters**, the Sift's own plant-like natives, hostile bands that roam Singer's Meadow and the Lullaby Hills by day
  and night (hurt one and its band joins in):
  - **Seedling**: small and quick; it scurries up, lunges and bites, and now and then catches an arrow and eats it.
  - **Nester**: tall, with a nest on its back; it gallops in on stilt legs, lunges and bites, and sidesteps one blow in four.
  - **Pollinator**: frog-like; it keeps its distance, puffs up its throat and spits pollen globs that burst into sticky,
    slowing yellow goo, and lashes out with its tongue up close.
  - **Sentinel**: bearded and crested; it shakes its head, then looses three homing quills that swing round toward you
    before flying straight, then stands still for a moment.
  - **Sprout**: squid-like, drifting on dangling tendrils and glowing turquoise when roused; its **Life Link** beam makes an
    ally (pollinators first) faster, stronger, tougher and quick to heal until the sprout is hit. With no one to link it
    whips with its tendrils.
  - **Harmonizer** (rare mini-boss with a boss bar): a massive sprout whose roar is a shockwave that calls a band of sifters
    it then strengthens through beams; it slaps with a tentacle and conjures a vortex that drags in anyone on the ground
    (keep jumping) before it slams down.
  - Killing a **Sprout or a Harmonizer enrages** every sifter around it for good: faster, harder-hitting, tougher, with
    glowing purple details, and out for the killer.
- **Scavenger**: the yellowish, cage-bodied sculker of the Humbler Huskland. Its hands ignite and it hurls two soul bolts
  that chase their target and burst into pools of soulsickness; get close and it dashes away backwards in a blur of golden
  afterimages.
- **Sculkers hunt sifters**: stalkers and scavengers attack any sifter they see.
- New drops: **Pollen Globs** (throw them: they stick and burst into slowing goo) and **Sentinel Quills**; spawn eggs for
  all seven; five new guide advancements, from defeating your first sifter to every kind of sifter.
- **The Monarch**, the sculker boss (boss bar). It strides slowly and every melee hit it lands heals it. Up close it
  stomps in a twirl; it roars for two stalkers and two scavengers; its dash strike leaves feathers hanging along its path,
  and its hover loop a ring of eight, that fly at you five seconds later. Once it has lost a quarter of its health its
  spectral **Monarch Echo** joins in (another forms if you kill it first; it fades when the Monarch falls). Drops echo
  shards, soul shards and **Monarch Feathers**.
- **Sculker nests** in the Ravines, under Singer's Meadow, the Humbler Huskland, the Lullaby Hills and the Sifter
  Canyons: a soulshale doorway in the hillside opens on a tunnel down to a dim cavern of dark red sculk, its floor broken
  by ichor pools and sculk spires, where the Monarch waits beside a chest on an obsidian plinth. Stalkers and scavengers
  haunt it. `/locate structure sift_unbound:sculker_nest` finds one.
- **Humbler husks**: the colossal fossil remains of humblers across the Humbler Huskland: rib cages with a spine arching
  over them and a curling tail, great half-buried carapaces broken open in places, and hollow skulls with a jaw of teeth.
- **Sifter Canyons**, a new region of Singer's Meadow cut by deep canyons, with the woven **sifter nests** the sifters live
  in, and sifters everywhere.
- Two more guide advancements: finding a sculker nest, and defeating the Monarch.
- **Guide advancements** that teach each mechanic the first time you meet it: humming and playing the melody, the note
  block machine, chiseled soulshale, ichor and soulsickness, both tides, blub jelly, shearing nuzzles, feeding slabbers,
  the Singer, echo golems (and letting one wake a portal), and visiting every region (26 in all). A new
  `sift_unbound:sift_event` trigger lets datapacks hook the same moments.

### Changed
- **Soul Water is now Ichor**, the Sift's liquid in Dungeons II: thick and slow like lava (it flows 4 blocks instead of 8),
  with bubbles popping on the surface. Existing worlds keep their ichor (the block ID is unchanged).
- Official region names: **Singer's Meadow** (was Sift Meadows), **Ichor Ponds** (was Soul Wetlands) and
  **Humbler Huskland** (was Husklands).
- **Blubs are true to the official ones**: they can't be hurt (only `/kill` and the void get through) or pushed.
- The **Sculk Prowler is now the Stalker**, the official clawed sculker: 30 health, a double claw strike, and a lunge
  from up to 10 blocks away that leaves a trail of souls. Existing worlds keep their stalkers.
- The light-the-portal advancement also counts the note block machine and echo golems.

## [0.2.0] - 2026-11-10

### Added
- **Automatic releases**: every push to `main` that changes the mod (not just docs, tools or tests) is versioned,
  tagged, released on GitHub and uploaded to Modrinth by the new **auto-release** workflow.
- **Modrinth publishing**: releases also upload the jar to [Modrinth](https://modrinth.com/project/w1FbpX4X) (needs
  the `MODRINTH_API_KEY` secret); the manual **modrinth** workflow uploads an existing release.
- **Sift title screen**: the main-menu panorama is a 360° view of the Lullaby Hills at Thrive Tide (shot in game with
  shaders), and the splash texts are Sift ones (*"The Sift!"*, *"Blub!"*, *"Mind the tide!"*…). Both replace Minecraft's
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
