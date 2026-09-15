# 콘텐츠 제작 진행 상황

마지막 갱신: 2026-09-15

## 현황
| 항목 | 상태 |
|---|---|
| 단어 목록 (`content/wordlist/master.tsv`) | 4,054개 확정 (PRE 300 / ELEM 800 / MID 1,200 / HIGH 1,154 / ADV 600, Core 400 포함) |
| 배치 manifest | 81개 생성 완료 (deep 20 × 20개, light 61 × 60개) |
| 작성 완료 | **4/81 배치 = 240개 (6%)** — l001~l004, 전부 `validate` 0 error |
| 앱 내장 DB | `content.db` 242 words / 294 senses / 35 units |

## 남은 작업
```
deep  d001~d020   Core 400개 (sense 3~8개의 깊은 항목)  ← 전부 미작성
light l005~l061   나머지 3,414개
```

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

## 알려진 판단 사항
- `forms` 는 `tools/lib/forms.mjs` 의 `cleanForms` 가 품사에 맞게 정리한다 (NGSL 원본에는 `peopled`, `marchs` 같은 비단어가 섞여 있음). manifest·validator·build-db 가 모두 같은 함수를 쓴다.
- 요일·월(`monday`, `april`)은 목록상 소문자지만 `build-db` 가 표시용으로 대문자화한다.
- `confusables` 는 master 에 있는 단어만 참조 가능. 없으면 `nuance` 로 설명한다.
