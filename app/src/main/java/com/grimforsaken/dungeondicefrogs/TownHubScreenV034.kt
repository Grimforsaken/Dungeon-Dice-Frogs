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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class V034Shop(val kind: ShopKind, val title: String, val subtitle: String, val doorX: Int, val doorY: Int)

private val V034TownGold = Color(0xFFFFC62D)
private val V034TownCream = Color(0xFFFFE9B4)
private val V034TownSky = Color(0xFF274F55)

@Composable
fun TownHubScreenV034(
    stats: HeroStats,
    inventory: MutableList<String>,
    coins: Int,
    onCoinsChange: (Int) -> Unit,
    helpers: MutableList<HiredHelper>,
    highestDungeonFloor: Int,
    onNotice: (String) -> Unit,
    frogColor: FrogColor,
    onEnterDungeon: () -> Unit
) {
    val shops = remember { v034TownShops() }
    val blocked = remember { v034BlockedTiles() }
    val scope = rememberCoroutineScope()
    var playerX by rememberSaveable { mutableStateOf(4) }
    var playerY by rememberSaveable { mutableStateOf(9) }
    var openShopKind by rememberSaveable { mutableStateOf<String?>(null) }
    var walking by remember { mutableStateOf(false) }

    val standingShop = shops.firstOrNull { it.doorX == playerX && it.doorY == playerY }
    val openShop = openShopKind?.let { key -> shops.firstOrNull { it.kind.name == key } }

    fun walkable(x: Int, y: Int) = x in 0..8 && y in 0..9 && (x to y) !in blocked

    fun step(nx: Int, ny: Int) {
        if (!walking && walkable(nx, ny) && kotlin.math.abs(nx - playerX) + kotlin.math.abs(ny - playerY) == 1) {
            playerX = nx
            playerY = ny
        }
    }

    fun pathTo(targetX: Int, targetY: Int): List<Pair<Int, Int>> {
        if (!walkable(targetX, targetY)) return emptyList()
        val start = playerX to playerY
        val goal = targetX to targetY
        if (start == goal) return emptyList()
        val queue = ArrayDeque<Pair<Int, Int>>()
        val previous = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>?>()
        queue.add(start)
        previous[start] = null
        val dirs = listOf(0 to -1, 1 to 0, 0 to 1, -1 to 0)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == goal) break
            for ((dx, dy) in dirs) {
                val next = current.first + dx to current.second + dy
                if (walkable(next.first, next.second) && next !in previous) {
                    previous[next] = current
                    queue.add(next)
                }
            }
        }
        if (goal !in previous) return emptyList()
        val reversed = mutableListOf<Pair<Int, Int>>()
        var cursor: Pair<Int, Int>? = goal
        while (cursor != null && cursor != start) {
            reversed += cursor
            cursor = previous[cursor]
        }
        return reversed.asReversed()
    }

    fun autoWalk(targetX: Int, targetY: Int) {
        if (walking) return
        val path = pathTo(targetX, targetY)
        if (path.isEmpty()) return
        walking = true
        scope.launch {
            for ((x, y) in path) {
                playerX = x
                playerY = y
                delay(75)
            }
            walking = false
        }
    }

    Column(Modifier.fillMaxSize().background(V034TownSky).padding(bottom = 82.dp)) {
        Column(Modifier.fillMaxWidth().background(Color(0xEE24170F)).padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("DUNGEON DICE FROGS", color = V034TownGold, fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text("Coins $coins", color = V034TownCream, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Text("Town • tap any reachable square to walk there • no turns", color = V034TownCream, fontSize = 11.sp)
        }

        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val mapSize = maxWidth
            val tileWidth = mapSize / 9f
            val tileHeight = mapSize / 10f
            val doorTiles = shops.map { it.doorX to it.doorY }.toSet()

            Image(painterResource(R.drawable.town_map), "Town map", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            V034TownArt(R.drawable.town_item_shop, "Item Shop", Modifier.offset(x = mapSize * 0.045f, y = mapSize * 0.105f).size(mapSize * 0.34f))
            V034TownArt(R.drawable.town_blacksmith, "Blacksmith", Modifier.offset(x = mapSize * 0.615f, y = mapSize * 0.105f).size(mapSize * 0.34f))
            V034TownArt(R.drawable.town_apothecary, "Apothecary", Modifier.offset(x = mapSize * 0.055f, y = mapSize * 0.575f).size(mapSize * 0.31f))
            V034TownArt(R.drawable.town_tavern, "Tavern", Modifier.offset(x = mapSize * 0.645f, y = mapSize * 0.585f).size(mapSize * 0.30f))
            V034TownArt(R.drawable.town_fountain, "Frog fountain", Modifier.offset(x = mapSize * 0.39f, y = mapSize * 0.385f).size(mapSize * 0.22f))

            for (y in 0..9) for (x in 0..8) {
                val isBlocked = (x to y) in blocked
                val isDoor = (x to y) in doorTiles
                Box(
                    Modifier.offset(x = tileWidth * x.toFloat(), y = tileHeight * y.toFloat())
                        .size(tileWidth, tileHeight)
                        .background(if (isDoor) Color(0x24FFD54F) else Color.Transparent)
                        .border(if (isDoor) 1.dp else 0.25.dp, if (isDoor) Color(0xAAFFD54F) else Color(0x18FFFFFF))
                        .clickable(enabled = !isBlocked && !walking) { autoWalk(x, y) }
                )
            }

            Image(
                painter = painterResource(frogArtResource(frogColor)),
                contentDescription = "${frogColor.displayName} main frog",
                modifier = Modifier.offset(x = tileWidth * playerX.toFloat() + tileWidth * 0.05f, y = tileHeight * playerY.toFloat() - tileHeight * 0.12f)
                    .size(tileWidth * 0.90f, tileHeight * 1.10f),
                contentScale = ContentScale.Fit
            )

            standingShop?.let { shop ->
                Button(onClick = { openShopKind = shop.kind.name }, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp)) {
                    Text("Enter ${shop.title}")
                }
            }
            if (standingShop == null && playerX == 4 && playerY == 0) {
                Button(onClick = onEnterDungeon, modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp)) { Text("Enter Dungeon") }
            }
        }

        V034TownMovePad(
            enabled = !walking,
            onNorth = { step(playerX, playerY - 1) },
            onSouth = { step(playerX, playerY + 1) },
            onWest = { step(playerX - 1, playerY) },
            onEast = { step(playerX + 1, playerY) }
        )
    }

    openShop?.let { shop ->
        V034ShopDialog(
            shop = shop,
            coins = coins,
            highestDungeonFloor = highestDungeonFloor,
            helperCount = helpers.size,
            onDismiss = { openShopKind = null },
            onBuy = { offer ->
                if (coins < 1) { onNotice("You need 1 coin."); return@V034ShopDialog }
                if (offer.helperRole != null) {
                    if (highestDungeonFloor < 30) { onNotice("Hired help unlocks after reaching Dungeon Floor 30."); return@V034ShopDialog }
                    if (helpers.size >= 2) { onNotice("You can hire a maximum of two helpers."); return@V034ShopDialog }
                    val helperColor = randomFrogColor()
                    val rolled = rollCharacterStats()
                    helpers += HiredHelper(offer.helperRole, helperColor, rolled.strength, rolled.dexterity, rolled.constitution)
                    onCoinsChange(coins - 1)
                    onNotice("Hired a ${helperColor.displayName} ${offer.helperRole}.")
                } else {
                    val id = offer.inventoryId ?: return@V034ShopDialog
                    val capacity = 6 + v034ThresholdBonus(stats.strength) + if (inventory.contains("backpack")) 4 else 0
                    if (inventory.size >= capacity) { onNotice("Inventory is full ($capacity slots)."); return@V034ShopDialog }
                    inventory += id
                    onCoinsChange(coins - 1)
                    onNotice("Purchased ${offer.name} for 1 coin.")
                }
            }
        )
    }
}

@Composable
private fun V034TownMovePad(enabled: Boolean, onNorth: () -> Unit, onSouth: () -> Unit, onWest: () -> Unit, onEast: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = onNorth, enabled = enabled, modifier = Modifier.size(width = 104.dp, height = 50.dp)) { Text("↑", fontSize = 22.sp) }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onWest, enabled = enabled, modifier = Modifier.size(width = 104.dp, height = 50.dp)) { Text("←", fontSize = 22.sp) }
            Text(if (enabled) "MOVE" else "WALKING", color = V034TownCream, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Button(onClick = onEast, enabled = enabled, modifier = Modifier.size(width = 104.dp, height = 50.dp)) { Text("→", fontSize = 22.sp) }
        }
        Button(onClick = onSouth, enabled = enabled, modifier = Modifier.size(width = 104.dp, height = 50.dp)) { Text("↓", fontSize = 22.sp) }
    }
}

@Composable
private fun V034TownArt(drawable: Int, description: String, modifier: Modifier) {
    Image(painterResource(drawable), description, modifier, contentScale = ContentScale.Fit)
}

private fun v034BlockedTiles(): Set<Pair<Int, Int>> = buildSet {
    for (y in 1..2) for (x in 0..3) add(x to y)
    for (y in 1..2) for (x in 5..8) add(x to y)
    for (y in 6..7) for (x in 0..3) add(x to y)
    for (y in 6..7) for (x in 5..8) add(x to y)
    add(4 to 5)
}

private fun v034TownShops() = listOf(
    V034Shop(ShopKind.ITEM_SHOP, "ITEM SHOP", "Items", 2, 3),
    V034Shop(ShopKind.BLACKSMITH, "BLACKSMITH", "Weapons & Armor", 6, 3),
    V034Shop(ShopKind.APOTHECARY, "APOTHECARY", "Healing & Health Potions", 2, 8),
    V034Shop(ShopKind.TAVERN, "TAVERN", "Hired Help", 6, 8)
)

private fun v034ThresholdBonus(stat: Int) = when { stat >= 18 -> 4; stat >= 16 -> 3; stat >= 14 -> 2; stat >= 12 -> 1; else -> 0 }

private fun v034Offers(kind: ShopKind): List<ShopOffer> = when (kind) {
    ShopKind.BLACKSMITH -> listOf(
        ShopOffer("dagger", "Dagger d2", "Always attacks twice; may attack three times.", "dagger1"),
        ShopOffer("sword", "Sword d4", "Straight blade; chance for a second attack.", "sword"),
        ShopOffer("greatsword", "Two-Handed Sword d6", "Heavy two-handed straight blade.", "greatsword"),
        ShopOffer("axe", "Axe d6", "One attack; 30% bleed chance.", "axe"),
        ShopOffer("great_axe", "Two-Handed Axe d8", "Heavy two-handed axe.", "great_axe"),
        ShopOffer("mace", "Blunt d8", "Ignores shields and 2 AC.", "mace"),
        ShopOffer("great_mace", "Two-Handed Blunt d10", "Heavy blunt weapon.", "great_mace"),
        ShopOffer("shield", "Shield", "+1 AC and -1 movement.", "shield"),
        ShopOffer("leather", "Leather Armor", "Light chest armor, AC 1.", "leather"),
        ShopOffer("chain", "Chain Armor", "Medium chest armor, AC 3.", "chain"),
        ShopOffer("plate", "Plate Armor", "Heavy chest armor, AC 5.", "plate")
    )
    ShopKind.ITEM_SHOP -> listOf(
        ShopOffer("backpack", "Backpack", "Adds 4 carry slots.", "backpack"),
        ShopOffer("scroll_return", "Scroll of Return", "Dungeon return portal item.", "scroll_return")
    )
    ShopKind.APOTHECARY -> listOf(
        ShopOffer("healing_potion", "Healing Potion", "Restorative potion.", "healing_potion"),
        ShopOffer("health_potion", "Health Potion", "Health-restoring potion.", "health_potion")
    )
    ShopKind.TAVERN -> listOf(
        ShopOffer("fighter_helper", "Fighter Helper", "Random frog color and 3d6 stats.", helperRole = "Fighter Helper"),
        ShopOffer("scout_helper", "Scout Helper", "Random frog color and 3d6 stats.", helperRole = "Scout Helper"),
        ShopOffer("guard_helper", "Guard Helper", "Random frog color and 3d6 stats.", helperRole = "Guard Helper")
    )
}

@Composable
private fun V034ShopDialog(shop: V034Shop, coins: Int, highestDungeonFloor: Int, helperCount: Int, onDismiss: () -> Unit, onBuy: (ShopOffer) -> Unit) {
    val offers = remember(shop.kind) { v034Offers(shop.kind) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(shop.title, fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.fillMaxWidth().height(430.dp).verticalScroll(rememberScrollState())) {
                Image(painterResource(shopArtResource(shop.kind)), shop.title, Modifier.fillMaxWidth().height(155.dp), contentScale = ContentScale.Fit)
                Text("${shop.subtitle} • Development price: 1 coin", fontSize = 11.sp)
                Text("Coins: $coins", color = Color(0xFF8A6500), fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
                offers.forEach { offer ->
                    val blocked = offer.helperRole != null && (highestDungeonFloor < 30 || helperCount >= 2)
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF1E3C4))) {
                        Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(offer.name, color = Color(0xFF17100B), fontWeight = FontWeight.Black)
                                Text(offer.description, color = Color(0xFF58493B), fontSize = 10.sp)
                            }
                            Button(onClick = { onBuy(offer) }, enabled = coins >= 1 && !blocked) { Text("1 coin") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
