# Cosmonautics: Warp Drive (`cosmowarp`)

Warp Sable-physicalized structures — ships, stations, anything built on a moving
Create contraption — to any recorded coordinate, in any dimension, using stored
electrical power.

Built for **NeoForge 1.21.1**, as an addon for the Create + Cosmonautics ecosystem.

---

## Features

- **Warp Drive** — a block that, once powered and loaded with a Warp Crystal
  (or Memory Card), can teleport the entire physicalized structure it's part
  of (via Sable) to a saved location, including across dimensions. Its
  console screen shows live info (FE stored, charge countdown, saved
  coordinates, or the selected point's name — configurable).
- **Warp Crystal** — the data core. Insert it into a Warp Drive to record the
  drive's current location, or into a Crystal Driver to copy/delete saved
  locations between crystals. Unlike a Memory Card, a Warp Crystal can
  actually trigger a warp on its own.
- **Memory Card** — a cheaper recording medium that doesn't require a trip to
  the Moon to craft. It can register and store locations just like a Warp
  Crystal, but can't trigger a warp by itself — use a Crystal Driver to copy
  its saved points onto a Warp Crystal first. Storage capacity is
  configurable.
- **Crystal Driver** — a workbench-style block for managing Warp Crystals and
  Memory Cards: duplicate a saved coordinate from one onto the other, or
  delete one from the first.
- **Moon Crystal** — a new resource found only in Cosmonautics' Moon dimension,
  refined with Create's Sand Paper and used to craft Warp Crystals.
- **Hyperspace jump effect** — a charge-up sequence before each warp, with a
  screen-distortion effect (visible only to players actually riding the ship)
  and a converging-particle effect (visible to everyone nearby).

## How it works

1. Mine **Moon Crystal Ore** in the Moon dimension (requires a diamond
   pickaxe). It drops **Raw Moon Crystal**.
2. Refine it into a **Moon Crystal** using Create's Sand Paper (hold the raw
   crystal in one hand, the sand paper in the other, and right-click).
3. Craft a **Warp Crystal** from the Moon Crystal and Titanium Alloy Sheets —
   or, if you don't want to make the trip yet, craft a cheaper **Memory
   Card** instead.
4. Craft a **Warp Drive** and place it as part of a Sable-physicalized
   structure (e.g. a Create contraption/airship).
5. Insert the Warp Crystal (or Memory Card) into the Warp Drive and press
   **Register Here** to save the drive's current location and dimension.
6. Fly (or otherwise physicalize) the structure to another location, insert
   the same crystal/card into a Warp Drive there, and register that location
   too. Repeat as needed — a single crystal or card can hold multiple saved
   points, and each one can be renamed from the Warp Drive's GUI.
7. Back at any saved location, select a destination from the list and press
   **Warp**. The drive consumes FE, plays a short hyperspace charge-up
   sequence, and then moves the whole physicalized structure to the selected
   point. (Only a Warp Crystal can trigger this step — if you've been using a
   Memory Card, copy its points onto a Warp Crystal with a Crystal Driver
   first.)

Use a **Crystal Driver** to copy a saved location from one crystal/card onto
another (put the source in slot 1 and an empty-list crystal/card in slot 2),
or to delete a location from one (slot 1 only, slot 2 empty).

## Requirements

- [NeoForge](https://neoforged.net/) for Minecraft 1.21.1
- [Create](https://www.curseforge.com/minecraft/mc-mods/create)
- [Create Cosmonautics](https://cosmonauticsteam.github.io/) (`rocketnautics`) —
  provides the Moon dimension, Titanium items, and the bundled Dimensional
  Sable support this mod's cross-dimension warp relies on
- [Sable](https://modrinth.com/mod/sable) — the physics/sub-level mod that
  Create Cosmonautics itself depends on
- [Dimensional Sable](https://modrinth.com/mod/dimensional-sable) — cross-dimension
  support for Sable sub-levels. Create Cosmonautics bundles a copy of this, so
  a separate install usually isn't required, but it must be present one way
  or the other.

> **Note:** This mod does not generate FE on its own. Add any mod that
> produces Forge Energy (Create's generators, Mekanism, Thermal, etc.) to
> actually power the Warp Drive.

## Configuration

Common config (`config/cosmowarp-common.toml`):

| Option                 | Default     | Description                                                              |
| ----------------------- | ----------- | ------------------------------------------------------------------------- |
| `warpCostFE`             | `8,000,000` | FE consumed by a single Warp Drive activation.                            |
| `warpChargeTicks`        | `60` (3s)   | Delay between pressing Warp and the actual jump.                          |
| `warpParticleCount`      | `32`        | Particles spawned around the ship per tick while charging.                |
| `warpDriveDisplayMode`   | `FE`        | What the Warp Drive's console screen shows: `FE`, `COUNTDOWN`, `COORDINATES`, or `POINT_NAME`. |
| `memoryCardCapacity`     | `5`         | How many saved locations a single Memory Card can hold (1–10).            |

## A note on development

Large parts of this mod's code were written with the help of AI (Claude).
Design decisions, testing, and final calls are mine, but I wanted to be
upfront about the tooling used.

## Credits

- [**Sable**](https://github.com/ryanhcode/sable) and
  [**Sable Companion**](https://github.com/ryanhcode/sable-companion) by
  **ryanhcode** — the physics/sub-level system this mod's warp mechanic is
  built on.
- [**Dimensional Sable**](https://modrinth.com/mod/dimensional-sable) by
  **hollow_egg** — cross-dimension sub-level support, used directly for the
  actual warp jump.
- [**Create Cosmonautics**](https://github.com/CosmonauticsTeam/Create-Cosmonautics) by the Cosmonautics Team — the Moon dimension, Titanium items, and
  Create/Sable integration this mod builds on top of.
- [**Create**](https://github.com/Creators-of-Create/Create) by the Create
  team.
- We received the warpdrive model and textures! Thank you so much!!

## License

[MIT](https://github.com/waco0311/cosmowarp/blob/main/LICENSE)
