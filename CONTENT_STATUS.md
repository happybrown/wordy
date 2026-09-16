# 콘텐츠 제작 진행 상황

마지막 갱신: 2026-09-16 11:01

## 현황
**콘텐츠 제작 완료.** 4,054개 전량 작성·검증·빌드 완료.

| 항목 | 상태 |
|---|---|
| 단어 목록 (`content/wordlist/master.tsv`) | 4,054개 (PRE 300 / ELEM 800 / MID 1,200 / HIGH 1,154 / ADV 600, Core 400 포함) |
| 배치 | 81/81 완료 (deep 20 × 20개, light 61) |
| 검증 | `node tools/validate.mjs --all` → **0 error / 0 warning** |
| 앱 내장 DB | `content.db` 4,054 words / 5,855 senses / 214 units (6,080 KB) |
| 항목 간 예문 중복 | 0건 |

품사별 sense: 명사 2,988 / 동사 1,558 / 형용사 939 / 부사 292 / 기타 78.
IPA 100%, 뉘앙스 100%, confusables 92%, wordFamily 45%.

## 변화형 필터 (`tools/lib/forms.mjs`)
원본 단어 목록에는 영어에 없는 변화형이 섞여 있어 걸러낸다. 판정은 단어가
아니라 **sense 단위**로 한다 — 같은 단어라도 뜻에 따라 가산/불가산이 갈린다.

- 불가산 명사의 가짜 복수: compliance, conservation, consumption, collateral,
  expertise, governance, liquidity, taxation, timber, volatility, radiation,
  rainfall, rage, oxygen 등
- 반대로 실재하므로 **남긴** 형태: consents(동의서), completions(완공 건수),
  maturities(만기일), grosser(구어 '역겨운'), people, less/least
- 접두사가 붙은 불규칙 동사: rewrited, overcomed, weaved 등 (rewrite → rewrote)
- 3음절 이상 형용사의 -er/-est: profounder → more profound

`node --test tools/test/forms.test.mjs` 28개가 이 분류를 고정한다.

## 배치 하나 작성하는 법 (콘텐츠 갱신·재작성 시 참고)
서브에이전트에게 다음을 시킨다 (배치 하나당 약 13분, 12만 토큰):

> content/GUIDE.md, content/schema/entry.schema.json, content/topics.json,
> content/golden/{deep,light}.json 을 먼저 읽고,
> content/batches/{deep,light}/XNNN.manifest.json 의 단어만 사용해
> content/batches/{deep,light}/XNNN.json 을 작성한 뒤
> `node tools/validate.mjs --batch XNNN` 이 0 error 가 될 때까지 고칠 것.

작성 후:
```bash
node tools/merge.mjs               # 작성 중이라 빠진 배치가 있으면 --partial
node tools/build-db.mjs            # 시드 포함하려면 --include-golden
./gradlew :app:assembleDebug
```

## 배치 작성 시 주의 (실제로 걸렸던 것들)
- **구동사 예문에는 원형이 들어가야 한다.** `get back`의 예문에 "I got back late"를 쓰면 검증 실패. "I'll get back to you"처럼 쓴다.
- **연어 개수는 단어 전체 합**이지 sense별이 아니다.
- **confusable 대상은 학습 목록에 없어도 된다.** `by` vs `until`처럼 기능어와 대조해도 경고나지 않는다.
- 동시에 3~5개까지만 돌린다. 그 이상은 세션 사용량 한도에 걸려 전부 중단된 적이 있다.
- **작업 중에는 `make-batches` 를 돌리지 않는다.** 배치 구성이 바뀌어 에이전트가 받은 manifest 와 어긋난다(안전장치가 막지만 `--force` 로 뚫으면 항목 중복·누락이 생긴다).
- **에이전트가 뺀 변화형은 그대로 믿지 말고 하나씩 확인한다.** 대부분 옳지만 `maturities`(만기일), `consents`(동의서)처럼 다른 sense 에서는 실재하는 형태를 불가산이라고 뺀 경우가 있었다. 진짜 오류면 `forms.mjs` 에 넣고, 실재하면 항목에 선언하거나 해당 sense 를 추가한다.
- **예문 중복은 병합본이 아니라 배치 원본에서 고친다.** `content/entries/*.json` 만 고치면 다음 `merge` 에서 되돌아간다.

## 알려진 판단 사항
- `forms` 는 `tools/lib/forms.mjs` 의 `cleanForms` 가 품사에 맞게 정리한다 (NGSL 원본에는 `peopled`, `marchs` 같은 비단어가 섞여 있음). manifest·validator·build-db 가 모두 같은 함수를 쓴다.
- 요일·월(`monday`, `april`)은 목록상 소문자지만 `build-db` 가 표시용으로 대문자화한다.
- `confusables` 는 master 에 있는 단어만 참조 가능. 없으면 `nuance` 로 설명한다.
