package com.grimforsaken.dungeondicefrogs

import kotlin.math.pow
import kotlin.random.Random

enum class ElementType {
    FIRE, ICE, LIGHTNING, POISON
}

enum class FrogColor(val immuneElement: ElementType, val displayName: String) {
    RED(ElementType.FIRE, "Red"),
    BLUE(ElementType.ICE, "Blue"),
    YELLOW(ElementType.LIGHTNING, "Yellow"),
    GREEN(ElementType.POISON, "Green");

    fun isImmuneTo(element: ElementType): Boolean = immuneElement == element
}

data class HeroStats(
    val strength: Int,
    val dexterity: Int,
    val constitution: Int
)

enum class EnemyWeaponStyle(val displayName: String) {
    DAGGER("Dagger"),
    SHORT_SWORD("Short Sword"),
    AXE("Axe"),
    BLUNT("Blunt")
}

data class EnemyVariant(
    val id: String,
    val name: String,
    val species: String,
    val variant: Int,
    val artIndex: Int,
    val tierOneStats: HeroStats,
    val hp: Int,
    val armorClass: Int,
    val move: Int,
    val weapon: EnemyWeaponStyle,
    val hasShield: Boolean = false,
    val elementalAttack: ElementType? = null
) {
    /** Every tier after Tier 1 adds +1 to STR, DEX, and CON. */
    fun statsForTier(tier: Int): HeroStats {
        val bonus = tier.coerceAtLeast(1) - 1
        return HeroStats(
            strength = tierOneStats.strength + bonus,
            dexterity = tierOneStats.dexterity + bonus,
            constitution = tierOneStats.constitution + bonus
        )
    }

    fun attackText(): String = when (weapon) {
        EnemyWeaponStyle.DAGGER -> "2 strikes, chance of 3"
        EnemyWeaponStyle.SHORT_SWORD -> "1 strike, chance of 2"
        EnemyWeaponStyle.AXE -> "1 strike; 30% bleed chance"
        EnemyWeaponStyle.BLUNT -> "1 strike; ignores shield and 2 AC; skips every 3rd attack turn"
    }

    fun effectiveArmorClass(): Int = armorClass + if (hasShield) 1 else 0
    fun effectiveMove(): Int = (move - if (hasShield) 1 else 0).coerceAtLeast(1)
}

fun roll3d6Stat(): Int = (1..3).sumOf { Random.nextInt(1, 7) }

fun rollCharacterStats(): HeroStats = HeroStats(
    strength = roll3d6Stat(),
    dexterity = roll3d6Stat(),
    constitution = roll3d6Stat()
)

fun randomFrogColor(): FrogColor = FrogColor.values().random()

fun frogColorEmoji(color: FrogColor): String = when (color) {
    FrogColor.RED -> "🔴🐸"
    FrogColor.BLUE -> "🔵🐸"
    FrogColor.YELLOW -> "🟡🐸"
    FrogColor.GREEN -> "🟢🐸"
}

fun elementalImmunityText(color: FrogColor): String = when (color) {
    FrogColor.RED -> "Fire damage and Burning"
    FrogColor.BLUE -> "Ice damage and Ice effects"
    FrogColor.YELLOW -> "Lightning damage and Lightning effects"
    FrogColor.GREEN -> "Poison damage and Poison effects"
}

data class ElementResolution(val damage: Int, val statusApplies: Boolean, val immune: Boolean)

fun resolveElementAgainstFrog(color: FrogColor, element: ElementType, damage: Int): ElementResolution {
    if (color.isImmuneTo(element)) return ElementResolution(0, false, true)
    return ElementResolution(damage.coerceAtLeast(0), true, false)
}

fun levelForXp(totalXp: Int): Int = 1 + totalXp.coerceAtLeast(0) / 10
fun xpRequiredForLevel(level: Int): Int = (level.coerceAtLeast(1) - 1) * 10
fun xpRequiredForNextLevel(totalXp: Int): Int = xpRequiredForLevel(levelForXp(totalXp) + 1)
fun xpForRecoveredLootTier(lootTier: Int): Int = lootTier.coerceAtLeast(1)
fun statPointsEarnedForLevelIncrease(oldLevel: Int, newLevel: Int): Int = ((newLevel - oldLevel).coerceAtLeast(0)) * 2

fun tierForDungeonFloor(floor: Int): Int = ((floor.coerceAtLeast(1) - 1) / 10) + 1
fun nextTierEnemiesCanAppear(floor: Int): Boolean = (((floor.coerceAtLeast(1) - 1) % 10) + 1) >= 6
fun enemyDifficultyMultiplier(tier: Int): Double = 1.5.pow((tier.coerceAtLeast(1) - 1).toDouble())

/**
 * Regular Tier-1 enemy individuals. Each variation has its own name and stats.
 * Art indices correspond to enemy_tier1_atlas.webp.
 * Dragonflies use ICE, Fireflies use FIRE, Poison Flies use POISON.
 */
val tierOneBugEnemies: List<EnemyVariant> = listOf(
    EnemyVariant("fly_v1", "Buzzbite", "Fly", 1, 0, HeroStats(5, 11, 6), hp = 10, armorClass = 0, move = 6, weapon = EnemyWeaponStyle.DAGGER),
    EnemyVariant("fly_v2", "Grumblegnat", "Fly", 2, 1, HeroStats(6, 10, 7), hp = 13, armorClass = 1, move = 5, weapon = EnemyWeaponStyle.SHORT_SWORD),

    EnemyVariant("mosquito_v1", "Needlezip", "Mosquito", 1, 2, HeroStats(4, 13, 5), hp = 9, armorClass = 0, move = 8, weapon = EnemyWeaponStyle.SHORT_SWORD),
    EnemyVariant("mosquito_v2", "Skeetersting", "Mosquito", 2, 3, HeroStats(5, 12, 6), hp = 11, armorClass = 0, move = 7, weapon = EnemyWeaponStyle.DAGGER),

    EnemyVariant("butterfly_v1", "Honeyflutter", "Butterfly", 1, 4, HeroStats(6, 11, 7), hp = 8, armorClass = 0, move = 6, weapon = EnemyWeaponStyle.DAGGER),
    EnemyVariant("butterfly_v2", "Bloomflutter", "Butterfly", 2, 5, HeroStats(7, 12, 8), hp = 10, armorClass = 1, move = 7, weapon = EnemyWeaponStyle.SHORT_SWORD),

    EnemyVariant("bee_v1", "Honeyjab", "Bee", 1, 6, HeroStats(8, 10, 8), hp = 12, armorClass = 1, move = 7, weapon = EnemyWeaponStyle.SHORT_SWORD),
    EnemyVariant("bee_v2", "Bumblebulwark", "Bee", 2, 7, HeroStats(9, 9, 10), hp = 17, armorClass = 1, move = 6, weapon = EnemyWeaponStyle.SHORT_SWORD, hasShield = true),

    EnemyVariant("dragonfly_v1", "Frostflutter", "Dragonfly", 1, 8, HeroStats(7, 14, 6), hp = 11, armorClass = 1, move = 8, weapon = EnemyWeaponStyle.DAGGER, elementalAttack = ElementType.ICE),
    EnemyVariant("dragonfly_v2", "Chilldart", "Dragonfly", 2, 9, HeroStats(8, 13, 7), hp = 13, armorClass = 1, move = 7, weapon = EnemyWeaponStyle.SHORT_SWORD, elementalAttack = ElementType.ICE),

    EnemyVariant("poison_fly_v1", "Gloomgloop", "Poison Fly", 1, 10, HeroStats(9, 8, 9), hp = 14, armorClass = 1, move = 6, weapon = EnemyWeaponStyle.AXE, elementalAttack = ElementType.POISON),
    EnemyVariant("poison_fly_v2", "Blightbonk", "Poison Fly", 2, 11, HeroStats(10, 9, 11), hp = 18, armorClass = 2, move = 4, weapon = EnemyWeaponStyle.BLUNT, elementalAttack = ElementType.POISON),

    EnemyVariant("firefly_v1", "Emberblink", "Firefly", 1, 12, HeroStats(6, 12, 7), hp = 10, armorClass = 0, move = 7, weapon = EnemyWeaponStyle.SHORT_SWORD, elementalAttack = ElementType.FIRE),
    EnemyVariant("firefly_v2", "Glowflare", "Firefly", 2, 13, HeroStats(7, 13, 8), hp = 13, armorClass = 1, move = 6, weapon = EnemyWeaponStyle.DAGGER, elementalAttack = ElementType.FIRE)
)
