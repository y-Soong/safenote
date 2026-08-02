# PRAFTA iOS App Store 정식 제출 가이드 (2026-07-31 작성, 08-02 제출 완료)

> TestFlight 배포는 이미 동작 중이다(빌드 110까지 실기기 확인 완료).
> 이 문서는 **거기서부터 App Store 정식 출시까지**를 다룬다.
> TestFlight 까지의 계정·인증서·CI 세팅은 `iOS_TestFlight_배포_작업지시서.md` 참조.
>
> 표기: ✋=사용자만 가능 / 🤖=Claude 처리 가능 / ✅=완료

---

## ✅ 제출 완료 (2026-08-02) — 현재 상태

| 항목 | 값 |
|---|---|
| 상태 | 🟡 **심사 대기 중 (Waiting for Review)** |
| 제출 항목 | iOS 앱 1.0.0 **(빌드 116)** |
| 제출 일시 | 2026-08-02 16:57 |
| 제출 ID | `51a8d5df-3d0e-491d-aff6-e5bcf75ec6f8` |
| 출시 방식 | **수동 출시** — 승인돼도 버튼을 눌러야 공개된다 |
| 데모 계정 | `SOON` (스크린샷을 찍은 계정과 동일) |

**다음 할 일 = 심사 결과 대기** (보통 24~48시간, 신규 개발자 첫 앱은 더 걸릴 수 있음).
승인 시 버전 페이지에서 출시 버튼을 누른다(§11). 리젝 시 Resolution Center 사유 → §10 대응표.

### 빌드 116 = 115(검증본) + 로그인 화면 문의처 교체

빌드 115(Flutter `31a07b4`)로 QR·GPS·권한 거부 경로 실기기 검증을 마친 뒤,
로그인 화면 푸터의 **더미 번호 `고객센터 1234-5678`** 을 발견해 `dudjswp@gmail.com` 으로 교체했다.
플레이스홀더는 **가이드라인 2.1(App Completeness)** 리젝 사유이고 심사자가 반드시 보는 화면이다.
커밋 = `2de99080`(웹 repo) / `454bbf7`(Flutter 번들). 이걸 실어 빌드한 것이 **116**.

### 제출 전 함께 잡은 데이터측 결함 2건

- **TBM 목록에 E2E 테스트 데이터 노출** (`[T5 E2E] 연동 세션 검증` 등) →
  운영·개발 `tb_tbm_session` TITLE UPDATE 로 교체. **스크린샷 촬영 전에 데이터부터 손볼 것**
  (촬영 → 발견 → 재촬영 왕복이 실제로 발생했다).
- **데모 계정 불일치** — 로그인 정보란 `ADMIN`(AUTH_CD `master`) vs 비고 `SOON`.
  심사자는 입력란 계정을 쓰므로 관리자 UI 를 보게 되어 스크린샷과 불일치(2.3) → `SOON` 으로 통일.
  같은 맥락으로 비고의 "site with a relaxed radius" 도 사실과 달라(실제로는 범위 밖 → 외근 등록
  폴백) 실제 동작 설명으로 교체하고, **한국어 UI 하단 탭바 영문 안내**를 추가했다.

---

## ⚠️ 본문 중 실제 콘솔과 달랐던 부분 (다음 버전 제출 시 반드시 읽을 것)

1. **스크린샷 규격 — §3 의 "6.9인치(1290×2796)" 는 틀렸다.**
   실제 슬롯은 **`iPhone 6.5 디스플레이`** 이고 요구 규격은
   `1242×2688 / 2688×1242 / **1284×2778** / 2778×1284`.
   요구 규격은 **콘솔 화면 표기가 유일한 진실**이므로 문서를 믿지 말고 화면을 볼 것.
   - 변환본: `OneDrive\바탕 화면\prafta\APP_이미지\ios\appstore_1284x2778\` (01~06)
   - **알파 채널이 있으면 업로드가 거부된다** → 24bpp RGB 로 저장할 것
   - 원본 1170×2532(6.1인치 실기기)와 화면비가 거의 같아 단순 확대로 대응 가능
2. **개인정보처리방침 URL 은 §4(앱 정보)가 아니라 §7(App Privacy) 페이지 상단**에 있다.
3. **버전 레코드는 반드시 `1.0.0`.** `1.0` 으로 만들면 빌드 선택 목록에 빌드가 아예 안 뜬다
   (`pubspec: 1.0.0+4` → `CFBundleShortVersionString = 1.0.0` 과 정확히 일치해야 함).
   제출 전에는 버전 페이지에서 편집 가능.
4. **가격 기준통화(대한민국 KRW) ≠ 배포 국가 제한.**
   배포 국가는 `가격 및 사용 가능 여부 → 앱 사용 가능 여부` 에서 따로 설정한다(기본 175개국).
5. **검증 오류가 하나라도 떠 있으면 페이지 저장 자체가 막혀 입력한 텍스트가 통째로 날아간다.**
   (스크린샷 규격 오류 상태에서 설명·키워드·심사정보를 전부 유실했다.)
   → **한 덩어리 입력 → 즉시 저장**을 반복할 것.
6. **`심사에 추가` 를 막는 것은 대개 앱 정보 쪽**이다 — 콘텐츠 권한 + 연령 등급 설문.

### 콘텐츠 권한 · 연령 등급 답안 (→ 최종 4+)

- **콘텐츠 권한 = 예**(타사 콘텐츠 포함·권한 보유). 카카오맵을 렌더링하고 고객사 업로드
  문서를 표시하므로 "아니요"는 부정확하다.
- **제한되지 않은 웹 액세스 = 아니요** — 웹뷰지만 앱 번들 내부 화면과 자사 API 만 로드한다.
  "예"로 답하면 등급이 17+ 로 올라간다.
- **사용자 생성 콘텐츠 = 아니요** — 문항 정의가 "배포"(공개 피드)다. PRAFTA 는 같은 회사
  안에서만 오가는 업무 기록. **"예"로 답하면 신고·차단·모더레이션 구현을 요구받아 오히려 리젝된다.**
- 소셜 미디어 / 13세 미만 비활성화 / 메시지 및 채팅 / 광고 = **아니요**
- **의료 또는 건강 = 없음** — 산업안전은 의료가 아니다. "자주" 등을 고르면 규제 대상
  의료기기 신고 절차로 빠진다.

### 한국어 콘솔 ↔ 본문 STEP 매핑

| 본문 | 콘솔 메뉴 |
|---|---|
| §4 STEP 3 앱 정보 | 일반 정보 → **앱 정보** (+ 콘텐츠 권한 · 연령 등급) |
| §5 STEP 4 가격 | 수익화 → **가격 및 사용 가능 여부** |
| §6 STEP 5 버전 등록정보 | iOS 앱 → **1.0.0 제출 준비 중** (스크린샷·설명·빌드) |
| §7 STEP 6 App Privacy | 신뢰 및 안전 → **앱이 수집하는 개인정보** |
| §8 STEP 7 앱 심사 정보 | 버전 페이지 하단 **앱 심사 정보** |
| §9 STEP 8 제출 | 버전 페이지 우측 상단 **심사에 추가** |

---

## 0. 현재 상태

**이미 끝난 것**

| 항목 | 상태 |
|---|---|
| Apple Developer 계정 · App ID `com.prafta.app` | ✅ |
| App Store Connect 앱 레코드 "프라프타" (Apple ID `6795914871`) | ✅ |
| Codemagic → TestFlight 자동 업로드 파이프라인 | ✅ |
| 개인정보처리방침 공개 URL `https://prafta.com/privacy` | ✅ |
| 앱 내 계정 삭제(탈퇴) 기능 — 가이드라인 5.1.1(v) 요건 | ✅ |
| 수출규정 `ITSAppUsesNonExemptEncryption=false` (Info.plist) | ✅ |
| 심사용 데모 계정 (운영 DB 실재·로그인 확인) | ✅ |
| 심사 리스크 코드 수정 3건 (아래 §1) | ✅ 커밋 `358a044` |
| QR 스캐너 권한 폴백 개선 — 권한 선확인 브리지 + 거부/점유/실패 사유별 안내·재시도 | ✅ 08-01 커밋 `31a07b4`, 갤럭시·TestFlight 양쪽 실기기 확인 |
| STEP 1 빌드 업로드 + TestFlight 실기기 확인 | ✅ 08-01 (31a07b4 빌드 이상 없음) |

**남은 것** = §2 권한 거부 경로 최종 확인 → 스크린샷 → 콘솔 입력 → 제출. 이 문서 §3~§9.

---

## 1. TestFlight 와 App Store 심사는 기준이 다르다

TestFlight 는 **자동 검사만** 통과하면 배포된다. App Store 는 **사람이 앱을 직접 써 본다.**
그래서 TestFlight 가 잘 돌아도 정식 심사에서 리젝될 수 있고, 실제로 이 앱에는 리스크가 있었다.

2026-07-31 에 선제 수정한 3건 (커밋 `358a044`):

1. **권한 하드 게이트 → 소프트 게이트** — 위치·카메라를 둘 다 허용해야만 앱에 진입할 수 있었다.
   가이드라인 **5.1.1** 은 "권한을 거부해도 앱은 동작해야 하고, 핵심 기능과 무관한 데이터 접근을
   강제할 수 없다"고 요구한다. → 두 게이트에 **'나중에 하기'** 추가.
2. **iPad 지원 해제** (`TARGETED_DEVICE_FAMILY` → `1`) — iPad 로 선언되면 심사자가 iPad 에서
   테스트하고 iPad 스크린샷도 필수가 된다. 내용물이 모바일 세로 웹뷰라 불리하다.
3. **ITMS-90683 경고 제거** — Podfile 에 `BYPASS_PERMISSION_LOCATION_ALWAYS=1`.

---

## 2. STEP 1 — 빌드 올리기 ✋

1. Codemagic → Start new build → **브랜치 `main`** → 빌드.
   (빌드번호는 `PROJECT_BUILD_NUMBER + 100` 이라 다음은 **111**)
2. TestFlight 에 올라오면 아이폰에서 업데이트.

### 실기기 확인 — 권한을 "거부"하는 경로가 핵심

심사자는 정확히 이 경로를 밟는다. 하나라도 막히면 리젝이다.

- [ ] 앱 삭제 후 재설치 → 위치 권한 **거부** → '나중에 하기' → 앱 진입되는가
- [ ] 카메라도 **거부** → '나중에 하기' → 로그인 화면까지 도달하는가
- [ ] 데모 계정으로 로그인 → 홈 화면이 정상인가
- [ ] 그 상태에서 출퇴근 시도 → 위치 권한을 다시 묻거나 안내가 뜨는가 (앱이 죽으면 안 됨)
- [ ] 순회점검 QR 진입 → 카메라 권한을 다시 묻고, 거부 시 폴백 화면이 뜨는가
- [ ] 권한을 모두 허용한 정상 경로도 한 번 더 확인

> 업로드 후 Apple 이 보내는 메일에 **ITMS-90683 이 또 오는지** 확인.
> 오면 Podfile 매크로가 안 먹은 것 — 제출은 가능하나 Claude 에게 알릴 것(대체 수단 있음).

---

## 3. STEP 2 — 스크린샷 준비 ✋🤖

### 규격

App Store Connect 는 **아이폰 스크린샷 1세트**를 요구한다(요구 규격은 콘솔 화면에 표시되므로
그 표기를 따를 것). 최근 기준으로 6.9인치 세트를 올리면 나머지 크기는 자동 적용되는 방향으로
단순화됐다. **iPad 는 §1-2 로 지원을 껐으므로 불필요하다.**

⚠️ 보유 기기가 6.1인치(1170×2532)라 촬영본이 요구 규격과 다르다. 다행히 **화면비가 거의 같아**
단순 확대/미세 보정으로 규격을 맞출 수 있다 → 촬영본을 주면 🤖 가 정확한 픽셀 크기로 변환한다.

### 촬영 시나리오 (6컷 권장, 최소 2컷)

심사용 데모 계정으로, **세로**로 촬영한다. 개인정보가 실명으로 보이지 않게 데모 데이터만 쓸 것.

1. 홈 — 오늘 스케줄 + 출퇴근 버튼
2. 출근 체크 — 위치 확인 화면
3. 내 근태 캘린더 — 월 뷰
4. 연차 신청 화면
5. TBM 세션 — 참석 서명 또는 교육안
6. 안전점검 / 아차사고 목록

---

## 4. STEP 3 — 앱 정보 (App Information) ✋

App Store Connect → 앱 선택 → 좌측 **App Information**

| 항목 | 입력값 |
|---|---|
| 이름 | 프라프타 (또는 PRAFTA) |
| 부제(Subtitle, 30자) | 현장 안전·근태 통합 관리 |
| 카테고리 (기본) | **비즈니스** |
| 카테고리 (보조) | 유틸리티 (선택) |
| 콘텐츠 권한 | 해당 없음 |
| 연령 등급 | 설문 응답 → **4+** 예상 (폭력/성적 콘텐츠 없음) |
| 개인정보처리방침 URL | `https://prafta.com/privacy` |

---

## 5. STEP 4 — 가격 및 이용 가능 여부 ✋

- **가격: 무료** (앱 자체는 무료, 회사 단위 계약 서비스)
- 국가/지역: **대한민국**만으로 좁히는 것을 권장 — 심사자가 한국어 업무앱임을 이해하기 쉽고,
  타국 출시에 따른 현지화 요구를 피한다. 이후 확장 가능.
- 인앱 결제 없음 → 별도 설정 불필요

---

## 6. STEP 5 — 버전 등록정보 ✋

### 프로모션 텍스트 (170자, 심사 없이 수시 변경 가능)

```
출퇴근부터 TBM·위험성평가·안전점검까지, 현장 안전과 근태를 하나의 앱에서 관리합니다.
```

### 설명 (Description)

```
PRAFTA는 산업 현장을 위한 안전·근태 통합 관리 서비스입니다.

[주요 기능]
· GPS 기반 출퇴근 체크와 근무계획·연차 관리
· TBM(작업 전 안전회의) 진행 및 참석 기록
· 위험성평가 · 아차사고 · 순회점검 기록 관리
· 일용근로자 QR 입실과 전자 계약서 서명
· 관리자 승인과 푸시 알림으로 이어지는 결재 흐름

[이용 안내]
본 앱은 PRAFTA를 도입한 고객사 소속 근로자를 위한 업무용 앱입니다.
계정은 소속 회사 관리자로부터 발급받거나, 회사가 제공한 가입 코드를 통해 생성합니다.

[권한 안내]
· 위치: 출퇴근 판정과 현장(TBM) 입장 확인에 사용합니다. 해당 기능을 실행하는 시점에만
  수집하며, 백그라운드에서 위치를 수집하지 않습니다.
· 카메라: 점검 개소 QR 스캔과 현장 사진 촬영에 사용합니다.
· 사진: 안전점검·아차사고 기록에 사진을 첨부할 때 사용합니다.
각 권한은 허용하지 않아도 앱의 나머지 기능을 사용할 수 있습니다.
```

> 마지막 줄은 **가이드라인 5.1.1 대응을 겸한다** — 심사자가 설명만 읽어도 권한 정책을 파악한다.

### 키워드 (100자, 쉼표 구분)

```
근태,출퇴근,안전관리,TBM,위험성평가,현장,건설,산업안전,연차,전자결재
```

### URL

| 항목 | 값 |
|---|---|
| 지원 URL | `https://prafta.com` (필수) |
| 마케팅 URL | 생략 가능 |
| 저작권 | `2026 PRAFTA` |

---

## 7. STEP 6 — App Privacy (데이터 수집 설문) ✋

Apple 의 설문은 Google Data safety 와 항목이 다르다. **아래가 iOS 기준 답안이다.**

### 공통

- **데이터를 수집하는가**: 예
- **추적(Tracking)에 사용하는가**: **아니오** — 광고·분석 SDK 없음
  → ATT(앱 추적 투명성) 팝업 불필요

### 데이터 유형별

| Apple 카테고리 | 세부 | 수집 | 신원 연결 | 추적 | 목적 |
|---|---|---|---|---|---|
| Contact Info | Name | 예 | 예 | 아니오 | App Functionality |
| Contact Info | Email Address | 예 | 예 | 아니오 | App Functionality |
| Contact Info | Phone Number | 예 | 예 | 아니오 | App Functionality |
| Location | Precise Location | 예 | 예 | 아니오 | App Functionality |
| User Content | Photos or Videos | 예 | 예 | 아니오 | App Functionality |
| Identifiers | User ID | 예 | 예 | 아니오 | App Functionality |
| Identifiers | Device ID | 예 | 예 | 아니오 | App Functionality |
| Other Data | 기타(생년월일·성별·소속·직급·고용형태) | 예 | 예 | 아니오 | App Functionality |

수집하지 **않는** 항목: 결제 정보, 건강, 금융, 검색/열람 기록, 사용 데이터, 진단 데이터
(Crashlytics 미사용 — FCM 만 사용), 연락처 목록.

> Google 답안에 있던 `RECORD_AUDIO`(오디오)·`READ_MEDIA_IMAGES` 는 이후 제거됐으므로
> **오디오 수집 = 아니오**. 구 문서(`플레이스토어_DataSafety_및_등록정보.md`)의 해당 항목은 폐기.

---

## 8. STEP 7 — 앱 심사 정보 (App Review Information) ✋

**가장 중요한 칸이다.** 여기가 부실하면 기능 확인 불가로 바로 리젝된다.

### 로그인 필요 (Sign-in required): **예**

데모 계정 ID/PW 를 정확히 기입한다. 심사 직전 **직접 로그인해서 살아있는지 재확인할 것**
(계정 잠금·비밀번호 만료 게이트가 걸리면 그대로 리젝된다).

### 연락처

담당자 이름 / 전화 / 이메일 (`aqswgg@gmail.com`)

### 비고 (Notes) — 아래 문안 권장

심사자가 한국어를 못 읽을 수 있으므로 **영문으로 적는다.**

```
PRAFTA is a B2B workforce-safety and attendance app for construction and
industrial worksites in South Korea. Accounts are issued by the customer
company; the demo account below is provided for review.

Demo account
  ID: (데모 계정 ID)
  PW: (데모 계정 PW)

Permissions
- Location is used ONLY when the user taps clock-in/clock-out or enters a
  worksite TBM session, to verify the user is at the registered site.
  We do NOT collect location in the background.
- Camera is used ONLY for scanning checkpoint QR codes and taking
  on-site photos.
- The app remains fully usable if the user denies these permissions
  ("Later" button on the permission screens). Features that require a
  permission request it again at the moment of use.

Account deletion
- Account deletion is available in-app: My page > account settings > withdraw.

Notes for testing
- The UI is in Korean. Bottom tab bar, left to right:
  Home / Attendance / Safety / TBM / My page.
- The demo account is registered to a worksite in Seoul, South Korea.
  When you tap the green "clock in" button from outside that worksite,
  the app opens an "off-site check-in" sheet: type any text in the reason
  field and tap the green button to complete the check-in.
  This is expected behavior, not an error.
- The app content is rendered in a WebView served from the app bundle;
  business data comes from our API over HTTPS.
```

> ⚠️ 위 문안은 08-02 제출 때 **실제로 고쳐 넣은 최종본**이다. 초안에 있던
> `The demo account is configured on a site with a relaxed radius.` 는 사실이 아니었다
> (실제로는 근무지 범위 밖 → 외근 등록 폴백으로 완료됨). 심사자는 한국 사업장에서
> 수천 km 떨어진 곳에서 테스트하므로 **반드시 그 화면을 본다** — 실제 동작을 그대로 적어야
> "고장인가?" 로 이어지지 않는다. 한국어 UI 탭바 안내와 계정 삭제 경로도 같은 이유로 넣었다.

> 괄호 부분은 실제 데모 계정 값으로 치환할 것.
> "위치는 기능 실행 시점에만 / 백그라운드 수집 없음 / 권한 거부해도 사용 가능" 이 세 문장이
> 5.1.1 관련 질의를 줄여 준다.

---

## 9. STEP 8 — 빌드 선택 후 제출 ✋

1. 버전 페이지 **빌드** 섹션 → `+` → 업로드된 빌드(111) 선택
2. 수출 규정 질문이 뜨면 → Info.plist 에 이미 기재돼 있어 대개 생략된다
3. **출시 방식**
   - *자동 출시* — 승인 즉시 스토어 공개
   - *수동 출시* — 승인 후 버튼을 눌러야 공개 ← **첫 출시는 이쪽 권장** (승인 시점을 통제)
4. **심사를 위해 제출(Add for Review → Submit)**

---

## 10. 예상 리젝 사유와 대응

| 가이드라인 | 사유 | 대응 |
|---|---|---|
| 2.1 | 데모 계정 로그인 실패 | 제출 직전 실로그인 재확인. 계정 잠금/비밀번호 변경 게이트 주의 |
| 5.1.1 | 권한 강제 | §1-1 소프트 게이트로 선제 대응 완료. 재지적 시 §8 비고 문안으로 소명 |
| 5.1.1(v) | 계정 삭제 경로 없음 | 앱 내 탈퇴 기능 존재 — 위치(마이페이지)를 비고에 안내 |
| 4.2 | "최소 기능" / 웹뷰 래퍼 지적 | 네이티브 기능(푸시·QR 스캐너·GPS·카메라) 보유를 소명. 리젝 시 비고에 명시 |
| 2.3 | 스크린샷이 실제 화면과 다름 | 데모 계정 실제 화면만 사용, 합성 이미지 금지 |
| 1.5 | 지원 URL 부실 | `prafta.com` 이 접속되고 연락처가 보이는지 확인 |

**리젝은 실패가 아니다.** App Store Connect 의 Resolution Center 로 사유가 오고,
수정 후 재제출하면 된다(보통 1~3일). 빌드 수정이 필요 없는 사유(문구·비고)는
**재빌드 없이 답변만으로** 해결되는 경우도 많다.

심사 기간은 보통 24~48시간이며, 신규 개발자 첫 앱은 더 걸릴 수 있다.

---

## 11. 승인 후

- 수동 출시로 뒀다면 **"이 버전 released" 버튼**을 눌러야 공개된다
- 공개 후 스토어 검색 반영까지 몇 시간 걸릴 수 있다
- 이후 업데이트는 **빌드 업로드 → 새 버전 생성 → 제출** 반복
  (`CFBundleShortVersionString` 은 pubspec `version:` 의 앞부분, 빌드번호는 Codemagic 자동)

---

## 부록. 안드로이드와 다른 점 요약

| | Google Play | App Store |
|---|---|---|
| 권한 거부 시 앱 차단 | 사실상 문제 삼지 않음 | **리젝 사유 (5.1.1)** |
| 데모 계정 | 권장 | **사실상 필수** |
| 심사 주체 | 자동 검사 비중 큼 | **사람이 직접 사용** |
| 데이터 설문 | Data safety | App Privacy (항목 체계 다름) |
| 태블릿 | 선택 | 지원 선언 시 **스크린샷 필수** |
