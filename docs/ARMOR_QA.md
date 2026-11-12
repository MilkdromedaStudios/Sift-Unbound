# Sift Unbound — Armor Release QA (v0.4.x)

These checks catch issues that compiling the mod alone cannot catch. Test in a **backup world** before publishing a build.

## Automatic coverage

- `python3 tools/check_assets.py` validates all 12 recipes, item sprites, equipment layers, armor atlas dimensions and catches a duplicated leggings/baby layer.
- `./gradlew runGameTest` checks the 12 items' equippable slots, singleton stack limits, durability, armor attribute components and repair component.
- `./gradlew runClientGameTest` still covers the Sift portal, dimension loading, worldgen and screenshots. It does **not** guarantee armor textures look correct.

## Manual in-game verification (required)

1. Install **Minecraft Java 26.3**, matching Fabric Loader and Fabric API.
2. Open the **The Sift Unbound** creative tab. Confirm all twelve armor icons appear and are recognizable, not checkerboard or tiny specks.
3. Equip each full set (Sifter, Humbler, Monarch). Rotate a player or armor stand and check front/back, arms, leggings and boots. Check a baby zombie if available.
4. In Survival, try each of the four armor crafting shapes. Chestplates additionally use a Soul Shard (Sifter/Humbler) or Echo Shard (Monarch).
5. Damage an armor piece, then use its repair ingredient in an anvil: Teal Sprouts (Sifter), Husk Shell (Humbler), Monarch Feather (Monarch). Make sure durability increases.
6. Confirm defense points total **12**, **16** and **20**, and that enchanted armor still equips.
7. Open a **copy of an existing v0.3.x world** and verify your previous items/blocks, mobs, portal and Sift structures remain available.
8. Run in singleplayer and on a dedicated multiplayer server. Look at `logs/latest.log` for missing textures, datapack/recipe load errors and exceptions.

If something fails, please include: the Minecraft version, mod jar version, the `logs/latest.log` error lines, and a screenshot if it is visual.

**Known scope:** Armor ability effects from *Minecraft Dungeons II* are *not* implemented. This is an original survival adaptation; Mojang has not confirmed these armor sets for 2027 Java/Bedrock.
