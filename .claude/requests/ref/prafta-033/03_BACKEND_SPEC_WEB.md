# TBM 모듈 백엔드 - 웹 API 사양

> 본 문서는 TBM 모듈의 **웹(Vue) 클라이언트 전용 API**를 정의합니다.
> 공통 컨벤션은 `02_BACKEND_SPEC_COMMON.md` 참조.
> 동일 도메인 로직(Service/Mapper)이 앱과 공유되더라도 **Controller는 분리**합니다.

---

## 1. URL 컨벤션

- **Base Path**: `/api/web/tbm`
- 웹/앱 분리: `/api/web/...` vs `/api/app/...`
- RESTful 명명 (동사 최소화, 리소스 중심)
- 액션이 필요한 경우 `:` 또는 동사 사용 (예: `/sessions/{no}/start`)

---

## 2. 인증 / 공통 헤더

| 헤더 | 용도 | 비고 |
|---|---|---|
| `Authorization` | `Bearer {JWT}` | 모든 API 필수 (공개 API 없음) |
| `Content-Type` | `application/json` 또는 `multipart/form-data` | 파일 업로드 시 multipart |

JWT에서 추출되는 정보 (`TokenInfo` 기존 패턴):
- `gv_cmpnyCd` — 회사 코드
- `gv_userId` — 사용자 ID
- `gv_userNm` — 사용자 이름
- `gv_siteCd` — 현재 사업장 (있는 경우)
- 권한 코드들

---

## 3. API 그룹 매핑 (화면 ↔ API)

| 화면 그룹 | 화면 ID | API 그룹 | 섹션 |
|---|---|---|---|
| 콘텐츠 라이브러리 | W-01~03 | `/api/web/tbm/contents/...` | §4 |
| TBM 세션 관리 | W-04~06 | `/api/web/tbm/sessions/...` | §5 |
| TBM 진행 (실시간) | W-07~11 | `/api/web/tbm/sessions/{no}/...` (액션) + SSE | §6 |
| 이력 관리 | W-12~15 | `/api/web/tbm/history/...`, `/api/web/tbm/attendance/...` | §7 |

---

## 4. 콘텐츠 라이브러리 API (W-01 ~ W-03)

### 4.1 콘텐츠 목록 조회 (W-01)

```
GET /api/web/tbm/contents
```

**쿼리 파라미터:**
| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `siteCd` | String | N | 사업장 필터 (지정 시 해당 사업장 + 회사 공통 조회) |
| `contentTypeCd` | String | N | IMAGE/VIDEO/YOUTUBE/PDF |
| `searchKeyword` | String | N | 제목/설명 검색 |
| `page` | Integer | N | 페이지 (default 1) |
| `pageSize` | Integer | N | 페이지 크기 (default 20) |

**응답:**
```json
{
    "contentList": [
        {
            "contentNo": 123,
            "siteCd": null,
            "contentTypeCd": "VIDEO",
            "title": "지게차 안전수칙",
            "description": "라인 작업 시 필수 시청",
            "filePath": "/files/content/123.mp4",
            "externalUrl": null,
            "thumbnailPath": "/files/thumb/123.jpg",
            "durationSec": 180,
            "fileSizeByte": 12345678,
            "mimeType": "video/mp4",
            "isCommonContent": true,
            "insertDate": "2026-05-27T09:30:00"
        }
    ],
    "totalCount": 42
}
```

**서비스 로직:**
- 회사 공통(`SITE_CD IS NULL`) + 자기 사업장 콘텐츠 함께 조회
- `USE_YN = 'Y'` 만
- `isCommonContent` = (`SITE_CD IS NULL`) 플래그

**DTO 클래스:**
- `ContentListRequest` (Lombok)
- `ContentListParam` (record, `from()` 메서드)
- `ContentListQuery` (record)
- `ContentResult` (record)
- `ContentListResponse` (Lombok @Builder)

### 4.2 콘텐츠 상세 조회 (W-03)

```
GET /api/web/tbm/contents/{contentNo}
```

**응답:** 단일 `ContentResult` + 사용된 TBM 세션 이력 (선택)

```json
{
    "content": { ... ContentResult ... },
    "usedSessions": [
        {
            "eduSessionNo": 100,
            "title": "5월 1주차 TBM",
            "insertDate": "2026-05-01T08:30:00"
        }
    ]
}
```

### 4.3 콘텐츠 등록 (W-02)

```
POST /api/web/tbm/contents
Content-Type: multipart/form-data
```

**Form Fields:**
| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `siteCd` | String | N | NULL이면 회사 공통 (master/safe만 가능) |
| `contentTypeCd` | String | Y | IMAGE/VIDEO/YOUTUBE/PDF |
| `title` | String | Y | |
| `description` | String | N | |
| `file` | File | 조건부 | IMAGE/VIDEO/PDF인 경우 필수 |
| `externalUrl` | String | 조건부 | YOUTUBE인 경우 필수 |

**검증:**
- `siteCd == null` 시 권한 체크 (master 또는 safe)
- 파일/URL 일관성: TYPE이 YOUTUBE면 `externalUrl` 필수, 나머지는 `file` 필수
- 파일 크기·MIME 타입 검증
- 썸네일 자동 생성 (영상은 첫 프레임, PDF는 첫 페이지, 이미지는 리사이즈)

**응답:**
```json
{
    "contentNo": 123,
    "filePath": "/files/content/123.mp4",
    "thumbnailPath": "/files/thumb/123.jpg"
}
```

### 4.4 콘텐츠 수정 (W-02)

```
PUT /api/web/tbm/contents/{contentNo}
```

**수정 가능 필드:** title, description, externalUrl(YOUTUBE만), file 교체

**제약:**
- 다른 사용자가 등록한 콘텐츠 수정 시 권한 체크
- 회사 공통 콘텐츠 수정은 master/safe만

### 4.5 콘텐츠 삭제 (W-01에서 액션)

```
DELETE /api/web/tbm/contents/{contentNo}
```

**동작:** 소프트 삭제 (`USE_YN = 'N'`)

**제약:**
- 진행 중인(`STATUS_CD = IN_PROGRESS`) 세션에 사용 중인 콘텐츠는 삭제 불가
- 회사 공통 콘텐츠 삭제는 master/safe만

---

## 5. TBM 세션 관리 API (W-04 ~ W-06)

### 5.1 세션 목록 조회 (W-04)

```
GET /api/web/tbm/sessions
```

**쿼리 파라미터:**
| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `siteCd` | String | N | 사업장 필터 |
| `statusCd` | String | N | DRAFT/OPENED/IN_PROGRESS/COMPLETED/CANCELLED |
| `startDate` | LocalDate | N | 기간 시작 |
| `endDate` | LocalDate | N | 기간 종료 |
| `searchKeyword` | String | N | 제목 검색 |
| `managerUserId` | String | N | 개설자 필터 |
| `page` | Integer | N | |
| `pageSize` | Integer | N | |

**응답:**
```json
{
    "sessionList": [
        {
            "eduSessionNo": 100,
            "siteCd": "A001",
            "siteNm": "본사 공장",
            "title": "5월 1주차 TBM",
            "statusCd": "COMPLETED",
            "managerUserId": "U0001",
            "managerUserNm": "김안전",
            "gpsVerifyTypeCd": "AUTO",
            "openedAt": "2026-05-01T08:00:00",
            "startedAt": "2026-05-01T08:05:00",
            "endedAt": "2026-05-01T08:15:00",
            "attendanceCount": 30,
            "completedCount": 28,
            "notCompletedCount": 2,
            "riskAssessmentCount": 2,
            "insertDate": "2026-05-01T07:30:00"
        }
    ],
    "totalCount": 156
}
```

**비고:** 집계 컬럼(`attendanceCount` 등)은 서비스에서 별도 쿼리 또는 SQL JOIN/SUBQUERY로 계산.

### 5.2 세션 상세 조회 (W-06)

```
GET /api/web/tbm/sessions/{eduSessionNo}
```

**응답:**
```json
{
    "session": {
        "eduSessionNo": 100,
        "siteCd": "A001",
        "siteNm": "본사 공장",
        "eduTypeCd": "TBM",
        "title": "5월 1주차 TBM",
        "contentBody": "<p>오늘 TBM 주제는...</p>",
        "contentFormatCd": "RICH_HTML",
        "statusCd": "OPENED",
        "entryPwd": "123456",
        "exitPwd": "654321",
        "managerUserId": "U0001",
        "managerUserNm": "김안전",
        "managerGpsLat": 37.5665,
        "managerGpsLng": 126.9780,
        "gpsVerifyTypeCd": "AUTO",
        "gpsVerifyRadiusM": 100,
        "gpsManualConfirmYn": "N",
        "openedAt": "2026-05-01T08:00:00",
        "startedAt": null,
        "endedAt": null,
        "insertDate": "2026-05-01T07:30:00"
    },
    "contents": [
        {
            "contentNo": 123,
            "displayOrder": 0,
            "title": "지게차 안전수칙",
            "contentTypeCd": "VIDEO",
            "filePath": "/files/content/123.mp4",
            "thumbnailPath": "/files/thumb/123.jpg",
            "durationSec": 180,
            "overrideDescription": null
        }
    ],
    "risks": [
        {
            "riskAssessmentNo": 55,
            "displayOrder": 0,
            "title": "지게차 추돌 위험",
            "statusCd": "COMPLETED"
        }
    ]
}
```

**비고:** 위험성평가 정보 조회는 기존 위험성평가 서비스 호출 (Claude Code 로컬 확인).

### 5.3 세션 개설 (W-05)

```
POST /api/web/tbm/sessions
```

**요청 본문:**
```json
{
    "siteCd": "A001",
    "title": "5월 1주차 TBM",
    "contentBody": "<p>오늘 TBM 주제는...</p>",
    "contentNos": [123, 124],
    "riskAssessmentNos": [55, 56],
    "managerGpsLat": 37.5665,
    "managerGpsLng": 126.9780,
    "gpsVerifyTypeCd": "AUTO",
    "gpsVerifyRadiusM": 100,
    "gpsManualConfirmYn": "N"
}
```

**검증:**
- `title`, `contentBody`, `siteCd` 필수
- `contentBody`가 빈 HTML(`<p></p>` 등)이면 거부 — 텍스트 추출 후 길이 검증
- `gpsVerifyTypeCd = MANUAL` 인 경우 `gpsManualConfirmYn = 'Y'` 필수
- `gpsVerifyTypeCd = AUTO` 인 경우 `managerGpsLat/Lng` 필수
- 위험성평가 미선택(`riskAssessmentNos`가 비어있음) 시: 거부하지 않음, **단 응답에 경고 메시지 포함**

**서비스 로직:**
1. 권한 체크 (safe 또는 회사별 커스텀 권한)
2. 입실/종료 비번 자동 생성 (`PwdGenerator`)
3. TB_EDU_SESSION INSERT (STATUS_CD='DRAFT' 또는 즉시 'OPENED')
4. TB_EDU_SESSION_CONTENT 매핑 INSERT
5. TB_EDU_SESSION_RISK 매핑 INSERT (옵션)
6. TB_EDU_SESSION_STATE 초기 row INSERT (UPSERT)

**응답:**
```json
{
    "eduSessionNo": 100,
    "statusCd": "OPENED",
    "entryPwd": "123456",
    "exitPwd": "654321",
    "warningMessage": "위험성평가 연동되지 않은 TBM입니다. 사고 발생 시 설득력이 떨어질 수 있습니다."
}
```

**개설 시점 STATUS 결정:**
- 기본: `DRAFT` (작성 중) → 별도 `/open` 호출 후 `OPENED`
- 또는: 즉시 `OPENED` (한 번에 처리)
- **권장: 즉시 OPENED** (단계 줄임). 별도 DRAFT 저장이 필요하면 향후 추가.

### 5.4 세션 수정 (W-06)

```
PUT /api/web/tbm/sessions/{eduSessionNo}
```

**수정 가능 시점:** `STATUS_CD IN ('DRAFT', 'OPENED')` 만. IN_PROGRESS 이후 수정 불가.

**수정 가능 필드:** title, contentBody, contents 매핑, risks 매핑, GPS 설정.
**수정 불가 필드:** entryPwd, exitPwd (재발급 필요 시 별도 API)

### 5.5 세션 취소 (W-04, W-06)

```
POST /api/web/tbm/sessions/{eduSessionNo}/cancel
```

**요청 본문:**
```json
{
    "cancelReason": "참석자 부재로 취소"
}
```

**제약:**
- `STATUS_CD IN ('DRAFT', 'OPENED')` 만 취소 가능
- IN_PROGRESS 이후는 강제 종료 API 사용

**서비스 로직:**
- TB_EDU_SESSION UPDATE (STATUS='CANCELLED', CANCELLED_AT, CANCEL_REASON)
- SSE 알림: 이미 입실한 참여자 있으면 `session.cancelled`

### 5.6 비번 재발급 (W-06, 선택)

```
POST /api/web/tbm/sessions/{eduSessionNo}/regenerate-passwords
```

**용도:** 비번 유출 의심 시 관리자가 재생성. STATUS_CD = OPENED 시점에만.

**응답:**
```json
{
    "entryPwd": "987654",
    "exitPwd": "456789"
}
```

---

## 6. TBM 진행 (실시간) API (W-07 ~ W-11)

### 6.1 교육 시작 (W-07)

```
POST /api/web/tbm/sessions/{eduSessionNo}/start
```

**요청 본문:** (없음 또는 빈 객체)

**제약:**
- `STATUS_CD = 'OPENED'` 만
- 권한: 개설자 본인 또는 safe

**서비스 로직:**
1. 상태 전이: OPENED → IN_PROGRESS, `STARTED_AT = NOW()`
2. SSE 알림: `session.started` (모든 참여자)

**응답:**
```json
{
    "statusCd": "IN_PROGRESS",
    "startedAt": "2026-05-01T08:05:00"
}
```

### 6.2 슬라이드/콘텐츠 변경 (W-07)

```
POST /api/web/tbm/sessions/{eduSessionNo}/sync-state
```

**요청 본문:**
```json
{
    "currentContentNo": 123,
    "currentSlideIndex": 2,
    "syncStateCd": "PLAYING"
}
```

**제약:**
- `STATUS_CD = 'IN_PROGRESS'` 만
- 권한: 개설자 본인 또는 safe

**서비스 로직:**
1. TB_EDU_SESSION_STATE UPSERT (`ON DUPLICATE KEY UPDATE ... AS NEW`)
2. SSE 알림: `session.state` (모든 참여자)

### 6.3 진행 중 참여자 명단 조회 (W-08)

```
GET /api/web/tbm/sessions/{eduSessionNo}/attendances/live
```

**응답:**
```json
{
    "attendances": [
        {
            "attendanceNo": 1001,
            "userType": "USER",
            "userId": "U0010",
            "userNm": "홍길동",
            "deptNm": "생산1팀",
            "entryTypeCd": "SELF_DEVICE",
            "entryAt": "2026-05-01T08:01:00",
            "entryDistanceM": 25,
            "exitAt": null,
            "anomalyFlags": {
                "backgroundCount": 0,
                "backgroundDurationSec": 0,
                "gpsOutOfRangeCount": 0,
                "networkLostCount": 0
            }
        },
        {
            "attendanceNo": 1002,
            "userType": "DAILY",
            "dailyUserNo": 5001,
            "userNm": "김일용",
            "phoneLast4": "1234",
            "entryTypeCd": "MANAGER_QR_SCAN",
            "entryByManagerUserId": "U0001",
            "entryAt": "2026-05-01T08:02:00",
            "exitAt": null,
            "anomalyFlags": { ... }
        }
    ],
    "summary": {
        "totalCount": 30,
        "regularCount": 25,
        "dailyCount": 5,
        "anomalyCount": 2
    }
}
```

**비고:**
- `anomalyFlags`는 `TB_EDU_ATTENDANCE_EVENT`를 집계해서 계산
- 실시간 갱신: 클라이언트가 주기적 폴링 또는 SSE 활용

### 6.4 일용직 QR 스캔 입실 (W-09)

```
POST /api/web/tbm/sessions/{eduSessionNo}/qr-attendance/enter
```

**요청 본문:**
```json
{
    "qrToken": "abc123xyz...",
    "managerGpsLat": 37.5665,
    "managerGpsLng": 126.9780
}
```

**서비스 로직:**
1. 권한 체크 (개설자 본인 또는 safe)
2. STATUS_CD 체크 (OPENED만, IN_PROGRESS 이후는 차단)
3. QR 토큰 → DAILY_USER_NO 변환 (기존 QR 서비스 호출)
4. **일용직 만료 검증** (`TB_DAILY_USER.EXPIRE_DT >= CURDATE()`)
5. 중복 출결 체크 (UK 제약)
6. TB_EDU_ATTENDANCE INSERT (DAILY_USER_NO, ENTRY_TYPE_CD='MANAGER_QR_SCAN', ENTRY_BY_MANAGER_USER_ID)
7. TB_EDU_ATTENDANCE_EVENT INSERT (ENTER)
8. SSE 알림: `attendance.updated` (관리자 화면용)

**응답:**
```json
{
    "attendanceNo": 1002,
    "dailyUser": {
        "dailyUserNo": 5001,
        "userNm": "김일용",
        "phoneLast4": "1234",
        "expireDt": "2026-06-30",
        "isFixed": false
    },
    "entryAt": "2026-05-01T08:02:00"
}
```

**에러 케이스:**
- QR 만료: `COMMON_403_003`
- 중복 출결: `COMMON_409_002`
- 상태 위반: `COMMON_409_001`

### 6.5 일용직 QR 스캔 종료 (W-09)

```
POST /api/web/tbm/sessions/{eduSessionNo}/qr-attendance/{attendanceNo}/exit
Content-Type: multipart/form-data
```

**Form Fields:**
| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `qrToken` | String | Y | 재스캔으로 검증 |
| `signatureFile` | File | Y | 일용직이 관리자 폰에 한 서명 이미지 |

**서비스 로직:**
1. 권한 체크
2. ATTENDANCE 조회 → QR 토큰 ↔ DAILY_USER_NO 일치 확인
3. 서명 파일 저장
4. TB_EDU_ATTENDANCE UPDATE (EXIT_TYPE_CD='MANAGER_QR_SCAN', EXIT_AT, EXIT_SIGNATURE_PATH)
5. TB_EDU_ATTENDANCE_EVENT INSERT (END)

**응답:**
```json
{
    "attendanceNo": 1002,
    "exitAt": "2026-05-01T08:15:00"
}
```

### 6.6 교육 종료 (W-10)

```
POST /api/web/tbm/sessions/{eduSessionNo}/end
```

**요청 본문:**
```json
{
    "forceEndOptions": {
        "applyToNotExited": true,
        "defaultCompletionStatus": "COMPLETED",
        "defaultReason": "관리자 일괄 처리"
    }
}
```

`forceEndOptions`가 제공되면 미종료자 일괄 강제 종료. 없으면 미종료자 그대로 두고 세션만 종료.

**서비스 로직:**
1. 권한 체크 (개설자 본인 또는 safe)
2. STATUS_CD 체크 (IN_PROGRESS만)
3. 미종료자(`EXIT_AT IS NULL`) 처리:
   - `forceEndOptions` 있으면 일괄 강제 종료
   - 없으면 그대로 (관리자가 W-11 화면에서 개별 처리)
4. TB_EDU_SESSION UPDATE (STATUS='COMPLETED', ENDED_AT)
5. SSE 알림: `session.ended` (모든 참여자)

**응답:**
```json
{
    "statusCd": "COMPLETED",
    "endedAt": "2026-05-01T08:15:00",
    "remainingNotExited": 0,
    "forcedCount": 2
}
```

### 6.7 미종료자 강제 종료 (W-11)

개별 처리. 종료 후 또는 종료 전 시점에서 호출.

```
POST /api/web/tbm/sessions/{eduSessionNo}/attendances/{attendanceNo}/force-end
```

**요청 본문:**
```json
{
    "completionStatusCd": "COMPLETED",
    "reason": "현장에서 직접 확인함. 자리에 끝까지 있었음."
}
```

**검증:**
- `reason` 필수 (빈 문자열 거부)
- `completionStatusCd`는 COMPLETED/NOT_COMPLETED

**서비스 로직:**
1. 권한 체크
2. ATTENDANCE 조회 (EXIT_AT IS NULL인 경우만 허용)
3. UPDATE: EXIT_TYPE_CD='MANAGER_FORCED', EXIT_BY_MANAGER_USER_ID, EXIT_AT=NOW, EXIT_FORCED_REASON, COMPLETION_STATUS_CD, NOT_COMPLETED_REASON(MISMATCH 시), STATUS_UPDATED_BY, STATUS_UPDATED_AT
4. TB_EDU_ATTENDANCE_EVENT INSERT (FORCED_END)

### 6.8 SSE 구독 (관리자 화면용 - 선택)

```
GET /api/web/tbm/sessions/{eduSessionNo}/sse
Accept: text/event-stream
```

관리자 화면에서 참여자 변동 알림 받기. 또는 폴링으로 대체 가능.

**이벤트 타입:**
- `attendance.updated` — 새 입실/종료/이벤트

---

## 7. 이력 관리 API (W-12 ~ W-15)

### 7.1 TBM 이력 조회 (W-12)

```
GET /api/web/tbm/history/sessions
```

쿼리 파라미터는 §5.1과 유사하되 **종료된(COMPLETED) 세션 중심** 조회.

**응답:** §5.1과 동일 구조 + 추가 통계 (집계 정보)

### 7.2 출결 상세 (W-13)

```
GET /api/web/tbm/sessions/{eduSessionNo}/attendances
```

**쿼리 파라미터:**
| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `userType` | String | N | USER / DAILY |
| `completionStatusCd` | String | N | COMPLETED / NOT_COMPLETED |
| `includeEventSummary` | Boolean | N | 이벤트 요약 포함 여부 (default true) |

**응답:**
```json
{
    "session": { ... SessionResult 요약 ... },
    "attendances": [
        {
            "attendanceNo": 1001,
            "userType": "USER",
            "userId": "U0010",
            "userNm": "홍길동",
            "deptNm": "생산1팀",
            "entryTypeCd": "SELF_DEVICE",
            "entryAt": "2026-05-01T08:01:00",
            "entryDistanceM": 25,
            "exitTypeCd": "SELF",
            "exitAt": "2026-05-01T08:15:00",
            "exitForcedReason": null,
            "completionStatusCd": "COMPLETED",
            "notCompletedReason": null,
            "entrySignaturePath": "/files/sig/1001-in.png",
            "exitSignaturePath": "/files/sig/1001-out.png",
            "statusUpdatedBy": null,
            "statusUpdatedAt": null,
            "eventSummary": {
                "backgroundCount": 1,
                "backgroundDurationSec": 30,
                "gpsOutOfRangeCount": 0,
                "networkLostCount": 0,
                "anomalyLevel": "NORMAL"
            }
        }
    ]
}
```

### 7.3 이벤트 로그 상세 (W-13 토글)

```
GET /api/web/tbm/attendances/{attendanceNo}/events
```

전체 이벤트 타임라인.

**응답:**
```json
{
    "events": [
        {
            "eventNo": 5001,
            "eventTypeCd": "ENTER",
            "eventTime": "2026-05-01T08:01:00",
            "serverReceivedAt": "2026-05-01T08:01:01",
            "eventData": null
        },
        {
            "eventNo": 5002,
            "eventTypeCd": "SLIDE_CHANGED",
            "eventTime": "2026-05-01T08:03:00",
            "eventData": { "contentNo": 123, "slideIndex": 1 }
        },
        {
            "eventNo": 5003,
            "eventTypeCd": "BACKGROUND_IN",
            "eventTime": "2026-05-01T08:08:00",
            "eventData": null
        }
    ]
}
```

### 7.4 미이수 처리 (W-14)

종료된 세션에 대해서도 사후 처리 가능 (분쟁 발생 시).

```
PUT /api/web/tbm/attendances/{attendanceNo}/completion
```

**요청 본문:**
```json
{
    "completionStatusCd": "NOT_COMPLETED",
    "reason": "동료 확인 결과 30분간 자리 비웠음 (휴게실에 있었음)"
}
```

**검증:**
- 권한: 개설자 본인, safe, 또는 master
- `reason` 필수 (특히 NOT_COMPLETED로 변경 시)

**서비스 로직:**
1. 변경 이력은 자동 (STATUS_UPDATED_BY, STATUS_UPDATED_AT 갱신)
2. NOT_COMPLETED 시 NOT_COMPLETED_REASON 저장
3. COMPLETED로 복귀 시 NOT_COMPLETED_REASON는 그대로 남김 (감사용)

**응답:** 갱신된 attendance row

### 7.5 사용자별 TBM 이수 이력 (W-15)

```
GET /api/web/tbm/history/users/{userId}/attendances
```

또는 일용직:
```
GET /api/web/tbm/history/daily-users/{dailyUserNo}/attendances
```

**쿼리 파라미터:**
| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `startDate` | LocalDate | N | |
| `endDate` | LocalDate | N | |
| `completionStatusCd` | String | N | |
| `page` | Integer | N | |
| `pageSize` | Integer | N | |

**응답:**
```json
{
    "user": {
        "userId": "U0010",
        "userNm": "홍길동",
        "deptNm": "생산1팀"
    },
    "attendances": [
        {
            "attendanceNo": 1001,
            "eduSessionNo": 100,
            "sessionTitle": "5월 1주차 TBM",
            "sessionDate": "2026-05-01",
            "entryAt": "2026-05-01T08:01:00",
            "exitAt": "2026-05-01T08:15:00",
            "completionStatusCd": "COMPLETED",
            "riskAssessmentCount": 2
        }
    ],
    "summary": {
        "totalCount": 22,
        "completedCount": 21,
        "notCompletedCount": 1
    },
    "totalCount": 22
}
```

**용도:** 향후 사고 관리 모듈에서 "사고 발생 인원의 TBM 이수 이력" 조회의 기반.

---

## 8. 컨트롤러 작성 가이드

### 8.1 클래스 분리

```
TbmContentWebController         # 콘텐츠 라이브러리 (§4)
TbmSessionWebController         # 세션 관리 (§5)
TbmSessionLiveWebController     # 진행 실시간 (§6)
TbmHistoryWebController         # 이력 (§7)
```

### 8.2 컨트롤러 예시

```java
@RestController
@RequestMapping("/api/web/tbm/sessions")
@RequiredArgsConstructor
public class TbmSessionWebController {

    private final TbmSessionService tbmSessionService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<SessionListResponse> getSessionList(
        @ModelAttribute SessionListRequest request,
        @RequestHeader("Authorization") String authorization
    ) {
        SessionListResponse response = tbmSessionService.getSessionList(
            SessionListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<SessionCreateResponse> createSession(
        @RequestBody SessionCreateRequest request,
        @RequestHeader("Authorization") String authorization
    ) {
        SessionCreateResponse response = tbmSessionService.createSession(
            SessionCreateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{eduSessionNo}/start")
    public ResponseEntity<SessionStateResponse> startSession(
        @PathVariable Long eduSessionNo,
        @RequestHeader("Authorization") String authorization
    ) {
        SessionStateResponse response = tbmSessionService.startSession(
            SessionStartParam.from(eduSessionNo, jwtUtil.getAllClaimsAsMap(authorization))
        );
        return ResponseEntity.ok(response);
    }

    // ... 이하 동일 패턴
}
```

### 8.3 응답 코드

| HTTP 코드 | 시나리오 |
|---|---|
| 200 OK | 조회 / 일반 액션 성공 |
| 201 Created | 리소스 생성 (세션 개설, 콘텐츠 등록) |
| 204 No Content | 삭제 / 빈 응답 |
| 400 Bad Request | 입력 검증 실패 |
| 401 Unauthorized | 인증 실패 / 비번 불일치 |
| 403 Forbidden | 권한 부족 / GPS 반경 이탈 / QR 만료 |
| 404 Not Found | 리소스 없음 |
| 409 Conflict | 상태 위반 / 중복 출결 |
| 500 Internal Server Error | 서버 오류 |

---

## 9. 본 문서에서 다루지 않는 부분

⚠️ **본 문서는 웹(Vue) 클라이언트 전용 API**입니다. 다음은 별도 문서:

- 앱(Flutter + Vue WebApp)이 호출하는 API → **`04_BACKEND_SPEC_APP.md`**
- 근로자(정규직)의 개인 TBM 참여 API → **`04_BACKEND_SPEC_APP.md`**
- 푸시 알림 발송 로직 → **`04_BACKEND_SPEC_APP.md` + `app/06_03_PUSH_NOTIFICATION.md`**

웹과 앱은 다음을 **공유**합니다 (Claude Code는 중복 구현하지 않음):
- Service 레이어 (`TbmSessionService` 등)
- Mapper 레이어 (`TbmSessionMapper` 등)
- DTO 중 Param/Query/Command/Result (도메인 로직 공통)
- Request/Response는 분리 (웹/앱 요구사항이 다를 수 있음)

웹/앱이 **분리**되는 부분:
- Controller (`/api/web/...` vs `/api/app/...`)
- Request/Response DTO (필요 시)
- 인증 흐름 (모바일은 디바이스 토큰 등 추가)
- 일부 검증 로직 (모바일은 GPS 필수, 웹은 옵션)

---

## 10. 다음 단계

`04_BACKEND_SPEC_APP.md`에서 앱 전용 API를 정의합니다:
- 근로자 TBM 참여 API (M-01 ~ M-10)
- 모바일 관리자 API (M-11 ~ M-13, 웹 API와 거의 동일하되 모바일 전용 응답 포맷)
- SSE 구독 (근로자용)
- 푸시 알림 등록/발송
