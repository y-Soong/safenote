# TBM 모듈 백엔드 공통 사양

> 본 문서는 TBM 모듈 백엔드(Spring Boot + MyBatis)의 공통 사양을 정의합니다.
> 웹/앱 API 사양(`03_BACKEND_SPEC_WEB.md`, `04_BACKEND_SPEC_APP.md`)은 본 문서의 컨벤션을 따릅니다.

---

## 1. 패키지 구조

```
com.prafta.safety.tbm
├── controller
│   ├── web                  # 웹 전용 컨트롤러
│   └── app                  # 앱 전용 컨트롤러
├── service
│   ├── TbmSessionService                  # 인터페이스
│   ├── TbmSessionServiceImpl
│   ├── TbmContentService
│   ├── TbmContentServiceImpl
│   ├── TbmAttendanceService
│   ├── TbmAttendanceServiceImpl
│   ├── TbmStateService                    # SSE 동기화 상태
│   ├── TbmStateServiceImpl
│   └── TbmPushService                     # 푸시 알림 (앱)
├── mapper
│   ├── TbmSessionMapper                   # 세션 CRUD
│   ├── TbmContentMapper                   # 콘텐츠 라이브러리
│   ├── TbmAttendanceMapper                # 출결
│   ├── TbmAttendanceEventMapper           # 이벤트 로그
│   ├── TbmStateMapper                     # 동기화 상태
│   ├── TbmRiskMapper                      # 위험성평가 매핑
│   └── TbmPwdFailMapper                   # 비번 실패 로그
├── dto
│   ├── request                            # 클라이언트 → 서버
│   ├── response                           # 서버 → 클라이언트
│   ├── param                              # 컨트롤러 → 서비스
│   ├── command                            # 서비스 → 매퍼 (쓰기)
│   ├── query                              # 서비스 → 매퍼 (읽기)
│   ├── result                             # 매퍼 → 서비스
│   └── model                              # 리스트 형태 request의 중간 모델
├── sse
│   └── TbmSseEmitterManager               # SseEmitter 관리
└── exception
    └── (CommonErrorCode 활용)
```

⚠️ **기존 패키지 구조 우선**: 위는 권장안이며, Prafta 기존 패키지 컨벤션이 다르면 그것을 따릅니다 (Claude Code 로컬 확인).

---

## 2. DTO 플로우 (Prafta 컨벤션 준수)

### 2.1 표준 흐름

```
request (클라이언트 요청, Lombok @Getter/@Setter)
   ↓ Param.from(request, tokenInfo)
param (record, JWT 정보 결합)
   ↓ Query.from(param) 또는 Command.from(param)
query / command (record, XML 전달용)
   ↓ MyBatis
result (record, XML 응답)
   ↓ Response.from(result)
response (Lombok @Getter/@Builder, 클라이언트로)
```

### 2.2 리스트 요청 처리

요청이 `List<XxxRequest>` 형태인 경우:

```
List<request>
   ↓
model (record, 단일 요소 표현)
   ↓
param (record, List<model> 포함)
```

예시 패턴 (메모리상 기존 패턴 따름):

```java
// Controller
@PostMapping("/web/tbm/sessions/bulk-cancel")
public ResponseEntity<?> cancelSessions(
    @RequestBody List<SessionCancelRequest> request,
    @RequestHeader("Authorization") String authorization
) {
    tbmSessionService.cancelSessions(
        SessionCancelParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))
    );
    return ResponseEntity.ok().build();
}
```

### 2.3 DTO 작성 컨벤션

#### request (Lombok)
```java
@Getter
@Setter
@NoArgsConstructor
public class SessionCreateRequest {
    private String siteCd;
    private String title;
    private String contentBody;
    private List<Long> contentNos;             // 첨부 콘텐츠 번호
    private List<Long> riskAssessmentNos;      // 연계 위험성평가 번호 (옵션)
    private BigDecimal managerGpsLat;          // null 가능
    private BigDecimal managerGpsLng;          // null 가능
    private String gpsVerifyTypeCd;            // AUTO/MANUAL/DISABLED
    private Integer gpsVerifyRadiusM;          // 기본 100
    private String gpsManualConfirmYn;         // MANUAL 시 'Y' 필수
}
```

#### param (record)
```java
public record SessionCreateParam(
    String siteCd,
    String title,
    String contentBody,
    List<Long> contentNos,
    List<Long> riskAssessmentNos,
    BigDecimal managerGpsLat,
    BigDecimal managerGpsLng,
    String gpsVerifyTypeCd,
    Integer gpsVerifyRadiusM,
    String gpsManualConfirmYn,
    String gvCmpnyCd,
    String gvUserId,
    String gvUserNm
) {
    public static SessionCreateParam from(SessionCreateRequest req, TokenInfo tokenInfo) {
        if (req == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - SessionCreateRequest");
        if (tokenInfo == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - TokenInfo");
        if (req.getSiteCd() == null || req.getSiteCd().isBlank())
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - siteCd");
        if (req.getTitle() == null || req.getTitle().isBlank())
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - title");
        if (req.getContentBody() == null || req.getContentBody().isBlank())
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - contentBody (교육 내용은 필수)");

        return new SessionCreateParam(
            req.getSiteCd(),
            req.getTitle(),
            req.getContentBody(),
            req.getContentNos() != null ? req.getContentNos() : List.of(),
            req.getRiskAssessmentNos() != null ? req.getRiskAssessmentNos() : List.of(),
            req.getManagerGpsLat(),
            req.getManagerGpsLng(),
            req.getGpsVerifyTypeCd() != null ? req.getGpsVerifyTypeCd() : "AUTO",
            req.getGpsVerifyRadiusM() != null ? req.getGpsVerifyRadiusM() : 100,
            req.getGpsManualConfirmYn() != null ? req.getGpsManualConfirmYn() : "N",
            tokenInfo.gv_cmpnyCd(),
            tokenInfo.gv_userId(),
            tokenInfo.gv_userNm()
        );
    }
}
```

#### command (record, INSERT/UPDATE)
```java
public record SessionInsertCommand(
    String cmpnyCd,
    String siteCd,
    String eduTypeCd,
    String title,
    String contentBody,
    String contentFormatCd,
    String statusCd,
    String entryPwd,
    String exitPwd,
    String managerUserId,
    BigDecimal managerGpsLat,
    BigDecimal managerGpsLng,
    String gpsVerifyTypeCd,
    Integer gpsVerifyRadiusM,
    String gpsManualConfirmYn,
    String gvUserId
) {
    public static SessionInsertCommand from(SessionCreateParam param, String entryPwd, String exitPwd) {
        return new SessionInsertCommand(
            param.gvCmpnyCd(),
            param.siteCd(),
            "TBM",
            param.title(),
            param.contentBody(),
            "RICH_HTML",
            "DRAFT",
            entryPwd,
            exitPwd,
            param.gvUserId(),
            param.managerGpsLat(),
            param.managerGpsLng(),
            param.gpsVerifyTypeCd(),
            param.gpsVerifyRadiusM(),
            param.gpsManualConfirmYn(),
            param.gvUserId()
        );
    }
}
```

#### query (record, SELECT 조건)
```java
public record SessionListQuery(
    String gvCmpnyCd,
    String siteCd,
    String statusCd,
    LocalDate startDate,
    LocalDate endDate,
    String searchKeyword
) {
    public static SessionListQuery from(SessionListParam param) {
        if (param == null)
            throw ApiException.appendf(CommonErrorCode.COMMON_400_001, "\n필수값 누락 - SessionListParam");
        return new SessionListQuery(
            param.gvCmpnyCd(),
            param.siteCd(),
            param.statusCd(),
            param.startDate(),
            param.endDate(),
            param.searchKeyword()
        );
    }
}
```

#### result (record, XML 응답)
```java
public record SessionResult(
    Long eduSessionNo,
    String cmpnyCd,
    String siteCd,
    String eduTypeCd,
    String title,
    String contentBody,
    String contentFormatCd,
    String statusCd,
    String entryPwd,
    String exitPwd,
    String managerUserId,
    String managerUserNm,
    BigDecimal managerGpsLat,
    BigDecimal managerGpsLng,
    String gpsVerifyTypeCd,
    Integer gpsVerifyRadiusM,
    String gpsManualConfirmYn,
    LocalDateTime openedAt,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    LocalDateTime insertDate
) {}
```

#### response (Lombok)
```java
@Getter
@Builder
public class SessionListResponse {
    private List<SessionResult> sessionList;
    private int totalCount;
}
```

---

## 3. MyBatis 컨벤션

### 3.1 SQL 코멘트

모든 SQL 첫 줄에 `/* MapperInterface.methodName */`:

```xml
<select id="selectSessionList" resultType="...">
    /* TbmSessionMapper.selectSessionList */
    SELECT ...
</select>
```

### 3.2 UPSERT 패턴

`ON DUPLICATE KEY UPDATE ... AS NEW` row alias (MySQL 8.0+):

```xml
<insert id="upsertSessionState">
    /* TbmStateMapper.upsertSessionState */
    INSERT INTO TB_EDU_SESSION_STATE (
        CMPNY_CD, EDU_SESSION_NO,
        CURRENT_CONTENT_NO, CURRENT_SLIDE_INDEX, SYNC_STATE_CD,
        LAST_UPDATED_BY,
        INSERT_NO, INSERT_DATE
    ) VALUES (
        #{cmpnyCd}, #{eduSessionNo},
        #{currentContentNo}, #{currentSlideIndex}, #{syncStateCd},
        #{gvUserId},
        #{gvUserId}, NOW(3)
    ) AS NEW
    ON DUPLICATE KEY UPDATE
        CURRENT_CONTENT_NO  = NEW.CURRENT_CONTENT_NO,
        CURRENT_SLIDE_INDEX = NEW.CURRENT_SLIDE_INDEX,
        SYNC_STATE_CD       = NEW.SYNC_STATE_CD,
        LAST_UPDATED_BY     = NEW.LAST_UPDATED_BY,
        UPDATE_NO           = NEW.LAST_UPDATED_BY,
        UPDATE_DATE         = NOW(3)
</insert>
```

### 3.3 멀티테넌시 필터 (CMPNY_CD)

**모든 쿼리에 `CMPNY_CD = #{gvCmpnyCd}` 조건 필수**.
누락 시 다른 회사 데이터 노출 위험. 코드 리뷰 시 핵심 체크 항목.

### 3.4 페이징

기존 Prafta 패턴 따름 (LIMIT/OFFSET 또는 마이바티스 페이저).

---

## 4. 권한 체크

### 4.1 기본 권한 체계

| 권한 코드 | 설명 |
|---|---|
| `master` | 최고 관리자 |
| `safe` | 안전관리 최고 |
| `hr` | 근태 최고 |
| `999999` | 일반 사용자 |
| 회사별 커스텀 | 화면/버튼 단위 권한 |

### 4.2 TBM 권한 매트릭스

| API/액션 | 필요 권한 |
|---|---|
| 회사 공통 콘텐츠 등록/수정/삭제 (SITE_CD=NULL) | master 또는 safe |
| 사업장 콘텐츠 등록/수정/삭제 | safe 또는 회사별 커스텀 권한 (콘텐츠 관리) |
| 콘텐츠 조회 | 모든 관리자 (자기 사업장 + 회사 공통) |
| TBM 세션 개설/수정/취소 | safe 또는 회사별 커스텀 권한 |
| TBM 세션 진행 (시작/슬라이드/종료) | 개설자 본인 또는 safe |
| 강제 종료, 미이수 처리 | 개설자 본인 또는 safe |
| 일용직 QR 스캔 입실/종료 | 개설자 본인 또는 safe |
| 출결 이력 조회 | master, safe, 회사별 커스텀 권한 |
| TBM 참여 (정규직) | 999999 (본인 디바이스, GPS, 비번) |

### 4.3 권한 체크 구현 패턴

기존 권한 시스템 활용. 예시:

```java
@Service
public class TbmContentServiceImpl implements TbmContentService {

    @Override
    public void createContent(ContentCreateParam param) {
        // 회사 공통 콘텐츠 권한 체크
        if (param.siteCd() == null) {
            if (!hasAuthority(param.gvUserId(), "master", "safe")) {
                throw ApiException.appendf(
                    CommonErrorCode.COMMON_403_001,
                    "\n회사 공통 콘텐츠는 master 또는 safe만 등록 가능합니다."
                );
            }
        }
        // ...
    }
}
```

⚠️ 실제 권한 체크 메서드(`hasAuthority` 등)는 기존 Prafta 유틸 활용. Claude Code 로컬 확인.

### 4.4 멀티테넌시 검증

- 토큰의 `gv_cmpnyCd`와 요청 데이터의 `CMPNY_CD` 일치 검증
- 사업장 권한이 있는 사용자는 해당 사업장 데이터만 접근
- 회사 공통 콘텐츠 조회는 자기 회사 + (SITE_CD IS NULL) 범위

---

## 5. 예외 처리

### 5.1 ApiException 패턴

```java
throw ApiException.appendf(
    CommonErrorCode.COMMON_400_001,
    "\n필수값 누락 - title"
);
```

### 5.2 TBM 모듈 주요 에러 케이스

기존 `CommonErrorCode` 활용. 필요 시 TBM 전용 코드 추가 (Prafta 코드 체계 따라 결정):

| 시나리오 | 에러 코드 (예시) | 메시지 |
|---|---|---|
| 필수값 누락 | `COMMON_400_001` | "필수값 누락 - {fieldName}" |
| 권한 부족 | `COMMON_403_001` | "권한이 없습니다 - {action}" |
| 데이터 없음 | `COMMON_404_001` | "데이터를 찾을 수 없습니다 - {entity}" |
| 상태 위반 (입실 시 IN_PROGRESS) | `COMMON_409_001` | "현재 상태에서 불가능한 작업 - {state} → {action}" |
| 비밀번호 불일치 | `COMMON_401_001` | "비밀번호가 일치하지 않습니다" |
| GPS 반경 이탈 | `COMMON_403_002` | "교육 장소에서 너무 멀리 떨어져 있습니다 ({distance}m)" |
| 일용직 QR 만료 | `COMMON_403_003` | "만료된 QR 코드입니다. 재등록이 필요합니다." |
| 중복 출결 | `COMMON_409_002` | "이미 참여한 교육입니다." |
| 잘못된 상태 전이 | `COMMON_409_003` | "잘못된 상태 전이 - {from} → {to}" |

⚠️ 위는 예시. 실제 코드 번호는 기존 `CommonErrorCode` enum 확인 후 매핑.

### 5.3 에러 응답 형식

기존 Prafta 패턴 따름 (`@ControllerAdvice` 등).

---

## 6. 상태 전이 검증

### 6.1 세션 상태 전이 규칙

| 현재 | 가능한 다음 상태 | 트리거 |
|---|---|---|
| DRAFT | OPENED, CANCELLED | 개설 / 취소 |
| OPENED | IN_PROGRESS, CANCELLED | 교육 시작 / 취소 |
| IN_PROGRESS | COMPLETED | 교육 종료 |
| COMPLETED | (불변) | - |
| CANCELLED | (불변) | - |

### 6.2 서비스 레이어 검증 패턴

```java
@Service
public class TbmSessionServiceImpl implements TbmSessionService {

    @Transactional
    public void startSession(SessionStartParam param) {
        SessionResult session = tbmSessionMapper.selectSessionByNo(
            SessionFindQuery.from(param)
        );
        if (session == null) {
            throw ApiException.appendf(CommonErrorCode.COMMON_404_001, "\n세션 없음");
        }
        if (!"OPENED".equals(session.statusCd())) {
            throw ApiException.appendf(
                CommonErrorCode.COMMON_409_003,
                String.format("\n잘못된 상태 전이 - %s → IN_PROGRESS", session.statusCd())
            );
        }
        // 상태 전이 + SSE 알림
        // ...
    }
}
```

### 6.3 입실 가능 조건

```java
public void enterSession(SessionEnterParam param) {
    SessionResult session = ...;

    // 1. 상태 체크
    if (!List.of("OPENED").contains(session.statusCd())) {
        // ⚠️ 정책: OPENED만 입실 허용 (IN_PROGRESS 이후 입실 차단)
        throw ApiException.appendf(CommonErrorCode.COMMON_409_001, "\n입실 불가 상태");
    }

    // 2. 비번 검증
    if (!session.entryPwd().equals(param.entryPwd())) {
        // 실패 로그 INSERT
        tbmPwdFailMapper.insertPwdFail(...);
        throw ApiException.appendf(CommonErrorCode.COMMON_401_001, "\n비밀번호 불일치");
    }

    // 3. GPS 검증 (AUTO 모드만)
    if ("AUTO".equals(session.gpsVerifyTypeCd())) {
        int distance = calculateDistance(
            session.managerGpsLat(), session.managerGpsLng(),
            param.userGpsLat(), param.userGpsLng()
        );
        if (distance > session.gpsVerifyRadiusM()) {
            throw ApiException.appendf(
                CommonErrorCode.COMMON_403_002,
                String.format("\n교육 장소 반경 이탈 - %dm", distance)
            );
        }
    }
    // MANUAL/DISABLED는 GPS 검증 스킵

    // 4. 중복 출결 체크 (UK 제약으로 DB에서도 잡힘)
    // 5. INSERT TB_EDU_ATTENDANCE
    // 6. INSERT TB_EDU_ATTENDANCE_EVENT (ENTER)
}
```

---

## 7. SSE (Server-Sent Events) 동기화

### 7.1 SSE 전체 흐름

```
[관리자 액션]
  ↓ 슬라이드 변경 / 콘텐츠 시작 / 일시정지
[Spring Boot]
  ↓ TbmStateMapper.upsertSessionState (DB 업데이트)
  ↓ TbmSseEmitterManager.broadcastToSession (모든 참여자에게 푸시)
[클라이언트]
  ↓ EventSource onmessage
  ↓ 화면 동기화
```

### 7.2 SseEmitterManager 인터페이스 (개념)

```java
@Component
public class TbmSseEmitterManager {

    // 세션별 SseEmitter 목록 (in-memory)
    private final Map<Long, List<SseEmitter>> emittersBySession = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long sessionNo, String userId) {
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(30).toMillis());
        emittersBySession.computeIfAbsent(sessionNo, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(sessionNo, emitter));
        emitter.onTimeout(() -> remove(sessionNo, emitter));
        emitter.onError(e -> remove(sessionNo, emitter));

        // 초기 상태 전송
        sendCurrentState(emitter, sessionNo);

        return emitter;
    }

    public void broadcastToSession(Long sessionNo, SseEvent event) {
        List<SseEmitter> emitters = emittersBySession.get(sessionNo);
        if (emitters == null) return;

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name(event.type())
                    .data(event.payload())
                );
            } catch (Exception e) {
                remove(sessionNo, emitter);
            }
        }
    }

    private void remove(Long sessionNo, SseEmitter emitter) {
        List<SseEmitter> list = emittersBySession.get(sessionNo);
        if (list != null) list.remove(emitter);
    }
}
```

### 7.3 SSE 이벤트 타입

| 이벤트 | 페이로드 | 용도 |
|---|---|---|
| `session.started` | `{ startedAt }` | 관리자가 교육 시작 → 클라이언트 입실 차단 |
| `session.state` | `{ currentContentNo, currentSlideIndex, syncStateCd }` | 슬라이드/콘텐츠 동기화 |
| `session.ended` | `{ endedAt, exitPwd }` | 종료 트리거 |
| `session.cancelled` | `{ reason }` | 취소 |
| `attendance.updated` | `{ attendanceNo, action, userInfo }` | 관리자 화면용 (참여자 변경 알림) |

### 7.4 클라이언트 측 처리 (참고)

```javascript
// 근로자 측 (Vue + Flutter WebApp)
const evtSource = new EventSource(`${API_BASE}/api/app/tbm/sessions/${sessionNo}/sse`);

evtSource.addEventListener('session.state', (e) => {
    const data = JSON.parse(e.data);
    updateCurrentContent(data.currentContentNo, data.currentSlideIndex);
});

evtSource.addEventListener('session.ended', (e) => {
    showExitPwdDialog();
});
```

### 7.5 SSE 운영 고려

- **연결 끊김 처리**: 클라이언트는 자동 재연결 시도. 재연결 시 `/sse` 호출하면 현재 상태 즉시 푸시.
- **다수 인스턴스 환경**: 향후 Redis Pub/Sub 또는 메시지 큐로 확장 가능. MVP는 단일 인스턴스 가정.
- **인증**: SSE 연결 시 JWT 토큰 검증 (헤더 또는 쿼리 파라미터).

---

## 8. 트랜잭션 경계

### 8.1 주요 트랜잭션 시나리오

| 시나리오 | 트랜잭션 범위 |
|---|---|
| 세션 개설 | TB_EDU_SESSION INSERT + TB_EDU_SESSION_CONTENT INSERT(다건) + TB_EDU_SESSION_RISK INSERT(다건) |
| 정규직 입실 | TB_EDU_ATTENDANCE INSERT + TB_EDU_ATTENDANCE_EVENT INSERT |
| 일용직 QR 입실 | TB_DAILY_USER 만료 검증 (SELECT) + TB_EDU_ATTENDANCE INSERT + TB_EDU_ATTENDANCE_EVENT INSERT |
| 교육 시작 | TB_EDU_SESSION UPDATE (STATUS, STARTED_AT) + TB_EDU_SESSION_STATE UPSERT |
| 정규직 종료 | TB_EDU_ATTENDANCE UPDATE (EXIT_AT, SIGNATURE) + TB_EDU_ATTENDANCE_EVENT INSERT |
| 관리자 강제 종료 | TB_EDU_ATTENDANCE UPDATE (EXIT_TYPE, REASON, COMPLETION) + TB_EDU_ATTENDANCE_EVENT INSERT |
| 교육 완전 종료 | TB_EDU_SESSION UPDATE (STATUS, ENDED_AT) |

### 8.2 SSE 알림은 트랜잭션 밖

```java
@Transactional
public void startSession(SessionStartParam param) {
    // 1. DB 업데이트
    tbmSessionMapper.updateStatusToInProgress(...);
    tbmStateMapper.upsertSessionState(...);

    // 2. 커밋 후 SSE (트랜잭션 밖에서)
    // 방법 A: AFTER_COMMIT 동기화
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sseManager.broadcastToSession(param.eduSessionNo(),
                    new SseEvent("session.started", ...));
            }
        }
    );
}
```

이유: 트랜잭션 롤백 시 SSE 알림이 잘못 나가지 않도록.

---

## 9. 멱등성 (Idempotency)

### 9.1 입실 API

UK 제약 (CMPNY_CD + EDU_SESSION_NO + USER_ID/DAILY_USER_NO)으로 자연스럽게 멱등.

```java
try {
    tbmAttendanceMapper.insertAttendance(...);
} catch (DuplicateKeyException e) {
    // 이미 입실한 케이스 → 200 OK 또는 의미 있는 응답
    throw ApiException.appendf(CommonErrorCode.COMMON_409_002, "\n이미 참여 중");
}
```

### 9.2 이벤트 로그

이벤트 로그는 멱등성 불필요 (중복 발생 시에도 모두 기록). 단 시간 정밀도(밀리초)로 동일 시각 중복 가능성 낮음.

---

## 10. 외부 의존 활용 (Claude Code 로컬 확인)

본 모듈에서 호출/참조하는 외부 컴포넌트:

| 의존 항목 | 용도 | 확인 필요 |
|---|---|---|
| `JwtUtil.getAllClaimsAsMap` | JWT → TokenInfo 추출 | 기존 메서드 시그니처 |
| 권한 체크 유틸 | 사용자 권한 검증 | 기존 메서드 (예: `hasAuthority`, `checkRole`) |
| `ApiException` / `CommonErrorCode` | 예외 처리 | 기존 에러 코드 enum |
| 일용직 만료 검증 | `TB_DAILY_USER` 만료일 컬럼 | 정확한 컬럼명·체크 로직 |
| QR 토큰 디코딩 | QR → DAILY_USER_NO 변환 | 기존 QR 발급/검증 서비스 |
| 위험성평가 조회 | 매핑 시 위험성평가 정보 표시 | 기존 위험성평가 서비스 |
| 사업장 조회 | SITE_CD 검증 | 기존 사업장 서비스 |
| 사용자 정보 조회 | USER_ID → 이름/소속 | 기존 사용자 서비스 |
| 파일 업로드 | 콘텐츠 파일, 서명 이미지 | 기존 파일 저장 서비스 |

⚠️ Claude Code 작업 시: 위 의존성은 기존 Prafta 코드베이스 활용. 헷갈리면 사용자에게 질문.

---

## 11. 공통 유틸리티

### 11.1 GPS 거리 계산

```java
public class GpsUtil {
    private static final double EARTH_RADIUS_M = 6_371_000;

    public static int distanceMeters(
        BigDecimal lat1, BigDecimal lng1,
        BigDecimal lat2, BigDecimal lng2
    ) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return Integer.MAX_VALUE;
        }
        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double deltaLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double deltaLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                 + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                 * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (int) Math.round(EARTH_RADIUS_M * c);
    }
}
```

### 11.2 랜덤 비번 생성

```java
public class PwdGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate6Digit() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    public static String generateDifferent6Digit(String exclude) {
        String pwd;
        do {
            pwd = generate6Digit();
        } while (pwd.equals(exclude));
        return pwd;
    }
}
```

세션 개설 시:
```java
String entryPwd = PwdGenerator.generate6Digit();
String exitPwd = PwdGenerator.generateDifferent6Digit(entryPwd);
```

### 11.3 디바이스 토큰 추출

```java
// 클라이언트가 헤더로 전달
@RequestHeader("X-Device-Token") String deviceToken
```

토큰 발급/관리 정책은 기존 Prafta 컨벤션 따름.

---

## 12. 다음 단계

본 공통 사양을 기반으로:

1. `03_BACKEND_SPEC_WEB.md` — 웹 API 컨트롤러/엔드포인트
2. `04_BACKEND_SPEC_APP.md` — 앱 API 컨트롤러/엔드포인트

각 문서는 본 문서의 컨벤션을 참조하며, 화면별 API 명세를 구체화합니다.
