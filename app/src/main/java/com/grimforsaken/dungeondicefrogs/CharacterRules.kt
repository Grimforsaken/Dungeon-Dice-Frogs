package com.grimforsaken.dungeondicefrogs

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
    val tierOneStats: HeroStats
) {
    fun statsForTier(tier: Int): HeroStats {
        val bonus = (tier.coerceAtLeast(1) - 1)
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

fun randomFrogColor(): FrogColor = FrogColor.entries.random()

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

/**
 * Elemental resolution entry point for combat. A matching frog color receives
 * neither the elemental damage nor the related status effect.
 */
data class ElementResolution(val damage: Int, val statusApplies: Boolean, val immune: Boolean)

fun resolveElementAgainstFrog(color: FrogColor, element: ElementType, damage: Int): ElementResolution {
    if (color.isImmuneTo(element)) return ElementResolution(0, false, true)
    return ElementResolution(damage.coerceAtLeast(0), true, false)
}

/** Tier 1 enemy roster derived from the seven Feed the Frog bugs. */
val tierOneBugEnemies: List<EnemyTemplate> = listOf(
    EnemyTemplate("fly", "Fly", "🪰", HeroStats(5, 12, 5)),
    EnemyTemplate("mosquito", "Mosquito / Gnat", "🦟", HeroStats(4, 13, 5)),
    EnemyTemplate("ladybug", "Ladybug", "🐞", HeroStats(7, 8, 9)),
    EnemyTemplate("dragonfly", "Dragonfly", "🪰", HeroStats(8, 13, 7)),
    EnemyTemplate("bee", "Bee", "🐝", HeroStats(8, 11, 7)),
    EnemyTemplate("butterfly", "Butterfly", "🦋", HeroStats(5, 10, 6)),
    EnemyTemplate("caterpillar", "Caterpillar", "🐛", HeroStats(8, 5, 10))
)
