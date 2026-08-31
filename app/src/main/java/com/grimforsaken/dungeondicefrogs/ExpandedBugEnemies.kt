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
    val artIndex: Int,
    val elementalAttack: ElementType? = null,
    val hasShield: Boolean = false,
    @DrawableRes val legacyArtRes: Int
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
    RegularBugEnemy("lightning_bug_dual_dagger_scout", "Sparkstab", "Lightning Bug • fast dual-dagger striker", "Light Armor", HeroStats(9,15,10), 12,1,7,1, "Twin Lightning Daggers", "2 dagger attacks; chance for a 3rd; lightning effect on the first successful hit.", 14, ElementType.LIGHTNING, false, R.drawable.enemy_01_lightning_bug_dual_dagger_scout),
    RegularBugEnemy("ladybug_sword_shield_guard", "Cloverguard", "Ladybug • sword-and-shield defender", "Medium Armor + Shield", HeroStats(13,13,14), 15,2,6,3, "Clover Sword + Shield", "1d4 straight-blade attack; shield defense; 15% bleed chance.", 15, null, true, R.drawable.enemy_02_ladybug_sword_shield_guard),
    RegularBugEnemy("ladybug_two_handed_blunt_mystic", "Cloverbonk", "Ladybug • two-handed blunt bruiser", "Medium Armor", HeroStats(15,10,15), 18,2,4,2, "Clover Maul", "1d8 blunt; ignores shields and 2 AC; skips every 3rd attack turn.", 16, null, false, R.drawable.enemy_03_ladybug_two_handed_blunt_mystic),
    RegularBugEnemy("lightning_bug_thunder_axe_raider", "Voltcleaver", "Lightning Bug • thunder-axe raider", "Medium Armor", HeroStats(14,12,13), 16,2,5,2, "Thunder Axe", "1d6 axe; 30% bleed; lightning effect on the first successful hit.", 17, ElementType.LIGHTNING, false, R.drawable.enemy_04_lightning_bug_thunder_axe_raider),
    RegularBugEnemy("june_bug_heavy_shield_guard", "Bronzebulwark", "June Bug • heavy shield guard", "Heavy Armor + Shield", HeroStats(16,8,16), 22,3,5,4, "Shell Cleaver + Shield", "Heavy armored blocker; shield adds defense and reduces movement by 1.", 18, null, true, R.drawable.enemy_05_june_bug_heavy_shield_guard),
    RegularBugEnemy("june_bug_heavy_dual_blade_raider", "Shellslash", "June Bug • heavy dual-blade raider", "Heavy Armor", HeroStats(14,13,15), 19,3,4,3, "Twin Shell Blades", "2 blade attacks; chance for a 3rd attack.", 19, null, false, R.drawable.enemy_06_june_bug_heavy_dual_blade_raider)
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
    val isBoss: Boolean = false,
    @DrawableRes val artRes: Int
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
            artIndex = enemy.artIndex,
            artRes = baseBugArtResource(enemy.species)
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
            artIndex = enemy.artIndex,
            artRes = enemy.legacyArtRes
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
    isBoss = true,
    artRes = R.drawable.enemy_bee
)
