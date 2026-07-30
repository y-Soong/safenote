# iOS TestFlight 배포 작업지시서

> **[2026-07-29 갱신] 진행 상태: STEP 0~6 완료 → 남은 것은 STEP 7(Codemagic 첫 빌드→TestFlight)뿐.**
>
> - STEP 0 ✅ Apple Developer 활성화 확인 완료
> - STEP 1 ✅ App ID `com.prafta.app` 등록 완료(Push Notifications 포함)
> - STEP 2 ✅ API 키 발급 완료 — .p8=`C:\Users\dudjs\Documents\apple_keys\AuthKey_9PN2VJ6246.p8`, Key ID=`9PN2VJ6246`, Issuer ID 는 콘솔 페이지 상단에서 확인
> - ✅ 07-29 iOS 준비분 4건(entitlements·pbxproj·Info.plist) 커밋·푸시 완료 (`9320f4b`)
> - ⚠️ 남은 미커밋 = `assets/vue_app/` 웹뷰 번들 재복사분(07-28 22:28 빌드, 연차 개인분모·반반차 반영).
>   iOS 배포와는 별건 — 포함 여부는 백엔드(1c1eddd3) 운영배포 여부에 따라 사용자가 결정.

> 작성: 2026-07-27 밤 | 상태: **Apple Developer 결제 완료, 활성화 대기 중단 지점**
> 목표: 맥 없이 Codemagic(클라우드 macOS)으로 iOS 빌드 → TestFlight → 실기기(아이폰, 보유) 테스트
> 코드 준비는 전부 완료됨(하단 §참고). 이 문서는 **STEP 0부터 순서대로** 진행하면 된다.
> ✋표시 = 사용자 수동(웹/터미널), 🤖표시 = Claude에게 맡길 것.

---

## STEP 0. ✅ Apple Developer 활성화 확인 — 완료(07-29)

결제 후 활성화까지 **몇 시간~최대 48시간** 걸린다 (2026-07-27 밤 결제함 → 07-28 중 활성화 예상).

확인 방법 (아래 2개가 모두 되면 활성화 완료):

1. https://developer.apple.com/account 로그인 → 상단에 **"멤버십 세부 사항"이 보이고 만료일이 표시**되면 활성.
   - 아직이면 "Pending" 또는 구매 안내 화면이 계속 뜸 → 더 기다리기.
2. https://appstoreconnect.apple.com 로그인이 되고 대시보드가 열리면 활성.

체크 포인트:
- [ ] Apple에서 영수증 메일 수신 확인 (결제 자체가 성공했는지)
- [ ] "Welcome to the Apple Developer Program" 메일 수신 = 활성화 완료 신호
- [ ] **48시간(07-29 밤) 지나도 활성화 안 되면**: https://developer.apple.com/contact/ 에서 문의 (한국어 전화 지원 있음). 흔한 원인 = 신원 확인 보류(신분증 요청 메일이 스팸함에 있을 수 있음 — 스팸함 확인).

---

## STEP 1. ✅ 번들ID(App ID) 등록 — 완료(07-29)

1. developer.apple.com → **Certificates, Identifiers & Profiles → Identifiers → [+]**
2. **App IDs → App** 선택 → Continue
3. Description: `PRAFTA` / Bundle ID: **Explicit** = `com.prafta.app`
4. Capabilities 목록에서 **Push Notifications 체크** ← ★빼먹으면 푸시 불가
5. Continue → Register

- [x] App ID `com.prafta.app` 등록 완료

## STEP 2. ✅ App Store Connect API 키 발급 — 완료(07-29)

1. appstoreconnect.apple.com → **사용자 및 액세스 → 통합(Integrations) → App Store Connect API → 팀 키 → [+]**
2. 이름 `codemagic` / 역할(액세스): **App Manager**
3. **⚠️ .p8 파일 다운로드는 단 1회만 가능** — 즉시 다운로드해서 안전한 곳 보관 (git 금지, `PRAFTA_FLUTTER` 폴더 밖에 둘 것)
4. **Key ID**(키 행에 표시) + **Issuer ID**(페이지 상단) 메모

- [x] .p8 확보 → **`C:\Users\dudjs\Documents\apple_keys\AuthKey_9PN2VJ6246.p8`** 보관
- [x] Key ID = **9PN2VJ6246**
- [ ] Issuer ID — 팀 키 페이지 상단에서 복사(STEP 3 Codemagic 등록 때 필요)
- 발급 팁: 액세스 역할은 드롭다운에서 **클릭 선택**해야 생성 버튼 활성화(타이핑 무효)

## STEP 3. ✅ Codemagic 가입·연결 — 완료(07-29)

1. https://codemagic.io → **GitHub 계정으로 가입** → Add application → `y-Soong/safenote_flutter` 선택
   - 온보딩 스캔에서 "no mobile application" 경고 시 **Project path = `safenote`** 입력 후 Retry (루트 `.` 이 아님)
2. 현 UI 경로: **왼쪽 사이드바 Settings(팀 설정) → Integrations → Developer Portal** → API 키 등록:
   - .p8 파일 업로드 + Key ID + Issuer ID 입력
   - **등록 이름 = `prafta-asc`** ← ★codemagic.yaml의 `integrations.app_store_connect` 값과 글자 단위 일치 필수.

- [x] 저장소 연결(Individual/Personal Account) + GitHub 통합 완료
- [x] Developer Portal 통합 `prafta-asc` 등록 완료(1 key connected, 이름 일치 확인)

## STEP 4. ✅ App Store Connect 앱 생성 — 완료(07-29)

1. appstoreconnect.apple.com → **나의 앱 → [+] → 신규 앱**
2. 플랫폼 iOS / 이름 **프라프타** (App Store 전체에서 고유해야 함 — 중복 오류 시 `PRAFTA`로) / 기본 언어 한국어 / 번들 ID `com.prafta.app` 선택 / SKU `prafta-app`
3. 생성만 하면 됨 — 스토어 등록정보(스크린샷·설명)는 TestFlight 테스트 후 작성해도 무방

- [x] 앱 레코드 생성 완료

## STEP 5. ✋ Firebase iOS 등록 (윈도우 터미널)

```powershell
npm install -g firebase-tools
firebase login                       # 브라우저 열림 — aqswgg@gmail.com 로그인
dart pub global activate flutterfire_cli
cd C:\PRAFTA\PRAFTA_FLUTTER\safenote
flutterfire configure --platforms=ios
```

- 프로젝트 선택: 기존 PRAFTA Firebase 프로젝트
- iOS bundle id 물으면: `com.prafta.app`
- 성공하면 `lib/firebase_options.dart` 생성됨

- [x] firebase_options.dart 생성 확인(iOS 전용, safenote-b9e88 / com.prafta.app / appId `...633a49`)
- [x] 🤖 main.dart 수정 완료(커밋 `8913a36` 푸시됨): iOS=DefaultFirebaseOptions.ios, 안드로이드=기존 google-services.json 유지(options=null), 백그라운드 핸들러 try/catch 추가
- 팁: `flutterfire` 미인식 시 Pub Cache bin(`%LOCALAPPDATA%\Pub\Cache\bin`) PATH 등록 필요(07-29 등록해둠)

> **[07-29 정정]** "이 수정 없이 iOS 기동 시 크래시"는 과장이었다. `main.dart` 의 `Firebase.initializeApp()`
> 은 try/catch 로 감싸여 있어 실패해도 앱은 기동하고 **FCM 만 조용히 죽는다**. 다만 백그라운드 핸들러
> `_firebaseMessagingBackgroundHandler` 의 `initializeApp()` 은 try/catch 가 없어 **푸시 수신 시 해당
> isolate 가 죽는다**. 그러므로 수정은 여전히 필요하다.

## STEP 6. ✅ APNs 키 발급 → Firebase 업로드 — 완료(07-29)

1. developer.apple.com → **Keys → [+]** → 이름 `prafta apns`(★하이픈 불가, 영숫자·공백만) / **APNs 체크 + Configure 로 환경(Sandbox & Production) 구성 필수** → Register → **.p8 다운로드(1회만!)** + Key ID 메모
2. https://console.firebase.google.com → SAFENOTE(safenote-b9e88) 프로젝트 → ⚙️프로젝트 설정 → **클라우드 메시징 탭** → iOS 앱 섹션 → **APNs 인증 키 업로드**: .p8 + Key ID + **Team ID**

- [x] APNs 키 발급·확보 → **`C:\Users\dudjs\Documents\apple_keys\AuthKey_8567AS3S7K.p8`** / Key ID = **8567AS3S7K** / Team ID = **4H6L36ZJYN**
- [x] Firebase 콘솔 업로드 완료 — **★개발·프로덕션 두 슬롯 모두** 같은 .p8 등록(TestFlight=production 토큰이라 프로덕션 슬롯 필수; 개발 슬롯만 채우면 푸시 전멸)
- 함정 메모: "APN 인증서" 섹션의 업로드(.p12 요구)와 혼동 주의 — .p8 은 **"APN 인증 키"** 섹션. 키 창 구분법=키 ID/팀 ID 필드 유무

## STEP 7. 🤖+✋ 첫 TestFlight 빌드

> **[07-29 서명 방식 전환]** 자동 서명(ios_signing.distribution_type)은 빌드머신 할당 전 사전검증에서
> "No matching profiles found"로 즉사(#1 App Manager 키·#2 Admin 키 모두 동일, 단계 로그 없음).
> 로컬에서 ASC API 직접 호출로 확인 결과 **Admin 키(779J4U48X4)는 인증서 생성 가능(HTTP 201)** —
> 즉 Codemagic 자동서명 쪽 문제로 판단, **수동 서명으로 전환**(커밋 `70b42a4`):
> - 인증서: `apple_keys\prafta_ios_distribution.p12` (비번 `prafta-ios-sign-2026`, 만료 2027-07-29, cert id K4Q58KDQAQ)
> - 개인키 원본: `apple_keys\dist_cert_private_key.pem` / 프로파일: `apple_keys\prafta_appstore.mobileprovision` (id 4D6K3367U7)
> - Codemagic UI 참조명: **prafta_dist_cert** / **prafta_appstore_profile** (yaml 과 글자 단위 일치 필수)
> - 진단 스크립트 재료: 세션 스크래치패드 asc_diag.py·asc_profile.py (Issuer ID 7859439e-c27f-480e-bfb4-b6599b0d217d)

1. Codemagic → safenote_flutter → **Start new build** → workflow `ios-testflight` 선택 → 실행 (15~30분)
2. 성공하면 TestFlight 자동 업로드 → App Store Connect → TestFlight 탭에서 빌드 확인 ("내부 테스트" 그룹에 본인 추가)
3. 아이폰에 **TestFlight 앱** 설치(앱스토어) → 초대 수락 → PRAFTA 설치
4. 스모크 테스트: 로그인 / 홈 / 위치권한 팝업 문구 / QR 스캔(카메라 권한) / 푸시 수신
5. 빌드 실패 시: 로그 캡처해서 Claude에게 → 원인 분석 (첫 빌드는 CocoaPods/서명에서 한두 번 튀는 게 보통)

---

## 참고: 이미 완료된 것 (2026-07-27)

| 항목 | 상태 |
|---|---|
| iOS 번들ID | `com.example.safenote`(템플릿 잔재) → `com.prafta.app` 수정 완료 |
| 배포 타깃 | 13.0 → 15.5 (mobile_scanner 6.x 요구) |
| Info.plist | 카메라·위치 권한 문구(기존재) + 암호화 문답 생략 + ATS 예외(localhost 번들서버·웹뷰 http) |
| codemagic.yaml | 저장소 루트에 작성 완료 (`working_directory: safenote`) |
| GitHub | 전부 push 완료 (`cdccb23`) |
| Apple Developer | **Individual $99 결제 완료 — 활성화 대기 ← 현재 위치** |

## 참고: 07-29 추가 코드 준비 (✅ 커밋·푸시 완료 `9320f4b`)

07-27 준비분에 빠져 있던 항목을 점검 중 발견해 보완했다.

| 파일 | 변경 | 없으면 생기는 일 |
|---|---|---|
| `ios/Runner/Runner.entitlements` | **신규 생성**, `aps-environment = production` | 빌드는 성공하는데 **APNs 토큰이 안 나와 푸시가 조용히 죽음**. STEP 1 Push 체크·STEP 6 APNs 키를 다 해도 무용 |
| `ios/Runner.xcodeproj/project.pbxproj` | Runner 타깃 **Debug/Release/Profile 3곳 모두**에 `CODE_SIGN_ENTITLEMENTS = Runner/Runner.entitlements;` | 위 entitlements 파일이 빌드에 적용되지 않음 |
| `ios/Runner/Info.plist` | `NSPhotoLibraryUsageDescription` 추가 | 웹뷰 `<input type="file">` 사진 첨부(아차사고 보고 등) 시 **iOS 가 앱 강제 종료** + 심사 리젝 |
| `ios/Runner/Info.plist` | `UIBackgroundModes = [remote-notification]` 추가 | 백그라운드 푸시가 앱을 깨우지 못함(`onBackgroundMessage` 무력) |

메모:
- `aps-environment` 는 **Codemagic app_store 배포 서명 전용이라 `production` 고정**. 개발 서명으로 실기기 디버그를 하게 되면 `development` 로 바꿔야 토큰이 나온다(파일 내 주석에도 기재).
- Xcode 파일 네비게이터용 파일 참조(PBXFileReference)는 **일부러 추가하지 않았다.** 빌드는 `CODE_SIGN_ENTITLEMENTS` 설정만으로 동작하고, UUID 를 손으로 만들어 넣으면 pbxproj 손상 위험이 크다.
- 두 plist 는 파서로 검증 완료(한글 문구 정상).
- `ios/Podfile` 은 아직 없다(맥에서 빌드한 적이 없어서). Codemagic 이 빌드 시 생성하는데, 템플릿 기본 iOS 버전이 15.5 미만이면 `mobile_scanner` 에서 CocoaPods 오류가 난다 → STEP 7 이 예고한 "첫 빌드 튐"이 이것일 가능성이 높다. 실제 오류 메시지 보고 잡는다.

---

주의사항:
- 개인(Individual) 계정이라 App Store 판매자명 = 대표자 개인 이름으로 표시됨 (법인 전환 시 Organization 승격 가능)
- .p8 키 2종(API 키·APNs 키)은 재다운로드 불가 — 분실 시 재발급해야 하며 연동 재설정 필요
- 안드로이드는 07-27 심사 제출 완료 상태(별건) — Play Console 심사 결과 메일 병행 확인
