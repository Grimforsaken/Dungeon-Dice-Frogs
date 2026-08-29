# Dungeon Dice Frogs — Development Roadmap

## Next APK update queue

Do not apply these changes to the current published APK yet. Apply them together with the next planned APK update.

- **APK / launcher icon:** use the first newly supplied frog-with-sword-and-red-d20 image from the August 29 update. Source attachment in the design thread: `1000040542.png`.
- **In-app icon / branding image:** use the second newly supplied frog-with-sword-and-red-d20 image from the August 29 update. Source attachment in the design thread: `1000040541.png`.
- Preserve the artwork composition when preparing Android icon resources; create the required Android launcher/adaptive icon sizes without replacing the source artwork with a generic system icon.
- Keep these queued until the next APK build so they can be applied alongside the next group of gameplay/UI changes rather than triggering a standalone release.

## Phase 1: Rules prototype

- Implement deterministic dice utilities for d2, d3, d4, d6, d8, percentile rolls, and 3d6 stat generation.
- Implement turn order and the three player actions: Attack, Stand Ground, Use Item.
- Implement weapon families and their different attack counts/armor interactions.
- Implement bleed stacking and elemental enchantment effects.
- Build automated combat simulations to tune weapon balance before art-heavy development.

## Phase 2: Character and inventory

- Character creation with rolled STR/DEX/CON.
- HP and resistance formulas from CON.
- Carrying capacity from STR.
- Equipment slots, AC, shields, weapons, and consumables.
- Whole-number XP progression and party XP split.

## Phase 3: Loot and dungeon progression

- Enemy-specific thematic loot tables.
- 20% regular-enemy loot check.
- Loot tiers in 10-floor bands.
- Next-tier enemies appearing increasingly after each band midpoint.
- Chests and configurable Mimic chance.
- Boss-specific guaranteed loot, progression key, and guaranteed non-Mimic boss chest.

## Phase 4: Town and expedition loop

- Town shops for basic non-enchanted equipment and starter weapons.
- Return travel through cleared dungeon floors.
- Scroll of Return two-way portal behavior.
- Helper hiring unlock at floor 30; maximum two helpers.

## Phase 5: Presentation

- Simultaneous physical and elemental dice presentation.
- Gray physical dice.
- Red Fire, Blue Ice, Yellow Lightning, Green Poison dice.
- Combat feedback for bleed, armor interaction, skipped turns, and status durations.

## Decisions still needed before full implementation

- Target platform(s).
- Game engine/framework.
- Exact dungeon navigation format: grid, rooms/nodes, free movement, or hybrid.
- CON → HP/resistance formulas.
- STR → carrying-capacity formula.
- Strength-based bonus-XP roll.
- Exact loot-tier XP values.
- Mimic percentage.
- Initiative/additional-turn formula beyond weapon extra attacks.
- Stand Ground defense value.
- Armor penetration/penalty values for axes and straight blades.
