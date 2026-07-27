# iOS TestFlight 배포 작업지시서

> 작성: 2026-07-27 밤 | 상태: **Apple Developer 결제 완료, 활성화 대기 중단 지점**
> 목표: 맥 없이 Codemagic(클라우드 macOS)으로 iOS 빌드 → TestFlight → 실기기(아이폰, 보유) 테스트
> 코드 준비는 전부 완료됨(하단 §참고). 이 문서는 **STEP 0부터 순서대로** 진행하면 된다.
> ✋표시 = 사용자 수동(웹/터미널), 🤖표시 = Claude에게 맡길 것.

---

## STEP 0. ✋ Apple Developer 활성화 확인 ← ★내일 여기부터

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

## STEP 1. ✋ 번들ID(App ID) 등록 — developer.apple.com

1. developer.apple.com → **Certificates, Identifiers & Profiles → Identifiers → [+]**
2. **App IDs → App** 선택 → Continue
3. Description: `PRAFTA` / Bundle ID: **Explicit** = `com.prafta.app`
4. Capabilities 목록에서 **Push Notifications 체크** ← ★빼먹으면 푸시 불가
5. Continue → Register

- [ ] App ID `com.prafta.app` 등록 완료

## STEP 2. ✋ App Store Connect API 키 발급

1. appstoreconnect.apple.com → **사용자 및 액세스 → 통합(Integrations) → App Store Connect API → 팀 키 → [+]**
2. 이름 `codemagic` / 역할(액세스): **App Manager**
3. **⚠️ .p8 파일 다운로드는 단 1회만 가능** — 즉시 다운로드해서 안전한 곳 보관 (git 금지, `PRAFTA_FLUTTER` 폴더 밖에 둘 것)
4. **Key ID**(키 행에 표시) + **Issuer ID**(페이지 상단) 메모

- [ ] .p8 다운로드 + Key ID + Issuer ID 3종 확보

## STEP 3. ✋ Codemagic 가입·연결

1. https://codemagic.io → **GitHub 계정으로 가입** → Add application → `y-Soong/safenote_flutter` 선택
   (codemagic.yaml은 저장소 루트에 푸시돼 있음 — 자동 인식됨)
2. **Teams → Personal Account → Integrations → Developer Portal (App Store Connect)** → API 키 등록:
   - .p8 파일 업로드 + Key ID + Issuer ID 입력
   - **등록 이름 = `prafta-asc`** ← ★codemagic.yaml의 `integrations.app_store_connect` 값과 글자 단위 일치 필수.
     다른 이름으로 등록했으면 Claude에게 알려서 yaml 수정.

- [ ] 저장소 연결 + API 키 통합(`prafta-asc`) 완료

## STEP 4. ✋ App Store Connect 앱 생성

1. appstoreconnect.apple.com → **나의 앱 → [+] → 신규 앱**
2. 플랫폼 iOS / 이름 **프라프타** (App Store 전체에서 고유해야 함 — 중복 오류 시 `PRAFTA`로) / 기본 언어 한국어 / 번들 ID `com.prafta.app` 선택 / SKU `prafta-app`
3. 생성만 하면 됨 — 스토어 등록정보(스크린샷·설명)는 TestFlight 테스트 후 작성해도 무방

- [ ] 앱 레코드 생성 완료

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

- [ ] firebase_options.dart 생성 확인
- [ ] 🤖 **여기서 Claude 호출**: "flutterfire configure 끝났어" → main.dart Firebase 초기화를 DefaultFirebaseOptions 방식으로 수정 + 커밋·푸시 (현재 코드는 이 수정 없이 iOS 기동 시 크래시)

## STEP 6. ✋ APNs 키 발급 → Firebase 업로드 (푸시)

1. developer.apple.com → **Keys → [+]** → 이름 `prafta-apns` / **APNs 체크** → Register → **.p8 다운로드(1회만!)** + Key ID 메모
2. https://console.firebase.google.com → PRAFTA 프로젝트 → ⚙️프로젝트 설정 → **클라우드 메시징 탭** → iOS 앱 섹션 → **APNs 인증 키 업로드**: .p8 + Key ID + **Team ID**
   (Team ID = developer.apple.com/account 멤버십 세부 사항에 표시되는 10자리)

- [ ] APNs 키 Firebase 업로드 완료

## STEP 7. 🤖+✋ 첫 TestFlight 빌드

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

주의사항:
- 개인(Individual) 계정이라 App Store 판매자명 = 대표자 개인 이름으로 표시됨 (법인 전환 시 Organization 승격 가능)
- .p8 키 2종(API 키·APNs 키)은 재다운로드 불가 — 분실 시 재발급해야 하며 연동 재설정 필요
- 안드로이드는 07-27 심사 제출 완료 상태(별건) — Play Console 심사 결과 메일 병행 확인
