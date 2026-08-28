from pathlib import Path

p = Path('app/src/main/java/com/grimforsaken/dungeondicefrogs/MainActivity.kt')
s = p.read_text()

if 'import androidx.compose.foundation.layout.BoxScope' not in s:
    s = s.replace('import androidx.compose.foundation.layout.Box\n', 'import androidx.compose.foundation.layout.Box\nimport androidx.compose.foundation.layout.BoxScope\n')

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

p.write_text(s)
print('Dungeon Dice Frogs source preflight applied')
