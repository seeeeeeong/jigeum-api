package com.jigeumopen.jigeum.cafe.entity

enum class DayOfWeek(val value: Int, val day: String) {
    SUNDAY(0, "일요일"),
    MONDAY(1, "월요일"),
    TUESDAY(2, "화요일"),
    WEDNESDAY(3, "수요일"),
    THURSDAY(4, "목요일"),
    FRIDAY(5, "금요일"),
    SATURDAY(6, "토요일");

    companion object {
        fun fromValue(value: Int): DayOfWeek {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Invalid day of week: $value")
        }
    }
}
