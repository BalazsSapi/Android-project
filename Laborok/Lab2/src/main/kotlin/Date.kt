import java.time.LocalDate

data class Date(
    val year: Int = LocalDate.now().year,
    val month: Int = LocalDate.now().monthValue,
    val day: Int = LocalDate.now().dayOfMonth
) : Comparable<Date> {

    override fun compareTo(other: Date): Int {
        return compareBy(Date::year, Date::month, Date::day).compare(this, other)
    }
}


fun Date.isLeapYear(): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

fun Date.isValid(): Boolean {
    if (year < 1 || month !in 1..12 || day < 1) return false

    val maxDays = when (month) {
        2 -> if (this.isLeapYear()) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    return day <= maxDays
}