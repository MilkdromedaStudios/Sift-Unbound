# Release plan

## Where things stand (v0.1.0)

| Area | Status |
|---|---|
| Build | `./gradlew build` produces `sift-unbound-0.1.0+mc26.3.jar` (+ sources jar). CI runs it on every push. |
| Automated tests | **8 server GameTests** (portal on the real ancient-city template, small frame + collapse, no-frame, soul water damage + blub immunity, portal is harmless without the Sift, bucket round-trip, bone meal) and **1 client GameTest** (real world: pig + player travel through a portal into the Sift, return portal check, screenshots of every tide, the mobs and a portal). |
| Static checks | `tools/check_assets.py`: every model/texture/sound reference resolves, every block/item/entity has a name. |
| Manual QA still to do | Survival playthrough: find an ancient city, craft the tuner, travel, survive an Endure Tide, breed blubs. Check multiplayer (dedicated server + 2 clients). |

## Release checklist

1. Run the manual QA above on a fresh survival world and a dedicated server. Note anything odd in an issue.
2. Update `CHANGELOG.md`: rename `## [0.1.0] - Unreleased` to today's date.
3. Make sure `mod_version` in `gradle.properties` is the version you're releasing.
4. Tag and push:

   ```bash
   git tag v0.1.0
   ```

   ```bash
   git push origin v0.1.0
   ```

   Or, without pushing a tag: **Actions → release → Run workflow**, enter the version (e.g. `0.1.0`) and the branch or commit; the
   workflow creates the `v<version>` tag itself.
5. The **release** workflow checks the tag matches `mod_version`, rebuilds, runs the asset checks, server GameTests and the server
   smoke test, then creates the GitHub release with the jar and the changelog section as notes.
6. Download the jar from the release and smoke-test it in a normal (non-dev) Fabric install.

## Distribution (after the first GitHub release)

* **Modrinth** (automatic): create the project page by hand once (summary, the `wiki/screenshots`, the credits and
  "unofficial, not affiliated with Mojang" notice, Apache-2.0 licence). Then, in the repository's
  *Settings -> Secrets and variables -> Actions*, add:
  * the secret **`MODRINTH_API_KEY`**: a Modrinth personal access token with the *Create versions* and *Read projects* scopes;
  * optionally the variable **`MODRINTH_PROJECT`**: the workflows already use the project ID **`w1FbpX4X`**; set this
    only to publish somewhere else.

  From then on every run of `release.yml` also uploads the jar to Modrinth (`tools/publish_modrinth.py`: Fabric,
  Minecraft from `gradle.properties`, Fabric API as a required dependency, the changelog section as notes). To upload a
  release that already exists, such as v0.1.0, run the **modrinth** workflow by hand with its version. A version that is
  already on Modrinth is skipped, and without the secret the step is skipped.
* **CurseForge**: same idea with a `CURSEFORGE_TOKEN` secret when a project page exists.
* Mark it **client and server** required.
* Keep the name "The Sift Unbound" and avoid Mojang logos or "Minecraft" in the title, per Mojang's usage guidelines.

## Versioning

* `0.x` while the official Sift isn't out. Bump MINOR for new content and PATCH for fixes.
* The jar version includes the Minecraft version (`+mc26.3`). When porting to a new Minecraft version, branch per version
  (`mc/26.3`, `mc/26.4`) and keep `main` on the newest.

## Roadmap

| Version | Planned |
|---|---|
| 0.2 | Pallid signs, hanging signs and boats; Soul Water cauldron; a "Singer's Shrine" structure in Sift Meadows; Lullaby Hills terrain with steeper terraces. |
| 0.3 | More Sift creatures (trill-like flyers, a tuner companion), a Sculk Prowler den structure, tide-specific events (Thrive Tide boosts plant growth). |
| 0.4 | Port to Minecraft 26.4 (watch the snapshots for official Sift blocks such as `minecraft:chiseled_soulshale`). |
| 2027 | When Mojang ships the real Sift, decide whether to retire the mod or turn it into an "extra content" add-on that uses the official dimension. |

## Known limitations

* Portals are linked by searching 64 blocks around the 1:1 position, so two ancient cities less than ~128 blocks apart in X/Z
  may share a Sift portal.
* Return portals in the Overworld are built on the surface if no portal is near, not back in the ancient city.
* Soul Water uses the generic water-like swimming physics from Fabric's fluid API (no custom slowness yet).
