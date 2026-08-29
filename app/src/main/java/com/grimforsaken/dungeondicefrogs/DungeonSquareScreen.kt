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
import androidx.compose.foundation.layout.height
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

private data class DungeonSquarePosition(
    val roomX: Int,
    val roomY: Int,
    val squareX: Int,
    val squareY: Int
)

private object DungeonSquarePositionRepository {
    private const val PREFS = "dungeon_square_positions"

    fun load(context: Context, floor: DungeonFloorData, state: DungeonFloorState): DungeonSquarePosition {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val prefix = "floor_${floor.floor}_"
        if (!p.contains(prefix + "room_x")) {
            val startRoom = floor.roomAt(state.currentRoom.x, state.currentRoom.y)
            val stairs = startRoom.stairs
            return DungeonSquarePosition(
                roomX = state.currentRoom.x,
                roomY = state.currentRoom.y,
                squareX = stairs?.x ?: 4,
                squareY = stairs?.y ?: 4
            ).also { save(context, floor.floor, it) }
        }
        return DungeonSquarePosition(
            roomX = p.getInt(prefix + "room_x", state.currentRoom.x).coerceIn(0, 9),
            roomY = p.getInt(prefix + "room_y", state.currentRoom.y).coerceIn(0, 9),
            squareX = p.getInt(prefix + "square_x", 4).coerceIn(0, 9),
            squareY = p.getInt(prefix + "square_y", 4).coerceIn(0, 9)
        )
    }

    fun save(context: Context, floorNumber: Int, position: DungeonSquarePosition) {
        val prefix = "floor_${floorNumber}_"
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(prefix + "room_x", position.roomX)
            .putInt(prefix + "room_y", position.roomY)
            .putInt(prefix + "square_x", position.squareX)
            .putInt(prefix + "square_y", position.squareY)
            .apply()
    }
}

private val SqDark = Color(0xFF110E0C)
private val SqBrown = Color(0xFF342820)
private val SqGold = Color(0xFFFFC54B)
private val SqCream = Color(0xFFF4E2C0)

@Composable
fun PersistentDungeonScreen(
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
    val floor = remember(highestDungeonFloor) {
        DungeonFloorRepository.loadOrGenerate(context, highestDungeonFloor)
    }
    var floorState by remember(highestDungeonFloor) {
        mutableStateOf(DungeonFloorRepository.loadState(context, floor))
    }
    val initial = remember(highestDungeonFloor) {
        DungeonSquarePositionRepository.load(context, floor, floorState)
    }

    var roomX by rememberSaveable(highestDungeonFloor) { mutableStateOf(initial.roomX) }
    var roomY by rememberSaveable(highestDungeonFloor) { mutableStateOf(initial.roomY) }
    var squareX by rememberSaveable(highestDungeonFloor) { mutableStateOf(initial.squareX) }
    var squareY by rememberSaveable(highestDungeonFloor) { mutableStateOf(initial.squareY) }

    val room = floor.roomAt(roomX, roomY)
    val encounterRoster = remember(floor.floor) { dungeonEnemyRosterForFloor(floor.floor) }
    val encounterIndex = ((floor.seed.toInt() xor (roomX * 97) xor (roomY * 193)).absoluteValue) % encounterRoster.size
    val roomEnemy = encounterRoster[encounterIndex]

    fun savePosition() {
        DungeonSquarePositionRepository.save(
            context,
            floor.floor,
            DungeonSquarePosition(roomX, roomY, squareX, squareY)
        )
    }

    fun crossingAllowed(direction: DungeonDirection, offset: Int): Boolean {
        val wall = floor.wallFor(room, direction)
        return when (wall.kind) {
            SharedWallKind.SOLID -> false
            SharedWallKind.DOORWAY -> wall.doorwayOffset == offset
            SharedWallKind.OPEN -> offset in 1..8
        }
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
            savePosition()
            return
        }

        val boundaryMatches = when (direction) {
            DungeonDirection.NORTH -> squareY == 0
            DungeonDirection.SOUTH -> squareY == 9
            DungeonDirection.WEST -> squareX == 0
            DungeonDirection.EAST -> squareX == 9
        }
        if (!boundaryMatches) return

        val offset = if (direction == DungeonDirection.NORTH || direction == DungeonDirection.SOUTH) squareX else squareY
        if (!crossingAllowed(direction, offset)) return

        val nextRoomX = roomX + direction.dx
        val nextRoomY = roomY + direction.dy
        if (nextRoomX !in 0..9 || nextRoomY !in 0..9) return
        val nextRoom = floor.roomAt(nextRoomX, nextRoomY)
        val nextSquareX = when (direction) {
            DungeonDirection.WEST -> 9
            DungeonDirection.EAST -> 0
            else -> squareX
        }
        val nextSquareY = when (direction) {
            DungeonDirection.NORTH -> 9
            DungeonDirection.SOUTH -> 0
            else -> squareY
        }
        if (!cellWalkable(nextRoom, nextSquareX, nextSquareY)) return

        roomX = nextRoomX
        roomY = nextRoomY
        squareX = nextSquareX
        squareY = nextSquareY
        floorState.currentRoom = DungeonCoord(roomX, roomY)
        floorState.discoveredRooms += floorState.currentRoom.id
        DungeonFloorRepository.saveState(context, floorState)
        floorState = floorState.copy(discoveredRooms = floorState.discoveredRooms.toMutableSet())
        savePosition()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(SqDark)
            .padding(bottom = 82.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "TOWER DUNGEON",
            color = SqGold,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "Floor ${floor.floor} • Tier ${floor.tier} • Room ($roomX,$roomY) • Square ($squareX,$squareY)",
            color = SqCream,
            fontSize = 11.sp
        )
        Text(
            "Move one movement square per press • ${floorState.discoveredRooms.size}/100 rooms discovered",
            color = Color.Gray,
            fontSize = 9.sp
        )

        SquareDungeonRoomCanvas(
            floor = floor,
            room = room,
            squareX = squareX,
            squareY = squareY,
            frogColor = frogColor,
            modifier = Modifier.fillMaxWidth().padding(6.dp).aspectRatio(1f)
        )

        DungeonMovePad(
            onNorth = { move(DungeonDirection.NORTH) },
            onSouth = { move(DungeonDirection.SOUTH) },
            onWest = { move(DungeonDirection.WEST) },
            onEast = { move(DungeonDirection.EAST) }
        )

        Card(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = SqBrown)
        ) {
            Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(roomEnemy.artRes),
                    contentDescription = roomEnemy.name,
                    modifier = Modifier.size(94.dp),
                    contentScale = ContentScale.Fit
                )
                Column(Modifier.padding(start = 8.dp)) {
                    Text(roomEnemy.name, color = SqGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text(roomEnemy.role, color = SqCream, fontSize = 9.sp)
                    Text(
                        "STR ${roomEnemy.stats.strength} • DEX ${roomEnemy.stats.dexterity} • CON ${roomEnemy.stats.constitution} • HP ${roomEnemy.hp} • AC ${roomEnemy.armorClass} • Move ${roomEnemy.move}",
                        color = SqCream,
                        fontSize = 8.sp
                    )
                    Text(roomEnemy.weaponText, color = Color(0xFFC8B8A1), fontSize = 8.sp)
                }
            }
        }

        room.stairs?.let { stairs ->
            val standingOnStairs = squareX == stairs.x && squareY == stairs.y
            Text(
                "Stairs ${stairs.type.uppercase()} at (${stairs.x},${stairs.y})",
                color = Color(0xFFBFD8FF),
                fontSize = 10.sp
            )
            if (stairs.type == "up" && standingOnStairs) {
                val locked = floor.bossRequired && !floorState.bossDefeated
                Button(onClick = onAdvanceFloor, enabled = !locked) {
                    Text(if (locked) "Stairs Up Locked — Defeat Boss" else "Use Stairs Up")
                }
            }
        }

        if (room.type == DungeonRoomType.BOSS && !floorState.bossDefeated) {
            Text(
                "BOSS ARENA • progression is locked until the boss is defeated",
                color = Color(0xFFFF8A70),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
            TextButton(onClick = {
                floorState.bossDefeated = true
                DungeonFloorRepository.saveState(context, floorState)
                floorState = floorState.copy(bossDefeated = true)
            }) {
                Text("Development: Mark Boss Defeated", color = Color(0xFFE8A38D))
            }
        }

        Card(
            Modifier.fillMaxWidth().padding(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF261E18))
        ) {
            Column(Modifier.padding(9.dp)) {
                Text("PERSISTENT FLOOR + POSITION", color = SqGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text(
                    "The floor geometry is generated once and saved. Your exact room and movement-square position are also saved when you move.",
                    color = SqCream,
                    fontSize = 9.sp
                )
                Text(
                    "Hero L$level • XP $xp/${xpRequiredForNextLevel(xp)} • $helperCount helpers • ${frogColor.displayName} immunity",
                    color = Color(0xFFC8B8A1),
                    fontSize = 9.sp
                )
                Button(onClick = { onRecoverLoot(floor.tier) }) {
                    Text("Test Recover Tier ${floor.tier} Loot (+${floor.tier} XP)")
                }
            }
        }

        TextButton(onClick = onCharacterDeath) {
            Text("Development: Character Death", color = Color(0xFFE58C82), fontSize = 10.sp)
        }
    }
}

@Composable
private fun DungeonMovePad(
    onNorth: () -> Unit,
    onSouth: () -> Unit,
    onWest: () -> Unit,
    onEast: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = onNorth, modifier = Modifier.size(width = 72.dp, height = 38.dp)) { Text("↑") }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onWest, modifier = Modifier.size(width = 72.dp, height = 38.dp)) { Text("←") }
            Text("MOVE", color = SqCream, fontWeight = FontWeight.Bold)
            Button(onClick = onEast, modifier = Modifier.size(width = 72.dp, height = 38.dp)) { Text("→") }
        }
        Button(onClick = onSouth, modifier = Modifier.size(width = 72.dp, height = 38.dp)) { Text("↓") }
    }
}

@Composable
private fun SquareDungeonRoomCanvas(
    floor: DungeonFloorData,
    room: DungeonRoomData,
    squareX: Int,
    squareY: Int,
    frogColor: FrogColor,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val floorAtlas = remember { ImageBitmap.imageResource(context.resources, R.drawable.greystone_floors) }
    val wallAtlas = remember { ImageBitmap.imageResource(context.resources, R.drawable.greystone_walls) }
    val stairsDownAtlas = remember { ImageBitmap.imageResource(context.resources, R.drawable.greystone_stairs_down) }
    val stairsUpAtlas = remember { ImageBitmap.imageResource(context.resources, R.drawable.greystone_stairs_up) }
    val pillarAtlas = remember { ImageBitmap.imageResource(context.resources, R.drawable.greystone_pillars) }
    val frog = remember(frogColor) { ImageBitmap.imageResource(context.resources, frogArtResource(frogColor)) }

    Canvas(modifier.background(Color.Black, RoundedCornerShape(4.dp))) {
        val cell = size.minDimension / 10f

        for (y in 0..9) {
            for (x in 0..9) {
                drawAtlasTile(
                    floorAtlas,
                    room.floorTileVariants.getOrElse(y * 10 + x) { 1 },
                    5,
                    x * cell,
                    y * cell,
                    cell,
                    cell
                )
            }
        }

        fun drawEdge(direction: DungeonDirection) {
            val wall = floor.wallFor(room, direction)
            for (i in 0..9) {
                val draw = when (wall.kind) {
                    SharedWallKind.SOLID -> true
                    SharedWallKind.DOORWAY -> i != wall.doorwayOffset
                    SharedWallKind.OPEN -> i == 0 || i == 9
                }
                if (!draw) continue
                val x = when (direction) {
                    DungeonDirection.WEST -> 0
                    DungeonDirection.EAST -> 9
                    else -> i
                }
                val y = when (direction) {
                    DungeonDirection.NORTH -> 0
                    DungeonDirection.SOUTH -> 9
                    else -> i
                }
                drawAtlasTile(
                    wallAtlas,
                    wall.wallVariants.getOrElse(i) { 1 },
                    5,
                    x * cell,
                    y * cell,
                    cell,
                    cell
                )
            }
        }
        DungeonDirection.values().forEach(::drawEdge)

        room.interiorWalls.forEach {
            drawAtlasTile(wallAtlas, it.variant, 5, it.x * cell, it.y * cell, cell, cell)
        }
        room.pillars.forEach {
            drawAtlasTile(pillarAtlas, it.variant, 4, it.x * cell, it.y * cell, cell, cell)
        }
        room.stairs?.let {
            drawAtlasTile(
                if (it.type == "down") stairsDownAtlas else stairsUpAtlas,
                it.variant,
                3,
                3.05f * cell,
                3.05f * cell,
                3.9f * cell,
                3.9f * cell
            )
        }

        for (i in 0..10) {
            drawLine(Color.Black.copy(alpha = 0.24f), Offset(i * cell, 0f), Offset(i * cell, 10 * cell), 1f)
            drawLine(Color.Black.copy(alpha = 0.24f), Offset(0f, i * cell), Offset(10 * cell, i * cell), 1f)
        }

        drawImage(
            image = frog,
            dstOffset = IntOffset((squareX * cell - cell * 0.08f).roundToInt(), (squareY * cell - cell * 0.20f).roundToInt()),
            dstSize = IntSize((cell * 1.16f).roundToInt(), (cell * 1.26f).roundToInt())
        )
    }
}

private fun DrawScope.drawAtlasTile(
    atlas: ImageBitmap,
    variant: Int,
    columns: Int,
    left: Float,
    top: Float,
    width: Float,
    height: Float
) {
    val index = (variant - 1).coerceAtLeast(0)
    val sourceSize = 160
    drawImage(
        image = atlas,
        srcOffset = IntOffset((index % columns) * sourceSize, (index / columns) * sourceSize),
        srcSize = IntSize(sourceSize, sourceSize),
        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
        dstSize = IntSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1))
    )
}
