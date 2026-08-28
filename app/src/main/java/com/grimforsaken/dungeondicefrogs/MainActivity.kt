package com.grimforsaken.dungeondicefrogs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DungeonDiceFrogsApp() }
    }
}

enum class Screen { TOWN, INVENTORY, HERO, DUNGEON }
enum class GearType { WEAPON, SHIELD, CHEST, BACKPACK }
enum class SlotKey { HAND1, HAND2, CHEST, HELMET, RING1, RING2, NECKLACE }

data class Gear(
    val id: String,
    val name: String,
    val icon: String,
    val type: GearType,
    val twoHanded: Boolean = false,
    val dagger: Boolean = false,
    val armorClass: Int = 0,
    val armorWeight: String? = null
)

data class HeroStats(val strength: Int = 12, val dexterity: Int = 12, val constitution: Int = 12)

private val Dark = Color(0xFF17100B)
private val Brown = Color(0xFF432719)
private val Gold = Color(0xFFFFC62D)
private val Cream = Color(0xFFFFE9B4)
private val Grass = Color(0xFF4F8F45)
private val Stone = Color(0xFF8A806F)

@Composable
fun DungeonDiceFrogsApp() {
    MaterialTheme {
        val stats = remember { HeroStats() }
        var screenName by rememberSaveable { mutableStateOf(Screen.TOWN.name) }
        val screen = Screen.valueOf(screenName)
        var notice by remember { mutableStateOf("") }
        val gear = remember { gearCatalog() }
        val inventory = remember {
            mutableStateListOf(
                "backpack", "dagger1", "dagger2", "sword", "greatsword",
                "axe", "great_axe", "mace", "shield", "leather", "chain"
            )
        }
        val equipped = remember { mutableStateMapOf<SlotKey, String>() }

        Surface(Modifier.fillMaxSize(), color = Dark) {
            Box(Modifier.fillMaxSize()) {
                when (screen) {
                    Screen.TOWN -> TownScreen { notice = it }
                    Screen.INVENTORY -> InventoryScreen(stats, inventory, equipped)
                    Screen.HERO -> EquipmentScreen(stats, inventory, gear, equipped) { notice = it }
                    Screen.DUNGEON -> DungeonScreen()
                }
                BottomNav(screen, Modifier.align(Alignment.BottomCenter)) { screenName = it.name }
                if (notice.isNotBlank()) {
                    Card(
                        modifier = Modifier.align(Alignment.TopCenter).padding(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xEE27170F))
                    ) {
                        Text(notice, color = Cream, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TownScreen(onNotice: (String) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF83BFE8)).padding(bottom = 78.dp)) {
        // Mountains and the tower dungeon in the distance.
        Box(Modifier.align(Alignment.TopCenter).padding(top = 42.dp).size(112.dp, 205.dp).background(Color(0xFF2C2933), RoundedCornerShape(18.dp))) {
            Text("♜", fontSize = 66.sp, modifier = Modifier.align(Alignment.TopCenter), color = Color(0xFFE13B2C))
            Text("TOWER\nDUNGEON", color = Cream, fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp))
        }
        Box(Modifier.align(Alignment.BottomCenter).offset(y = (-70).dp).size(310.dp).graphicsLayer { rotationZ = 45f; scaleY = 0.62f }.background(Grass, RoundedCornerShape(34.dp)).border(8.dp, Color(0xFF6D644E), RoundedCornerShape(34.dp)))
        Box(Modifier.align(Alignment.Center).offset(y = 70.dp).size(112.dp).graphicsLayer { rotationZ = 45f; scaleY = 0.62f }.background(Stone, RoundedCornerShape(14.dp)))
        Text("🐸", fontSize = 62.sp, modifier = Modifier.align(Alignment.Center).offset(y = 65.dp))

        TownBuilding("⚒", "BLACKSMITH", Alignment.CenterStart, 20, -70) { onNotice("Blacksmith: weapon services are planned for the next town update.") }
        TownBuilding("🧪", "ITEM SHOP", Alignment.CenterEnd, -20, -70) { onNotice("Item Shop: starter gear and supplies will be sold here.") }
        TownBuilding("🛏", "INN", Alignment.CenterEnd, -20, 90) { onNotice("Inn: recovery services will be added here.") }
        TownBuilding("🎯", "TRAINING", Alignment.CenterStart, 20, 95) { onNotice("Training Hall: character training will be added later.") }
        TownBuilding("🤝", "HIRE", Alignment.BottomEnd, -20, -75) { onNotice("Helpers unlock when the dungeon reaches Floor 30.") }

        Column(Modifier.align(Alignment.TopStart).padding(14.dp)) {
            Text("DUNGEON DICE FROGS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text("Town Hub", color = Dark, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TownBuilding(icon: String, label: String, alignment: Alignment, x: Int, y: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.align(alignment).offset(x.dp, y.dp).size(118.dp, 92.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF56331F)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(icon, fontSize = 31.sp)
            Text(label, color = Gold, fontWeight = FontWeight.Black, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun BottomNav(current: Screen, modifier: Modifier, onNavigate: (Screen) -> Unit) {
    NavigationBar(modifier = modifier, containerColor = Color(0xFF2C1A11)) {
        listOf(Screen.TOWN to "Town", Screen.INVENTORY to "Inventory", Screen.HERO to "Hero", Screen.DUNGEON to "Dungeon").forEach { (s, label) ->
            NavigationBarItem(
                selected = current == s,
                onClick = { onNavigate(s) },
                icon = { Text(when (s) { Screen.TOWN -> "🏘"; Screen.INVENTORY -> "🎒"; Screen.HERO -> "🐸"; Screen.DUNGEON -> "🎲" }, fontSize = 20.sp) },
                label = { Text(label, color = Cream, fontSize = 10.sp) }
            )
        }
    }
}

@Composable
fun InventoryScreen(stats: HeroStats, inventory: List<String>, equipped: Map<SlotKey, String>) {
    val hasBackpack = inventory.contains("backpack") || equipped.values.contains("backpack")
    val capacity = 6 + thresholdBonus(stats.strength) + if (hasBackpack) 4 else 0
    Column(Modifier.fillMaxSize().background(Dark).padding(bottom = 82.dp).verticalScroll(rememberScrollState())) {
        Header("Inventory", "Visible carry slots: $capacity")
        Text("6 base + ${thresholdBonus(stats.strength)} STR${if (hasBackpack) " + 4 Backpack" else ""}", color = Cream, modifier = Modifier.padding(14.dp))
        val names = inventory.map { gearCatalog()[it]?.let { g -> "${g.icon}\n${g.name}" } ?: it }
        val rows = (capacity + 2) / 3
        repeat(rows) { r ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                repeat(3) { c ->
                    val i = r * 3 + c
                    InventorySlot(i + 1, names.getOrNull(i))
                }
            }
        }
    }
}

@Composable
private fun InventorySlot(number: Int, item: String?) {
    Box(
        Modifier.padding(5.dp).size(104.dp).background(Color(0xFF2B1C14), RoundedCornerShape(14.dp)).border(1.dp, Color(0xFF7A5B36), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("$number", color = Color(0xFF806C51), fontSize = 9.sp, modifier = Modifier.align(Alignment.TopStart).padding(5.dp))
        Text(item ?: "EMPTY", color = if (item == null) Color(0xFF65594A) else Cream, textAlign = TextAlign.Center, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EquipmentScreen(
    stats: HeroStats,
    inventory: MutableList<String>,
    catalog: Map<String, Gear>,
    equipped: MutableMap<SlotKey, String>,
    onNotice: (String) -> Unit
) {
    val bounds = remember { mutableStateMapOf<SlotKey, Rect>() }
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    val chest = equipped[SlotKey.CHEST]?.let(catalog::get)
    val h1 = equipped[SlotKey.HAND1]?.let(catalog::get)
    val h2 = equipped[SlotKey.HAND2]?.let(catalog::get)
    val dualDaggers = h1?.dagger == true && h2?.dagger == true
    val shield = h1?.type == GearType.SHIELD || h2?.type == GearType.SHIELD
    val ac = (chest?.armorClass ?: 0) + if (shield) 1 else 0 - if (dualDaggers) 2 else 0
    val move = 5 + thresholdBonus(stats.dexterity) - if (shield) 1 else 0 - if (chest?.armorWeight == "heavy" && stats.strength < 12) 1 else 0

    Box(Modifier.fillMaxSize().background(Dark).padding(bottom = 82.dp)) {
        Column(Modifier.fillMaxSize()) {
            Header("Hero Equipment", "STR ${stats.strength}   DEX ${stats.dexterity}   CON ${stats.constitution}")
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Chip("AC", ac); Chip("MOVE", move); Chip("CARRY", 6 + thresholdBonus(stats.strength) + if (inventory.contains("backpack")) 4 else 0)
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Text("🐸", fontSize = 128.sp, modifier = Modifier.align(Alignment.Center))
                EquipSlot("HELMET\nFUTURE", SlotKey.HELMET, null, true, false, Modifier.align(Alignment.TopCenter)) { k, r -> bounds[k] = r }
                EquipSlot("CHEST", SlotKey.CHEST, chest, false, false, Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) { k, r -> bounds[k] = r }
                EquipSlot("HAND 1", SlotKey.HAND1, h1, false, false, Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 18.dp)) { k, r -> bounds[k] = r }
                EquipSlot("HAND 2", SlotKey.HAND2, h2, false, h1?.twoHanded == true, Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 18.dp)) { k, r -> bounds[k] = r }
                EquipSlot("RING 1\nFUTURE", SlotKey.RING1, null, true, false, Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) { k, r -> bounds[k] = r }
                EquipSlot("RING 2\nFUTURE", SlotKey.RING2, null, true, false, Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 82.dp)) { k, r -> bounds[k] = r }
                EquipSlot("NECK\nFUTURE", SlotKey.NECKLACE, null, true, false, Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 82.dp)) { k, r -> bounds[k] = r }
            }
            Text("Hold and drag gear onto a slot", color = Gold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            LazyRow(Modifier.fillMaxWidth().height(100.dp).background(Color(0xFF2C1A11)).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(inventory.toList(), key = { it }) { id ->
                    val g = catalog[id] ?: return@items
                    DraggableGear(g,
                        onStart = { p -> dragging = id; dragPos = p },
                        onMove = { d -> dragPos += d },
                        onEnd = {
                            bounds.entries.firstOrNull { it.value.contains(dragPos) }?.key?.let { slot -> equip(slot, id, stats, inventory, catalog, equipped, onNotice) }
                            dragging = null
                        })
                }
            }
        }
        dragging?.let { id ->
            val g = catalog[id] ?: return@let
            val half = with(LocalDensity.current) { 34.dp.toPx() }
            Box(Modifier.offset { IntOffset((dragPos.x - half).roundToInt(), (dragPos.y - half).roundToInt()) }.size(68.dp).background(Gold, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(g.icon, fontSize = 32.sp)
            }
        }
    }
}

@Composable
private fun EquipSlot(title: String, key: SlotKey, gear: Gear?, locked: Boolean, blocked: Boolean, modifier: Modifier, onBounds: (SlotKey, Rect) -> Unit) {
    Box(
        modifier.size(84.dp).background(Color(0xDD2B1A11), RoundedCornerShape(14.dp)).border(2.dp, if (blocked) Color.Red else Gold, RoundedCornerShape(14.dp)).onGloballyPositioned { onBounds(key, it.boundsInRoot()) },
        contentAlignment = Alignment.Center
    ) {
        if (gear != null) Text("${gear.icon}\n${gear.name}", color = Cream, textAlign = TextAlign.Center, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        else Text(title, color = if (locked) Color.Gray else Cream, textAlign = TextAlign.Center, fontSize = 9.sp)
        if (blocked) Text("✕", color = Color.Red, fontWeight = FontWeight.Black, fontSize = 58.sp)
    }
}

@Composable
private fun DraggableGear(g: Gear, onStart: (Offset) -> Unit, onMove: (Offset) -> Unit, onEnd: () -> Unit) {
    var rect by remember { mutableStateOf(Rect.Zero) }
    Box(
        Modifier.size(86.dp).background(Color(0xFF3B281C), RoundedCornerShape(12.dp)).onGloballyPositioned { rect = it.boundsInRoot() }.pointerInput(g.id) {
            detectDragGesturesAfterLongPress(
                onDragStart = { local -> onStart(rect.topLeft + local) },
                onDrag = { change, amount -> change.consume(); onMove(amount) },
                onDragEnd = onEnd,
                onDragCancel = onEnd
            )
        }, contentAlignment = Alignment.Center
    ) {
        Text("${g.icon}\n${g.name}", color = Cream, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

private fun equip(slot: SlotKey, id: String, stats: HeroStats, inventory: MutableList<String>, catalog: Map<String, Gear>, equipped: MutableMap<SlotKey, String>, notice: (String) -> Unit) {
    val g = catalog[id] ?: return
    if (slot == SlotKey.HELMET || slot == SlotKey.RING1 || slot == SlotKey.RING2 || slot == SlotKey.NECKLACE) { notice("That slot is reserved for a future update."); return }
    if (slot == SlotKey.CHEST && g.type != GearType.CHEST) { notice("Only chest armor fits there."); return }
    if (slot == SlotKey.HAND1 || slot == SlotKey.HAND2) {
        if (g.type != GearType.WEAPON && g.type != GearType.SHIELD) { notice("Only weapons and shields fit in hand slots."); return }
        val other = if (slot == SlotKey.HAND1) SlotKey.HAND2 else SlotKey.HAND1
        val otherGear = equipped[other]?.let(catalog::get)
        if (g.twoHanded && slot == SlotKey.HAND2) { notice("Two-handed weapons use Hand 1 and block Hand 2."); return }
        if (otherGear?.twoHanded == true) { notice("Remove the two-handed weapon first."); return }
        if (g.dagger && otherGear?.dagger == true && stats.dexterity < 12) { notice("DEX 12 is required for two daggers."); return }
        if (g.type == GearType.SHIELD && otherGear?.type == GearType.SHIELD) { notice("Only one shield may be equipped."); return }
        if (g.twoHanded) {
            listOf(SlotKey.HAND1, SlotKey.HAND2).forEach { s -> equipped.remove(s)?.let { inventory.add(it) } }
        }
    }
    equipped.remove(slot)?.let { inventory.add(it) }
    inventory.remove(id)
    equipped[slot] = id
    notice("Equipped ${g.name}.")
}

@Composable
private fun Chip(label: String, value: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = Brown)) {
        Column(Modifier.padding(horizontal = 17.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Cream, fontSize = 9.sp); Text(value.toString(), color = Gold, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().background(Brown).padding(15.dp)) {
        Text(title, color = Gold, fontWeight = FontWeight.Black, fontSize = 25.sp)
        Text(subtitle, color = Cream, fontSize = 12.sp)
    }
}

@Composable
fun DungeonScreen() {
    Column(Modifier.fillMaxSize().background(Dark).padding(bottom = 82.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("♜", fontSize = 100.sp, color = Color(0xFFD33B31))
        Text("TOWER DUNGEON", color = Gold, fontWeight = FontWeight.Black, fontSize = 28.sp)
        Text("Floor 1 • Tier 1", color = Cream, fontSize = 18.sp)
        Text("Enemy difficulty ×1.00", color = Color(0xFFC8B89C), modifier = Modifier.padding(8.dp))
        Text("Dungeon floor generation and combat are the next development step.", color = Cream, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
    }
}

private fun thresholdBonus(stat: Int) = when { stat >= 18 -> 4; stat >= 16 -> 3; stat >= 14 -> 2; stat >= 12 -> 1; else -> 0 }

private fun gearCatalog(): Map<String, Gear> = listOf(
    Gear("backpack", "Backpack +4", "🎒", GearType.BACKPACK),
    Gear("dagger1", "Dagger d2", "🗡", GearType.WEAPON, dagger = true),
    Gear("dagger2", "Dagger d2", "🗡", GearType.WEAPON, dagger = true),
    Gear("sword", "Sword d4", "⚔", GearType.WEAPON),
    Gear("greatsword", "2H Sword d6", "⚔", GearType.WEAPON, twoHanded = true),
    Gear("axe", "Axe d6", "🪓", GearType.WEAPON),
    Gear("great_axe", "2H Axe d8", "🪓", GearType.WEAPON, twoHanded = true),
    Gear("mace", "Blunt d8", "🔨", GearType.WEAPON),
    Gear("shield", "Shield +1 AC", "🛡", GearType.SHIELD),
    Gear("leather", "Leather AC 1", "🥋", GearType.CHEST, armorClass = 1, armorWeight = "light"),
    Gear("chain", "Chain AC 3", "⛓", GearType.CHEST, armorClass = 3, armorWeight = "medium"),
    Gear("plate", "Plate AC 5", "🦺", GearType.CHEST, armorClass = 5, armorWeight = "heavy")
).associateBy { it.id }
