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
# The older ddf_art_pack_v033.b64.part* test chunks are deliberately ignored.
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

# Keep generator/data persistence, but retire its old room-to-room UI.
dungeon = JAVA / 'ProceduralDungeon.kt'
if dungeon.exists():
    d = dungeon.read_text().replace('import androidx.compose.foundation.layout.weight\n', '')
    d = d.replace('@Composable fun PersistentDungeonScreen(', '@Composable fun LegacyPersistentDungeonScreen(', 1)
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
