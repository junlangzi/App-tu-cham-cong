package com.example.ui

import kotlin.math.sin
import kotlin.math.floor
import java.util.Date
import java.util.Calendar

object LunarCalendar {
    const val PI = Math.PI

    /* Discard the fractional part of a number, e.g., INT(3.2) = 3 */
    fun INT(d: Double): Int {
        return floor(d).toInt()
    }

    /* Compute the (integral) Julian day number of day dd/mm/yyyy */
    fun jdFromDate(day: Int, month: Int, year: Int): Int {
        val a = INT((14 - month) / 12.0)
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        var jd = day + INT((153 * m + 2) / 5.0) + 365 * y + INT(y / 4.0) - INT(y / 100.0) + INT(y / 400.0) - 32045
        if (jd < 2299161) {
            jd = day + INT((153 * m + 2) / 5.0) + 365 * y + INT(y / 4.0) - 32083
        }
        return jd
    }

    /* Convert a Julian day number to Date */
    fun jdToDate(jd: Int): Date {
        var a: Int
        var b: Int
        var c: Int
        // After 5/10/1582, Gregorian calendar
        if (jd > 2299160) {
            a = jd + 32044
            b = INT((4.0 * a + 3.0) / 146097.0)
            c = a - INT((b * 146097.0) / 4.0)
        } else {
            b = 0
            c = jd + 32082
        }
        val d = INT((4.0 * c + 3.0) / 1461.0)
        val e = c - INT((1461.0 * d) / 4.0)
        val m = INT((5.0 * e + 2.0) / 153.0)
        val day = e - INT((153 * m + 2) / 5.0) + 1
        val month = m + 3 - 12 * INT(m / 10.0)
        val year = b * 100 + d - 4800 + INT(m / 10.0)
        
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    /* Compute the time of the k-th new moon */
    fun NewMoon(k: Double): Double {
        val T = k / 1236.85 // Time in Julian centuries from 1900 January 0.5
        val T2 = T * T
        val T3 = T2 * T
        val dr = PI / 180.0
        var Jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * T2 - 0.000000155 * T3
        Jd1 = Jd1 + 0.00033 * sin((166.56 + 132.87 * T - 0.009173 * T2) * dr) // Mean new moon
        val M = 359.2242 + 29.10535608 * k - 0.0000333 * T2 - 0.00000347 * T3 // Sun's mean anomaly
        val Mpr = 306.0253 + 385.81691806 * k + 0.0107306 * T2 + 0.00001236 * T3 // Moon's mean anomaly
        val F = 21.2964 + 390.67050646 * k - 0.0016528 * T2 - 0.00000239 * T3 // Moon's argument of latitude
        var C1 = (0.1734 - 0.000393 * T) * sin(M * dr) + 0.0021 * sin(2.0 * dr * M)
        C1 = C1 - 0.4068 * sin(Mpr * dr) + 0.0161 * sin(dr * 2.0 * Mpr)
        C1 = C1 - 0.0004 * sin(dr * 3.0 * Mpr)
        C1 = C1 + 0.0104 * sin(dr * 2.0 * F) - 0.0051 * sin(dr * (M + Mpr))
        C1 = C1 - 0.0074 * sin(dr * (M - Mpr)) + 0.0004 * sin(dr * (2.0 * F + M))
        C1 = C1 - 0.0004 * sin(dr * (2.0 * F - M)) - 0.0006 * sin(dr * (2.0 * F + Mpr))
        C1 = C1 + 0.0010 * sin(dr * (2.0 * F - Mpr)) + 0.0005 * sin(dr * (2.0 * Mpr + M))
        val deltat = if (T < -11.0) {
            0.001 + 0.000839 * T + 0.0002261 * T2 - 0.00000845 * T3 - 0.000000081 * T * T3
        } else {
            -0.000278 + 0.000265 * T + 0.000262 * T2
        }
        return Jd1 + C1 - deltat
    }

    fun getNewMoon(k: Int, timezone: Double): Int {
        return INT(NewMoon(k.toDouble()) + 0.5 + timezone / 24.0)
    }

    fun sunLongitude(jdn: Double): Double {
        val T = (jdn - 2451545.0) / 36525.0
        val T2 = T * T
        val dr = PI / 180.0
        val M = 357.52910 + 35999.05030 * T - 0.0001559 * T2 - 0.00000048 * T * T2
        val L0 = 280.46645 + 36000.76983 * T + 0.0003032 * T2
        var DL = (1.914600 - 0.004817 * T - 0.000014 * T2) * sin(dr * M)
        DL = DL + (0.019993 - 0.000101 * T) * sin(dr * 2.0 * M) + 0.000290 * sin(dr * 3.0 * M)
        var L = L0 + DL
        L = L * dr
        L = L - PI * 2.0 * INT(L / (PI * 2.0))
        return L
    }

    fun getSunLongitude(jdn: Int, timezone: Double): Int {
        return INT(sunLongitude(jdn.toDouble() - 0.5 - timezone / 24.0) / PI * 6.0)
    }

    fun getLunarMonth11(year: Int, timezone: Double): Int {
        val off = jdFromDate(31, 12, year) - 2415021
        val k = INT(off / 29.530588853)
        val nm = getNewMoon(k, timezone)
        val sunLong = getSunLongitude(nm, timezone)
        return if (sunLong >= 9) getNewMoon(k - 1, timezone) else nm
    }

    fun getLeapMonthOffset(a11: Int, timezone: Double): Int {
        val k = INT((a11 - 2415021.076998695) / 29.530588853 + 0.5)
        var last = 0
        var i = 1
        var arc = getSunLongitude(getNewMoon(k + i, timezone), timezone)
        do {
            i++
            last = arc
            arc = getSunLongitude(getNewMoon(k + i, timezone), timezone)
        } while (arc != last && i < 14)
        return i - 1
    }

    data class LunarDate(
        val day: Int,
        val month: Int,
        val year: Int,
        val isLeap: Boolean
    )

    fun convertSolar2Lunar(dd: Int, mm: Int, yy: Int, timezone: Double = 7.0): LunarDate {
        var k: Int
        var monthStart: Int
        var a11: Int
        var b11: Int
        var lunarDay: Int
        var lunarMonth: Int
        var lunarYear: Int
        var lunarLeap: Int
        var diff: Int
        var leapMonthDiff: Int

        val dayNumber = jdFromDate(dd, mm, yy)
        k = INT((dayNumber - 2415021.076998695) / 29.530588853)
        monthStart = getNewMoon(k + 1, timezone)
        if (monthStart > dayNumber) {
            monthStart = getNewMoon(k, timezone)
        }
        a11 = getLunarMonth11(yy, timezone)
        b11 = a11
        if (a11 >= monthStart) {
            lunarYear = yy
            a11 = getLunarMonth11(yy - 1, timezone)
        } else {
            lunarYear = yy + 1
            b11 = getLunarMonth11(yy + 1, timezone)
        }
        lunarDay = dayNumber - monthStart + 1
        diff = INT((monthStart - a11) / 29.0)
        lunarLeap = 0
        lunarMonth = diff + 11
        if (b11 - a11 > 365) {
            leapMonthDiff = getLeapMonthOffset(a11, timezone)
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10
                if (diff == leapMonthDiff) {
                    lunarLeap = 1
                }
            }
        }
        if (lunarMonth > 12) {
            lunarMonth = lunarMonth - 12
        }
        if (lunarMonth >= 11 && diff < 4) {
            lunarYear -= 1
        }
        return LunarDate(
            day = lunarDay,
            month = lunarMonth,
            year = lunarYear,
            isLeap = lunarLeap == 1
        )
    }

    fun isVietnameseHoliday(solarDay: Int, solarMonth: Int, lunarDay: Int, lunarMonth: Int): Boolean {
        // Solar holidays
        if (solarDay == 1 && solarMonth == 1) return true // Tết Dương lịch
        if (solarDay == 30 && solarMonth == 4) return true // Giải phóng miền Nam
        if (solarDay == 1 && solarMonth == 5) return true // Quốc tế Lao động
        if (solarDay == 2 && solarMonth == 9) return true // Quốc khánh thứ nhất
        if (solarDay == 1 && solarMonth == 9) return true // Quốc khánh thứ hai (thường nghỉ thêm 1 ngày trước/sau)

        // Lunar holidays
        if (lunarDay == 10 && lunarMonth == 3) return true // Giỗ Tổ Hùng Vương
        if (lunarMonth == 1 && (lunarDay in 1..5)) return true // Tết Nguyên Đán (mùng 1-5 âm lịch)
        if (lunarMonth == 12 && (lunarDay in 29..30)) return true // Tết Nguyên Đán cuối năm (29, 30 âm lịch)

        return false
    }
}
