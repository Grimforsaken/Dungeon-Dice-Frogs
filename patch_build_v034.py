from pathlib import Path

source = Path('patch_build.py').read_text()

# Preserve higher-resolution drawables committed directly in the repo.
source = source.replace(
    "        (DRAWABLE / name).write_bytes(data)\n",
    "        target = DRAWABLE / name\n        if not target.exists():\n            target.write_bytes(data)\n"
)

# The modern app already has its Town arguments wired in. The old patch is allowed
# to skip that migration instead of failing the whole build.
old_guard = """    if old not in g:\n        raise RuntimeError('Could not patch TownHubScreen call in GameActivity.kt')\n    game.write_text(g.replace(old, new, 1))\n"""
new_guard = """    if old in g:\n        game.write_text(g.replace(old, new, 1))\n"""
if old_guard not in source:
    raise RuntimeError('Could not locate legacy GameActivity compatibility guard')
source = source.replace(old_guard, new_guard, 1)

exec(compile(source, 'patch_build.py', 'exec'), {'__name__': '__main__'})

# 0.3.5 clean build: keep the existing persistent floor data but route the live game
# through the simplified Greystone renderer. This intentionally removes the visually
# noisy master-asset mix from normal Tier-1 room rendering.
game_activity = Path('app/src/main/java/com/grimforsaken/dungeondicefrogs/GameActivity.kt')
if game_activity.exists():
    game_source = game_activity.read_text()
    game_source = game_source.replace('PersistentDungeonScreenV034(', 'PersistentDungeonScreenV035(')
    game_activity.write_text(game_source)

# MainActivity.kt contains shared live UI functions even though MainActivity itself
# is now only a fallback entry point. Keep it compatible with the current enemy model
# and render real equipment artwork from the 4x4 equipment atlas.
legacy_main = Path('app/src/main/java/com/grimforsaken/dungeondicefrogs/MainActivity.kt')
if legacy_main.exists():
    main_source = legacy_main.read_text()

    stale_icon = '                        Text(enemy.icon, fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))\n'
    main_source = main_source.replace(stale_icon, '', 1)

    import_pairs = [
        ('import androidx.compose.foundation.background\n', 'import androidx.compose.foundation.background\nimport androidx.compose.foundation.Canvas\n'),
        ('import androidx.compose.ui.graphics.Color\n', 'import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.ImageBitmap\n'),
        ('import androidx.compose.ui.platform.LocalDensity\n', 'import androidx.compose.ui.platform.LocalDensity\nimport androidx.compose.ui.res.imageResource\n'),
        ('import androidx.compose.ui.unit.IntOffset\n', 'import androidx.compose.ui.unit.IntOffset\nimport androidx.compose.ui.unit.IntSize\n'),
    ]
    for old, new in import_pairs:
        if new.splitlines()[-1] not in main_source:
            main_source = main_source.replace(old, new, 1)

    main_source = main_source.replace(
        '                Text(gearItem.icon, fontSize = 32.sp)\n',
        '                GearArtIcon(gearItem.id, Modifier.size(58.dp))\n',
        1
    )
    main_source = main_source.replace(
        '        if (gear != null) Text("${gear.icon}\\n${gear.name}", color = Cream, textAlign = TextAlign.Center, fontSize = 10.sp, fontWeight = FontWeight.Bold)\n',
        '''        if (gear != null) {\n            Column(horizontalAlignment = Alignment.CenterHorizontally) {\n                GearArtIcon(gear.id, Modifier.size(48.dp))\n                Text(gear.name, color = Cream, textAlign = TextAlign.Center, fontSize = 8.sp, fontWeight = FontWeight.Bold)\n            }\n        }\n''',
        1
    )
    main_source = main_source.replace(
        '        Text("${gear.icon}\\n${gear.name}", color = Cream, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)\n',
        '''        Column(horizontalAlignment = Alignment.CenterHorizontally) {\n            GearArtIcon(gear.id, Modifier.size(54.dp))\n            Text(gear.name, color = Cream, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)\n        }\n''',
        1
    )

    marker = '\nprivate fun equip(\n'
    if 'private fun GearArtIcon(' not in main_source:
        gear_icon = r'''
@Composable
private fun GearArtIcon(id: String, modifier: Modifier = Modifier) {
    val atlas = ImageBitmap.imageResource(R.drawable.equipment_atlas)
    val cell = 160
    val index = when (id) {
        "dagger1", "dagger2" -> 0
        "greatsword" -> 1
        "sword" -> 2
        "axe" -> 3
        "great_axe" -> 4
        "mace" -> 5
        "great_mace" -> 6
        "shield" -> 7
        "backpack" -> 8
        "leather" -> 9
        "chain" -> 10
        "plate" -> 11
        else -> 0
    }
    val sourceOffset = IntOffset((index % 4) * cell, (index / 4) * cell)
    Canvas(modifier) {
        drawImage(
            image = atlas,
            srcOffset = sourceOffset,
            srcSize = IntSize(cell, cell),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))
        )
    }
}
'''
        if marker not in main_source:
            raise RuntimeError('Could not locate equip() insertion point for equipment atlas UI')
        main_source = main_source.replace(marker, gear_icon + marker, 1)

    legacy_main.write_text(main_source)

required_assets = [
    'boss_stormsting_sovereign.webp',
    'enemy_tier1_atlas.webp',
    'dungeon_doors.webp',
    'dungeon_details.webp',
    'equipment_atlas.webp',
    'greystone_floors.webp',
    'greystone_walls.webp',
    'greystone_stairs_down.webp',
    'greystone_stairs_up.webp',
    'greystone_pillars.webp',
]
missing = [name for name in required_assets if not (Path('app/src/main/res/drawable-nodpi') / name).exists()]
if missing:
    raise RuntimeError('Missing clean renderer assets: ' + ', '.join(missing))

print('0.3.5 clean-build compatibility preflight OK.')
