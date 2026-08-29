# Persistent Procedural Dungeon Floors

Dungeon Dice Frogs uses a room-based procedural dungeon.

- A floor is a 10×10 grid of rooms.
- Each room is a 10×10 movement-square grid.
- A floor therefore represents up to 100×100 movement squares.
- The complete floor is generated the first time it is entered, validated, and written to app-internal persistent storage.
- Existing floor geometry is authoritative and is never silently regenerated.
- Each floor stores its seed, room types, room coordinates, shared borders, doorway offsets, visual floor variants, wall-cube variants, pillars, stairs, open-room groups, and boss data.
- Gameplay state is stored separately so opened chests, defeated enemies, collected loot, unlocked doors, discovered rooms, triggered traps, current room, and boss completion can persist without changing geometry.

## Shared walls

Internal borders are generated once. A room's east wall is the neighboring room's west wall, and north/south borders work the same way. A shared border is `SOLID`, `DOORWAY`, or `OPEN`. Normal doorways are created by omitting one wall cube at a stored offset. Merged rooms use `OPEN` borders.

All four outside edges are always `SOLID` and are validation failures if an opening appears.

## Generation

The generator creates a full connected maze with randomized depth-first search, then adds loops and merged open-room groups. Room types include standard rooms, hallway rooms, open rooms, pillar rooms, stairs rooms, and boss rooms. Hallway rooms receive internal wall-cube layouts that maintain routes between all connected doorways.

Floor 10, 20, 30, etc. are boss floors. The boss area is a merged 2×2 arena with four distributed pillars. The stairs-up exit is located in the boss area and remains locked until the boss is defeated.

## Validation

Before any new floor is saved, the generator verifies:

- exactly one stairs-down and one stairs-up;
- all 100 rooms are reachable from stairs-down;
- stairs-up is reachable;
- boss room is reachable on boss floors;
- every outer border is solid;
- every room connection agrees with its neighboring room;
- no connection points outside the map;
- each room has movement-square connectivity between all required doorways and stairs after hallway walls and pillars are applied.

Invalid attempts are discarded before saving. Once a valid floor is saved, later loads reuse it exactly.

## Tier 1 assets

Tier 1 (floors 1–10) uses the supplied Greystone set:

- 10 floor variations;
- 10 wall-cube variations;
- 3 stairs-down variations;
- 3 stairs-up variations;
- 4 pillar-top variations.

The app packages these as compact WebP atlases; visual selection never changes logical dungeon geometry.
