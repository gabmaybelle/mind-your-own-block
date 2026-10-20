# Mind Your Own Block

A Fabric and NeoForge mod for Minecraft 26.3 (mod ID `mind_your_own_block`): hold a key while
placing and the block keeps the shape you placed it in. Fences, walls, panes and bars don't join
their neighbours, stairs don't bend into corners, and blocks placed in water stay dry, until the
block is broken. Invisible vanilla: no new blocks or items, no settings screen, no wheel or menu.
`PROGRESS.md` says what's done, what's broken and what's next, with a row per session: read it
first, and add this session's row before ending.

## The design, in short

- **The key** (`client/ExactKey`): its own rebindable key, never sneak, so it works on scaffolding
  (where sneaking climbs down) and while sneaking on a roof. Read at the start of every client tick,
  before the game handles clicks; when it goes down or comes up the game sends one small message
  (`ExactMode.Held`) to the server. On a server without the mod the key does nothing.
- **Exact mode** (`ExactMode`): the server keeps the players holding the key, in memory only, by
  player id. Messages on one connection arrive in order, so the server knows before that player's
  block is placed. `ExactMode.isOn(player)` answers on either side (the player's own game answers
  from the key itself, so its prediction matches the server).
- **Placing** (planned, session 2): one hook where a block item works out the state it places. In
  exact mode it clears the join sides (fences, walls, panes, bars), sets stairs straight and sets
  waterlogged to false, reading block properties, never block names, so modded blocks built on
  vanilla's work too. Buckets are never touched.
- **Stays put** (planned, session 3): each chunk keeps a small list of locked positions (the
  loaders' chunk-data attachments, saved and synced to players); a locked block keeps its state when
  a neighbour changes. The lock clears when the block at that spot becomes a different block
  (broken, replaced, pushed by a piston). A normal fence beside a locked post still reaches toward
  it; a water bucket still fills a locked dry block.
- **Nothing runs every tick** on the server. Work happens only when someone places or a neighbour
  changes.

## Layout

One codebase, two jars:

- `common/`: everything that doesn't care which loader it runs on. `MindYourOwnBlock.init()` is the
  one entrypoint; `Platform` is the tiny loader abstraction (set by each loader's client entrypoint).
  All data files live in `common/src/main/resources`. Never import Fabric or NeoForge here;
  `./gradlew :common:build` fails if you do.
- `fabric/` and `neoforge/`: the entrypoints (one for both sides, one for the player's game).
  Their `build.gradle` compiles `common`'s sources straight into their jar.
- `fabric/src/gametest/`: the Fabric client game test (`MindYourOwnBlockClientTest`).
- `neoforge/src/clienttest/`: the NeoForge in-game test: `ClientTest` runs steps one tick at a time,
  notes every failed check and crashes the game at the end if any failed;
  `MindYourOwnBlockClientTest` holds the same checks as the Fabric one.

## Build and test

- `./gradlew build`: jars in `fabric/build/libs/` and `neoforge/build/libs/`.
- `./gradlew :fabric:runClientGameTest`: the in-game checks in a real game window (a minute or two).
  `./gradlew :neoforge:runClientTest`: the same on the NeoForge jar. Add `-PtestBuildBetter` or
  `-PtestMacawsFences` to either to play it with that mod installed (fetched from Modrinth's maven
  by version id in `gradle.properties`, never bundled); CI runs all three.

## Gotchas

- **The common sources are compiled twice**, once per loader. A class that compiles on Fabric but
  not NeoForge (or the reverse) is a common class that leaked a loader import.
- **26.3 renumbered keys** (and moved from GLFW to SDL3): read key numbers while the game runs
  (`InputConstants.getKey("key.keyboard.x").getValue()`), never compile a constant in.
- Vanilla names changed a lot in 26.x: check the merged jar rather than older docs.
- **Build Better shows welcome cards** (through its library, Rosetta) the first time a world loads.
  Both in-game tests close any screen another mod opens after the world loads
  (`closeOtherModsScreens`), or the cards sit over the test and catch its key presses.

## This mod's job

**Mind Your Own Block**: hold a key while placing and the block keeps the shape you placed it in
(no fence joins, no stair corners, no waterlogging) until it's broken.

Gabby has other mods, each with its own job. Add only features that fit this mod's job. If an
idea doesn't fit, don't add it here or start it in another repo: ask Gabby where it goes.

## Working on Gabby's mods (the same in every repo)

Gabby makes these mods with help from Claude. Several Claude sessions work on them: one on her
computer and others in the cloud. These rules keep their work fitting together.

**One session per repo at a time.**
- A cloud session works on a `claude/...` branch and never pushes to `main`. Gabby's local
  session reviews the branch, runs its tests and merges it as one commit.
- Start from the latest `main` (`git pull`), and say in the branch's last commit or a short note
  what is finished and what isn't.

**Commits are made one at a time, by hand.** Never commit or push from a chained script. Run
the test, read its result (and the screenshot, for anything visual), then commit that one
branch, then push it. A cherry-pick that conflicts stops the work until it is resolved by hand.
A worldgen or balance change is tested on every terrain in the branch's matrix before it goes
to any other branch. (Learned 2026-10-14, when scripted chains committed a conflicted
cherry-pick, put one branch's files on another, and sent an untested ore change to all three.)
For a Gradle game test use a game watcher that reports a verdict (in the world, error screen, crashed or hung), not a wait loop.

**Commits.**
- Author and committer are always `gabmaybelle <211247938+gabmaybelle@users.noreply.github.com>`:
  `git -c user.name=gabmaybelle -c user.email=211247938+gabmaybelle@users.noreply.github.com commit ...`.
  Never a personal email, and never `Claude <noreply@anthropic.com>` as the author. Claude goes in
  a `Co-Authored-By:` line at the end of the message.
- Ask Gabby before committing or pushing when she's around. Finish the build and the tests first.
- Commit messages say what changed for players, in plain sentences.

**Privacy.** These repos are public. Don't add personal email addresses, real names other than
"Gabby", or anything about her other private projects and modpacks: not in code, docs, commits
or test names. Check the diff for these before every push.

**Tests.**
- `./gradlew build` and `./gradlew :fabric:runClientGameTest` must pass before a commit. The game
  test opens a real game window for a minute or two.
- A new or changed check should fail with the feature switched off: run it that way once, then
  put the code back. A check that compares against the setting it tests proves nothing.
- A data change (worldgen, loot, a template) gets the same proof: run the part of the test that
  sees it, on every terrain in the branch's matrix, and quote the numbers in the progress row.
- On Linux without a screen, run game tests under `xvfb-run -a`. 26.3 also needs
  `SDL_VIDEO_FORCE_EGL=1` with `libegl1 libegl-mesa0` installed, or the game hangs at startup.

**Writing.**
- README and CHANGELOG are for players: plain, friendly, no jargon. New player-facing changes
  go in the CHANGELOG's newest section.
- Keep the credit line: "made by Gabby with help from Claude, an AI assistant".
- Gabby publishes on Modrinth herself. Don't upload anything there.
**Minecraft.** Fabric and NeoForge (Mojang names; Loom 1.18.4, ModDevGradle 2.0, Java 25).
`main` is the newest Minecraft (26.3) and the only line: no older versions are planned.
Vanilla names changed a lot in 26.x: check the merged jar rather than older docs.
