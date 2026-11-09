#!/usr/bin/env python3
"""Builds the wiki/ folder (GitHub-flavoured Markdown) from the mod's own data.

Names come from the lang file, recipes / drops / tags from data/the_rift, textures from the generated PNGs, so the
wiki never drifts from the code. Hand-written facts (hardness, where to find things, behaviour) live in INFO below.

Run from the repository root:  python3 tools/gen_wiki.py
"""
import glob
import json
import os
from collections import defaultdict

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
A = os.path.join(RES, "assets", "the_rift")
D = os.path.join(RES, "data", "the_rift")
WIKI = os.path.join(ROOT, "wiki")
LANG = json.load(open(os.path.join(A, "lang", "en_us.json")))
TEX_REL = "../src/main/resources/assets/the_rift/textures"


def name(i):
    p = i.split(":")[-1]
    for k in (f"block.the_rift.{p}", f"item.the_rift.{p}", f"entity.the_rift.{p}"):
        if k in LANG:
            return LANG[k]
    if i.startswith("minecraft:") or i.startswith("#"):
        return i.replace("minecraft:", "").replace("_", " ").title() if not i.startswith("#") else f"any `{i[1:]}`"
    return p.replace("_", " ").title()


def slug(n):
    return n.replace(" ", "-")


def link(i):
    if i.startswith("the_rift:"):
        p = i.split(":")[1]
        page = PAGE_OF.get(p)
        if page:
            return f"[{name(i)}]({page}#{slug(name(i)).lower()})"
    return name(i)


# ------------------------------------------------------------------ facts that are not in the data files
STONE = {"hardness": "3.5", "tool": "Pickaxe (wood+)", "blast": "6"}
INFO = {
    # terrain
    "sift_grass_block": {"hardness": "0.6", "tool": "Shovel", "found": "Surface of Sift Meadows, Pallid Grove and Soul Wetlands",
        "desc": "The soft salmon-pink grass that carpets the Sift - the colour shown in Mojang's Minecraft LIVE reveal. Spreads onto nearby Sift Soil in light, turns back into soil when covered, and sprouts Sift plants when bone-mealed. Blubs and Sculk Prowlers spawn on it."},
    "sift_soil": {"hardness": "0.5", "tool": "Shovel", "found": "Under the grass of most Sift biomes", "desc": "Dusky rose earth. Sift grass spreads onto it; trees and plants grow on it."},
    "sift_sculk": {"hardness": "0.6", "tool": "Hoe", "light": "1", "found": "Surface of the Lullaby Hills",
        "desc": "The red, 'healthy' sculk that covers the Sift's hills. In Minecraft Dungeons II the Sift is the original home of sculk; this block is the mod's take on that red living ground. Glows very faintly."},
    "dark_sift_sculk": {"hardness": "0.6", "tool": "Hoe", "light": "2", "found": "Patches in the Husklands", "desc": "Darker, starved sculk with cyan sparks, found in patches across the sandy Husklands."},
    "sift_sand": {"hardness": "0.5", "tool": "Shovel", "found": "Surface of the Husklands", "desc": "Pale, sun-bleached sand. Falls like sand and smelts into glass. Carapace blubs spawn on it."},
    "husk_shell": {"hardness": "2", "tool": "Pickaxe", "found": "Boulders scattered across the Husklands",
        "desc": "Layered, bone-like plates - the remains of giant husks that dot the Husklands. Placed like a log (it has an axis). Craft one into 6 bone meal."},
    "soulshale": {"hardness": "3", "tool": "Pickaxe (wood+)", "blast": "6", "found": "The Sift's bedrock-to-surface base stone",
        "desc": "The dark teal stone the whole dimension is made of (the 26.4 snapshot added a decorative 'chiseled soulshale' block to Minecraft; this mod builds a full stone family around the name). Drops Cobbled Soulshale unless mined with Silk Touch."},
    "cobbled_soulshale": dict(STONE, found="Mining Soulshale", desc="Broken soulshale. Smelt it back into Soulshale; craft it into stairs, slabs, walls, Polished Soulshale and bricks. Works for stone tools and furnaces."),
    "polished_soulshale": dict(STONE, found="Crafting", desc="Smooth bevelled soulshale. Also the platform the mod builds under return portals in the Sift."),
    "soulshale_bricks": dict(STONE, found="Crafting", desc="Neat dark-teal bricks."),
    "chiseled_soulshale": dict(STONE, light="3", found="Crafting (2 Cobbled Soulshale Slabs) or stonecutter",
        desc="Carved with a glowing 'resonance' ring and a little singing figure - a nod to the Singer of the Sift. Gives off a faint light."),
    "soul_shard_ore": {"hardness": "4", "tool": "Pickaxe (stone+)", "blast": "6", "light": "4", "found": "Inside Soulshale, Y -48 to 80, 14 veins per chunk",
        "desc": "Soulshale threaded with crystallised souls. Drops 1-2 Soul Shards (Fortune works) and 2-5 XP."},
    "soul_shard_block": {"hardness": "3", "tool": "Pickaxe (stone+)", "blast": "6", "light": "10", "found": "Crafting", "desc": "Nine soul shards fused into a softly glowing block."},
    # plants
    "short_sift_grass": {"hardness": "0", "tool": "Shears", "found": "Meadows, groves, wetlands, hills",
        "desc": "Salmon tufts (the 'soft orange grasses' of the reveal). Bone meal grows it into Tall Sift Grass. Only drops with shears or Silk Touch."},
    "tall_sift_grass": {"hardness": "0", "tool": "Shears", "found": "Sift Meadows", "desc": "Two-block-tall salmon grass. Shearing gives two Short Sift Grass."},
    "teal_sprouts": {"hardness": "0", "tool": "Shears", "found": "Every Sift biome", "desc": "The teal vegetation that grows on the Sift's red ground in Dungeons II. Blubs love it (it's a breeding food). Two make cyan dye."},
    "lullaby_bloom": {"hardness": "0", "tool": "Any", "light": "6", "found": "Sift Meadows, Pallid Grove, Lullaby Hills",
        "desc": "A drooping pale-blue bell flower that glows. Blub breeding food; makes light blue dye; can be potted; gives Night Vision in suspicious stew."},
    # wood
    "pallid_log": {"hardness": "2", "tool": "Axe", "found": "Pallid trees", "desc": "Pale blue-white bark from the Sift's 'pale tree-like structures'. Strip it with an axe."},
    "pallid_leaves": {"hardness": "0.2", "tool": "Hoe / Shears", "found": "Pallid trees", "desc": "A droopy, pale-blue canopy with hanging tendrils. Drops Pallid Saplings and sticks; sheds white ash particles."},
    "pallid_sapling": {"hardness": "0", "tool": "Any", "found": "Pallid Leaves", "desc": "Grows a Pallid Tree (80%) or a tall branching Pallid Tree (20%). Plant on Sift grass/soil (or any dirt-like block)."},
    "pallid_planks": {"hardness": "2", "tool": "Axe", "found": "Crafting", "desc": "Pale, almost white planks. Uses the cherry wood sounds. Works for every recipe that takes planks."},
    # special
    "soul_water": {"hardness": "100 (liquid)", "tool": "Bucket", "light": "5", "found": "Lakes, springs and the seas below Y 52 in the Sift",
        "desc": "See [Soul Water](Soul-Water.md)."},
    "sift_portal": {"hardness": "Unbreakable", "tool": "-", "light": "12", "found": "Inside an awakened Warden Portal frame", "desc": "See [Warden Portal](Warden-Portal.md)."},
}
FAMILY_NOTES = {
    "stairs": "Stairs version. Craftable or stonecutter-able like vanilla stairs.",
    "slab": "Slab version.",
    "wall": "Wall version.",
    "wood": "Bark on all six sides.",
    "stripped": "Stripped with an axe.",
    "fence": "Fence.", "fence_gate": "Fence gate (cherry sounds).", "door": "Door - opens by hand, two blocks tall.",
    "trapdoor": "Trapdoor.", "button": "Wooden button (stays pressed 1.5 s).", "pressure_plate": "Wooden pressure plate.",
}

PAGES = {
    "Terrain-Blocks.md": ("Terrain blocks", ["sift_grass_block", "sift_soil", "sift_sculk", "dark_sift_sculk", "sift_sand", "husk_shell", "soul_shard_ore"]),
    "Soulshale.md": ("Soulshale stone family", ["soulshale", "cobbled_soulshale", "cobbled_soulshale_stairs", "cobbled_soulshale_slab", "cobbled_soulshale_wall",
                                                "polished_soulshale", "polished_soulshale_stairs", "polished_soulshale_slab", "polished_soulshale_wall",
                                                "soulshale_bricks", "soulshale_brick_stairs", "soulshale_brick_slab", "soulshale_brick_wall",
                                                "chiseled_soulshale", "soul_shard_block"]),
    "Plants.md": ("Plants", ["short_sift_grass", "tall_sift_grass", "teal_sprouts", "lullaby_bloom", "pallid_sapling"]),
    "Pallid-Wood.md": ("Pallid wood", ["pallid_log", "pallid_wood", "stripped_pallid_log", "stripped_pallid_wood", "pallid_planks", "pallid_leaves",
                                       "pallid_stairs", "pallid_slab", "pallid_fence", "pallid_fence_gate", "pallid_door", "pallid_trapdoor",
                                       "pallid_button", "pallid_pressure_plate"]),
}
PAGE_OF = {b: f for f, (_, bs) in PAGES.items() for b in bs}
PAGE_OF.update({"soul_water": "Soul-Water.md", "sift_portal": "Warden-Portal.md", "echo_tuner": "Items.md", "soul_shard": "Items.md",
                "blub_jelly": "Items.md", "soul_water_bucket": "Items.md", "blub_bucket": "Items.md", "blub_spawn_egg": "Items.md",
                "soul_wisp_spawn_egg": "Items.md", "sculk_prowler_spawn_egg": "Items.md"})


# ------------------------------------------------------------------ data readers
def load_recipes():
    by_result = defaultdict(list)
    uses = defaultdict(list)
    for f in sorted(glob.glob(f"{D}/recipe/*.json")):
        r = json.load(open(f))
        res = r.get("result", {}).get("id")
        if res:
            by_result[res.split(":")[-1]].append(r)
        txt = json.dumps({k: v for k, v in r.items() if k != "result"})
        for m in set(__import__("re").findall(r'the_rift:[a-z_]+', txt)):
            uses[m.split(":")[1]].append(r)
    return by_result, uses


def ingredient(x):
    if isinstance(x, list):
        return " or ".join(ingredient(i) for i in x)
    return link(x) if isinstance(x, str) else str(x)


def recipe_md(r):
    t = r["type"].split(":")[1]
    res = r["result"]
    out = f"{res.get('count', 1)}x {name(res['id'])}"
    if t == "crafting_shaped":
        grid = "<br>".join("`" + row.replace(" ", "·") + "`" for row in r["pattern"])
        keys = ", ".join(f"`{k}` = {ingredient(v)}" for k, v in r["key"].items())
        return f"Crafting table: {grid}<br>{keys} -> **{out}**"
    if t == "crafting_shapeless":
        return f"Crafting (shapeless): {' + '.join(ingredient(i) for i in r['ingredients'])} -> **{out}**"
    if t == "stonecutting":
        return f"Stonecutter: {ingredient(r['ingredient'])} -> **{out}**"
    if t in ("smelting", "blasting", "smoking"):
        return f"{t.title()}: {ingredient(r['ingredient'])} -> **{out}**"
    return f"{t}: -> **{out}**"


def drops(block):
    f = f"{D}/loot_table/blocks/{block}.json"
    if not os.path.exists(f):
        return "Nothing"
    txt = json.dumps(json.load(open(f)))
    items = sorted(set(__import__("re").findall(r'"name": "([a-z_]+:[a-z_]+)"', txt)))
    silk = "can_silk_touch" in txt
    shear = "can_shear" in txt
    s = ", ".join(name(i) for i in items)
    if silk and len(items) > 1:
        s += " (itself with Silk Touch)"
    if shear:
        s += " (needs shears or Silk Touch)"
    return s


def tags_of(block):
    out = []
    for f in glob.glob(f"{RES}/data/*/tags/block/**/*.json", recursive=True):
        if f"the_rift:{block}\"" in open(f).read():
            ns = f.split("/data/")[1].split("/")[0]
            out.append(f"`#{ns}:{f.split('/tags/block/')[1][:-5]}`")
    return ", ".join(sorted(out))


def texture_for(block):
    for cand in (block, f"{block}_top", f"{block}_side", block.replace("_wood", "_log"), block.replace("_stairs", "").replace("_slab", "").replace("_wall", ""),
                 block.replace("_brick_stairs", "_bricks").replace("_brick_slab", "_bricks").replace("_brick_wall", "_bricks"),
                 block.replace("_fence_gate", "_planks").replace("_fence", "_planks").replace("_button", "_planks").replace("_pressure_plate", "_planks").replace("_stairs", "_planks").replace("_slab", "_planks"),
                 block + "_bottom", "tall_sift_grass_top"):
        if os.path.exists(f"{A}/textures/block/{cand}.png"):
            return f"{TEX_REL}/block/{cand}.png"
        if os.path.exists(f"{A}/textures/item/{cand}.png"):
            return f"{TEX_REL}/item/{cand}.png"
    return None


def family_note(block):
    for k in ("fence_gate", "pressure_plate", "stairs", "slab", "wall", "button", "trapdoor", "door", "fence", "stripped", "wood"):
        if k in block and block not in INFO:
            return FAMILY_NOTES[k]
    return None


# ------------------------------------------------------------------ pages
def block_section(b, recipes, uses):
    info = INFO.get(b, {})
    n = name(f"the_rift:{b}")
    tex = texture_for(b)
    lines = [f"### {n}", ""]
    if tex:
        lines += [f'<img src="{tex}" width="64" height="64" style="image-rendering: pixelated" alt="{n} texture">', ""]
    desc = info.get("desc") or family_note(b) or ""
    if desc:
        lines += [desc, ""]
    lines += ["| Property | Value |", "|---|---|", f"| ID | `the_rift:{b}` |"]
    if "hardness" in info:
        lines.append(f"| Hardness | {info['hardness']} |")
    if "blast" in info:
        lines.append(f"| Blast resistance | {info['blast']} |")
    if "tool" in info:
        lines.append(f"| Best tool | {info['tool']} |")
    if "light" in info:
        lines.append(f"| Light level | {info['light']} |")
    if "found" in info:
        lines.append(f"| Where to find | {info['found']} |")
    lines.append(f"| Drops | {drops(b)} |")
    t = tags_of(b)
    if t:
        lines.append(f"| Tags | {t} |")
    lines.append("")
    rs = recipes.get(b, [])
    if rs:
        lines += ["**How to get it**", ""] + [f"- {recipe_md(r)}" for r in rs] + [""]
    us = [r for r in uses.get(b, []) if r.get("result", {}).get("id", "").split(":")[-1] != b]
    if us:
        lines += ["**Used to make**: " + ", ".join(sorted({name(r['result']['id']) for r in us})), ""]
    return "\n".join(lines)


def write(page, text):
    os.makedirs(WIKI, exist_ok=True)
    with open(os.path.join(WIKI, page), "w") as f:
        f.write(text.rstrip() + "\n")


NAV = ("[Home](Home.md) · [The Sift (research)](The-Sift-Research.md) · [Warden Portal](Warden-Portal.md) · [Biomes & Tides](Biomes-and-Tides.md) · "
       "[Terrain](Terrain-Blocks.md) · [Soulshale](Soulshale.md) · [Plants](Plants.md) · [Pallid Wood](Pallid-Wood.md) · [Soul Water](Soul-Water.md) · "
       "[Mobs](Mobs.md) · [Items](Items.md) · [Advancements](Advancements.md) · [Sounds](Sounds.md) · [Art pipeline](Art-Pipeline.md) · [Credits](Credits.md)")


def gen_block_pages():
    recipes, uses = load_recipes()
    for page, (title, blocks) in PAGES.items():
        body = [f"# {title}", "", NAV, ""]
        body += ["| | Block |", "|---|---|"]
        for b in blocks:
            tex = texture_for(b)
            img = f'<img src="{tex}" width="24" height="24" alt="">' if tex else ""
            body.append(f"| {img} | [{name('the_rift:' + b)}](#{slug(name('the_rift:' + b)).lower()}) |")
        body.append("")
        for b in blocks:
            body.append(block_section(b, recipes, uses))
        write(page, "\n".join(body))


def gen_items_page():
    recipes, _ = load_recipes()
    items = [
        ("echo_tuner", "Tool (16 uses)", "Rare. Use it on - or inside - a reinforced-deepslate Warden Portal frame (the one in the middle of every ancient city) to play the melody that awakens it. One use per awakening. Also found in ~12% of ancient city chests."),
        ("soul_shard", "Material", "Crystallised souls from Soul Shard Ore and Sculk Prowlers. Nine make a Block of Soul Shard."),
        ("blub_jelly", "Food (3 hunger, 0.4 saturation)", "A wobbly treat dropped by blubs (0-1, Looting adds more). Always edible; makes you Glow for 10 seconds."),
        ("soul_water_bucket", "Bucket", "Scoop up Soul Water. Use it on a Blub to carry it in a Bucket of Blub."),
        ("blub_bucket", "Bucket", "A blub in a bucket of soul water. Empty it to release the blub (keeps its look, name and health)."),
        ("blub_spawn_egg", "Spawn egg", "Spawns a Blub."), ("sculk_prowler_spawn_egg", "Spawn egg", "Spawns a Sculk Prowler."), ("soul_wisp_spawn_egg", "Spawn egg", "Spawns a Soul Wisp."),
    ]
    body = ["# Items", "", NAV, ""]
    for i, kind, desc in items:
        n = name(f"the_rift:{i}")
        body += [f"### {n}", "", f'<img src="{TEX_REL}/item/{i}.png" width="64" height="64" alt="{n} texture">', "", desc, "",
                 "| Property | Value |", "|---|---|", f"| ID | `the_rift:{i}` |", f"| Type | {kind} |", ""]
        for r in recipes.get(i, []):
            body += [f"- {recipe_md(r)}", ""]
    write("Items.md", "\n".join(body))


if __name__ == "__main__":
    gen_block_pages()
    gen_items_page()
    print("wiki pages generated in", os.path.relpath(WIKI, ROOT))
