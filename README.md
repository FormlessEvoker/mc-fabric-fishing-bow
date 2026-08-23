# Fishing Bow

A [Fabric](https://fabricmc.net/) mod for Minecraft 26.2 that adds the **Fishing Bow** — a bow that fires an arrow
you reel back in like a fishing line, instead of losing it downrange.

## What it does

- **No ammo required** — draws and fires without any arrows in your inventory.
- **Hooks what it hits.** A non-fatal hit sticks the arrow to the target (it moves with the target as it wanders)
  and drags it toward you when reeled in. A fatal hit — or an item frame/painting knocked loose — captures whatever
  drops and reels that in instead.
- **Right-click to fire, right-click again to reel** — you can't draw the bow again until the current arrow (or its
  catch) is back in hand. A line renders from your hand to the arrow while it's out.
- **64 durability**, crafted from vanilla materials. Costs 1 durability to fire and 1 more to reel — every full
  cycle costs 2, even an "empty" reel that hooked nothing.

Damage is intentionally modest (see [`docs/balancing.md`](docs/balancing.md)): it one-shots fish and chickens at a
strong enough draw, but isn't a viable replacement for a real bow against anything tougher.

## Crafting

Shapeless recipe:

| Ingredients | Result |
|---|---|
| Fishing Rod + Bow | Fishing Bow |

## How it works

- `FishingBowItem` extends vanilla `BowItem` but bypasses its ammo check, and switches right-click between
  "draw the bow" and "reel in the active arrow" based on whether the player already has one out.
- `FishingBowArrow` extends `AbstractArrow` and owns the hook/hit/reel state machine — capturing drops, tracking a
  hooked target, and pulling everything back to the player once reeling starts.
- A `FishingBowArrowRenderer` draws the arrow plus a line back to the owner's hand while it's in flight or hooked.

The mechanism is more involved than it sounds — see [`docs/reeling-mechanic.md`](docs/reeling-mechanic.md) for a
full walkthrough (with diagrams) of the state machine and the client/server + reentrancy bugs that came up building
it.

## Documentation

Design/balance and mechanism docs that don't belong in this README live under [`docs/`](docs/):

- [`docs/balancing.md`](docs/balancing.md) — damage and durability tuning rationale.
- [`docs/reeling-mechanic.md`](docs/reeling-mechanic.md) — how the fire/hook/reel loop actually works, and the bugs
  that shaped it.

## Requirements

- Minecraft 26.2
- Fabric Loader ≥ 0.19.3
- Fabric API
- Java 25

## Building

```
./gradlew build
```

Built jars are output to `build/libs/`.

## License

This project is available under the CC0 license.
