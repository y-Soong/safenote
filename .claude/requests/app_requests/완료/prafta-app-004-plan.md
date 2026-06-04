# prafta-app-004 분해/플랜 — TBM 입실·종료(앱) + 스케줄 없는 날 추가근무 상신 강제

> 작성: planner (자율 라운드). 산출물 파일 저장만 — Notion 등록은 메인 세션 담당.
> 원본 작업서: `.claude/requests/app_requests/prafta-app-004.md`
> 법적 책임 도메인(안전교육/근태) — 우선순위 +1단계 격상 대상.
> 스키마는 **prafta-033-A 마이그레이션 SQL(실파일)** 로 전량 재확인 완료(작업서 기입분 일치). 추측 0.

---

## 0. 정독·확인 결과 (근거 출처)

### 0-1. 스키마 확인 (실파일 — 작업서 기입분과 일치 확인)
- `prafta-backend/src/main/resources/sql/migration/prafta-033-A-tbm-session.sql` — tb_tbm_session(+ session_content/risk/state)
- `.../prafta-033-A-tbm-attendance.sql` — tb_tbm_attendance, tb_tbm_attendance_event, tb_tbm_pwd_fail
- `.../prafta-033-A-codes.sql` — SYS046~SYS055 시드
- ⚠️ **이 3개 마이그레이션은 "작성만, 운영 적용은 사용자 수동(read-only MCP)"** 라고 헤더에 명시됨. **운영 DB 적용 여부는 미확인**(메인이 MCP로 `SHOW TABLES LIKE 'tb_tbm_session'`·SYS046~055 존재 확인 필요). → §스키마갭 G1.

### 0-2. 정책서 (INDEX 경유 정독)
- `attd/07-checkin-checkout.md` §7.5 — 스케줄 없는 날: 출퇴근 허용 / 전량 추가근무 / 상신·승인 필수 / 미승인 시 마감 차단
- `attd/09-requests-approval.md` §9.3 — 초과근무 상신(사전/사후, 필수입력=일자/시작·종료/사유, 대기→승인 반영, §9.3.3 "휴일·스케줄 없는 날 근무=전량 초과근무 상신")
- `request-approval/03-policy-alignment.md` §3.2 — **사후 상신 기한 D+5 → 사업장별 근태 마감 전까지로 재정의(단일 출처 우선)**. §3.3 자동 마감 금지·강제 마감 미도입.
- `attd/13-attendance-close.md` §13.3 — 마감 차단 조건에 "미승인 추가근무(스케줄 없는 날 근무 포함)" 명시됨.
- ⚠️ TBM 입실/종료 **서명 필수 여부, 비번 실패 잠금**의 정책 본문 출처는 `attd/`·`request-approval/` INDEX에서 발견 못함. TBM은 prafta-033 기획 기반 도메인이고 정책서 본문 미수록 → **정책 미확정**(설계결정 C-D1/C-D4).

### 0-3. 재사용 자산 (실파일 확인)
- GPS 브리지: `src/utils/gpsBridge.js` `requestGps({timeoutMs})` → `{status:'OK',lat,lon,accuracy,isMocked}` / PERMISSION_DENIED / SERVICE_DISABLED / TIMEOUT / BRIDGE_UNAVAILABLE. (app-003 A0-1)
- QR: `src/views/_common/QrScanner.vue`(라우트 `/QrScanner`, html5-qrcode) + Flutter `qr_scan_page.dart`.
- 기존 TBM 읽기 카드: `src/views/main/components/TbmAttendCard.vue` — 4상태(BEFORE_CHECK_IN/AVAILABLE/ATTENDED/NONE) 읽기전용, `@click:attend` emit만(본 작업이 실제 액션 연결). prop명 tbmStatus/sessionTime/sessionLocation/sessionLeader/attendedAt.
- 디자인 토큰: `src/views/attd/MyAttendanceView.vue` / `MainView.vue` 루트에 1회 선언(자식 scoped는 상속). 핵심: `--color-primary:#16a34a`, `--color-warning*`, `--color-danger*`, `--color-text-primary/secondary/tertiary`, `--color-surface/bg/border/border-light/overlay`, `--radius-sm/md/lg/xl/full`, `--space-xs/sm/md/lg`. **신규 화면 루트도 같은 토큰 세트를 1회 선언**하는 것이 앱 컨벤션.
- 라우트: `src/router/index.js` 정적 배열 + beforeEach(토큰 게이트). 신규 보호 화면은 정적 등록(예: `/TbmEntry`).
- 액션시트 패턴: `src/views/attd/components/AttendanceActionSheet.vue` — 바텀시트(v-model open) + 액션 4종(scheduleModify/attendanceCorrection/overtime/leave). 초과근무 신청은 이미 `canRequestOvertime` 게이팅으로 존재.

---

## 1. 작업 단위표 (APP004-Cx = TBM, APP004-Ox = §7.5)

| 단위ID | 유형 | 사이즈 | 제목 | 선행 | 정책/근거 출처 |
|---|---|---|---|---|---|
| **APP004-C0** | 조사 | S | TBM 입실/종료 정합 조사(033-C 상태전이·서명·잠금 정책 확정, DB 적용 확인) | 없음 | prafta-033-A SQL, attd(TBM 본문 부재 확인), 메인 MCP |
| **APP004-C1** | backend | M | TBM 입실 엔드포인트(`POST /appApi/tbm/enter`) — 정규직 MVP | C0 | prafta-033-A 스키마, §0-2 |
| **APP004-C2** | backend | M | TBM 종료 엔드포인트(`POST /appApi/tbm/exit`) — 정규직 MVP | C1 | prafta-033-A 스키마 |
| **APP004-C3** | backend | S | TBM 세션 입실 컨텍스트 조회(`GET /appApi/tbm/entry-context`) | C0 | prafta-033-A 스키마 |
| **APP004-C4** | frontend | M | TBM 입실/종료 화면(`TbmEntryView.vue`) — 비번/QR/GPS/서명/종료 | C1,C2,C3 | UI-A0xx(본 문서 골격) |
| **APP004-C5** | frontend | S | 메인 TbmAttendCard 액션 연결(입실 화면 진입 라우팅) | C4 | TbmAttendCard.vue 기존 emit |
| **APP004-C6** | flutter | S | (서명 필수 확정 시) 서명 캡처/파일 연계 검토 — 조건부 | C0 | C-D1 결정 |
| **APP004-O1** | backend | M | 스케줄 없는 날 미승인 초과근무 → 마감 차단/액션필요 판정 연계 | 없음(prafta-028 선행) | §7.5, §13.3, prafta-028 |
| **APP004-O2** | backend | S | 앱 초과근무 상신 엔드포인트(웹 OT 생성 재사용/래핑) | O1 | §9.3, prafta-020/025/027 |
| **APP004-O3** | frontend | S | 스케줄 없는 날 "추가근무 상신 필요" 유도 + 상신 폼 진입 | O2 | §7.5, §9.3, app-002 actionSheet |

### 사이즈 합계: C = S·M·M·S·M·S(+조건부) ≈ 중대형 / O = M·S·S ≈ 중형
### 5단위 초과 → 본 플랜은 **C 라인(C0~C5)** 을 1차 분해 본체로, **O 라인(O1~O3)** 을 동시 정리하되 착수는 C 우선(작업서 권고: C가 사용자 가치 큼).

---

## 2. C 라인 상세 (TBM 입실/종료)

### APP004-C0 — 조사 (S, 선행 없음)
- **목표**: 구현 전 미확정 사실 확정.
- **확인 항목**:
  1. prafta-033 세션 상태전이(DRAFT→OPENED→IN_PROGRESS→COMPLETED) 중 **앱 입실 가능 구간** 확정. STARTED_AT/ENDED_AT은 `[C단계]`(prafta-033-C 실시간, DEFERRED) 컬럼 → IN_PROGRESS 전이가 미구현일 수 있음. **OPENED 부터 입실 허용**이 MVP 후보(ENTRY_PWD가 OPENED부터 생성됨). → 메인 확정.
  2. tb_tbm_session/attendance/pwd_fail **운영 DB 적용 여부**(MCP SHOW TABLES). SYS046~055 운영 등록.
  3. 입실/종료 **서명 필수 여부 + 파일관리 연계 경로**(ENTRY/EXIT_SIGN_FILE_MGMT_CD). 기존 앱 파일 업로드 유틸/엔드포인트 존재 여부.
  4. 비번 실패 **잠금 임계** 정책(tb_tbm_pwd_fail은 로그만, 잠금 로직 미정).
- **산출물**: C0 조사 결과 메모(메인이 설계결정 확정에 사용).
- **영향파일**: 없음(읽기/MCP).

### APP004-C1 — 입실 엔드포인트 (M, 선행 C0)
- **유형**: backend / 신규 모듈 `com.prafta.app.tbm`.
- **계약 초안**: `POST /prafta/appApi/tbm/enter`
  - 요청: `{ sessionCd, entryType('SELF_DEVICE'|'MANAGER_QR_SCAN'), entryPwd?, gps:{lat,lon,accuracy,isMocked,status}, entrySignFileMgmtCd? }`
    - SELF_DEVICE 경로: entryPwd 필수. MANAGER_QR_SCAN: QR 페이로드(sessionCd 등)로 진입(관리자 처리분이지만 앱 본인 디바이스 자가 입실 MVP는 비번 경로 우선).
  - 서버 검증:
    1. 세션 존재 + STATUS_CD ∈ 입실허용구간(C0 확정). CANCELLED/COMPLETED/DRAFT 차단.
    2. ENTRY_PWD 일치(불일치 → tb_tbm_pwd_fail INSERT(PWD_TYPE_CD='ENTRY') + 실패 응답; 잠금은 C0-D4).
    3. GPS_VERIFY_TYPE_CD=AUTO → haversine(요청 lat/lon, MANAGER_GPS_LAT/LON) ≤ GPS_VERIFY_RADIUS_M. 초과 시 차단/경고(C-D5). MANUAL/DISABLED 분기.
    4. UNIQUE(CMPNY_CD,SESSION_CD,USER_TYPE_CD='REGULAR',USER_CD) — 중복 입실 멱등(기입실이면 기존 출결 반환).
    5. USER_CD/CMPNY_CD는 JWT(TokenInfo) 클레임에서 도출(요청 바디 신뢰 금지). USER_TYPE_CD='REGULAR' 고정(MVP).
  - 처리: tb_tbm_attendance INSERT(ENTRY_TYPE_CD, ENTRY_AT, ENTRY_GPS_*, ENTRY_DISTANCE_M, ENTRY_SIGN_FILE_MGMT_CD?). tb_tbm_attendance_event(ENTER) append.
  - 응답: `{ attendanceCd, entryAt, entryDistanceM, completionStatusCd, sessionStatusCd }`
- **영향파일(예상)**:
  - `app/tbm/controller/TbmAttendanceController.java`
  - `app/tbm/service/TbmAttendanceService.java` + `impl/TbmAttendanceServiceImpl.java`
  - `app/tbm/mapper/TbmAttendanceMapper.java` + `resources/.../mapper/TbmAttendanceMapper.xml`
  - `app/tbm/dto/*`(EnterCommand, EntryContextResult 등) / `result/*`
  - 거리계산 유틸(기존 출퇴근 haversine 재사용 검토 — app-003 산출물).
- **보안 위임**: IDOR(타 회사 세션 입실), GPS 위조(isMocked), 비번 무차별(잠금)은 security 라운드.

### APP004-C2 — 종료 엔드포인트 (M, 선행 C1)
- **계약 초안**: `POST /prafta/appApi/tbm/exit`
  - 요청: `{ sessionCd, exitType('SELF'|'MANAGER_QR_SCAN'), exitPwd?, exitSignFileMgmtCd?, completionStatusCd? }`
  - 검증: 본인 출결(ENTRY_AT 존재, EXIT_AT NULL) 존재. SELF → EXIT_PWD 일치(실패→pwd_fail PWD_TYPE_CD='EXIT'). 세션 상태 확인.
  - 처리: UPDATE tb_tbm_attendance(EXIT_TYPE_CD, EXIT_AT, EXIT_SIGN_FILE_MGMT_CD?, COMPLETION_STATUS_CD). event(END) append.
  - 응답: `{ attendanceCd, exitAt, completionStatusCd }`
- **영향파일**: C1과 동일 컨트롤러/서비스/매퍼에 메서드 추가.
- ⚠️ MANAGER_FORCED(EXIT_TYPE_CD)는 관리자 웹/앱 강제종료 — **본 앱 작업 범위 밖**(C-D2 일용직·강제종료 범위 결정 참조).

### APP004-C3 — 입실 컨텍스트 조회 (S, 선행 C0)
- **계약 초안**: `GET /prafta/appApi/tbm/entry-context?sessionCd=...` (또는 오늘자 가용 세션 목록)
  - 응답: `{ sessionCd, title, status, gpsVerifyType, gpsVerifyRadiusM, managerGps:{lat,lon}, requireEntrySign(boolean=C-D1), alreadyEntered(boolean), exitPwdRequired(boolean) }`
  - 화면이 입실 전 분기(비번/GPS/서명 노출 여부)에 사용. MANAGER_GPS는 거리 사전표시용(노출 정책 C-D5 확인 — 좌표 직접 노출 대신 "근무지 N m 이내" 표현 권장).
- **영향파일**: C1 컨트롤러/서비스/매퍼 + EntryContextResult dto.

### APP004-C4 — 입실/종료 화면 (M, 선행 C1·C2·C3)
- **유형**: frontend / `src/views/tbm/TbmEntryView.vue`(신규). 라우트 `/TbmEntry`(정적 등록, 보호).
- 골격은 본 플랜 §4(Vue 골격) + UI 명세 §3 참조. script는 `// TODO(developer):`.
- **백엔드 의존**: C3(컨텍스트) → C1(입실) → C2(종료).

### APP004-C5 — 메인 카드 액션 연결 (S, 선행 C4)
- **유형**: frontend / `src/views/main/components/TbmAttendCard.vue`(보완) + `MainView.vue` 핸들러.
- 기존 `@click:attend` emit → MainView가 `/TbmEntry?sessionCd=...` 진입(라우팅은 developer). 카드 자체 골격 변경 최소(developer가 핸들러 연결).

### APP004-C6 — 서명 연계 (S, 조건부, 선행 C0)
- C-D1에서 "서명 필수"로 확정될 때만. 서명 캡처(웹 canvas 또는 Flutter 네이티브) + 파일 업로드 → ENTRY/EXIT_SIGN_FILE_MGMT_CD. 미확정이면 화면에 서명 영역 placeholder만(TODO).

---

## 3. O 라인 상세 (§7.5 추가근무 강제)

### APP004-O1 — 마감 차단/액션필요 판정 연계 (M, prafta-028 선행)
- **유형**: backend.
- **정책**: §7.5(미승인 시 마감 차단) + §13.3(차단조건 "미승인 추가근무, 스케줄 없는 날 근무 포함"). prafta-028 tb_attd_close 선행조건에 사유 추가.
- **요구**:
  1. 스케줄 없는 날 출근 기록 존재 + 해당 일자 `tb_user_attd_req`(REQ_TYPE='03', REQ_STATUS≠'02'승인) 부재 → 마감 차단 사유.
  2. app-002 read의 dayType=ACTION_REQUIRED 판정에도 반영(앱 일자카드가 "추가근무 상신 필요" 노출).
- **영향파일(예상)**: 마감 차단조건 산출 서비스(prafta-028 산출물)에 룰 추가, app-002 day actions 산출 로직에 `canRequestOvertime` + needOvertimeSubmit 플래그.
- ⚠️ prafta-028 마감 차단 산출 위치는 웹 작업 산출물 — **실파일 미확인**(스키마갭 G2). 메인이 위치 확인 필요.

### APP004-O2 — 앱 초과근무 상신 엔드포인트 (S, 선행 O1)
- **계약 초안**: `POST /prafta/appApi/attd/overtime-request`
  - 요청: `{ workYmd, startTime, endTime, otType('EXTEND'|'NIGHT'|'HOLIDAY'), reason }`
  - 서버: REQ_TYPE='03', TARGET_ID=NULL(생성), REQ_STATUS='01'(신청), 결재라인(tb_user_attd_req_approval) 부여(prafta-020 프리셋/사용자정의 재사용). 사후 상신 기한 = **해당 일자 근태 마감 전까지**(§3.2, D+5 폐기).
  - 처리이력 prafta-027: HIST_TYPE='03'(생성요청), 그날 ATTD_ID 앵커.
- **재사용 검토**: 웹 attd07 OT 생성(`InsertUserOvertimeCommand` 등) 서비스 재사용 — **실파일 미확인**(G3). 앱 전용 래핑 vs 직접 호출은 developer 판단(메인 확인 권장).
- **OT_TYPE 판정**: O-D2 결정(자동추정 vs 사용자선택).
- **영향파일(예상)**: `app/attd/*` 신규 또는 기존 OT 서비스 재사용.

### APP004-O3 — 상신 유도 + 폼 진입 (S, 선행 O2)
- **유형**: frontend.
- **요구**: 스케줄 없는 날 출근/일자카드에서 "이 날은 추가근무 상신이 필요합니다" 콜아웃 + 초과근무 신청 진입. 기존 `AttendanceActionSheet.vue`의 overtime 액션 게이팅(`canRequestOvertime`) 재사용 + 상신 폼.
- **백엔드 의존**: O2.
- **결정**: 작업서 §2.4 권고 = (b) **사용자 유도**(자동생성 아님 — OT_TYPE/사유 사용자 입력 필요). O-D1 확정 필요.

---

## 4. 엔드포인트 계약 초안 (요약)

| 메서드 | 경로 | 용도 | 단위 |
|---|---|---|---|
| GET | `/appApi/tbm/entry-context` | 입실 분기 컨텍스트(상태/GPS/서명요구/기입실) | C3 |
| POST | `/appApi/tbm/enter` | 입실(비번/QR + GPS + 서명?) | C1 |
| POST | `/appApi/tbm/exit` | 종료(비번/QR + 서명? + 이수상태) | C2 |
| POST | `/appApi/attd/overtime-request` | 초과근무 상신(REQ_TYPE='03') | O2 |

> 공통: `/prafta/appApi` 자동 prefix, gv_* 세션 클레임 자동 주입(USER_CD/CMPNY_CD는 서버가 JWT에서 도출, 바디 신뢰 금지). 응답 필드 camelCase.

---

## 5. 사용자 확정 필요 — 설계결정 목록

### C (TBM 입실/종료)
- **C-D1 (서명)**: 입실/종료 서명 **필수 여부**와 파일 저장 경로. 정책 본문 출처 부재. (a)필수+파일관리 연계(C6 활성) (b)선택 (c)MVP 제외. → **정책 미확정, 확정 필요**.
- **C-D2 (일용직 범위)**: MVP는 **정규직(REGULAR)만**? 일용직(DAILY)은 별도(QR 관리자 스캔/일일계정 흐름). 권장: 정규직만 MVP, DAILY는 후속. → 확정.
- **C-D3 (입실 가능 세션상태)**: **OPENED만** vs **IN_PROGRESS까지**. STARTED_AT/ENDED_AT은 prafta-033-C(DEFERRED) 컬럼 → IN_PROGRESS 전이 미구현 가능. 권장: OPENED+IN_PROGRESS 입실 허용, COMPLETED/CANCELLED/DRAFT 차단. → 확정.
- **C-D4 (비번 실패 잠금)**: tb_tbm_pwd_fail은 로그만. 잠금 임계(예: N회/세션·시간) 적용? 정책 부재. → 확정(보안 라운드 연계).
- **C-D5 (GPS MANUAL/DISABLED 처리 + 좌표 노출)**: AUTO=거리검증. MANUAL=관리자 확인(GPS_MANUAL_CONFIRM_YN) 의미와 앱 동작, DISABLED=검증생략. 그리고 MANAGER_GPS 좌표를 화면에 직접 노출 vs "근무지 N m 이내"만. 권장: 좌표 비노출, 거리만. → 확정.

### §7.5 (추가근무 강제)
- **O-D1 (상신 트리거)**: (a)자동생성 vs (b)사용자 유도. 작업서·정책(§9.3 필수입력=시작/종료/사유) 근거로 **(b) 유도 권장**. → 확정.
- **O-D2 (OT_TYPE 판정)**: 자동추정(휴일=HOLIDAY/야간=NIGHT/그외=EXTEND) vs 사용자선택. 권장: 기본값 자동추정 + 사용자 수정 가능. → 확정.
- **O-D3 (마감 차단 반영 방식)**: prafta-028 차단조건 산출에 룰 추가(서버) + app-002 dayType=ACTION_REQUIRED 노출. 사후 상신 기한 = **해당 일자 근태 마감 전까지**(§3.2, D+5 폐기) 재확인. → 확정.

---

## 6. 스키마/정합 미확인 (⚠️ 메인 MCP·웹산출물 확인 필요)

- **G1 (DB 적용)**: tb_tbm_session/attendance/event/pwd_fail/session_state 운영 적용 + SYS046~055 등록 여부. (마이그 헤더상 "수동 적용", 적용됐는지 미확인) → MCP `SHOW TABLES LIKE 'tb_tbm_%'`, `SELECT SYST_VAL_CD FROM tb_syst_val_m WHERE SYST_VAL_CD IN ('SYS046','SYS055')`.
- **G2 (마감 차단 산출 위치)**: prafta-028 마감 차단조건 산출 서비스 실파일 위치 미확인. O1 영향범위 정밀화 필요.
- **G3 (웹 OT 생성 재사용)**: 웹 attd07 초과근무 생성 서비스(`InsertUserOvertimeCommand` 등) 실존/시그니처 미확인. O2 재사용 가부.
- **G4 (파일관리 테이블)**: 서명 파일 저장용 파일관리 테이블/업로드 엔드포인트(*_SIGN_FILE_MGMT_CD가 가리키는 테이블) 미확인. C-D1 "필수" 확정 시 선행.
- **G5 (prafta-033-C 상태전이)**: STARTED_AT/ENDED_AT 전이(IN_PROGRESS) 구현 여부 — DEFERRED 표기. C-D3 입실 가능 구간에 직접 영향.
- **G6 (TBM 입실 정책 본문)**: TBM 입실/서명/잠금의 정책서 본문 출처 부재(prafta-033 기획 기반). 비즈니스 룰 추측 금지 → 미확정은 결정목록으로 위임.

---

## 7. 권장 착수 순서

1. **APP004-C0(조사)** — C-D1~D5, G1·G4·G5 해소(메인 MCP + 사용자 확정).
2. **APP004-C3 → C1 → C2** (backend: 컨텍스트 → 입실 → 종료) — 정규직 MVP.
3. **APP004-C4 → C5** (frontend: 입실/종료 화면 → 메인 카드 연결).
4. (C6은 C-D1=필수 확정 시에만.)
5. **APP004-O1 → O2 → O3** (§7.5 — prafta-028·웹 OT 재사용 확인 후. C와 독립이라 병렬 가능, C 우선).

> C/§7.5는 도메인이 달라 독립 착수 가능. 사용자 가치·작업서 권고상 **C 우선**.
