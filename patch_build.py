from pathlib import Path

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
    dungeon.write_text(d)

print('Dungeon Dice Frogs source preflight applied')
