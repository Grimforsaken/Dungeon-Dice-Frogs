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

Loot awards XP **one-for-one with its loot tier**:

- Tier 1 loot = 1 XP
- Tier 2 loot = 2 XP
- Tier 3 loot = 3 XP
- Tier 4 loot = 4 XP
- Continue the same pattern for higher tiers.

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
- **5% of ordinary chests are Mimics.**
- Boss-guarded chests are never Mimics.
- Mimics are enemies and therefore award no XP for the kill itself; their recovered loot can still award XP.

## Loot tiers and dungeon floors

Loot tiers advance every 10 dungeon floors.

- Tier 1: floors 1–10
- Tier 2: floors 11–20
- Tier 3: floors 21–30
- Tier 4: floors 31–40
- Continue in 10-floor bands.

Starting around the midpoint of each 10-floor band, encounters increasingly include enemies associated with the next tier.

Enemy difficulty scales by a cumulative **×1.5 per tier** relative to the previous tier:

- Tier 1: ×1.00 baseline
- Tier 2: ×1.50
- Tier 3: ×2.25
- Tier 4: ×3.375

Helpers unlock at **dungeon floor 30**.

## Character creation

Every current character stat is rolled on **3d6**, producing a 3–18 range. The same rule applies to the player and hired helpers.

Current stats:

- Strength (STR)
- Dexterity (DEX)
- Constitution (CON)

## Shared stat thresholds

Several systems use the same threshold ladder:

- 12 = +1
- 14 = +2
- 16 = +3
- 18 = +4

The earlier supplied CON value of 28 for +4 HP is treated as **18**, because 3d6 cannot produce 28.

### Constitution

CON bonus HP:

- 12: +1 HP
- 14: +2 HP
- 16: +3 HP
- 18: +4 HP

CON also governs resistances; the exact resistance formula remains open for balancing.

### Strength

STR affects:

- Physical bonus-damage chance
- Carrying capacity
- Heavy-armor movement penalty removal at STR 12+
- The bonus-XP system tied to recovered loot/treasure

Carrying capacity is measured in **slots**. Base carrying capacity is **6 slots**. STR adds slots using the shared thresholds above.

A purchased **backpack adds 4 carry slots**.

### Dexterity

DEX affects:

- Turn order / initiative
- Extra-attack chance for eligible weapons
- Movement
- Dual-dagger eligibility

Base movement is **5 squares per turn**. DEX adds movement using the shared thresholds:

- 12: +1 square
- 14: +2 squares
- 16: +3 squares
- 18: +4 squares

## Party movement

The party's movement speed is determined by the **lowest DEX among the current party members**.

Movement modifiers include:

- Shield: -1 square
- Heavy armor: -1 square
- Heavy-armor penalty is ignored when the wearer has STR 12+

## Town

The player may leave the dungeon and return to town.

Without special travel, the party must physically traverse all intervening dungeon floors to reach the entrance.

Town can sell:

- Basic equipment
- Simple non-enchanted starter weapons
- Basic utility/consumable items
- Backpacks (+4 carry slots)
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
- Party movement is limited by the lowest DEX in the party.

## Armor Class

Armor Class begins at 0 and increases with armor quality/type.

Current baseline:

- No armor: AC 0
- Leather: AC 1
- Heavy/studded leather: AC 2
- Chain: AC 3
- Scale: AC 4
- Plate: AC 5

A shield adds **+1 AC** but reduces movement by **1 square**.

Heavy armor reduces movement by **1 square** unless the wearer has STR 12+.

## Stand Ground

Stand Ground defensive bonus depends on armor category:

- No armor: +0
- Light armor: +1
- Medium armor: +2
- Heavy armor: +3
- Shield: +1 additional defense

## Weapon additions

Two-handed weapons increase the physical damage die:

- Two-handed straight blade: **d6**
- Two-handed axe: **d8**
- Two-handed blunt weapon: **d10**

At DEX 12+, a character may use two daggers. Dual daggers use the two-attack pattern with a maximum potential of three strikes and impose **-2 AC**.

See `COMBAT_BALANCE.md` for the full weapon and enchantment rules.

## Inventory and equipment screens

The inventory screen shows carrying capacity as visible slots. The base six slots are always visible, with additional visible slots added for STR bonuses and backpacks.

The equipment screen shows the character with dedicated equipment boxes around them and a draggable equipable-item tray along the bottom of the screen.

Current equipment slots:

- Chest armor
- Hand 1
- Hand 2

Reserved visible slots for future updates:

- Helmet
- Ring 1
- Ring 2
- Necklace

Armor is a single chest piece. One-handed weapons and shields use the two hand boxes. A character with DEX 12+ may place a dagger in each hand.

When a two-handed weapon is equipped, it occupies the primary hand and blocks the other hand. The second hand box displays a large **red X** and rejects additional equipment until the two-handed weapon is removed.

Equipable items carried by the character appear at the bottom of the equipment screen and can be dragged onto compatible equipment boxes. Equipped items can be dragged back into open inventory capacity.

See `UI_EQUIPMENT.md` for the detailed interaction rules and future-slot behavior.
