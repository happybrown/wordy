package dev.brown.wordy.domain.session

import dev.brown.wordy.data.repo.WordRepository
import dev.brown.wordy.domain.model.Card
import dev.brown.wordy.domain.model.Skill
import dev.brown.wordy.domain.model.Word
import dev.brown.wordy.domain.srs.CardState
import kotlin.random.Random

/**
 * 카드(단어 × 스킬)에 맞는 문제를 만든다. 같은 스킬 안에서도 카드 상태에 따라 유형이 바뀐다.
 * - RECOGNIZE: 뜻 고르기
 * - CONTEXT: 빈칸 — 카드가 익숙해지기 전(REVIEW 이전 또는 stability < 3일)엔 4지선다, 이후 타이핑
 * - RECALL: 한국어→영어 타이핑, 홀수 번째 복습은 듣고 쓰기 (TTS 가능할 때)
 * - USE: 연어 짝 맞추기 / 문장 조립 교대
 */
class ExerciseGenerator(private val words: WordRepository, private val random: Random = Random.Default) {

    suspend fun generate(card: Card, word: Word, ttsAvailable: Boolean, preferVariant: Boolean = false): Exercise {
        return when (card.skill) {
            Skill.RECOGNIZE -> meaningChoice(card, word)
            Skill.CONTEXT -> cloze(card, word, typing = card.state == CardState.REVIEW && card.stability >= 3.0 && !preferVariant)
            Skill.RECALL -> recall(card, word, listen = ttsAvailable && ((card.reps % 2 == 1) xor preferVariant))
            Skill.USE -> if ((card.reps % 2 == 0) xor preferVariant) collocationMatch(card, word) ?: sentenceScramble(card, word) else sentenceScramble(card, word)
        }
    }

    suspend fun meaningChoice(card: Card, word: Word): Exercise.MeaningChoice {
        val sense = word.senses.firstOrNull { it.id == card.senseId } ?: word.primarySense
        val correct = sense.ko
        val distractors = words.distractorSenses(word, sense.pos, 3)
            .map { it.ko }.filter { it != correct && !word.senses.any { s -> s.ko == it } }.distinct().take(3)
        val options = (distractors + correct).shuffled(random)
        return Exercise.MeaningChoice(card, word, sense, options, options.indexOf(correct))
    }

    suspend fun cloze(card: Card, word: Word, typing: Boolean): Exercise {
        val sense = word.senses.firstOrNull { it.id == card.senseId } ?: word.primarySense
        val example = sense.examples[random.nextInt(sense.examples.size)]
        val blank = blankOut(example.en, example.focus, word.allForms) ?: return meaningChoice(card, word)
        if (typing) return Exercise.Cloze(card, word, sense, example, blank.first, blank.second, null, -1)
        val distractorWords = words.words(words.distractorSenses(word, sense.pos, 3).map { it.wordId })
            .map { it.headword }.filter { it != word.headword }.distinct().take(3)
        val options = (distractorWords + word.headword).shuffled(random)
        return Exercise.Cloze(card, word, sense, example, blank.first, blank.second, options, options.indexOf(word.headword))
    }

    fun recall(card: Card, word: Word, listen: Boolean): Exercise.Recall {
        val glosses = word.senses.take(3).map { "${it.pos.ko} ${it.ko}" }
        return Exercise.Recall(card, word, glosses, word.primarySense.en, listen)
    }

    suspend fun collocationMatch(card: Card, word: Word): Exercise.CollocationMatch? {
        val own = word.allCollocations.shuffled(random).mapNotNull { c -> blankOut(c, c, word.allForms)?.let { it.first to word.headword } }.take(2)
        if (own.isEmpty()) return null
        val others = words.words(words.distractorSenses(word, word.primarySense.pos, 6).map { it.wordId })
            .filter { it.id != word.id && it.allCollocations.isNotEmpty() }
            .mapNotNull { w -> val c = w.allCollocations.random(random); blankOut(c, c, w.allForms)?.let { it.first to w.headword } }
            .distinctBy { it.second }.take(4 - own.size)
        val pairs = (own + others).take(4)
        return if (pairs.size >= 3) Exercise.CollocationMatch(card, word, pairs) else null
    }

    fun sentenceScramble(card: Card, word: Word): Exercise.SentenceScramble {
        val sense = word.senses.firstOrNull { it.id == card.senseId } ?: word.primarySense
        val example = sense.examples.maxBy { it.en.split(' ').size }
        val tokens = example.en.split(' ')
        val chunkSize = if (tokens.size <= 6) 1 else 2
        val answer = tokens.chunked(chunkSize).map { it.joinToString(" ") }
        var shuffled = answer.shuffled(random)
        if (shuffled == answer && answer.size > 1) shuffled = answer.reversed()
        return Exercise.SentenceScramble(card, word, example, shuffled, answer)
    }

    companion object {
        const val BLANK = "______"

        /** 문장에서 focus 구간 안의 headword 형태를 찾아 빈칸으로 바꾼다. (빈칸 문장, 정답 표기) 또는 null */
        fun blankOut(sentence: String, focus: String, forms: List<String>): Pair<String, String>? {
            val focusStart = sentence.indexOf(focus).coerceAtLeast(0)
            val focusEnd = if (sentence.indexOf(focus) >= 0) focusStart + focus.length else sentence.length
            var best: MatchResult? = null
            for (form in forms.sortedByDescending { it.length }) {
                val re = Regex("(?<![A-Za-z])${Regex.escape(form)}(?![A-Za-z])", RegexOption.IGNORE_CASE)
                val m = re.findAll(sentence).firstOrNull { it.range.first >= focusStart && it.range.last < focusEnd } ?: re.find(sentence)
                if (m != null && (best == null || m.value.length > best.value.length)) best = m
            }
            val m = best ?: return null
            return sentence.replaceRange(m.range, BLANK) to m.value
        }
    }
}
