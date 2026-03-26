package com.jackob.dvz.kits

import com.jackob.dvz.storage.KitDisplay

enum class KitType(val kitClass: Class<out Kit>, val team: Team, val isHero: Boolean, val displayData: KitDisplay) {
}