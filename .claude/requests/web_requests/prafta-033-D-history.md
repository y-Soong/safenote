# prafta-033-D — TBM 이력 관리 (W-12~15)

> 마스터 플랜: `prafta-033-plan.md`
> 선행: `prafta-033-A` (출결/이벤트 테이블 생성됨), `prafta-033-B` (세션 데이터 존재)
> To-Be 사양: `ref/prafta-033/05_04_HISTORY.md`, `03_BACKEND_SPEC_WEB.md §7`

---

## 0. 범위 / 데이터 의존 경계

| 포함 (D) | 비고 |
|---|---|
| W-12 TBM 이력 목록 (COMPLETED/CANCELLED 위주) | 세션 데이터(B)에서 채워짐 |
| W-13 출결 상세 + 이벤트 로그 토글 | **출결 데이터는 C·앱이 채움** |
| W-14 미이수 처리(이수/미이수 사후 변경) | 출결 row 존재 전제 |
| W-15 사용자별 TBM 이수 이력 (정규직/일용직) | 출결 데이터 전제 |

> ⚠️ **데이터 의존**: 출결(`TB_TBM_ATTENDANCE`)·이벤트(`TB_TBM_ATTENDANCE_EVENT`)는 **C(실시간)·모바일 앱이 생성**한다. D 착수 시점엔 데이터가 비어 있을 수 있다 → **화면/API는 모두 구현**하되, 빈 목록·테스트 데이터로 검증한다. 데이터 채워짐은 C/앱 이후.
> W-14(미이수 처리)는 출결 row를 UPDATE하지만 **사후 보정**이라 실시간 흐름과 무관 → D에 포함(단 row 존재해야 동작).

신규 모듈: 백엔드 `com.prafta.web.tbm.tbm04`, 프론트 `src/views/tbm/Tbm_04.vue`(+팝업).

---

## 1. 엔드포인트 (`/webApi/tbm04/*`)

| 화면 | 메서드/경로 | 용도 | To-Be |
|---|---|---|---|
| W-12 | `GET /webApi/tbm04/history-sessions` | 이력 목록(종료 위주 + 통계) | §7.1 |
| W-13 | `GET /webApi/tbm04/session-attendances` | 세션 출결 명단(+이벤트 요약) | §7.2 |
| W-13 | `GET /webApi/tbm04/attendance-events` | 출결 단건 이벤트 타임라인 | §7.3 |
| W-14 | `POST /webApi/tbm04/update-completion` | 이수/미이수 사후 변경 | §7.4 |
| W-15 | `GET /webApi/tbm04/user-attendances` | 정규직 사용자별 이수 이력 | §7.5 |
| W-15 | `GET /webApi/tbm04/daily-user-attendances` | 일용직 사용자별 이수 이력 | §7.5 |
| 선택 | `GET /webApi/tbm04/history-sessions/export` | Excel/PDF 내보내기 | §7.1 (후속) |

---

## 2. W-12: 이력 목록

- 파라미터: `siteCd`, `startDate`, `endDate`, `managerUserCd`, `searchKeyword`, `page`, `pageSize`. 기본 대상 `STATUS_CD IN ('COMPLETED','CANCELLED')`.
- 컬럼: 제목, 사업장명, 종료일(ENDED_AT), `참여/이수`(attendanceCount/completedCount + 이수율%), 미이수수(빨강 강조), 위험성평가 연계수(0건 ⚠️).
- 통계 영역(권장): 기간 합계 — TBM 횟수, 참여 인원, 평균 이수율.
- 행 클릭 → W-13.

DTO: `HistorySessionListRequest/Param/Query/Result/Response`. 집계는 출결 테이블 LEFT JOIN/서브쿼리(빈 데이터 시 0).

---

## 3. W-13: 출결 상세

### 3.1 구성
- 세션 정보 요약: 사업장/개설자/시작·종료 시각/교육내용 요약(100자+전체보기)/위험성평가 연계.
- 참여자 그리드: 이름, 타입(정규직/일용직 배지), 입실, 종료(빈칸=미종료), 이상신호 요약, 이수(✓/✕), 액션(📋 이벤트, ✏️ W-14).
- 행 색상: 정상 이수=기본 / 이상신호=노랑 / 미이수=빨강 / 강제종료=회색.

### 3.2 출결 명단 (`GET /session-attendances`)
- 파라미터: `sessionCd`(필수), `userTypeCd`(REGULAR/DAILY), `completionStatusCd`, `includeEventSummary`(default true).
- 정규직: `TB_TBM_ATTENDANCE` JOIN `TB_USER`(USER_NM/소속). 일용직: JOIN `TB_DAILY_USER`(MBL_NO_LAST4 등). **USER_TYPE_CD로 분기 조인** — 평문 휴대폰 금지, `MBL_NO_LAST4`만.
- 이상신호 요약(`eventSummary`)은 **백엔드가 `TB_TBM_ATTENDANCE_EVENT` 집계**(backgroundCount/durationSec, gpsOutOfRangeCount, networkLostCount, anomalyLevel). 클라이언트 계산 금지.

### 3.3 이벤트 타임라인 (`GET /attendance-events`)
- 파라미터: `attendanceCd`(필수). 전체 이벤트 시간순.
- 응답: eventNo, eventTypeCd, eventTime, serverReceivedAt, eventData(JSON).
- 이벤트 다수 가능(plan §8: 페이징/무한스크롤 검토 — 1세션 수백건 가능).

### 3.4 서명 미리보기
- 입실/종료 서명(`*_SIGN_FILE_MGMT_CD`) → 파일서비스로 라이트박스. 강제종료(서명없음)는 "서명 없음(관리자 강제 종료)" 표기.

---

## 4. W-14: 미이수 처리 (사후 변경)

### 4.1 동작 (`POST /update-completion`)
- 파라미터: `attendanceCd`, `completionStatusCd`(COMPLETED/NOT_COMPLETED), `reason`(필수, ≥10자).
- 권한: 개설자 본인, safe, master.
- UPDATE: `COMPLETION_STATUS_CD`, `NOT_COMPLETED_REASON`(NOT_COMPLETED 시), `STATUS_UPDATED_BY`, `STATUS_UPDATED_AT`.
- COMPLETED 복귀 시 `NOT_COMPLETED_REASON`은 보존(감사용 — 덮어쓰지 않거나 이력 남김).
- ⚠️ **다중 변경 이력**: 현재 스키마는 마지막 변경만 저장(STATUS_UPDATED_*). 다중 이력 표시는 Phase 2 `TB_TBM_ATTENDANCE_HISTORY` 별도(MVP 제외, plan/사양 05_04 §3.5). MVP는 마지막 1회만.

### 4.2 화면
- 모달: 현재 상태 → 변경 상태 라디오 + 사유 입력 + "관리자 책임 기록" 안내. 처리 후 W-13 그리드 즉시 갱신.

---

## 5. W-15: 사용자별 TBM 이수 이력

### 5.1 조회
- 정규직: `GET /user-attendances?userCd=&startDate=&endDate=&completionStatusCd=&page=&pageSize=`
- 일용직: `GET /daily-user-attendances?userCd=...` (일용직도 USER_CD 식별 — plan §3.1)
- 응답: user(이름/소속) + attendances(세션 제목/일자/입실·종료/이수/위험성평가수) + summary(총참여/이수/미이수/이수율/평균참여시간).
- 통계: 이수 = COMPLETION_STATUS_CD='COMPLETED', 이수율 = 이수/총참여.

### 5.2 권한/진입
- master/safe = 전체. 사업장 관리자 = 자기 사업장 사용자만. 진입: 인사 모듈 탭 / W-13 이름 클릭 / 직접 URL.
- 본인 조회(앱)는 본 화면 범위 아님(웹 관리자용).

---

## 6. Excel/PDF 내보내기 (선택, 후속)

- plan §8-5: 백엔드(POI/iText) vs 프론트(SheetJS) — 기존 PRAFTA 라이브러리 확인 후 결정.
- W-12 Excel: 세션 요약 시트 + 세션별 출결 시트. PDF: A4 보고서(감독관 점검 대비).
- 1차 구현에서는 **목록/상세까지 우선**, 내보내기는 라이브러리 확인 후 별도 작업 항목으로 분리 가능.

---

## 7. Vue 컴포넌트 (권장, JS)

```
src/views/tbm/
├── Tbm_04.vue                      # W-12 이력 목록 (메인)
└── popup/
    ├── TbmAttendanceDetail.vue     # W-13 출결 상세
    ├── TbmEventTimeline.vue        # 이벤트 타임라인(펼침/모달)
    ├── TbmCompletionModal.vue      # W-14 미이수 처리
    └── TbmUserAttendance.vue       # W-15 사용자별 이수
```
- 인쇄용 레이아웃(@media print), 필터/정렬, 색상 CSS 변수·scoped·공통 컴포넌트 우선.

---

## 8. 작업 항목 분해 (developer)

1. **D-BE-1**: tbm04 골격 + 이력 목록(집계/통계)
2. **D-BE-2**: 출결 명단(유형별 조인, 이벤트 요약 집계, PII 마스킹)
3. **D-BE-3**: 이벤트 타임라인 조회(페이징)
4. **D-BE-4**: 미이수 처리 UPDATE(권한/사유 검증)
5. **D-BE-5**: 사용자별 이수(정규직/일용직)
6. **D-FE-1**: Tbm_04 이력 목록 + 통계
7. **D-FE-2**: 출결 상세 + 이벤트 타임라인 + 서명 미리보기
8. **D-FE-3**: 미이수 처리 모달
9. **D-FE-4**: 사용자별 이수 화면
10. **D-OPT**: Excel/PDF 내보내기(라이브러리 확인 후 분리)

---

## 9. 검증 기준

- [ ] 출결 데이터 0건이어도 화면/통계 정상(빈 상태)
- [ ] 일용직 휴대폰 평문 노출 없음(MBL_NO_LAST4만)
- [ ] 이상신호 요약은 백엔드 집계값 사용(클라 계산 금지)
- [ ] 미이수 처리: 사유 ≥10자, 권한(개설자/safe/master), STATUS_UPDATED_* 기록
- [ ] 사업장 관리자 타 사업장 이력/사용자 조회 차단
- [ ] 모든 쿼리 CMPNY_CD 스코프
- [ ] 이력은 COMPLETED/CANCELLED 위주(필터)

---

**연관**: 실시간 진행(C)이 출결 데이터를 생성한다 → `prafta-033-C-live-session-DEFERRED.md`(앱 이후 활성화).
