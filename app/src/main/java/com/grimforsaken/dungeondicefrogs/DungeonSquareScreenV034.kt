package com.grimforsaken.dungeondicefrogs

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private data class V034Position(val roomX: Int, val roomY: Int, val squareX: Int, val squareY: Int)

private object V034PositionRepository {
    private const val PREFS = "dungeon_square_positions_v034"

    fun load(context: Context, floor: DungeonFloorData, state: DungeonFloorState): V034Position {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val prefix = "floor_${floor.floor}_"
        if (!prefs.contains(prefix + "room_x")) {
            val start = floor.roomAt(state.currentRoom.x, state.currentRoom.y)
            return V034Position(
                state.currentRoom.x,
                state.currentRoom.y,
                start.stairs?.x ?: 4,
                start.stairs?.y ?: 4
            ).also { save(context, floor.floor, it) }
        }
        return V034Position(
            prefs.getInt(prefix + "room_x", state.currentRoom.x).coerceIn(0, 9),
            prefs.getInt(prefix + "room_y", state.currentRoom.y).coerceIn(0, 9),
            prefs.getInt(prefix + "square_x", 4).coerceIn(0, 9),
            prefs.getInt(prefix + "square_y", 4).coerceIn(0, 9)
        )
    }

    fun save(context: Context, floorNumber: Int, p: V034Position) {
        val prefix = "floor_${floorNumber}_"
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(prefix + "room_x", p.roomX)
            .putInt(prefix + "room_y", p.roomY)
            .putInt(prefix + "square_x", p.squareX)
            .putInt(prefix + "square_y", p.squareY)
            .apply()
    }
}

private val V034Dark = Color(0xFF100D0B)
private val V034Brown = Color(0xFF342820)
private val V034Gold = Color(0xFFFFC54B)
private val V034Cream = Color(0xFFF4E2C0)

@Composable
fun PersistentDungeonScreenV034(
    frogColor: FrogColor,
    level: Int,
    xp: Int,
    highestDungeonFloor: Int,
    helperCount: Int,
    onRecoverLoot: (Int) -> Unit,
    onAdvanceFloor: () -> Unit,
    onCharacterDeath: () -> Unit
) {
    val context = LocalContext.current
    val floor = remember(highestDungeonFloor) { DungeonFloorRepository.loadOrGenerate(context, highestDungeonFloor) }
    var floorState by remember(highestDungeonFloor) { mutableStateOf(DungeonFloorRepository.loadState(context, floor)) }
    val initial = remember(highestDungeonFloor) { V034PositionRepository.load(context, floor, floorState) }

    var roomX by rememberSaveable(highestDungeonFloor) { mutableStateOf(initial.roomX) }
    var roomY by rememberSaveable(highestDungeonFloor) { mutableStateOf(initial.roomY) }
    var squareX by rememberSaveable(highestDungeonFloor) { mutableStateOf(initial.squareX) }
    var squareY by rememberSaveable(highestDungeonFloor) { mutableStateOf(initial.squareY) }

    val room = floor.roomAt(roomX, roomY)
    val roster = remember(floor.floor) { dungeonEnemyRosterForFloor(floor.floor) }
    val regularIndex = ((floor.seed.toInt() xor (roomX * 97) xor (roomY * 193)).absoluteValue) % roster.size
    val roomEnemy = if (floor.floor == 10 && room.type == DungeonRoomType.BOSS && !floorState.bossDefeated) {
        tierOneFloorTenBoss
    } else roster[regularIndex]

    fun persist() {
        V034PositionRepository.save(context, floor.floor, V034Position(roomX, roomY, squareX, squareY))
    }

    fun cellWalkable(targetRoom: DungeonRoomData, x: Int, y: Int): Boolean {
        if (x !in 0..9 || y !in 0..9) return false
        if (targetRoom.pillars.any { it.x == x && it.y == y }) return false
        if (targetRoom.interiorWalls.any { it.x == x && it.y == y }) return false
        if (x in 1..8 && y in 1..8) return true
        if ((x == 0 || x == 9) && (y == 0 || y == 9)) return false

        val direction = when {
            y == 0 -> DungeonDirection.NORTH
            y == 9 -> DungeonDirection.SOUTH
            x == 0 -> DungeonDirection.WEST
            else -> DungeonDirection.EAST
        }
        val offset = if (direction == DungeonDirection.NORTH || direction == DungeonDirection.SOUTH) x else y
        val wall = floor.wallFor(targetRoom, direction)
        return when (wall.kind) {
            SharedWallKind.SOLID -> false
            SharedWallKind.DOORWAY -> wall.doorwayOffset == offset
            SharedWallKind.OPEN -> offset in 1..8
        }
    }

    fun move(direction: DungeonDirection) {
        val nx = squareX + direction.dx
        val ny = squareY + direction.dy
        if (nx in 0..9 && ny in 0..9) {
            if (!cellWalkable(room, nx, ny)) return
            squareX = nx
            squareY = ny
            persist()
            return
        }

        val atBoundary = when (direction) {
            DungeonDirection.NORTH -> squareY == 0
            DungeonDirection.SOUTH -> squareY == 9
            DungeonDirection.WEST -> squareX == 0
            DungeonDirection.EAST -> squareX == 9
        }
        if (!atBoundary) return

        val offset = if (direction == DungeonDirection.NORTH || direction == DungeonDirection.SOUTH) squareX else squareY
        val wall = floor.wallFor(room, direction)
        val crossingAllowed = when (wall.kind) {
            SharedWallKind.SOLID -> false
            SharedWallKind.DOORWAY -> wall.doorwayOffset == offset
            SharedWallKind.OPEN -> offset in 1..8
        }
        if (!crossingAllowed) return

        val nrx = roomX + direction.dx
        val nry = roomY + direction.dy
        if (nrx !in 0..9 || nry !in 0..9) return
        val next = floor.roomAt(nrx, nry)
        val nsx = when (direction) {
            DungeonDirection.WEST -> 9
            DungeonDirection.EAST -> 0
            else -> squareX
        }
        val nsy = when (direction) {
            DungeonDirection.NORTH -> 9
            DungeonDirection.SOUTH -> 0
            else -> squareY
        }
        if (!cellWalkable(next, nsx, nsy)) return

        roomX = nrx
        roomY = nry
        squareX = nsx
        squareY = nsy
        floorState.currentRoom = DungeonCoord(roomX, roomY)
        floorState.discoveredRooms += floorState.currentRoom.id
        DungeonFloorRepository.saveState(context, floorState)
        floorState = floorState.copy(discoveredRooms = floorState.discoveredRooms.toMutableSet())
        persist()
    }

    Column(
        Modifier.fillMaxSize().background(V034Dark).padding(bottom = 82.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("TOWER DUNGEON", color = V034Gold, fontWeight = FontWeight.Black, fontSize = 24.sp, modifier = Modifier.padding(top = 8.dp))
        Text("Floor ${floor.floor} • Tier ${floor.tier} • Room ($roomX,$roomY) • Square ($squareX,$squareY)", color = V034Cream, fontSize = 11.sp)
        Text("Master dungeon art • one square per move • ${floorState.discoveredRooms.size}/100 rooms discovered", color = Color.Gray, fontSize = 9.sp)

        DungeonRoomCanvasV034(
            floor = floor,
            room = room,
            squareX = squareX,
            squareY = squareY,
            frogColor = frogColor,
            modifier = Modifier.fillMaxWidth().padding(6.dp).aspectRatio(1f)
        )

        DungeonMovePadV034(
            onNorth = { move(DungeonDirection.NORTH) },
            onSouth = { move(DungeonDirection.SOUTH) },
            onWest = { move(DungeonDirection.WEST) },
            onEast = { move(DungeonDirection.EAST) }
        )

        Card(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = V034Brown)) {
            Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                EnemyPortraitV034(roomEnemy, Modifier.size(112.dp))
                Column(Modifier.padding(start = 9.dp)) {
                    Text(roomEnemy.name, color = if (roomEnemy.isBoss) Color(0xFFFF7867) else V034Gold, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text(roomEnemy.role, color = V034Cream, fontSize = 9.sp)
                    Text("STR ${roomEnemy.stats.strength} • DEX ${roomEnemy.stats.dexterity} • CON ${roomEnemy.stats.constitution} • HP ${roomEnemy.hp} • AC ${roomEnemy.armorClass} • Move ${roomEnemy.move}", color = V034Cream, fontSize = 8.sp)
                    Text(roomEnemy.weaponText, color = Color(0xFFC8B8A1), fontSize = 8.sp)
                    roomEnemy.elementalAttack?.let { Text("Element: ${it.name}", color = Color(0xFFBFD8FF), fontWeight = FontWeight.Bold, fontSize = 8.sp) }
                }
            }
        }

        if (floor.floor == 10 && room.type == DungeonRoomType.BOSS && !floorState.bossDefeated) {
            Text("FLOOR 10 BOSS • Stormsting Sovereign is the only boss on Floors 1-10", color = Color(0xFFFF8A70), fontWeight = FontWeight.Bold, fontSize = 10.sp)
            TextButton(onClick = {
                floorState.bossDefeated = true
                DungeonFloorRepository.saveState(context, floorState)
                floorState = floorState.copy(bossDefeated = true)
            }) { Text("Development: Mark Stormsting Defeated", color = Color(0xFFE8A38D)) }
        }

        room.stairs?.let { stairs ->
            val standing = squareX == stairs.x && squareY == stairs.y
            Text("Stairs ${stairs.type.uppercase()} at (${stairs.x},${stairs.y})", color = Color(0xFFBFD8FF), fontSize = 10.sp)
            if (stairs.type == "up" && standing) {
                val locked = floor.bossRequired && !floorState.bossDefeated
                Button(onClick = onAdvanceFloor, enabled = !locked) { Text(if (locked) "Stairs Up Locked — Defeat Boss" else "Use Stairs Up") }
            }
        }

        Card(Modifier.fillMaxWidth().padding(10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF261E18))) {
            Column(Modifier.padding(9.dp)) {
                Text("PERSISTENT FLOOR + POSITION", color = V034Gold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text("Generated geometry, door openings, exact room and exact movement square are remembered.", color = V034Cream, fontSize = 9.sp)
                Text("Hero L$level • XP $xp/${xpRequiredForNextLevel(xp)} • $helperCount helpers • ${frogColor.displayName} immunity", color = Color(0xFFC8B8A1), fontSize = 9.sp)
                Button(onClick = { onRecoverLoot(floor.tier) }) { Text("Test Recover Tier ${floor.tier} Loot (+${floor.tier} XP)") }
            }
        }

        TextButton(onClick = onCharacterDeath) { Text("Development: Character Death", color = Color(0xFFE58C82), fontSize = 10.sp) }
    }
}

@Composable
private fun DungeonMovePadV034(onNorth: () -> Unit, onSouth: () -> Unit, onWest: () -> Unit, onEast: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = onNorth, modifier = Modifier.size(width = 104.dp, height = 50.dp)) { Text("↑", fontSize = 22.sp) }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onWest, modifier = Modifier.size(width = 104.dp, height = 50.dp)) { Text("←", fontSize = 22.sp) }
            Text("MOVE", color = V034Cream, fontWeight = FontWeight.Bold)
            Button(onClick = onEast, modifier = Modifier.size(width = 104.dp, height = 50.dp)) { Text("→", fontSize = 22.sp) }
        }
        Button(onClick = onSouth, modifier = Modifier.size(width = 104.dp, height = 50.dp)) { Text("↓", fontSize = 22.sp) }
    }
}

@Composable
private fun EnemyPortraitV034(enemy: DungeonEnemyDisplay, modifier: Modifier) {
    if (enemy.isBoss) {
        Image(painter = painterResource(R.drawable.boss_stormsting_sovereign), contentDescription = enemy.name, modifier = modifier, contentScale = ContentScale.Fit)
        return
    }
    val context = LocalContext.current
    val atlas = remember { ImageBitmap.imageResource(context.resources, R.drawable.enemy_tier1_atlas) }
    Canvas(modifier) {
        val cell = 180
        val index = enemy.artIndex.coerceIn(0, 19)
        drawImage(
            image = atlas,
            srcOffset = IntOffset((index % 5) * cell, (index / 5) * cell),
            srcSize = IntSize(cell, cell),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))
        )
    }
}

@Composable
private fun DungeonRoomCanvasV034(
    floor: DungeonFloorData,
    room: DungeonRoomData,
    squareX: Int,
    squareY: Int,
    frogColor: FrogColor,
    modifier: Modifier
) {
    val context = LocalContext.current
    val floorAtlas = remember { ImageBitmap.imageResource(context.resources, R.drawable.greystone_floors) }
    val wallAtlas = remember { ImageBitmap.imageResource(context.resources, R.drawable.greystone_walls) }
    val stairsDown = remember { ImageBitmap.imageResource(context.resources, R.drawable.greystone_stairs_down) }
    val stairsUp = remember { ImageBitmap.imageResource(context.resources, R.drawable.greystone_stairs_up) }
    val pillars = remember { ImageBitmap.imageResource(context.resources, R.drawable.greystone_pillars) }
    val doors = remember { ImageBitmap.imageResource(context.resources, R.drawable.dungeon_doors) }
    val details = remember { ImageBitmap.imageResource(context.resources, R.drawable.dungeon_details) }
    val frog = remember(frogColor) { ImageBitmap.imageResource(context.resources, frogArtResource(frogColor)) }

    Canvas(modifier.background(Color.Black, RoundedCornerShape(4.dp))) {
        val cell = size.minDimension / 10f

        for (y in 0..9) for (x in 0..9) {
            drawDungeonAtlasTile(floorAtlas, room.floorTileVariants.getOrElse(y * 10 + x) { 1 }, 5, x * cell, y * cell, cell, cell)
        }

        fun drawEdge(direction: DungeonDirection) {
            val wall = floor.wallFor(room, direction)
            for (i in 0..9) {
                val shouldDraw = when (wall.kind) {
                    SharedWallKind.SOLID -> true
                    SharedWallKind.DOORWAY -> i != wall.doorwayOffset
                    SharedWallKind.OPEN -> i == 0 || i == 9
                }
                if (!shouldDraw) continue
                val x = when (direction) { DungeonDirection.WEST -> 0; DungeonDirection.EAST -> 9; else -> i }
                val y = when (direction) { DungeonDirection.NORTH -> 0; DungeonDirection.SOUTH -> 9; else -> i }
                drawDungeonAtlasTile(wallAtlas, wall.wallVariants.getOrElse(i) { 1 }, 5, x * cell, y * cell, cell, cell)
            }

            if (wall.kind == SharedWallKind.DOORWAY && wall.doorwayOffset != null) {
                val i = wall.doorwayOffset
                val x = when (direction) { DungeonDirection.WEST -> 0; DungeonDirection.EAST -> 9; else -> i }
                val y = when (direction) { DungeonDirection.NORTH -> 0; DungeonDirection.SOUTH -> 9; else -> i }
                val style = ((floor.seed + room.x * 31L + room.y * 17L).absoluteValue % 3L).toInt()
                val openVariant = when (style) { 0 -> 2; 1 -> 5; else -> 8 }
                val rotation = when (direction) {
                    DungeonDirection.NORTH -> 0f
                    DungeonDirection.SOUTH -> 180f
                    DungeonDirection.WEST -> -90f
                    DungeonDirection.EAST -> 90f
                }
                rotate(rotation, pivot = Offset((x + 0.5f) * cell, (y + 0.5f) * cell)) {
                    drawDungeonAtlasTile(doors, openVariant, 3, x * cell, y * cell, cell, cell)
                }
            }
        }
        DungeonDirection.values().forEach(::drawEdge)

        room.interiorWalls.forEach { drawDungeonAtlasTile(wallAtlas, it.variant, 5, it.x * cell, it.y * cell, cell, cell) }
        room.pillars.forEach { drawDungeonAtlasTile(pillars, it.variant, 4, it.x * cell, it.y * cell, cell, cell) }

        if (room.type == DungeonRoomType.STANDARD || room.type == DungeonRoomType.OPEN || room.type == DungeonRoomType.PILLAR) {
            val seed = (floor.seed + room.x * 101L + room.y * 211L).absoluteValue
            val variant = (seed % 12L).toInt() + 1
            val dx = 2 + ((seed / 13L) % 5L).toInt()
            val dy = 2 + ((seed / 29L) % 5L).toInt()
            if (room.pillars.none { it.x == dx && it.y == dy } && room.interiorWalls.none { it.x == dx && it.y == dy }) {
                drawDungeonAtlasTile(details, variant, 4, dx * cell, dy * cell, cell, cell)
            }
        }

        room.stairs?.let {
            drawDungeonAtlasTile(if (it.type == "down") stairsDown else stairsUp, it.variant, 3, 3.05f * cell, 3.05f * cell, 3.9f * cell, 3.9f * cell)
        }

        for (i in 0..10) {
            drawLine(Color.Black.copy(alpha = 0.18f), Offset(i * cell, 0f), Offset(i * cell, 10 * cell), 1f)
            drawLine(Color.Black.copy(alpha = 0.18f), Offset(0f, i * cell), Offset(10 * cell, i * cell), 1f)
        }

        drawImage(
            image = frog,
            dstOffset = IntOffset((squareX * cell - cell * 0.12f).roundToInt(), (squareY * cell - cell * 0.25f).roundToInt()),
            dstSize = IntSize((cell * 1.25f).roundToInt(), (cell * 1.35f).roundToInt())
        )
    }
}

private fun DrawScope.drawDungeonAtlasTile(
    atlas: ImageBitmap,
    variant: Int,
    columns: Int,
    left: Float,
    top: Float,
    width: Float,
    height: Float
) {
    val sourceSize = 120
    val index = (variant - 1).coerceAtLeast(0)
    drawImage(
        image = atlas,
        srcOffset = IntOffset((index % columns) * sourceSize, (index / columns) * sourceSize),
        srcSize = IntSize(sourceSize, sourceSize),
        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
        dstSize = IntSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1))
    )
}
