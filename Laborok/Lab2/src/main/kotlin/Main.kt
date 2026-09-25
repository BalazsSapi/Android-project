import kotlin.random.Random

fun String.monogram(): String = this.split(' ').map { it[0] }.joinToString("")

fun List<String>.joinWithSeparator(separator: String): String = this.joinToString(separator)

fun List<String>.longest(): String = this.maxByOrNull { it.length } ?: ""

fun generateRandomDate(): Date {
    val year = Random.nextInt(1, 2030)
    val month = Random.nextInt(1, 13)
    val day = Random.nextInt(1, 32)
    return Date(year, month, day)
}

fun main() {
    // 1.
    /*val dictionaryProvider = DictionaryProvider()
    val dict: IDictionary = dictionaryProvider.createDictionary(DictionaryType.HASH_SET)
    println("Number of words: ${dict.size()}")
    var word: String?
    while(true){
        print("What to find? ")
        word = readln()
        if(word == "quit"){
            break
        }
        println("Result: ${word.let { dict.find(it) }}")
    }*/

    // 2.
    /*val name: String = "John Smith Kebab"
    println("Monogram: ${name.monogram()}")

    val words: List<String> = listOf("apple", "banana", "cherry")
    println("Joined with comma: ${words.joinWithSeparator(", ")}")

    println("Longest word: ${words.longest()}")*/

    // 3.
    val validDates = mutableListOf<Date>()

    println("--- 1. Generálás és érvényesség ellenőrzése ---")
    while (validDates.size < 10) {
        val randomDate = generateRandomDate()
        if (randomDate.isValid()) {
            validDates.add(randomDate)
        } else {
            println("Érvénytelen dátum kiszűrve: $randomDate")
        }
    }

    println("\n--- 2. A 10 érvényes dátum (forEach) ---")
    validDates.forEach { println(it) }

    println("\n--- 3. Rendezett lista (Comparable / Természetes sorrend) ---")
    val sortedDates = validDates.sorted()
    sortedDates.forEach { println(it) }

    println("\n--- 4. Megfordított rendezett lista ---")
    val reversedDates = sortedDates.reversed()
    reversedDates.forEach { println(it) }

    println("\n--- 5. Egyedi rendezés (Comparator: Hónap -> Nap -> Év) ---")
    val customComparator = Comparator<Date> { d1, d2 ->
        if (d1.month != d2.month) d1.month.compareTo(d2.month)
        else if (d1.day != d2.day) d1.day.compareTo(d2.day)
        else d1.year.compareTo(d2.year)
    }

    val customSortedDates = validDates.sortedWith(customComparator)
    customSortedDates.forEach { println(it) }
}
