from pathlib import Path
import re

main = Path('app/src/main/java/com/grimforsaken/dungeondicefrogs/MainActivity.kt')
s = main.read_text()

if 'import androidx.compose.foundation.layout.BoxScope' not in s:
    s = s.replace('import androidx.compose.foundation.layout.Box\n', 'import androidx.compose.foundation.layout.Box\nimport androidx.compose.foundation.layout.BoxScope\n')

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
# Route the Dungeon tab through the persistent floor generator instead of the
# older development-only monster-list screen.
s = s.replace('Screen.DUNGEON -> DungeonScreen(', 'Screen.DUNGEON -> PersistentDungeonScreen(')
main.write_text(s)

# Compose exposes RowScope.weight as a scope extension; importing the internal symbol
# directly breaks compilation on the Compose version used by this project.
town = Path('app/src/main/java/com/grimforsaken/dungeondicefrogs/TownHubScreen.kt')
if town.exists():
    t = town.read_text().replace('import androidx.compose.foundation.layout.weight\n', '')
    town.write_text(t)

# Keep the procedural dungeon compatible with the project's Compose version and
# the compact 40-pixel Greystone atlas cells packaged in drawable-nodpi.
dungeon = Path('app/src/main/java/com/grimforsaken/dungeondicefrogs/ProceduralDungeon.kt')
if dungeon.exists():
    d = dungeon.read_text()
    d = d.replace('import androidx.compose.foundation.layout.weight\n', '')
    d = d.replace('val s=160', 'val s=40')
    d = d.replace('val sourceSize = 160', 'val sourceSize = 40')

    def replace_one(pattern: str, replacement: str, label: str):
        nonlocal_d = None
        return pattern, replacement, label

    room_types = '''    private fun types(
        rooms: MutableList<DungeonRoomData>,
        l: Array<MutableSet<Int>>,
        s: DungeonCoord,
        e: DungeonCoord,
        r: Random
    ) {
        rooms.forEachIndexed { i, room ->
            val c = co(i)
            room.type = when {
                c == s -> DungeonRoomType.STAIRS_DOWN
                room.mergedGroup?.startsWith("boss-") == true -> DungeonRoomType.BOSS
                c == e -> DungeonRoomType.STAIRS_UP
                room.mergedGroup != null -> DungeonRoomType.OPEN
                else -> {
                    val ds = l[i].map { dir(c, co(it)) }
                    val straight = ds.size == 2 && ds[0].opposite() == ds[1]
                    when {
                        straight && r.nextFloat() < .72f -> DungeonRoomType.HALLWAY
                        ds.size in 2..3 && r.nextFloat() < .22f -> DungeonRoomType.HALLWAY
                        r.nextFloat() < .10f -> DungeonRoomType.PILLAR
                        r.nextFloat() < .12f -> DungeonRoomType.OPEN
                        else -> DungeonRoomType.STANDARD
                    }
                }
            }
        }
    }
'''
    d, count = re.subn(r'^    private fun types\([^\n]*\n', room_types, d, count=1, flags=re.MULTILINE)
    if count != 1:
        raise RuntimeError('Could not patch procedural room type function')

    walls_fn = '''    private fun walls(
        rooms: List<DungeonRoomData>,
        links: Array<MutableSet<Int>>,
        r: Random
    ): Map<String, SharedDungeonWall> {
        val result = mutableMapOf<String, SharedDungeonWall>()
        fun variants(): List<Int> = List(10) { r.nextInt(1, 11) }

        for (x in 0..9) {
            result["N:$x"] = SharedDungeonWall("N:$x", SharedWallKind.SOLID, null, variants())
            result["S:$x"] = SharedDungeonWall("S:$x", SharedWallKind.SOLID, null, variants())
        }
        for (y in 0..9) {
            result["W:$y"] = SharedDungeonWall("W:$y", SharedWallKind.SOLID, null, variants())
            result["E:$y"] = SharedDungeonWall("E:$y", SharedWallKind.SOLID, null, variants())
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
                result[key] = SharedDungeonWall(
                    key,
                    kind,
                    if (kind == SharedWallKind.DOORWAY) r.nextInt(2, 8) else null,
                    variants()
                )
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
                result[key] = SharedDungeonWall(
                    key,
                    kind,
                    if (kind == SharedWallKind.DOORWAY) r.nextInt(2, 8) else null,
                    variants()
                )
            }
        }
        return result
    }
'''
    d, count = re.subn(r'^    private fun walls\([^\n]*\n', walls_fn, d, count=1, flags=re.MULTILINE)
    if count != 1:
        raise RuntimeError('Could not patch procedural shared-wall function')

    connections_fn = '''    private fun connections(
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
                    room.doorways += DungeonDoorway(direction, wall.doorwayOffset ?: 4)
                }
            }
        }
    }
'''
    d, count = re.subn(r'^    private fun connections\([^\n]*\n', connections_fn, d, count=1, flags=re.MULTILINE)
    if count != 1:
        raise RuntimeError('Could not patch procedural connection function')

    visuals_fn = '''    private fun visuals(rooms: MutableList<DungeonRoomData>, r: Random) {
        for (room in rooms) {
            val base = r.nextInt(1, 11)
            room.floorTileVariants = List(100) { i ->
                val x = i % 10
                val y = i / 10
                val clustered = ((x / 3) + (y / 3) + base - 1) % 10 + 1
                val roll = r.nextFloat()
                when {
                    roll < .68f -> clustered
                    roll < .91f -> base
                    else -> r.nextInt(1, 11)
                }
            }
        }
    }
'''
    d, count = re.subn(r'^    private fun visuals\([^\n]*\n', visuals_fn, d, count=1, flags=re.MULTILINE)
    if count != 1:
        raise RuntimeError('Could not patch procedural visual function')

    features_fn = '''    private fun features(
        rooms: MutableList<DungeonRoomData>,
        start: DungeonCoord,
        exit: DungeonCoord,
        r: Random
    ) {
        rooms[idx(start.x, start.y)].stairs = DungeonStairs("down", r.nextInt(1, 4))
        rooms[idx(exit.x, exit.y)].stairs = DungeonStairs("up", r.nextInt(1, 4))

        for (room in rooms) {
            when (room.type) {
                DungeonRoomType.BOSS -> {
                    val groupRooms = rooms.filter { it.mergedGroup == room.mergedGroup }
                    val minX = groupRooms.minOf { it.x }
                    val minY = groupRooms.minOf { it.y }
                    room.pillars += DungeonPillar(
                        if (room.x == minX) 2 else 7,
                        if (room.y == minY) 2 else 7,
                        r.nextInt(1, 5)
                    )
                }
                DungeonRoomType.PILLAR -> {
                    val layouts = listOf(
                        listOf(2 to 2, 7 to 2, 2 to 7, 7 to 7),
                        listOf(3 to 2, 6 to 2, 3 to 7, 6 to 7),
                        listOf(2 to 4, 7 to 4),
                        listOf(4 to 2, 4 to 7)
                    )
                    layouts.random(r).forEach { (x, y) ->
                        room.pillars += DungeonPillar(x, y, r.nextInt(1, 5))
                    }
                }
                DungeonRoomType.OPEN -> {
                    if (r.nextFloat() < .35f) {
                        listOf(3 to 3, 6 to 6).forEach { (x, y) ->
                            room.pillars += DungeonPillar(x, y, r.nextInt(1, 5))
                        }
                    }
                }
                else -> Unit
            }
        }
    }
'''
    d, count = re.subn(r'^    private fun features\([^\n]*\n', features_fn, d, count=1, flags=re.MULTILINE)
    if count != 1:
        raise RuntimeError('Could not patch procedural feature function')

    hallways_fn = '''    private fun hallways(
        rooms: MutableList<DungeonRoomData>,
        walls: Map<String, SharedDungeonWall>,
        r: Random
    ) {
        for (room in rooms.filter { it.type == DungeonRoomType.HALLWAY }) {
            val open = mutableSetOf<Pair<Int, Int>>()
            for (y in 3..6) for (x in 3..6) open += x to y

            for (direction in room.connections) {
                val wall = walls[wallKey(room.x, room.y, direction)] ?: continue
                val offset = wall.doorwayOffset ?: 4
                when (direction) {
                    DungeonDirection.NORTH -> for (y in 1..4) for (x in offset - 1..offset + 1) open += x to y
                    DungeonDirection.SOUTH -> for (y in 5..8) for (x in offset - 1..offset + 1) open += x to y
                    DungeonDirection.WEST -> for (x in 1..4) for (y in offset - 1..offset + 1) open += x to y
                    DungeonDirection.EAST -> for (x in 5..8) for (y in offset - 1..offset + 1) open += x to y
                }
            }

            for (y in 1..8) {
                for (x in 1..8) {
                    if ((x to y) !in open) {
                        room.interiorWalls += DungeonInteriorWall(x, y, r.nextInt(1, 11))
                    }
                }
            }
        }
    }
'''
    d, count = re.subn(r'^    private fun hallways\([^\n]*\n', hallways_fn, d, count=1, flags=re.MULTILINE)
    if count != 1:
        raise RuntimeError('Could not patch procedural hallway function')

    validate_fn = '''    fun validate(floor: DungeonFloorData): List<String> {
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
            val current = queue.removeFirst()
            val room = floor.roomAt(current.x, current.y)
            for (direction in room.connections) {
                val next = DungeonCoord(current.x + direction.dx, current.y + direction.dy)
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
        if (floor.bossRequired && floor.bossRoom == null) errors += "boss missing"

        for (room in floor.rooms) errors += localValidate(floor, room)
        return errors.distinct()
    }
'''
    d, count = re.subn(r'^    fun validate\([^\n]*\n', validate_fn, d, count=1, flags=re.MULTILINE)
    if count != 1:
        raise RuntimeError('Could not patch procedural validation function')

    local_validate_fn = '''    private fun localValidate(
        floor: DungeonFloorData,
        room: DungeonRoomData
    ): List<String> {
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
            val current = queue.removeFirst()
            for ((dx, dy) in steps) {
                val next = (current.first + dx) to (current.second + dy)
                if (
                    next.first in 0..9 && next.second in 0..9 &&
                    next !in blocked && seen.add(next)
                ) {
                    queue.add(next)
                }
            }
        }

        if (entries.any { it !in seen }) errors += "blocked route ${room.coord.id}"
        return errors
    }
'''
    d, count = re.subn(r'^    private fun localValidate\([^\n]*\n', local_validate_fn, d, count=1, flags=re.MULTILINE)
    if count != 1:
        raise RuntimeError('Could not patch procedural local validation function')

    dungeon.write_text(d)

print('Dungeon Dice Frogs source preflight applied')
