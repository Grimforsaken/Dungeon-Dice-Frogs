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
    dungeon.write_text(d)

print('Dungeon Dice Frogs source preflight applied')
