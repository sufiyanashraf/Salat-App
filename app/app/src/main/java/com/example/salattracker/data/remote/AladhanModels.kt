package com.example.salattracker.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class AladhanResponse(
    val code: Int,
    val data: AladhanData
)

@Serializable
data class AladhanMonthResponse(
    val code: Int,
    val data: List<AladhanData>
)

@Serializable
data class AladhanData(
    val timings: AladhanTimings,
    val date: AladhanDate
)

@Serializable
data class AladhanTimings(
    val Fajr: String,
    val Dhuhr: String,
    val Asr: String,
    val Maghrib: String,
    val Isha: String
)

@Serializable
data class AladhanDate(
    val readable: String,
    val gregorian: GregorianDate
)

@Serializable
data class GregorianDate(
    val date: String // "DD-MM-YYYY" format
)
