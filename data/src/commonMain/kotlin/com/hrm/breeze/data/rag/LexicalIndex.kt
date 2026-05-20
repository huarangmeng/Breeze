package com.hrm.breeze.data.rag

import kotlin.math.min

class LexicalIndex {
    fun tokenize(text: String): List<String> {
        val normalized = text.lowercase()
        val wordTerms = Regex("[\\p{L}\\p{N}_]+")
            .findAll(normalized)
            .map { match -> match.value }
            .filter { term -> term.length > 1 && term !in StopWords }
            .toList()
        val cjkTerms = normalized
            .filter { char -> char.code in CJK_RANGE_START..CJK_RANGE_END }
            .windowed(size = 2, step = 1, partialWindows = false)
        return (wordTerms + cjkTerms).distinct()
    }

    fun encodeTerms(text: String): String = tokenize(text).joinToString(separator = "\n")

    fun score(query: String, encodedTerms: String, content: String): Float {
        val queryTerms = tokenize(query)
        if (queryTerms.isEmpty() || encodedTerms.isBlank()) return 0f
        val chunkTerms = encodedTerms.lineSequence().filter(String::isNotBlank).toSet()
        val uniqueMatches = queryTerms.count { term -> term in chunkTerms }
        if (uniqueMatches == 0) return 0f
        val frequencyMatches = queryTerms.sumOf { term -> content.lowercase().countOccurrences(term) }
        return uniqueMatches * UNIQUE_MATCH_WEIGHT + min(frequencyMatches, MAX_FREQUENCY_BOOST)
    }

    private fun String.countOccurrences(term: String): Int {
        var count = 0
        var startIndex = 0
        while (true) {
            val index = indexOf(term, startIndex = startIndex)
            if (index < 0) return count
            count += 1
            startIndex = index + term.length
        }
    }

    private companion object {
        const val CJK_RANGE_START = 0x4E00
        const val CJK_RANGE_END = 0x9FFF
        const val UNIQUE_MATCH_WEIGHT = 3f
        const val MAX_FREQUENCY_BOOST = 6
        val StopWords = setOf("the", "and", "for", "with", "this", "that", "from", "have", "you")
    }
}
