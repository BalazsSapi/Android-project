class DictionaryProvider {
    fun createDictionary(type: DictionaryType): IDictionary = when (type) {
        DictionaryType.HASH_SET -> HashSetDictionary()
        DictionaryType.TREE_SET -> TreeSetDictionary()
        DictionaryType.ARRAY_LIST -> ListDictionary()
        else -> throw IllegalArgumentException("Unknown dictionary type")
    }
}