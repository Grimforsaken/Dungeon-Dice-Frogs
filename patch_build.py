from pathlib import Path
import base64
import hashlib
import json
import zipfile

ROOT = Path('.')
JAVA = ROOT / 'app/src/main/java/com/grimforsaken/dungeondicefrogs'
DRAWABLE = ROOT / 'app/src/main/res/drawable-nodpi'
ASSETS = ROOT / 'assets'
ART_PACK = ASSETS / 'ddf_art_pack_mini_v033.zip'

# Reconstruct the validated mobile art bundle from small repository-safe chunks.
parts = sorted(ASSETS.glob('ddf_art_pack_mini_v033.b64.chunk*'))
if not parts:
    raise RuntimeError('Missing verified Dungeon Dice Frogs mini art-pack chunks')
encoded = ''.join(part.read_text().strip() for part in parts)
ART_PACK.write_bytes(base64.b64decode(encoded, validate=True))

DRAWABLE.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(ART_PACK, 'r') as z:
    manifest = json.loads(z.read('asset_manifest.json').decode('utf-8'))
    expected = set(manifest)
    if len(expected) < 30:
        raise RuntimeError(f'Art manifest unexpectedly small: {len(expected)} assets')
    for name, info in manifest.items():
        if not name.endswith('.webp'):
            raise RuntimeError(f'Unexpected drawable type in art pack: {name}')
        data = z.read(name)
        if hashlib.sha256(data).hexdigest() != info['sha256']:
            raise RuntimeError(f'Checksum mismatch for {name}')
        if len(data) != int(info['bytes']):
            raise RuntimeError(f'Size mismatch for {name}')
        (DRAWABLE / name).write_bytes(data)

required = {
    'app_icon.webp', 'app_branding.webp',
    'home_logo.webp', 'home_play.webp', 'home_continue.webp', 'home_town.webp',
    'home_shop.webp', 'home_heroes.webp', 'home_settings.webp',
    'town_map.webp', 'town_item_shop.webp', 'town_blacksmith.webp',
    'town_apothecary.webp', 'town_tavern.webp', 'town_fountain.webp',
    'frog_green.webp', 'frog_blue.webp', 'frog_yellow.webp', 'frog_red.webp',
    'greystone_floors.webp', 'greystone_walls.webp', 'greystone_stairs_down.webp',
    'greystone_stairs_up.webp', 'greystone_pillars.webp',
    'enemy_fly.webp', 'enemy_mosquito.webp', 'enemy_butterfly.webp', 'enemy_bee.webp',
    'enemy_dragonfly.webp', 'enemy_poison_fly.webp', 'enemy_firefly.webp',
    'enemy_01_lightning_bug_dual_dagger_scout.webp',
    'enemy_02_ladybug_sword_shield_guard.webp',
    'enemy_03_ladybug_two_handed_blunt_mystic.webp',
    'enemy_04_lightning_bug_thunder_axe_raider.webp',
    'enemy_05_june_bug_heavy_shield_guard.webp',
    'enemy_06_june_bug_heavy_dual_blade_raider.webp',
}
missing = sorted(required - expected)
if missing:
    raise RuntimeError('Art pack is missing: ' + ', '.join(missing))

# Existing Compose compatibility repairs for the older development activity.
main = JAVA / 'MainActivity.kt'
if main.exists():
    s = main.read_text()
    if 'import androidx.compose.foundation.layout.BoxScope' not in s:
        s = s.replace(
            'import androidx.compose.foundation.layout.Box\n',
            'import androidx.compose.foundation.layout.Box\nimport androidx.compose.foundation.layout.BoxScope\n'
        )
    s = s.replace('import androidx.compose.foundation.layout.weight\n', '')
    s = s.replace('import androidx.compose.ui.input.pointer.consume\n', '')
    s = s.replace('private fun TownBuilding(', 'private fun BoxScope.TownBuilding(')
    s = s.replace(
        'val ac = (chest?.armorClass ?: 0) + if (shield) 1 else 0 - if (dualDaggers) 2 else 0',
        'val ac = (chest?.armorClass ?: 0) + (if (shield) 1 else 0) - (if (dualDaggers) 2 else 0)'
    )
    s = s.replace(
        'val move = 5 + thresholdBonus(stats.dexterity) - if (shield) 1 else 0 - if (chest?.armorWeight == "heavy" && stats.strength < 12) 1 else 0',
        'val move = 5 + thresholdBonus(stats.dexterity) - (if (shield) 1 else 0) - (if (chest?.armorWeight == "heavy" && stats.strength < 12) 1 else 0)'
    )
    s = s.replace(
        'Chip("AC", ac); Chip("MOVE", move); Chip("CARRY", 6 + thresholdBonus(stats.strength) + if (inventory.contains("backpack")) 4 else 0)',
        'Chip("AC", ac); Chip("MOVE", move); Chip("CARRY", 6 + thresholdBonus(stats.strength) + (if (inventory.contains("backpack")) 4 else 0))'
    )
    s = s.replace(
        'onDrag = { change, amount -> change.consume(); onMove(amount) },',
        'onDrag = { _, amount -> onMove(amount) },'
    )
    s = s.replace('Screen.DUNGEON -> DungeonScreen(', 'Screen.DUNGEON -> PersistentDungeonScreen(')
    main.write_text(s)

# Compose RowScope.weight is a scope extension; importing the internal symbol breaks this Compose version.
town = JAVA / 'TownHubScreen.kt'
if town.exists():
    town.write_text(town.read_text().replace('import androidx.compose.foundation.layout.weight\n', ''))

# Normalize the compact procedural-generator functions into ordinary Kotlin.
# This keeps the same generation rules while avoiding parser ambiguity from giant one-line expressions.
dungeon = JAVA / 'ProceduralDungeon.kt'
if dungeon.exists():
    d = dungeon.read_text().replace('import androidx.compose.foundation.layout.weight\n', '')

    types_start = d.find('    private fun types(')
    walls_start = d.find('\n    private fun walls(', types_start)
    if types_start < 0 or walls_start < 0:
        raise RuntimeError('Could not locate DungeonGenerator.types() for Kotlin repair')

    repaired_types = '''    private fun types(
        rooms: MutableList<DungeonRoomData>,
        links: Array<MutableSet<Int>>,
        start: DungeonCoord,
        exit: DungeonCoord,
        random: Random
    ) {
        rooms.forEachIndexed { index, room ->
            val coord = co(index)
            room.type = when {
                coord == start -> DungeonRoomType.STAIRS_DOWN
                room.mergedGroup?.startsWith("boss-") == true -> DungeonRoomType.BOSS
                coord == exit -> DungeonRoomType.STAIRS_UP
                room.mergedGroup != null -> DungeonRoomType.OPEN
                else -> {
                    val directions = links[index].map { dir(coord, co(it)) }
                    val straight = directions.size == 2 && directions[0].opposite() == directions[1]
                    when {
                        straight && random.nextFloat() < 0.72f -> DungeonRoomType.HALLWAY
                        directions.size in 2..3 && random.nextFloat() < 0.22f -> DungeonRoomType.HALLWAY
                        random.nextFloat() < 0.10f -> DungeonRoomType.PILLAR
                        random.nextFloat() < 0.12f -> DungeonRoomType.OPEN
                        else -> DungeonRoomType.STANDARD
                    }
                }
            }
        }
    }'''
    d = d[:types_start] + repaired_types + d[walls_start:]

    walls_start = d.find('    private fun walls(')
    generator_end = d.find('\n}\n\nprivate val DDark', walls_start)
    if walls_start < 0 or generator_end < 0:
        raise RuntimeError('Could not locate remaining DungeonGenerator functions for Kotlin repair')

    repaired_tail = '''    private fun walls(
        rooms: List<DungeonRoomData>,
        links: Array<MutableSet<Int>>,
        random: Random
    ): Map<String, SharedDungeonWall> {
        val output = mutableMapOf<String, SharedDungeonWall>()
        fun variants() = List(10) { random.nextInt(1, 11) }

        for (x in 0..9) {
            output["N:$x"] = SharedDungeonWall("N:$x", SharedWallKind.SOLID, null, variants())
            output["S:$x"] = SharedDungeonWall("S:$x", SharedWallKind.SOLID, null, variants())
        }
        for (y in 0..9) {
            output["W:$y"] = SharedDungeonWall("W:$y", SharedWallKind.SOLID, null, variants())
            output["E:$y"] = SharedDungeonWall("E:$y", SharedWallKind.SOLID, null, variants())
        }

        for (y in 0..9) {
            for (x in 0..8) {
                val a = idx(x, y)
                val b = idx(x + 1, y)
                val key = "V:$x:$y"
                val sameGroup = rooms[a].mergedGroup != null && rooms[a].mergedGroup == rooms[b].mergedGroup
                val kind = when {
                    sameGroup -> SharedWallKind.OPEN
                    b in links[a] -> SharedWallKind.DOORWAY
                    else -> SharedWallKind.SOLID
                }
                val offset = if (kind == SharedWallKind.DOORWAY) random.nextInt(2, 8) else null
                output[key] = SharedDungeonWall(key, kind, offset, variants())
            }
        }

        for (y in 0..8) {
            for (x in 0..9) {
                val a = idx(x, y)
                val b = idx(x, y + 1)
                val key = "H:$x:$y"
                val sameGroup = rooms[a].mergedGroup != null && rooms[a].mergedGroup == rooms[b].mergedGroup
                val kind = when {
                    sameGroup -> SharedWallKind.OPEN
                    b in links[a] -> SharedWallKind.DOORWAY
                    else -> SharedWallKind.SOLID
                }
                val offset = if (kind == SharedWallKind.DOORWAY) random.nextInt(2, 8) else null
                output[key] = SharedDungeonWall(key, kind, offset, variants())
            }
        }
        return output
    }

    private fun connections(
        rooms: MutableList<DungeonRoomData>,
        walls: Map<String, SharedDungeonWall>
    ) {
        for (room in rooms) {
            room.connections.clear()
            room.doorways.clear()
            for (direction in DungeonDirection.values()) {
                val wall = walls[wallKey(room.x, room.y, direction)] ?: continue
                if (wall.kind != SharedWallKind.SOLID) room.connections += direction
                if (wall.kind == SharedWallKind.DOORWAY) {
                    room.doorways += DungeonDoorway(direction, wall.doorwayOffset!!)
                }
            }
        }
    }

    private fun visuals(rooms: MutableList<DungeonRoomData>, random: Random) {
        for (room in rooms) {
            val base = random.nextInt(1, 11)
            room.floorTileVariants = List(100) { index ->
                val x = index % 10
                val y = index / 10
                val coordinated = ((x / 3) + (y / 3) + base - 1) % 10 + 1
                when {
                    random.nextFloat() < 0.68f -> coordinated
                    random.nextFloat() < 0.72f -> base
                    else -> random.nextInt(1, 11)
                }
            }
        }
    }

    private fun features(
        rooms: MutableList<DungeonRoomData>,
        start: DungeonCoord,
        exit: DungeonCoord,
        random: Random
    ) {
        rooms[idx(start.x, start.y)].stairs = DungeonStairs("down", random.nextInt(1, 4))
        rooms[idx(exit.x, exit.y)].stairs = DungeonStairs("up", random.nextInt(1, 4))

        for (room in rooms) {
            when (room.type) {
                DungeonRoomType.BOSS -> {
                    val groupRooms = rooms.filter { it.mergedGroup == room.mergedGroup }
                    val minX = groupRooms.minOf { it.x }
                    val minY = groupRooms.minOf { it.y }
                    room.pillars += DungeonPillar(
                        if (room.x == minX) 2 else 7,
                        if (room.y == minY) 2 else 7,
                        random.nextInt(1, 5)
                    )
                }
                DungeonRoomType.PILLAR -> {
                    val layouts = listOf(
                        listOf(2 to 2, 7 to 2, 2 to 7, 7 to 7),
                        listOf(3 to 2, 6 to 2, 3 to 7, 6 to 7),
                        listOf(2 to 4, 7 to 4),
                        listOf(4 to 2, 4 to 7)
                    )
                    layouts.random(random).forEach { (x, y) ->
                        room.pillars += DungeonPillar(x, y, random.nextInt(1, 5))
                    }
                }
                DungeonRoomType.OPEN -> {
                    if (random.nextFloat() < 0.35f) {
                        listOf(3 to 3, 6 to 6).forEach { (x, y) ->
                            room.pillars += DungeonPillar(x, y, random.nextInt(1, 5))
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    private fun hallways(
        rooms: MutableList<DungeonRoomData>,
        walls: Map<String, SharedDungeonWall>,
        random: Random
    ) {
        for (room in rooms.filter { it.type == DungeonRoomType.HALLWAY }) {
            val openCells = mutableSetOf<Pair<Int, Int>>()
            for (y in 3..6) for (x in 3..6) openCells += x to y

            for (direction in room.connections) {
                val wall = walls[wallKey(room.x, room.y, direction)]!!
                val offset = wall.doorwayOffset ?: 4
                when (direction) {
                    DungeonDirection.NORTH -> for (y in 1..4) for (x in offset - 1..offset + 1) openCells += x to y
                    DungeonDirection.SOUTH -> for (y in 5..8) for (x in offset - 1..offset + 1) openCells += x to y
                    DungeonDirection.WEST -> for (x in 1..4) for (y in offset - 1..offset + 1) openCells += x to y
                    DungeonDirection.EAST -> for (x in 5..8) for (y in offset - 1..offset + 1) openCells += x to y
                }
            }

            for (y in 1..8) {
                for (x in 1..8) {
                    if ((x to y) !in openCells) {
                        room.interiorWalls += DungeonInteriorWall(x, y, random.nextInt(1, 11))
                    }
                }
            }
        }
    }

    fun validate(floor: DungeonFloorData): List<String> {
        val errors = mutableListOf<String>()
        if (floor.rooms.size != 100) errors += "room count"
        if (floor.rooms.count { it.stairs?.type == "down" } != 1) errors += "stairs down count"
        if (floor.rooms.count { it.stairs?.type == "up" } != 1) errors += "stairs up count"

        for (x in 0..9) {
            if (floor.sharedWalls["N:$x"]?.kind != SharedWallKind.SOLID) errors += "north edge"
            if (floor.sharedWalls["S:$x"]?.kind != SharedWallKind.SOLID) errors += "south edge"
        }
        for (y in 0..9) {
            if (floor.sharedWalls["W:$y"]?.kind != SharedWallKind.SOLID) errors += "west edge"
            if (floor.sharedWalls["E:$y"]?.kind != SharedWallKind.SOLID) errors += "east edge"
        }

        val seen = mutableSetOf(floor.startRoom.id)
        val queue = ArrayDeque<DungeonCoord>()
        queue.add(floor.startRoom)
        while (queue.isNotEmpty()) {
            val coord = queue.removeFirst()
            val room = floor.roomAt(coord.x, coord.y)
            for (direction in room.connections) {
                val next = DungeonCoord(coord.x + direction.dx, coord.y + direction.dy)
                if (next.x !in 0..9 || next.y !in 0..9) {
                    errors += "outside connection"
                    continue
                }
                if (direction.opposite() !in floor.roomAt(next.x, next.y).connections) {
                    errors += "mismatch"
                }
                if (seen.add(next.id)) queue.add(next)
            }
        }
        if (floor.stairsUpRoom.id !in seen) errors += "exit unreachable"
        if (seen.size != 100) errors += "disconnected"
        if (floor.bossRequired && floor.bossRoom?.id !in seen) errors += "boss unreachable"
        for (room in floor.rooms) errors += localValidate(floor, room)
        return errors.distinct()
    }

    private fun localValidate(floor: DungeonFloorData, room: DungeonRoomData): List<String> {
        val errors = mutableListOf<String>()
        val blocked = mutableSetOf<Pair<Int, Int>>()
        room.interiorWalls.forEach { blocked += it.x to it.y }
        room.pillars.forEach { blocked += it.x to it.y }
        for (i in 0..9) {
            blocked += i to 0
            blocked += i to 9
            blocked += 0 to i
            blocked += 9 to i
        }

        val entries = mutableListOf<Pair<Int, Int>>()
        for (direction in room.connections) {
            val wall = floor.wallFor(room, direction)
            val offset = wall.doorwayOffset ?: 4
            val points = if (wall.kind == SharedWallKind.OPEN) {
                (1..8).map { i ->
                    when (direction) {
                        DungeonDirection.NORTH -> i to 0
                        DungeonDirection.SOUTH -> i to 9
                        DungeonDirection.WEST -> 0 to i
                        DungeonDirection.EAST -> 9 to i
                    }
                }
            } else {
                listOf(
                    when (direction) {
                        DungeonDirection.NORTH -> offset to 0
                        DungeonDirection.SOUTH -> offset to 9
                        DungeonDirection.WEST -> 0 to offset
                        DungeonDirection.EAST -> 9 to offset
                    }
                )
            }
            points.forEach { blocked.remove(it) }
            entries += points[points.size / 2]
        }
        room.stairs?.let { entries += it.x to it.y }
        if (entries.isEmpty()) return listOf("no route ${room.coord.id}")

        val seen = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        if (entries.first() !in blocked) {
            seen += entries.first()
            queue.add(entries.first())
        }
        val steps = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            for ((dx, dy) in steps) {
                val next = (cell.first + dx) to (cell.second + dy)
                if (next.first in 0..9 && next.second in 0..9 && next !in blocked && seen.add(next)) {
                    queue.add(next)
                }
            }
        }
        if (entries.any { it !in seen }) errors += "blocked route ${room.coord.id}"
        return errors
    }'''

    d = d[:walls_start] + repaired_tail + d[generator_end:]

    # The square-based dungeon screen supersedes the old room-to-room Compose screen.
    legacy_ui = d.find('\nprivate val DDark')
    if legacy_ui >= 0:
        d = d[:legacy_ui].rstrip() + '\n'
    dungeon.write_text(d)

# The compact Greystone atlases use 32-pixel source cells.
square = JAVA / 'DungeonSquareScreen.kt'
if square.exists():
    q = square.read_text().replace('val sourceSize = 160', 'val sourceSize = 32')
    square.write_text(q)

# Use the persistent main frog in Town and let the tower entrance switch to Dungeon.
game = JAVA / 'GameActivity.kt'
if game.exists():
    g = game.read_text()
    old = '''                            helpers = helpers,\n                            highestDungeonFloor = highestDungeonFloor,\n                            onNotice = { notice = it }\n                        )'''
    new = '''                            helpers = helpers,\n                            highestDungeonFloor = highestDungeonFloor,\n                            frogColor = currentColor,\n                            onEnterDungeon = {\n                                screenName = Screen.DUNGEON.name\n                                PlayerCharacterRepository.saveWorldProgress(\n                                    context, coins, highestDungeonFloor, Screen.DUNGEON\n                                )\n                            },\n                            onNotice = { notice = it }\n                        )'''
    if old not in g:
        raise RuntimeError('Could not patch TownHubScreen call in GameActivity.kt')
    game.write_text(g.replace(old, new, 1))

print(f'Preflight OK: reconstructed and verified {len(expected)} supplied WebP art assets.')
