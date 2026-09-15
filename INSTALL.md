# 휴대폰에 설치하기

바탕화면의 `Wordy.apk` 를 갤럭시 S23 에 설치한다. 설치 파일은
`./gradlew :app:assembleRelease` 로 다시 만들 수 있고, 결과는
`app/build/outputs/apk/release/app-release.apk` 에 생긴다.

## 방법 1 — USB 로 바로 설치 (가장 빠름)
폰을 USB 로 연결하고 개발자 옵션의 USB 디버깅을 켠 뒤:
```bash
~/Library/Android/sdk/platform-tools/adb install -r ~/Desktop/Wordy.apk
```
폰 화면에 "USB 디버깅을 허용하시겠습니까?" 가 뜨면 허용한다.

## 방법 2 — 파일로 옮겨서 설치
`Wordy.apk` 를 카카오톡 나에게 보내기·구글 드라이브·USB 중 편한 방법으로
폰에 옮긴 뒤, 파일 관리자에서 탭한다. "출처를 알 수 없는 앱" 설치를
허용하라는 안내가 나오면 허용한다.

## 설치 후
- 첫 실행에서 시작 레벨과 하루 목표를 정한다. 나중에 설정에서 바꿀 수 있다.
- 알림을 켜면 매일 정한 시각에 복습할 개수를 알려 준다.
- 발음 듣기는 안드로이드 기본 TTS 를 쓴다. 소리가 안 나면
  설정 → 일반 → 텍스트 음성 변환에서 영어 음성이 설치돼 있는지 확인한다.

## 콘텐츠를 더 채운 뒤 업데이트
같은 서명 키로 빌드하므로 덮어쓰기 설치가 되고 **학습 기록은 유지된다**
(단어 데이터와 학습 기록이 별도 DB 로 분리돼 있다).
```bash
node tools/merge.mjs --partial && node tools/build-db.mjs
./gradlew :app:assembleRelease
cp app/build/outputs/apk/release/app-release.apk ~/Desktop/Wordy.apk
```

## 서명 키
`~/.android/wordy-release.keystore` 와 `keystore.properties` 에 있다.
둘 다 git 에 올라가지 않는다. **이 키를 잃어버리면 덮어쓰기 업데이트가
불가능해지므로**(지우고 다시 깔아야 하고 학습 기록도 사라진다) 다른 곳에
한 벌 백업해 두는 것이 좋다.
