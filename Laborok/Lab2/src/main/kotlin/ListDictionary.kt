import java.net.URL

class ListDictionary: IDictionary {
    val words: MutableList<String>

    init {
        val url = URL("https://www.ms.sapientia.ro/~manyi/dict.txt")
        words = url.openStream().bufferedReader().readLines().toMutableList()
    }

    override fun add(str: String): Boolean {
        return words.add(str)
    }

    override fun size(): Int {
        return words.size
    }

    override fun find(str: String): Boolean {
        return words.contains(str)
    }
}