import java.util.Locale
import java.util.Locale.getDefault
import kotlin.math.sqrt
import kotlin.random.Random

fun isPrime(num: Int): Boolean {
    if (num <= 1) return false
    if (num == 2) return true
    if (num % 2 == 0) return false

    for (i in 3..sqrt(num.toDouble()).toInt() step 2) {
        if (num % i == 0) return false
    }
    return true
}

fun encodeString(str: String): String {
    return str.reversed()
}

fun decodeString(str: String): String {
    return str.reversed()
}

fun tester(str: String, func: (String) -> String): String {
    return func(str)
}

fun evenFromList(list: List<Int>) = list.filter { it % 2 == 0 }

fun main() {
    // 1.
    /*
    val num1: Int=2
    val num2: Int = 3
    println("$num1 + $num2 = ${num1 + num2}") */

    // 2.
    /*
    val dayOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    for (day in dayOfWeek) {
        println(day)
    }
    println()

    for (day in dayOfWeek.filter{it.startsWith("T")}){
        println(day)
    }
    println()

    for (day in dayOfWeek.filter{it.contains('e')}) {
        println(day)
    }
    println()

    for (day in dayOfWeek.filter{it.length==6}) {
        println(day)
    }*/

    // 3.
    /*
    for (i in 1 .. 10000) {
        if (isPrime(i)) println(i)
    }*/

    // 4.
    /*
    var str = "Helló"
    str = tester(str, ::encodeString)
    println(str)
    str = tester(str, ::decodeString)
    println(str)*/

    // 5.
    /*
    val list = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    println(evenFromList(list))*/

    // 6.
    /*
    val list = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    println(list.map { it * 2 })

    val dayOfWeek = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
    println(dayOfWeek.map { it.uppercase() })
    println(dayOfWeek.map { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } })
    println(dayOfWeek.map { it.length })
    print(dayOfWeek.map { it.length }.average())*/

    // 7.
    /*
    val dayOfWeek = mutableListOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    dayOfWeek.removeIf { it.contains('n') }
    println(dayOfWeek)
    for ((index, day) in dayOfWeek.withIndex()) {
        println("Item at $index is $day")
    }
    dayOfWeek.sort()
    println(dayOfWeek)*/

    // 8.
    /*
    val randomValues = Array(10) { Random.nextInt(0, 101) }
    for (value in randomValues) {
        println(value)
    }
    randomValues.sort()
    println('\n')
    for (value in randomValues) {
        println(value)
    }
    println(randomValues.any { it % 2 == 0 })
    println(randomValues.all { it % 2 == 0 })
    var sum = 0
    var numOfElements = 0
    randomValues.forEach { sum += it ; numOfElements++}
    println(sum.toDouble() / numOfElements)*/


}