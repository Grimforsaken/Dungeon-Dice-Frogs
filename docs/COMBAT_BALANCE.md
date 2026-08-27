# Dungeon Dice Frogs — Combat Balance Baseline

## Turn choices

Each character chooses one action on their turn:

- **Attack**
- **Stand Ground / Defend**
- **Use Item**

## Rolled stats

Every current character stat is rolled on **3d6**, producing a 3–18 range. This applies to the player character and hired helpers.

Current physical stats:

- Strength (STR)
- Dexterity (DEX)
- Constitution (CON)

## Shared stat thresholds

CON, STR carrying capacity, and DEX movement use the same threshold ladder:

- 12 = +1
- 14 = +2
- 16 = +3
- 18 = +4

The previously supplied CON value of 28 for +4 HP is recorded as **18**, because 3d6 cannot exceed 18 and this preserves the 12/14/16 progression.

## Constitution and HP

CON grants bonus HP:

- CON 12: +1 HP
- CON 14: +2 HP
- CON 16: +3 HP
- CON 18: +4 HP

The base HP formula remains open for balancing.

## Dexterity, movement, and extra attacks

### Party movement

Party movement speed is determined by the **lowest DEX in the active party**.

Base movement in squares per turn remains configurable. DEX then adds:

- DEX 12: +1 square
- DEX 14: +2 squares
- DEX 16: +3 squares
- DEX 18: +4 squares

A shield reduces movement by **1 square**.

Heavy armor reduces movement by **1 square**, unless the wearer has **STR 12 or higher**.

### Extra attacks

Current balance rule:

**Extra-attack chance = DEX × 2%**

This produces 6% at DEX 3 and 36% at DEX 18.

- Straight blades: 1 normal attack; successful DEX check grants a 2nd attack.
- Daggers: 2 attacks; successful DEX check grants a 3rd attack.
- The extra-attack check is rolled simultaneously with the first attack.

### Dual daggers

At **DEX 12 or higher**, a character may equip two daggers.

- 2 attacks are made when attacking.
- The DEX extra-attack check can raise this to a maximum of 3 strikes.
- Dual-wielding daggers reduces the character's AC by **2**.
- An enchantment still applies only to the first attack of the turn.

## Strength

Current combat balance rule:

**Strength bonus chance = STR × 3%**

This produces 9% at STR 3 and 54% at STR 18.

On success, add **+1 physical damage to the first attack of that character's turn**.

### Carrying capacity

Carrying capacity is measured in **inventory slots**.

STR increases carrying capacity using the shared thresholds:

- STR 12: +1 slot
- STR 14: +2 slots
- STR 16: +3 slots
- STR 18: +4 slots

A purchased **backpack adds 4 slots**.

The base number of carry slots remains configurable.

## Weapon families

### Dagger

- Physical die: **d2**
- 2 attacks when using the current dagger attack pattern.
- DEX extra-attack success grants a third attack.
- Best suited to unarmored/lightly armored targets.
- Enchantment triggers only on the first attack of the turn.

### Straight Blade

- One-handed physical die: **d4**
- Two-handed straight blade physical die: **d6**
- 1 attack normally.
- DEX extra-attack success grants a second attack.
- Stronger against light armor.
- Less effective against heavier armor.
- Flat **15% bleed chance per successful attack**.
- Every attack gets its own bleed check, including an extra attack.

### Axe

- One-handed physical die: **d6**
- Two-handed axe physical die: **d8**
- 1 attack per turn.
- More physical damage than straight blades.
- Slightly more effective against heavier armor than straight blades.
- Flat **30% bleed chance per successful attack**.

### Blunt Weapon

- One-handed physical die: **d8**
- Two-handed blunt physical die: **d10**
- 1 attack on its normal attack turns.
- Slow: every third attack turn is skipped.
- Most effective against heavy armor.
- Ignores shields.
- Ignores **2 points of AC**.

## Bleeding

Bleed can stack.

Each bleed stack:

- Lasts 4 turns.
- Deals 1 damage every other turn.
- Deals 2 total damage if the full duration completes.

Each qualifying physical attack rolls its own bleed chance simultaneously with damage.

## Armor Class

AC starts at 0 and rises with armor.

Current armor baseline:

- None: 0
- Leather: 1
- Heavy/studded leather: 2
- Chain: 3
- Scale: 4
- Plate: 5

Armor categories for Stand Ground:

- Light: Leather / Heavy Leather
- Medium: Chain / Scale
- Heavy: Plate

### Shields

A shield:

- Adds **+1 AC**.
- Reduces movement by **1 square**.

Blunt weapons ignore shield AC.

### Heavy armor

Heavy armor reduces movement by **1 square** unless the wearer has **STR 12+**.

## Stand Ground / Defend

Stand Ground gives a temporary defensive bonus based on worn armor:

- No armor: +0
- Light armor: +1
- Medium armor: +2
- Heavy armor: +3
- Shield: an additional +1

The bonus lasts for the defensive period defined by the turn system.

## Elemental enchantments

Enchantments apply **only to the first attack made by that weapon each turn**. Extra attacks remain physical and may still make their normal bleed checks where applicable.

Physical damage dice are **gray**. Elemental dice are color-coded and rolled simultaneously with the first physical attack.

### Fire — Red

- Roll **d6** for duration.
- Deals **1 fire damage per turn** until the rolled duration expires.

### Ice — Blue

- Roll **d6** for duration.
- Target cannot attack every other turn while the effect remains active.

### Lightning — Yellow

- Roll **d3** elemental damage.
- Target skips its next turn.
- Lightning damage is doubled against metallic armor.
- Only the lightning component doubles; physical damage does not.

### Poison — Green

- Roll **d2** once for poison damage per tick.
- Deals that result each turn for **3 turns**.

## Initial enchantment rarity weights

Current generation weights:

- Fire: 40%
- Poison: 30%
- Ice: 20%
- Lightning: 10%

These remain balance values subject to playtesting.

## Simultaneous first-attack roll

A first attack may visually roll together:

- Gray physical weapon die/dice
- DEX percentage check when the weapon can gain an extra attack
- STR percentage check for +1 physical damage
- Bleed percentage check when applicable
- One colored elemental die when the weapon is enchanted

Subsequent attacks during that same turn do not repeat the elemental enchantment or STR bonus check, but they do make their own physical damage and bleed checks where applicable.
