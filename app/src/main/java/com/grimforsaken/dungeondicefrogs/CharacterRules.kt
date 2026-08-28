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
    val species: String,
    val variant: Int,
    val icon: String,
    val artKey: String,
    val tierOneStats: HeroStats,
    val hp: Int,
    val armorClass: Int,
    val move: Int,
    val weapon: EnemyWeaponStyle,
    val hasShield: Boolean = false,
    val elementalAttack: ElementType? = null
) {
    val name: String get() = "$species Variant $variant"

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
    FrogColor.BLUE -> "Ice effects"
    FrogColor.YELLOW -> "Lightning damage and Lightning stun"
    FrogColor.GREEN -> "Poison damage"
}

/** Matching frog colors take no damage and no status from that element. */
data class ElementResolution(val damage: Int, val statusApplies: Boolean, val immune: Boolean)

fun resolveElementAgainstFrog(color: FrogColor, element: ElementType, damage: Int): ElementResolution {
    if (color.isImmuneTo(element)) return ElementResolution(0, false, true)
    return ElementResolution(damage.coerceAtLeast(0), true, false)
}

// ----- Established character leveling -----

/** Level 1 = 0 XP, Level 2 = 10 XP, Level 3 = 20 XP, and so on. */
fun levelForXp(totalXp: Int): Int = 1 + totalXp.coerceAtLeast(0) / 10

fun xpRequiredForLevel(level: Int): Int = (level.coerceAtLeast(1) - 1) * 10

fun xpRequiredForNextLevel(totalXp: Int): Int = xpRequiredForLevel(levelForXp(totalXp) + 1)

/** Loot XP is one-for-one with loot tier. Enemy kills themselves grant 0 XP. */
fun xpForRecoveredLootTier(lootTier: Int): Int = lootTier.coerceAtLeast(1)

fun statPointsEarnedForLevelIncrease(oldLevel: Int, newLevel: Int): Int =
    ((newLevel - oldLevel).coerceAtLeast(0)) * 2

// ----- Established dungeon / monster tier rules -----

/** Tier 1 = floors 1-10, Tier 2 = 11-20, etc. */
fun tierForDungeonFloor(floor: Int): Int = ((floor.coerceAtLeast(1) - 1) / 10) + 1

/** Current design begins introducing the next tier halfway through a 10-floor band: floor 6, 16, 26, etc. */
fun nextTierEnemiesCanAppear(floor: Int): Boolean {
    val bandPosition = ((floor.coerceAtLeast(1) - 1) % 10) + 1
    return bandPosition >= 6
}

/** Cumulative difficulty multiplier: 1.0, 1.5, 2.25, 3.375 ... */
fun enemyDifficultyMultiplier(tier: Int): Double = 1.5.pow((tier.coerceAtLeast(1) - 1).toDouble())

/**
 * Tier 1 uses two variants of each of the seven Feed the Frog bugs.
 * Art identity is important:
 * - Fly = black fuzzy body, huge red eyes.
 * - Mosquito = long proboscis, red abdomen.
 * - Dragonfly = blue dragon-like insect with long tail and four wings.
 */
val tierOneBugEnemies: List<EnemyVariant> = listOf(
    EnemyVariant("fly_v1", "Fly", 1, "🪰", "fly_black_fuzzy_red_eyes_1", HeroStats(5, 11, 6), hp = 6, armorClass = 0, move = 5, weapon = EnemyWeaponStyle.DAGGER),
    EnemyVariant("fly_v2", "Fly", 2, "🪰", "fly_black_fuzzy_red_eyes_2", HeroStats(6, 10, 7), hp = 7, armorClass = 1, move = 5, weapon = EnemyWeaponStyle.SHORT_SWORD),

    EnemyVariant("mosquito_v1", "Mosquito", 1, "🦟", "mosquito_long_proboscis_red_abdomen_1", HeroStats(4, 13, 5), hp = 5, armorClass = 0, move = 6, weapon = EnemyWeaponStyle.DAGGER),
    EnemyVariant("mosquito_v2", "Mosquito", 2, "🦟", "mosquito_long_proboscis_red_abdomen_2", HeroStats(5, 12, 6), hp = 6, armorClass = 0, move = 6, weapon = EnemyWeaponStyle.SHORT_SWORD),

    EnemyVariant("butterfly_v1", "Butterfly", 1, "🦋", "butterfly_blue_gold_1", HeroStats(6, 11, 6), hp = 6, armorClass = 0, move = 5, weapon = EnemyWeaponStyle.DAGGER),
    EnemyVariant("butterfly_v2", "Butterfly", 2, "🦋", "butterfly_blue_gold_2", HeroStats(7, 12, 6), hp = 6, armorClass = 1, move = 6, weapon = EnemyWeaponStyle.SHORT_SWORD),

    EnemyVariant("bee_v1", "Bee", 1, "🐝", "bee_honey_1", HeroStats(8, 10, 8), hp = 8, armorClass = 1, move = 5, weapon = EnemyWeaponStyle.DAGGER),
    EnemyVariant("bee_v2", "Bee", 2, "🐝", "bee_honey_shield_2", HeroStats(9, 9, 9), hp = 9, armorClass = 1, move = 5, weapon = EnemyWeaponStyle.SHORT_SWORD, hasShield = true),

    EnemyVariant("dragonfly_v1", "Dragonfly", 1, "🐉", "dragonfly_blue_draconic_1", HeroStats(7, 13, 6), hp = 6, armorClass = 1, move = 6, weapon = EnemyWeaponStyle.SHORT_SWORD),
    EnemyVariant("dragonfly_v2", "Dragonfly", 2, "🐉", "dragonfly_blue_draconic_2", HeroStats(8, 14, 7), hp = 7, armorClass = 1, move = 7, weapon = EnemyWeaponStyle.DAGGER),

    EnemyVariant("poison_fly_v1", "Poison Fly", 1, "☠️🪰", "poison_fly_green_purple_blunt_1", HeroStats(9, 8, 9), hp = 9, armorClass = 1, move = 4, weapon = EnemyWeaponStyle.BLUNT, elementalAttack = ElementType.POISON),
    EnemyVariant("poison_fly_v2", "Poison Fly", 2, "☠️🪰", "poison_fly_green_purple_axe_2", HeroStats(8, 10, 8), hp = 8, armorClass = 1, move = 5, weapon = EnemyWeaponStyle.AXE, elementalAttack = ElementType.POISON),

    EnemyVariant("firefly_v1", "Firefly", 1, "🔥🪰", "firefly_actual_fire_1", HeroStats(6, 12, 6), hp = 6, armorClass = 0, move = 6, weapon = EnemyWeaponStyle.DAGGER, elementalAttack = ElementType.FIRE),
    EnemyVariant("firefly_v2", "Firefly", 2, "🔥🪰", "firefly_actual_fire_2", HeroStats(5, 13, 6), hp = 6, armorClass = 0, move = 6, weapon = EnemyWeaponStyle.SHORT_SWORD, elementalAttack = ElementType.FIRE)
)
