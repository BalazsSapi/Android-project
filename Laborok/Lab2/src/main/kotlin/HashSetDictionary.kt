import java.net.URL

class HashSetDictionary: IDictionary {
    private val words: HashSet<String>

    init {
        val url = URL("https://www.ms.sapientia.ro/~manyi/dict.txt")
        words = url.openStream().bufferedReader().readLines().toHashSet()
    }

    override fun find(str: String): Boolean {
        return words.contains(str)
    }

    override fun add(str: String): Boolean {
        return words.add(str)
    }

    override fun size(): Int {
        return words.size
    }
}