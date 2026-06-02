# 앱 작업지시서 - TBM 푸시 알림 (클라이언트)

> 본 문서는 TBM 모듈의 **푸시 알림 클라이언트 측 처리**를 정의합니다.
> 백엔드 푸시 발송 로직은 `04_BACKEND_SPEC_APP.md §10` 참조.

---

## 0. 본 문서의 범위

본 문서는 다음을 다룹니다:
- 푸시 알림 시나리오별 페이로드 정의
- 알림 수신 시 클라이언트 동작 (포그라운드/백그라운드/종료 상태)
- 딥링크 처리 (`prafta://tbm/sessions/{no}`)
- 디바이스 토큰 등록/갱신 흐름
- FCM(Android) / APNs(iOS) 통합 가이드

본 문서는 다음을 다루지 **않습니다**:
- 백엔드 푸시 발송 로직 → `04_BACKEND_SPEC_APP.md §10`
- 푸시 발송 대상자 필터링 정책 → 백엔드 사양 또는 별도 결정
- Prafta 전체 푸시 인프라 → 기존 Prafta 서비스 (Claude Code 로컬 확인)

---

## 1. 푸시 알림 시나리오

### 1.1 시나리오 매트릭스

| # | 트리거 시점 | 발송 대상 | 알림 타입 | 우선순위 |
|---|---|---|---|---|
| 1 | TBM 세션 개설 (OPENED) | 사업장 내 권한 있는 근로자 | TBM_OPENED | 일반 |
| 2 | 교육 시작 (IN_PROGRESS) | 입실 대기 중인 참여자 (SSE 백업) | TBM_STARTED | 높음 |
| 3 | 교육 종료 (COMPLETED) | 입실 후 종료 안 한 사용자 | TBM_ENDING | 높음 |
| 4 | 세션 취소 (CANCELLED) | 입실한 사용자 | TBM_CANCELLED | 높음 |
| 5 | 미이수 처리 (사후) | 미이수 대상자 | TBM_NOT_COMPLETED | 일반 |
| 6 | 신규 일용직 입실 알림 (선택) | 개설자 | TBM_DAILY_ENTERED | 낮음 |

### 1.2 시나리오별 메시지 예시

#### 1. TBM 세션 개설 (TBM_OPENED)

```
제목: TBM 개설 안내
내용: [A공장] 5월 1주차 TBM이 개설되었습니다. 입실 비밀번호를 받아 입실해주세요.
딥링크: prafta://tbm/sessions/100
```

#### 2. 교육 시작 (TBM_STARTED)

```
제목: TBM 시작
내용: [A공장] 5월 1주차 TBM이 시작되었습니다.
딥링크: prafta://tbm/sessions/100/live
```

⚠️ SSE로도 처리되지만, 앱이 백그라운드인 경우 SSE가 끊겨 있을 수 있어 푸시로 백업.

#### 3. 교육 종료 (TBM_ENDING)

```
제목: TBM 종료 - 출결 마감 필요
내용: [A공장] 5월 1주차 TBM이 종료되었습니다. 종료 비번 입력 후 서명해주세요.
딥링크: prafta://tbm/sessions/100/exit
```

#### 4. 세션 취소 (TBM_CANCELLED)

```
제목: TBM 취소
내용: [A공장] 5월 1주차 TBM이 취소되었습니다.
딥링크: prafta://tbm/sessions/100
```

#### 5. 미이수 처리 사후 알림 (TBM_NOT_COMPLETED)

```
제목: TBM 출결 변경 안내
내용: 4월 3주차 TBM이 미이수로 처리되었습니다. 사유 확인 및 이의 제기는 관리자에게 문의하세요.
딥링크: prafta://tbm/history/sessions/95
```

⚠️ 본 시나리오는 사용자 입장에서 민감할 수 있어 **회사 정책에 따라 발송 여부 결정**. 기본은 비활성, 옵션으로 활성화.

#### 6. 일용직 입실 알림 (TBM_DAILY_ENTERED, 선택)

```
제목: 일용직 입실
내용: [A공장] 김일용 외 1명이 입실했습니다.
딥링크: prafta://tbm/sessions/100/live
```

⚠️ 본 시나리오는 알림 피로도 우려로 **기본 비활성**. 관리자가 옵션으로 켤 수 있음.

---

## 2. 푸시 페이로드 구조

### 2.1 FCM 표준 페이로드

```json
{
    "to": "fcm:device_token...",
    "priority": "high",
    "notification": {
        "title": "TBM 시작",
        "body": "[A공장] 5월 1주차 TBM이 시작되었습니다.",
        "sound": "default",
        "icon": "ic_tbm_notification",
        "color": "#1D9E75",
        "android_channel_id": "tbm_high"
    },
    "data": {
        "type": "TBM_STARTED",
        "eduSessionNo": "100",
        "siteCd": "A001",
        "siteNm": "A공장",
        "sessionTitle": "5월 1주차 TBM",
        "deeplink": "prafta://tbm/sessions/100/live",
        "ts": "1717216820"
    }
}
```

### 2.2 페이로드 필드 정의

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `type` | String | Y | 알림 타입 코드 (TBM_OPENED 등) |
| `eduSessionNo` | String | Y | 세션 번호 (문자열로 전송, 클라이언트가 변환) |
| `siteCd` | String | Y | 사업장 코드 |
| `siteNm` | String | N | 사업장 이름 (UI 표시용) |
| `sessionTitle` | String | N | 세션 제목 (UI 표시용) |
| `deeplink` | String | Y | 딥링크 URI |
| `ts` | String | N | 발송 시각 timestamp (epoch seconds) |

### 2.3 APNs 페이로드 (iOS)

```json
{
    "aps": {
        "alert": {
            "title": "TBM 시작",
            "body": "[A공장] 5월 1주차 TBM이 시작되었습니다."
        },
        "sound": "default",
        "badge": 1,
        "content-available": 1
    },
    "type": "TBM_STARTED",
    "eduSessionNo": "100",
    "siteCd": "A001",
    "siteNm": "A공장",
    "sessionTitle": "5월 1주차 TBM",
    "deeplink": "prafta://tbm/sessions/100/live",
    "ts": "1717216820"
}
```

⚠️ iOS는 향후 검토 사항. MVP는 Android(FCM)만.

---

## 3. 안드로이드 알림 채널

### 3.1 채널 구성

| 채널 ID | 이름 | 중요도 | 진동 | 사운드 |
|---|---|---|---|---|
| `tbm_high` | TBM 긴급 | HIGH | Y | Y |
| `tbm_normal` | TBM 일반 | DEFAULT | Y | Y |
| `tbm_low` | TBM 정보 | LOW | N | N |

### 3.2 알림 타입별 채널 매핑

| 알림 타입 | 채널 | 이유 |
|---|---|---|
| TBM_STARTED | tbm_high | 즉시 대응 필요 |
| TBM_ENDING | tbm_high | 즉시 대응 필요 (서명 누락 방지) |
| TBM_CANCELLED | tbm_high | 알아야 함 |
| TBM_OPENED | tbm_normal | 일반 안내 |
| TBM_NOT_COMPLETED | tbm_normal | 일반 안내 |
| TBM_DAILY_ENTERED | tbm_low | 관리자 정보 |

### 3.3 채널 생성 코드 (Flutter)

```dart
// Flutter android_alarm_manager 또는 flutter_local_notifications 사용
const AndroidNotificationChannel highChannel = AndroidNotificationChannel(
  'tbm_high',
  'TBM 긴급',
  description: 'TBM 시작·종료·취소 등 즉시 대응이 필요한 알림',
  importance: Importance.high,
);

const AndroidNotificationChannel normalChannel = AndroidNotificationChannel(
  'tbm_normal',
  'TBM 일반',
  description: 'TBM 개설 등 일반 안내',
  importance: Importance.defaultImportance,
);

const AndroidNotificationChannel lowChannel = AndroidNotificationChannel(
  'tbm_low',
  'TBM 정보',
  description: '관리자 대상 정보성 알림',
  importance: Importance.low,
);
```

⚠️ 실제 구현은 Prafta 기존 알림 채널 구조 확인 후 통합.

---

## 4. 클라이언트 측 알림 처리

### 4.1 앱 상태별 동작

| 앱 상태 | 알림 수신 | 화면 동작 |
|---|---|---|
| **포그라운드** | 인앱 배너 또는 SSE 처리 | SSE 우선 (푸시는 무시) |
| **백그라운드** | 시스템 알림 표시 | 탭 시 딥링크 처리 |
| **종료 상태** | 시스템 알림 표시 | 탭 시 앱 실행 + 딥링크 처리 |

### 4.2 포그라운드 처리 흐름

```
[푸시 수신 - 포그라운드]
  ↓
SSE 연결 상태 확인
  ↓
┌────────────────┬────────────────┐
│ SSE 연결됨      │ SSE 끊김        │
└────────────────┴────────────────┘
  ↓                ↓
푸시 무시         인앱 배너 표시
(SSE가 처리)      + 탭 시 딥링크
```

**이유**: SSE가 동작 중이면 같은 이벤트가 이중 처리될 수 있음. SSE가 우선.

### 4.3 백그라운드/종료 처리 흐름

```
[푸시 수신 - 백그라운드/종료]
  ↓
시스템 알림 표시
  ↓
사용자 탭
  ↓
앱 실행 또는 포그라운드 복귀
  ↓
딥링크 처리 → 해당 화면 이동
  ↓
SSE 재연결 + 데이터 동기화
```

### 4.4 인앱 배너 (포그라운드)

```
┌──────────────────────────────────┐
│  🔔 TBM 시작                  [X]│
│  5월 1주차 TBM이 시작되었습니다    │
│                       [확인 →]   │
└──────────────────────────────────┘
```

- 화면 상단에 약 3~5초 표시
- 좌측에서 슬라이드 인 / 우측 X 또는 자동 슬라이드 아웃
- 탭 시 딥링크로 이동

---

## 5. 딥링크 처리

### 5.1 URI 스키마

```
prafta://tbm/{path}
```

### 5.2 경로 정의

| 딥링크 | 동작 |
|---|---|
| `prafta://tbm/sessions/{no}` | 해당 세션 상세 또는 입실 가능 시 M-03 |
| `prafta://tbm/sessions/{no}/live` | M-05 (입실되어 있으면) 또는 M-03 (입실 안 한 경우) |
| `prafta://tbm/sessions/{no}/exit` | M-07 종료 처리 화면 |
| `prafta://tbm/sessions/{no}/manager` | 관리자 콘솔 M-11 |
| `prafta://tbm/history/sessions/{no}` | M-10 상세 (이력) |
| `prafta://tbm/history` | M-10 (완료 탭) |
| `prafta://tbm` | M-01 (TBM 메인) |

### 5.3 딥링크 처리 흐름

```
[딥링크 수신]
  ↓
앱이 실행 중인가?
  ↓
┌────────────────┬────────────────┐
│ Yes - 포그라운드 │ Yes - 백그라운드│
│ 또는 No - 종료   │                │
└────────────────┴────────────────┘
  ↓                ↓
딥링크 즉시 처리   포그라운드 복귀 후
인증 상태 확인     딥링크 처리
  ↓
┌────────────────┬────────────────┐
│ 인증됨          │ 미인증          │
└────────────────┴────────────────┘
  ↓                ↓
딥링크 화면 이동   로그인 화면 →
                  로그인 후 딥링크
```

### 5.4 권한 체크

딥링크로 진입 시:
- **사용자 역할 확인** (근로자 vs 관리자)
- 근로자가 관리자 경로 진입 시 → 404 또는 권한 부족 안내
- 권한 있어도 세션이 본인 사업장이 아닌 경우 → 안내 후 메인 복귀

### 5.5 컨텍스트별 분기 (스마트 라우팅)

`prafta://tbm/sessions/{no}` 같은 모호한 경로:

```
[딥링크 진입: /sessions/100]
  ↓
사용자가 이미 입실했는가?
  ↓
┌────────────────┬────────────────┐
│ Yes             │ No              │
└────────────────┴────────────────┘
  ↓                ↓
세션 상태 확인     세션 상태 확인
  ↓                ↓
┌──────────┬──────┐ ┌──────────┬──────────┐
│IN_PROGRESS│COMPLETED│OPENED   │ETC      │
└──────────┴──────┘ └──────────┴──────────┘
  ↓        ↓       ↓        ↓
M-05      M-10    M-03     M-02
(진행)    (이력)  (입실)   (목록)
```

---

## 6. 디바이스 토큰 등록 흐름

### 6.1 초기 등록

```
[앱 최초 실행]
  ↓
FCM 토큰 발급
  ↓
사용자 로그인
  ↓
JWT 토큰 확보
  ↓
POST /api/app/tbm/push/devices
  { pushToken, platform, appVersion }
  ↓
서버에 토큰 저장
  ↓
백엔드가 푸시 발송 가능 상태
```

### 6.2 토큰 갱신

FCM 토큰은 다음 경우 갱신될 수 있음:
- 앱 재설치
- 앱 데이터 초기화
- FCM 인프라 토큰 회전

```
[FCM 토큰 변경 감지]
  ↓
새 토큰 받음
  ↓
POST /api/app/tbm/push/devices
  (기존 토큰 자동 무효화 또는 별도 갱신 API)
  ↓
서버에 새 토큰 저장
```

### 6.3 로그아웃 시

```
[사용자 로그아웃]
  ↓
DELETE /api/app/tbm/push/devices
  (또는 토큰 비활성화)
  ↓
서버에서 해당 토큰 비활성
  ↓
이 사용자 명의로는 더 이상 푸시 발송 안 됨
```

⚠️ Prafta 전체 푸시 토큰 시스템이 이미 있으면 그것 활용. TBM 전용 토큰 X.

### 6.4 다중 디바이스 처리

같은 사용자가 여러 디바이스 사용 시:
- 디바이스별 토큰 각각 등록
- 푸시 발송 시 사용자 ID로 모든 토큰 조회 후 일괄 발송
- 또는 가장 최근 활성 디바이스만 발송 (정책에 따라)

⚠️ Prafta 정책 확인 (Claude Code 로컬).

---

## 7. 알림 권한 처리

### 7.1 Android 권한 요청 흐름 (Android 13+)

```
[앱 최초 실행 - Android 13+]
  ↓
POST_NOTIFICATIONS 권한 확인
  ↓
┌────────────────┬────────────────┐
│ 권한 있음       │ 권한 없음        │
└────────────────┴────────────────┘
  ↓                ↓
FCM 토큰 등록     권한 요청 다이얼로그
  ↓                ↓
                ┌────────┬────────┐
                │ 허용    │ 거부     │
                └────────┴────────┘
                  ↓        ↓
                FCM 등록  안내:
                          "푸시 알림 없이도
                          앱 사용 가능합니다"
```

### 7.2 권한 거부 시 안내

```
┌──────────────────────────────────┐
│  알림 권한이 없습니다              │
├──────────────────────────────────┤
│                                  │
│  TBM 시작·종료 알림을 받지        │
│  못할 수 있습니다.                │
│                                  │
│  앱을 사용하는 데는 문제가         │
│  없습니다.                        │
│                                  │
│  [나중에]      [설정으로 이동]    │
└──────────────────────────────────┘
```

### 7.3 권한 재요청 정책

- 거부 후 즉시 재요청 X (사용자 피로도)
- 다음 진입 시 또는 일정 기간 후 재안내
- 명시적 설정 화면에서 사용자가 다시 시도 가능

---

## 8. 알림 클릭 추적 (선택)

### 8.1 추적 데이터 (Phase 2)

- 알림 표시 → 사용자 클릭 여부
- 클릭 후 화면 진입까지 시간
- 푸시 도달률 (FCM 전송 vs 실제 표시)

### 8.2 추적용 페이로드 확장

```json
{
    "data": {
        "...": "...",
        "trackingId": "push_20260527_100_user001"
    }
}
```

클릭 시:
```
POST /api/app/tbm/push/track
{ "trackingId": "...", "event": "CLICKED" }
```

⚠️ MVP는 미구현, Phase 2 분석용.

---

## 9. 알림 그룹화

### 9.1 같은 세션의 알림 그룹화

같은 TBM 세션에서 여러 알림이 발생할 수 있음:
- OPENED 알림
- STARTED 알림
- ENDING 알림

→ 같은 그룹으로 묶어 알림 패널 정리:

```
🔔 TBM 알림 (3건)
  • 5월 1주차 TBM이 종료되었습니다 (방금)
  • 5월 1주차 TBM이 시작되었습니다 (10분 전)
  • 5월 1주차 TBM이 개설되었습니다 (30분 전)
```

### 9.2 Android 알림 그룹화 구현

```dart
AndroidNotificationDetails(
  'tbm_high',
  'TBM 긴급',
  groupKey: 'tbm_session_100',  // 세션 번호별 그룹
  setAsGroupSummary: false,
)
```

요약 알림 (summary):
```dart
AndroidNotificationDetails(
  'tbm_high',
  'TBM 긴급',
  groupKey: 'tbm_session_100',
  setAsGroupSummary: true,
)
```

### 9.3 처리된 알림 자동 제거

- 사용자가 TBM 입실 → 해당 세션의 미처리 알림 자동 삭제
- 사용자가 세션 종료 → 해당 세션의 모든 알림 삭제

```dart
flutterLocalNotificationsPlugin.cancelAll();  // 또는 ID 지정
```

---

## 10. 테스트 가이드

### 10.1 시나리오별 테스트 케이스

| # | 시나리오 | 기대 동작 |
|---|---|---|
| 1 | 앱 종료 상태에서 푸시 수신 | 시스템 알림 표시 |
| 2 | 알림 탭 → 앱 실행 + 딥링크 | 해당 화면 진입 |
| 3 | 미인증 상태에서 딥링크 | 로그인 → 후속 처리 |
| 4 | 앱 포그라운드 + 푸시 수신 | SSE 우선, 푸시 무시 또는 인앱 배너 |
| 5 | 같은 세션 다중 알림 | 그룹화 표시 |
| 6 | 권한 거부 사용자에게 푸시 발송 | 시스템 표시 없음, 백엔드 로그 |
| 7 | FCM 토큰 만료 후 푸시 | 백엔드 토큰 갱신 또는 폐기 |
| 8 | 다중 디바이스 같은 사용자 | 모든 디바이스에 푸시 |
| 9 | 외국인 노동자 다국어 (Phase 2) | 모국어로 알림 표시 |

### 10.2 디버깅 도구

- **FCM Console**: Firebase Console에서 토큰별 테스트 발송
- **로컬 알림 테스트**: 백엔드 통해 자기 자신에게 발송하는 디버그 API (선택)
- **알림 로그**: 클라이언트 측 수신 로그 (디버그 빌드만)

---

## 11. 클라이언트 컴포넌트 구조

### 11.1 Flutter 측

```
flutter/lib/
├── services/
│   ├── push_notification_service.dart       # FCM 통합
│   ├── deep_link_service.dart               # 딥링크 라우팅
│   └── notification_channel_service.dart    # 채널 관리
├── handlers/
│   ├── push_handler.dart                    # 푸시 수신 처리
│   └── deep_link_handler.dart               # 딥링크 처리
└── models/
    └── tbm_notification.dart                # 페이로드 모델
```

### 11.2 Vue WebApp 측 (브릿지)

```
src/services/
├── pushBridge.ts                 # Flutter ↔ Vue 푸시 통신
└── deepLinkBridge.ts             # 딥링크 라우팅
```

### 11.3 통신 예시

```dart
// Flutter 측 - 푸시 수신 시 WebView로 전달
void onPushReceived(RemoteMessage message) {
  if (appState == AppState.foreground) {
    // SSE 연결 상태 확인 후 WebView로 전달
    webViewController.runJavaScript(
      'window.onPushReceived(${jsonEncode(message.data)})'
    );
  } else {
    // 시스템 알림 표시
    showLocalNotification(message);
  }
}
```

```typescript
// Vue 측
window.onPushReceived = (data) => {
  // SSE가 연결 안 된 경우만 처리
  if (!sseConnected.value) {
    handlePushAsEvent(data)
  }
}

// 딥링크 처리
window.onDeepLink = (url) => {
  const route = parseDeepLink(url)
  router.push(route)
}
```

---

## 12. 백엔드 푸시 정책과의 정합성

### 12.1 백엔드 정책 (`04_BACKEND_SPEC_APP.md §10` 요약)

- 시점: OPENED / STARTED / ENDING / CANCELLED
- 대상자: 시나리오별 자동 계산
- 발송: 백엔드가 트랜잭션 커밋 후 비동기 발송

### 12.2 클라이언트는 다음을 처리

- 수신 후 디스플레이
- 딥링크 처리
- 토큰 등록·갱신
- 권한 관리
- 그룹화·자동 제거

### 12.3 정합성 체크

- 모든 시나리오의 `type` 코드가 백엔드 ↔ 클라이언트 일치
- `eduSessionNo` 등 데이터 필드 명명 일치
- 딥링크 경로 일치

---

## 13. 운영 시 고려사항

### 13.1 알림 피로도 관리

- 같은 사용자에게 짧은 시간 내 다수 알림 발송 X
- 너무 잦은 푸시는 무음·진동 차단 가능
- (Phase 2) 사용자 옵션: 알림 종류별 on/off

### 13.2 비상 시 대응

- FCM 장애 시 → SSE 백업 (이미 설계됨)
- 푸시 안 받는 사용자 분쟁 시 → 백엔드 로그로 발송 시도 증빙

### 13.3 GDPR / 개인정보 (참고)

- 푸시 알림에 민감 정보 X
- 본명 노출 최소화 (예: "○○○ 외 1명")
- 회사 정책에 따른 알림 동의 (Phase 2 약관)

---

## 14. 본 문서에서 다루지 않는 부분

- **백엔드 푸시 발송 로직** → `04_BACKEND_SPEC_APP.md §10`
- **Prafta 전체 푸시 인프라** → 기존 시스템 (Claude Code 로컬 확인)
- **iOS APNs 상세** → Phase 2
- **다국어 알림 메시지** → Phase 2
- **분석/추적 시스템** → Phase 2

---

## 15. Claude Code 작업 시 우선 확인 사항

1. **Prafta 기존 푸시 인프라** — FCM 통합 패턴, 토큰 관리 서비스
2. **알림 채널 컨벤션** — 기존 알림 채널 구조 (TBM 추가 vs 통합)
3. **딥링크 처리 패턴** — Flutter ↔ Vue 라우팅 컨벤션
4. **알림 권한 처리 컨벤션** — 권한 요청 시점, 거부 시 안내
5. **로그아웃 시 토큰 처리** — Prafta 정책
6. **다중 디바이스 처리** — 같은 사용자 다중 토큰 정책
7. **푸시 발송 실패 처리** — 재시도, 로그, 폴백 전략

위 사항 확인 후 차이가 있으면 사용자와 협의.

---

## 16. 완료 - 다음 단계

본 문서로 TBM 모듈 작업지시서 작성이 완료됩니다.

| Phase | 문서 | 상태 |
|---|---|---|
| 1 | `_PROGRESS.md`, `00_OVERVIEW.md`, `01_DDL_SPEC.md` | ✅ |
| 2 | `02~04_BACKEND_SPEC_*.md` | ✅ |
| 3 | `web/05_01~04.md` | ✅ |
| 4 | `app/06_01~03.md` | ✅ |

Claude Code가 본 작업지시서를 받아 구현에 착수할 수 있습니다. **각 문서 상단의 ⚠️ "Claude Code 작업 시 우선 확인 사항"을 먼저 검토** 후 진행 권장.
