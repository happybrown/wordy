# 콘텐츠 제작 진행 상황

마지막 갱신: 2026-09-15 22:30

## 현황
| 항목 | 상태 |
|---|---|
| 단어 목록 (`content/wordlist/master.tsv`) | 4,054개 확정 (PRE 300 / ELEM 800 / MID 1,200 / HIGH 1,154 / ADV 600, Core 400 포함) |
| 배치 manifest | 81개 생성 완료 (deep 20 × 20개, light 61 × 60개) |
| 작성 완료 | **38/81 배치 = 1,600개 (39%)** — Core d001~d017, 초등·중등 l001~l021. 전부 `validate` 0 error / 0 warning |
| 앱 내장 DB | `content.db` 1,600 words / 2,802 senses / 97 units |

## 남은 작업
```
deep  d018~d020   Core 60개 (d020 이 마지막 Core 배치)
light l022~l061   나머지 2,394개
```
레벨별: PRE 300/300, ELEM 800/800 완료. MID 500/1200, HIGH 0/1154, ADV 0/600.
진행 상황은 `node tools/batch-status.mjs`, 다음 대상은 `node tools/batch-status.mjs --next 3`.

## 배치 하나 작성하는 법
서브에이전트에게 다음을 시킨다 (배치 하나당 약 13분, 12만 토큰):

> content/GUIDE.md, content/schema/entry.schema.json, content/topics.json,
> content/golden/{deep,light}.json 을 먼저 읽고,
> content/batches/{deep,light}/XNNN.manifest.json 의 단어만 사용해
> content/batches/{deep,light}/XNNN.json 을 작성한 뒤
> `node tools/validate.mjs --batch XNNN` 이 0 error 가 될 때까지 고칠 것.

작성 후:
```bash
node tools/merge.mjs --partial     # 전부 끝나면 --partial 없이
node tools/build-db.mjs            # 시드 포함하려면 --include-golden
./gradlew :app:assembleDebug
```

## 배치 작성 시 주의 (실제로 걸렸던 것들)
- **구동사 예문에는 원형이 들어가야 한다.** `get back`의 예문에 "I got back late"를 쓰면 검증 실패. "I'll get back to you"처럼 쓴다.
- **연어 개수는 단어 전체 합**이지 sense별이 아니다.
- **confusable 대상은 학습 목록에 없어도 된다.** `by` vs `until`처럼 기능어와 대조해도 경고나지 않는다.
- 동시에 3~5개까지만 돌린다. 그 이상은 세션 사용량 한도에 걸려 전부 중단된 적이 있다.
- **작업 중에는 `make-batches` 를 돌리지 않는다.** 배치 구성이 바뀌어 에이전트가 받은 manifest 와 어긋난다(안전장치가 막지만 `--force` 로 뚫으면 항목 중복·누락이 생긴다).
- 배치 완료 후 `node /tmp/cleanforms.mjs` 대신 직접 확인: 변화형은 `cleanForms` 가 품사에 맞게 정리하므로 에이전트가 master.tsv 를 직접 보고 넣은 형태는 불필요할 수 있다.

## 알려진 판단 사항
- `forms` 는 `tools/lib/forms.mjs` 의 `cleanForms` 가 품사에 맞게 정리한다 (NGSL 원본에는 `peopled`, `marchs` 같은 비단어가 섞여 있음). manifest·validator·build-db 가 모두 같은 함수를 쓴다.
- 요일·월(`monday`, `april`)은 목록상 소문자지만 `build-db` 가 표시용으로 대문자화한다.
- `confusables` 는 master 에 있는 단어만 참조 가능. 없으면 `nuance` 로 설명한다.
