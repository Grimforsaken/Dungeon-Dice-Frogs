package com.grimforsaken.dungeondicefrogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

enum class ShopKind { BLACKSMITH, ITEM_SHOP, APOTHECARY, TAVERN }

data class HiredHelper(
    val role: String,
    val color: FrogColor,
    var strength: Int,
    var dexterity: Int,
    var constitution: Int,
    var xp: Int = 0,
    var level: Int = 1,
    var unspentStatPoints: Int = 0
)

data class TownShop(
    val kind: ShopKind,
    val title: String,
    val subtitle: String,
    val doorX: Int,
    val doorY: Int
)

data class ShopOffer(
    val id: String,
    val name: String,
    val description: String,
    val inventoryId: String? = null,
    val helperRole: String? = null
)

private val TownGold = Color(0xFFFFC62D)
private val TownCream = Color(0xFFFFE9B4)
private val TownSky = Color(0xFF274F55)

@Composable
fun TownHubScreen(
    stats: HeroStats,
    inventory: MutableList<String>,
    coins: Int,
    onCoinsChange: (Int) -> Unit,
    helpers: MutableList<HiredHelper>,
    highestDungeonFloor: Int,
    onNotice: (String) -> Unit,
    frogColor: FrogColor = FrogColor.GREEN,
    onEnterDungeon: (() -> Unit)? = null
) {
    val shops = remember { townShops() }
    var playerX by rememberSaveable { mutableStateOf(4) }
    var playerY by rememberSaveable { mutableStateOf(9) }
    var openShopName by rememberSaveable { mutableStateOf<String?>(null) }

    val standingShop = shops.firstOrNull { it.doorX == playerX && it.doorY == playerY }
    val openShop = openShopName?.let { name -> shops.firstOrNull { it.kind.name == name } }
    val blockedTiles = remember { townBlockedTiles() }

    fun moveTo(nx: Int, ny: Int) {
        if (nx !in 0..8 || ny !in 0..9) return
        if ((nx to ny) in blockedTiles) return
        if (abs(nx - playerX) + abs(ny - playerY) != 1) return
        playerX = nx
        playerY = ny
    }

    Column(Modifier.fillMaxSize().background(TownSky).padding(bottom = 78.dp)) {
        Column(Modifier.fillMaxWidth().background(Color(0xEE24170F)).padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("DUNGEON DICE FROGS", color = TownGold, fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text("Coins $coins", color = TownCream, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Text("Town • move one square at a time • no turns", color = TownCream, fontSize = 11.sp)
        }

        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val mapSize = maxWidth
            val tileWidth = mapSize / 9f
            val tileHeight = mapSize / 10f
            val doorTiles = shops.map { it.doorX to it.doorY }.toSet()

            Image(
                painter = painterResource(R.drawable.town_map),
                contentDescription = "Town map",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            TownArtLayer(R.drawable.town_item_shop, "Item Shop", Modifier.offset(x = mapSize * 0.045f, y = mapSize * 0.105f).size(mapSize * 0.34f))
            TownArtLayer(R.drawable.town_blacksmith, "Blacksmith", Modifier.offset(x = mapSize * 0.615f, y = mapSize * 0.105f).size(mapSize * 0.34f))
            TownArtLayer(R.drawable.town_apothecary, "Apothecary", Modifier.offset(x = mapSize * 0.055f, y = mapSize * 0.575f).size(mapSize * 0.31f))
            TownArtLayer(R.drawable.town_tavern, "Tavern", Modifier.offset(x = mapSize * 0.645f, y = mapSize * 0.585f).size(mapSize * 0.30f))
            TownArtLayer(R.drawable.town_fountain, "Frog fountain", Modifier.offset(x = mapSize * 0.39f, y = mapSize * 0.385f).size(mapSize * 0.22f))

            for (y in 0..9) {
                for (x in 0..8) {
                    val blocked = (x to y) in blockedTiles
                    val adjacent = abs(x - playerX) + abs(y - playerY) == 1
                    val door = (x to y) in doorTiles
                    Box(
                        Modifier
                            .offset(x = tileWidth * x.toFloat(), y = tileHeight * y.toFloat())
                            .size(tileWidth, tileHeight)
                            .background(if (door) Color(0x22FFD54F) else Color.Transparent)
                            .border(if (door) 1.dp else 0.25.dp, if (door) Color(0x99FFD54F) else Color(0x18FFFFFF))
                            .clickable(enabled = !blocked && adjacent) { moveTo(x, y) }
                    )
                }
            }

            Image(
                painter = painterResource(frogArtResource(frogColor)),
                contentDescription = "${frogColor.displayName} main frog",
                modifier = Modifier
                    .offset(x = tileWidth * playerX.toFloat() + tileWidth * 0.12f, y = tileHeight * playerY.toFloat() - tileHeight * 0.05f)
                    .size(tileWidth * 0.78f, tileHeight * 0.95f),
                contentScale = ContentScale.Fit
            )

            if (standingShop != null) {
                Button(
                    onClick = { openShopName = standingShop.kind.name },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                ) { Text("Enter ${standingShop.title}") }
            } else if (playerX == 4 && playerY == 0 && onEnterDungeon != null) {
                Button(
                    onClick = onEnterDungeon,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp)
                ) { Text("Enter Dungeon") }
            }
        }

        TownMovePad(
            onNorth = { moveTo(playerX, playerY - 1) },
            onSouth = { moveTo(playerX, playerY + 1) },
            onWest = { moveTo(playerX - 1, playerY) },
            onEast = { moveTo(playerX + 1, playerY) }
        )
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
                    val helperColor = randomFrogColor()
                    val rolled = rollCharacterStats()
                    helpers.add(
                        HiredHelper(
                            role = offer.helperRole,
                            color = helperColor,
                            strength = rolled.strength,
                            dexterity = rolled.dexterity,
                            constitution = rolled.constitution
                        )
                    )
                    onCoinsChange(coins - 1)
                    onNotice("Hired a ${helperColor.displayName} ${offer.helperRole}: STR ${rolled.strength}, DEX ${rolled.dexterity}, CON ${rolled.constitution}.")
                } else {
                    val id = offer.inventoryId ?: return@ShopDialog
                    val capacity = 6 + townThresholdBonus(stats.strength) + (if (inventory.contains("backpack")) 4 else 0)
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

@Composable
private fun TownMovePad(onNorth: () -> Unit, onSouth: () -> Unit, onWest: () -> Unit, onEast: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = onNorth, modifier = Modifier.size(width = 72.dp, height = 38.dp)) { Text("↑") }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onWest, modifier = Modifier.size(width = 72.dp, height = 38.dp)) { Text("←") }
            Text("MOVE", color = TownCream, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Button(onClick = onEast, modifier = Modifier.size(width = 72.dp, height = 38.dp)) { Text("→") }
        }
        Button(onClick = onSouth, modifier = Modifier.size(width = 72.dp, height = 38.dp)) { Text("↓") }
    }
}

@Composable
private fun TownArtLayer(drawable: Int, description: String, modifier: Modifier) {
    Image(painter = painterResource(drawable), contentDescription = description, modifier = modifier, contentScale = ContentScale.Fit)
}

private fun townBlockedTiles(): Set<Pair<Int, Int>> = buildSet {
    for (y in 1..2) for (x in 0..3) add(x to y)
    for (y in 1..2) for (x in 5..8) add(x to y)
    for (y in 6..7) for (x in 0..3) add(x to y)
    for (y in 6..7) for (x in 5..8) add(x to y)
    add(4 to 5)
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
        title = { Text(shop.title, fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.fillMaxWidth().height(420.dp).verticalScroll(rememberScrollState())) {
                Image(
                    painter = painterResource(shopArtResource(shop.kind)),
                    contentDescription = shop.title,
                    modifier = Modifier.fillMaxWidth().height(145.dp),
                    contentScale = ContentScale.Fit
                )
                Text("${shop.subtitle} • Development price: 1 coin", fontSize = 11.sp)
                Text("Coins: $coins", color = Color(0xFF8A6500), fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
                if (shop.kind == ShopKind.TAVERN && highestDungeonFloor < 30) {
                    Text(
                        "Hired help unlocks at Dungeon Floor 30. Maximum: 2 helpers. Helper color is random and stats are 3d6.",
                        color = Color(0xFF9A5B38),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                offers.forEach { offer ->
                    val helperBlocked = offer.helperRole != null && (highestDungeonFloor < 30 || helperCount >= 2)
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1E3C4))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(offer.name, color = Color(0xFF17100B), fontWeight = FontWeight.Black)
                                Text(offer.description, color = Color(0xFF58493B), fontSize = 10.sp)
                            }
                            Button(onClick = { onBuy(offer) }, enabled = coins >= 1 && !helperBlocked) { Text("1 coin") }
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

private fun townShops() = listOf(
    TownShop(ShopKind.ITEM_SHOP, "ITEM SHOP", "Items", 2, 3),
    TownShop(ShopKind.BLACKSMITH, "BLACKSMITH", "Weapons & Armor", 6, 3),
    TownShop(ShopKind.APOTHECARY, "APOTHECARY", "Healing & Health Potions", 2, 8),
    TownShop(ShopKind.TAVERN, "TAVERN", "Hired Help", 6, 8)
)

private fun shopOffers(kind: ShopKind): List<ShopOffer> = when (kind) {
    ShopKind.BLACKSMITH -> listOf(
        ShopOffer("dagger", "Dagger d2", "Always attacks twice; may strike three times.", "dagger1"),
        ShopOffer("sword", "Sword d4", "Straight blade; chance for a second attack.", "sword"),
        ShopOffer("greatsword", "Two-Handed Sword d6", "Two-handed straight blade.", "greatsword"),
        ShopOffer("axe", "Axe d6", "One attack with a 30% bleed chance.", "axe"),
        ShopOffer("great_axe", "Two-Handed Axe d8", "Heavy two-handed axe.", "great_axe"),
        ShopOffer("mace", "Blunt d8", "Ignores shields and 2 AC; slow weapon.", "mace"),
        ShopOffer("great_mace", "Two-Handed Blunt d10", "Heavy blunt weapon; slow but powerful.", "great_mace"),
        ShopOffer("shield", "Shield", "+1 AC and -1 movement.", "shield"),
        ShopOffer("leather", "Leather Armor", "Light chest armor, AC 1.", "leather"),
        ShopOffer("chain", "Chain Armor", "Medium chest armor, AC 3.", "chain"),
        ShopOffer("plate", "Plate Armor", "Heavy chest armor, AC 5.", "plate")
    )
    ShopKind.ITEM_SHOP -> listOf(
        ShopOffer("backpack", "Backpack", "Adds 4 carry slots.", "backpack"),
        ShopOffer("scroll_return", "Scroll of Return", "Opens a return portal from the dungeon to town.", "scroll_return")
    )
    ShopKind.APOTHECARY -> listOf(
        ShopOffer("healing_potion", "Healing Potion", "Restorative potion for development testing.", "healing_potion"),
        ShopOffer("health_potion", "Health Potion", "Health-restoring potion for development testing.", "health_potion")
    )
    ShopKind.TAVERN -> listOf(
        ShopOffer("fighter_helper", "Fighter Helper", "Random frog color and 3d6 stats when hired.", helperRole = "Fighter Helper"),
        ShopOffer("scout_helper", "Scout Helper", "Random frog color and 3d6 stats when hired.", helperRole = "Scout Helper"),
        ShopOffer("guard_helper", "Guard Helper", "Random frog color and 3d6 stats when hired.", helperRole = "Guard Helper")
    )
}
