import java.time.LocalTime
import java.time.temporal.ChronoUnit

fun main() {
    val fajr = LocalTime.parse("04:13")
    val sunrise = LocalTime.parse("05:30")
    val dhuhr = LocalTime.parse("13:00")
    val asr = LocalTime.parse("16:00")
    val maghrib = LocalTime.parse("19:00")
    val isha = LocalTime.parse("20:30")

    data class PrayerWindow(val name: String, val start: LocalTime, val end: LocalTime)
    val windows = listOf(
        PrayerWindow("Fajr", fajr, sunrise),
        PrayerWindow("Dhuhr", dhuhr, asr),
        PrayerWindow("Asr", asr, maghrib),
        PrayerWindow("Maghrib", maghrib, isha),
        PrayerWindow("Isha", isha, LocalTime.of(23, 59)),
    )

    fun testTime(nowStr: String) {
        val now = LocalTime.parse(nowStr)
        for (window in windows) {
            val graceEnd = window.start.plusMinutes(15)
            if (!now.isBefore(window.start) && now.isBefore(graceEnd)) {
                println("$nowStr -> GRACE: ${window.name}")
                return
            }
            if (!now.isBefore(graceEnd) && now.isBefore(window.end)) {
                println("$nowStr -> ACTIVE: ${window.name}")
                return
            }
        }
        val allPrayers = listOf(
            "Fajr" to fajr, "Dhuhr" to dhuhr, "Asr" to asr, "Maghrib" to maghrib, "Isha" to isha
        )
        for ((name, time) in allPrayers) {
            if (time.isAfter(now)) {
                println("$nowStr -> WAITING: $name")
                return
            }
        }
        println("$nowStr -> ALL_DONE")
    }

    testTime("04:10")
    testTime("04:13")
    testTime("04:20")
    testTime("04:28")
    testTime("05:00")
    testTime("06:00")
}
