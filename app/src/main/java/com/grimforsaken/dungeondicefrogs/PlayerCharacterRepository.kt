package com.grimforsaken.dungeondicefrogs

import android.content.Context

/**
 * Persistent save for the player's one active main frog.
 *
 * A living frog remains the main character across the home screen, activity
 * recreation, app restarts, and game navigation. Character creation is only
 * available when no living main frog is saved. Death clears these character
 * fields so a replacement frog can then be created.
 */
data class MainCharacterSave(
    val color: FrogColor,
    val strength: Int,
    val dexterity: Int,
    val constitution: Int,
    val xp: Int,
    val unspentStatPoints: Int
)

data class PersistentWorldProgress(
    val coins: Int = 20,
    val highestDungeonFloor: Int = 1,
    val lastScreen: Screen = Screen.TOWN
)

object PlayerCharacterRepository {
    private const val PREFS = "dungeon_dice_frogs_player"
    private const val KEY_ALIVE = "main_frog_alive"
    private const val KEY_COLOR = "main_frog_color"
    private const val KEY_STR = "main_frog_strength"
    private const val KEY_DEX = "main_frog_dexterity"
    private const val KEY_CON = "main_frog_constitution"
    private const val KEY_XP = "main_frog_xp"
    private const val KEY_POINTS = "main_frog_unspent_stat_points"
    private const val KEY_COINS = "coins"
    private const val KEY_HIGHEST_FLOOR = "highest_dungeon_floor"
    private const val KEY_LAST_SCREEN = "last_screen"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasLivingMainCharacter(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ALIVE, false) && loadMainCharacter(context) != null

    fun loadMainCharacter(context: Context): MainCharacterSave? {
        val p = prefs(context)
        if (!p.getBoolean(KEY_ALIVE, false)) return null
        val colorName = p.getString(KEY_COLOR, null) ?: return null
        val color = runCatching { FrogColor.valueOf(colorName) }.getOrNull() ?: return null
        val strength = p.getInt(KEY_STR, 0)
        val dexterity = p.getInt(KEY_DEX, 0)
        val constitution = p.getInt(KEY_CON, 0)
        if (strength <= 0 || dexterity <= 0 || constitution <= 0) return null
        return MainCharacterSave(
            color = color,
            strength = strength,
            dexterity = dexterity,
            constitution = constitution,
            xp = p.getInt(KEY_XP, 0).coerceAtLeast(0),
            unspentStatPoints = p.getInt(KEY_POINTS, 0).coerceAtLeast(0)
        )
    }

    fun saveMainCharacter(context: Context, character: MainCharacterSave) {
        prefs(context).edit()
            .putBoolean(KEY_ALIVE, true)
            .putString(KEY_COLOR, character.color.name)
            .putInt(KEY_STR, character.strength)
            .putInt(KEY_DEX, character.dexterity)
            .putInt(KEY_CON, character.constitution)
            .putInt(KEY_XP, character.xp)
            .putInt(KEY_POINTS, character.unspentStatPoints)
            .apply()
    }

    /**
     * Clears only the dead main frog. Dungeon floor geometry/state and general
     * world progression remain intact. A subsequent game launch will therefore
     * enter character creation exactly once for the replacement main frog.
     */
    fun recordMainCharacterDeath(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_ALIVE, false)
            .remove(KEY_COLOR)
            .remove(KEY_STR)
            .remove(KEY_DEX)
            .remove(KEY_CON)
            .remove(KEY_XP)
            .remove(KEY_POINTS)
            .apply()
    }

    fun loadWorldProgress(context: Context): PersistentWorldProgress {
        val p = prefs(context)
        val screen = runCatching {
            Screen.valueOf(p.getString(KEY_LAST_SCREEN, Screen.TOWN.name) ?: Screen.TOWN.name)
        }.getOrDefault(Screen.TOWN)
        return PersistentWorldProgress(
            coins = p.getInt(KEY_COINS, 20).coerceAtLeast(0),
            highestDungeonFloor = p.getInt(KEY_HIGHEST_FLOOR, 1).coerceAtLeast(1),
            lastScreen = screen
        )
    }

    fun saveWorldProgress(
        context: Context,
        coins: Int,
        highestDungeonFloor: Int,
        lastScreen: Screen
    ) {
        prefs(context).edit()
            .putInt(KEY_COINS, coins.coerceAtLeast(0))
            .putInt(KEY_HIGHEST_FLOOR, highestDungeonFloor.coerceAtLeast(1))
            .putString(KEY_LAST_SCREEN, lastScreen.name)
            .apply()
    }
}
