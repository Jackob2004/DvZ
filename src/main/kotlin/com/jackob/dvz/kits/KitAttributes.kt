package com.jackob.dvz.kits

import kotlinx.serialization.Serializable

@Serializable
data class KitAttributes(val rampage: Int? = null, val rampageImmune: Boolean? = null)