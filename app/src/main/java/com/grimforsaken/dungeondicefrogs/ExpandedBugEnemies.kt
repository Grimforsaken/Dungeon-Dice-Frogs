package com.grimforsaken.dungeondicefrogs

import androidx.annotation.DrawableRes

data class RegularBugEnemy(
    val id: String,
    val name: String,
    val role: String,
    val armorType: String,
    val baseStats: HeroStats,
    val hp: Int,
    val armorClass: Int,
    val move: Int,
    val standGroundBonus: Int,
    val weaponName: String,
    val weaponSummary: String,
    val elementalAttack: ElementType? = null,
    @DrawableRes val artRes: Int
) {
    fun statsForTier(tier: Int): HeroStats {
        val bonus = tier.coerceAtLeast(1) - 1
        return HeroStats(
            strength = baseStats.strength + bonus,
            dexterity = baseStats.dexterity + bonus,
            constitution = baseStats.constitution + bonus
        )
    }
}

/**
 * Regular / elite enemies supplied in dungeon_dice_frogs_bug_stats_and_images.zip.
 * These are explicitly NOT bosses. The butterfly remains non-electric.
 */
val expandedRegularBugEnemies: List<RegularBugEnemy> = listOf(
    RegularBugEnemy(
        id = "lightning_bug_dual_dagger_scout",
        name = "Lightning Bug Dual Dagger Scout",
        role = "Fast striker",
        armorType = "Light Armor",
        baseStats = HeroStats(9, 15, 10),
        hp = 10,
        armorClass = 1,
        move = 6,
        standGroundBonus = 1,
        weaponName = "Twin Lightning Daggers",
        weaponSummary = "1d2 physical per hit; always 2 attacks; 30% chance for a 3rd; 1d3 lightning on the first hit; STR 27% chance for +1 physical on first hit.",
        elementalAttack = ElementType.LIGHTNING,
        artRes = R.drawable.enemy_01_lightning_bug_dual_dagger_scout
    ),
    RegularBugEnemy(
        id = "ladybug_sword_shield_guard",
        name = "Ladybug Sword and Shield Guard",
        role = "Defensive frontliner",
        armorType = "Medium Armor + Shield",
        baseStats = HeroStats(13, 13, 14),
        hp = 14,
        armorClass = 3,
        move = 5,
        standGroundBonus = 3,
        weaponName = "Clover Sword + Shield",
        weaponSummary = "1d4 physical; 26% chance for a 2nd attack; 15% bleed per hit; STR 39% chance for +1 physical on first hit. Shield AC is included.",
        artRes = R.drawable.enemy_02_ladybug_sword_shield_guard
    ),
    RegularBugEnemy(
        id = "ladybug_two_handed_blunt_mystic",
        name = "Ladybug Two-Handed Blunt Mystic",
        role = "Slow bruiser / support bruiser",
        armorType = "Medium Armor",
        baseStats = HeroStats(15, 10, 15),
        hp = 15,
        armorClass = 2,
        move = 5,
        standGroundBonus = 2,
        weaponName = "Clover Maul / Heavy Clover Staff",
        weaponSummary = "1d8 physical; ignores shields and 2 AC; after attacking for 2 consecutive turns, skips the 3rd attack turn; STR 45% chance for +1 physical on first hit.",
        artRes = R.drawable.enemy_03_ladybug_two_handed_blunt_mystic
    ),
    RegularBugEnemy(
        id = "lightning_bug_thunder_axe_raider",
        name = "Lightning Bug Thunder Axe Raider",
        role = "Midline damage dealer",
        armorType = "Medium Armor",
        baseStats = HeroStats(14, 12, 13),
        hp = 13,
        armorClass = 2,
        move = 6,
        standGroundBonus = 2,
        weaponName = "Thunder Axe",
        weaponSummary = "1d6 physical; 1 attack; 30% bleed; 1d3 lightning on the first hit; STR 42% chance for +1 physical on first hit.",
        elementalAttack = ElementType.LIGHTNING,
        artRes = R.drawable.enemy_04_lightning_bug_thunder_axe_raider
    ),
    RegularBugEnemy(
        id = "june_bug_heavy_shield_guard",
        name = "June Bug Heavy Shield Guard",
        role = "Tank / blocker",
        armorType = "Heavy Armor + Shield",
        baseStats = HeroStats(16, 8, 16),
        hp = 18,
        armorClass = 4,
        move = 4,
        standGroundBonus = 4,
        weaponName = "One-Handed Cleaver / Claw Shield Build",
        weaponSummary = "1d6 physical; 1 attack; 30% bleed; STR 48% chance for +1 physical on first hit. STR 12+ removes heavy-armor speed penalty; shield still reduces movement by 1.",
        artRes = R.drawable.enemy_05_june_bug_heavy_shield_guard
    ),
    RegularBugEnemy(
        id = "june_bug_heavy_dual_blade_raider",
        name = "June Bug Heavy Dual Blade Raider",
        role = "Heavy skirmisher",
        armorType = "Heavy Armor",
        baseStats = HeroStats(14, 13, 15),
        hp = 16,
        armorClass = 3,
        move = 6,
        standGroundBonus = 3,
        weaponName = "Twin Shell Blades",
        weaponSummary = "1d2 physical per hit; always 2 attacks; 26% chance for a 3rd; STR 42% chance for +1 physical on first hit. STR 12+ removes heavy-armor speed penalty.",
        artRes = R.drawable.enemy_06_june_bug_heavy_dual_blade_raider
    )
)

data class DungeonEnemyDisplay(
    val id: String,
    val name: String,
    val role: String,
    val stats: HeroStats,
    val hp: Int,
    val armorClass: Int,
    val move: Int,
    val weaponText: String,
    val elementalAttack: ElementType?,
    @DrawableRes val artRes: Int
)

fun dungeonEnemyRosterForFloor(floor: Int): List<DungeonEnemyDisplay> {
    val tier = tierForDungeonFloor(floor)
    val base = tierOneBugEnemies.map { enemy ->
        val s = enemy.statsForTier(tier)
        DungeonEnemyDisplay(
            id = enemy.id,
            name = enemy.name,
            role = enemy.species,
            stats = s,
            hp = enemy.hp,
            armorClass = enemy.effectiveArmorClass(),
            move = enemy.effectiveMove(),
            weaponText = "${enemy.weapon.displayName}: ${enemy.attackText()}",
            elementalAttack = enemy.elementalAttack,
            artRes = baseBugArtResource(enemy.species)
        )
    }

    val bandPosition = ((floor.coerceAtLeast(1) - 1) % 10) + 1
    if (bandPosition < 6 && tier == 1) return base

    val expanded = expandedRegularBugEnemies.map { enemy ->
        DungeonEnemyDisplay(
            id = enemy.id,
            name = enemy.name,
            role = enemy.role,
            stats = enemy.statsForTier(tier),
            hp = enemy.hp,
            armorClass = enemy.armorClass,
            move = enemy.move,
            weaponText = "${enemy.weaponName}: ${enemy.weaponSummary}",
            elementalAttack = enemy.elementalAttack,
            artRes = enemy.artRes
        )
    }
    return base + expanded
}
