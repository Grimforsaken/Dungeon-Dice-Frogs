package com.grimforsaken.dungeondicefrogs

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.random.Random

enum class V2DungeonStyle { CONSTRUCTED_STONE, NATURAL_CAVE }
enum class V2DungeonSize { SMALL, MEDIUM, LARGE, HUGE }
enum class V2RoomType {
    ENTRANCE, EXIT, NORMAL, HALLWAY, TREASURE, BOSS, TRAP, SECRET,
    PILLAR, LARGE_CHAMBER, DEAD_END, SPECIAL_FEATURE, EMPTY
}
enum class V2RoomShape {
    STANDARD_SQUARE, RECTANGLE, WIDE_CHAMBER, NARROW_CHAMBER, HALLWAY,
    LONG_CORRIDOR, L_SHAPED, IRREGULAR, PILLAR_CHAMBER, OPEN_CHAMBER,
    CAVE_CHAMBER, CAVE_TUNNEL, BOSS_ARENA
}
enum class V2WallKind { SOLID, DOORWAY, OPEN, SECRET }
enum class V2FeatureType {
    PILLAR, RUBBLE_DECORATIVE, RUBBLE_BLOCKING, STATUE, COLLAPSE,
    ROCK_SPIRE, WATER, MUD, CRYSTAL, GROWTH, SPECIAL
}

data class V2DungeonCoord(val x: Int, val y: Int) {
    val id: String get() = "$x,$y"
}

data class V2DungeonDoorway(
    val wall: DungeonDirection,
    val offset: Int,
    val kind: V2WallKind = V2WallKind.DOORWAY
)

data class V2DungeonStairs(val type: String, val variant: Int, val x: Int, val y: Int)
data class V2DungeonInteriorWall(val x: Int, val y: Int, val variant: Int)

data class V2DungeonFeature(
    val id: String,
    val type: V2FeatureType,
    val x: Int,
    val y: Int,
    val variant: Int,
    val blocking: Boolean
)

data class V2TreasureSpawn(
    val id: String,
    val x: Int,
    val y: Int,
    val table: String,
    val bossChest: Boolean = false
)

data class V2EncounterSpawn(
    val id: String,
    val x: Int,
    val y: Int,
    val table: String,
    val boss: Boolean = false
)

data class V2SharedWall(
    val key: String,
    val kind: V2WallKind,
    val doorwayOffset: Int?,
    val wallVariants: List<Int>,
    val doorVariant: Int = 1
)

data class V2DungeonRoomData(
    val x: Int,
    val y: Int,
    var present: Boolean = false,
    var type: V2RoomType = V2RoomType.EMPTY,
    var shape: V2RoomShape = V2RoomShape.STANDARD_SQUARE,
    val connections: MutableSet<DungeonDirection> = mutableSetOf(),
    val doorways: MutableList<V2DungeonDoorway> = mutableListOf(),
    val interiorWalls: MutableList<V2DungeonInteriorWall> = mutableListOf(),
    val features: MutableList<V2DungeonFeature> = mutableListOf(),
    val treasure: MutableList<V2TreasureSpawn> = mutableListOf(),
    val encounters: MutableList<V2EncounterSpawn> = mutableListOf(),
    var stairs: V2DungeonStairs? = null,
    var mergedGroup: String? = null,
    var floorTileVariants: List<Int> = emptyList()
) {
    val coord: V2DungeonCoord get() = V2DungeonCoord(x, y)
    val pillars: List<V2DungeonFeature> get() = features.filter { it.type == V2FeatureType.PILLAR }
}

data class V2GenerationRequest(
    val style: V2DungeonStyle = V2DungeonStyle.CONSTRUCTED_STONE,
    val size: V2DungeonSize? = null,
    val targetRoomCount: Int? = null,
    val treasureRooms: Int? = null,
    val secretRooms: Int? = null,
    val trapRooms: Int? = null,
    val largeChambers: Int? = null,
    val pillarRooms: Int? = null,
    val allowDeadEnds: Boolean = true,
    val allowLoops: Boolean = true
)

data class V2DungeonFloorData(
    val schemaVersion: Int = 2,
    val tier: Int,
    val floor: Int,
    val style: V2DungeonStyle,
    val theme: String,
    val seed: Long,
    val generated: Boolean,
    val size: V2DungeonSize,
    val requestedRoomCount: Int,
    val startRoom: V2DungeonCoord,
    val stairsUpRoom: V2DungeonCoord,
    val bossRoom: V2DungeonCoord?,
    val bossRequired: Boolean,
    val rooms: List<V2DungeonRoomData>,
    val sharedWalls: Map<String, V2SharedWall>,
    val importedLegacyFloor: Boolean = false
) {
    val roomCount: Int get() = rooms.count { it.present }
    val presentRooms: List<V2DungeonRoomData> get() = rooms.filter { it.present }

    fun roomAt(x: Int, y: Int): V2DungeonRoomData {
        require(x in 0..9 && y in 0..9) { "Room coordinate outside floor: $x,$y" }
        return rooms[y * 10 + x]
    }

    fun wallFor(room: V2DungeonRoomData, direction: DungeonDirection): V2SharedWall =
        sharedWalls[V2DungeonGenerator.wallKey(room.x, room.y, direction)]
            ?: error("Missing shared wall for ${room.coord.id} $direction")
}

data class V2DungeonFloorState(
    val floor: Int,
    var currentRoom: V2DungeonCoord,
    val discoveredRooms: MutableSet<String> = mutableSetOf(),
    val openedChests: MutableSet<String> = mutableSetOf(),
    val defeatedEnemies: MutableSet<String> = mutableSetOf(),
    val collectedLoot: MutableSet<String> = mutableSetOf(),
    val unlockedDoors: MutableSet<String> = mutableSetOf(),
    val triggeredTraps: MutableSet<String> = mutableSetOf(),
    val discoveredSecretRooms: MutableSet<String> = mutableSetOf(),
    val brokenObjects: MutableSet<String> = mutableSetOf(),
    val usedItems: MutableSet<String> = mutableSetOf(),
    var bossDefeated: Boolean = false
)

object V2DungeonFloorRepository {
    private fun floorDir(context: Context) = File(context.filesDir, "dungeon_floors_v2").apply { mkdirs() }
    private fun stateDir(context: Context) = File(context.filesDir, "dungeon_states_v2").apply { mkdirs() }
    private fun floorFile(context: Context, floor: Int) = File(floorDir(context), "floor_${floor.toString().padStart(3, '0')}.json")
    private fun stateFile(context: Context, floor: Int) = File(stateDir(context), "floor_${floor.toString().padStart(3, '0')}_state.json")
    private fun legacyFloorFile(context: Context, floor: Int) = File(context.filesDir, "dungeon_floors/floor_${floor.toString().padStart(3, '0')}.json")
    private fun legacyStateFile(context: Context, floor: Int) = File(context.filesDir, "dungeon_states/floor_${floor.toString().padStart(3, '0')}_state.json")

    fun loadOrGenerate(
        context: Context,
        floorNumber: Int,
        request: V2GenerationRequest? = null
    ): V2DungeonFloorData {
        val file = floorFile(context, floorNumber)
        if (file.exists()) {
            return runCatching { floorFromJson(JSONObject(file.readText())) }
                .getOrElse { throw IllegalStateException("Saved v2 dungeon floor is unreadable; refusing regeneration", it) }
        }

        // Preserve an already-generated legacy floor instead of silently replacing it.
        if (legacyFloorFile(context, floorNumber).exists()) {
            val legacy = DungeonFloorRepository.loadOrGenerate(context, floorNumber)
            val migrated = migrateLegacyFloor(legacy)
            atomicWrite(file, floorToJson(migrated).toString())
            return migrated
        }

        val initialSeed = SecureRandom().nextLong()
        repeat(32) { attempt ->
            val seed = initialSeed + attempt * 104729L
            val resolvedRequest = request ?: V2DungeonGenerator.defaultRequest(floorNumber, seed)
            val generated = V2DungeonGenerator.generate(floorNumber, seed, resolvedRequest)
            val errors = V2DungeonGenerator.validate(generated)
            if (errors.isEmpty()) {
                atomicWrite(file, floorToJson(generated).toString())
                return generated
            }
        }
        error("Could not generate a valid v2 dungeon floor after 32 attempts")
    }

    fun regenerateForDevelopment(
        context: Context,
        floorNumber: Int,
        request: V2GenerationRequest? = null
    ): V2DungeonFloorData {
        floorFile(context, floorNumber).delete()
        stateFile(context, floorNumber).delete()
        return loadOrGenerate(context, floorNumber, request)
    }

    fun loadState(context: Context, floor: V2DungeonFloorData): V2DungeonFloorState {
        val file = stateFile(context, floor.floor)
        if (file.exists()) {
            return runCatching { stateFromJson(JSONObject(file.readText())) }
                .getOrElse {
                    V2DungeonFloorState(floor.floor, floor.startRoom, mutableSetOf(floor.startRoom.id)).also { saveState(context, it) }
                }
        }

        if (floor.importedLegacyFloor && legacyStateFile(context, floor.floor).exists()) {
            val legacyFloor = DungeonFloorRepository.loadOrGenerate(context, floor.floor)
            val legacyState = DungeonFloorRepository.loadState(context, legacyFloor)
            val migrated = V2DungeonFloorState(
                floor = floor.floor,
                currentRoom = V2DungeonCoord(legacyState.currentRoom.x, legacyState.currentRoom.y),
                discoveredRooms = legacyState.discoveredRooms.toMutableSet(),
                openedChests = legacyState.openedChests.toMutableSet(),
                defeatedEnemies = legacyState.defeatedEnemies.toMutableSet(),
                collectedLoot = legacyState.collectedLoot.toMutableSet(),
                unlockedDoors = legacyState.unlockedDoors.toMutableSet(),
                triggeredTraps = legacyState.triggeredTraps.toMutableSet(),
                bossDefeated = legacyState.bossDefeated
            )
            saveState(context, migrated)
            return migrated
        }

        return V2DungeonFloorState(
            floor = floor.floor,
            currentRoom = floor.startRoom,
            discoveredRooms = mutableSetOf(floor.startRoom.id)
        ).also { saveState(context, it) }
    }

    fun saveState(context: Context, state: V2DungeonFloorState) {
        atomicWrite(stateFile(context, state.floor), stateToJson(state).toString())
    }

    private fun atomicWrite(file: File, text: String) {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, file.name + ".tmp")
        temporary.writeText(text)
        if (file.exists()) file.delete()
        check(temporary.renameTo(file)) { "Could not atomically save ${file.name}" }
    }

    private fun coordJson(coord: V2DungeonCoord) = JSONObject().apply {
        put("x", coord.x)
        put("y", coord.y)
    }

    private fun stringArray(values: Set<String>) = JSONArray().also { array -> values.sorted().forEach(array::put) }
    private fun intArray(values: List<Int>) = JSONArray().also { array -> values.forEach(array::put) }

    private fun floorToJson(floor: V2DungeonFloorData) = JSONObject().apply {
        put("schemaVersion", floor.schemaVersion)
        put("tier", floor.tier)
        put("floor", floor.floor)
        put("style", floor.style.name)
        put("theme", floor.theme)
        put("seed", floor.seed)
        put("generated", floor.generated)
        put("size", floor.size.name)
        put("requestedRoomCount", floor.requestedRoomCount)
        put("startRoom", coordJson(floor.startRoom))
        put("stairsUpRoom", coordJson(floor.stairsUpRoom))
        put("bossRoom", floor.bossRoom?.let(::coordJson) ?: JSONObject.NULL)
        put("bossRequired", floor.bossRequired)
        put("importedLegacyFloor", floor.importedLegacyFloor)

        put("sharedWalls", JSONArray().also { array ->
            floor.sharedWalls.values.sortedBy { it.key }.forEach { wall ->
                array.put(JSONObject().apply {
                    put("key", wall.key)
                    put("kind", wall.kind.name)
                    put("doorwayOffset", wall.doorwayOffset ?: JSONObject.NULL)
                    put("wallVariants", intArray(wall.wallVariants))
                    put("doorVariant", wall.doorVariant)
                })
            }
        })

        put("rooms", JSONArray().also { array ->
            floor.rooms.forEach { room ->
                array.put(JSONObject().apply {
                    put("x", room.x)
                    put("y", room.y)
                    put("present", room.present)
                    put("type", room.type.name)
                    put("shape", room.shape.name)
                    put("mergedGroup", room.mergedGroup ?: JSONObject.NULL)
                    put("connections", JSONArray().also { a -> room.connections.sortedBy { it.ordinal }.forEach { a.put(it.name) } })
                    put("doorways", JSONArray().also { a -> room.doorways.forEach { d ->
                        a.put(JSONObject().apply { put("wall", d.wall.name); put("offset", d.offset); put("kind", d.kind.name) })
                    } })
                    put("interiorWalls", JSONArray().also { a -> room.interiorWalls.forEach { w ->
                        a.put(JSONObject().apply { put("x", w.x); put("y", w.y); put("variant", w.variant) })
                    } })
                    put("features", JSONArray().also { a -> room.features.forEach { feature ->
                        a.put(JSONObject().apply {
                            put("id", feature.id); put("type", feature.type.name); put("x", feature.x); put("y", feature.y)
                            put("variant", feature.variant); put("blocking", feature.blocking)
                        })
                    } })
                    put("treasure", JSONArray().also { a -> room.treasure.forEach { spawn ->
                        a.put(JSONObject().apply {
                            put("id", spawn.id); put("x", spawn.x); put("y", spawn.y); put("table", spawn.table); put("bossChest", spawn.bossChest)
                        })
                    } })
                    put("encounters", JSONArray().also { a -> room.encounters.forEach { spawn ->
                        a.put(JSONObject().apply {
                            put("id", spawn.id); put("x", spawn.x); put("y", spawn.y); put("table", spawn.table); put("boss", spawn.boss)
                        })
                    } })
                    put("floorTileVariants", intArray(room.floorTileVariants))
                    put("stairs", room.stairs?.let { stairs ->
                        JSONObject().apply {
                            put("type", stairs.type); put("variant", stairs.variant); put("x", stairs.x); put("y", stairs.y)
                        }
                    } ?: JSONObject.NULL)
                })
            }
        })
    }

    private fun floorFromJson(json: JSONObject): V2DungeonFloorData {
        val walls = mutableMapOf<String, V2SharedWall>()
        val wallArray = json.getJSONArray("sharedWalls")
        for (i in 0 until wallArray.length()) {
            val w = wallArray.getJSONObject(i)
            val variants = w.getJSONArray("wallVariants")
            val wall = V2SharedWall(
                key = w.getString("key"),
                kind = V2WallKind.valueOf(w.getString("kind")),
                doorwayOffset = if (w.isNull("doorwayOffset")) null else w.getInt("doorwayOffset"),
                wallVariants = List(variants.length()) { variants.getInt(it) },
                doorVariant = w.optInt("doorVariant", 1)
            )
            walls[wall.key] = wall
        }

        val rooms = mutableListOf<V2DungeonRoomData>()
        val roomArray = json.getJSONArray("rooms")
        for (i in 0 until roomArray.length()) {
            val r = roomArray.getJSONObject(i)
            val room = V2DungeonRoomData(
                x = r.getInt("x"),
                y = r.getInt("y"),
                present = r.optBoolean("present", true),
                type = V2RoomType.valueOf(r.optString("type", V2RoomType.NORMAL.name)),
                shape = V2RoomShape.valueOf(r.optString("shape", V2RoomShape.STANDARD_SQUARE.name))
            )
            if (!r.isNull("mergedGroup")) room.mergedGroup = r.getString("mergedGroup")
            val connections = r.optJSONArray("connections") ?: JSONArray()
            for (j in 0 until connections.length()) room.connections += DungeonDirection.valueOf(connections.getString(j))
            val doorways = r.optJSONArray("doorways") ?: JSONArray()
            for (j in 0 until doorways.length()) {
                val d = doorways.getJSONObject(j)
                room.doorways += V2DungeonDoorway(
                    DungeonDirection.valueOf(d.getString("wall")),
                    d.getInt("offset"),
                    V2WallKind.valueOf(d.optString("kind", V2WallKind.DOORWAY.name))
                )
            }
            val interior = r.optJSONArray("interiorWalls") ?: JSONArray()
            for (j in 0 until interior.length()) {
                val w = interior.getJSONObject(j)
                room.interiorWalls += V2DungeonInteriorWall(w.getInt("x"), w.getInt("y"), w.getInt("variant"))
            }
            val features = r.optJSONArray("features") ?: JSONArray()
            for (j in 0 until features.length()) {
                val f = features.getJSONObject(j)
                room.features += V2DungeonFeature(
                    f.getString("id"), V2FeatureType.valueOf(f.getString("type")), f.getInt("x"), f.getInt("y"),
                    f.optInt("variant", 1), f.optBoolean("blocking", false)
                )
            }
            val treasure = r.optJSONArray("treasure") ?: JSONArray()
            for (j in 0 until treasure.length()) {
                val t = treasure.getJSONObject(j)
                room.treasure += V2TreasureSpawn(t.getString("id"), t.getInt("x"), t.getInt("y"), t.getString("table"), t.optBoolean("bossChest", false))
            }
            val encounters = r.optJSONArray("encounters") ?: JSONArray()
            for (j in 0 until encounters.length()) {
                val e = encounters.getJSONObject(j)
                room.encounters += V2EncounterSpawn(e.getString("id"), e.getInt("x"), e.getInt("y"), e.getString("table"), e.optBoolean("boss", false))
            }
            val tiles = r.optJSONArray("floorTileVariants") ?: JSONArray()
            room.floorTileVariants = if (tiles.length() == 100) List(100) { tiles.getInt(it) } else List(100) { 1 }
            if (!r.isNull("stairs")) {
                val s = r.getJSONObject("stairs")
                room.stairs = V2DungeonStairs(s.getString("type"), s.getInt("variant"), s.getInt("x"), s.getInt("y"))
            }
            rooms += room
        }

        return V2DungeonFloorData(
            schemaVersion = json.optInt("schemaVersion", 2),
            tier = json.getInt("tier"),
            floor = json.getInt("floor"),
            style = V2DungeonStyle.valueOf(json.optString("style", V2DungeonStyle.CONSTRUCTED_STONE.name)),
            theme = json.optString("theme", "greystone"),
            seed = json.getLong("seed"),
            generated = json.optBoolean("generated", true),
            size = V2DungeonSize.valueOf(json.optString("size", V2DungeonSize.HUGE.name)),
            requestedRoomCount = json.optInt("requestedRoomCount", rooms.count { it.present }),
            startRoom = coordFromJson(json.getJSONObject("startRoom")),
            stairsUpRoom = coordFromJson(json.getJSONObject("stairsUpRoom")),
            bossRoom = if (json.isNull("bossRoom")) null else coordFromJson(json.getJSONObject("bossRoom")),
            bossRequired = json.optBoolean("bossRequired", false),
            rooms = rooms,
            sharedWalls = walls,
            importedLegacyFloor = json.optBoolean("importedLegacyFloor", false)
        )
    }

    private fun stateToJson(state: V2DungeonFloorState) = JSONObject().apply {
        put("floor", state.floor)
        put("currentRoom", coordJson(state.currentRoom))
        put("discoveredRooms", stringArray(state.discoveredRooms))
        put("openedChests", stringArray(state.openedChests))
        put("defeatedEnemies", stringArray(state.defeatedEnemies))
        put("collectedLoot", stringArray(state.collectedLoot))
        put("unlockedDoors", stringArray(state.unlockedDoors))
        put("triggeredTraps", stringArray(state.triggeredTraps))
        put("discoveredSecretRooms", stringArray(state.discoveredSecretRooms))
        put("brokenObjects", stringArray(state.brokenObjects))
        put("usedItems", stringArray(state.usedItems))
        put("bossDefeated", state.bossDefeated)
    }

    private fun stateFromJson(json: JSONObject) = V2DungeonFloorState(
        floor = json.getInt("floor"),
        currentRoom = coordFromJson(json.getJSONObject("currentRoom")),
        discoveredRooms = stringSet(json.optJSONArray("discoveredRooms")),
        openedChests = stringSet(json.optJSONArray("openedChests")),
        defeatedEnemies = stringSet(json.optJSONArray("defeatedEnemies")),
        collectedLoot = stringSet(json.optJSONArray("collectedLoot")),
        unlockedDoors = stringSet(json.optJSONArray("unlockedDoors")),
        triggeredTraps = stringSet(json.optJSONArray("triggeredTraps")),
        discoveredSecretRooms = stringSet(json.optJSONArray("discoveredSecretRooms")),
        brokenObjects = stringSet(json.optJSONArray("brokenObjects")),
        usedItems = stringSet(json.optJSONArray("usedItems")),
        bossDefeated = json.optBoolean("bossDefeated", false)
    )

    private fun coordFromJson(json: JSONObject) = V2DungeonCoord(json.getInt("x"), json.getInt("y"))
    private fun stringSet(array: JSONArray?) = mutableSetOf<String>().apply {
        if (array != null) for (i in 0 until array.length()) add(array.getString(i))
    }

    private fun migrateLegacyFloor(legacy: DungeonFloorData): V2DungeonFloorData {
        val migratedRooms = legacy.rooms.map { old ->
            V2DungeonRoomData(
                x = old.x,
                y = old.y,
                present = true,
                type = when (old.type) {
                    DungeonRoomType.STAIRS_DOWN -> V2RoomType.ENTRANCE
                    DungeonRoomType.STAIRS_UP -> V2RoomType.EXIT
                    DungeonRoomType.BOSS -> V2RoomType.BOSS
                    DungeonRoomType.HALLWAY -> V2RoomType.HALLWAY
                    DungeonRoomType.PILLAR -> V2RoomType.PILLAR
                    DungeonRoomType.OPEN -> V2RoomType.LARGE_CHAMBER
                    else -> V2RoomType.NORMAL
                },
                shape = when (old.type) {
                    DungeonRoomType.HALLWAY -> V2RoomShape.HALLWAY
                    DungeonRoomType.PILLAR -> V2RoomShape.PILLAR_CHAMBER
                    DungeonRoomType.OPEN -> V2RoomShape.OPEN_CHAMBER
                    DungeonRoomType.BOSS -> V2RoomShape.BOSS_ARENA
                    else -> V2RoomShape.STANDARD_SQUARE
                },
                connections = old.connections.toMutableSet(),
                doorways = old.doorways.map { V2DungeonDoorway(it.wall, it.offset) }.toMutableList(),
                interiorWalls = old.interiorWalls.map { V2DungeonInteriorWall(it.x, it.y, it.variant) }.toMutableList(),
                features = old.pillars.mapIndexed { index, p ->
                    V2DungeonFeature("legacy_${old.x}_${old.y}_pillar_$index", V2FeatureType.PILLAR, p.x, p.y, p.variant, true)
                }.toMutableList(),
                stairs = old.stairs?.let { V2DungeonStairs(it.type, it.variant, it.x, it.y) },
                mergedGroup = old.mergedGroup,
                floorTileVariants = old.floorTileVariants
            )
        }
        val migratedWalls = legacy.sharedWalls.mapValues { (_, old) ->
            V2SharedWall(
                key = old.key,
                kind = when (old.kind) {
                    SharedWallKind.SOLID -> V2WallKind.SOLID
                    SharedWallKind.DOORWAY -> V2WallKind.DOORWAY
                    SharedWallKind.OPEN -> V2WallKind.OPEN
                },
                doorwayOffset = old.doorwayOffset,
                wallVariants = old.wallVariants,
                doorVariant = 1
            )
        }
        return V2DungeonFloorData(
            tier = legacy.tier,
            floor = legacy.floor,
            style = V2DungeonStyle.CONSTRUCTED_STONE,
            theme = legacy.theme,
            seed = legacy.seed,
            generated = legacy.generated,
            size = V2DungeonSize.HUGE,
            requestedRoomCount = 100,
            startRoom = V2DungeonCoord(legacy.startRoom.x, legacy.startRoom.y),
            stairsUpRoom = V2DungeonCoord(legacy.stairsUpRoom.x, legacy.stairsUpRoom.y),
            bossRoom = legacy.bossRoom?.let { V2DungeonCoord(it.x, it.y) },
            bossRequired = legacy.bossRequired,
            rooms = migratedRooms,
            sharedWalls = migratedWalls,
            importedLegacyFloor = true
        )
    }
}

object V2DungeonGenerator {
    private const val GRID = 10

    fun wallKey(x: Int, y: Int, direction: DungeonDirection): String = when (direction) {
        DungeonDirection.NORTH -> if (y == 0) "N:$x" else "H:$x:${y - 1}"
        DungeonDirection.SOUTH -> if (y == 9) "S:$x" else "H:$x:$y"
        DungeonDirection.WEST -> if (x == 0) "W:$y" else "V:${x - 1}:$y"
        DungeonDirection.EAST -> if (x == 9) "E:$y" else "V:$x:$y"
    }

    fun defaultRequest(floorNumber: Int, seed: Long): V2GenerationRequest {
        val random = randomFor(seed xor 0xDDF204L)
        val boss = floorNumber % 10 == 0
        val size = if (boss) {
            if (random.nextBoolean()) V2DungeonSize.LARGE else V2DungeonSize.HUGE
        } else {
            val roll = random.nextInt(100)
            when {
                roll < 22 -> V2DungeonSize.SMALL
                roll < 62 -> V2DungeonSize.MEDIUM
                roll < 88 -> V2DungeonSize.LARGE
                else -> V2DungeonSize.HUGE
            }
        }
        return V2GenerationRequest(
            style = V2DungeonStyle.CONSTRUCTED_STONE,
            size = size,
            allowDeadEnds = true,
            allowLoops = true
        )
    }

    fun generate(floorNumber: Int, seed: Long, request: V2GenerationRequest): V2DungeonFloorData {
        val random = randomFor(seed)
        val tier = (floorNumber - 1) / 10 + 1
        val bossFloor = floorNumber % 10 == 0
        val size = request.size ?: chooseSize(random, bossFloor)
        val target = (request.targetRoomCount ?: roomCountForSize(size, random, bossFloor)).coerceIn(15, 100)

        val rooms = List(100) { index -> V2DungeonRoomData(index % GRID, index / GRID) }.toMutableList()
        val start = chooseEdgeStart(random)
        val occupied = growFootprint(start, target, random)
        occupied.forEach { rooms[index(it)].present = true }

        val links = Array(100) { mutableSetOf<Int>() }
        makeSpanningTree(start, occupied, links, random)
        if (request.allowLoops) addLoops(occupied, links, random, target)

        val exit = farthestRoom(start, occupied, links)
        val mainPath = shortestPath(start, exit, links)
        val bossRoom = if (bossFloor) exit else null

        val counts = assignRoomRoles(
            rooms = rooms,
            occupied = occupied,
            links = links,
            start = start,
            exit = exit,
            mainPath = mainPath,
            bossFloor = bossFloor,
            request = request,
            random = random
        )

        val secretEdges = chooseSecretEdges(rooms, links, counts.secretRooms)
        val walls = generateSharedWalls(rooms, links, secretEdges, random)
        populateConnections(rooms, walls)
        assignRoomShapes(rooms, links, request.style, random)
        assignVisualTiles(rooms, request.style, random)
        placeStairs(rooms, start, exit, random)
        carveRoomShapes(rooms, walls, request.style, random)
        placeFeatures(rooms, request.style, random)
        placeTreasureAndEncounters(rooms, tier, floorNumber, bossFloor, random)

        return V2DungeonFloorData(
            tier = tier,
            floor = floorNumber,
            style = request.style,
            theme = if (request.style == V2DungeonStyle.NATURAL_CAVE) "natural_cave" else if (tier == 1) "greystone" else "constructed_stone",
            seed = seed,
            generated = true,
            size = size,
            requestedRoomCount = target,
            startRoom = start,
            stairsUpRoom = exit,
            bossRoom = bossRoom,
            bossRequired = bossFloor,
            rooms = rooms,
            sharedWalls = walls
        )
    }

    private data class AssignedCounts(val secretRooms: Int)

    private fun assignRoomRoles(
        rooms: MutableList<V2DungeonRoomData>,
        occupied: Set<V2DungeonCoord>,
        links: Array<MutableSet<Int>>,
        start: V2DungeonCoord,
        exit: V2DungeonCoord,
        mainPath: List<V2DungeonCoord>,
        bossFloor: Boolean,
        request: V2GenerationRequest,
        random: Random
    ): AssignedCounts {
        val present = occupied.map { rooms[index(it)] }
        present.forEach { it.type = V2RoomType.NORMAL }
        rooms[index(start)].type = V2RoomType.ENTRANCE
        rooms[index(exit)].type = if (bossFloor) V2RoomType.BOSS else V2RoomType.EXIT

        val mainIds = mainPath.map { it.id }.toSet()
        val candidates = present.filter { it.coord != start && it.coord != exit }.toMutableList()
        val leaves = candidates.filter { links[index(it.coord)].size == 1 && it.coord.id !in mainIds }.shuffled(random)
        val optional = candidates.filter { it.coord.id !in mainIds }.shuffled(random)

        val treasureTarget = (request.treasureRooms ?: (present.size / 14).coerceIn(1, 6))
        val secretTarget = (request.secretRooms ?: (present.size / 24).coerceIn(0, 4))
        val trapTarget = (request.trapRooms ?: (present.size / 15).coerceIn(1, 6))
        val largeTarget = (request.largeChambers ?: (present.size / 16).coerceIn(1, 5))
        val pillarTarget = (request.pillarRooms ?: (present.size / 22).coerceIn(1, 4))

        fun assignFrom(pool: List<V2DungeonRoomData>, target: Int, type: V2RoomType) {
            pool.filter { it.type == V2RoomType.NORMAL }.take(target).forEach { it.type = type }
        }

        assignFrom(leaves + optional, treasureTarget, V2RoomType.TREASURE)
        val secretCandidates = leaves.filter { it.type == V2RoomType.NORMAL }.take(secretTarget)
        secretCandidates.forEach { it.type = V2RoomType.SECRET }
        assignFrom(optional, trapTarget, V2RoomType.TRAP)
        assignFrom(candidates.shuffled(random), largeTarget, V2RoomType.LARGE_CHAMBER)
        assignFrom(candidates.shuffled(random), pillarTarget, V2RoomType.PILLAR)

        for (room in candidates) {
            if (room.type != V2RoomType.NORMAL) continue
            val degree = links[index(room.coord)].size
            if (degree == 1 && request.allowDeadEnds) room.type = V2RoomType.DEAD_END
            else if (degree == 2 && isStraightConnection(room.coord, links[index(room.coord)])) room.type = V2RoomType.HALLWAY
            else if (degree >= 3 && random.nextFloat() < 0.25f) room.type = V2RoomType.HALLWAY
            else if (random.nextFloat() < 0.05f) room.type = V2RoomType.SPECIAL_FEATURE
        }
        return AssignedCounts(secretCandidates.size)
    }

    private fun chooseSecretEdges(
        rooms: List<V2DungeonRoomData>,
        links: Array<MutableSet<Int>>,
        secretCount: Int
    ): Set<String> {
        if (secretCount <= 0) return emptySet()
        val result = mutableSetOf<String>()
        rooms.filter { it.present && it.type == V2RoomType.SECRET }.forEach { room ->
            val roomIndex = index(room.coord)
            val neighbor = links[roomIndex].firstOrNull() ?: return@forEach
            result += canonicalEdge(roomIndex, neighbor)
        }
        return result
    }

    private fun generateSharedWalls(
        rooms: List<V2DungeonRoomData>,
        links: Array<MutableSet<Int>>,
        secretEdges: Set<String>,
        random: Random
    ): Map<String, V2SharedWall> {
        val result = mutableMapOf<String, V2SharedWall>()
        fun variants() = List(10) { random.nextInt(1, 11) }

        for (x in 0..9) {
            result["N:$x"] = V2SharedWall("N:$x", V2WallKind.SOLID, null, variants())
            result["S:$x"] = V2SharedWall("S:$x", V2WallKind.SOLID, null, variants())
        }
        for (y in 0..9) {
            result["W:$y"] = V2SharedWall("W:$y", V2WallKind.SOLID, null, variants())
            result["E:$y"] = V2SharedWall("E:$y", V2WallKind.SOLID, null, variants())
        }

        for (y in 0..9) for (x in 0..8) {
            val a = y * 10 + x
            val b = y * 10 + x + 1
            val key = "V:$x:$y"
            result[key] = wallBetween(a, b, key, rooms, links, secretEdges, variants(), random)
        }
        for (y in 0..8) for (x in 0..9) {
            val a = y * 10 + x
            val b = (y + 1) * 10 + x
            val key = "H:$x:$y"
            result[key] = wallBetween(a, b, key, rooms, links, secretEdges, variants(), random)
        }
        return result
    }

    private fun wallBetween(
        a: Int,
        b: Int,
        key: String,
        rooms: List<V2DungeonRoomData>,
        links: Array<MutableSet<Int>>,
        secretEdges: Set<String>,
        variants: List<Int>,
        random: Random
    ): V2SharedWall {
        if (!rooms[a].present || !rooms[b].present || b !in links[a]) {
            return V2SharedWall(key, V2WallKind.SOLID, null, variants)
        }
        val edge = canonicalEdge(a, b)
        val secret = edge in secretEdges
        val canOpen = !secret && rooms[a].type in setOf(V2RoomType.LARGE_CHAMBER, V2RoomType.BOSS) &&
            rooms[b].type in setOf(V2RoomType.LARGE_CHAMBER, V2RoomType.BOSS) && random.nextFloat() < 0.35f
        val kind = when {
            secret -> V2WallKind.SECRET
            canOpen -> V2WallKind.OPEN
            else -> V2WallKind.DOORWAY
        }
        val offset = if (kind == V2WallKind.OPEN) null else random.nextInt(2, 8)
        return V2SharedWall(key, kind, offset, variants, random.nextInt(1, 10))
    }

    private fun populateConnections(rooms: MutableList<V2DungeonRoomData>, walls: Map<String, V2SharedWall>) {
        rooms.forEach { room ->
            room.connections.clear()
            room.doorways.clear()
            if (!room.present) return@forEach
            for (direction in DungeonDirection.values()) {
                val wall = walls[wallKey(room.x, room.y, direction)] ?: continue
                if (wall.kind != V2WallKind.SOLID) room.connections += direction
                if (wall.kind == V2WallKind.DOORWAY || wall.kind == V2WallKind.SECRET) {
                    room.doorways += V2DungeonDoorway(direction, wall.doorwayOffset ?: 4, wall.kind)
                }
            }
        }
    }

    private fun assignRoomShapes(
        rooms: MutableList<V2DungeonRoomData>,
        links: Array<MutableSet<Int>>,
        style: V2DungeonStyle,
        random: Random
    ) {
        rooms.filter { it.present }.forEach { room ->
            room.shape = when {
                room.type == V2RoomType.BOSS -> V2RoomShape.BOSS_ARENA
                style == V2DungeonStyle.NATURAL_CAVE && room.type == V2RoomType.HALLWAY -> V2RoomShape.CAVE_TUNNEL
                style == V2DungeonStyle.NATURAL_CAVE -> if (random.nextFloat() < 0.65f) V2RoomShape.CAVE_CHAMBER else V2RoomShape.IRREGULAR
                room.type == V2RoomType.HALLWAY -> if (random.nextBoolean()) V2RoomShape.HALLWAY else V2RoomShape.LONG_CORRIDOR
                room.type == V2RoomType.PILLAR -> V2RoomShape.PILLAR_CHAMBER
                room.type == V2RoomType.LARGE_CHAMBER -> if (random.nextBoolean()) V2RoomShape.OPEN_CHAMBER else V2RoomShape.WIDE_CHAMBER
                room.type == V2RoomType.TREASURE -> if (random.nextBoolean()) V2RoomShape.RECTANGLE else V2RoomShape.STANDARD_SQUARE
                room.type == V2RoomType.DEAD_END -> if (random.nextBoolean()) V2RoomShape.NARROW_CHAMBER else V2RoomShape.RECTANGLE
                else -> when (random.nextInt(100)) {
                    in 0..54 -> V2RoomShape.STANDARD_SQUARE
                    in 55..69 -> V2RoomShape.RECTANGLE
                    in 70..79 -> V2RoomShape.L_SHAPED
                    in 80..89 -> V2RoomShape.IRREGULAR
                    else -> V2RoomShape.OPEN_CHAMBER
                }
            }
        }
    }

    private fun assignVisualTiles(rooms: MutableList<V2DungeonRoomData>, style: V2DungeonStyle, random: Random) {
        rooms.filter { it.present }.forEach { room ->
            val base = random.nextInt(1, 11)
            room.floorTileVariants = List(100) { index ->
                val x = index % 10
                val y = index / 10
                val cluster = ((x / 3) + (y / 3) + base - 1) % 10 + 1
                val roll = random.nextFloat()
                when {
                    roll < 0.64f -> cluster
                    roll < 0.90f -> base
                    else -> random.nextInt(1, 11)
                }
            }
        }
    }

    private fun placeStairs(rooms: MutableList<V2DungeonRoomData>, start: V2DungeonCoord, exit: V2DungeonCoord, random: Random) {
        rooms[index(start)].stairs = V2DungeonStairs("down", random.nextInt(1, 4), 5, 5)
        rooms[index(exit)].stairs = V2DungeonStairs("up", random.nextInt(1, 4), 5, 5)
    }

    private fun carveRoomShapes(
        rooms: MutableList<V2DungeonRoomData>,
        walls: Map<String, V2SharedWall>,
        style: V2DungeonStyle,
        random: Random
    ) {
        rooms.filter { it.present }.forEach { room ->
            val reserved = reservedCells(room, walls)
            val candidates = buildList {
                for (y in 1..8) for (x in 1..8) if ((x to y) !in reserved) add(x to y)
            }.shuffled(random)

            val blockTarget = when (room.shape) {
                V2RoomShape.STANDARD_SQUARE, V2RoomShape.OPEN_CHAMBER, V2RoomShape.WIDE_CHAMBER, V2RoomShape.BOSS_ARENA -> 0
                V2RoomShape.RECTANGLE -> 8
                V2RoomShape.NARROW_CHAMBER -> 18
                V2RoomShape.HALLWAY, V2RoomShape.LONG_CORRIDOR -> 30
                V2RoomShape.L_SHAPED -> 15
                V2RoomShape.IRREGULAR -> 12
                V2RoomShape.PILLAR_CHAMBER -> 0
                V2RoomShape.CAVE_CHAMBER -> 14
                V2RoomShape.CAVE_TUNNEL -> 26
            }
            candidates.take(blockTarget).forEach { (x, y) ->
                room.interiorWalls += V2DungeonInteriorWall(x, y, random.nextInt(1, 11))
            }
        }
    }

    private fun reservedCells(room: V2DungeonRoomData, walls: Map<String, V2SharedWall>): Set<Pair<Int, Int>> {
        val reserved = mutableSetOf<Pair<Int, Int>>()
        val center = 5 to 5
        reserved += center
        room.stairs?.let { reserved += it.x to it.y }

        fun reservePath(fromX: Int, fromY: Int) {
            var x = fromX
            var y = fromY
            reserved += x to y
            while (x != center.first) {
                x += if (x < center.first) 1 else -1
                reserved += x to y
            }
            while (y != center.second) {
                y += if (y < center.second) 1 else -1
                reserved += x to y
            }
            for (dx in -1..1) for (dy in -1..1) {
                val px = fromX + dx
                val py = fromY + dy
                if (px in 1..8 && py in 1..8) reserved += px to py
            }
        }

        for (direction in room.connections) {
            val wall = walls[wallKey(room.x, room.y, direction)] ?: continue
            val offset = wall.doorwayOffset ?: 5
            when (direction) {
                DungeonDirection.NORTH -> reservePath(offset, 1)
                DungeonDirection.SOUTH -> reservePath(offset, 8)
                DungeonDirection.WEST -> reservePath(1, offset)
                DungeonDirection.EAST -> reservePath(8, offset)
            }
        }
        return reserved
    }

    private fun placeFeatures(rooms: MutableList<V2DungeonRoomData>, style: V2DungeonStyle, random: Random) {
        rooms.filter { it.present }.forEach { room ->
            val blocked = room.interiorWalls.map { it.x to it.y }.toMutableSet()
            room.stairs?.let { blocked += it.x to it.y }
            val safe = buildList {
                for (y in 2..7) for (x in 2..7) if ((x to y) !in blocked) add(x to y)
            }.shuffled(random).toMutableList()

            fun addFeature(type: V2FeatureType, blocking: Boolean, count: Int) {
                repeat(count) { n ->
                    val cell = safe.firstOrNull() ?: return@repeat
                    safe.remove(cell)
                    room.features += V2DungeonFeature(
                        id = "f_${room.x}_${room.y}_${type.name.lowercase()}_${room.features.size}",
                        type = type,
                        x = cell.first,
                        y = cell.second,
                        variant = random.nextInt(1, 13),
                        blocking = blocking
                    )
                    if (blocking) blocked += cell
                }
            }

            when (room.type) {
                V2RoomType.BOSS -> addFeature(V2FeatureType.PILLAR, true, 4)
                V2RoomType.PILLAR -> addFeature(V2FeatureType.PILLAR, true, if (random.nextBoolean()) 4 else 2)
                V2RoomType.LARGE_CHAMBER -> if (random.nextFloat() < 0.45f) addFeature(V2FeatureType.PILLAR, true, 2)
                else -> Unit
            }

            if (style == V2DungeonStyle.NATURAL_CAVE) {
                if (random.nextFloat() < 0.45f) addFeature(V2FeatureType.ROCK_SPIRE, true, 1)
                if (random.nextFloat() < 0.35f) addFeature(V2FeatureType.CRYSTAL, false, 1)
                if (random.nextFloat() < 0.25f) addFeature(V2FeatureType.WATER, false, 1)
                if (random.nextFloat() < 0.30f) addFeature(V2FeatureType.GROWTH, false, 1)
            } else {
                if (random.nextFloat() < 0.25f) addFeature(V2FeatureType.RUBBLE_DECORATIVE, false, 1)
                if (room.type == V2RoomType.DEAD_END && random.nextFloat() < 0.35f) addFeature(V2FeatureType.RUBBLE_BLOCKING, true, 1)
                if (room.type == V2RoomType.SPECIAL_FEATURE) addFeature(V2FeatureType.STATUE, true, 1)
            }
        }
    }

    private fun placeTreasureAndEncounters(
        rooms: MutableList<V2DungeonRoomData>,
        tier: Int,
        floorNumber: Int,
        bossFloor: Boolean,
        random: Random
    ) {
        rooms.filter { it.present }.forEach { room ->
            val blocked = room.interiorWalls.map { it.x to it.y }.toMutableSet()
            blocked += room.features.filter { it.blocking }.map { it.x to it.y }
            room.stairs?.let { blocked += it.x to it.y }
            val safe = buildList {
                for (y in 2..7) for (x in 2..7) if ((x to y) !in blocked) add(x to y)
            }.shuffled(random)

            if (room.type == V2RoomType.TREASURE || room.type == V2RoomType.SECRET) {
                safe.firstOrNull()?.let { cell ->
                    room.treasure += V2TreasureSpawn(
                        id = "chest_${floorNumber}_${room.x}_${room.y}",
                        x = cell.first,
                        y = cell.second,
                        table = if (room.type == V2RoomType.SECRET) "tier_${tier}_secret" else "tier_${tier}_treasure"
                    )
                }
            }

            if (room.type == V2RoomType.BOSS) {
                safe.getOrNull(0)?.let { cell ->
                    room.encounters += V2EncounterSpawn("boss_${floorNumber}", cell.first, cell.second, "tier_${tier}_boss", true)
                }
                safe.getOrNull(1)?.let { cell ->
                    room.treasure += V2TreasureSpawn("boss_chest_${floorNumber}", cell.first, cell.second, "tier_${tier}_boss_chest", true)
                }
            } else if (room.type !in setOf(V2RoomType.ENTRANCE, V2RoomType.SECRET, V2RoomType.EMPTY) && random.nextFloat() < 0.62f) {
                safe.firstOrNull()?.let { cell ->
                    room.encounters += V2EncounterSpawn("enc_${floorNumber}_${room.x}_${room.y}", cell.first, cell.second, "tier_${tier}_regular")
                }
            }
        }
    }

    fun validate(floor: V2DungeonFloorData): List<String> {
        val errors = mutableListOf<String>()
        val present = floor.presentRooms
        if (present.size !in 15..100) errors += "actual room count outside 15..100"
        if (!floor.roomAt(floor.startRoom.x, floor.startRoom.y).present) errors += "entrance missing"
        if (!floor.roomAt(floor.stairsUpRoom.x, floor.stairsUpRoom.y).present) errors += "exit missing"
        if (present.count { it.stairs?.type == "down" } != 1) errors += "entrance staircase count"
        if (present.count { it.stairs?.type == "up" } != 1) errors += "exit staircase count"

        for (x in 0..9) {
            if (floor.sharedWalls["N:$x"]?.kind != V2WallKind.SOLID) errors += "north exterior boundary open"
            if (floor.sharedWalls["S:$x"]?.kind != V2WallKind.SOLID) errors += "south exterior boundary open"
        }
        for (y in 0..9) {
            if (floor.sharedWalls["W:$y"]?.kind != V2WallKind.SOLID) errors += "west exterior boundary open"
            if (floor.sharedWalls["E:$y"]?.kind != V2WallKind.SOLID) errors += "east exterior boundary open"
        }

        present.forEach { room ->
            for (direction in DungeonDirection.values()) {
                val nx = room.x + direction.dx
                val ny = room.y + direction.dy
                val wall = floor.wallFor(room, direction)
                val neighbor = if (nx in 0..9 && ny in 0..9) floor.roomAt(nx, ny) else null
                if ((neighbor == null || !neighbor.present) && wall.kind != V2WallKind.SOLID) {
                    errors += "passage from ${room.coord.id} into missing/outside room"
                }
                if (wall.kind == V2WallKind.DOORWAY || wall.kind == V2WallKind.SECRET) {
                    if (wall.doorwayOffset !in 1..8) errors += "bad doorway offset ${wall.key}"
                }
            }
        }

        val normallyReachable = graphReachable(floor, includeSecret = false)
        if (floor.stairsUpRoom.id !in normallyReachable) errors += "no normal route from entrance to exit"
        present.filter { it.type != V2RoomType.SECRET }.forEach {
            if (it.coord.id !in normallyReachable) errors += "mandatory room ${it.coord.id} unreachable"
        }
        present.filter { it.type == V2RoomType.SECRET }.forEach {
            if (it.coord == floor.stairsUpRoom || it.coord == floor.startRoom) errors += "secret room used for mandatory progression"
        }

        if (floor.bossRequired) {
            val boss = floor.bossRoom
            if (boss == null) errors += "boss floor missing boss room"
            else {
                val room = floor.roomAt(boss.x, boss.y)
                if (!room.present || room.type != V2RoomType.BOSS) errors += "boss room invalid"
                if (boss.id !in normallyReachable) errors += "boss room unreachable"
                if (floor.stairsUpRoom != boss) errors += "boss floor exit can bypass boss"
                if (room.treasure.none { it.bossChest }) errors += "boss chest missing"
            }
        }

        present.forEach { room ->
            val internalError = validateRoomInterior(floor, room)
            if (internalError != null) errors += internalError
        }
        return errors.distinct()
    }

    private fun validateRoomInterior(floor: V2DungeonFloorData, room: V2DungeonRoomData): String? {
        val required = mutableListOf<Pair<Int, Int>>()
        room.stairs?.let { required += it.x to it.y }
        for (direction in room.connections) {
            val wall = floor.wallFor(room, direction)
            if (wall.kind == V2WallKind.SECRET) continue
            val offset = wall.doorwayOffset ?: 5
            required += when (direction) {
                DungeonDirection.NORTH -> offset to 0
                DungeonDirection.SOUTH -> offset to 9
                DungeonDirection.WEST -> 0 to offset
                DungeonDirection.EAST -> 9 to offset
            }
        }
        if (required.size <= 1) return null
        val start = required.first()
        if (!cellWalkableForValidation(floor, room, start.first, start.second)) return "required cell blocked in ${room.coord.id}"
        val seen = mutableSetOf(start)
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue += start
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val neighbors = listOf(current.first + 1 to current.second, current.first - 1 to current.second, current.first to current.second + 1, current.first to current.second - 1)
            neighbors.forEach { next ->
                if (next.first in 0..9 && next.second in 0..9 && next !in seen && cellWalkableForValidation(floor, room, next.first, next.second)) {
                    seen += next
                    queue += next
                }
            }
        }
        return if (required.any { it !in seen }) "room interior disconnect in ${room.coord.id}" else null
    }

    private fun cellWalkableForValidation(floor: V2DungeonFloorData, room: V2DungeonRoomData, x: Int, y: Int): Boolean {
        if (room.interiorWalls.any { it.x == x && it.y == y }) return false
        if (room.features.any { it.x == x && it.y == y && it.blocking }) return false
        if (x in 1..8 && y in 1..8) return true
        if ((x == 0 || x == 9) && (y == 0 || y == 9)) return false
        val direction = when {
            y == 0 -> DungeonDirection.NORTH
            y == 9 -> DungeonDirection.SOUTH
            x == 0 -> DungeonDirection.WEST
            else -> DungeonDirection.EAST
        }
        val offset = if (direction == DungeonDirection.NORTH || direction == DungeonDirection.SOUTH) x else y
        val wall = floor.wallFor(room, direction)
        return when (wall.kind) {
            V2WallKind.SOLID, V2WallKind.SECRET -> false
            V2WallKind.DOORWAY -> wall.doorwayOffset == offset
            V2WallKind.OPEN -> offset in 1..8
        }
    }

    private fun graphReachable(floor: V2DungeonFloorData, includeSecret: Boolean): Set<String> {
        val seen = mutableSetOf(floor.startRoom.id)
        val queue = ArrayDeque<V2DungeonCoord>()
        queue += floor.startRoom
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val room = floor.roomAt(current.x, current.y)
            for (direction in room.connections) {
                val wall = floor.wallFor(room, direction)
                if (wall.kind == V2WallKind.SECRET && !includeSecret) continue
                if (wall.kind == V2WallKind.SOLID) continue
                val next = V2DungeonCoord(current.x + direction.dx, current.y + direction.dy)
                if (next.x !in 0..9 || next.y !in 0..9) continue
                val nextRoom = floor.roomAt(next.x, next.y)
                if (!nextRoom.present || next.id in seen) continue
                seen += next.id
                queue += next
            }
        }
        return seen
    }

    private fun growFootprint(start: V2DungeonCoord, target: Int, random: Random): Set<V2DungeonCoord> {
        val occupied = mutableSetOf(start)
        while (occupied.size < target) {
            val frontier = occupied.flatMap(::neighbors).filter { it !in occupied }.distinct()
            if (frontier.isEmpty()) break
            val sparse = frontier.filter { candidate -> neighbors(candidate).count { it in occupied } <= 2 }
            val pool = if (sparse.isNotEmpty() && random.nextFloat() < 0.82f) sparse else frontier
            val maxDistance = occupied.maxOf { abs(it.x - start.x) + abs(it.y - start.y) }
            val outward = pool.filter { abs(it.x - start.x) + abs(it.y - start.y) >= maxDistance - 1 }
            val chosen = if (outward.isNotEmpty() && random.nextFloat() < 0.58f) outward.random(random) else pool.random(random)
            occupied += chosen
        }
        return occupied
    }

    private fun makeSpanningTree(
        start: V2DungeonCoord,
        occupied: Set<V2DungeonCoord>,
        links: Array<MutableSet<Int>>,
        random: Random
    ) {
        val seen = mutableSetOf(start)
        val stack = ArrayDeque<V2DungeonCoord>()
        stack += start
        while (stack.isNotEmpty()) {
            val current = stack.last()
            val options = neighbors(current).filter { it in occupied && it !in seen }
            if (options.isEmpty()) {
                stack.removeLast()
            } else {
                val next = options.random(random)
                connect(links, index(current), index(next))
                seen += next
                stack += next
            }
        }
    }

    private fun addLoops(occupied: Set<V2DungeonCoord>, links: Array<MutableSet<Int>>, random: Random, roomCount: Int) {
        val candidates = mutableListOf<Pair<Int, Int>>()
        occupied.forEach { coord ->
            val a = index(coord)
            neighbors(coord).filter { it in occupied }.forEach { neighbor ->
                val b = index(neighbor)
                if (a < b && b !in links[a]) candidates += a to b
            }
        }
        val desired = (roomCount / 7 + random.nextInt(1, (roomCount / 8).coerceAtLeast(2))).coerceAtMost(candidates.size)
        candidates.shuffled(random).take(desired).forEach { connect(links, it.first, it.second) }
    }

    private fun farthestRoom(start: V2DungeonCoord, occupied: Set<V2DungeonCoord>, links: Array<MutableSet<Int>>): V2DungeonCoord {
        val distances = graphDistances(start, links)
        return occupied.maxWithOrNull(compareBy<V2DungeonCoord> { distances[it.id] ?: -1 }.thenBy { abs(it.x - start.x) + abs(it.y - start.y) }) ?: start
    }

    private fun shortestPath(start: V2DungeonCoord, exit: V2DungeonCoord, links: Array<MutableSet<Int>>): List<V2DungeonCoord> {
        val predecessor = mutableMapOf<Int, Int>()
        val startIndex = index(start)
        val exitIndex = index(exit)
        val seen = mutableSetOf(startIndex)
        val queue = ArrayDeque<Int>()
        queue += startIndex
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == exitIndex) break
            links[current].forEach { next ->
                if (next !in seen) {
                    seen += next
                    predecessor[next] = current
                    queue += next
                }
            }
        }
        if (exitIndex !in seen) return listOf(start)
        val reversed = mutableListOf(exitIndex)
        var cursor = exitIndex
        while (cursor != startIndex) {
            cursor = predecessor[cursor] ?: break
            reversed += cursor
        }
        return reversed.asReversed().map(::coord)
    }

    private fun graphDistances(start: V2DungeonCoord, links: Array<MutableSet<Int>>): Map<String, Int> {
        val distances = mutableMapOf(start.id to 0)
        val queue = ArrayDeque<V2DungeonCoord>()
        queue += start
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val distance = distances[current.id] ?: 0
            links[index(current)].forEach { nextIndex ->
                val next = coord(nextIndex)
                if (next.id !in distances) {
                    distances[next.id] = distance + 1
                    queue += next
                }
            }
        }
        return distances
    }

    private fun chooseEdgeStart(random: Random): V2DungeonCoord = when (random.nextInt(4)) {
        0 -> V2DungeonCoord(random.nextInt(1, 9), 0)
        1 -> V2DungeonCoord(9, random.nextInt(1, 9))
        2 -> V2DungeonCoord(random.nextInt(1, 9), 9)
        else -> V2DungeonCoord(0, random.nextInt(1, 9))
    }

    private fun chooseSize(random: Random, boss: Boolean): V2DungeonSize {
        if (boss) return if (random.nextBoolean()) V2DungeonSize.LARGE else V2DungeonSize.HUGE
        return V2DungeonSize.values().random(random)
    }

    private fun roomCountForSize(size: V2DungeonSize, random: Random, boss: Boolean): Int {
        val count = when (size) {
            V2DungeonSize.SMALL -> random.nextInt(15, 26)
            V2DungeonSize.MEDIUM -> random.nextInt(25, 46)
            V2DungeonSize.LARGE -> random.nextInt(45, 71)
            V2DungeonSize.HUGE -> random.nextInt(70, 101)
        }
        return if (boss) count.coerceAtLeast(45) else count
    }

    private fun isStraightConnection(coord: V2DungeonCoord, neighbors: Set<Int>): Boolean {
        if (neighbors.size != 2) return false
        val directions = neighbors.map { directionBetween(coord, coord(it)) }
        return directions[0].opposite() == directions[1]
    }

    private fun directionBetween(a: V2DungeonCoord, b: V2DungeonCoord): DungeonDirection = when {
        b.x > a.x -> DungeonDirection.EAST
        b.x < a.x -> DungeonDirection.WEST
        b.y > a.y -> DungeonDirection.SOUTH
        else -> DungeonDirection.NORTH
    }

    private fun neighbors(coord: V2DungeonCoord): List<V2DungeonCoord> = DungeonDirection.values().mapNotNull { direction ->
        val x = coord.x + direction.dx
        val y = coord.y + direction.dy
        if (x in 0..9 && y in 0..9) V2DungeonCoord(x, y) else null
    }

    private fun canonicalEdge(a: Int, b: Int) = if (a < b) "$a:$b" else "$b:$a"
    private fun connect(links: Array<MutableSet<Int>>, a: Int, b: Int) { links[a] += b; links[b] += a }
    private fun index(coord: V2DungeonCoord) = coord.y * 10 + coord.x
    private fun coord(index: Int) = V2DungeonCoord(index % 10, index / 10)
    private fun randomFor(seed: Long) = Random((seed xor (seed ushr 32)).toInt())
}
