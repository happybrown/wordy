package dev.brown.wordy.data.repo

import dev.brown.wordy.data.db.content.ContentDao
import dev.brown.wordy.data.db.content.SenseEntity
import dev.brown.wordy.data.db.content.WordEntity
import dev.brown.wordy.domain.model.Confusable
import dev.brown.wordy.domain.model.Example
import dev.brown.wordy.domain.model.FamilyMember
import dev.brown.wordy.domain.model.Level
import dev.brown.wordy.domain.model.Phrasal
import dev.brown.wordy.domain.model.Pos
import dev.brown.wordy.domain.model.Sense
import dev.brown.wordy.domain.model.Unit
import dev.brown.wordy.domain.model.Word
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** content.db → 도메인 모델. 읽기 전용. */
class WordRepository(private val dao: ContentDao) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = HashMap<String, Word>()

    suspend fun word(id: String): Word? {
        cache[id]?.let { return it }
        val w = dao.word(id) ?: return null
        return toWord(w, dao.senses(id)).also { cache[id] = it }
    }

    suspend fun words(ids: List<String>): List<Word> {
        if (ids.isEmpty()) return emptyList()
        val missing = ids.filter { it !in cache }
        if (missing.isNotEmpty()) {
            val entities = missing.chunked(500).flatMap { dao.words(it) }
            val senses = missing.chunked(500).flatMap { dao.sensesFor(it) }.groupBy { it.wordId }
            for (e in entities) cache[e.id] = toWord(e, senses[e.id].orEmpty())
        }
        return ids.mapNotNull { cache[it] }
    }

    suspend fun wordIdsByLevel(level: Level): List<String> = dao.wordIdsByLevel(level.code)
    suspend fun coreWordIds(): List<String> = dao.coreWordIds()
    suspend fun wordsByUnit(unitId: String): List<Word> = words(dao.wordsByUnit(unitId).map { it.id })
    suspend fun units(level: Level): List<Unit> = dao.units(level.code).map { Unit(it.id, Level.of(it.level), it.ord, it.title, it.topic, it.wordCount) }
    suspend fun countsByLevel(): Map<Level, Int> = dao.countsByLevel().associate { Level.of(it.level) to it.count }
    suspend fun search(query: String, limit: Int = 50): List<Word> = words(dao.search(query.trim().lowercase(), limit).map { it.id })
    suspend fun meta(key: String): String? = dao.meta(key)

    /** 오답지: 같은 레벨(또는 Core)·같은 품사의 다른 단어 대표 의미 */
    suspend fun distractorSenses(word: Word, pos: Pos, limit: Int): List<Sense> {
        val rows = if (word.isCore) dao.coreDistractorSenses(pos.code, word.id, limit * 2) else dao.distractorSenses(word.level.code, pos.code, word.id, limit * 2)
        val fallback = if (rows.size < limit) dao.distractorSenses(word.level.code, pos.code, word.id, limit * 2) else emptyList()
        val anyPos = if (rows.size + fallback.size < limit) dao.distractorSensesAnyPos(word.level.code, word.id, limit * 2) else emptyList()
        return (rows + fallback + anyPos).distinctBy { it.wordId }.map { toSense(it) }
    }

    private fun toWord(w: WordEntity, senses: List<SenseEntity>) = Word(
        id = w.id, headword = w.headword, ipa = w.ipa, level = Level.of(w.level), cefr = w.cefr, freqRank = w.freqRank,
        isCore = w.isCore, topic = w.topic, unitId = w.unitId,
        forms = json.decodeFromString(ListSerializer(String.serializer()), w.formsJson),
        family = json.decodeFromString(ListSerializer(FamilyMember.serializer()), w.familyJson),
        confusables = json.decodeFromString(ListSerializer(Confusable.serializer()), w.confusablesJson),
        memoryHook = w.memoryHook,
        senses = senses.sortedBy { it.ord }.map { toSense(it) },
    )

    private fun toSense(s: SenseEntity) = Sense(
        id = s.id, wordId = s.wordId, ord = s.ord, pos = Pos.of(s.pos), ko = s.ko, en = s.en, nuance = s.nuance, pattern = s.pattern,
        examples = json.decodeFromString(ListSerializer(Example.serializer()), s.examplesJson),
        collocations = json.decodeFromString(ListSerializer(String.serializer()), s.collocationsJson),
        phrasals = json.decodeFromString(ListSerializer(Phrasal.serializer()), s.phrasalsJson),
    )
}
