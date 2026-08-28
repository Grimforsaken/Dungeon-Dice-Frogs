package com.grimforsaken.dungeondicefrogs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class TownMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DungeonDiceFrogsTownApp() }
    }
}

@Composable
fun DungeonDiceFrogsTownApp() {
    MaterialTheme {
        var hero by remember { mutableStateOf<HeroCharacter?>(null) }

        if (hero == null) {
            Surface(Modifier.background(Color(0xFF17100B))) {
                CharacterCreationScreen { created -> hero = created }
            }
            return@MaterialTheme
        }

        val activeHero = hero!!
        val stats = activeHero.stats
        var screenName by rememberSaveable { mutableStateOf(Screen.TOWN.name) }
        val screen = Screen.valueOf(screenName)
        var notice by remember { mutableStateOf("") }
        var coins by rememberSaveable { mutableStateOf(20) }
        val gear = remember { townGearCatalog() }
        val inventory = remember {
            mutableStateListOf(
                "backpack", "dagger1", "dagger2", "sword", "greatsword",
                "axe", "great_axe", "mace", "shield", "leather", "chain"
            )
        }
        val helpers = remember { mutableStateListOf<HiredHelper>() }
        val equipped = remember { mutableStateMapOf<SlotKey, String>() }
        val highestDungeonFloor = 1

        Surface(Modifier.background(Color(0xFF17100B))) {
            Box {
                when (screen) {
                    Screen.TOWN -> TownHubScreen(
                        stats = stats,
                        frogColor = activeHero.color,
                        inventory = inventory,
                        coins = coins,
                        onCoinsChange = { coins = it },
                        helpers = helpers,
                        highestDungeonFloor = highestDungeonFloor,
                        onNotice = { notice = it }
                    )
                    Screen.INVENTORY -> InventoryScreen(stats, inventory, equipped)
                    Screen.HERO -> EquipmentScreen(stats, inventory, gear, equipped) { notice = it }
                    Screen.DUNGEON -> DungeonScreen()
                }
                BottomNav(screen, Modifier.align(Alignment.BottomCenter)) { screenName = it.name }
                if (notice.isNotBlank()) {
                    Card(
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp, start = 18.dp, end = 18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xEE27170F))
                    ) {
                        Text(
                            notice,
                            color = Color(0xFFFFE9B4),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp).clickable { notice = "" }
                        )
                    }
                }
            }
        }
    }
}

fun townGearCatalog(): Map<String, Gear> = listOf(
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
