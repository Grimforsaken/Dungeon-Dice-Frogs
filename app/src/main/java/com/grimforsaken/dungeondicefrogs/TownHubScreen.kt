package com.grimforsaken.dungeondicefrogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

enum class ShopKind { BLACKSMITH, ITEM_SHOP, APOTHECARY, TAVERN }

data class HiredHelper(
    val role: String,
    val strength: Int,
    val dexterity: Int,
    val constitution: Int
)

data class TownShop(
    val kind: ShopKind,
    val title: String,
    val subtitle: String,
    val icon: String,
    val tileX: Int,
    val tileY: Int,
    val doorX: Int,
    val doorY: Int
)

data class ShopOffer(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val inventoryId: String? = null,
    val helperRole: String? = null
)

private val TownGold = Color(0xFFFFC62D)
private val TownCream = Color(0xFFFFE9B4)
private val TownGrass = Color(0xFF4F8F45)
private val TownStone = Color(0xFFA69A86)
private val TownSky = Color(0xFF7FC3E8)

@Composable
fun TownHubScreen(
    stats: HeroStats,
    inventory: MutableList<String>,
    coins: Int,
    onCoinsChange: (Int) -> Unit,
    helpers: MutableList<HiredHelper>,
    highestDungeonFloor: Int,
    onNotice: (String) -> Unit
) {
    val shops = remember { townShops() }
    var playerX by rememberSaveable { mutableStateOf(4) }
    var playerY by rememberSaveable { mutableStateOf(6) }
    var openShopName by rememberSaveable { mutableStateOf<String?>(null) }
    val standingShop = shops.firstOrNull { it.doorX == playerX && it.doorY == playerY }
    val openShop = openShopName?.let { saved -> shops.firstOrNull { it.kind.name == saved } }

    Box(Modifier.fillMaxSize().background(TownSky).padding(bottom = 78.dp)) {
        Column(
            Modifier.fillMaxWidth().background(Color(0xCC24170F)).padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("DUNGEON DICE FROGS", color = TownGold, fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text("🪙 $coins", color = TownCream, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Text("Town • free movement • no turns", color = TownCream, fontSize = 11.sp)
        }

        BoxWithConstraints(Modifier.fillMaxSize().padding(top = 55.dp)) {
            val tileSize = 39.dp
            val stepX = 20.dp
            val stepY = 11.dp
            val originX = maxWidth / 2f - tileSize / 2f
            val originY = 92.dp
            val blockedTiles = shops.map { it.tileX to it.tileY }.toSet()
            val doorTiles = shops.map { it.doorX to it.doorY }.toSet()

            Card(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 1.dp).size(92.dp, 80.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF302F38)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("♜", color = Color(0xFFE14A35), fontSize = 34.sp)
                    Text("TOWER DUNGEON", color = TownCream, fontWeight = FontWeight.Black, fontSize = 8.sp)
                }
            }

            for (y in 0..9) {
                for (x in 0..8) {
                    val pair = x to y
                    val blocked = pair in blockedTiles
                    val door = pair in doorTiles
                    val tileColor = when {
                        door -> Color(0xFFD4B25B)
                        x in 3..5 || y in 4..7 -> TownStone
                        else -> TownGrass
                    }
                    val p = isoOffset(originX, originY, x, y, stepX, stepY)
                    Box(
                        Modifier
                            .offset(p.first, p.second)
                            .size(tileSize)
                            .graphicsLayer { rotationZ = 45f; scaleY = 0.52f }
                            .background(tileColor, RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0x88685E50), RoundedCornerShape(4.dp))
                            .clickable(enabled = !blocked) {
                                playerX = x
                                playerY = y
                            }
                    )
                }
            }

            shops.forEach { shop ->
                val p = isoOffset(originX, originY, shop.tileX, shop.tileY, stepX, stepY)
                TownBuilding(shop, Modifier.offset(p.first - 31.dp, p.second - 49.dp))
            }

            val fountain = isoOffset(originX, originY, 4, 5, stepX, stepY)
            Text("⛲", fontSize = 28.sp, modifier = Modifier.offset(fountain.first + 3.dp, fountain.second - 23.dp))

            val player = isoOffset(originX, originY, playerX, playerY, stepX, stepY)
            Text("🐸", fontSize = 31.sp, modifier = Modifier.offset(player.first + 4.dp, player.second - 22.dp))

            if (standingShop != null) {
                Button(
                    onClick = { openShopName = standingShop.kind.name },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
                ) {
                    Text("Enter")
                }
            }
        }

        if (openShop != null) {
            ShopDialog(
                shop = openShop,
                coins = coins,
                highestDungeonFloor = highestDungeonFloor,
                helperCount = helpers.size,
                onDismiss = { openShopName = null },
                onBuy = { offer ->
                    if (coins < 1) {
                        onNotice("You need 1 coin.")
                        return@ShopDialog
                    }
                    if (offer.helperRole != null) {
                        if (highestDungeonFloor < 30) {
                            onNotice("Hired help unlocks after reaching Dungeon Floor 30.")
                            return@ShopDialog
                        }
                        if (helpers.size >= 2) {
                            onNotice("You can hire a maximum of two helpers.")
                            return@ShopDialog
                        }
                        val helper = HiredHelper(offer.helperRole, roll3d6(), roll3d6(), roll3d6())
                        helpers.add(helper)
                        onCoinsChange(coins - 1)
                        onNotice("Hired ${helper.role}: STR ${helper.strength}, DEX ${helper.dexterity}, CON ${helper.constitution}.")
                    } else {
                        val id = offer.inventoryId ?: return@ShopDialog
                        val capacity = 6 + townThresholdBonus(stats.strength) + if (inventory.contains("backpack")) 4 else 0
                        if (inventory.size >= capacity) {
                            onNotice("Inventory is full ($capacity slots).")
                            return@ShopDialog
                        }
                        inventory.add(id)
                        onCoinsChange(coins - 1)
                        onNotice("Purchased ${offer.name} for 1 coin.")
                    }
                }
            )
        }
    }
}

private fun isoOffset(originX: Dp, originY: Dp, x: Int, y: Int, stepX: Dp, stepY: Dp): Pair<Dp, Dp> {
    return (originX + stepX * (x - y).toFloat()) to (originY + stepY * (x + y).toFloat())
}

@Composable
private fun TownBuilding(shop: TownShop, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.size(101.dp, 59.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF5B3521)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(shop.icon, fontSize = 20.sp)
            Text(shop.title, color = TownGold, fontWeight = FontWeight.Black, fontSize = 9.sp, textAlign = TextAlign.Center)
            Text(shop.subtitle, color = TownCream, fontSize = 7.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ShopDialog(
    shop: TownShop,
    coins: Int,
    highestDungeonFloor: Int,
    helperCount: Int,
    onDismiss: () -> Unit,
    onBuy: (ShopOffer) -> Unit
) {
    val offers = remember(shop.kind) { shopOffers(shop.kind) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("${shop.icon} ${shop.title}", fontWeight = FontWeight.Black)
                Text("${shop.subtitle} • Development price: 1 coin", fontSize = 11.sp)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().height(390.dp).verticalScroll(rememberScrollState())) {
                Text("Coins: $coins", color = Color(0xFF8A6500), fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 8.dp))
                if (shop.kind == ShopKind.TAVERN && highestDungeonFloor < 30) {
                    Text(
                        "Hired help unlocks at Dungeon Floor 30. Maximum: 2 helpers.",
                        color = Color(0xFF9A5B38),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                offers.forEach { offer ->
                    val helperBlocked = offer.helperRole != null && (highestDungeonFloor < 30 || helperCount >= 2)
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1E3C4))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Text(offer.icon, fontSize = 27.sp, modifier = Modifier.padding(end = 8.dp))
                                Column {
                                    Text(offer.name, color = Color(0xFF17100B), fontWeight = FontWeight.Black)
                                    Text(offer.description, color = Color(0xFF58493B), fontSize = 10.sp)
                                }
                            }
                            Button(onClick = { onBuy(offer) }, enabled = coins >= 1 && !helperBlocked) {
                                Text("🪙 1")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private fun townThresholdBonus(stat: Int) = when {
    stat >= 18 -> 4
    stat >= 16 -> 3
    stat >= 14 -> 2
    stat >= 12 -> 1
    else -> 0
}

private fun roll3d6(): Int = (1..3).sumOf { Random.nextInt(1, 7) }

private fun townShops() = listOf(
    TownShop(ShopKind.BLACKSMITH, "BLACKSMITH", "Weapons & Armor", "⚒", 1, 3, 2, 4),
    TownShop(ShopKind.ITEM_SHOP, "ITEM SHOP", "Items", "🎒", 5, 1, 5, 2),
    TownShop(ShopKind.APOTHECARY, "APOTHECARY", "Healing & Health Potions", "🧪", 3, 7, 4, 7),
    TownShop(ShopKind.TAVERN, "TAVERN", "Hired Help", "🍺", 7, 5, 6, 5)
)

private fun shopOffers(kind: ShopKind): List<ShopOffer> = when (kind) {
    ShopKind.BLACKSMITH -> listOf(
        ShopOffer("dagger", "Dagger d2", "Always attacks twice; may strike three times.", "🗡", "dagger1"),
        ShopOffer("sword", "Sword d4", "Straight blade; chance for a second attack.", "⚔", "sword"),
        ShopOffer("greatsword", "Two-Handed Sword d6", "Two-handed straight blade.", "⚔", "greatsword"),
        ShopOffer("axe", "Axe d6", "One attack with a higher bleed chance.", "🪓", "axe"),
        ShopOffer("great_axe", "Two-Handed Axe d8", "Heavy two-handed axe.", "🪓", "great_axe"),
        ShopOffer("mace", "Blunt d8", "Ignores shields and 2 AC; slow weapon.", "🔨", "mace"),
        ShopOffer("great_mace", "Two-Handed Blunt d10", "Heavy blunt weapon; slow but powerful.", "🔨", "great_mace"),
        ShopOffer("shield", "Shield", "+1 AC and -1 movement.", "🛡", "shield"),
        ShopOffer("leather", "Leather Armor", "Light chest armor, AC 1.", "🥋", "leather"),
        ShopOffer("chain", "Chain Armor", "Medium chest armor, AC 3.", "⛓", "chain"),
        ShopOffer("plate", "Plate Armor", "Heavy chest armor, AC 5.", "🦺", "plate")
    )
    ShopKind.ITEM_SHOP -> listOf(
        ShopOffer("backpack", "Backpack", "Adds 4 carry slots.", "🎒", "backpack"),
        ShopOffer("scroll_return", "Scroll of Return", "Opens a return portal from the dungeon to town.", "📜", "scroll_return")
    )
    ShopKind.APOTHECARY -> listOf(
        ShopOffer("healing_potion", "Healing Potion", "Restorative potion for development testing.", "🧪", "healing_potion"),
        ShopOffer("health_potion", "Health Potion", "Health-restoring potion for development testing.", "❤️", "health_potion")
    )
    ShopKind.TAVERN -> listOf(
        ShopOffer("fighter_helper", "Fighter Helper", "Stats are rolled 3d6 when hired.", "🐸", helperRole = "Fighter Helper"),
        ShopOffer("scout_helper", "Scout Helper", "Stats are rolled 3d6 when hired.", "🐸", helperRole = "Scout Helper"),
        ShopOffer("guard_helper", "Guard Helper", "Stats are rolled 3d6 when hired.", "🐸", helperRole = "Guard Helper")
    )
}
