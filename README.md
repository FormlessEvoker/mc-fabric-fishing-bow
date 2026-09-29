# Fishing Bow

A [Fabric](https://fabricmc.net/) mod for Minecraft 26.2 that adds the **Fishing Bow** — a bow that fires an arrow
you reel back in like a fishing line, instead of losing it downrange.

## What it does

- **No ammo required** — draws and fires without any arrows in your inventory.
- **Hooks what it hits.** A surviving creature carries a rendered arrow at the hit point and is pulled toward you
  during reeling. If the creature dies, the arrow visual falls to the ground. A nonliving entity hit leaves an
  arrow visual at the impact point. Newly produced drops can be reeled in, and shooting a block within 1 block
  of ground items also catches those items.
- **Right-click to draw and fire, right-click again to reel** — only one shot can be active per player. A curved line
  renders from the shooter's hand to the arrow or hook anchor while the shot is active.
- **48-block line limit by default** — if the arrow or hook moves farther from the shooter, the shot breaks and the
  bow can fire again. The limit is configurable with `maxLineDistance` in `config/fishing-bow.properties` (4–96 blocks).
  Changing dimensions with an active shot breaks the line the same way.
- **64 durability**, crafted from vanilla materials. Costs 1 durability to fire and 1 more to reel — every full
  cycle costs 2, even an "empty" reel that hooked nothing. Breaking the line costs 1 additional durability instead
  of the reel cost.

Damage is intentionally modest (see [`docs/balancing.md`](docs/balancing.md)): it one-shots fish and chickens at a
strong enough draw, but isn't a viable replacement for a real bow against anything tougher.

## Crafting

Shapeless recipe:

| Ingredients | Result |
|---|---|
| Fishing Rod + Bow | Fishing Bow |

## How it works

- `FishingBowItem` extends vanilla `BowItem` but bypasses its ammo check. It switches right-click between
  drawing and reeling based on a synced active-shot flag.
- `ActiveFishingShot` owns the server-side fire/hook/reel state and captured drops. `FishingBowArrow` is the
  outgoing/returning projectile; a non-colliding `FishingBowHook` tracks a creature or a fixed impact point while
  there is no arrow projectile.
- Client renderers draw the arrow and 16-segment line from the projectile or hook anchor. A creature hook uses the
  creature's interpolated position and body rotation; it does not follow individual animated limbs.

See [`docs/reeling-mechanic.md`](docs/reeling-mechanic.md) for the shot lifecycle and
[`docs/hook-visual-spec.md`](docs/hook-visual-spec.md) for the current rendering behavior.

## Documentation

Design/balance and mechanism docs that don't belong in this README live under [`docs/`](docs/):

- [`docs/balancing.md`](docs/balancing.md) — damage and durability tuning rationale.
- [`docs/reeling-mechanic.md`](docs/reeling-mechanic.md) — current fire, hook, and reel states and transitions.
- [`docs/hook-visual-spec.md`](docs/hook-visual-spec.md) — current arrow and line rendering.
- [`docs/in-game-test-guide.md`](docs/in-game-test-guide.md) — in-game checks for each hit outcome.

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
