# Progress

## Where things stand

- **What it does so far:** the key. Holding it tells the server this player is placing exactly;
  letting go turns it off. Nothing changes how blocks are placed yet. Fabric and NeoForge on 26.3.
- **Broken:** nothing known.
- **Open question for Gabby:** the key's default, before release (a placeholder, X, for now).
- **Next (0.1.0, scope locked):** session 2, exact placing: no joining (fences, walls, panes, bars),
  straight stairs, dry blocks and the small crosshair mark while the key is held, with in-game
  checks. Session 3, stays put: the locked positions per chunk, synced to players, the piston rule.
  Then release prep. New ideas go to a later version.

## Sessions

| Date | Session | What happened |
|---|---|---|
| 2026-10-20 | Skeleton and the key | The repo, from the multiloader layout (common / fabric / neoforge) with the NeoForge in-game test runner that notes every failed check. The key (`client/ExactKey`, in Controls under its own category) is read at the start of each client tick and sends `ExactMode.Held` to the server when it goes down or comes up; the server keeps who holds it in memory, and forgets a player who leaves. The message is optional on NeoForge, so players without the mod can join. Both jars build; both in-game tests compile; Fabric and NeoForge dedicated servers load the jars with 0 problem lines, alone and with Macaw's Fences and Walls 1.2.1. In-game checks written for both loaders, for CI to run on their own, with Build Better 0.10.0 and with Macaw's Fences and Walls: the key is in Controls; holding it turns exact mode on, on the server and in the player's game; letting go turns it off; sneaking alone doesn't; the key works while sneaking. First CI run: every check passes on both loaders on their own and with Macaw's Fences and Walls. With Build Better both failed: a welcome card from Build Better's library (Rosetta) stayed open over the test, so the NeoForge test never reached the world and the Fabric key presses went to the card. Both tests now close any screen another mod opens once the world has loaded (never the loading screen), until none has opened for two seconds. |
