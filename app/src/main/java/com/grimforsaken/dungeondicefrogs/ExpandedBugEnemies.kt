package com.grimforsaken.dungeondicefrogs

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
    val artIndex: Int,
    val elementalAttack: ElementType? = null,
    val hasShield: Boolean = false
) {
    fun statsForTier(tier: Int): HeroStats {
        val bonus = tier.coerceAtLeast(1) - 1
        return HeroStats(
            strength = baseStats.strength + bonus,
            dexterity = baseStats.dexterity + bonus,
            constitution = baseStats.constitution + bonus
        )
    }

    fun effectiveArmorClass(): Int = armorClass + if (hasShield) 1 else 0
    fun effectiveMove(): Int = (move - if (hasShield) 1 else 0).coerceAtLeast(1)
}

/** All six are regular Tier-1 enemies. There is no elite class in Tier 1. */
val tierOneAdditionalRegularBugEnemies: List<RegularBugEnemy> = listOf(
    RegularBugEnemy(
        id = "lightning_bug_dual_dagger_scout",
        name = "Sparkstab",
        role = "Lightning Bug • fast dual-dagger striker",
        armorType = "Light Armor",
        baseStats = HeroStats(9, 15, 10),
        hp = 12,
        armorClass = 1,
        move = 7,
        standGroundBonus = 1,
        weaponName = "Twin Lightning Daggers",
        weaponSummary = "2 dagger attacks; chance for a 3rd; lightning effect on the first successful hit.",
        artIndex = 14,
        elementalAttack = ElementType.LIGHTNING
    ),
    RegularBugEnemy(
        id = "ladybug_sword_shield_guard",
        name = "Cloverguard",
        role = "Ladybug • sword-and-shield defender",
        armorType = "Medium Armor + Shield",
        baseStats = HeroStats(13, 13, 14),
        hp = 15,
        armorClass = 2,
        move = 6,
        standGroundBonus = 3,
        weaponName = "Clover Sword + Shield",
        weaponSummary = "1d4 straight-blade attack; shield defense; 15% bleed chance.",
        artIndex = 15,
        hasShield = true
    ),
    RegularBugEnemy(
        id = "ladybug_two_handed_blunt_mystic",
        name = "Cloverbonk",
        role = "Ladybug • two-handed blunt bruiser",
        armorType = "Medium Armor",
        baseStats = HeroStats(15, 10, 15),
        hp = 18,
        armorClass = 2,
        move = 4,
        standGroundBonus = 2,
        weaponName = "Clover Maul",
        weaponSummary = "1d8 blunt; ignores shields and 2 AC; skips every 3rd attack turn.",
        artIndex = 16
    ),
    RegularBugEnemy(
        id = "lightning_bug_thunder_axe_raider",
        name = "Voltcleaver",
        role = "Lightning Bug • thunder-axe raider",
        armorType = "Medium Armor",
        baseStats = HeroStats(14, 12, 13),
        hp = 16,
        armorClass = 2,
        move = 5,
        standGroundBonus = 2,
        weaponName = "Thunder Axe",
        weaponSummary = "1d6 axe; 30% bleed; lightning effect on the first successful hit.",
        artIndex = 17,
        elementalAttack = ElementType.LIGHTNING
    ),
    RegularBugEnemy(
        id = "june_bug_heavy_shield_guard",
        name = "Bronzebulwark",
        role = "June Bug • heavy shield guard",
        armorType = "Heavy Armor + Shield",
        baseStats = HeroStats(16, 8, 16),
        hp = 22,
        armorClass = 3,
        move = 5,
        standGroundBonus = 4,
        weaponName = "Shell Cleaver + Shield",
        weaponSummary = "Heavy armored blocker; shield adds defense and reduces movement by 1.",
        artIndex = 18,
        hasShield = true
    ),
    RegularBugEnemy(
        id = "june_bug_heavy_dual_blade_raider",
        name = "Shellslash",
        role = "June Bug • heavy dual-blade raider",
        armorType = "Heavy Armor",
        baseStats = HeroStats(14, 13, 15),
        hp = 19,
        armorClass = 3,
        move = 4,
        standGroundBonus = 3,
        weaponName = "Twin Shell Blades",
        weaponSummary = "2 blade attacks; chance for a 3rd attack.",
        artIndex = 19
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
    val artIndex: Int,
    val isBoss: Boolean = false
)

private fun elementSuffix(element: ElementType?): String = when (element) {
    ElementType.FIRE -> " • FIRE effect"
    ElementType.ICE -> " • ICE effect"
    ElementType.LIGHTNING -> " • LIGHTNING effect"
    ElementType.POISON -> " • POISON effect"
    null -> ""
}

fun dungeonEnemyRosterForFloor(floor: Int): List<DungeonEnemyDisplay> {
    val tier = tierForDungeonFloor(floor)
    val base = tierOneBugEnemies.map { enemy ->
        DungeonEnemyDisplay(
            id = enemy.id,
            name = enemy.name,
            role = enemy.species,
            stats = enemy.statsForTier(tier),
            hp = enemy.hp,
            armorClass = enemy.effectiveArmorClass(),
            move = enemy.effectiveMove(),
            weaponText = "${enemy.weapon.displayName}: ${enemy.attackText()}${elementSuffix(enemy.elementalAttack)}",
            elementalAttack = enemy.elementalAttack,
            artIndex = enemy.artIndex
        )
    }

    val additionalRegular = tierOneAdditionalRegularBugEnemies.map { enemy ->
        DungeonEnemyDisplay(
            id = enemy.id,
            name = enemy.name,
            role = enemy.role,
            stats = enemy.statsForTier(tier),
            hp = enemy.hp,
            armorClass = enemy.effectiveArmorClass(),
            move = enemy.effectiveMove(),
            weaponText = "${enemy.weaponName}: ${enemy.weaponSummary}${elementSuffix(enemy.elementalAttack)}",
            elementalAttack = enemy.elementalAttack,
            artIndex = enemy.artIndex
        )
    }

    return base + additionalRegular
}

/** The only boss on Floors 1-10. It appears in the Floor-10 boss room. */
val tierOneFloorTenBoss = DungeonEnemyDisplay(
    id = "stormsting_sovereign",
    name = "Stormsting Sovereign",
    role = "Lightning Bee • Tier-1 Boss",
    stats = HeroStats(17, 16, 17),
    hp = 60,
    armorClass = 4,
    move = 7,
    weaponText = "3 attacks/turn: Dagger 1 (1d2+2), Dagger 2 (1d2+2), Short Sword (1d4+2) • LIGHTNING effect",
    elementalAttack = ElementType.LIGHTNING,
    artIndex = -1,
    isBoss = true
)
