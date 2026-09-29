# Fishing Bow: In-Game Test Guide

Use this guide to check the current [fire, hook, and reel mechanic](reeling-mechanic.md) and [hook and line rendering](hook-visual-spec.md). Record the mod build, Minecraft version, single-player or dedicated-server setup, and whether each observation is from the shooter or another player.

## Setup

Use a survival-mode test world, a fresh Fishing Bow, a solid block, a sturdy living target, a small creature that can die from one shot, and an item frame or painting. Leave room to watch the returning arrow. Repeat visual checks in first and third person. For every case, note the line during flight, waiting, and return; the visible arrow count; what is pulled; durability used; and whether the bow can fire again after return.

A successful fire uses one durability. Starting a valid reel uses one more. Exceeding `maxLineDistance` (48 blocks by default) breaks the line and costs one additional durability instead of the reel cost; a break during return costs nothing extra. Changing dimensions with an active shot counts as a distance break. Entity hits and state transfers have no separate cost. A shot remains active until its returning arrow and captured targets arrive, up to 80 ticks after the arrow returns, or the shot is cleared. For unexpected behavior, inspect `[FBDEBUG]` lines in `run/logs/latest.log` near the shot. The logs show server transitions; rendering still needs direct observation.

## Hit scenarios

| Case | Action | Behavior to check |
| --- | --- | --- |
| Flying / miss | Fire into open space and reel before impact. | The outbound arrow becomes the returning arrow. The line ends at that arrow throughout; no target or item is pulled. |
| Block | Shoot a solid block, wait, then reel. | The physical arrow stays embedded in the block until reeling begins. It returns without colliding with intervening terrain. |
| Ground items near block | Place items on the ground and hit a block within 1 block of them. | The nearby items are pulled toward the player when reeling; farther items are left alone. Pulling can continue after the arrow arrives. |
| Surviving creature, still | Use a low-power shot on a sturdy creature and inspect the hit point. | One rendered arrow sits at the hit offset. The physical outbound arrow is gone. The line ends at the rendered arrow. |
| Surviving creature, moving | Hook a creature and let it walk and turn before reeling. | The arrow and line endpoint follow its interpolated position and body rotation. Limb animation is not tracked, so inspect whether the arrow looks seated through movement. |
| Surviving creature, reel | Reel a moving creature from several blocks away. | The hook visual is replaced by one returning arrow at the current anchor position. The creature keeps being pulled after the arrow arrives, until it reaches the player or the 80 tick follow through expires. |
| Fatal hit with drops | Kill a small creature with the shot and wait before reeling. | The arrow visual and line endpoint fall to the ground, then stay there. Newly produced nearby items are pulled during the return. The dead creature is not hooked. |
| Fatal hit without drops | In a disposable world, disable mob loot, make a fatal hit, then restore the setting. | The shot remains reelable even with no captured items. |
| Item frame / painting | Hit each in a separate shot; leave an unrelated item nearby for one shot. | A stationary impact anchor remains. Newly produced nearby items are tracked; the pre-existing item is not. The nonliving target is not pulled as a creature. |
| Other nonliving entity | Hit a boat or minecart. | The result uses a stationary impact anchor rather than a creature hook. Any newly produced nearby items are tracked. |

The item check compares nearby item entity IDs immediately before and after hit handling. It is not a provenance check: an unrelated item newly appearing in that same area during the hit could also be tracked.

## Lifecycle and multiplayer

| Case | Action | Behavior to check |
| --- | --- | --- |
| Quick reel | Fire and right-click again immediately. | The client does not start another draw; the server starts one reel and applies one reel cost. |
| Target dies later | Hook a creature, then kill it by another means before reeling. | The arrow visual detaches, falls to the ground, and remains reelable from its current position. Later drops are not added to this shot. |
| Target becomes unavailable | Hook a creature and make it unload, leave the level, or be removed. | The server releases the target reference and retains the last anchor position when the hook remains available. A missing hook clears the shot. |
| Shooter leaves | Hook a creature, disconnect, then rejoin. | The server clears the shot and discards its arrow or hook. The bow is usable after rejoining. |
| Two players | Have player A fire and reel while player B watches nearby, then reverse roles. | Both clients render the same shot anchor and the correct shooter's hand as the line origin. Only the owner controls the reel. |
| Repeated cycles | Alternate miss, block, creature, and nonliving entity shots. | Each completed return restores the ability to draw. No old line, arrow, hook, or captured-item reference remains in the next cycle. |
| Distance break | Fire or hook a target, then move more than `maxLineDistance` from the arrow or hook. | The arrow and line disappear, the original bow takes one additional durability, and another shot can be fired. Check that the visuals remain visible until the break. |
| Dimension break | Fire or hook a target, then enter a Nether portal before reeling. Repeat while the arrow is returning. | Same result as a distance break. Before reeling, the bow takes one additional durability; during return, no extra durability is taken. |
| Off-hand line | Hold a Fishing Bow in each hand, fire from the off hand, then swap held items while the shot is out. | The line starts from the off-hand arm and stays there through hook and return. |

A short third-person recording is useful for checking whether the arrow and line endpoint move together during creature movement and the transition to reeling.
