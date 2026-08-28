package com.grimforsaken.dungeondicefrogs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
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

private val Dark = Color(0xFF17100B)
private val Brown = Color(0xFF432719)
private val Gold = Color(0xFFFFC62D)
private val Cream = Color(0xFFFFE9B4)

@Composable
fun DungeonDiceFrogsApp() {
    MaterialTheme {
        var frogColorName by rememberSaveable { mutableStateOf<String?>(null) }
        var strength by rememberSaveable { mutableStateOf(0) }
        var dexterity by rememberSaveable { mutableStateOf(0) }
        var constitution by rememberSaveable { mutableStateOf(0) }
        var xp by rememberSaveable { mutableStateOf(0) }
        var unspentStatPoints by rememberSaveable { mutableStateOf(0) }
        var screenName by rememberSaveable { mutableStateOf(Screen.TOWN.name) }
        var coins by rememberSaveable { mutableStateOf(20) }
        var highestDungeonFloor by rememberSaveable { mutableStateOf(1) }
        var notice by remember { mutableStateOf("") }

        val inventory = remember {
            mutableStateListOf(
                "backpack", "dagger1", "dagger2", "sword", "greatsword",
                "axe", "great_axe", "mace", "shield", "leather", "chain"
            )
        }
        val equipped = remember { mutableStateMapOf<SlotKey, String>() }
        val helpers = remember { mutableStateListOf<HiredHelper>() }
        val gear = remember { gearCatalog() }

        val color = frogColorName?.let { runCatching { FrogColor.valueOf(it) }.getOrNull() }
        val hasCharacter = color != null && strength > 0 && dexterity > 0 && constitution > 0
        val stats = HeroStats(strength, dexterity, constitution)
        val level = levelForXp(xp)

        Surface(Modifier.fillMaxSize(), color = Dark) {
            if (!hasCharacter) {
                CharacterCreationScreen { chosenColor, rolledStats ->
                    frogColorName = chosenColor.name
                    strength = rolledStats.strength
                    dexterity = rolledStats.dexterity
                    constitution = rolledStats.constitution
                    xp = 0
                    unspentStatPoints = 0
                    screenName = Screen.TOWN.name
                    notice = "${chosenColor.displayName} frog created. Immune to ${elementalImmunityText(chosenColor)}."
                }
            } else {
                val currentColor = color!!
                val screen = Screen.valueOf(screenName)
                Box(Modifier.fillMaxSize()) {
                    when (screen) {
                        Screen.TOWN -> TownHubScreen(
                            stats = stats,
                            inventory = inventory,
                            coins = coins,
                            onCoinsChange = { coins = it },
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
                                }
                            },
                            onNotice = { notice = it }
                        )
                        Screen.DUNGEON -> DungeonScreen(
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
                                val playerPoints = statPointsEarnedForLevelIncrease(oldPlayerLevel, newPlayerLevel)
                                unspentStatPoints += playerPoints

                                helpers.forEach { helper ->
                                    val oldHelperLevel = levelForXp(helper.xp)
                                    helper.xp += equalShare
                                    val newHelperLevel = levelForXp(helper.xp)
                                    helper.level = newHelperLevel
                                    helper.unspentStatPoints += statPointsEarnedForLevelIncrease(oldHelperLevel, newHelperLevel)
                                }

                                val levelMessage = if (playerPoints > 0) {
                                    " Level $newPlayerLevel reached: +$playerPoints stat points."
                                } else ""
                                val helperMessage = if (helpers.isNotEmpty()) {
                                    " Helpers each receive $equalShare XP."
                                } else ""
                                notice = "Recovered Tier $lootTier loot: $award XP total. Player receives $playerGain XP.$helperMessage$levelMessage"
                            },
                            onAdvanceFloor = {
                                highestDungeonFloor += 1
                                notice = "Highest dungeon floor is now $highestDungeonFloor."
                            },
                            onCharacterDeath = {
                                frogColorName = null
                                strength = 0
                                dexterity = 0
                                constitution = 0
                                xp = 0
                                unspentStatPoints = 0
                                equipped.clear()
                                helpers.clear()
                            }
                        )
                    }
                    BottomNav(screen, Modifier.align(Alignment.BottomCenter)) { screenName = it.name }
                    if (notice.isNotBlank()) {
                        Card(
                            modifier = Modifier.align(Alignment.TopCenter).padding(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xEE27170F))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(notice, color = Cream, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp).weight(1f))
                                TextButton(onClick = { notice = "" }) { Text("×", color = Gold, fontSize = 22.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterCreationScreen(onCreated: (FrogColor, HeroStats) -> Unit) {
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var rolledStrength by rememberSaveable { mutableStateOf(0) }
    var rolledDexterity by rememberSaveable { mutableStateOf(0) }
    var rolledConstitution by rememberSaveable { mutableStateOf(0) }
    val selected = selectedName?.let { runCatching { FrogColor.valueOf(it) }.getOrNull() }
    val rolled = rolledStrength > 0 && rolledDexterity > 0 && rolledConstitution > 0

    Column(
        Modifier.fillMaxSize().background(Dark).padding(20.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("DUNGEON DICE FROGS", color = Gold, fontWeight = FontWeight.Black, fontSize = 27.sp, modifier = Modifier.padding(top = 24.dp))
        Text("Create Your Frog", color = Cream, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 18.dp))
        Text("Choose a color first. Your color makes you completely immune to its matching element.", color = Cream, textAlign = TextAlign.Center, fontSize = 13.sp)

        FrogColor.values().forEach { frogColor ->
            val selectedThis = frogColor == selected
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                colors = CardDefaults.cardColors(containerColor = if (selectedThis) Color(0xFF6A4A1B) else Brown),
                onClick = {
                    selectedName = frogColor.name
                    rolledStrength = 0
                    rolledDexterity = 0
                    rolledConstitution = 0
                }
            ) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(frogColorEmoji(frogColor), fontSize = 32.sp, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text("${frogColor.displayName} Frog", color = Gold, fontWeight = FontWeight.Black, fontSize = 17.sp)
                        Text("Immune: ${elementalImmunityText(frogColor)}", color = Cream, fontSize = 11.sp)
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

        if (rolled && selected != null) {
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatRollCard("STR", rolledStrength)
                StatRollCard("DEX", rolledDexterity)
                StatRollCard("CON", rolledConstitution)
            }
            Text("These are this frog's starting stats.", color = Cream, fontSize = 11.sp, modifier = Modifier.padding(bottom = 10.dp))
            Button(onClick = { onCreated(selected, HeroStats(rolledStrength, rolledDexterity, rolledConstitution)) }) {
                Text("Begin Adventure")
            }
        }
    }
}

@Composable
private fun StatRollCard(label: String, value: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = Brown)) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Cream, fontSize = 11.sp)
            Text(value.toString(), color = Gold, fontWeight = FontWeight.Black, fontSize = 25.sp)
            Text("3d6", color = Color.Gray, fontSize = 9.sp)
        }
    }
}

@Composable
fun BottomNav(current: Screen, modifier: Modifier, onNavigate: (Screen) -> Unit) {
    NavigationBar(modifier = modifier, containerColor = Color(0xFF2C1A11)) {
        listOf(Screen.TOWN to "Town", Screen.INVENTORY to "Inventory", Screen.HERO to "Hero", Screen.DUNGEON to "Dungeon").forEach { (screen, label) ->
            NavigationBarItem(
                selected = current == screen,
                onClick = { onNavigate(screen) },
                icon = { Text(when (screen) { Screen.TOWN -> "🏘"; Screen.INVENTORY -> "🎒"; Screen.HERO -> "🐸"; Screen.DUNGEON -> "🎲" }, fontSize = 20.sp) },
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
        val names = inventory.map { gearCatalog()[it]?.let { gear -> "${gear.icon}\n${gear.name}" } ?: it.replace('_', ' ').uppercase() }
        val rows = (capacity + 2) / 3
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                repeat(3) { column ->
                    val index = row * 3 + column
                    InventorySlot(index + 1, names.getOrNull(index))
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
    frogColor: FrogColor,
    level: Int,
    xp: Int,
    unspentStatPoints: Int,
    inventory: MutableList<String>,
    catalog: Map<String, Gear>,
    equipped: MutableMap<SlotKey, String>,
    onSpendStat: (String) -> Unit,
    onNotice: (String) -> Unit
) {
    val bounds = remember { mutableStateMapOf<SlotKey, Rect>() }
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    val chest = equipped[SlotKey.CHEST]?.let(catalog::get)
    val hand1 = equipped[SlotKey.HAND1]?.let(catalog::get)
    val hand2 = equipped[SlotKey.HAND2]?.let(catalog::get)
    val dualDaggers = hand1?.dagger == true && hand2?.dagger == true
    val shield = hand1?.type == GearType.SHIELD || hand2?.type == GearType.SHIELD
    val ac = (chest?.armorClass ?: 0) + (if (shield) 1 else 0) - (if (dualDaggers) 2 else 0)
    val move = 5 + thresholdBonus(stats.dexterity) - (if (shield) 1 else 0) - (if (chest?.armorWeight == "heavy" && stats.strength < 12) 1 else 0)

    Box(Modifier.fillMaxSize().background(Dark).padding(bottom = 82.dp)) {
        Column(Modifier.fillMaxSize()) {
            Header(
                "Hero Equipment",
                "${frogColor.displayName} Frog • Level $level • XP $xp/${xpRequiredForNextLevel(xp)} • Immune: ${frogColor.immuneElement.name.lowercase()}"
            )
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Chip("STR", stats.strength)
                Chip("DEX", stats.dexterity)
                Chip("CON", stats.constitution)
            }
            if (unspentStatPoints > 0) {
                Card(Modifier.fillMaxWidth().padding(horizontal = 10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF5A3A16))) {
                    Column(Modifier.fillMaxWidth().padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$unspentStatPoints STAT POINTS AVAILABLE", color = Gold, fontWeight = FontWeight.Black)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Button(onClick = { onSpendStat("STR") }) { Text("+ STR") }
                            Button(onClick = { onSpendStat("DEX") }) { Text("+ DEX") }
                            Button(onClick = { onSpendStat("CON") }) { Text("+ CON") }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Chip("AC", ac)
                Chip("MOVE", move)
                Chip("CARRY", 6 + thresholdBonus(stats.strength) + (if (inventory.contains("backpack")) 4 else 0))
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Text(frogColorEmoji(frogColor), fontSize = 100.sp, modifier = Modifier.align(Alignment.Center))
                EquipSlot("HELMET\nFUTURE", SlotKey.HELMET, null, true, false, Modifier.align(Alignment.TopCenter)) { key, rect -> bounds[key] = rect }
                EquipSlot("CHEST", SlotKey.CHEST, chest, false, false, Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) { key, rect -> bounds[key] = rect }
                EquipSlot("HAND 1", SlotKey.HAND1, hand1, false, false, Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 18.dp)) { key, rect -> bounds[key] = rect }
                EquipSlot("HAND 2", SlotKey.HAND2, hand2, false, hand1?.twoHanded == true, Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 18.dp)) { key, rect -> bounds[key] = rect }
                EquipSlot("RING 1\nFUTURE", SlotKey.RING1, null, true, false, Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) { key, rect -> bounds[key] = rect }
                EquipSlot("RING 2\nFUTURE", SlotKey.RING2, null, true, false, Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 82.dp)) { key, rect -> bounds[key] = rect }
                EquipSlot("NECK\nFUTURE", SlotKey.NECKLACE, null, true, false, Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 82.dp)) { key, rect -> bounds[key] = rect }
            }
            Text("Hold and drag gear onto a slot", color = Gold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            LazyRow(Modifier.fillMaxWidth().height(100.dp).background(Color(0xFF2C1A11)).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(inventory.toList().filter { catalog.containsKey(it) }) { id ->
                    val gearItem = catalog[id] ?: return@items
                    DraggableGear(
                        gearItem,
                        onStart = { point -> dragging = id; dragPos = point },
                        onMove = { delta -> dragPos += delta },
                        onEnd = {
                            bounds.entries.firstOrNull { it.value.contains(dragPos) }?.key?.let { slot ->
                                equip(slot, id, stats, inventory, catalog, equipped, onNotice)
                            }
                            dragging = null
                        }
                    )
                }
            }
        }
        dragging?.let { id ->
            val gearItem = catalog[id] ?: return@let
            val half = with(LocalDensity.current) { 34.dp.toPx() }
            Box(
                Modifier.offset { IntOffset((dragPos.x - half).roundToInt(), (dragPos.y - half).roundToInt()) }.size(68.dp).background(Gold, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(gearItem.icon, fontSize = 32.sp)
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
private fun DraggableGear(gear: Gear, onStart: (Offset) -> Unit, onMove: (Offset) -> Unit, onEnd: () -> Unit) {
    var rect by remember { mutableStateOf(Rect.Zero) }
    Box(
        Modifier.size(86.dp).background(Color(0xFF3B281C), RoundedCornerShape(12.dp)).onGloballyPositioned { rect = it.boundsInRoot() }.pointerInput(gear.id) {
            detectDragGesturesAfterLongPress(
                onDragStart = { local -> onStart(rect.topLeft + local) },
                onDrag = { _, amount -> onMove(amount) },
                onDragEnd = onEnd,
                onDragCancel = onEnd
            )
        },
        contentAlignment = Alignment.Center
    ) {
        Text("${gear.icon}\n${gear.name}", color = Cream, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

private fun equip(
    slot: SlotKey,
    id: String,
    stats: HeroStats,
    inventory: MutableList<String>,
    catalog: Map<String, Gear>,
    equipped: MutableMap<SlotKey, String>,
    notice: (String) -> Unit
) {
    val gear = catalog[id] ?: return
    if (slot == SlotKey.HELMET || slot == SlotKey.RING1 || slot == SlotKey.RING2 || slot == SlotKey.NECKLACE) {
        notice("That slot is reserved for a future update.")
        return
    }
    if (slot == SlotKey.CHEST && gear.type != GearType.CHEST) {
        notice("Only chest armor fits there.")
        return
    }
    if (slot == SlotKey.HAND1 || slot == SlotKey.HAND2) {
        if (gear.type != GearType.WEAPON && gear.type != GearType.SHIELD) {
            notice("Only weapons and shields fit in hand slots.")
            return
        }
        val other = if (slot == SlotKey.HAND1) SlotKey.HAND2 else SlotKey.HAND1
        val otherGear = equipped[other]?.let(catalog::get)
        if (gear.twoHanded && slot == SlotKey.HAND2) {
            notice("Two-handed weapons use Hand 1 and block Hand 2.")
            return
        }
        if (otherGear?.twoHanded == true) {
            notice("Remove the two-handed weapon first.")
            return
        }
        if (gear.dagger && otherGear?.dagger == true && stats.dexterity < 12) {
            notice("DEX 12 is required for two daggers.")
            return
        }
        if (gear.type == GearType.SHIELD && otherGear?.type == GearType.SHIELD) {
            notice("Only one shield may be equipped.")
            return
        }
        if (gear.twoHanded) {
            listOf(SlotKey.HAND1, SlotKey.HAND2).forEach { hand -> equipped.remove(hand)?.let { inventory.add(it) } }
        }
    }
    equipped.remove(slot)?.let { inventory.add(it) }
    inventory.remove(id)
    equipped[slot] = id
    notice("Equipped ${gear.name}.")
}

@Composable
private fun Chip(label: String, value: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = Brown)) {
        Column(Modifier.padding(horizontal = 17.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Cream, fontSize = 9.sp)
            Text(value.toString(), color = Gold, fontWeight = FontWeight.Black, fontSize = 18.sp)
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
fun DungeonScreen(
    frogColor: FrogColor,
    level: Int,
    xp: Int,
    highestDungeonFloor: Int,
    helperCount: Int,
    onRecoverLoot: (Int) -> Unit,
    onAdvanceFloor: () -> Unit,
    onCharacterDeath: () -> Unit
) {
    val tier = tierForDungeonFloor(highestDungeonFloor)
    val difficulty = enemyDifficultyMultiplier(tier)
    val transition = nextTierEnemiesCanAppear(highestDungeonFloor)

    Column(
        Modifier.fillMaxSize().background(Dark).padding(bottom = 82.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("♜", fontSize = 72.sp, color = Color(0xFFD33B31), modifier = Modifier.padding(top = 16.dp))
        Text("TOWER DUNGEON", color = Gold, fontWeight = FontWeight.Black, fontSize = 25.sp)
        Text("Floor $highestDungeonFloor • Tier $tier • Hero Level $level", color = Cream, fontSize = 15.sp)
        Text("XP $xp / ${xpRequiredForNextLevel(xp)} • Enemy kills give 0 XP", color = Cream, fontSize = 11.sp)
        Text("${frogColor.displayName} immunity: ${elementalImmunityText(frogColor)}", color = Color(0xFFBFD8FF), fontSize = 11.sp, modifier = Modifier.padding(5.dp))

        Card(Modifier.fillMaxWidth().padding(12.dp), colors = CardDefaults.cardColors(containerColor = Brown)) {
            Column(Modifier.padding(12.dp)) {
                Text("MONSTER TIER RULES", color = Gold, fontWeight = FontWeight.Black)
                Text("Tier bands are 10 floors. Current difficulty ×${"%.3f".format(difficulty)}.", color = Cream, fontSize = 11.sp)
                Text("Each tier adds +1 STR, +1 DEX, and +1 CON to enemies.", color = Cream, fontSize = 11.sp)
                Text(
                    if (transition) "Mid-band transition active: next-tier enemies may begin appearing." else "Next-tier enemy transition begins at floor ${((highestDungeonFloor - 1) / 10) * 10 + 6}.",
                    color = Color(0xFFC9B99F),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text("TIER 1 • FEED THE FROG BUGS", color = Gold, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
                tierOneBugEnemies.forEach { enemy ->
                    val enemyStats = enemy.statsForTier(tier)
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(enemy.icon, fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(enemy.name, color = Cream, fontWeight = FontWeight.Bold)
                            enemy.elementalAttack?.let { element ->
                                Text("${element.name.lowercase().replaceFirstChar { it.uppercase() }} attack", color = Color(0xFFDBB576), fontSize = 9.sp)
                            }
                        }
                        Text("${enemyStats.strength}/${enemyStats.dexterity}/${enemyStats.constitution}", color = Gold, fontSize = 11.sp)
                    }
                }
                Text("Stats shown as STR / DEX / CON for the current tier.", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2119))) {
            Column(Modifier.padding(10.dp)) {
                Text("LOOT XP", color = Gold, fontWeight = FontWeight.Black)
                Text("Recovered Tier $tier loot is worth $tier XP. XP is split into whole equal shares among the player and $helperCount active helpers; any remainder goes to the player.", color = Cream, fontSize = 10.sp)
                Button(onClick = { onRecoverLoot(tier) }, modifier = Modifier.padding(top = 7.dp)) {
                    Text("Test Recover Tier $tier Loot (+$tier XP)")
                }
            }
        }

        Text("Development controls", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 9.dp))
        Button(onClick = onAdvanceFloor) { Text("Next Floor") }
        TextButton(onClick = onCharacterDeath) {
            Text("Test Character Death / Create New Frog", color = Color(0xFFE58C82), fontSize = 11.sp)
        }
    }
}

private fun thresholdBonus(stat: Int) = when {
    stat >= 18 -> 4
    stat >= 16 -> 3
    stat >= 14 -> 2
    stat >= 12 -> 1
    else -> 0
}

private fun gearCatalog(): Map<String, Gear> = listOf(
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
