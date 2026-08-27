# Dungeon Dice Frogs — Game Design Baseline

## Core loop

Enter dungeon → explore → fight when necessary → recover loot/treasure → decide whether to push deeper or return to town → upgrade equipment → defeat floor boss → use boss key to proceed.

Enemy kills themselves award **0 XP**. Progression is tied to valuables recovered from the dungeon.

## XP and leveling

Character levels use small whole-number XP totals.

- Level 1: 0 XP
- Level 2: 10 total XP
- Level 3: 20 total XP
- Level 4: 30 total XP
- Continue at +10 total XP per level.

Higher-tier loot awards more XP than lower-tier loot. Exact per-tier XP values remain configurable for balancing.

### Party XP split

When helpers are present, each XP award is divided into equal whole-number shares among the player and active helpers. Any remainder goes to the player.

Examples:

- Player + 1 helper, 2 XP → 1 / 1
- Player + 1 helper, 3 XP → player 2 / helper 1
- Player + 2 helpers, 4 XP → player 2 / helpers 1, 1
- Player + 2 helpers, 8 XP → player 4 / helpers 2, 2

No fractional XP is used.

## Loot

### Regular enemies

- 20% chance to carry loot.
- On success, roll on that enemy type's thematic loot table.
- Lower-level enemy types are restricted to appropriate lower-tier loot.
- Loot should make sense for the enemy carrying or guarding it.

### Bosses

Every floor boss guarantees:

1. Boss-specific unique loot.
2. A progression key required to access the next floor.
3. Access to a guarded chest that is never a Mimic.

The guarded boss chest uses loot appropriate to that floor's tier.

### Chests and Mimics

- Chests appear throughout dungeon floors.
- A small configurable percentage of ordinary chests are Mimics.
- Boss-guarded chests are never Mimics.
- Mimics are enemies and therefore award no XP for the kill itself; their recovered loot can still award XP.

## Loot tiers and dungeon floors

Loot tiers advance every 10 dungeon floors.

- Tier 1: floors 1–10
- Tier 2: floors 11–20
- Tier 3: floors 21–30
- Tier 4: floors 31–40
- Continue in 10-floor bands.

Starting around the midpoint of each 10-floor band, encounters increasingly include enemies associated with the next tier. This previews upcoming threats and gives dangerous early access to higher-tier enemy loot.

Helpers unlock at **dungeon floor 30**.

## Town

The player may leave the dungeon and return to town.

Without special travel, the party must physically traverse all intervening dungeon floors to reach the entrance.

Town can sell:

- Basic equipment
- Simple non-enchanted starter weapons
- Basic utility/consumable items
- Scrolls of Return, subject to economy balancing

Enchanted weapons are intended primarily as dungeon rewards rather than routine town purchases.

## Scroll of Return

A Scroll of Return is consumable.

When used:

1. The scroll is consumed.
2. A portal opens at the exact dungeon location where it was used.
3. The party returns to town.
4. A matching portal in town can be used to return to that exact dungeon position.
5. After the return trip, the portal closes.

## Helpers

- Unavailable until dungeon floor 30.
- The player may hire up to two helpers.
- Helpers have their own rolled stats, HP, equipment, level, and XP.
- XP is split using the whole-number equal-share rule above.

## Character creation

Current baseline uses **3d6** for core physical stats:

- Strength (STR)
- Dexterity (DEX)
- Constitution (CON)

The same rule applies when a hireable helper is generated.

## Core stats

### Strength

- Physical damage bonus chance
- Carrying capacity
- Used by the bonus-XP system when qualifying loot/treasure is recovered; exact bonus-XP resolution remains subject to final balancing

### Dexterity

- Turn order / initiative
- Extra-attack chance for eligible weapons
- Potential future additional-turn thresholds

### Constitution

- Maximum HP
- Resistance to physical and elemental conditions

## Armor Class

Armor Class begins at 0 and increases with armor quality/type.

Current baseline:

- No armor: AC 0
- Leather: AC 1
- Heavy/studded leather: AC 2
- Chain: AC 3
- Scale: AC 4
- Plate: AC 5

Shields can add AC separately.

Weapon families interact with armor differently; see `COMBAT_BALANCE.md`.
