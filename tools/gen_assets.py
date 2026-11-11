#!/usr/bin/env python3
"""Generates the JSON assets and data for The Sift Unbound.

Run from the repository root:
    python3 tools/gen_assets.py --vanilla <dir with extracted 26.3 assets/ and data/>

The vanilla folder is only read to clone *formats* (blockstates, models,
loot tables and recipes) for the block families that mirror vanilla ones
(the Pallid wood set is cloned from cherry, the Soulshale stone sets from the
deepslate family); every texture and name is The Sift Unbound's own. If you don't
have an extracted copy, `./gradlew genSources` plus unzipping the merged
Minecraft jar from .gradle/loom-cache gives you one.

Everything else (worldgen, dimension, biomes, tags, lang, advancements...)
is described directly in this file so it can be reviewed in one place.
"""
import argparse
import json
import os
import re
import shutil

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
NS = "sift_unbound"
A = os.path.join(RES, "assets", NS)
D = os.path.join(RES, "data", NS)
DM = os.path.join(RES, "data", "minecraft")

WRITTEN = []


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")
    WRITTEN.append(path)


def rid(path):
    return f"{NS}:{path}"


# ============================================================================ template cloning
class Cloner:
    def __init__(self, vanilla, src_kw, dst_kw, names):
        """names: list of vanilla block ids (e.g. 'cherry_log') to clone."""
        self.v = vanilla
        self.src, self.dst = src_kw, dst_kw
        self.names = names
        self.done_models = set()

    def rewrite(self, text):
        kw = self.src
        # namespaced ids & tags: minecraft:xxx_cherry_yyy  -> sift_unbound:xxx_pallid_yyy
        text = re.sub(r'(#?)minecraft:([a-z0-9_/]*%s[a-z0-9_/]*)' % kw,
                      lambda m: f"{m.group(1)}{NS}:{m.group(2).replace(kw, self.dst)}", text)
        # un-namespaced model / texture refs: "block/cherry_log"
        text = re.sub(r'"((?:block|item)/[a-z0-9_]*%s[a-z0-9_]*)"' % kw,
                      lambda m: f'"{NS}:{m.group(1).replace(kw, self.dst)}"', text)
        return text

    def model_refs(self, text):
        kw = self.src
        refs = set(re.findall(r'(?:minecraft:)?((?:block|item)/[a-z0-9_]*%s[a-z0-9_]*)' % kw, text))
        return refs

    def copy_model(self, ref):
        if ref in self.done_models:
            return
        self.done_models.add(ref)
        src = os.path.join(self.v, "assets", "minecraft", "models", ref + ".json")
        if not os.path.exists(src):
            return  # a texture reference, not a model
        text = open(src).read()
        dst = os.path.join(A, "models", ref.replace(self.src, self.dst) + ".json")
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        open(dst, "w").write(self.rewrite(text))
        WRITTEN.append(dst)
        for r in self.model_refs(text):
            self.copy_model(r)

    def copy_file(self, rel_src, rel_dst):
        src = os.path.join(self.v, rel_src)
        if not os.path.exists(src):
            return None
        text = open(src).read()
        dst = os.path.join(RES, rel_dst)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        open(dst, "w").write(self.rewrite(text))
        WRITTEN.append(dst)
        for r in self.model_refs(text):
            self.copy_model(r)
        return text

    def run(self, recipe_whitelist):
        for n in self.names:
            m = n.replace(self.src, self.dst)
            self.copy_file(f"assets/minecraft/blockstates/{n}.json", f"assets/{NS}/blockstates/{m}.json")
            self.copy_file(f"assets/minecraft/items/{n}.json", f"assets/{NS}/items/{m}.json")
            self.copy_file(f"data/minecraft/loot_table/blocks/{n}.json", f"data/{NS}/loot_table/blocks/{m}.json")
        rdir = os.path.join(self.v, "data", "minecraft", "recipe")
        for f in sorted(os.listdir(rdir)):
            if f[:-5] in recipe_whitelist:
                self.copy_file(f"data/minecraft/recipe/{f}", f"data/{NS}/recipe/{f.replace(self.src, self.dst)}")


WOOD = ["cherry_log", "cherry_wood", "stripped_cherry_log", "stripped_cherry_wood", "cherry_planks", "cherry_leaves",
        "cherry_sapling", "potted_cherry_sapling", "cherry_stairs", "cherry_slab", "cherry_fence", "cherry_fence_gate",
        "cherry_door", "cherry_trapdoor", "cherry_button", "cherry_pressure_plate"]
WOOD_RECIPES = ["cherry_planks", "cherry_wood", "stripped_cherry_wood", "cherry_stairs", "cherry_slab", "cherry_fence",
                "cherry_fence_gate", "cherry_door", "cherry_trapdoor", "cherry_button", "cherry_pressure_plate"]
STONE = ["cobbled_deepslate", "cobbled_deepslate_stairs", "cobbled_deepslate_slab", "cobbled_deepslate_wall",
         "polished_deepslate", "polished_deepslate_stairs", "polished_deepslate_slab", "polished_deepslate_wall",
         "deepslate_bricks", "deepslate_brick_stairs", "deepslate_brick_slab", "deepslate_brick_wall", "chiseled_deepslate"]
STONE_RECIPES = [r for r in """chiseled_deepslate chiseled_deepslate_from_cobbled_deepslate_stonecutting cobbled_deepslate_slab
cobbled_deepslate_slab_from_cobbled_deepslate_stonecutting cobbled_deepslate_stairs cobbled_deepslate_stairs_from_cobbled_deepslate_stonecutting
cobbled_deepslate_wall cobbled_deepslate_wall_from_cobbled_deepslate_stonecutting deepslate_brick_slab deepslate_brick_slab_from_cobbled_deepslate_stonecutting
deepslate_brick_slab_from_deepslate_bricks_stonecutting deepslate_brick_slab_from_polished_deepslate_stonecutting deepslate_brick_stairs
deepslate_brick_stairs_from_cobbled_deepslate_stonecutting deepslate_brick_stairs_from_deepslate_bricks_stonecutting deepslate_brick_stairs_from_polished_deepslate_stonecutting
deepslate_brick_wall deepslate_brick_wall_from_cobbled_deepslate_stonecutting deepslate_brick_wall_from_deepslate_bricks_stonecutting
deepslate_brick_wall_from_polished_deepslate_stonecutting deepslate_bricks deepslate_bricks_from_cobbled_deepslate_stonecutting
deepslate_bricks_from_polished_deepslate_stonecutting polished_deepslate polished_deepslate_from_cobbled_deepslate_stonecutting polished_deepslate_slab
polished_deepslate_slab_from_cobbled_deepslate_stonecutting polished_deepslate_slab_from_polished_deepslate_stonecutting polished_deepslate_stairs
polished_deepslate_stairs_from_cobbled_deepslate_stonecutting polished_deepslate_stairs_from_polished_deepslate_stonecutting polished_deepslate_wall
polished_deepslate_wall_from_cobbled_deepslate_stonecutting polished_deepslate_wall_from_polished_deepslate_stonecutting""".split()]
PORTAL = ["nether_portal"]


# ============================================================================ hand-made blocks
def cube_all(name, tex=None):
    write_json(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": rid(f"block/{name}")}}})
    write_json(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": rid(f"block/{tex or name}")}})
    block_item(name)


def block_item(name, model=None):
    write_json(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": model or rid(f"block/{name}")}})


def flat_item(name, tex=None):
    write_json(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{tex or name}")}})
    write_json(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{name}")}})


def cross_plant(name, potted=False):
    write_json(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": rid(f"block/{name}")}}})
    write_json(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cross", "textures": {"cross": rid(f"block/{name}")}})
    write_json(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"block/{name}")}})
    write_json(f"{A}/items/{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{name}")}})
    if potted:
        p = f"potted_{name}"
        write_json(f"{A}/blockstates/{p}.json", {"variants": {"": {"model": rid(f"block/{p}")}}})
        write_json(f"{A}/models/block/{p}.json", {"parent": "minecraft:block/flower_pot_cross", "textures": {"plant": rid(f"block/{name}")}})


def loot_self(name, item=None):
    write_json(f"{D}/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"condition": {"type": "minecraft:survives_explosion"},
                   "entries": [{"type": "minecraft:item", "name": rid(item or name)}], "rolls": 1}],
        "random_sequence": rid(f"blocks/{name}")})


def loot_silk_or(name, other, count=None):
    alt = {"type": "minecraft:item", "modifier": [{"type": "minecraft:explosion_decay"}], "name": other}
    if count:
        alt["modifier"] = [{"type": "minecraft:set_count", "count": count},
                           {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                           {"type": "minecraft:explosion_decay"}]
    write_json(f"{D}/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": rid(name)}, alt]}], "rolls": 1}],
        "random_sequence": rid(f"blocks/{name}")})


def loot_shears_only(name, item=None, double=False):
    children = [{"type": "minecraft:item", "condition": "minecraft:tool/can_shear", "name": rid(item or name)},
                {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": rid(item or name)}]
    pool = {"entries": [{"type": "minecraft:alternatives", "children": children}], "rolls": 1}
    if double:
        children[0]["name"] = rid("short_sift_grass")
        children[0]["modifier"] = {"type": "minecraft:set_count", "count": 2}
        pool["condition"] = {"type": "minecraft:match_block", "blocks": rid(name), "state": {"half": "lower"}}
    write_json(f"{D}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [pool], "random_sequence": rid(f"blocks/{name}")})


def gen_handmade_blocks():
    # terrain
    write_json(f"{A}/blockstates/sift_grass_block.json", {"variants": {"": [{"model": rid("block/sift_grass_block")},
                                                                           {"model": rid("block/sift_grass_block"), "y": 90},
                                                                           {"model": rid("block/sift_grass_block"), "y": 180},
                                                                           {"model": rid("block/sift_grass_block"), "y": 270}]}})
    write_json(f"{A}/models/block/sift_grass_block.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": rid("block/sift_grass_block_top"), "side": rid("block/sift_grass_block_side"), "bottom": rid("block/sift_soil")}})
    block_item("sift_grass_block")
    loot_silk_or("sift_grass_block", rid("sift_soil"))
    for n in ["sift_soil", "sift_sculk", "dark_sift_sculk", "sift_sand", "soul_shard_block"]:
        cube_all(n); loot_self(n)
    cube_all("soulshale")
    loot_silk_or("soulshale", rid("cobbled_soulshale"))
    cube_all("soul_shard_ore")
    loot_silk_or("soul_shard_ore", rid("soul_shard"), count={"type": "minecraft:uniform", "min": 1, "max": 2})
    # husk shell: pillar
    write_json(f"{A}/blockstates/husk_shell.json", {"variants": {
        "axis=x": {"model": rid("block/husk_shell"), "x": 90, "y": 90}, "axis=y": {"model": rid("block/husk_shell")},
        "axis=z": {"model": rid("block/husk_shell"), "x": 90}}})
    write_json(f"{A}/models/block/husk_shell.json", {"parent": "minecraft:block/cube_column", "textures": {
        "end": rid("block/husk_shell_top"), "side": rid("block/husk_shell")}})
    block_item("husk_shell"); loot_self("husk_shell")

    # plants
    cross_plant("short_sift_grass"); loot_shears_only("short_sift_grass")
    cross_plant("teal_sprouts"); loot_shears_only("teal_sprouts")
    cross_plant("lullaby_bloom", potted=True); loot_self("lullaby_bloom")
    write_json(f"{D}/loot_table/blocks/potted_lullaby_bloom.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": "minecraft:flower_pot"}]},
        {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": rid("lullaby_bloom")}]}],
        "random_sequence": rid("blocks/potted_lullaby_bloom")})
    write_json(f"{A}/blockstates/tall_sift_grass.json", {"variants": {"half=lower": {"model": rid("block/tall_sift_grass_bottom")},
                                                                      "half=upper": {"model": rid("block/tall_sift_grass_top")}}})
    for h in ("bottom", "top"):
        write_json(f"{A}/models/block/tall_sift_grass_{h}.json", {"parent": "minecraft:block/cross", "textures": {"cross": rid(f"block/tall_sift_grass_{h}")}})
    write_json(f"{A}/models/item/tall_sift_grass.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid("block/tall_sift_grass_top")}})
    write_json(f"{A}/items/tall_sift_grass.json", {"model": {"type": "minecraft:model", "model": rid("item/tall_sift_grass")}})
    loot_shears_only("tall_sift_grass", double=True)

    # fluid block
    write_json(f"{A}/blockstates/soul_water.json", {"variants": {"": {"model": rid("block/soul_water")}}})
    write_json(f"{A}/models/block/soul_water.json", {"textures": {"particle": rid("block/soul_water_still")}})


SIFTER_MOBS = ["seedling", "nester", "pollinator", "sentinel", "sprout", "harmonizer", "scavenger"]


def gen_items():
    for n in ["echo_tuner", "soul_shard", "blub_jelly", "soul_water_bucket", "blub_bucket",
              "blub_spawn_egg", "sculk_prowler_spawn_egg", "soul_wisp_spawn_egg",
              "nuzzle_spawn_egg", "slabber_spawn_egg", "echo_golem_spawn_egg", "singer_spawn_egg",
              "pollen_glob", "sentinel_quill", "soul_bolt", "monarch_feather", "monarch_spawn_egg"] + [f"{m}_spawn_egg" for m in SIFTER_MOBS]:
        flat_item(n)


# ============================================================================ lang
LANG = {
    "itemGroup.sift_unbound": "The Sift Unbound",
    # terrain
    "block.sift_unbound.sift_grass_block": "Sift Grass Block",
    "block.sift_unbound.sift_soil": "Sift Soil",
    "block.sift_unbound.sift_sculk": "Sift Sculk",
    "block.sift_unbound.dark_sift_sculk": "Dark Sift Sculk",
    "block.sift_unbound.sift_sand": "Sift Sand",
    "block.sift_unbound.husk_shell": "Husk Shell",
    "block.sift_unbound.soulshale": "Soulshale",
    "block.sift_unbound.cobbled_soulshale": "Cobbled Soulshale",
    "block.sift_unbound.cobbled_soulshale_stairs": "Cobbled Soulshale Stairs",
    "block.sift_unbound.cobbled_soulshale_slab": "Cobbled Soulshale Slab",
    "block.sift_unbound.cobbled_soulshale_wall": "Cobbled Soulshale Wall",
    "block.sift_unbound.polished_soulshale": "Polished Soulshale",
    "block.sift_unbound.polished_soulshale_stairs": "Polished Soulshale Stairs",
    "block.sift_unbound.polished_soulshale_slab": "Polished Soulshale Slab",
    "block.sift_unbound.polished_soulshale_wall": "Polished Soulshale Wall",
    "block.sift_unbound.soulshale_bricks": "Soulshale Bricks",
    "block.sift_unbound.soulshale_brick_stairs": "Soulshale Brick Stairs",
    "block.sift_unbound.soulshale_brick_slab": "Soulshale Brick Slab",
    "block.sift_unbound.soulshale_brick_wall": "Soulshale Brick Wall",
    "block.sift_unbound.chiseled_soulshale": "Chiseled Soulshale",
    "block.sift_unbound.soul_shard_ore": "Soul Shard Ore",
    "block.sift_unbound.soul_shard_block": "Block of Soul Shard",
    # plants
    "block.sift_unbound.short_sift_grass": "Short Sift Grass",
    "block.sift_unbound.tall_sift_grass": "Tall Sift Grass",
    "block.sift_unbound.teal_sprouts": "Teal Sprouts",
    "block.sift_unbound.lullaby_bloom": "Lullaby Bloom",
    "block.sift_unbound.potted_lullaby_bloom": "Potted Lullaby Bloom",
    # pallid wood
    "block.sift_unbound.pallid_log": "Pallid Log", "block.sift_unbound.pallid_wood": "Pallid Wood",
    "block.sift_unbound.stripped_pallid_log": "Stripped Pallid Log", "block.sift_unbound.stripped_pallid_wood": "Stripped Pallid Wood",
    "block.sift_unbound.pallid_planks": "Pallid Planks", "block.sift_unbound.pallid_leaves": "Pallid Leaves",
    "block.sift_unbound.pallid_sapling": "Pallid Sapling", "block.sift_unbound.potted_pallid_sapling": "Potted Pallid Sapling",
    "block.sift_unbound.pallid_stairs": "Pallid Stairs", "block.sift_unbound.pallid_slab": "Pallid Slab",
    "block.sift_unbound.pallid_fence": "Pallid Fence", "block.sift_unbound.pallid_fence_gate": "Pallid Fence Gate",
    "block.sift_unbound.pallid_door": "Pallid Door", "block.sift_unbound.pallid_trapdoor": "Pallid Trapdoor",
    "block.sift_unbound.pallid_button": "Pallid Button", "block.sift_unbound.pallid_pressure_plate": "Pallid Pressure Plate",
    # fluid & portal
    "block.sift_unbound.soul_water": "Ichor",
    "block.sift_unbound.sift_portal": "Sift Portal",
    # items
    "item.sift_unbound.echo_tuner": "Echo Tuner",
    "item.sift_unbound.echo_tuner.desc": "Use inside an ancient city's Warden Portal frame",
    "item.sift_unbound.echo_tuner.desc2": "Sneak-use to hum the Sift's melody",
    "item.sift_unbound.soul_shard": "Soul Shard",
    "item.sift_unbound.blub_jelly": "Blub Jelly",
    "item.sift_unbound.soul_water_bucket": "Ichor Bucket",
    "item.sift_unbound.blub_bucket": "Bucket of Blub",
    "item.sift_unbound.blub_spawn_egg": "Blub Spawn Egg",
    "item.sift_unbound.sculk_prowler_spawn_egg": "Stalker Spawn Egg",
    "item.sift_unbound.soul_wisp_spawn_egg": "Soul Wisp Spawn Egg",
    "item.sift_unbound.nuzzle_spawn_egg": "Nuzzle Spawn Egg",
    "item.sift_unbound.slabber_spawn_egg": "Slabber Spawn Egg",
    "item.sift_unbound.echo_golem_spawn_egg": "Echo Golem Spawn Egg",
    "item.sift_unbound.singer_spawn_egg": "Singer Spawn Egg",
    "item.sift_unbound.seedling_spawn_egg": "Seedling Spawn Egg",
    "item.sift_unbound.nester_spawn_egg": "Nester Spawn Egg",
    "item.sift_unbound.pollinator_spawn_egg": "Pollinator Spawn Egg",
    "item.sift_unbound.sentinel_spawn_egg": "Sentinel Spawn Egg",
    "item.sift_unbound.sprout_spawn_egg": "Sprout Spawn Egg",
    "item.sift_unbound.harmonizer_spawn_egg": "Harmonizer Spawn Egg",
    "item.sift_unbound.scavenger_spawn_egg": "Scavenger Spawn Egg",
    "item.sift_unbound.pollen_glob": "Pollen Glob",
    "item.sift_unbound.pollen_glob.desc": "Thrown, it sticks and bursts into slowing goo",
    "item.sift_unbound.sentinel_quill": "Sentinel Quill",
    "item.sift_unbound.soul_bolt": "Soul Bolt",
    # entities
    "entity.sift_unbound.blub": "Blub",
    "entity.sift_unbound.sculk_prowler": "Stalker",
    "entity.sift_unbound.soul_wisp": "Soul Wisp",
    "entity.sift_unbound.nuzzle": "Nuzzle",
    "entity.sift_unbound.slabber": "Slabber",
    "entity.sift_unbound.echo_golem": "Echo Golem",
    "entity.sift_unbound.singer": "Singer",
    "entity.sift_unbound.seedling": "Seedling",
    "entity.sift_unbound.nester": "Nester",
    "entity.sift_unbound.pollinator": "Pollinator",
    "entity.sift_unbound.sentinel": "Sentinel",
    "entity.sift_unbound.sprout": "Sprout",
    "entity.sift_unbound.harmonizer": "Harmonizer",
    "entity.sift_unbound.scavenger": "Scavenger",
    "entity.sift_unbound.pollen_glob": "Pollen Glob",
    "entity.sift_unbound.sentinel_quill": "Sentinel Quill",
    "entity.sift_unbound.soul_bolt": "Soul Bolt",
    # effect
    "effect.sift_unbound.soulsick": "Soulsick",
    "death.attack.sift_unbound.soulsick": "%1$s had their soul drained away",
    "death.attack.sift_unbound.soulsick.player": "%1$s had their soul drained away while fighting %2$s",
    # biomes
    "biome.sift_unbound.sift_meadows": "Singer's Meadow",
    "biome.sift_unbound.pallid_grove": "Pallid Grove",
    "biome.sift_unbound.soul_wetlands": "Ichor Ponds",
    "biome.sift_unbound.husklands": "Humbler Huskland",
    "biome.sift_unbound.lullaby_hills": "Lullaby Hills",
    "biome.sift_unbound.sifter_canyons": "Sifter Canyons",
    "entity.sift_unbound.monarch": "The Monarch",
    "entity.sift_unbound.monarch_echo": "Monarch Echo",
    "entity.sift_unbound.monarch_feather": "Monarch Feather",
    "item.sift_unbound.monarch_feather": "Monarch Feather",
    "item.sift_unbound.monarch_spawn_egg": "Monarch Spawn Egg",
    "subtitles.sift_unbound.monarch.ambient": "Monarch clicks",
    "subtitles.sift_unbound.monarch.hurt": "Monarch hurts",
    "subtitles.sift_unbound.monarch.death": "Monarch dies",
    "subtitles.sift_unbound.monarch.roar": "Monarch roars",
    "subtitles.sift_unbound.monarch.dash": "Monarch dashes",
    "subtitles.sift_unbound.monarch.echo": "Monarch Echo forms",
    "advancements.sift_unbound.sculker_nest.title": "Into the Ravines",
    "advancements.sift_unbound.sculker_nest.description": "Find a sculker nest: a tunnel behind a soulshale doorway in the hillside leads down to the Monarch's cavern",
    "advancements.sift_unbound.monarch.title": "Dethroned",
    "advancements.sift_unbound.monarch.description": "Defeat the Monarch in its nest. Every melee hit it lands heals it, and once wounded its Echo joins the fight",
    # messages
    "message.sift_unbound.portal.lit": "The Warden Portal hums awake...",
    "message.sift_unbound.portal.machine": "The note blocks ring out the Sift's melody. The Warden Portal hums awake...",
    "message.sift_unbound.melody": "The Sift's melody: %s. Play it on eight note blocks in front of a Warden Portal frame.",
    "message.sift_unbound.singer": "The Singer sings the Sift's melody: %s",
    "message.sift_unbound.tide.flow": "The Flow Tide returns",
    "message.sift_unbound.tide.endure": "The Endure Tide rises. Wounds will not mend on their own.",
    "message.sift_unbound.tide.thrive": "The Thrive Tide blooms. Wounds mend quickly.",
    "message.sift_unbound.portal.no_frame": "The Echo Tuner hums, but there's no complete Warden Portal frame here.",
    "message.sift_unbound.portal.too_big": "This frame is too big for the Echo Tuner to fill.",
    "message.sift_unbound.bed": "The tides of the Sift keep you awake",
    "message.sift_unbound.tide": "Sift tide: %s",
    "tide.sift_unbound.flow": "Flow Tide",
    "tide.sift_unbound.endure": "Endure Tide",
    "tide.sift_unbound.thrive": "Thrive Tide",
    # subtitles
    "subtitles.sift_unbound.blub.ambient": "Blub squeaks",
    "subtitles.sift_unbound.blub.hurt": "Blub hurts",
    "subtitles.sift_unbound.blub.death": "Blub dies",
    "subtitles.sift_unbound.blub.jump": "Blub hops",
    "subtitles.sift_unbound.soul_wisp.ambient": "Soul Wisp chimes",
    "subtitles.sift_unbound.soul_wisp.hurt": "Soul Wisp flickers",
    "subtitles.sift_unbound.soul_wisp.death": "Soul Wisp fades",
    "subtitles.sift_unbound.sculk_prowler.ambient": "Stalker clicks",
    "subtitles.sift_unbound.sculk_prowler.hurt": "Stalker hurts",
    "subtitles.sift_unbound.sculk_prowler.death": "Stalker dies",
    "subtitles.sift_unbound.nuzzle.ambient": "Nuzzle hums",
    "subtitles.sift_unbound.nuzzle.hurt": "Nuzzle hurts",
    "subtitles.sift_unbound.nuzzle.death": "Nuzzle dies",
    "subtitles.sift_unbound.slabber.ambient": "Slabber grunts",
    "subtitles.sift_unbound.slabber.hurt": "Slabber hurts",
    "subtitles.sift_unbound.slabber.death": "Slabber dies",
    "subtitles.sift_unbound.echo_golem.ambient": "Echo Golem resonates",
    "subtitles.sift_unbound.echo_golem.hurt": "Echo Golem hurts",
    "subtitles.sift_unbound.echo_golem.death": "Echo Golem falls silent",
    "subtitles.sift_unbound.singer.ambient": "Singer sings",
    "subtitles.sift_unbound.seedling.ambient": "Seedling chitters",
    "subtitles.sift_unbound.seedling.hurt": "Seedling hurts",
    "subtitles.sift_unbound.seedling.death": "Seedling dies",
    "subtitles.sift_unbound.nester.ambient": "Nester hoots",
    "subtitles.sift_unbound.nester.hurt": "Nester hurts",
    "subtitles.sift_unbound.nester.death": "Nester dies",
    "subtitles.sift_unbound.pollinator.ambient": "Pollinator croaks",
    "subtitles.sift_unbound.pollinator.hurt": "Pollinator hurts",
    "subtitles.sift_unbound.pollinator.death": "Pollinator dies",
    "subtitles.sift_unbound.sentinel.ambient": "Sentinel rattles",
    "subtitles.sift_unbound.sentinel.hurt": "Sentinel hurts",
    "subtitles.sift_unbound.sentinel.death": "Sentinel dies",
    "subtitles.sift_unbound.sprout.ambient": "Sprout chimes",
    "subtitles.sift_unbound.sprout.hurt": "Sprout hurts",
    "subtitles.sift_unbound.sprout.death": "Sprout dies",
    "subtitles.sift_unbound.harmonizer.ambient": "Harmonizer hums",
    "subtitles.sift_unbound.harmonizer.hurt": "Harmonizer hurts",
    "subtitles.sift_unbound.harmonizer.death": "Harmonizer dies",
    "subtitles.sift_unbound.scavenger.ambient": "Scavenger clicks",
    "subtitles.sift_unbound.scavenger.hurt": "Scavenger hurts",
    "subtitles.sift_unbound.scavenger.death": "Scavenger dies",
    "subtitles.sift_unbound.pollinator.puff": "Pollinator puffs up",
    "subtitles.sift_unbound.sentinel.shake": "Sentinel shakes its head",
    "subtitles.sift_unbound.sentinel.quills": "Quills fly",
    "subtitles.sift_unbound.sprout.link": "Sprout links",
    "subtitles.sift_unbound.harmonizer.roar": "Harmonizer roars",
    "subtitles.sift_unbound.scavenger.cast": "Scavenger casts",
    "subtitles.sift_unbound.scavenger.dash": "Scavenger dashes",
    "subtitles.sift_unbound.sifter.enrage": "Sifters are enraged",
    "subtitles.sift_unbound.sifter.dodge": "Sifter dodges",
    "subtitles.sift_unbound.soul_water.ambient": "Ichor bubbles",
    "subtitles.sift_unbound.soul_water.burn": "Soul drains",
    "subtitles.sift_unbound.sift_portal.ambient": "Sift Portal hums",
    "subtitles.sift_unbound.sift_portal.travel": "Sift Portal resonates",
    "subtitles.sift_unbound.sift_portal.activate": "Warden Portal awakens",
    # advancements
    "advancements.sift_unbound.root.title": "The Sift Unbound",
    "advancements.sift_unbound.root.description": "Craft an Echo Tuner from echo shards found in ancient cities",
    "advancements.sift_unbound.hum_melody.title": "Hum Along",
    "advancements.sift_unbound.hum_melody.description": "Sneak and use the Echo Tuner to hum the Sift's melody and see its notes",
    "advancements.sift_unbound.note_block_machine.title": "Note Block Machine",
    "advancements.sift_unbound.note_block_machine.description": "Play the melody (F♯ A C♯ B A E C♯ F♯) on eight note blocks by a Warden Portal. Each right note sends up a soul",
    "advancements.sift_unbound.chiseled_soulshale.title": "Singer in Stone",
    "advancements.sift_unbound.chiseled_soulshale.description": "Find Chiseled Soulshale, carved with a singer, in an ancient city chest",
    "advancements.sift_unbound.light_portal.title": "Resonance",
    "advancements.sift_unbound.light_portal.description": "Wake a Warden Portal with the Sift's melody: use an Echo Tuner on its frame, or play the melody on note blocks",
    "advancements.sift_unbound.enter_sift.title": "Beyond the Frame",
    "advancements.sift_unbound.enter_sift.description": "Step through the Warden Portal into the Sift",
    "advancements.sift_unbound.soulsick.title": "Ouch!",
    "advancements.sift_unbound.soulsick.description": "Wade into ichor and feel Soulsickness drain your soul. The Sift's own creatures are immune",
    "advancements.sift_unbound.soul_shard.title": "Crystallized Souls",
    "advancements.sift_unbound.soul_shard.description": "Mine a Soul Shard from Soulshale",
    "advancements.sift_unbound.tide_endure.title": "Endure",
    "advancements.sift_unbound.tide_endure.description": "Be in the Sift when the Endure Tide rises: wounds stop mending on their own and monsters grow stronger",
    "advancements.sift_unbound.tide_thrive.title": "Thrive",
    "advancements.sift_unbound.tide_thrive.description": "Be in the Sift when the Thrive Tide comes: wounds mend 50% faster. Check the tide any time with /sift tide",
    "advancements.sift_unbound.blub_jelly.title": "Jelly Squeeze",
    "advancements.sift_unbound.blub_jelly.description": "Collect Blub Jelly, which blubs shed after a soak in ichor",
    "advancements.sift_unbound.blub_friend.title": "Squeak Squeak",
    "advancements.sift_unbound.blub_friend.description": "Breed two Blubs with Lullaby Blooms",
    "advancements.sift_unbound.shear_nuzzle.title": "Pink and Fluffy",
    "advancements.sift_unbound.shear_nuzzle.description": "Shear a Nuzzle for pink wool. Its fleece grows back when it grazes Sift grass",
    "advancements.sift_unbound.feed_slabber.title": "Is That a Rock?",
    "advancements.sift_unbound.feed_slabber.description": "Feed a Slabber Teal Sprouts. Slabbers doze through the Endure Tide, tucked in like part of the ground",
    "advancements.sift_unbound.singer.title": "The Singer's Song",
    "advancements.sift_unbound.singer.description": "Ask the Singer to sing. The healthy warden keeps the Singer's Portal and creates echo golems",
    "advancements.sift_unbound.echo_golem.title": "Caretaker",
    "advancements.sift_unbound.echo_golem.description": "Hand an Echo Golem an Echo Shard: it hums the melody, and wakes a dormant Warden Portal nearby. Golems recharge in ichor",
    "advancements.sift_unbound.golem_portal.title": "Golem at Work",
    "advancements.sift_unbound.golem_portal.description": "Let an Echo Golem wake a Warden Portal for you",
    "advancements.sift_unbound.prowler.title": "Tide Hunter",
    "advancements.sift_unbound.prowler.description": "Defeat a Stalker, a sculker of the Endure Tide",
    "advancements.sift_unbound.sifter.title": "Weeding",
    "advancements.sift_unbound.sifter.description": "Defeat a sifter, one of the Sift's plant-like natives. Hurt one and its whole band comes for you",
    "advancements.sift_unbound.sprout_enrage.title": "Root of the Problem",
    "advancements.sift_unbound.sprout_enrage.description": "Defeat a Sprout with other sifters nearby. Its death enrages them for good: faster, stronger and tougher",
    "advancements.sift_unbound.all_sifters.title": "Sifted",
    "advancements.sift_unbound.all_sifters.description": "Defeat every kind of sifter: Seedling, Nester, Pollinator, Sentinel, Sprout and Harmonizer",
    "advancements.sift_unbound.harmonizer.title": "Discord",
    "advancements.sift_unbound.harmonizer.description": "Defeat a Harmonizer, the massive sprout. Keep jumping to escape its vortex",
    "advancements.sift_unbound.scavenger.title": "Golden Afterimage",
    "advancements.sift_unbound.scavenger.description": "Defeat a Scavenger. Its soul bolts chase you down and leave pools of soulsickness; get close and it dashes away",
    "advancements.sift_unbound.explore_sift.title": "Sift Explorer",
    "advancements.sift_unbound.explore_sift.description": "Visit every region of the Sift",
    # commands
    "commands.sift_unbound.credits": "The Sift Unbound - an unofficial fan mod. The Sift, the Blub, sculk and the ancient city are created by Mojang Studios; Minecraft is a trademark of Mojang Studios / Microsoft. This mod is not affiliated with or endorsed by them. Reference info: minecraft.wiki (CC BY-NC-SA 3.0). All textures and sounds in this mod are original. Made by MilkdromedaStudios.",
}


def gen_lang():
    write_json(f"{A}/lang/en_us.json", LANG)


# ============================================================================ sounds.json
SOUNDS = {
    "entity.blub.ambient": ["blub/squeak1", "blub/squeak2", "blub/squeak3"],
    "entity.blub.hurt": ["blub/hurt1", "blub/hurt2"],
    "entity.blub.death": ["blub/death"],
    "entity.blub.jump": ["blub/hop1", "blub/hop2"],
    "entity.soul_wisp.ambient": ["soul_wisp/chime1", "soul_wisp/chime2"],
    "entity.soul_wisp.hurt": ["soul_wisp/hurt"],
    "entity.soul_wisp.death": ["soul_wisp/death"],
    "entity.sculk_prowler.ambient": ["sculk_prowler/click1", "sculk_prowler/click2"],
    "entity.sculk_prowler.hurt": ["sculk_prowler/hurt"],
    "entity.sculk_prowler.death": ["sculk_prowler/death"],
    "entity.nuzzle.ambient": ["nuzzle/hum1", "nuzzle/hum2", "nuzzle/hum3"],
    "entity.nuzzle.hurt": ["nuzzle/hurt"],
    "entity.nuzzle.death": ["nuzzle/death"],
    "entity.slabber.ambient": ["slabber/grunt1", "slabber/grunt2"],
    "entity.slabber.hurt": ["slabber/hurt"],
    "entity.slabber.death": ["slabber/death"],
    "entity.echo_golem.ambient": ["echo_golem/hum1", "echo_golem/hum2"],
    "entity.echo_golem.hurt": ["echo_golem/hurt"],
    "entity.echo_golem.death": ["echo_golem/death"],
    "entity.singer.ambient": ["singer/song1", "singer/song2"],
    "entity.seedling.ambient": ["seedling/ambient1", "seedling/ambient2"],
    "entity.seedling.hurt": ["seedling/hurt"],
    "entity.seedling.death": ["seedling/death"],
    "entity.nester.ambient": ["nester/ambient1", "nester/ambient2"],
    "entity.nester.hurt": ["nester/hurt"],
    "entity.nester.death": ["nester/death"],
    "entity.pollinator.ambient": ["pollinator/ambient1", "pollinator/ambient2"],
    "entity.pollinator.hurt": ["pollinator/hurt"],
    "entity.pollinator.death": ["pollinator/death"],
    "entity.sentinel.ambient": ["sentinel/ambient1", "sentinel/ambient2"],
    "entity.sentinel.hurt": ["sentinel/hurt"],
    "entity.sentinel.death": ["sentinel/death"],
    "entity.sprout.ambient": ["sprout/ambient1", "sprout/ambient2"],
    "entity.sprout.hurt": ["sprout/hurt"],
    "entity.sprout.death": ["sprout/death"],
    "entity.harmonizer.ambient": ["harmonizer/ambient1", "harmonizer/ambient2"],
    "entity.harmonizer.hurt": ["harmonizer/hurt"],
    "entity.harmonizer.death": ["harmonizer/death"],
    "entity.scavenger.ambient": ["scavenger/ambient1", "scavenger/ambient2"],
    "entity.scavenger.hurt": ["scavenger/hurt"],
    "entity.scavenger.death": ["scavenger/death"],
    "entity.pollinator.puff": ["pollinator/puff"],
    "entity.sentinel.shake": ["sentinel/shake"],
    "entity.sentinel.quills": ["sentinel/quills"],
    "entity.sprout.link": ["sprout/link"],
    "entity.harmonizer.roar": ["harmonizer/roar"],
    "entity.scavenger.cast": ["scavenger/cast"],
    "entity.scavenger.dash": ["scavenger/dash"],
    "entity.sifter.enrage": ["sifter/enrage"],
    "entity.monarch.ambient": ["monarch/ambient1", "monarch/ambient2"],
    "entity.monarch.hurt": ["monarch/hurt"],
    "entity.monarch.death": ["monarch/death"],
    "entity.monarch.roar": ["monarch/roar"],
    "entity.monarch.dash": ["monarch/dash"],
    "entity.monarch.echo": ["monarch/echo"],
    "entity.sifter.dodge": ["sifter/dodge"],
    "block.soul_water.ambient": ["soul_water/bubble1", "soul_water/bubble2"],
    "block.soul_water.burn": ["soul_water/burn"],
    "block.sift_portal.ambient": ["portal/hum"],
    "block.sift_portal.travel": ["portal/travel"],
    "block.sift_portal.activate": ["portal/activate"],
    "ambient.sift.loop": ["ambient/loop"],
    "ambient.sift.additions": ["ambient/addition1", "ambient/addition2", "ambient/addition3"],
    "ambient.sift.mood": ["ambient/mood"],
    "music.sift": ["music/sift_lullaby"],
}


def gen_sounds_json():
    out = {}
    for ev, files in SOUNDS.items():
        entry = {"sounds": [{"name": rid(f), **({"stream": True} if ev.startswith("music") or ev.endswith("loop") else {})} for f in files]}
        key = "subtitles.sift_unbound." + ".".join(ev.split(".")[1:])
        if key in LANG:
            entry["subtitle"] = key
        out[ev] = entry
    write_json(f"{A}/sounds.json", out)


# ============================================================================ tags
def tag(kind, ns, name, values, replace=False):
    base = f"{RES}/data/{ns}/tags/{kind}/{name}.json"
    write_json(base, {"replace": replace, "values": values})


PALLID_LOGS = [rid(x) for x in ["pallid_log", "pallid_wood", "stripped_pallid_log", "stripped_pallid_wood"]]
STONES = [rid(x) for x in ["soulshale", "cobbled_soulshale", "cobbled_soulshale_stairs", "cobbled_soulshale_slab", "cobbled_soulshale_wall",
                           "polished_soulshale", "polished_soulshale_stairs", "polished_soulshale_slab", "polished_soulshale_wall",
                           "soulshale_bricks", "soulshale_brick_stairs", "soulshale_brick_slab", "soulshale_brick_wall",
                           "chiseled_soulshale", "soul_shard_ore", "soul_shard_block", "husk_shell", "sift_sculk", "dark_sift_sculk"]]
GROUND = [rid(x) for x in ["sift_grass_block", "sift_soil", "sift_sculk", "dark_sift_sculk", "sift_sand"]]


def gen_damage_type():
    write_json(f"{D}/damage_type/soulsick.json", {"exhaustion": 0.0, "message_id": "sift_unbound.soulsick", "scaling": "never"})


def gen_tags():
    tag("block", NS, "pallid_logs", PALLID_LOGS)
    tag("item", NS, "pallid_logs", PALLID_LOGS)
    tag("block", NS, "warden_portal_frame", ["minecraft:reinforced_deepslate"])
    tag("block", NS, "sift_ground", GROUND)
    tag("block", NS, "soulshale_ore_replaceables", [rid("soulshale")])
    tag("item", NS, "blub_food", [rid("lullaby_bloom"), rid("teal_sprouts")])
    tag("item", NS, "nuzzle_food", [rid("short_sift_grass"), rid("tall_sift_grass")])
    tag("item", NS, "slabber_food", [rid("teal_sprouts")])
    tag("fluid", NS, "soul_water", [rid("soul_water"), rid("flowing_soul_water")])
    tag("entity_type", NS, "soul_water_immune", [rid("blub"), rid("soul_wisp"), rid("sculk_prowler"), rid("nuzzle"), rid("slabber"),
                                                  rid("echo_golem"), rid("singer"), "minecraft:warden"] + [rid(m) for m in SIFTER_MOBS] + [rid("monarch"), rid("monarch_echo")])
    tag("entity_type", NS, "sculkers", [rid("sculk_prowler"), rid("scavenger"), rid("monarch"), rid("monarch_echo")])
    tag("worldgen/biome", NS, "has_structure/sculker_nest", [rid("husklands"), rid("sift_meadows"), rid("lullaby_hills"), rid("sifter_canyons")])
    tag("timeline", NS, "in_sift", [rid("sift_tides")])
    tag("worldgen/biome", NS, "is_sift", [rid(b) for b in BIOMES])

    mc = "minecraft"
    tag("damage_type", mc, "bypasses_armor", [rid("soulsick")])
    tag("damage_type", mc, "no_knockback", [rid("soulsick")])
    # 26.x heightmaps, fluid flow, suffocation and teleport checks all read these tags instead of block properties.
    # Stairs, slabs, walls, logs, planks, doors... are covered through the vanilla tags they're already in.
    tag("block", mc, "blocks_motion_no_leaves", [rid(x) for x in [
        "sift_grass_block", "sift_soil", "sift_sculk", "dark_sift_sculk", "sift_sand", "husk_shell", "soulshale", "cobbled_soulshale",
        "polished_soulshale", "soulshale_bricks", "chiseled_soulshale", "soul_shard_ore", "soul_shard_block"]])
    tag("block", mc, "replaceable", [rid(x) for x in ["short_sift_grass", "tall_sift_grass", "teal_sprouts", "soul_water"]])
    tag("block", mc, "replaceable_by_trees", [rid(x) for x in ["short_sift_grass", "tall_sift_grass", "teal_sprouts", "soul_water"]])
    tag("block", mc, "mineable/pickaxe", STONES)
    tag("block", mc, "mineable/shovel", [rid("sift_grass_block"), rid("sift_soil"), rid("sift_sand")])
    tag("block", mc, "mineable/hoe", [rid("sift_sculk"), rid("dark_sift_sculk")])
    tag("block", mc, "needs_stone_tool", [rid("soul_shard_ore"), rid("soul_shard_block")])
    tag("block", mc, "supports_vegetation", [rid("sift_grass_block"), rid("sift_soil"), rid("sift_sculk"), rid("sift_sand")])
    tag("block", mc, "animals_spawnable_on", GROUND)
    tag("block", mc, "overworld_carver_replaceables", GROUND + [rid("soulshale"), rid("husk_shell")])
    tag("block", mc, "logs_that_burn", [f"#{NS}:pallid_logs"])
    tag("item", mc, "logs_that_burn", [f"#{NS}:pallid_logs"])
    tag("block", mc, "overworld_natural_logs", [rid("pallid_log")])
    for k in ("block", "item"):
        tag(k, mc, "planks", [rid("pallid_planks")])
        tag(k, mc, "leaves", [rid("pallid_leaves")])
        tag(k, mc, "saplings", [rid("pallid_sapling")])
        tag(k, mc, "wooden_stairs", [rid("pallid_stairs")])
        tag(k, mc, "wooden_slabs", [rid("pallid_slab")])
        tag(k, mc, "wooden_fences", [rid("pallid_fence")])
        tag(k, mc, "fence_gates", [rid("pallid_fence_gate")])
        tag(k, mc, "wooden_doors", [rid("pallid_door")])
        tag(k, mc, "wooden_trapdoors", [rid("pallid_trapdoor")])
        tag(k, mc, "wooden_buttons", [rid("pallid_button")])
        tag(k, mc, "wooden_pressure_plates", [rid("pallid_pressure_plate")])
        tag(k, mc, "stairs", [rid(x) for x in ["cobbled_soulshale_stairs", "polished_soulshale_stairs", "soulshale_brick_stairs"]])
        tag(k, mc, "slabs", [rid(x) for x in ["cobbled_soulshale_slab", "polished_soulshale_slab", "soulshale_brick_slab"]])
        tag(k, mc, "walls", [rid(x) for x in ["cobbled_soulshale_wall", "polished_soulshale_wall", "soulshale_brick_wall"]])
        tag(k, mc, "small_flowers", [rid("lullaby_bloom")])
    tag("block", mc, "flower_pots", [rid("potted_lullaby_bloom"), rid("potted_pallid_sapling")])
    tag("block", mc, "sand", [rid("sift_sand")])
    tag("item", mc, "stone_crafting_materials", [rid("cobbled_soulshale")])
    tag("item", mc, "stone_tool_materials", [rid("cobbled_soulshale")])


# ============================================================================ recipes
def shaped(name, pattern, key, result, count=1):
    write_json(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                                            "key": key, "result": {"id": result, "count": count}})


def shapeless(name, ingredients, result, count=1):
    write_json(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                            "ingredients": ingredients, "result": {"id": result, "count": count}})


def smelt(name, ingredient, result, xp=0.1):
    write_json(f"{D}/recipe/{name}.json", {"type": "minecraft:smelting", "category": "blocks", "cookingtime": 200,
                                            "experience": xp, "ingredient": ingredient, "result": {"id": result}})


def gen_recipes():
    shaped("echo_tuner", [" E ", "EAE", " N "], {"E": "minecraft:echo_shard", "A": "minecraft:amethyst_shard", "N": "minecraft:note_block"}, rid("echo_tuner"))
    shaped("soul_shard_block", ["###", "###", "###"], {"#": rid("soul_shard")}, rid("soul_shard_block"))
    shapeless("soul_shard_from_block", [rid("soul_shard_block")], rid("soul_shard"), 9)
    smelt("soulshale_from_smelting", rid("cobbled_soulshale"), rid("soulshale"))
    smelt("glass_from_sift_sand", rid("sift_sand"), "minecraft:glass")
    shapeless("light_blue_dye_from_lullaby_bloom", [rid("lullaby_bloom")], "minecraft:light_blue_dye")
    shapeless("cyan_dye_from_teal_sprouts", [rid("teal_sprouts"), rid("teal_sprouts")], "minecraft:cyan_dye")
    shaped("husk_shell_to_bone_meal", ["#"], {"#": rid("husk_shell")}, "minecraft:bone_meal", 6)


# ============================================================================ loot (entities)
def gen_entity_loot():
    def looting():
        return {"type": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting", "count": {"type": "minecraft:uniform", "min": 0.0, "max": 1.0}}
    write_json(f"{D}/loot_table/entities/blub.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": rid("blub_jelly"), "modifier": [
            {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}, looting()]}]}],
        "random_sequence": rid("entities/blub")})
    write_json(f"{D}/loot_table/entities/sculk_prowler.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rid("soul_shard"), "modifier": [
            {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}}, looting()]}]},
        {"rolls": 1, "condition": {"type": "minecraft:all_of", "terms": [{"type": "minecraft:killed_by_player"},
                                                                          {"type": "minecraft:random_chance", "chance": 0.08}]},
         "entries": [{"type": "minecraft:item", "name": "minecraft:echo_shard"}]}],
        "random_sequence": rid("entities/sculk_prowler")})
    write_json(f"{D}/loot_table/entities/soul_wisp.json", {"type": "minecraft:entity", "pools": [], "random_sequence": rid("entities/soul_wisp")})
    write_json(f"{D}/loot_table/entities/nuzzle.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "minecraft:pink_wool"}]}], "random_sequence": rid("entities/nuzzle")})
    write_json(f"{D}/loot_table/shearing/nuzzle.json", {"type": "minecraft:shearing", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "minecraft:pink_wool", "modifier": [
            {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]}]}],
        "random_sequence": rid("shearing/nuzzle")})
    write_json(f"{D}/loot_table/entities/slabber.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": rid("cobbled_soulshale"), "modifier": [
            {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}]}],
        "random_sequence": rid("entities/slabber")})
    write_json(f"{D}/loot_table/entities/echo_golem.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": rid("soul_shard"), "modifier": [
            {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]}]}],
        "random_sequence": rid("entities/echo_golem")})
    write_json(f"{D}/loot_table/entities/singer.json", {"type": "minecraft:entity", "pools": [], "random_sequence": rid("entities/singer")})

    def drops(name, *entries):
        pools = []
        for item, lo, hi, chance in entries:
            pool = {"rolls": 1, "entries": [{"type": "minecraft:item", "name": item, "modifier": [
                {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}]}
            if chance < 1:
                pool["conditions"] = [{"condition": "minecraft:random_chance", "chance": chance}]
            pools.append(pool)
        write_json(f"{D}/loot_table/entities/{name}.json", {"type": "minecraft:entity", "pools": pools, "random_sequence": rid(f"entities/{name}")})
    drops("seedling", (rid("teal_sprouts"), 0, 2, 1))
    drops("nester", ("minecraft:stick", 1, 3, 1), (rid("teal_sprouts"), 0, 1, 1))
    drops("pollinator", (rid("pollen_glob"), 1, 2, 1))
    drops("sentinel", (rid("sentinel_quill"), 1, 3, 1))
    drops("sprout", (rid("soul_shard"), 1, 1, 0.5), (rid("lullaby_bloom"), 0, 1, 1))
    drops("harmonizer", (rid("soul_shard"), 3, 6, 1), (rid("lullaby_bloom"), 2, 4, 1), (rid("sentinel_quill"), 2, 5, 1))
    drops("scavenger", (rid("soul_shard"), 0, 2, 1), ("minecraft:echo_shard", 1, 1, 0.1))
    drops("monarch", ("minecraft:echo_shard", 3, 6, 1), (rid("soul_shard"), 8, 16, 1), (rid("monarch_feather"), 4, 8, 1))
    write_json(f"{D}/loot_table/entities/monarch_echo.json", {"type": "minecraft:entity", "pools": [], "random_sequence": rid("entities/monarch_echo")})
    entries = [(rid("soul_shard"), 3, 8, 20), ("minecraft:echo_shard", 1, 3, 12), (rid("monarch_feather"), 1, 2, 6), ("minecraft:golden_apple", 1, 2, 8),
               ("minecraft:diamond", 1, 3, 8), (rid("chiseled_soulshale"), 1, 2, 10), ("minecraft:enchanted_golden_apple", 1, 1, 1),
               ("minecraft:music_disc_5", 1, 1, 2), (rid("blub_jelly"), 2, 5, 10)]
    write_json(f"{D}/loot_table/chests/sculker_nest.json", {"type": "minecraft:chest", "random_sequence": rid("chests/sculker_nest"), "pools": [{
        "rolls": {"type": "minecraft:uniform", "min": 5, "max": 8},
        "entries": [{"type": "minecraft:item", "name": n, "weight": wt, "modifier": [
            {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]} for n, lo, hi, wt in entries]}]})


# ============================================================================ advancements
def adv(name, parent, icon, criteria, frame="task", hidden=False, show=True, any_of=False):
    obj = {"display": {"icon": {"id": icon}, "title": {"translate": f"advancements.sift_unbound.{name}.title"},
                       "description": {"translate": f"advancements.sift_unbound.{name}.description"},
                       "frame": frame, "show_toast": show, "announce_to_chat": show, "hidden": hidden},
           "criteria": criteria}
    if any_of:  # any one criterion is enough
        obj["requirements"] = [list(criteria)]
    if parent:
        obj["parent"] = rid(parent)
    else:
        obj["display"]["background"] = rid("block/sift_grass_block_top")
    write_json(f"{D}/advancement/{name}.json", obj)


def is_entity(entity_type):
    return {"type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:entity_type": entity_type}}


def killed(entity_type):
    return {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": is_entity(entity_type)}}


def has_item(item):
    return {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}}


def interacted(entity_type, item=None):
    conditions = {"entity": is_entity(entity_type)}
    if item:
        conditions["item"] = {"items": item}
    return {"trigger": "minecraft:player_interacted_with_entity", "conditions": conditions}


def sift_event(event):
    """The mod's own trigger (ModCriteria): things no vanilla trigger sees, such as the melody or the tides."""
    return {"trigger": rid("sift_event"), "conditions": {"event": event}}


def in_biome(biome):
    return {"trigger": "minecraft:location", "conditions": {"player": {
        "type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:location": {"biomes": biome}}}}}


def gen_advancements():
    """A guide: each advancement teaches one of the Sift's mechanics the first time the player meets it."""
    adv("root", None, rid("echo_tuner"), {"has_tuner": has_item(rid("echo_tuner"))})
    # the melody
    adv("hum_melody", "root", "minecraft:music_disc_5", {"hummed": sift_event("hummed_melody")})
    adv("note_block_machine", "hum_melody", "minecraft:note_block", {"played": sift_event("played_melody")}, frame="goal")
    adv("chiseled_soulshale", "root", rid("chiseled_soulshale"), {"found": has_item(rid("chiseled_soulshale"))})
    # awakened with the Echo Tuner, the note block machine or an echo golem (or just stepping into a lit portal)
    adv("light_portal", "root", "minecraft:reinforced_deepslate", {
        "lit": {"trigger": "minecraft:item_used_on_block", "conditions": {
            "location": {"type": "minecraft:match_tool", "predicate": {"items": rid("echo_tuner")}}}},
        "played": sift_event("played_melody"),
        "golem": sift_event("golem_woke_portal"),
        "touched": {"trigger": "minecraft:enter_block", "conditions": {"block": rid("sift_portal")}}}, frame="goal", any_of=True)
    adv("enter_sift", "light_portal", rid("sift_grass_block"), {"entered": {"trigger": "minecraft:changed_dimension", "conditions": {"to": rid("the_sift")}}}, frame="challenge")
    # the land
    adv("soulsick", "enter_sift", rid("soul_water_bucket"), {"sick": {"trigger": "minecraft:effects_changed", "conditions": {"effects": {rid("soulsick"): {}}}}})
    adv("soul_shard", "enter_sift", rid("soul_shard"), {"shard": has_item(rid("soul_shard"))})
    adv("tide_endure", "enter_sift", "minecraft:sculk_shrieker", {"endure": sift_event("tide_endure")})
    adv("tide_thrive", "tide_endure", "minecraft:glistering_melon_slice", {"thrive": sift_event("tide_thrive")})
    adv("explore_sift", "enter_sift", "minecraft:recovery_compass", {b: in_biome(rid(b)) for b in BIOMES}, frame="challenge")
    # its creatures
    adv("blub_jelly", "enter_sift", rid("blub_jelly"), {"jelly": has_item(rid("blub_jelly"))})
    adv("blub_friend", "blub_jelly", rid("lullaby_bloom"), {"bred": {"trigger": "minecraft:bred_animals", "conditions": {"child": is_entity(rid("blub"))}}})
    adv("shear_nuzzle", "enter_sift", "minecraft:shears", {"sheared": sift_event("sheared_nuzzle")})
    adv("feed_slabber", "enter_sift", rid("teal_sprouts"), {"fed": interacted(rid("slabber"), "#" + rid("slabber_food"))})
    adv("singer", "enter_sift", "minecraft:sculk_catalyst", {"sang": interacted(rid("singer"))})
    adv("echo_golem", "singer", "minecraft:soul_lantern", {"shard": interacted(rid("echo_golem"), "minecraft:echo_shard")})
    adv("golem_portal", "echo_golem", "minecraft:reinforced_deepslate", {"woke": sift_event("golem_woke_portal")}, frame="goal")
    adv("prowler", "enter_sift", "minecraft:echo_shard", {"killed": killed(rid("sculk_prowler"))})
    adv("scavenger", "prowler", rid("soul_bolt"), {"killed": killed(rid("scavenger"))})
    adv("sculker_nest", "scavenger", rid("dark_sift_sculk"), {"found": {"trigger": "minecraft:location", "conditions": {"player": {
        "type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:location": {"structures": rid("sculker_nest")}}}}}})
    adv("monarch", "sculker_nest", rid("monarch_feather"), {"killed": killed(rid("monarch"))}, frame="challenge")
    # the sifters
    sifters = ["seedling", "nester", "pollinator", "sentinel", "sprout", "harmonizer"]
    adv("sifter", "enter_sift", rid("sentinel_quill"), {m: killed(rid(m)) for m in sifters[:5]}, any_of=True)
    adv("sprout_enrage", "sifter", rid("lullaby_bloom"), {"enraged": sift_event("sifters_enraged")})
    adv("harmonizer", "sprout_enrage", rid("harmonizer_spawn_egg"), {"killed": killed(rid("harmonizer"))}, frame="goal")
    adv("all_sifters", "harmonizer", rid("pollen_glob"), {m: killed(rid(m)) for m in sifters}, frame="challenge")


# ============================================================================ dimension + worldgen
SKY = "#60d5c8"        # vanilla reveal sky (teal) - from palettes.json sift_sky_vanilla
FOG = "#a8e6dc"
BIOMES = ["sift_meadows", "pallid_grove", "soul_wetlands", "husklands", "lullaby_hills", "sifter_canyons"]


def gen_dimension():
    write_json(f"{D}/dimension_type/the_sift.json", {
        "ambient_light": 0.08,
        "attributes": {
            "minecraft:audio/ambient_sounds": {
                "loop": rid("ambient.sift.loop"),
                "additions": {"sound": rid("ambient.sift.additions"), "tick_chance": 0.006},
                "mood": {"block_search_extent": 8, "offset": 2.0, "sound": rid("ambient.sift.mood"), "tick_delay": 6000}},
            "minecraft:audio/background_music": {"default": {"max_delay": 18000, "min_delay": 6000, "sound": rid("music.sift")}},
            "minecraft:gameplay/bed_rule": {"can_set_spawn": "never", "can_sleep": "never", "error_message": {"translate": "message.sift_unbound.bed"}},
            "minecraft:gameplay/straw_bed_rule": {"can_set_spawn": "never", "can_sleep": "never", "error_message": {"translate": "message.sift_unbound.bed"}},
            "minecraft:gameplay/respawn_anchor_works": False,
            "minecraft:gameplay/can_start_raid": False,
            "minecraft:gameplay/can_pillager_patrol_spawn": False,
            "minecraft:visual/sky_color": SKY,
            "minecraft:visual/fog_color": FOG,
            "minecraft:visual/cloud_color": "#ccf5c9d2",
            "minecraft:visual/cloud_height": 200.0,
            "minecraft:visual/water_fog_color": "#3f9fa8",
            "minecraft:visual/ambient_light_color": "#101818",
            "minecraft:visual/sun_angle": 180.0,
            "minecraft:visual/moon_angle": 180.0,
            "minecraft:visual/ambient_particles": [{"particle": {"type": "minecraft:white_ash"}, "probability": 0.0012}],
        },
        "coordinate_scale": 1.0,
        "default_clock": "minecraft:overworld",
        "has_ceiling": False,
        "has_ender_dragon_fight": False,
        "has_skylight": True,
        "height": 384,
        "infiniburn": "#minecraft:infiniburn_overworld",
        "logical_height": 384,
        "min_y": -64,
        "monster_spawn_block_light_limit": 0,
        "monster_spawn_light_level": {"type": "minecraft:uniform", "max_inclusive": 7, "min_inclusive": 0},
        "timelines": f"#{NS}:in_sift",
    })

    # The three Sift tides (Flow -> Endure -> Thrive), each 8000 ticks of the day clock.
    def kf(*pairs):
        return [{"ticks": t, "value": v} for t, v in pairs]
    flow_sky, endure_sky, thrive_sky = SKY, "#24395c", "#f3a988"
    flow_fog, endure_fog, thrive_fog = FOG, "#2a3f5a", "#f6c3a6"
    write_json(f"{D}/timeline/sift_tides.json", {
        "clock": "minecraft:overworld",
        "period_ticks": 24000,
        "time_markers": {rid("flow_tide"): {"ticks": 0, "show_in_commands": True},
                         rid("endure_tide"): {"ticks": 8000, "show_in_commands": True},
                         rid("thrive_tide"): {"ticks": 16000, "show_in_commands": True}},
        "tracks": {
            "minecraft:visual/sky_color": {"keyframes": kf((600, flow_sky), (7400, flow_sky), (8600, endure_sky), (15400, endure_sky),
                                                              (16600, thrive_sky), (23400, thrive_sky))},
            "minecraft:visual/fog_color": {"keyframes": kf((600, flow_fog), (7400, flow_fog), (8600, endure_fog), (15400, endure_fog),
                                                              (16600, thrive_fog), (23400, thrive_fog))},
            "minecraft:visual/star_brightness": {"keyframes": kf((7400, 0.0), (8600, 0.6), (15400, 0.6), (16600, 0.0))},
            "minecraft:visual/sky_light_factor": {"keyframes": kf((7400, 1.0), (8600, 0.35), (15400, 0.35), (16600, 1.0)), "modifier": "multiply"},
            "minecraft:gameplay/sky_light_level": {"keyframes": kf((7400, 1.0), (8600, 0.3), (15400, 0.3), (16600, 1.0)), "modifier": "multiply"},
            "minecraft:gameplay/monsters_burn": {"keyframes": kf((0, False))},
        },
    })

    write_json(f"{D}/dimension/the_sift.json", {
        "type": rid("the_sift"),
        "generator": {"type": "minecraft:noise", "settings": rid("the_sift"), "biome_source": {"type": "minecraft:multi_noise", "biomes": [
            {"biome": rid("sift_meadows"), "parameters": P(t=(-1, 0.2), h=(-1, 0.1), c=(-0.2, 1), e=(-1, 1), w=(-1, 0.45))},
            {"biome": rid("sifter_canyons"), "parameters": P(t=(-1, 0.2), h=(-1, 0.1), c=(-0.2, 1), e=(-1, 1), w=(0.45, 1))},
            {"biome": rid("pallid_grove"), "parameters": P(t=(-1, 0.2), h=(0.1, 1), c=(-0.2, 1), e=(-1, 1), w=(-1, 1))},
            {"biome": rid("husklands"), "parameters": P(t=(0.2, 1), h=(-1, 0.0), c=(-0.2, 1), e=(-1, 1), w=(-1, 1))},
            {"biome": rid("lullaby_hills"), "parameters": P(t=(0.2, 1), h=(0.0, 1), c=(0.1, 1), e=(-1, -0.2), w=(-1, 1))},
            {"biome": rid("soul_wetlands"), "parameters": P(t=(-1, 1), h=(-1, 1), c=(-1, -0.2), e=(-1, 1), w=(-1, 1))},
            {"biome": rid("lullaby_hills"), "parameters": P(t=(0.2, 1), h=(0.0, 1), c=(0.1, 1), e=(-0.2, 1), w=(-1, 1), off=0.1)},
        ]}}})

    # Noise: reuse the overworld terrain shaper (heights, caves) but with Sift materials, soul water seas and no aquifers.
    ow = json.load(open(os.path.join(VANILLA, "data/minecraft/worldgen/noise_settings/overworld.json")))
    ns = {k: v for k, v in ow.items() if k not in ("aquifers", "debug_functions")}
    ns["default_block"] = {"id": rid("soulshale")}
    ns["default_fluid"] = {"id": rid("soul_water"), "properties": {"level": "0"}}
    ns["sea_level"] = 52
    ns["material_rule"] = rid("the_sift")
    ns["disable_mob_generation"] = False
    write_json(f"{D}/worldgen/noise_settings/the_sift.json", ns)

    def biome_is(*names):
        return {"type": "minecraft:biome", "biome_is": [rid(n) for n in names]}

    def block(name):
        return {"type": "minecraft:block", "result_state": {"id": name}}

    on_floor = "minecraft:on_floor"
    under_floor = "minecraft:under_floor"
    write_json(f"{D}/worldgen/material_rule/the_sift.json", {"type": "minecraft:sequence", "sequence": [
        "minecraft:bedrock_floor",
        {"type": "minecraft:condition", "if_true": {"type": "minecraft:above_preliminary_surface"}, "then_run": {"type": "minecraft:sequence", "sequence": [
            {"type": "minecraft:condition", "if_true": biome_is("husklands"), "then_run": {"type": "minecraft:sequence", "sequence": [
                {"type": "minecraft:condition", "if_true": on_floor, "then_run": block(rid("sift_sand"))},
                {"type": "minecraft:condition", "if_true": under_floor, "then_run": block(rid("sift_sand"))}]}},
            {"type": "minecraft:condition", "if_true": biome_is("lullaby_hills"), "then_run": {"type": "minecraft:sequence", "sequence": [
                {"type": "minecraft:condition", "if_true": on_floor, "then_run": block(rid("sift_sculk"))},
                {"type": "minecraft:condition", "if_true": under_floor, "then_run": block(rid("sift_sculk"))}]}},
            {"type": "minecraft:condition", "if_true": on_floor, "then_run": {"type": "minecraft:sequence", "sequence": [
                {"type": "minecraft:condition", "if_true": "minecraft:not_underwater", "then_run": block(rid("sift_grass_block"))},
                block(rid("sift_soil"))]}},
            {"type": "minecraft:condition", "if_true": under_floor, "then_run": block(rid("sift_soil"))},
        ]}},
    ]})


def P(t, h, c, e, w, off=0.0):
    return {"temperature": list(t), "humidity": list(h), "continentalness": list(c), "erosion": list(e),
            "depth": 0.0, "weirdness": list(w), "offset": off}


def feature(name, obj):
    write_json(f"{D}/worldgen/feature/{name}.json", obj)


def placed(name, feat, placement):
    write_json(f"{D}/worldgen/placed_feature/{name}.json", {"feature": feat, "placement": placement})


def patch_placement(count, spread=7):
    return [{"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"},
            {"type": "minecraft:count", "count": 24},
            {"type": "minecraft:offset", "x": {"type": "minecraft:trapezoid", "max": spread, "min": -spread, "plateau": 0},
             "y": {"type": "minecraft:trapezoid", "max": 3, "min": -3, "plateau": 0},
             "z": {"type": "minecraft:trapezoid", "max": spread, "min": -spread, "plateau": 0}},
            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}}]


def plant_filter(block_id):
    return {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": {"id": block_id}}}


def gen_features():
    leaves = {"id": rid("pallid_leaves"), "properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}}
    log = {"id": rid("pallid_log"), "properties": {"axis": "y"}}
    soil = {"type": "minecraft:rule_based", "rules": [{"if_true": {"type": "minecraft:not", "predicate": {
        "type": "minecraft:matching_block_tag", "tag": "minecraft:cannot_replace_below_tree_trunk"}}, "then": {"id": rid("sift_soil")}}]}
    # A pale, droopy canopy on a straight trunk ("pale tree-like structures" in the reveal).
    feature("pallid_tree", {"type": "minecraft:tree", "below_trunk_provider": soil, "decorators": [], "ignore_vines": True,
        "foliage_placer": {"type": "minecraft:cherry_foliage_placer", "corner_hole_chance": 0.3, "hanging_leaves_chance": 0.45,
                           "hanging_leaves_extension_chance": 0.55, "height": 4, "offset": 0, "radius": 3, "wide_bottom_layer_hole_chance": 0.2},
        "foliage_provider": leaves, "minimum_size": {"type": "minecraft:two_layers_feature_size", "upper_size": 2},
        "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": 7, "height_rand_a": 3, "height_rand_b": 2},
        "trunk_provider": log})
    feature("tall_pallid_tree", {"type": "minecraft:tree", "below_trunk_provider": soil, "decorators": [], "ignore_vines": True,
        "foliage_placer": {"type": "minecraft:cherry_foliage_placer", "corner_hole_chance": 0.25, "hanging_leaves_chance": 0.5,
                           "hanging_leaves_extension_chance": 0.6, "height": 5, "offset": 0, "radius": 4, "wide_bottom_layer_hole_chance": 0.25},
        "foliage_provider": leaves, "minimum_size": {"type": "minecraft:two_layers_feature_size", "upper_size": 2},
        "trunk_placer": {"type": "minecraft:cherry_trunk_placer", "base_height": 9, "height_rand_a": 2, "height_rand_b": 1,
                         "branch_count": {"type": "minecraft:weighted_list", "distribution": [{"data": 1, "weight": 2}, {"data": 2, "weight": 1}]},
                         "branch_end_offset_from_top": {"type": "minecraft:uniform", "max_inclusive": 0, "min_inclusive": -1},
                         "branch_horizontal_length": {"type": "minecraft:uniform", "max_inclusive": 3, "min_inclusive": 2},
                         "branch_start_offset_from_top": {"max_inclusive": -3, "min_inclusive": -4}},
        "trunk_provider": log})
    for t, counts in (("pallid_tree", [(1, 9), (2, 1)]), ("tall_pallid_tree", [(0, 4), (1, 1)])):
        pass
    tree_place = lambda dist: [{"type": "minecraft:count", "count": {"type": "minecraft:weighted_list", "distribution": [{"data": d, "weight": w} for d, w in dist]}},
                               {"type": "minecraft:in_square"}, {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
                               {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"}, {"type": "minecraft:biome"}, plant_filter(rid("pallid_sapling"))]
    placed("pallid_trees_sparse", rid("pallid_tree"), [{"type": "minecraft:rarity_filter", "chance": 3}] + tree_place([(1, 1)]))
    placed("pallid_trees_dense", rid("pallid_tree"), tree_place([(4, 3), (6, 1)]))
    placed("tall_pallid_trees", rid("tall_pallid_tree"), tree_place([(1, 3), (2, 1)]))

    feature("short_sift_grass", {"type": "minecraft:simple_block", "to_place": {"id": rid("short_sift_grass")}})
    feature("tall_sift_grass", {"type": "minecraft:simple_block", "to_place": {"id": rid("tall_sift_grass")}})
    feature("teal_sprouts", {"type": "minecraft:simple_block", "to_place": {"id": rid("teal_sprouts")}})
    feature("lullaby_bloom", {"type": "minecraft:simple_block", "to_place": {"id": rid("lullaby_bloom")}})
    placed("patch_short_sift_grass", rid("short_sift_grass"), patch_placement(6) + [plant_filter(rid("short_sift_grass"))])
    placed("patch_tall_sift_grass", rid("tall_sift_grass"), patch_placement(3) + [plant_filter(rid("tall_sift_grass"))])
    placed("patch_teal_sprouts", rid("teal_sprouts"), patch_placement(2) + [plant_filter(rid("teal_sprouts"))])
    placed("patch_lullaby_bloom", rid("lullaby_bloom"), [{"type": "minecraft:rarity_filter", "chance": 3}] + patch_placement(1, 5) + [plant_filter(rid("lullaby_bloom"))])

    feature("ore_soul_shard", {"type": "minecraft:ore", "discard_chance_on_air_exposure": 0.0, "size": 6, "targets": [
        {"state": {"id": rid("soul_shard_ore")}, "target": {"predicate_type": "minecraft:tag_match", "tag": f"{NS}:soulshale_ore_replaceables"}}]})
    placed("ore_soul_shard", rid("ore_soul_shard"), [{"type": "minecraft:count", "count": 14}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:trapezoid", "max_inclusive": {"absolute": 80}, "min_inclusive": {"absolute": -48}}},
        {"type": "minecraft:biome"}])

    feature("soul_water_lake", {"type": "minecraft:lake", "barrier": {"id": rid("soulshale")}, "can_place_feature": {"type": "minecraft:true"},
        "can_replace_with_air_or_fluid": {"type": "minecraft:not", "predicate": {"type": "minecraft:matching_block_tag", "tag": "minecraft:features_cannot_replace"}},
        "can_replace_with_barrier": {"type": "minecraft:not", "predicate": {"type": "minecraft:matching_block_tag", "tag": "minecraft:lava_pool_stone_cannot_replace"}},
        "fluid": {"id": rid("soul_water"), "properties": {"level": "0"}}})
    placed("soul_water_lake_rare", rid("soul_water_lake"), [{"type": "minecraft:rarity_filter", "chance": 14}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}])
    placed("soul_water_lake_common", rid("soul_water_lake"), [{"type": "minecraft:rarity_filter", "chance": 3}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}])

    feature("husk_rock", {"type": "minecraft:block_blob", "can_place_on": {"type": "minecraft:matching_block_tag", "tag": f"{NS}:sift_ground"},
                          "state": {"id": rid("husk_shell"), "properties": {"axis": "y"}}})
    placed("husk_rocks", rid("husk_rock"), [{"type": "minecraft:count", "count": 2}, {"type": "minecraft:in_square"},
                                            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"}])
    feature("dark_sculk_disk", {"type": "minecraft:disk", "half_height": 1, "radius": {"type": "minecraft:uniform", "max_inclusive": 5, "min_inclusive": 2},
        "state_provider": {"type": "minecraft:rule_based", "fallback": {"id": rid("dark_sift_sculk")}, "rules": []},
        "target": {"type": "minecraft:matching_blocks", "blocks": [rid("sift_sand"), rid("sift_soil"), rid("sift_grass_block")]}})
    placed("dark_sculk_disks", rid("dark_sculk_disk"), [{"type": "minecraft:count", "count": 2}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}])
    feature("soul_water_spring", {"type": "minecraft:spring_feature", "state": {"id": rid("soul_water"), "properties": {"falling": "true"}},
                                  "valid_blocks": [rid("soulshale"), rid("sift_soil"), rid("sift_sculk")]})
    placed("soul_water_springs", rid("soul_water_spring"), [{"type": "minecraft:count", "count": 12}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "max_inclusive": {"absolute": 160}, "min_inclusive": {"above_bottom": 8}}},
        {"type": "minecraft:biome"}])

    # The mod's own feature types (ModFeatures): carved soulshale monoliths and red sculk formations.
    surface = lambda chance: [{"type": "minecraft:rarity_filter", "chance": chance}, {"type": "minecraft:in_square"},
                              {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]
    feature("soulshale_monolith", {"type": rid("monolith"), "toppled_chance": 0.3,
                                   "height": {"type": "minecraft:uniform", "min_inclusive": 6, "max_inclusive": 11}})
    placed("soulshale_monoliths", rid("soulshale_monolith"), surface(9))
    placed("soulshale_monoliths_rare", rid("soulshale_monolith"), surface(24))
    feature("sculk_formation", {"type": rid("sculk_formation"), "radius": 5,
                                "spires": {"type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 5},
                                "spire_height": {"type": "minecraft:uniform", "min_inclusive": 6, "max_inclusive": 13}})
    placed("sculk_formations", rid("sculk_formation"), surface(5))
    placed("sculk_formations_rare", rid("sculk_formation"), surface(16))
    feature("humbler_husk", {"type": rid("humbler_husk"), "size": 16})
    placed("humbler_husks", rid("humbler_husk"), surface(7))
    feature("sifter_nest", {"type": rid("sifter_nest")})
    placed("sifter_nests", rid("sifter_nest"), surface(2))
    placed("sifter_nests_rare", rid("sifter_nest"), surface(14))
    # the Ravines: sculker nests under Singer's Meadow, the Humbler Huskland and the Lullaby Hills
    write_json(f"{D}/worldgen/structure/sculker_nest.json", {"type": rid("sculker_nest"), "biomes": "#" + rid("has_structure/sculker_nest"),
        "step": "underground_structures", "spawn_overrides": {"monster": {"bounding_box": "piece", "spawns": [
            {"type": rid("sculk_prowler"), "weight": 10, "count": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 2}},
            {"type": rid("scavenger"), "weight": 6, "count": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 1}}]}}})
    write_json(f"{D}/worldgen/structure_set/sculker_nests.json", {"placement": {"type": "minecraft:random_spread", "salt": 7341823,
        "separation": 12, "spacing": 30}, "structures": [{"structure": rid("sculker_nest"), "weight": 1}]})
    # the Sifter Canyons: deep, frequent canyons
    write_json(f"{D}/worldgen/carver/sifter_canyon.json", {"type": "minecraft:canyon", "probability": 0.09, "shape": {
        "distance_factor": {"type": "minecraft:uniform", "max_exclusive": 1.0, "min_inclusive": 0.75},
        "horizontal_radius_factor": {"type": "minecraft:uniform", "max_exclusive": 1.4, "min_inclusive": 0.9},
        "thickness": {"type": "minecraft:trapezoid", "max": 8.0, "min": 2.0, "plateau": 3.0},
        "vertical_radius_center_factor": 0.0, "vertical_radius_default_factor": 1.0, "width_smoothness": 3, "y_scale": 3.5},
        "vertical_rotation": {"type": "minecraft:uniform", "max_exclusive": 0.125, "min_inclusive": -0.125},
        "y": {"type": "minecraft:uniform", "max_inclusive": {"absolute": 82}, "min_inclusive": {"absolute": 48}}})


def gen_biomes():
    def spawns(creature=(), monster=(), ambient=()):
        def entries(lst):
            return [{"type": t, "count": {"type": "minecraft:uniform", "min_inclusive": a, "max_inclusive": b}, "weight": w} for t, w, a, b in lst]
        return {"argument": {"spawn_costs": {}, "spawns_by_category": {
            "creature": entries(creature), "monster": entries(monster), "ambient": entries(ambient)}}, "modifier": "overlay"}

    blub = (rid("blub"), 10, 2, 4)
    prowler = (rid("sculk_prowler"), 100, 1, 2)
    wisp = (rid("soul_wisp"), 10, 1, 3)
    nuzzle = (rid("nuzzle"), 8, 2, 4)
    slabber = (rid("slabber"), 5, 1, 2)
    golem = (rid("echo_golem"), 2, 1, 1)
    # the sifters (any light; they are the Sift's own) and the scavenger
    seedling, nester, pollinator = (rid("seedling"), 40, 2, 4), (rid("nester"), 25, 1, 2), (rid("pollinator"), 20, 1, 2)
    sentinel, sprout, harmonizer = (rid("sentinel"), 25, 1, 2), (rid("sprout"), 12, 1, 1), (rid("harmonizer"), 1, 1, 1)
    scavenger = (rid("scavenger"), 60, 1, 2)
    common = ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"]

    # Features in the same step must appear in the same relative order in every biome (else "feature order cycle").
    veg_order = ["sift_unbound:pallid_trees_dense", "sift_unbound:tall_pallid_trees", "sift_unbound:pallid_trees_sparse", "sift_unbound:husk_rocks",
                 "sift_unbound:patch_tall_sift_grass", "sift_unbound:patch_short_sift_grass", "sift_unbound:patch_teal_sprouts", "sift_unbound:patch_lullaby_bloom"]

    local_order = ["sift_unbound:soulshale_monoliths", "sift_unbound:soulshale_monoliths_rare",
                   "sift_unbound:sculk_formations", "sift_unbound:sculk_formations_rare", "sift_unbound:humbler_husks",
                   "sift_unbound:sifter_nests", "sift_unbound:sifter_nests_rare"]

    def biome(name, temp, features_veg, extra_attrs=None, spawn=None, extra_lakes=False, water="#5fd3c8", local=(), carvers=None):
        features_veg = sorted(features_veg, key=veg_order.index)
        feats = [[], ["sift_unbound:soul_water_lake_common" if extra_lakes else "sift_unbound:soul_water_lake_rare"],
                 sorted(local, key=local_order.index), [], [], [],
                 ["sift_unbound:ore_soul_shard", "sift_unbound:dark_sculk_disks"] if name == "husklands" else ["sift_unbound:ore_soul_shard"],
                 [], ["sift_unbound:soul_water_springs"], features_veg, []]
        attrs = {"minecraft:visual/water_fog_color": "#3f9fa8"}
        attrs.update(extra_attrs or {})
        if spawn:
            attrs["minecraft:gameplay/natural_mob_spawns"] = spawn
        write_json(f"{D}/worldgen/biome/{name}.json", {"attributes": attrs, "carvers": carvers or common, "downfall": 0.4,
            "effects": {"water_color": water, "grass_color": "#f17582", "foliage_color": "#9fd8ec"},
            "features": feats, "has_precipitation": False, "temperature": temp})

    veg_meadow = ["sift_unbound:patch_short_sift_grass", "sift_unbound:patch_tall_sift_grass", "sift_unbound:patch_lullaby_bloom",
                  "sift_unbound:patch_teal_sprouts", "sift_unbound:pallid_trees_sparse"]
    biome("sift_meadows", 0.6, veg_meadow, local=["sift_unbound:soulshale_monoliths", "sift_unbound:sifter_nests_rare"], spawn=spawns(creature=[blub, nuzzle, slabber, golem],
          monster=[prowler, seedling, nester, pollinator, sentinel, sprout, harmonizer], ambient=[wisp]))
    biome("pallid_grove", 0.5, ["sift_unbound:pallid_trees_dense", "sift_unbound:tall_pallid_trees", "sift_unbound:patch_short_sift_grass",
                                "sift_unbound:patch_teal_sprouts", "sift_unbound:patch_lullaby_bloom"],
          extra_attrs={"minecraft:visual/ambient_particles": {"argument": [{"particle": {"type": "minecraft:sculk_soul"}, "probability": 0.0006}], "modifier": "append"}},
          spawn=spawns(creature=[(rid("blub"), 12, 2, 5)], monster=[prowler, (rid("seedling"), 20, 2, 3), (rid("sentinel"), 15, 1, 2), (rid("sprout"), 8, 1, 1)],
                       ambient=[(rid("soul_wisp"), 16, 1, 3)]),
          local=["sift_unbound:soulshale_monoliths_rare", "sift_unbound:sculk_formations_rare"])
    biome("soul_wetlands", 0.7, ["sift_unbound:patch_teal_sprouts", "sift_unbound:patch_short_sift_grass", "sift_unbound:pallid_trees_sparse"],
          extra_attrs={"minecraft:visual/ambient_particles": {"argument": [{"particle": {"type": "minecraft:soul"}, "probability": 0.0009}], "modifier": "append"}},
          spawn=spawns(creature=[(rid("blub"), 14, 3, 6), slabber], monster=[(rid("sculk_prowler"), 60, 1, 1), (rid("pollinator"), 20, 1, 2), (rid("sprout"), 8, 1, 1)],
                       ambient=[(rid("soul_wisp"), 30, 2, 4)]),
          extra_lakes=True, local=["sift_unbound:sculk_formations_rare"])
    biome("husklands", 1.2, ["sift_unbound:husk_rocks", "sift_unbound:patch_teal_sprouts"],
          local=["sift_unbound:soulshale_monoliths_rare", "sift_unbound:sculk_formations", "sift_unbound:humbler_husks"],
          spawn=spawns(creature=[(rid("blub"), 6, 1, 3)], monster=[(rid("sculk_prowler"), 120, 1, 3), scavenger, (rid("seedling"), 10, 1, 2)], ambient=[wisp]))
    biome("lullaby_hills", 0.9, ["sift_unbound:patch_teal_sprouts", "sift_unbound:patch_lullaby_bloom", "sift_unbound:patch_short_sift_grass"],
          local=["sift_unbound:soulshale_monoliths"],
          spawn=spawns(creature=[blub, nuzzle, golem], monster=[prowler, (rid("seedling"), 30, 2, 4), (rid("nester"), 15, 1, 2), (rid("pollinator"), 15, 1, 2),
                                                                  (rid("sentinel"), 20, 1, 2), (rid("sprout"), 10, 1, 1), harmonizer], ambient=[wisp]))

    biome("sifter_canyons", 0.6, veg_meadow, local=["sift_unbound:sifter_nests"], carvers=common + [rid("sifter_canyon")],
          spawn=spawns(creature=[blub, nuzzle], monster=[(rid("seedling"), 45, 2, 4), (rid("nester"), 30, 1, 3), (rid("pollinator"), 20, 1, 2),
                                                         (rid("sentinel"), 30, 1, 2), (rid("sprout"), 15, 1, 1), (rid("harmonizer"), 2, 1, 1)],
                       ambient=[wisp]))

VANILLA = None


def main():
    global VANILLA
    ap = argparse.ArgumentParser()
    ap.add_argument("--vanilla", required=True, help="folder holding extracted vanilla assets/ and data/ (26.3)")
    ap.add_argument("--clean", action="store_true", help="delete previously generated assets/data first")
    args = ap.parse_args()
    VANILLA = args.vanilla
    if args.clean:
        for p in (A + "/blockstates", A + "/models", A + "/items", D, DM):
            shutil.rmtree(p, ignore_errors=True)
    Cloner(VANILLA, "cherry", "pallid", WOOD).run(set(WOOD_RECIPES))
    Cloner(VANILLA, "deepslate", "soulshale", STONE).run(set(STONE_RECIPES))
    Cloner(VANILLA, "nether_portal", "sift_portal", PORTAL).run(set())
    gen_handmade_blocks(); gen_items(); gen_damage_type(); gen_lang(); gen_sounds_json(); gen_tags(); gen_recipes(); gen_entity_loot()
    gen_advancements(); gen_dimension(); gen_features(); gen_biomes()
    print(f"wrote {len(WRITTEN)} files")


if __name__ == "__main__":
    main()
