# Fishing Bow: Fire, Hook, and Reel

The Fishing Bow has one active shot per player. Right-click starts drawing when there is no active shot. After firing, right-click starts reeling; the player can draw again when the returning arrow and any hooked creature or captured items arrive, or the shot is cleared.

## Components

| Component | Responsibility |
| --- | --- |
| `FishingBowItem` | Starts the draw, fires without inventory arrows, routes the next use to the active shot, and applies durability and sounds. |
| `ActiveFishingShot` | Server authority for the shot state, hooked creature, captured items, and return motion. |
| `FishingBowArrow` | Physical projectile during outbound flight, block embedding, and return flight. |
| `FishingBowHook` | Non-colliding anchor while a creature or entity impact waits for a reel. It synchronizes owner, target, and hit offset to clients. |
| `ModAttachments` | Stores the server-only active shot and a synced `HAS_ACTIVE_ARROW` flag on the player. |
| Client renderers | Draw the arrow and line for a physical arrow or the arrow and line at a hook anchor. |

The hook anchor is an entity, but it is not an arrow projectile and does not deal damage or collide with targets.

Every server tick, the shot compares the shooter's eye position with its current arrow or hook anchor. If the
distance exceeds `maxLineDistance` (48 blocks by default), or the shooter is in a different dimension from the arrow
or hook, it discards the arrow or hook and clears the active-shot flag. This applies during flight, while waiting
at a hit, and during return. Before reeling, the break charges one additional durability to the bow that fired it;
during return the reel durability has already been charged, so the break costs nothing extra. A hooked creature and captured drops remain in the world where they are.

## Shot states

| State | Server-side representation | What happens next |
| --- | --- | --- |
| `FLYING` | Outgoing `FishingBowArrow` | A block hit enters `IN_BLOCK`; an entity hit enters `HOOKED_CREATURE` or `AT_IMPACT`; use starts the return. |
| `IN_BLOCK` | The same arrow embedded in the block | Use starts the return. |
| `HOOKED_CREATURE` | Living target plus `FishingBowHook`; no physical arrow | The anchor follows the target. Use spawns a returning arrow. If the target dies, the visual arrow falls to the ground and the state becomes `AT_IMPACT`. |
| `AT_IMPACT` | Unattached `FishingBowHook`; no physical arrow | A fatal creature hit makes the visual arrow fall to the ground; a nonliving entity hit leaves it at impact. Use spawns a returning arrow from the current position. |
| `RETURNING` | Physical `FishingBowArrow` until it reaches the player | Each server tick pulls the arrow and any tracked creature or captured drops toward the player. After the arrow arrives, pulling continues until the targets arrive or 80 more ticks pass. |

The bow's ready and drawing conditions have no active shot. The client predicts `HAS_ACTIVE_ARROW = true` on a qualifying release so a quick second click routes to reeling without waiting for a server sync packet. The server decides whether a shot exists and whether a reel starts.

## Hit handling

- **Block:** Vanilla embeds the outbound arrow. It stays there until reeling begins. Existing ground items within 1 block of the impact point are captured for reeling.
- **Living entity that survives:** Vanilla applies the hit. The mod creates a hook anchor tied to the target and removes the outbound arrow. It restores the target's stuck-arrow count if vanilla added one for this impact, because the hook renderer supplies the shot's visible arrow.
- **Living entity killed by the hit:** The mod creates a hook anchor at impact that falls to the ground. Drops produced by the hit are tracked for reeling.
- **Nonliving entity:** The mod uses the same stationary impact anchor and tracks any newly produced item entities. It does not pull the struck entity as a creature.

For entity hits, the mod compares item entities within 1.5 blocks of the target before and after vanilla hit handling. Only newly present items are tracked. This is a proximity-and-timing check; it does not inspect a drop's source. A hit with no drops still leaves a retrievable shot.

Vanilla hit handling can remove the outbound arrow before the mod finishes classifying the hit. `FishingBowArrow` marks that operation as in progress so its removal callback does not clear the active shot during the transfer to a hook anchor.

## Reeling and cleanup

Starting a valid reel returns the block-embedded or still-flying arrow. A creature or impact anchor instead spawns one returning arrow at its current position, then discards the anchor. The arrow has no physics during its return. Each server tick applies velocity toward a point near the owner's body to the arrow, any still-valid hooked creature, and tracked item entities. Once the arrow is within 1.25 blocks of that point, it is discarded. The shot keeps pulling any remaining creature and items until they arrive or another 80 ticks pass. A returning arrow is not an inventory pickup.

If a hooked creature dies, is removed, or changes level before reeling, the hook detaches and its visual arrow falls until it reaches a block. Its later drops are not added to the shot. If the owner dies or disconnects, the shot and its visual entities are cleared. A missing arrow or anchor also clears the shot rather than leaving the bow locked. Arrow and hook entity types are configured `noSave`; an active shot is not persisted across a world restart.

See [hook and line rendering](hook-visual-spec.md), [balance](balancing.md), and the [in-game test guide](in-game-test-guide.md).
