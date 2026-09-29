# Balancing: Fishing Bow

This documents the Fishing Bow's current fire/hook/reel balance and the reasoning behind it.
See the main [README](../README.md) for what the item does, and [`reeling-mechanic.md`](reeling-mechanic.md) for how
the mechanism itself works.

## Goals

- The Fishing Bow should feel like a fishing rod that happens to fire an arrow, not a free substitute for a real,
  enchanted bow.
- No ammo cost (fires without needing arrows in inventory) is a real convenience, so damage and durability need to
  pull their weight elsewhere to keep it from being strictly better than carrying a bow.
- Some deliberate draw-back should be required before anything dies — a fully-uncharged tap shot should never be a
  free kill.

## Ammo

Firing requires **no arrow item** in the inventory at all — closer to how a fishing rod casts freely than
to how a vanilla bow checks for ammo. This is the main "free lunch" the rest of the balancing has to account for.

Only one shot can be active per player at a time. During a creature hook, that shot has no physical arrow, but the
bow still cannot fire again until the shot is reeled in or cleared.

## Durability

Vanilla bows lose 1 durability per shot fired; ordinary arrows cost nothing extra to retrieve. The Fishing Bow
spends durability on **both ends of the cycle**:

- **1 durability to fire.**
- **1 durability to reel in** — always, even on an "empty" reel where nothing got hooked and the arrow is just
  flying back. Reasoning: you're still winching back a physical line and arrow either way, and a real arrow
  wouldn't survive that trip for free.
- **1 additional durability if the line breaks at the distance limit** — this replaces the reel cost for that shot.

Net effect: a full fire-and-reel cycle costs 2 durability, so the Fishing Bow burns through its durability bar
twice as fast as an equivalent normal bow for the same number of shots. At 64 durability, that's 32 full cycles.

This remains exactly 2 durability per completed fire-and-reel cycle even when a creature hit removes the outgoing
arrow and the reel creates a returning arrow. Those entity transitions do not cost durability.

The line breaks when its arrow or hook moves more than 48 blocks from the shooter by default. `maxLineDistance`
in `config/fishing-bow.properties` can set the limit from 4 to 96 blocks. Changing dimensions with an active shot counts as exceeding the limit. The client renders the arrow and line
until 16 blocks beyond the configured limit so visuals remain present until the server clears the shot.

## Damage

`AbstractArrow` computes damage as:

```
power(t)  = clamp( ((t/20)² + 2·(t/20)) / 3 ,  0, 1 )   // t = ticks held, min ~3 ticks to fire at all
velocity  = power × 3.0                                  // blocks/tick
damage    = ceil(velocity × baseDamage)
```

A vanilla, unenchanted arrow uses `baseDamage = 2.0`, giving 1 damage on a bare tap up to 6 damage at full draw
(6-10 with the random critical-hit bonus that only applies at a full 1.0 draw).

**Fishing Bow uses `baseDamage = 1.05`, with critical hits disabled** (flat, predictable damage — no randomness).
This value was solved for two specific thresholds rather than picked arbitrarily:

- Fish (Cod/Salmon/Tropical Fish/Pufferfish — 3 HP) should die in one hit once the bow is drawn back **~75% of the
  way (0.75s of a 1.0s max draw)**, not before.
- Chickens (4 HP) should die in one hit **only at a true, full 1.0s draw** — not a tick earlier.

Solving `ceil(velocity(t) × b) ≥ target` at those two draw times constrains `b` to the range `(1.0, 1.0705]`;
`1.05` sits safely in the middle of that window rather than right on an edge, to avoid floating-point rounding
flipping a threshold by one tick.

| Held | velocity | damage |
|---|---|---|
| 0.15s (minimum draw to fire at all) | 0.32 | 1 |
| 0.30s | 0.69 | 1 |
| 0.45s | 1.10 | 2 |
| 0.55s – 0.70s | 1.40 – 1.89 | 2 |
| **0.75s** | 2.06 | **3 — fish die from here on** |
| 0.80s – 0.95s | 2.24 – 2.80 | 3 |
| **1.0s (full draw)** | 3.00 | **4 — chickens die only here** |

Damage tops out at **4** at full draw — well under vanilla's unenchanted ceiling of 6-10+, and nowhere close to
what an enchanted (e.g. Power V) bow can do. Against anything past chicken-tier HP (wolf: 8, spider: 16,
zombie/skeleton/player: 20, ...) this is not a viable weapon on its own; it takes several full-draw hits to kill
even a zombie. That's intentional — the Fishing Bow is a utility/fishing tool with a fun "one-shot small things"
property, not a bow replacement.

Because a single linear `baseDamage` value governs the whole curve, the two thresholds above (fish, chicken) are
coupled to each other and to everything above them — we can't tune one HP tier's kill-threshold independently of
the rest without switching to a custom step function instead of the vanilla formula. Revisit if a third precise
threshold is ever needed.

## Enchantments

The mod does not define special behavior for Power, Infinity, Punch, Flame, Mending, or other enchantments. The
current damage and durability figures describe the unenchanted bow.
