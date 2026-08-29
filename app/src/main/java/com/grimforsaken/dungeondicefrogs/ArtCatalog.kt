package com.grimforsaken.dungeondicefrogs

import androidx.annotation.DrawableRes

@DrawableRes
fun frogArtResource(color: FrogColor): Int = when (color) {
    FrogColor.GREEN -> R.drawable.frog_green
    FrogColor.BLUE -> R.drawable.frog_blue
    FrogColor.YELLOW -> R.drawable.frog_yellow
    FrogColor.RED -> R.drawable.frog_red
}

@DrawableRes
fun baseBugArtResource(species: String): Int = when (species) {
    "Fly" -> R.drawable.enemy_fly
    "Mosquito" -> R.drawable.enemy_mosquito
    "Butterfly" -> R.drawable.enemy_butterfly
    "Bee" -> R.drawable.enemy_bee
    "Dragonfly" -> R.drawable.enemy_dragonfly
    "Poison Fly" -> R.drawable.enemy_poison_fly
    "Firefly" -> R.drawable.enemy_firefly
    else -> R.drawable.enemy_fly
}

@DrawableRes
fun shopArtResource(kind: ShopKind): Int = when (kind) {
    ShopKind.BLACKSMITH -> R.drawable.town_blacksmith
    ShopKind.ITEM_SHOP -> R.drawable.town_item_shop
    ShopKind.APOTHECARY -> R.drawable.town_apothecary
    ShopKind.TAVERN -> R.drawable.town_tavern
}
