package dev.brown.wordy.domain.model

import kotlinx.serialization.Serializable

enum class Level(val code: String, val ko: String, val shortKo: String) {
    PRE("PRE", "초등이전", "초등이전"),
    ELEM("ELEM", "초등", "초등"),
    MID("MID", "중등", "중등"),
    HIGH("HIGH", "고등", "고등"),
    ADV("ADV", "그 이상", "심화");

    companion object {
        fun of(code: String) = entries.firstOrNull { it.code == code } ?: ELEM
    }
}

enum class Pos(val code: String, val ko: String) {
    N("n", "명사"), V("v", "동사"), ADJ("adj", "형용사"), ADV("adv", "부사"), PREP("prep", "전치사"),
    PRON("pron", "대명사"), DET("det", "한정사"), CONJ("conj", "접속사"), NUM("num", "수사"),
    MODAL("modal", "조동사"), INTERJ("interj", "감탄사");

    companion object {
        fun of(code: String) = entries.firstOrNull { it.code == code } ?: N
    }
}

@Serializable
data class Example(val en: String, val ko: String, val focus: String)

@Serializable
data class Phrasal(val phrase: String, val ko: String, val example: Example)

@Serializable
data class FamilyMember(val form: String, val pos: String, val ko: String)

@Serializable
data class Confusable(val word: String, val note: String)

data class Sense(
    val id: String,
    val wordId: String,
    val ord: Int,
    val pos: Pos,
    val ko: String,
    val en: String,
    val nuance: String?,
    val pattern: String?,
    val examples: List<Example>,
    val collocations: List<String>,
    val phrasals: List<Phrasal>,
)

data class Word(
    val id: String,
    val headword: String,
    val ipa: String?,
    val level: Level,
    val cefr: String?,
    val freqRank: Int?,
    val isCore: Boolean,
    val topic: String,
    val unitId: String,
    val forms: List<String>,
    val family: List<FamilyMember>,
    val confusables: List<Confusable>,
    val memoryHook: String?,
    val senses: List<Sense>,
) {
    /** headword + 변화형 (소문자). 정답 판정·빈칸 판정에 사용 */
    val allForms: List<String> get() = (listOf(headword) + forms).map { it.lowercase() }.distinct()
    val primarySense: Sense get() = senses.first()
    val allCollocations: List<String> get() = senses.flatMap { it.collocations }
    val allPhrasals: List<Phrasal> get() = senses.flatMap { it.phrasals }
}

data class Unit(val id: String, val level: Level, val ord: Int, val title: String, val topic: String, val wordCount: Int)

/** 콘텐츠 주제 분류 표시명 (content/topics.json 과 동일) */
object Topics {
    val ko: Map<String, String> = mapOf(
        "people_family" to "사람·가족·관계", "body_health" to "신체·건강", "food_cooking" to "음식·요리", "home_daily" to "집·일상생활",
        "school_learning" to "학교·학습", "work_business" to "일·직장·비즈니스", "money_shopping" to "돈·쇼핑·경제", "travel_transport" to "여행·교통·장소",
        "nature_animals" to "자연·동물·날씨", "environment_science" to "환경·과학·기술", "society_politics" to "사회·정치·법", "culture_arts" to "문화·예술·미디어",
        "sports_leisure" to "스포츠·취미·여가", "emotions_personality" to "감정·성격", "thinking_opinion" to "사고·의견·판단", "communication" to "말하기·의사소통",
        "time_quantity" to "시간·수량·정도", "movement_action" to "동작·이동", "description_quality" to "묘사·성질·상태", "abstract_general" to "추상·일반", "academic" to "학술·연구",
    )
    fun label(key: String) = ko[key] ?: key
}
