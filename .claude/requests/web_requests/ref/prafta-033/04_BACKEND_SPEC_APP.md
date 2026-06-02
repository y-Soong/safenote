# TBM 모듈 백엔드 - 앱 API 사양

> 본 문서는 TBM 모듈의 **모바일 앱(Flutter + Vue WebApp) 전용 API**를 정의합니다.
> 공통 컨벤션은 `02_BACKEND_SPEC_COMMON.md`, 웹 API와 공유되는 도메인 로직은 `03_BACKEND_SPEC_WEB.md` 참조.

---

## 1. URL 컨벤션

- **Base Path**: `/api/app/tbm`
- 웹 API와는 **Controller만 분리**, Service/Mapper는 공유
- RESTful 명명 일관 유지

---

## 2. 인증 및 모바일 특수 헤더

| 헤더 | 용도 | 필수 |
|---|---|---|
| `Authorization` | `Bearer {JWT}` | Y |
| `X-Device-Token` | 디바이스 고유 토큰 | Y (정규직) |
| `X-App-Version` | 앱 버전 (예: `1.0.0`) | N |
| `X-Platform` | `ANDROID` / `IOS` | N |
| `Content-Type` | 상황별 | Y |

**디바이스 토큰 정책:**
- 본 모듈에서는 토큰 발급/검증을 Prafta 기존 시스템 활용 (Claude Code 로컬 확인)
- TBM에서는 **출결 시점 토큰 기록** 용도로만 사용
- 디바이스 변경 시 재인증 정책은 기존 시스템 따름

---

## 3. API 그룹 (화면 ↔ API)

| 화면 그룹 | 화면 ID | API 그룹 | 섹션 |
|---|---|---|---|
| 근로자 TBM 메인/탭 | M-01, M-02, M-09, M-10 | `/api/app/tbm/sessions/...` (조회) | §4 |
| 근로자 입실 흐름 | M-03, M-04 | `/api/app/tbm/sessions/{no}/enter` | §5 |
| 근로자 진행 화면 | M-05, M-06 | `/api/app/tbm/sessions/{no}/sse`, 이벤트 보고 | §6 |
| 근로자 종료 흐름 | M-07, M-08 | `/api/app/tbm/sessions/{no}/exit` | §7 |
| 모바일 관리자 콘솔 | M-11 | `/api/app/tbm/sessions/...` (관리자 액션) | §8 |
| 일용직 QR 스캔 | M-12, M-13 | `/api/app/tbm/sessions/{no}/qr-attendance/...` | §9 |
| 푸시 알림 | (전체) | `/api/app/tbm/push/...` | §10 |

---

## 4. 근로자 - TBM 목록 조회 API (M-01, M-02, M-09, M-10)

### 4.1 입실 가능 세션 목록 (M-02 "교육시작전" 탭)

```
GET /api/app/tbm/sessions/available
```

**쿼리 파라미터:**
| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `siteCd` | String | N | 현재 사업장 (없으면 JWT의 gv_siteCd) |

**서비스 로직:**
- 자기 회사 + 접근 가능한 사업장
- `STATUS_CD = 'OPENED'` 만
- 자기가 이미 입실한 세션 제외 (TB_EDU_ATTENDANCE에 ENTRY_AT 있으면 제외)
- 사용자에게 노출 가능한 세션만 (사업장/부서 필터링)

**응답:**
```json
{
    "sessions": [
        {
            "eduSessionNo": 100,
            "title": "5월 1주차 TBM",
            "siteCd": "A001",
            "siteNm": "본사 공장",
            "managerUserNm": "김안전",
            "openedAt": "2026-05-01T08:00:00",
            "contentCount": 2,
            "riskAssessmentCount": 1
        }
    ]
}
```

**비고:** 내용(`contentBody`)이나 비번은 입실 전에는 노출 X. 제목·관리자·시간만.

### 4.2 진행 중 세션 (M-09 "교육진행중" 탭)

자기가 입실했지만 종료 안 한 세션 (앱 재진입 시 복귀용).

```
GET /api/app/tbm/sessions/in-progress
```

**서비스 로직:**
- `TB_EDU_ATTENDANCE` 중 `EXIT_AT IS NULL` AND 세션이 `STATUS_CD = 'IN_PROGRESS'`
- 정규직: `USER_ID = #{gvUserId}`
- 일용직 케이스는 본 API에서 제외 (앱은 정규직 본인 디바이스 가정)

**응답:**
```json
{
    "sessions": [
        {
            "eduSessionNo": 100,
            "title": "5월 1주차 TBM",
            "siteNm": "본사 공장",
            "startedAt": "2026-05-01T08:05:00",
            "attendanceNo": 1001,
            "entryAt": "2026-05-01T08:01:00"
        }
    ]
}
```

### 4.3 완료 세션 이력 (M-10 "교육완료" 탭)

```
GET /api/app/tbm/sessions/completed
```

**쿼리 파라미터:**
| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `startDate` | LocalDate | N | |
| `endDate` | LocalDate | N | |
| `page` | Integer | N | |
| `pageSize` | Integer | N | |

**응답:**
```json
{
    "sessions": [
        {
            "eduSessionNo": 100,
            "title": "5월 1주차 TBM",
            "siteNm": "본사 공장",
            "endedAt": "2026-05-01T08:15:00",
            "attendanceNo": 1001,
            "entryAt": "2026-05-01T08:01:00",
            "exitAt": "2026-05-01T08:15:00",
            "completionStatusCd": "COMPLETED"
        }
    ],
    "totalCount": 22
}
```

---

## 5. 근로자 - 입실 흐름 API (M-03, M-04)

### 5.1 입실 비밀번호 검증 (M-03)

```
POST /api/app/tbm/sessions/{eduSessionNo}/enter
```

**요청 본문:**
```json
{
    "entryPwd": "123456",
    "userGpsLat": 37.5664,
    "userGpsLng": 126.9782
}
```

**필수 헤더:** `X-Device-Token`

**서비스 로직:**
1. 세션 조회
2. **STATUS_CD 체크**: `OPENED` 만 입실 허용. `IN_PROGRESS` 이상이면 `COMMON_409_001` 에러 ("교육이 이미 시작되어 입실할 수 없습니다.")
3. **비번 검증**: 일치 안 하면
   - TB_EDU_PWD_FAIL_LOG INSERT (USER_ID, DEVICE_TOKEN 포함)
   - `COMMON_401_001` 에러 반환
4. **GPS 검증** (`gpsVerifyTypeCd = 'AUTO'` 인 경우만):
   - 관리자 GPS와 사용자 GPS 거리 계산
   - 반경 초과 시 `COMMON_403_002` 에러
5. **중복 출결 체크**: UK 제약. 이미 입실한 경우 `COMMON_409_002`
6. TB_EDU_ATTENDANCE INSERT (USER_ID, ENTRY_TYPE_CD='SELF_DEVICE', ENTRY_DEVICE_TOKEN, ENTRY_GPS_LAT/LNG, ENTRY_DISTANCE_M, ENTRY_AT=NOW)
7. TB_EDU_ATTENDANCE_EVENT INSERT (ENTER)
8. SSE 알림: `attendance.updated` (관리자 화면용)

**응답:**
```json
{
    "attendanceNo": 1001,
    "entryAt": "2026-05-01T08:01:00",
    "session": {
        "eduSessionNo": 100,
        "title": "5월 1주차 TBM",
        "managerUserNm": "김안전",
        "statusCd": "OPENED"
    },
    "waitingForStart": true
}
```

**비고:** 입실 직후엔 `waitingForStart = true`로 대기 화면 표시 (M-04). 관리자가 교육 시작하면 SSE로 `session.started` 수신 → 진행 화면(M-05)으로 자동 이동.

### 5.2 입실 후 대기 / 진행 정보 조회 (M-04, M-05 초기 로딩)

```
GET /api/app/tbm/sessions/{eduSessionNo}/my-view
```

자기 디바이스가 입실한 상태에서 세션의 현재 표시 정보 조회.

**응답:**
```json
{
    "session": {
        "eduSessionNo": 100,
        "title": "5월 1주차 TBM",
        "statusCd": "IN_PROGRESS",
        "contentBody": "<p>오늘 TBM 주제는...</p>",
        "contentFormatCd": "RICH_HTML",
        "startedAt": "2026-05-01T08:05:00"
    },
    "contents": [
        {
            "contentNo": 123,
            "displayOrder": 0,
            "title": "지게차 안전수칙",
            "contentTypeCd": "VIDEO",
            "filePath": "/files/content/123.mp4",
            "externalUrl": null,
            "thumbnailPath": "/files/thumb/123.jpg",
            "durationSec": 180,
            "overrideDescription": null
        }
    ],
    "currentState": {
        "currentContentNo": 123,
        "currentSlideIndex": 0,
        "syncStateCd": "PLAYING",
        "lastUpdatedAt": "2026-05-01T08:06:00"
    },
    "myAttendance": {
        "attendanceNo": 1001,
        "entryAt": "2026-05-01T08:01:00"
    }
}
```

**서비스 로직:**
- 입실하지 않은 사용자가 호출 시 거부 (`COMMON_403_001`)
- 세션 상태 무관 (OPENED/IN_PROGRESS 모두 허용) — 콘텐츠 미리 받아두기 가능

---

## 6. 근로자 - 진행 화면 API (M-05, M-06)

### 6.1 SSE 구독

```
GET /api/app/tbm/sessions/{eduSessionNo}/sse
Accept: text/event-stream
```

**전제:** 호출 사용자가 해당 세션에 입실한 상태여야 함 (`TB_EDU_ATTENDANCE` 존재)

**SSE 이벤트 (수신):**

| 이벤트 | 페이로드 | 처리 |
|---|---|---|
| `session.started` | `{ startedAt }` | M-04 → M-05 전환 |
| `session.state` | `{ currentContentNo, currentSlideIndex, syncStateCd }` | 슬라이드 동기화 |
| `session.ended` | `{ endedAt }` | 종료 비번 입력 화면(M-07) 트리거 |
| `session.cancelled` | `{ reason }` | 화면 이탈 + 안내 |

**연결 유지:**
- 30분 timeout, 클라이언트 자동 재연결
- 재연결 시 현재 상태 즉시 푸시 (`session.state` 강제 전송)

### 6.2 이벤트 보고 (클라이언트 → 서버)

```
POST /api/app/tbm/attendances/{attendanceNo}/events
```

**요청 본문:**
```json
{
    "events": [
        {
            "eventTypeCd": "BACKGROUND_IN",
            "eventTime": "2026-05-01T08:08:00.123",
            "eventData": null
        },
        {
            "eventTypeCd": "BACKGROUND_OUT",
            "eventTime": "2026-05-01T08:08:35.456",
            "eventData": null
        },
        {
            "eventTypeCd": "GPS_UPDATED",
            "eventTime": "2026-05-01T08:09:00.789",
            "eventData": {
                "lat": 37.5664,
                "lng": 126.9782,
                "accuracy": 12
            }
        }
    ]
}
```

**서비스 로직:**
- 권한 체크: 본인 ATTENDANCE만 보고 가능
- 배치 INSERT (`TB_EDU_ATTENDANCE_EVENT` 다건)
- SERVER_RECEIVED_AT = NOW(3)

**비고:**
- 클라이언트는 이벤트 발생 시 즉시 또는 배치(1~5초)로 전송
- **오프라인 큐잉 지원**: 네트워크 끊김 시 로컬 저장 → 복구 시 일괄 전송 (EVENT_TIME은 클라이언트 시각, 서버는 SERVER_RECEIVED_AT로 도착 시각 기록)

**응답:**
```json
{
    "receivedCount": 3
}
```

### 6.3 콘텐츠 시청 보고 (선택, Phase 2 분석용 데이터 축적)

```
POST /api/app/tbm/attendances/{attendanceNo}/events
```

§6.2와 동일 API. `eventTypeCd = 'CONTENT_VIEWED'` + eventData에 `contentNo`, `durationSec` 포함.

---

## 7. 근로자 - 종료 흐름 API (M-07, M-08)

### 7.1 종료 비밀번호 검증 + 출결 마감

```
POST /api/app/tbm/sessions/{eduSessionNo}/exit
Content-Type: multipart/form-data
```

**Form Fields:**
| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `exitPwd` | String | Y | 종료 비번 |
| `signatureFile` | File | Y | 서명 이미지 (PNG) |

**서비스 로직:**
1. 본인 ATTENDANCE 조회 (USER_ID = gv_userId)
2. ATTENDANCE.EXIT_AT IS NULL 확인 (이미 종료된 경우 거부)
3. 세션 STATUS_CD 확인:
   - `IN_PROGRESS` 또는 `COMPLETED` 만 허용
   - `COMPLETED`여도 본인이 종료 안 했으면 종료 가능 (지각 종료 케이스)
4. exitPwd 검증 (TB_EDU_SESSION.EXIT_PWD 일치)
   - 실패 시 TB_EDU_PWD_FAIL_LOG INSERT
5. 서명 파일 저장
6. TB_EDU_ATTENDANCE UPDATE (EXIT_TYPE_CD='SELF', EXIT_AT=NOW, EXIT_SIGNATURE_PATH)
7. TB_EDU_ATTENDANCE_EVENT INSERT (END)

**응답:**
```json
{
    "attendanceNo": 1001,
    "exitAt": "2026-05-01T08:15:00",
    "completionStatusCd": "COMPLETED"
}
```

**비고:** 본 API 호출 시 디바이스 토큰 검증 등은 입실 시 토큰과 일치하는지 체크 (선택. Prafta 정책 따름).

---

## 8. 모바일 관리자 콘솔 API (M-11)

⚠️ **본 섹션은 웹 API(§5~§7 of `03_BACKEND_SPEC_WEB.md`)와 사실상 동일 기능**입니다.

차이점:
- Path: `/api/app/tbm/...` 사용
- Controller만 분리, Service는 공유
- Request/Response 동일 또는 모바일 특화 (간소화된 응답)

### 8.1 매핑 표

| 액션 | 웹 API | 앱 API |
|---|---|---|
| 세션 목록 | `GET /api/web/tbm/sessions` | `GET /api/app/tbm/manager/sessions` |
| 세션 상세 | `GET /api/web/tbm/sessions/{no}` | `GET /api/app/tbm/manager/sessions/{no}` |
| 세션 개설 | `POST /api/web/tbm/sessions` | `POST /api/app/tbm/manager/sessions` |
| 세션 수정 | `PUT /api/web/tbm/sessions/{no}` | `PUT /api/app/tbm/manager/sessions/{no}` |
| 세션 취소 | `POST /api/web/tbm/sessions/{no}/cancel` | `POST /api/app/tbm/manager/sessions/{no}/cancel` |
| 교육 시작 | `POST /api/web/tbm/sessions/{no}/start` | `POST /api/app/tbm/manager/sessions/{no}/start` |
| 슬라이드 동기화 | `POST /api/web/tbm/sessions/{no}/sync-state` | `POST /api/app/tbm/manager/sessions/{no}/sync-state` |
| 참여자 명단 | `GET /api/web/tbm/sessions/{no}/attendances/live` | `GET /api/app/tbm/manager/sessions/{no}/attendances/live` |
| 교육 종료 | `POST /api/web/tbm/sessions/{no}/end` | `POST /api/app/tbm/manager/sessions/{no}/end` |
| 강제 종료 | `POST /api/web/tbm/sessions/{no}/attendances/{ano}/force-end` | `POST /api/app/tbm/manager/sessions/{no}/attendances/{ano}/force-end` |
| 이력 조회 | `GET /api/web/tbm/history/sessions` | `GET /api/app/tbm/manager/history/sessions` |
| 출결 상세 | `GET /api/web/tbm/sessions/{no}/attendances` | `GET /api/app/tbm/manager/sessions/{no}/attendances` |
| 이벤트 로그 | `GET /api/web/tbm/attendances/{ano}/events` | `GET /api/app/tbm/manager/attendances/{ano}/events` |
| 미이수 처리 | `PUT /api/web/tbm/attendances/{ano}/completion` | `PUT /api/app/tbm/manager/attendances/{ano}/completion` |
| 사용자별 이수 이력 | `GET /api/web/tbm/history/users/{uid}/attendances` | `GET /api/app/tbm/manager/history/users/{uid}/attendances` |
| 콘텐츠 목록 | `GET /api/web/tbm/contents` | `GET /api/app/tbm/manager/contents` |
| 콘텐츠 등록 | `POST /api/web/tbm/contents` | `POST /api/app/tbm/manager/contents` |

### 8.2 모바일 특화 응답 (예시)

모바일은 화면 폭이 좁아 일부 필드가 생략되거나 압축될 수 있음. Claude Code가 구현 시 판단:

- 페이지네이션 size 기본값 차이 (모바일 default 10, 웹 default 20)
- 일부 통계 응답을 별도 API로 분리 (모바일은 한 번에 큰 데이터 받기 부담)

### 8.3 Controller 클래스 분리

```
TbmContentAppController          # /api/app/tbm/manager/contents (관리자 콘텐츠)
TbmSessionAppController          # /api/app/tbm/manager/sessions (관리자 세션)
TbmSessionLiveAppController      # /api/app/tbm/manager/sessions/{no}/... (실시간)
TbmHistoryAppController          # /api/app/tbm/manager/history (이력)
TbmWorkerAppController           # /api/app/tbm/sessions/... (근로자 본인)
TbmQrAppController               # /api/app/tbm/sessions/{no}/qr-attendance/... (QR)
```

---

## 9. 일용직 QR 스캔 API (M-12, M-13)

본 섹션은 **모바일에서만 사용** (웹은 §6.4~6.5 of `03_BACKEND_SPEC_WEB.md` 참조). 동일 Service 메서드 호출.

### 9.1 QR 스캔 - 일용직 정보 미리보기

QR 스캔 후 입실 처리 전에 정보 표시용 (관리자가 본인 확인 시간).

```
POST /api/app/tbm/qr/preview
```

**요청 본문:**
```json
{
    "qrToken": "abc123xyz..."
}
```

**서비스 로직:**
1. 기존 QR 서비스로 QR 토큰 → DAILY_USER_NO 변환
2. TB_DAILY_USER 조회
3. **만료 검증** (TB_DAILY_USER 만료일 컬럼 - Claude Code 로컬 확인)
4. 만료된 경우에도 정보는 반환하되, `isExpired = true` 플래그

**응답:**
```json
{
    "dailyUserNo": 5001,
    "userNm": "김일용",
    "phoneLast4": "1234",
    "expireDt": "2026-06-30",
    "isFixed": false,
    "isExpired": false
}
```

**비고:** 본 API는 입실/종료 처리 전 단계. 실제 처리는 §9.2/9.3.

### 9.2 일용직 QR 입실 (M-12)

```
POST /api/app/tbm/sessions/{eduSessionNo}/qr-attendance/enter
```

`03_BACKEND_SPEC_WEB.md §6.4`와 동일 (Service 공유).

**모바일 특화 차이:**
- 관리자 GPS는 모바일 디바이스에서 자동 수집 (필수)
- 응답 후 즉시 관리자 화면에 명단 갱신

### 9.3 일용직 QR 종료 + 서명 (M-13)

```
POST /api/app/tbm/sessions/{eduSessionNo}/qr-attendance/{attendanceNo}/exit
Content-Type: multipart/form-data
```

`03_BACKEND_SPEC_WEB.md §6.5`와 동일.

**Form Fields:** §6.5와 동일.

**모바일 특화 흐름:**
1. 관리자가 QR 스캔 (재검증)
2. 일용직 정보 확인 화면 표시
3. 관리자가 폰을 일용직에게 넘김
4. 일용직이 서명 (Canvas)
5. 서명 이미지 + qrToken 함께 전송

---

## 10. 푸시 알림 API (전체)

⚠️ 본 섹션은 푸시 알림의 **백엔드 API**만 정의. 클라이언트 측 처리 및 알림 내용은 `app/06_03_PUSH_NOTIFICATION.md` 참조.

### 10.1 디바이스 푸시 토큰 등록

```
POST /api/app/tbm/push/devices
```

**요청 본문:**
```json
{
    "pushToken": "fcm:abc123...",
    "platform": "ANDROID",
    "appVersion": "1.0.0"
}
```

**비고:** Prafta 전체에서 공통적으로 사용하는 푸시 토큰 시스템이 이미 있다면 그것 활용. TBM 모듈 전용 토큰 X. Claude Code 로컬 확인.

### 10.2 푸시 알림 시나리오

다음 시점에 백엔드가 자동으로 푸시 발송:

| 시점 | 대상 | 메시지 예시 |
|---|---|---|
| **TBM 세션 OPENED** | 해당 사업장의 정규직 근로자 중 권한 있는 자 | "5월 1주차 TBM이 개설되었습니다. 입실 가능합니다." |
| **교육 시작 (IN_PROGRESS)** | 입실 대기 중인 참여자 | "교육이 시작되었습니다." (앱 SSE로도 가능, 백업용) |
| **교육 종료 (COMPLETED)** | 입실 중이지만 종료 안 한 사용자 | "교육이 종료되었습니다. 출결 마감을 진행해주세요." |
| **세션 취소** | 입실한 사용자 | "TBM이 취소되었습니다." |
| **관리자에게 알림** (선택) | 개설자 | "참여자 입실 시작" 등 |

### 10.3 푸시 발송 백엔드 처리

이벤트 발생 시점에 백엔드 서비스가 발송:

```java
@Service
public class TbmPushService {

    public void notifySessionOpened(Long eduSessionNo) {
        // 1. 세션 조회 (siteCd 추출)
        // 2. 해당 사업장의 푸시 대상자 조회 (기존 Prafta 서비스 활용)
        // 3. 푸시 메시지 구성
        // 4. 기존 Prafta 푸시 발송 서비스 호출
    }

    public void notifySessionStarted(Long eduSessionNo) { /* ... */ }
    public void notifySessionEnded(Long eduSessionNo) { /* ... */ }
    public void notifySessionCancelled(Long eduSessionNo) { /* ... */ }
}
```

발송 시점은 **트랜잭션 커밋 후** (SSE와 동일 원리).

### 10.4 푸시 알림 페이로드 (참고)

```json
{
    "notification": {
        "title": "TBM 알림",
        "body": "5월 1주차 TBM이 개설되었습니다."
    },
    "data": {
        "type": "TBM_OPENED",
        "eduSessionNo": "100",
        "deeplink": "prafta://tbm/sessions/100"
    }
}
```

`deeplink`는 앱 클라이언트에서 처리 (탭하면 해당 세션 화면으로 이동).

---

## 11. 디바이스 토큰 및 보안

### 11.1 디바이스 토큰

본 모듈은 디바이스 토큰을 **출결 기록 용도로만 사용**:
- `TB_EDU_ATTENDANCE.ENTRY_DEVICE_TOKEN` 컬럼에 저장
- 사후 분석 시 "어느 디바이스에서 입실했는가" 추적

**디바이스 토큰 정책 (Prafta 전체 시스템 따름):**
- 발급 시점: 앱 최초 설치 + JWT 발급 시
- 갱신 시점: JWT 갱신 시
- 폐기 시점: 로그아웃, 디바이스 변경

⚠️ 실제 발급/검증 로직은 Claude Code 로컬 확인. TBM 모듈에서는 기존 시스템의 토큰을 받아 출결에 기록만.

### 11.2 위변조 방지 고려 (참고)

본 MVP에서는 다루지 않지만 향후:
- 디바이스 토큰 위조 시도 감지
- 동일 토큰의 다중 출결 시 알림
- → Phase 2

---

## 12. 일관성 / 멱등성 / 오프라인 처리

### 12.1 멱등성

- **입실/종료**: UK 제약으로 중복 방지 (`UK_EDU_ATTENDANCE_USER`)
- **이벤트 보고**: 중복 발생 가능, 단 무해 (모두 기록)
- **종료 비번 검증**: 이미 종료된 경우 거부 (`EXIT_AT IS NOT NULL`)

### 12.2 오프라인 처리 (이벤트 큐잉)

클라이언트가 네트워크 끊김 시:
- 이벤트는 로컬 SQLite 또는 메모리 큐에 저장
- 네트워크 복구 시 일괄 전송 (§6.2)
- 서버는 `eventTime` (클라이언트 보고)과 `serverReceivedAt`(수신) 둘 다 기록

### 12.3 시간 동기화

클라이언트와 서버 시각 차이 발생 가능:
- `eventTime`: 클라이언트 디바이스 시간 (참고용)
- `entryAt`, `exitAt`: **서버 시간** (DB의 NOW())
- 분쟁 발생 시 **서버 시간 기준** 판단

---

## 13. SSE 채널 권한

근로자가 SSE 구독 시 (§6.1):
- 본 세션에 **입실한 상태**여야 구독 가능
- 입실 안 한 사용자가 구독 시도 시 403

관리자가 SSE 구독 시:
- 세션 개설자 또는 safe 권한
- 별도 엔드포인트 또는 동일 URL에서 권한별 다른 페이로드

→ 단순화 위해 **근로자/관리자 SSE 엔드포인트 분리** 권장:
- `/api/app/tbm/sessions/{no}/sse` — 근로자용
- `/api/app/tbm/manager/sessions/{no}/sse` — 관리자용 (참여자 변동 알림 포함)

---

## 14. 본 문서에서 다루지 않는 부분

- 푸시 메시지 본문/디자인 → `app/06_03_PUSH_NOTIFICATION.md`
- 화면별 UI/UX 흐름 → `app/06_01_WORKER_FLOW.md`, `app/06_02_MANAGER_FLOW.md`
- 웹 클라이언트 화면 → `web/05_xx_xxx.md`
- 푸시 발송 시 대상자 필터링 정책 (사업장 단위? 부서 단위?) → 별도 결정 필요 (현재 가정: 사업장 단위)

---

## 15. 컨트롤러 작성 가이드

### 15.1 예시 코드

```java
@RestController
@RequestMapping("/api/app/tbm/sessions")
@RequiredArgsConstructor
public class TbmWorkerAppController {

    private final TbmAttendanceService tbmAttendanceService;
    private final TbmSessionService tbmSessionService;
    private final JwtUtil jwtUtil;

    @GetMapping("/available")
    public ResponseEntity<AvailableSessionListResponse> getAvailableSessions(
        @RequestParam(required = false) String siteCd,
        @RequestHeader("Authorization") String authorization
    ) {
        AvailableSessionListResponse response = tbmSessionService.getAvailableSessions(
            AvailableSessionListParam.from(siteCd, jwtUtil.getAllClaimsAsMap(authorization))
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eduSessionNo}/enter")
    public ResponseEntity<EnterSessionResponse> enterSession(
        @PathVariable Long eduSessionNo,
        @RequestBody EnterSessionRequest request,
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("X-Device-Token") String deviceToken
    ) {
        EnterSessionResponse response = tbmAttendanceService.enterSession(
            EnterSessionParam.from(eduSessionNo, request, deviceToken,
                jwtUtil.getAllClaimsAsMap(authorization))
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{eduSessionNo}/exit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExitSessionResponse> exitSession(
        @PathVariable Long eduSessionNo,
        @RequestParam String exitPwd,
        @RequestParam MultipartFile signatureFile,
        @RequestHeader("Authorization") String authorization
    ) {
        ExitSessionResponse response = tbmAttendanceService.exitSession(
            ExitSessionParam.from(eduSessionNo, exitPwd, signatureFile,
                jwtUtil.getAllClaimsAsMap(authorization))
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{eduSessionNo}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeSse(
        @PathVariable Long eduSessionNo,
        @RequestHeader("Authorization") String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        // 입실 여부 검증
        tbmAttendanceService.verifyMyAttendance(eduSessionNo, tokenInfo);
        // SSE 등록
        return tbmSseEmitterManager.subscribeWorker(eduSessionNo, tokenInfo.gv_userId());
    }
}
```

---

## 16. 다음 단계

본 백엔드 사양을 기반으로 화면별 작업지시서가 작성됩니다:

- `web/05_01_CONTENT_LIBRARY.md` — W-01~03 (콘텐츠 라이브러리 화면)
- `web/05_02_SESSION_MANAGEMENT.md` — W-04~06 (세션 개설/수정)
- `web/05_03_LIVE_SESSION.md` — W-07~11 (실시간 진행)
- `web/05_04_HISTORY.md` — W-12~15 (이력)
- `app/06_01_WORKER_FLOW.md` — M-01~10 (근로자 플로우)
- `app/06_02_MANAGER_FLOW.md` — M-11~13 (모바일 관리자 + QR)
- `app/06_03_PUSH_NOTIFICATION.md` — 푸시 알림 클라이언트
