package com.grimforsaken.dungeondicefrogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

enum class ElementType(val displayName: String) {
    FIRE("Fire"),
    ICE("Ice"),
    LIGHTNING("Lightning"),
    POISON("Poison")
}

enum class FrogColor(val displayName: String, val immunity: ElementType) {
    RED("Red", ElementType.FIRE),
    BLUE("Blue", ElementType.ICE),
    YELLOW("Yellow", ElementType.LIGHTNING),
    GREEN("Green", ElementType.POISON)
}

data class HeroCharacter(
    val color: FrogColor,
    val stats: HeroStats
)

fun frogUiColor(color: FrogColor): Color = when (color) {
    FrogColor.RED -> Color(0xFFE53935)
    FrogColor.BLUE -> Color(0xFF1976D2)
    FrogColor.YELLOW -> Color(0xFFFFD600)
    FrogColor.GREEN -> Color(0xFF2E7D32)
}

fun FrogColor.isImmuneTo(element: ElementType): Boolean = immunity == element

@Composable
fun CharacterCreationScreen(onCharacterCreated: (HeroCharacter) -> Unit) {
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var strength by rememberSaveable { mutableStateOf<Int?>(null) }
    var dexterity by rememberSaveable { mutableStateOf<Int?>(null) }
    var constitution by rememberSaveable { mutableStateOf<Int?>(null) }

    val selected = selectedName?.let { FrogColor.valueOf(it) }
    val rolled = if (strength != null && dexterity != null && constitution != null) {
        HeroStats(strength!!, dexterity!!, constitution!!)
    } else null

    Column(
        Modifier.fillMaxSize().background(Color(0xFF17100B)).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("DUNGEON DICE FROGS", color = Color(0xFFFFC62D), fontWeight = FontWeight.Black, fontSize = 24.sp)
        Text("CREATE A NEW FROG", color = Color(0xFFFFE9B4), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            "Choose a color. Your frog is immune to the matching element.",
            color = Color(0xFFD8C9AA),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FrogColorChoice(FrogColor.RED, selected == FrogColor.RED, Modifier.weight(1f)) {
                selectedName = FrogColor.RED.name; strength = null; dexterity = null; constitution = null
            }
            FrogColorChoice(FrogColor.BLUE, selected == FrogColor.BLUE, Modifier.weight(1f)) {
                selectedName = FrogColor.BLUE.name; strength = null; dexterity = null; constitution = null
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FrogColorChoice(FrogColor.YELLOW, selected == FrogColor.YELLOW, Modifier.weight(1f)) {
                selectedName = FrogColor.YELLOW.name; strength = null; dexterity = null; constitution = null
            }
            FrogColorChoice(FrogColor.GREEN, selected == FrogColor.GREEN, Modifier.weight(1f)) {
                selectedName = FrogColor.GREEN.name; strength = null; dexterity = null; constitution = null
            }
        }

        if (selected != null) {
            Text(
                "${selected.displayName} Frog • IMMUNE TO ${selected.immunity.displayName.uppercase()}",
                color = frogUiColor(selected),
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )
            Button(onClick = {
                strength = rollCharacter3d6()
                dexterity = rollCharacter3d6()
                constitution = rollCharacter3d6()
            }) {
                Text(if (rolled == null) "ROLL 3D6 FOR EACH STAT" else "REROLL ALL 3D6 STATS")
            }
        }

        if (rolled != null && selected != null) {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatRollCard("STR", rolled.strength)
                StatRollCard("DEX", rolled.dexterity)
                StatRollCard("CON", rolled.constitution)
            }
            Text("Each stat is the total of 3d6.", color = Color(0xFFD8C9AA), fontSize = 11.sp, modifier = Modifier.padding(8.dp))
            Button(onClick = { onCharacterCreated(HeroCharacter(selected, rolled)) }) {
                Text("BEGIN ADVENTURE")
            }
        }
    }
}

@Composable
private fun FrogColorChoice(color: FrogColor, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val uiColor = frogUiColor(color)
    Card(
        modifier = modifier.height(128.dp).clickable(onClick = onClick).border(
            if (selected) 4.dp else 1.dp,
            if (selected) Color(0xFFFFC62D) else Color(0xFF685746),
            RoundedCornerShape(18.dp)
        ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1C14)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(54.dp).background(uiColor, CircleShape), contentAlignment = Alignment.Center) {
                Text("🐸", fontSize = 35.sp)
            }
            Text(color.displayName, color = Color(0xFFFFE9B4), fontWeight = FontWeight.Black)
            Text("${color.immunity.displayName} immune", color = uiColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatRollCard(label: String, value: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF432719))) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color(0xFFFFE9B4), fontSize = 11.sp)
            Text(value.toString(), color = Color(0xFFFFC62D), fontWeight = FontWeight.Black, fontSize = 25.sp)
            Text("3d6", color = Color(0xFFB9A78B), fontSize = 9.sp)
        }
    }
}

private fun rollCharacter3d6(): Int = (1..3).sumOf { Random.nextInt(1, 7) }
