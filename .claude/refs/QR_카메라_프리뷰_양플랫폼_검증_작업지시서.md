# QR 카메라 프리뷰 양플랫폼 검증 작업지시서

> 작성: 2026-07-29 밤 | 상태: **✅ 종결 (2026-08-01)** — 양플랫폼 실기기 검증 완료, 하단 §7 최종 결론 참조
> 관련 메모리: `project_prafta_ios_release_prep`, `feedback_qr_scanner_which_component_and_html5qrcode_css` / 선행 문서: `iOS_TestFlight_배포_작업지시서.md`
> ⚠️ §1~§6 의 mobile_scanner 조사는 **오진 경로**였다(순회점검 QR 은 Flutter 가 아니라 웹뷰 Vue 스캐너). 기록 보존용으로만 남긴다.

---

## 1. 현재 증상 (2026-07-29 실기기 기준)

| 플랫폼 | mobile_scanner | 증상 |
|---|---|---|
| iOS (TestFlight) | 6.0.11 (구빌드) | 프리뷰 **아래 절반 검정** + 검정 영역 스캔 불가 체감 |
| iOS (신빌드) | 7.4.0 | **미검증** — 7.4 반영 Codemagic 빌드를 아직 안 돌림 |
| Android (로컬 APK) | 6.0.11 (운영 v3/v4) | 정상 (전체 화면) |
| Android (로컬 APK) | 7.4.0 | **회귀: 아래 1/4 가량 검정** (카메라 자체는 나옴) |

핵심 단서: **6.0.11=안드 정상/iOS 절반검정, 7.4.0=안드 1/4검정(iOS 미확인)**.
→ 버전마다 프리뷰 사이즈 처리(FittedBox에 넘기는 카메라 보고 사이즈)가 달라 화면비 처리가 갈리는 것.
→ 목표는 **양플랫폼 동시에 전체화면 프리뷰가 되는 단일 버전(또는 코드 보정)** 찾기. pubspec은 하나라 버전 분기 불가.

## 2. 오늘까지 반영된 변경 (모두 커밋·푸시 완료, safenote_flutter@main)

- `a9a8fc3` — mobile_scanner **6.0.11 → 7.4.0** (iOS 절반검정 대응 목적)
- `a3cc292` — **AGP 8.6.0 → 8.9.1 + Gradle 래퍼 8.7 → 8.11.1** (7.4의 androidx.camera 1.6.1 요구)
  - Android release APK 빌드 성공(63MB)·`libapp.so`에 APP_BASE_URL 주입 검증됨
  - ⚠️ 되돌릴 경우(6.x 복귀) AGP/Gradle 은 **그대로 둬도 무해** (하위호환)
- 참고(iOS 라인): `0ef96c4` Podfile(PERMISSION_CAMERA=1) / `672f4da` iOS 빌드번호 `PROJECT_BUILD_NUMBER+100`

빌드 명령(안드 로컬 검증):
```powershell
cd C:\PRAFTA\PRAFTA_FLUTTER\safenote
flutter build apk --release --dart-define=APP_BASE_URL=https://api.prafta.com
# 산출물: build\app\outputs\flutter-apk\app-release.apk
```

## 3. 내일 작업 순서 (제안)

1. **코드 보정 먼저 시도 (버전 유지 7.4.0)** — `lib/qr_scan_page.dart`
   - `MobileScanner(fit: BoxFit.cover, ...)` 명시 (6.x/7.x 기본값 동일하다지만 명시 후 재확인)
   - 안 되면 `LayoutBuilder`+`SizedBox.expand` 로 강제 전체화면 래핑, `controller.value.size` 로그 출력해
     플랫폼이 보고하는 프리뷰 사이즈(가로세로 스왑 여부) 실측
2. **mobile_scanner GitHub 이슈 조사** — "7.4 preview black bottom / aspect ratio" 검색.
   7.4.0 회귀 이슈가 있으면 수정 버전/워크어라운드 채택
3. **버전 스윕(코드 보정 실패 시)** — 7.3.x → 7.2.x → 7.1.3 순으로 내리며 안드 APK 실기기 확인
   (7.1.3에 "프리뷰 크기 수정" 항목 있음. 각 버전의 androidx.camera 요구 확인 — AGP 8.9.1이면 대부분 충족)
4. **안드 확정 후 iOS 검증** — Codemagic Start new build(main/`ios-testflight`) → TestFlight 업데이트
   → QR 전체화면 + 실제 스캔(TBM 입실 등) 확인
5. **최종 기준**: 양플랫폼 모두 ①프리뷰 전체화면 ②실스캔 인식 ③기존 기능(출퇴근/웹뷰) 무영향

## 4. 판단 기준 / 롤백 경로

- 단일 버전으로 양쪽 만족이 목표. 끝내 못 찾으면:
  - 최후수단 A: `fit`/래핑 코드로 시각 보정만이라도 확보(스캔 자체는 프레임 전체로 동작하는지 실측 필요)
  - 최후수단 B: 6.0.11 복귀(안드 우선) + iOS는 절반검정 감수하고 이슈 수정 버전 대기 — **비권장**, TestFlight 단계라 iOS 품질 우선순위 높음
- 안드 운영 영향: 심사중 v3·대기중 AAB v4는 기빌드 산출물이라 **이번 변경 무영향**. 다음 안드 릴리즈부터 반영됨.

## 5. 같이 남아있는 잔여 항목 (이번 건 마무리 후)

- [ ] **iOS 첫 세션 화면이동 전멸(재현 1회, 미해결)** — 재발 시 eruda 콘솔 주입 빌드로 계측
  (배제됨: 잔류 오버레이·overlayBack 죽은코드·가드 API. 후보: 첫 실행 권한 프롬프트 타이밍/동적 라우트 주입/키보드 viewport 오프셋.
  발견사항: 앱 전역 $alert/$confirm 미등록 → window.alert/confirm 폴백 동작 중)
- [ ] **iOS 푸시 수신 실검증** (entitlements→APNs→Firebase 체인 실전 미확인)
- [ ] TestFlight 테스트 정보 기입 확인 — https://appstoreconnect.apple.com/apps/6795914871/testflight/test-info
  (미기입 시 빌드마다 베타심사 제출 단계만 실패 — 내부 테스트에는 무영향)
- [ ] 미커밋 `assets/vue_app/` 번들 재복사분(07-28 22:28 빌드) 커밋 여부 결정 — 백엔드 1c1eddd3 운영배포 여부에 따름
- [ ] 안전관리 쪽 실사용 시나리오(안전점검 QR→체크리스트) 및 관리자 화면 스모크

---

## 6. 2026-07-30 진행 결과 (원인 규명 + 픽스)

### 원인 (mobile_scanner 소스 실측 — pub cache 6.0.11 vs 7.4.0 비교)

- **안드 7.4.0 하단 1/4 검정**: 네이티브(`MobileScanner.kt`)가 Flutter 에 보고하는 프리뷰 사이즈가
  Preview 유즈케이스가 아니라 **ImageAnalysis 해상도**(`analysis.resolutionInfo`). 7.4.0 은 미지정 시
  `Size(1920,1080)`(16:9)을 강제하는데 실제 Preview 스트림은 CameraX 기본 4:3.
  → 4:3 콘텐츠가 16:9 선언 박스에 폭 기준으로 들어가 높이 75%만 채움 = **정확히 하단 1/4 검정**.
- **6.0.11 안드 정상이었던 이유**: 해상도 미지정 시 analysis 기본 640x480(4:3)이라 우연히 Preview(4:3)와 일치.
- **iOS 6.0.11 절반검정**: 구식 `sessionPreset=.photo`(4:3) 구현 문제. 7.4.0 은 `.high`(16:9)+
  `activeFormat` 실측 치수 보고로 전면 개편 → **iOS 는 7.4.0 자체로 해소 기대** (Codemagic 빌드로 확인).
- iOS 카메라 재검토(사용자 요청): Info.plist `NSCameraUsageDescription` / Podfile `PERMISSION_CAMERA=1`(0ef96c4) /
  SCAN_QR 브리지 권한 게이트(web_app.dart) / `iframeAllow: camera` 모두 정상 확인.

### 반영 (커밋 `deebe3c`, safenote_flutter@main — **미푸시**)

`lib/qr_scan_page.dart`:
- `cameraResolution: Size(1440,1080)` (4:3, **안드 전용 옵션** — iOS 는 무시하므로 부작용 없음)
- `fit: BoxFit.cover` 명시 (기본값과 동일하나 의도 명시)
- `[QR_PREVIEW] size=WxH orientation=...` 1회 로깅 (adb logcat 으로 실기기 확인용)

APK 빌드 완료: `build\app\outputs\flutter-apk\app-release.apk` (63MB, api.prafta.com 주입 검증됨)

### 다음 단계

1. **안드 실기기**: 위 APK 설치 → QR 프리뷰 전체화면 여부 + 실스캔 확인.
   실패 시 `adb logcat | findstr QR_PREVIEW` 로 보고 사이즈 확보 (4:3 이 나오는데도 검정이면 다른 원인).
2. 안드 OK → push → **Codemagic Start new build**(main/ios-testflight) → TestFlight 에서 iOS 프리뷰/스캔 확인.
3. 실패 시 폴백: 버전 스윕 7.3.x→7.2.x→7.1.3 (§3-3) 또는 최후수단 §4.

---

## 7. 최종 결론 (2026-07-31 ~ 08-01, 종결)

### 진짜 원인 — 순회점검 QR 은 mobile_scanner 가 아니었다

문제의 화면(순회점검 QR·일용직 QR)은 Flutter 네이티브 스캐너가 아니라 **웹뷰 안 Vue 스캐너**
(`prafta-app-frontend` 의 `QrScanner.vue` / `AdminSiteOpsView.vue`, html5-qrcode)였다.
§1~§6 의 mobile_scanner 버전 조사·cameraResolution 픽스(deebe3c)는 이 증상과 무관
(SCAN_QR 브리지 경로에는 해가 없어 커밋은 유지). 감별법·재발 방지는 메모리
`feedback_qr_scanner_which_component_and_html5qrcode_css` 에 집약.

### 해결된 결함 3건 (모두 실기기 검증 완료)

| 날짜 | 증상 | 원인 | 픽스 커밋 (웹 repo / Flutter repo) |
|---|---|---|---|
| 07-31 | 화면 절반 검정(스캔은 됨) | html5-qrcode 가 컨테이너에 인라인 `position:relative` 를 박아 absolute 무효화 → 컨테이너 auto 높이 | `0a760cd7` / `3c1b798` (`!important`) |
| 07-31 | 회귀: 꽉 차는데 인식 불가(iOS) | video 를 CSS 로 늘리면 디코딩 좌표(레이아웃 크기=프레임 화면비 전제)가 깨짐 | `5f79b09f` / `555ff8e` (transform scale, `utils/qrPreviewCover.js`) |
| 08-01 | 갤럭시 화면 전체 검정, 안내 없음 | **네이티브 CAMERA 권한 부재**. 안드 웹뷰가 이를 NotReadableError 로 보고 + `start()` hang 으로 폴백 미발동 + getCameras() 라벨 매칭이 전면 카메라로 오폴백 | `aa4aad57` / `31a07b4` |

### 08-01 픽스 내용 (최종 구조)

- **`REQUEST_CAMERA_PERMISSION` 브리지 신설**(web_app.dart): 웹이 getUserMedia 전에 네이티브
  권한 선확인/요청. `{status:GRANTED|DENIED|PERMANENTLY_DENIED}`. permission_handler 공용이라 iOS 동일.
- **`utils/qrCameraStart.js` 공용 유틸**: `facingMode:{exact:'environment'}` 로 카메라 1회만 오픈
  (Overconstrained 시 비강제 재시도) + 8초 타임아웃(hang→실패 전환, 유령 세션 stop) + 실패 사유
  분류(denied/busy/error). html5-qrcode 가 에러를 문자열로 감싸 err.name 이 소실되므로 문자열 매칭 병행.
- **`SafetyCameraPermissionView` 사유별 분기**: denied=[설정으로 이동] / busy·error=[다시 시도].
- 진단 인프라: 안드 chrome://inspect 는 `isInspectable`(iOS 전용)이 아니라
  `setWebContentsDebuggingEnabled` 필요 → `WEBVIEW_DEBUG` dart-define + `build-apk.ps1 -WebviewDebug`
  로 게이트(그 APK 는 배부 금지).

### 검증 결과

- 갤럭시 S25U: 프리뷰 전체화면 + 실스캔 인식 + (제출 전 확인 항목: 권한 거부 폴백) — 08-01 확인
- iOS TestFlight(31a07b4 빌드): 이상 없음 — 08-01 사용자 확인

### 잔여 (이 건 아님 — 다른 트랙)

- iOS 앱스토어 정식 제출 → `iOS_AppStore_제출_가이드.md` 로 진행 중
- §5 의 기존 잔여(iOS 첫 세션 화면이동 관찰·푸시 실검증 등)는 그대로 유효
