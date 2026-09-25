import java.net.URL
import java.util.TreeSet

class TreeSetDictionary: IDictionary {
    val words: TreeSet<String>

    init {
        val url = URL("https://www.ms.sapientia.ro/~manyi/dict.txt")
        words = url.openStream().bufferedReader().readLines().toCollection(TreeSet())
    }

    override fun size(): Int {
        return words.size
    }

    override fun find(str: String): Boolean {
        return words.contains(str)
    }

    override fun add(str: String): Boolean {
        return words.add(str)
    }
}