package com.grimforsaken.dungeondicefrogs

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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

private data class V035Position(val roomX: Int, val roomY: Int, val squareX: Int, val squareY: Int)

private object V035PositionRepository {
    private const val PREFS = "dungeon_square_positions_v2"

    fun load(context: Context, floor: V2DungeonFloorData, state: V2DungeonFloorState): V035Position {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val prefix = "floor_${floor.floor}_"
        if (prefs.contains(prefix + "room_x")) {
            val loaded = V035Position(
                prefs.getInt(prefix + "room_x", state.currentRoom.x).coerceIn(0, 9),
                prefs.getInt(prefix + "room_y", state.currentRoom.y).coerceIn(0, 9),
                prefs.getInt(prefix + "square_x", 5).coerceIn(0, 9),
                prefs.getInt(prefix + "square_y", 5).coerceIn(0, 9)
            )
            if (floor.roomAt(loaded.roomX, loaded.roomY).present) return loaded
        }
        val start = floor.roomAt(state.currentRoom.x, state.currentRoom.y).takeIf { it.present }
            ?: floor.roomAt(floor.startRoom.x, floor.startRoom.y)
        val stairs = start.stairs
        return V035Position(start.x, start.y, stairs?.x ?: 5, stairs?.y ?: 5).also {
            save(context, floor.floor, it)
        }
    }

    fun save(context: Context, floorNumber: Int, position: V035Position) {
        val prefix = "floor_${floorNumber}_"
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(prefix + "room_x", position.roomX)
            .putInt(prefix + "room_y", position.roomY)
            .putInt(prefix + "square_x", position.squareX)
            .putInt(prefix + "square_y", position.squareY)
            .apply()
    }
}

private val V035Dark = Color(0xFF100D0B)
private val V035Brown = Color(0xFF342820)
private val V035Gold = Color(0xFFFFC54B)
private val V035Cream = Color(0xFFF4E2C0)
private val V035Enemy = Color(0xFFD44E45)
private val V035Treasure = Color(0xFFFFD54F)
private val V035Stairs = Color(0xFF64B5F6)

@Composable
fun PersistentDungeonScreenV035(
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
    val floor = remember(highestDungeonFloor) { V2DungeonFloorRepository.loadOrGenerate(context, highestDungeonFloor) }
    var floorState by remember(highestDungeonFloor) { mutableStateOf(V2DungeonFloorRepository.loadState(context, floor)) }
    val initial = remember(highestDungeonFloor) { V035PositionRepository.load(context, floor, floorState) }

    var roomX by rememberSaveable(highestDungeonFloor) { mutableStateOf(initial.roomX) }
    var roomY by rememberSaveable(highestDungeonFloor) { mutableStateOf(initial.roomY) }
    var squareX by rememberSaveable(highestDungeonFloor) { mutableStateOf(initial.squareX) }
    var squareY by rememberSaveable(highestDungeonFloor) { mutableStateOf(initial.squareY) }

    val room = floor.roomAt(roomX, roomY)
    val roster = remember(floor.floor) { dungeonEnemyRosterForFloor(floor.floor) }
    val activeEncounter = room.encounters.firstOrNull { it.id !in floorState.defeatedEnemies }
    val roomEnemy = activeEncounter?.let { encounter ->
        if (encounter.boss && floor.floor == 10) {
            tierOneFloorTenBoss
        } else {
            val index = ((floor.seed.toInt() xor (roomX * 97) xor (roomY * 193) xor encounter.id.hashCode()).absoluteValue) % roster.size
            roster[index]
        }
    }
    val activeTreasure = room.treasure.firstOrNull { it.id !in floorState.openedChests && it.id !in floorState.collectedLoot }

    fun persistPosition() {
        V035PositionRepository.save(context, floor.floor, V035Position(roomX, roomY, squareX, squareY))
    }

    fun wallPassable(wall: V2SharedWall, offset: Int): Boolean = when (wall.kind) {
        V2WallKind.SOLID -> false
        V2WallKind.DOORWAY -> wall.doorwayOffset == offset
        V2WallKind.OPEN -> offset in 1..8
        V2WallKind.SECRET -> wall.key in floorState.unlockedDoors && wall.doorwayOffset == offset
    }

    fun cellWalkable(targetRoom: V2DungeonRoomData, x: Int, y: Int): Boolean {
        if (!targetRoom.present || x !in 0..9 || y !in 0..9) return false
        if (targetRoom.interiorWalls.any { it.x == x && it.y == y }) return false
        if (targetRoom.features.any { it.x == x && it.y == y && it.blocking }) return false
        if (x in 1..8 && y in 1..8) return true
        if ((x == 0 || x == 9) && (y == 0 || y == 9)) return false
        val direction = when {
            y == 0 -> DungeonDirection.NORTH
            y == 9 -> DungeonDirection.SOUTH
            x == 0 -> DungeonDirection.WEST
            else -> DungeonDirection.EAST
        }
        val offset = if (direction == DungeonDirection.NORTH || direction == DungeonDirection.SOUTH) x else y
        return wallPassable(floor.wallFor(targetRoom, direction), offset)
    }

    fun move(direction: DungeonDirection) {
        val nx = squareX + direction.dx
        val ny = squareY + direction.dy
        if (nx in 0..9 && ny in 0..9) {
            if (!cellWalkable(room, nx, ny)) return
            squareX = nx
            squareY = ny
            persistPosition()
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
        if (!wallPassable(wall, offset)) return

        val nextRoomX = roomX + direction.dx
        val nextRoomY = roomY + direction.dy
        if (nextRoomX !in 0..9 || nextRoomY !in 0..9) return
        val nextRoom = floor.roomAt(nextRoomX, nextRoomY)
        if (!nextRoom.present) return

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
        floorState.currentRoom = V2DungeonCoord(roomX, roomY)
        floorState.discoveredRooms += floorState.currentRoom.id
        if (nextRoom.type == V2RoomType.SECRET) floorState.discoveredSecretRooms += floorState.currentRoom.id
        V2DungeonFloorRepository.saveState(context, floorState)
        floorState = floorState.copy(
            discoveredRooms = floorState.discoveredRooms.toMutableSet(),
            discoveredSecretRooms = floorState.discoveredSecretRooms.toMutableSet()
        )
        persistPosition()
    }

    val hiddenSecretWall = room.connections.map { direction -> direction to floor.wallFor(room, direction) }
        .firstOrNull { (_, wall) -> wall.kind == V2WallKind.SECRET && wall.key !in floorState.unlockedDoors }

    Column(
        Modifier.fillMaxSize().background(V035Dark).padding(bottom = 82.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("TOWER DUNGEON", color = V035Gold, fontWeight = FontWeight.Black, fontSize = 24.sp, modifier = Modifier.padding(top = 8.dp))
        Text("Floor ${floor.floor} • Tier ${floor.tier} • ${room.type.name.replace('_', ' ')}", color = V035Cream, fontSize = 11.sp)
        Text("Move one square at a time. Gold outlines are doorway squares.", color = Color(0xFFC9B99F), fontSize = 9.sp)

        DungeonRoomCanvasV035(
            floor = floor,
            state = floorState,
            room = room,
            squareX = squareX,
            squareY = squareY,
            frogColor = frogColor,
            activeEncounter = activeEncounter,
            activeTreasure = activeTreasure,
            modifier = Modifier.fillMaxWidth().padding(8.dp).aspectRatio(1f).border(2.dp, Color(0xFF6D5A43), RoundedCornerShape(8.dp))
        )

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("GOLD = DOOR", color = V035Gold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text("RED = ENEMY", color = V035Enemy, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text("YELLOW = TREASURE", color = V035Treasure, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text("BLUE = STAIRS", color = V035Stairs, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }

        DungeonMovePadV035(
            onNorth = { move(DungeonDirection.NORTH) },
            onSouth = { move(DungeonDirection.SOUTH) },
            onWest = { move(DungeonDirection.WEST) },
            onEast = { move(DungeonDirection.EAST) }
        )

        hiddenSecretWall?.let { (direction, wall) ->
            Button(onClick = {
                floorState.unlockedDoors += wall.key
                V2DungeonFloorRepository.saveState(context, floorState)
                floorState = floorState.copy(unlockedDoors = floorState.unlockedDoors.toMutableSet())
            }) { Text("Search ${direction.name.lowercase().replaceFirstChar { it.uppercase() }} Wall") }
        }

        if (roomEnemy != null && activeEncounter != null) {
            Card(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = V035Brown)) {
                Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    EnemyPortraitV035(roomEnemy, Modifier.size(104.dp))
                    Column(Modifier.padding(start = 9.dp)) {
                        Text(roomEnemy.name, color = if (roomEnemy.isBoss) Color(0xFFFF7867) else V035Gold, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text(roomEnemy.role, color = V035Cream, fontSize = 9.sp)
                        Text("STR ${roomEnemy.stats.strength} • DEX ${roomEnemy.stats.dexterity} • CON ${roomEnemy.stats.constitution}", color = V035Cream, fontSize = 8.sp)
                        Text("HP ${roomEnemy.hp} • AC ${roomEnemy.armorClass} • Move ${roomEnemy.move}", color = V035Cream, fontSize = 8.sp)
                        Text(roomEnemy.weaponText, color = Color(0xFFC8B8A1), fontSize = 8.sp)
                        roomEnemy.elementalAttack?.let { Text("Element: ${it.name}", color = Color(0xFFBFD8FF), fontWeight = FontWeight.Bold, fontSize = 8.sp) }
                        TextButton(onClick = {
                            floorState.defeatedEnemies += activeEncounter.id
                            if (activeEncounter.boss) floorState.bossDefeated = true
                            V2DungeonFloorRepository.saveState(context, floorState)
                            floorState = floorState.copy(
                                defeatedEnemies = floorState.defeatedEnemies.toMutableSet(),
                                bossDefeated = floorState.bossDefeated
                            )
                        }) { Text("Development: Mark Defeated", color = Color(0xFFE8A38D), fontSize = 9.sp) }
                    }
                }
            }
        }

        if (floor.floor == 10 && room.type == V2RoomType.BOSS && !floorState.bossDefeated) {
            Text("STORMSTING SOVEREIGN • 2 daggers + 1 short sword • LIGHTNING", color = Color(0xFFFF8A70), fontWeight = FontWeight.Bold, fontSize = 9.sp)
        }

        activeTreasure?.let { treasure ->
            val standingOnTreasure = squareX == treasure.x && squareY == treasure.y
            Card(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2B19))) {
                Column(Modifier.padding(8.dp)) {
                    Text(if (treasure.bossChest) "BOSS CHEST" else "TREASURE", color = V035Gold, fontWeight = FontWeight.Black)
                    Text(if (standingOnTreasure) "You are standing on the treasure square." else "Move onto the yellow treasure square to collect it.", color = V035Cream, fontSize = 9.sp)
                    Button(onClick = {
                        floorState.openedChests += treasure.id
                        floorState.collectedLoot += treasure.id
                        V2DungeonFloorRepository.saveState(context, floorState)
                        floorState = floorState.copy(
                            openedChests = floorState.openedChests.toMutableSet(),
                            collectedLoot = floorState.collectedLoot.toMutableSet()
                        )
                        onRecoverLoot(floor.tier)
                    }, enabled = standingOnTreasure) { Text("Collect Treasure") }
                }
            }
        }

        room.stairs?.let { stairs ->
            val standing = squareX == stairs.x && squareY == stairs.y
            if (stairs.type == "up" && standing) {
                val locked = floor.bossRequired && !floorState.bossDefeated
                Button(onClick = onAdvanceFloor, enabled = !locked) {
                    Text(if (locked) "Stairs Up Locked — Defeat Boss" else "Use Stairs Up")
                }
            }
        }

        Text("Hero L$level • XP $xp/${xpRequiredForNextLevel(xp)} • $helperCount helpers • ${frogColor.displayName} immunity", color = Color(0xFFC8B8A1), fontSize = 9.sp, modifier = Modifier.padding(8.dp))
        TextButton(onClick = onCharacterDeath) { Text("Development: Character Death", color = Color(0xFFE58C82), fontSize = 10.sp) }
    }
}

@Composable
private fun DungeonMovePadV035(onNorth: () -> Unit, onSouth: () -> Unit, onWest: () -> Unit, onEast: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = onNorth, modifier = Modifier.size(width = 132.dp, height = 58.dp)) { Text("↑  NORTH", fontSize = 16.sp) }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onWest, modifier = Modifier.size(width = 132.dp, height = 58.dp)) { Text("← WEST", fontSize = 16.sp) }
            Button(onClick = onEast, modifier = Modifier.size(width = 132.dp, height = 58.dp)) { Text("EAST →", fontSize = 16.sp) }
        }
        Button(onClick = onSouth, modifier = Modifier.size(width = 132.dp, height = 58.dp)) { Text("↓ SOUTH", fontSize = 16.sp) }
    }
}

@Composable
private fun EnemyPortraitV035(enemy: DungeonEnemyDisplay, modifier: Modifier) {
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
private fun DungeonRoomCanvasV035(
    floor: V2DungeonFloorData,
    state: V2DungeonFloorState,
    room: V2DungeonRoomData,
    squareX: Int,
    squareY: Int,
    frogColor: FrogColor,
    activeEncounter: V2EncounterSpawn?,
    activeTreasure: V2TreasureSpawn?,
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

    Canvas(modifier.background(Color(0xFF181411), RoundedCornerShape(6.dp))) {
        val cell = size.minDimension / 10f
        val baseFloorVariant = room.floorTileVariants.firstOrNull()?.coerceIn(1, 10) ?: 1

        for (y in 0..9) for (x in 0..9) {
            val index = y * 10 + x
            val variant = if ((x * 3 + y * 5 + room.x + room.y) % 7 == 0) {
                room.floorTileVariants.getOrElse(index) { baseFloorVariant }.coerceIn(1, 10)
            } else baseFloorVariant
            drawDungeonAtlasTileV035(floorAtlas, variant, 5, x * cell, y * cell, cell, cell)
        }

        fun drawWallCube(x: Int, y: Int, variant: Int) {
            val inset = cell * 0.10f
            drawDungeonAtlasTileV035(wallAtlas, variant.coerceIn(1, 10), 5, x * cell + inset, y * cell + inset, cell - inset * 2f, cell - inset * 2f)
        }

        fun drawDoorHighlight(x: Int, y: Int) {
            drawRect(
                color = V035Gold,
                topLeft = Offset(x * cell + cell * 0.08f, y * cell + cell * 0.08f),
                size = Size(cell * 0.84f, cell * 0.84f),
                style = Stroke(width = (cell * 0.08f).coerceAtLeast(2f))
            )
        }

        fun drawEdge(direction: DungeonDirection) {
            val wall = floor.wallFor(room, direction)
            val secretUnlocked = wall.key in state.unlockedDoors
            for (i in 0..9) {
                val shouldDraw = when (wall.kind) {
                    V2WallKind.SOLID -> true
                    V2WallKind.DOORWAY -> i != wall.doorwayOffset
                    V2WallKind.OPEN -> i == 0 || i == 9
                    V2WallKind.SECRET -> if (secretUnlocked) i != wall.doorwayOffset else true
                }
                if (!shouldDraw) continue
                val x = when (direction) { DungeonDirection.WEST -> 0; DungeonDirection.EAST -> 9; else -> i }
                val y = when (direction) { DungeonDirection.NORTH -> 0; DungeonDirection.SOUTH -> 9; else -> i }
                drawWallCube(x, y, wall.wallVariants.getOrElse(i) { 1 })
            }

            val showDoor = wall.kind == V2WallKind.DOORWAY || (wall.kind == V2WallKind.SECRET && secretUnlocked)
            if (showDoor && wall.doorwayOffset != null) {
                val i = wall.doorwayOffset
                val x = when (direction) { DungeonDirection.WEST -> 0; DungeonDirection.EAST -> 9; else -> i }
                val y = when (direction) { DungeonDirection.NORTH -> 0; DungeonDirection.SOUTH -> 9; else -> i }
                val rotation = when (direction) {
                    DungeonDirection.NORTH -> 0f
                    DungeonDirection.SOUTH -> 180f
                    DungeonDirection.WEST -> -90f
                    DungeonDirection.EAST -> 90f
                }
                rotate(rotation, pivot = Offset((x + 0.5f) * cell, (y + 0.5f) * cell)) {
                    drawDungeonAtlasTileV035(doors, wall.doorVariant.coerceIn(1, 9), 3, x * cell, y * cell, cell, cell)
                }
                drawDoorHighlight(x, y)
            }
        }
        DungeonDirection.values().forEach(::drawEdge)

        room.interiorWalls.forEach { wall -> drawWallCube(wall.x, wall.y, wall.variant) }

        room.features.forEach { feature ->
            when {
                feature.type == V2FeatureType.PILLAR || feature.type == V2FeatureType.ROCK_SPIRE -> {
                    val inset = cell * 0.08f
                    drawDungeonAtlasTileV035(pillars, ((feature.variant - 1) % 4) + 1, 4, feature.x * cell + inset, feature.y * cell + inset, cell - inset * 2f, cell - inset * 2f)
                }
                feature.blocking -> {
                    val inset = cell * 0.16f
                    drawDungeonAtlasTileV035(details, 1, 4, feature.x * cell + inset, feature.y * cell + inset, cell - inset * 2f, cell - inset * 2f)
                    drawRect(Color(0xFF8D5A43), Offset(feature.x * cell + inset, feature.y * cell + inset), Size(cell - inset * 2f, cell - inset * 2f), style = Stroke(width = 2f))
                }
                else -> Unit
            }
        }

        room.stairs?.let { stairs ->
            val atlas = if (stairs.type == "down") stairsDown else stairsUp
            drawDungeonAtlasTileV035(atlas, stairs.variant.coerceIn(1, 3), 3, (stairs.x - 0.35f) * cell, (stairs.y - 0.35f) * cell, 1.7f * cell, 1.7f * cell)
            drawRect(V035Stairs, Offset(stairs.x * cell + cell * 0.08f, stairs.y * cell + cell * 0.08f), Size(cell * 0.84f, cell * 0.84f), style = Stroke(width = (cell * 0.06f).coerceAtLeast(2f)))
        }

        activeEncounter?.let { encounter ->
            drawRect(V035Enemy, Offset(encounter.x * cell + cell * 0.14f, encounter.y * cell + cell * 0.14f), Size(cell * 0.72f, cell * 0.72f), style = Stroke(width = (cell * 0.07f).coerceAtLeast(2f)))
        }
        activeTreasure?.let { treasure ->
            drawRect(V035Treasure.copy(alpha = 0.28f), Offset(treasure.x * cell + cell * 0.18f, treasure.y * cell + cell * 0.18f), Size(cell * 0.64f, cell * 0.64f))
            drawRect(V035Treasure, Offset(treasure.x * cell + cell * 0.14f, treasure.y * cell + cell * 0.14f), Size(cell * 0.72f, cell * 0.72f), style = Stroke(width = (cell * 0.07f).coerceAtLeast(2f)))
        }

        for (i in 0..10) {
            drawLine(Color.Black.copy(alpha = 0.13f), Offset(i * cell, 0f), Offset(i * cell, 10 * cell), 1f)
            drawLine(Color.Black.copy(alpha = 0.13f), Offset(0f, i * cell), Offset(10 * cell, i * cell), 1f)
        }

        drawCircle(Color.White.copy(alpha = 0.30f), radius = cell * 0.38f, center = Offset((squareX + 0.5f) * cell, (squareY + 0.58f) * cell))
        drawImage(
            image = frog,
            dstOffset = IntOffset((squareX * cell + cell * 0.05f).roundToInt(), (squareY * cell - cell * 0.02f).roundToInt()),
            dstSize = IntSize((cell * 0.90f).roundToInt(), (cell * 1.02f).roundToInt())
        )
    }
}

private fun DrawScope.drawDungeonAtlasTileV035(
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
