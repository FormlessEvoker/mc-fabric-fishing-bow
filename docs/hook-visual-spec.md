# Fishing Bow: Hook and Line Rendering

The client renders a line from the shooter's hand to the active arrow or hook anchor. `FishingLineRenderer` supplies the same line geometry for both the physical arrow renderer and the hook renderer.

## Endpoints by shot state

| Shot state | Visible endpoint |
| --- | --- |
| `FLYING` | Outgoing physical arrow. |
| `IN_BLOCK` | Physical arrow embedded in the block. |
| `HOOKED_CREATURE` | Arrow model rendered at the creature's hit offset; no physical arrow remains at the target. |
| `AT_IMPACT` | Arrow model rendered at the impact anchor. After a creature dies, the anchor falls to the ground; nonliving entity impacts remain fixed. |
| `RETURNING` | Returning physical arrow. |

The hook entity synchronizes the owner's entity ID, the target's entity ID, and an offset from the creature's origin in body-relative coordinates. On each render frame, `FishingBowHookRenderer` combines that offset with the creature's interpolated position and body rotation. It renders the arrow and starts the line at the same calculated point. The server follows the target for shot state and for the return spawn position. If the target dies, the hook falls to the ground.

This attachment follows movement and body turns. It does not attach to individual animated model parts, so limb movement, unusual poses, and model-specific animation can change how deeply the arrow appears embedded. Vanilla's generic stuck-arrow count is suppressed for this bow's hit while the target survives; the hook renderer draws the shot-specific arrow instead.

## Line geometry

`FishingLineRenderer` estimates the bow user's hand position from the camera near plane in first person and from body rotation, scale, and crouch state in third person. The line is submitted using `RenderTypes.lines()` with a color, normal, and width on every vertex.

The first-person endpoint can be tuned in `config/fishing-bow-client.properties`:

```properties
firstPersonLineHorizontalOffset=0.0
firstPersonLineVerticalOffset=0.0
```

These values are added to the current camera-plane position. Positive horizontal values move the line toward the right side of the screen; negative vertical values move it downward. Try increments of `0.025`. The settings load at client startup, so restart the client after each change. An existing config file may not list the new keys; add them manually. Third-person hand placement is unchanged.

The line's appearance is controlled by these values:

| Constant | Current value | Effect |
| --- | --- | --- |
| `lineWidth` | `2.0F` | Width submitted with each line vertex; client config. |
| `lineColor` | `0xFFD2D2D2` | Opaque light gray; client config. |
| `SEGMENTS` | `16` | Number of straight segments making up the curve. |
| `lineSag` | `0.15F` | Maximum downward bend, in blocks, at the line midpoint; client config. |

The line uses linear horizontal interpolation between endpoints and a downward parabolic offset of `4 × SAG × t × (1 − t)`. Both endpoints stay fixed as the middle sags. The arrow and hook renderers disable ordinary entity culling so their line remains eligible for rendering while the shooter is in view.

Both shot entities use a render distance of `maxLineDistance + 16` blocks. The limit is synchronized from the
server and gives the line room to stay visible until the server breaks a shot at `maxLineDistance`.

Client rendering uses synchronized entity IDs and offsets; hit classification, pulling, durability, and cleanup remain server decisions. See [fire, hook, and reel](reeling-mechanic.md) for the full lifecycle.
