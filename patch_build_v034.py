from pathlib import Path

source = Path('patch_build.py').read_text()

# Preserve any higher-resolution 0.3.4 drawables committed directly in the repo.
source = source.replace(
    "        (DRAWABLE / name).write_bytes(data)\n",
    "        target = DRAWABLE / name\n        if not target.exists():\n            target.write_bytes(data)\n"
)

# The legacy preflight used to inject frogColor/onEnterDungeon into GameActivity.
# 0.3.4 already uses TownHubScreenV034 and PersistentDungeonScreenV034, so the
# old call pattern is intentionally absent and must no longer be treated as an error.
old_guard = """    if old not in g:\n        raise RuntimeError('Could not patch TownHubScreen call in GameActivity.kt')\n    game.write_text(g.replace(old, new, 1))\n"""
new_guard = """    if old in g:\n        game.write_text(g.replace(old, new, 1))\n"""
if old_guard not in source:
    raise RuntimeError('Could not locate legacy GameActivity compatibility guard')
source = source.replace(old_guard, new_guard, 1)

# Execute the established preflight with only the 0.3.4 compatibility changes above.
exec(compile(source, 'patch_build.py', 'exec'), {'__name__': '__main__'})

required_v034 = [
    'boss_stormsting_sovereign.webp',
    'enemy_tier1_atlas.webp',
    'dungeon_doors.webp',
    'dungeon_details.webp',
]
missing = [name for name in required_v034 if not (Path('app/src/main/res/drawable-nodpi') / name).exists()]
if missing:
    raise RuntimeError('Missing 0.3.4 renderer assets: ' + ', '.join(missing))

print('0.3.4 compatibility preflight OK.')
