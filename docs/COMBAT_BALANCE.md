# Dungeon Dice Frogs — Combat Balance Baseline

## Turn choices

Each character chooses one action on their turn:

- **Attack**
- **Stand Ground / Defend**
- **Use Item**

## Rolled stats

Player characters and hired helpers currently roll **3d6** for STR, DEX, and CON, producing a 3–18 range.

## Dexterity and extra attacks

Current balance rule:

**Extra-attack chance = DEX × 2%**

This produces 6% at DEX 3 and 36% at DEX 18.

- Straight blades: 1 normal attack; successful DEX check grants a 2nd attack.
- Daggers: always 2 attacks; successful DEX check grants a 3rd attack.
- The extra-attack check is rolled simultaneously with the first attack.

## Strength bonus

Current balance rule:

**Strength bonus chance = STR × 3%**

This produces 9% at STR 3 and 54% at STR 18.

On success, add **+1 physical damage to the first attack of that character's turn**.

Strength also affects carrying capacity. The loot-based bonus-XP mechanic also keys off Strength, but its final exact roll is still open for tuning.

## Weapon families

### Dagger

- Physical die: **d2**
- Always attacks twice per turn when attacking.
- DEX extra-attack success grants a third attack.
- Best suited to unarmored/lightly armored targets.
- Enchantment triggers only on the first attack of the turn.

### Straight Blade

- Example physical die: **d4**
- 1 attack normally.
- DEX extra-attack success grants a second attack.
- Stronger against light armor.
- Less effective against heavier armor.
- Flat **15% bleed chance per successful attack**.
- Every attack gets its own bleed check, including an extra attack.

### Axe

- Example physical die: **d6**
- 1 attack per turn.
- More physical damage than straight blades.
- Slightly more effective against heavier armor than straight blades.
- Flat **30% bleed chance per successful attack**.

### Blunt Weapon

- Example physical die: **d8**
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

AC starts at 0 and rises with armor. Heavier armor should increasingly reduce the effectiveness of small/fast weapons while preserving the role of axes and especially blunt weapons.

Current armor baseline:

- None: 0
- Leather: 1
- Heavy/studded leather: 2
- Chain: 3
- Scale: 4
- Plate: 5

Shields are tracked separately so blunt weapons can ignore them.

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

To balance strength of effects, current generation weights are:

- Fire: 40%
- Poison: 30%
- Ice: 20%
- Lightning: 10%

These are balance values, not lore restrictions, and should be playtested.

## Simultaneous first-attack roll

A first attack may visually roll together:

- Gray physical weapon die/dice
- DEX percentage check when the weapon can gain an extra attack
- STR percentage check for +1 physical damage
- Bleed percentage check when applicable
- One colored elemental die when the weapon is enchanted

Subsequent attacks during that same turn do not repeat the elemental enchantment or STR bonus check, but they do make their own physical damage and bleed checks where applicable.
