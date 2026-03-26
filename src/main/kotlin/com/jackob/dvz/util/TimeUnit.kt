package com.jackob.dvz.util

enum class TimeUnit {
    TICKS,
    SECONDS,
    MINUTES;

    operator fun invoke(num: Long): Long = when (this) {
        TICKS -> num
        SECONDS -> num * 20
        MINUTES -> num * 20 * 60
    }
}