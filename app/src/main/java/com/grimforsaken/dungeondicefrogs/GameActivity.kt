package com.grimforsaken.dungeondicefrogs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class GameActivity : ComponentActivity() {
    companion object {
        const val EXTRA_START_SCREEN = "start_screen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val world = PlayerCharacterRepository.loadWorldProgress(this)
        val requestedScreen = intent.getStringExtra(EXTRA_START_SCREEN)?.let { name ->
            runCatching { Screen.valueOf(name) }.getOrNull()
        } ?: world.lastScreen

        setContent {
            PersistentDungeonDiceFrogsApp(initialScreen = requestedScreen)
        }
    }
}

private val GameDark = Color(0xFF17100B)
private val GameBrown = Color(0xFF432719)
private val GameGold = Color(0xFFFFC62D)
private val GameCream = Color(0xFFFFE9B4)

@Composable
fun PersistentDungeonDiceFrogsApp(initialScreen: Screen) {
    val context = LocalContext.current
    val loadedCharacter = remember { PlayerCharacterRepository.loadMainCharacter(context) }
    val loadedWorld = remember { PlayerCharacterRepository.loadWorldProgress(context) }

    var frogColorName by rememberSaveable { mutableStateOf(loadedCharacter?.color?.name) }
    var strength by rememberSaveable { mutableStateOf(loadedCharacter?.strength ?: 0) }
    var dexterity by rememberSaveable { mutableStateOf(loadedCharacter?.dexterity ?: 0) }
    var constitution by rememberSaveable { mutableStateOf(loadedCharacter?.constitution ?: 0) }
    var xp by rememberSaveable { mutableStateOf(loadedCharacter?.xp ?: 0) }
    var unspentStatPoints by rememberSaveable { mutableStateOf(loadedCharacter?.unspentStatPoints ?: 0) }
    var screenName by rememberSaveable { mutableStateOf(initialScreen.name) }
    var coins by rememberSaveable { mutableStateOf(loadedWorld.coins) }
    var highestDungeonFloor by rememberSaveable { mutableStateOf(loadedWorld.highestDungeonFloor) }
    var notice by remember { mutableStateOf("") }

    val inventory = remember {
        mutableStateListOf(
            "backpack", "dagger1", "dagger2", "sword", "greatsword",
            "axe", "great_axe", "mace", "shield", "leather", "chain"
        )
    }
    val equipped = remember { mutableStateMapOf<SlotKey, String>() }
    val helpers = remember { mutableStateListOf<HiredHelper>() }
    val gear = remember { persistentGearCatalog() }

    fun saveCharacter() {
        val color = frogColorName?.let { runCatching { FrogColor.valueOf(it) }.getOrNull() } ?: return
        if (strength <= 0 || dexterity <= 0 || constitution <= 0) return
        PlayerCharacterRepository.saveMainCharacter(
            context,
            MainCharacterSave(
                color = color,
                strength = strength,
                dexterity = dexterity,
                constitution = constitution,
                xp = xp,
                unspentStatPoints = unspentStatPoints
            )
        )
    }

    fun saveWorld(screen: Screen = Screen.valueOf(screenName)) {
        PlayerCharacterRepository.saveWorldProgress(
            context = context,
            coins = coins,
            highestDungeonFloor = highestDungeonFloor,
            lastScreen = screen
        )
    }

    val color = frogColorName?.let { runCatching { FrogColor.valueOf(it) }.getOrNull() }
    val hasLivingCharacter = color != null && strength > 0 && dexterity > 0 && constitution > 0
    val stats = HeroStats(strength, dexterity, constitution)
    val level = levelForXp(xp)

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = GameDark) {
            if (!hasLivingCharacter) {
                MainFrogCreationScreen { chosenColor, rolledStats ->
                    frogColorName = chosenColor.name
                    strength = rolledStats.strength
                    dexterity = rolledStats.dexterity
                    constitution = rolledStats.constitution
                    xp = 0
                    unspentStatPoints = 0
                    PlayerCharacterRepository.saveMainCharacter(
                        context,
                        MainCharacterSave(
                            color = chosenColor,
                            strength = rolledStats.strength,
                            dexterity = rolledStats.dexterity,
                            constitution = rolledStats.constitution,
                            xp = 0,
                            unspentStatPoints = 0
                        )
                    )
                    saveWorld(Screen.valueOf(screenName))
                    notice = "${chosenColor.displayName} frog created. This is your main character until it dies."
                }
            } else {
                val currentColor = color!!
                val screen = runCatching { Screen.valueOf(screenName) }.getOrDefault(Screen.TOWN)

                Box(Modifier.fillMaxSize()) {
                    when (screen) {
                        Screen.TOWN -> TownHubScreen(
                            stats = stats,
                            inventory = inventory,
                            coins = coins,
                            onCoinsChange = {
                                coins = it
                                PlayerCharacterRepository.saveWorldProgress(context, coins, highestDungeonFloor, screen)
                            },
                            helpers = helpers,
                            highestDungeonFloor = highestDungeonFloor,
                            onNotice = { notice = it }
                        )

                        Screen.INVENTORY -> InventoryScreen(stats, inventory, equipped)

                        Screen.HERO -> EquipmentScreen(
                            stats = stats,
                            frogColor = currentColor,
                            level = level,
                            xp = xp,
                            unspentStatPoints = unspentStatPoints,
                            inventory = inventory,
                            catalog = gear,
                            equipped = equipped,
                            onSpendStat = { stat ->
                                if (unspentStatPoints > 0) {
                                    when (stat) {
                                        "STR" -> strength += 1
                                        "DEX" -> dexterity += 1
                                        "CON" -> constitution += 1
                                    }
                                    unspentStatPoints -= 1
                                    saveCharacter()
                                }
                            },
                            onNotice = { notice = it }
                        )

                        Screen.DUNGEON -> PersistentDungeonScreen(
                            frogColor = currentColor,
                            level = level,
                            xp = xp,
                            highestDungeonFloor = highestDungeonFloor,
                            helperCount = helpers.size,
                            onRecoverLoot = { lootTier ->
                                val award = xpForRecoveredLootTier(lootTier)
                                val participants = 1 + helpers.size
                                val equalShare = award / participants
                                val remainder = award % participants
                                val playerGain = equalShare + remainder
                                val oldPlayerLevel = levelForXp(xp)
                                xp += playerGain
                                val newPlayerLevel = levelForXp(xp)
                                val earnedPoints = statPointsEarnedForLevelIncrease(oldPlayerLevel, newPlayerLevel)
                                unspentStatPoints += earnedPoints

                                helpers.forEach { helper ->
                                    val oldHelperLevel = levelForXp(helper.xp)
                                    helper.xp += equalShare
                                    val newHelperLevel = levelForXp(helper.xp)
                                    helper.level = newHelperLevel
                                    helper.unspentStatPoints += statPointsEarnedForLevelIncrease(oldHelperLevel, newHelperLevel)
                                }

                                saveCharacter()
                                val levelText = if (earnedPoints > 0) {
                                    " Level $newPlayerLevel reached: +$earnedPoints stat points."
                                } else ""
                                notice = "Recovered Tier $lootTier loot. Main frog receives $playerGain XP.$levelText"
                            },
                            onAdvanceFloor = {
                                highestDungeonFloor += 1
                                PlayerCharacterRepository.saveWorldProgress(context, coins, highestDungeonFloor, screen)
                                notice = "Highest dungeon floor is now $highestDungeonFloor."
                            },
                            onCharacterDeath = {
                                PlayerCharacterRepository.recordMainCharacterDeath(context)
                                frogColorName = null
                                strength = 0
                                dexterity = 0
                                constitution = 0
                                xp = 0
                                unspentStatPoints = 0
                                equipped.clear()
                                helpers.clear()
                                screenName = Screen.TOWN.name
                                PlayerCharacterRepository.saveWorldProgress(context, coins, highestDungeonFloor, Screen.TOWN)
                                notice = "Your main frog has died. Create a new main character to continue."
                            }
                        )
                    }

                    BottomNav(screen, Modifier.align(Alignment.BottomCenter)) { destination ->
                        screenName = destination.name
                        PlayerCharacterRepository.saveWorldProgress(
                            context,
                            coins,
                            highestDungeonFloor,
                            destination
                        )
                    }

                    if (notice.isNotBlank()) {
                        Card(
                            modifier = Modifier.align(Alignment.TopCenter).padding(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xEE27170F))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    notice,
                                    color = GameCream,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(12.dp).weight(1f)
                                )
                                TextButton(onClick = { notice = "" }) {
                                    Text("×", color = GameGold, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainFrogCreationScreen(onCreated: (FrogColor, HeroStats) -> Unit) {
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var rolledStrength by rememberSaveable { mutableStateOf(0) }
    var rolledDexterity by rememberSaveable { mutableStateOf(0) }
    var rolledConstitution by rememberSaveable { mutableStateOf(0) }

    val selected = selectedName?.let { runCatching { FrogColor.valueOf(it) }.getOrNull() }
    val rolled = rolledStrength > 0 && rolledDexterity > 0 && rolledConstitution > 0

    Column(
        Modifier
            .fillMaxSize()
            .background(GameDark)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "CREATE YOUR MAIN FROG",
            color = GameGold,
            fontWeight = FontWeight.Black,
            fontSize = 25.sp,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )
        Text(
            "You keep this frog until it dies. Only then can you create a replacement main character.",
            color = GameCream,
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 18.dp)
        )

        FrogColor.values().forEach { frogColor ->
            val isSelected = frogColor == selected
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF6A4A1B) else GameBrown
                ),
                onClick = {
                    selectedName = frogColor.name
                    rolledStrength = 0
                    rolledDexterity = 0
                    rolledConstitution = 0
                }
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(frogColorEmoji(frogColor), fontSize = 30.sp, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text(
                            "${frogColor.displayName} Frog",
                            color = GameGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Text(
                            "Immune: ${elementalImmunityText(frogColor)}",
                            color = GameCream,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        if (selected != null && !rolled) {
            Button(
                onClick = {
                    val stats = rollCharacterStats()
                    rolledStrength = stats.strength
                    rolledDexterity = stats.dexterity
                    rolledConstitution = stats.constitution
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Roll 3d6 For Each Stat")
            }
        }

        if (selected != null && rolled) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CreationStat("STR", rolledStrength)
                CreationStat("DEX", rolledDexterity)
                CreationStat("CON", rolledConstitution)
            }
            Button(
                onClick = {
                    onCreated(
                        selected,
                        HeroStats(rolledStrength, rolledDexterity, rolledConstitution)
                    )
                }
            ) {
                Text("Begin With This Frog")
            }
        }
    }
}

@Composable
private fun CreationStat(label: String, value: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = GameBrown)) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = GameCream, fontSize = 11.sp)
            Text(value.toString(), color = GameGold, fontWeight = FontWeight.Black, fontSize = 25.sp)
            Text("3d6", color = Color.Gray, fontSize = 9.sp)
        }
    }
}

private fun persistentGearCatalog(): Map<String, Gear> = listOf(
    Gear("backpack", "Backpack +4", "🎒", GearType.BACKPACK),
    Gear("dagger1", "Dagger d2", "🗡", GearType.WEAPON, dagger = true),
    Gear("dagger2", "Dagger d2", "🗡", GearType.WEAPON, dagger = true),
    Gear("sword", "Sword d4", "⚔", GearType.WEAPON),
    Gear("greatsword", "2H Sword d6", "⚔", GearType.WEAPON, twoHanded = true),
    Gear("axe", "Axe d6", "🪓", GearType.WEAPON),
    Gear("great_axe", "2H Axe d8", "🪓", GearType.WEAPON, twoHanded = true),
    Gear("mace", "Blunt d8", "🔨", GearType.WEAPON),
    Gear("great_mace", "2H Blunt d10", "🔨", GearType.WEAPON, twoHanded = true),
    Gear("shield", "Shield +1 AC", "🛡", GearType.SHIELD),
    Gear("leather", "Leather AC 1", "🥋", GearType.CHEST, armorClass = 1, armorWeight = "light"),
    Gear("chain", "Chain AC 3", "⛓", GearType.CHEST, armorClass = 3, armorWeight = "medium"),
    Gear("plate", "Plate AC 5", "🦺", GearType.CHEST, armorClass = 5, armorWeight = "heavy")
).associateBy { it.id }
