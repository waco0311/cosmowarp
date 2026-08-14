# beta.5 pre-release checklist

## What's in this package
Everything needed for both the hyperspace stuck-effect fix AND the advancement system,
fully wired together (registerHere()/executeWarp() now fire the achievement triggers directly --
no more manual wiring needed for those two spots).

- `WarpDriveBlockEntity.java` (full replacement) --
  - `chargingEffectPlayers` now persists to NBT (the actual bug fix)
  - `registerHere()` now returns `boolean` (true on actual success)
  - `executeWarp()` fires `cosmowarp:warp_performed` (with cross-dimension flag) for every player
    who made the trip, right after `WarpSubLevel()` succeeds
- `ModNetworking.java` (full replacement) -- `REGISTER_HERE` case now fires
  `cosmowarp:register_location` when `registerHere()` returns true
- `WarpEffectClient.java` (full replacement) -- `clear()` + automatic safety net
- `ModClientCommands.java` (new) -- `/cosmowarp clearhyperspace`
- `advancement/` (3 Java files) -- the two custom triggers + their DeferredRegister
- `data/cosmowarp/advancement/*.json` (8 files) -- the advancement tree
- `LANG_SNIPPET_en_us.json` -- entries to merge in (not a full lang file)

## Remaining wiring (can't be done without your project open)
1. `ModTriggers.TRIGGER_TYPES.register(modEventBus);` -- once, in your main mod constructor
2. `WarpEffectClient.registerSafetyNet();` -- once, in client setup
3. `modEventBus.addListener(ModClientCommands::register);` -- once, in your main mod constructor
4. Merge `LANG_SNIPPET_en_us.json`'s contents into your real `en_us.json`
5. Double-check the item/block IDs referenced in the advancement JSONs
   (`warp_crystal`, `memory_card`, `moon_crystal`, `crystal_driver`, `warp_drive`) match your
   actual `ModItems`/`ModBlocks` registry names

## Before tagging beta.5
- [ ] Build clean, no compile errors from the above
- [ ] Re-test the original double-warp repro (charge two drives on the same physicalized
      structure, let one finish first) -- effect should now turn off correctly, and the advancement
      toasts should fire ("Point of No Return" -> "Engage!" -> "Beam Me Up" for a cross-dimension one)
- [ ] `/cosmowarp clearhyperspace` visibly clears the effect if you force-trigger the old bug
      some other way (sanity check that the command itself works)
- [ ] Memory Card v3 texture actually copied into `assets/cosmowarp/textures/item/memory_card.png`
      (or wherever your item textures live) -- this was delivered as a standalone PNG earlier and
      needs to actually replace the old texture file if that hasn't happened yet
- [ ] Test Drive confirmed absent from the creative tab (already the case per your own notes, just
      flagging it since it's the thing you're consciously deferring)

## Known-open items you're consciously carrying past this release
- Warp Drive model seam (reproduction pending, not something to chase further right now)
- Model credit attribution name (waiting on confirmation from the original creator)
- Test Drive (deferred to a future beta, as discussed)
- The `BlockPos`-collision latent bug noted in the hyperspace fix (separate, rarer issue --
  optional follow-up, not a blocker)

None of the open items above block a beta.5 release on their own.
