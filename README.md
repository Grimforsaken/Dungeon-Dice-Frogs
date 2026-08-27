# Dungeon Dice Frogs

A dungeon-crawler project built around physical weapons, dice-driven combat, rare loot, enchantments, and treasure-based progression.

## Core identity

- No spellcasting in the initial development phase.
- Character advancement comes from recovered loot and treasure, not enemy kills.
- Regular enemies have a 20% chance to carry loot from enemy-specific thematic tables.
- Bosses always drop their unique boss loot and a key required to proceed.
- Each boss guards a guaranteed non-mimic chest using the floor's loot tier.
- Loot tiers increase every 10 dungeon floors.
- Higher-tier loot grants more XP.
- Ordinary dungeon chests have a small configurable chance to be Mimics.
- Town sells basic equipment and non-enchanted starter weapons.
- Scrolls of Return create a temporary two-way portal between town and the exact dungeon location where used.
- Up to two helpers become hireable at dungeon floor 30.

## Combat

Each character chooses one action on a turn:

1. Attack
2. Stand Ground / Defend
3. Use an Item

Turn order and extra-attack chances are driven by Dexterity. Strength affects physical bonuses and carrying capacity. Constitution determines HP and resistances.

Physical damage dice are gray. Elemental enchantment dice are color-coded and rolled at the same time as the first physical attack of the turn.

See `docs/GAME_DESIGN.md` and `docs/COMBAT_BALANCE.md` for the current rules.

## Development status

Pre-production / rules baseline. Engine and platform architecture have not yet been locked in.
