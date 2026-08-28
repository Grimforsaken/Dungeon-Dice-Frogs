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

data class EnemyTemplate(
    val id: String,
    val name: String,
    val icon: String,
    val tierOneStats: HeroStats,
    val elementalAttack: ElementType? = null
) {
    /** Every tier after Tier 1 adds +1 to every enemy stat. */
    fun statsForTier(tier: Int): HeroStats {
        val bonus = tier.coerceAtLeast(1) - 1
        return HeroStats(
            strength = tierOneStats.strength + bonus,
            dexterity = tierOneStats.dexterity + bonus,
            constitution = tierOneStats.constitution + bonus
        )
    }
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

/** Tier 1 enemy roster: the seven Feed the Frog bugs. */
val tierOneBugEnemies: List<EnemyTemplate> = listOf(
    EnemyTemplate("fly", "Fly", "🪰", HeroStats(5, 12, 5)),
    EnemyTemplate("mosquito", "Mosquito", "🦟", HeroStats(4, 13, 5)),
    EnemyTemplate("butterfly", "Butterfly", "🦋", HeroStats(5, 10, 6)),
    EnemyTemplate("bee", "Bee", "🐝", HeroStats(8, 11, 7)),
    EnemyTemplate("dragonfly", "Dragonfly", "🪰", HeroStats(8, 13, 7)),
    EnemyTemplate("poison_fly", "Poison Fly", "☠️🪰", HeroStats(6, 11, 7), ElementType.POISON),
    EnemyTemplate("firefly", "Firefly", "🔥🪰", HeroStats(6, 12, 6), ElementType.FIRE)
)
