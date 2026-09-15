# Wordy — 영어 어휘를 "쓸 수 있게" 익히는 안드로이드 앱

`apple → 사과` 식 단어장이 아니라, 의미(sense)별 용법·연어·예문·구동사·뉘앙스까지 익히고 FSRS 간격 반복으로 잊지 않게 하는 개인용 앱.
설계 문서: `~/.claude/plans/rustling-floating-hartmanis.md` (승인본).

## 구성
```
app/        Android 앱 (Kotlin, Jetpack Compose, Room 3, 수동 DI). 패키지 dev.brown.wordy
content/    콘텐츠 원천: 단어 목록(master.tsv), 작성 가이드, 스키마, 배치 산출물, 병합 결과
tools/      Node 26 파이프라인 (외부 의존성은 @exodus/schemasafe 하나)
```

## 빌드·실행
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"   # JDK 17+
./gradlew :app:assembleDebug            # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest        # domain 단위테스트 (FSRS, 판정, 문제 생성, 숙달)
```
AGP 9.x built-in Kotlin 을 쓰므로 `org.jetbrains.kotlin.android` 플러그인은 적용하지 않는다.

## 콘텐츠 파이프라인
```bash
cd tools && npm install                 # 최초 1회
node tools/fetch-sources.mjs            # NGSL/NAWL/BSL/CEFR-J/교육부 원본 수집 (content/sources, git 미추적)
node tools/build-wordlist.mjs           # 레벨 매핑 → content/wordlist/master.tsv + report.md
node tools/make-batches.mjs             # 배치 manifest (deep 20개 × 20, light 60개 × 61)
#   → 서브에이전트가 content/GUIDE.md 에 따라 batches/{deep,light}/XNNN.json 작성
node tools/validate.mjs --batch d001 [--repair]   # 스키마 + 의미 규칙 검증, 실패분 재생성 manifest
node tools/merge.mjs [--partial]        # → content/entries/{pre,elem,mid,high,adv}.json
node tools/build-db.mjs [--include-golden]        # → app/src/main/assets/database/content.db (+ content.version)
node tools/stats.mjs                    # 분포 리포트
```
`build-db` 는 Room 이 내보낸 스키마(`app/schemas/**/1.json`)의 `createSql` 을 그대로 실행하므로 앱의 스키마 검증을 통과한다.
콘텍츠 DB(content.db) 와 학습 진행 DB(progress.db) 는 분리되어 콘텐츠 갱신이 학습 기록을 지우지 않는다.

## 레벨과 트랙
| 레벨 | 단어 수 | 기준 |
|---|---|---|
| PRE 초등이전 | 300 | 수작업 시드 + 교육부 초등 800 ∩ CEFR-J A1 |
| ELEM 초등 | 800 | 교육부 초등 800 → CEFR-J A1 → NGSL |
| MID 중등 | 1,200 | NGSL 상위 1,500 → 교육부 중고등 ∩ A2/B1 |
| HIGH 고등 | 1,154 | 나머지 NGSL·교육부 중고등, ★★ 일부 |
| ADV 그 이상 | 600 | 교육부 ★★ + NAWL(학술)/BSL(비즈니스) 교차 |
Core 회화 트랙 400개는 위 레벨 안에 포함되며 sense 3~8개의 깊은 항목으로 작성된다.

## 라이선스 고지
단어 목록: NGSL·NAWL·BSL (CC BY-SA 4.0, Browne/Culligan/Phillips), CEFR-J Wordlist 1.6 (Tono Lab, TUFS), 교육부 고시 제2015-74호 기본 어휘.
뜻·예문·설명은 이 프로젝트에서 작성. 간격 반복: FSRS-6 (open-spaced-repetition).
