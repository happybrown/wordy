package dev.brown.wordy.domain.session

/** 타이핑 답 판정. 대소문자·앞뒤 공백·연속 공백·아포스트로피 종류를 무시하고, 1글자 오타는 "거의 맞음"으로 본다. */
object AnswerChecker {
    data class Result(val correct: Boolean, val almost: Boolean)

    fun normalize(s: String): String = s.trim().lowercase().replace('’', '\'').replace(Regex("\\s+"), " ")

    fun check(input: String, accepted: List<String>, strict: Boolean = false): Result {
        val a = normalize(input)
        if (a.isEmpty()) return Result(false, false)
        val targets = accepted.map(::normalize).filter { it.isNotEmpty() }
        if (targets.any { it == a }) return Result(true, false)
        if (strict) return Result(false, false)
        val almost = targets.any { it.length >= 4 && levenshtein(a, it) == 1 }
        return Result(false, almost)
    }

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1); cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            prev = cur
        }
        return prev[b.length]
    }
}
