package dev.brown.wordy.data.db.content

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * 콘텐츠 DB(content.db)는 tools/build-db.mjs 가 Room이 내보낸 스키마(app/schemas)로 빌드해 assets 에 넣는다.
 * 리스트 필드는 JSON 문자열 컬럼으로 두고 앱 계층에서 kotlinx.serialization 으로 디코딩한다.
 * 스키마를 바꾸면 version 을 올리고 build-db 를 다시 실행한다 (진행 데이터는 별도 progress.db 라 영향 없음).
 */
@Entity(
    tableName = "word",
    indices = [Index("level"), Index("headword"), Index("isCore"), Index("unitId"), Index("topic")],
)
data class WordEntity(
    @PrimaryKey val id: String,
    val headword: String,
    val ipa: String?,
    val level: String,
    val cefr: String?,
    val freqRank: Int?,
    val isCore: Boolean,
    val topic: String,
    val unitId: String,
    val unitOrd: Int,
    /** JSON array<string>: 변화형·철자 변형 */
    val formsJson: String,
    /** JSON array<{form,pos,ko}> */
    val familyJson: String,
    /** JSON array<{word,note}> */
    val confusablesJson: String,
    val memoryHook: String?,
    /** 검색용: headword + 모든 sense 의 ko 를 공백으로 이어 붙인 소문자 문자열 */
    val searchText: String,
)

@Entity(tableName = "sense", indices = [Index("wordId")])
data class SenseEntity(
    @PrimaryKey val id: String,
    val wordId: String,
    val ord: Int,
    val pos: String,
    val ko: String,
    val en: String,
    val nuance: String?,
    val pattern: String?,
    /** JSON array<{en,ko,focus}> */
    val examplesJson: String,
    /** JSON array<string> */
    val collocationsJson: String,
    /** JSON array<{phrase,ko,example:{en,ko,focus}}> */
    val phrasalsJson: String,
)

@Entity(tableName = "unit", indices = [Index("level")])
data class UnitEntity(
    @PrimaryKey val id: String,
    val level: String,
    val ord: Int,
    val title: String,
    val topic: String,
    val wordCount: Int,
)

/** key: version, generatedAt, wordCount, coreCount … */
@Entity(tableName = "content_meta")
data class ContentMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)
