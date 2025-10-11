package com.jigeumopen.jigeum.common.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

fun LocalDate.toDatabaseDayOfWeek(): Int =
    this.dayOfWeek.value % 7

fun LocalTime.isBusinessHours(): Boolean =
    this in LocalTime.of(6, 0)..LocalTime.of(23, 0)

fun String.toSafeDouble(): Double? =
    this.toDoubleOrNull()

fun <T> List<T>.batch(size: Int): List<List<T>> =
    this.chunked(size)
