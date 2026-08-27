# Dungeon Dice Frogs — Inventory & Equipment UI

## Inventory screen

The inventory screen must show the character's carry capacity as **visible slots**, not as a hidden number or simple list.

- Base capacity: **6 visible inventory slots**.
- STR threshold bonuses add visible slots.
- A backpack adds **4 additional visible slots**.
- Occupied slots display the item icon/art and item name on inspection.
- Empty slots remain visibly open so the player can immediately see remaining capacity.
- Equipment that is currently worn/equipped does not occupy a carry slot unless later balance rules explicitly change this.

## Equipment screen layout

The equipment screen shows the character prominently, with equipment boxes positioned around the character.

Required equipment slots:

- **Helmet** — reserved for a future update.
- **Chest** — the single armor slot; all wearable body armor is a chest piece.
- **Hand 1**
- **Hand 2**
- **Ring 1** — reserved for a future update.
- **Ring 2** — reserved for a future update.
- **Necklace** — reserved for a future update.

Future-update slots should be visible in the layout from the beginning so the interface does not need to be redesigned later. Until their item systems are enabled, Helmet, Ring 1, Ring 2, and Necklace remain unavailable/locked and cannot accept items.

## Inventory tray on equipment screen

The bottom of the equipment screen displays the character's currently carried **equipable items** in a visible inventory tray.

- Items can be dragged from the bottom inventory tray directly onto a compatible equipment box.
- Equipped items can be dragged back into an available inventory slot to unequip them.
- An item cannot be dropped into an incompatible equipment slot.
- Invalid drop targets should visibly reject the item and leave the current equipment unchanged.

## Hand-slot rules

There are always two visible hand boxes.

Valid one-handed combinations include:

- One one-handed weapon in either hand.
- One one-handed weapon + one shield.
- Two daggers when the character has **DEX 12+**.

Dual daggers keep the established combat rule:

- Require DEX 12+.
- Two normal dagger attacks with a maximum potential of three strikes.
- While two daggers are equipped, the character suffers **-2 AC**.

## Two-handed weapons

When a two-handed weapon is equipped:

- The weapon occupies the primary hand slot.
- The second hand slot becomes unavailable.
- A large **red X** is drawn over the second hand box to clearly show that the hand is occupied by the two-handed weapon.
- The player cannot place another weapon, shield, or item in the crossed-out hand slot.
- Removing the two-handed weapon immediately removes the red X and restores the second hand slot.

Two-handed weapon examples already defined by the combat rules:

- Two-handed straight blade: **d6**
- Two-handed axe: **d8**
- Two-handed blunt weapon: **d10**

## Chest armor

Armor is represented by a single **Chest** equipment slot.

The equipped chest piece determines the character's armor category and base armor AC contribution:

- Light armor
- Medium armor
- Heavy armor

Stand Ground, heavy-armor movement penalties, and other armor rules read from the equipped chest piece.

## Reserved future slots

The following slots are designed into the equipment screen now but their item systems are deferred:

- Helmet
- Ring 1
- Ring 2
- Necklace

Keeping these slots visible from the initial UI build reserves their screen positions and avoids a disruptive equipment-screen redesign when those item types are added later.
