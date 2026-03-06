package com.jackob.dvz.setup

enum class FormType(val key: String) {
    SETUP_FORM("setup:"),
    DELETE_MAP_FORM("delete:");

    companion object {
        private val map = entries.associateBy(FormType::key)

        fun fromKey(key: String): FormType? = map[key]
    }
}