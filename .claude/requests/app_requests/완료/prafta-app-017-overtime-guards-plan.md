# prafta-app-017 — 초과근무(OT) 등록 가드 + 웹 OT카드 BEFORE/AFTER 정정

작성: 메인 세션(사용자 확인 결정 반영). 영역: **모바일 앱(FE/BE) + 웹(FE)** 혼합 — 이슈별 영역 태깅함.
구동 맥락: 사용자가 초과근무 신청을 테스트하던 중 발견한 3개 결함. 정책 출처: 근태관리 정책서 §10.3(추가근무 인정 — 정규 구간 밖 초과분), §10.1(정규 근무 계산).

관련 코드(조사 완료):
- 앱 OT 신청 BE: `prafta-backend/.../com/prafta/app/req/req07/service/impl/AppReq07ServiceImpl.java` (`registerOvertime`)
- 앱 OT 신청 BE 매퍼: `com/prafta/app/req/req07/mapper/AppReq07Mapper.java` (+ XML)
- 앱 OT 신청 FE: `prafta-app-frontend/.../src/views/req/components/OvertimeForm.vue`
- 웹 일자상세 팝업 FE: `prafta-web-frontend/.../src/views/attd/popup/AttdDayDetailPop.vue` (`reqCards` computed, 약 1617~1625행)
- REQ 코드: `web/attd/attd07/util/AttdReqTypeUtils.java` — REQ_TYPE 01 근태생성/02 근태수정/03 OT생성/04 OT수정/05·06 연차/10 스케줄수정. REQ_STATUS 01 신청(=미처리)/02 승인/03 반려/04 취소.

---

## 이슈 ① OT 시각이 정규 스케줄 구간과 겹치면 거부 (앱 FE + BE 이중)

**현상:** 1구간 스케줄 00:00~07:00, 실근무 전일 23:57~당일 07:12 인데 OT를 06:58~07:12로 등록 가능. 06:58~07:00는 정규구간 내부.

**규칙(확정):** OT 시각은 해당 구간의 **정규 스케줄 구간과 한 순간도 겹치면 안 된다.** 겹치면 **요청 전체 거부**(잘라서 허용하지 않음). 유효 영역 = "등록 가능 시간"과 동일 개념: 앞 OT(실출근\~스케줄시작) / 뒤 OT(스케줄종료\~실퇴근). 즉 OT [start,end] ∩ 정규스케줄[schStart,schEnd] = ∅ 이어야 한다.

**자정/야간:** 반드시 **(일자+시각) 타임스탬프**로 판정. 스케줄 구간은 근무일(WORK_YMD) 기준, 종료<시작이면 종료를 익일로 보정(웹 Attd_11 / 앱 attd01 isEarlyStamp와 동일 규약). OT 시각도 startDate/startTime~endDate/endTime 인스턴트로 비교. (시:분만 비교 금지 — 본 프로젝트 반복 결함.)

**구간 매핑:** OT slot.workSeq=1 → 1구간 스케줄(fstStart/fstEnd)과 비교, workSeq=2 → 2구간(secStart/secEnd)과 비교. 스케줄 없는 구간(스케줄 없는 날/추가출근)은 정규구간이 없으므로 겹침검사 면제(전량 OT 허용 — 기존 정책 유지).

**구현:**
- **BE(필수):** `registerOvertime`에 겹침 검증 추가 → 위반 시 신규 에러코드(예: `ATTD_400_098` 또는 적절한 미사용 코드, AttdErrorCode에서 확인 후 배정)로 거부. 스케줄 로딩 쿼리 신규 필요(req07 매퍼에 TB_USER_WORK_PLAN+TB_SCH_MGMT 조인으로 해당 WORK_YMD 스케줄 1건 조회 — 식별값은 Param의 JWT 출처만). attd01 모듈 직접 의존은 피하고 req07 내부에 자체 쿼리/헬퍼로 둔다.
- **FE(필수):** OvertimeForm에서 제출 전 동일 규칙으로 사전 차단 — 겹치면 제출 비활성 + 안내 문구("스케줄 시간 내에는 초과근무를 등록할 수 없어요"). 이미 있는 `slotWindowText`(등록 가능 시간, 일자+시각 비교로 직전 수정됨)와 정합되게. 사전차단은 UX용이고 **서버 검증이 최종 권위**(이중 검증 필수).

---

## 이슈 ② 미처리 수정요청이 있으면 OT 등록 거부 (앱 FE + BE 이중)

**규칙(확정, 미처리=REQ_STATUS '01' 신청만 대상. 반려/취소/승인은 무관):**
- **근태보정 요청(REQ_TYPE 01 또는 02)** 이 해당 (USER, WORK_YMD, **WORK_SEQ**) 로 미처리 존재 → 그 **구간**의 OT 거부.
- **스케줄수정 요청(REQ_TYPE 10)** 이 해당 (USER, WORK_YMD) 로 미처리 존재 → **그날 모든 구간**(1·2 무관)의 OT 거부.

**구현:**
- **BE(필수):** `registerOvertime` 에서 slots 처리 전/중 검증.
  - 신규 매퍼: (a) `countPendingAttdCorrectionBySlot(user,workYmd,workSeq)` = REQ_TYPE IN('01','02') AND REQ_STATUS='01' 카운트. (b) `countPendingSchedModify(user,workYmd)` = REQ_TYPE='10' AND REQ_STATUS='01' 카운트. (기존 `countDuplicateReq`는 workSeq 미필터라 그대로 두고 신규 추가.)
  - 스케줄수정 미처리>0 → 전체 거부. 각 slot에 대해 근태보정 미처리>0 → 거부. 위반 시 신규 에러코드(예 `ATTD_400_099` 등 미사용 확인 후 배정) + 명확한 한국어 메시지.
- **FE(권장):** OT 폼 진입 컨텍스트에 "해당 일자/구간 미처리 요청" 정보가 있으면 사전 안내·비활성. 컨텍스트에 없으면 서버 거부 메시지로 처리(최소 서버측 보장은 필수). 컨텍스트 확장이 큰 경우 FE는 서버 에러 표면화만 하고 follow-up 으로 분리 가능.

---

## 이슈 ③ 웹 OT "생성" 요청 카드 BEFORE에 정규근무 값 노출 (웹 FE)

**현상:** 처음 올리는 OT 생성요청인데 일자상세 팝업 OT카드 BEFORE에 정규 근무 출퇴근 시각이 들어감.

**원인:** `AttdDayDetailPop.vue` `reqCards`(약 1617~1625행)가 01~06을 한 분기로 묶어 `befIn/befOut = act{n}InTime/OutTime`(정규 실근태), `aftIn/aftOut = req.startTime/endTime` 로 매핑. OT 생성(03, TARGET_ID=null)은 "변경 전"이 없어야 함. (스케줄수정 10 분기는 "없음" 처리를 이미 올바로 함.)

**규칙(확정):** OT **생성** 요청(REQ_TYPE 03, 또는 TARGET_ID 없음)은 BEFORE를 정규근태에서 끌어오지 말고 공란/"없음" 표기. OT **수정**(04, TARGET_ID 존재)은 기존 OT 값이 BEFORE가 되어야 하나, 본 작업 범위는 우선 **생성 케이스의 오표시 제거**가 핵심. 04의 BEFORE 정밀화는 데이터 출처(기존 OT 행) 확인 후 동일 카드에서 처리하되, 불확실하면 03만 정정하고 04는 현행 유지 + follow-up 주석.

**구현(웹 FE only, 데이터는 이미 응답에 존재 — targetId/reqType):** reqCards에서 OT(03/04) 또는 생성(targetId null)을 분기해 BEFORE를 정규근태와 분리. 다른 타입(근태보정 01/02, 스케줄수정 10) 표시 회귀 없도록 주의.

---

## 검증 관점(QA/보안 공통)
- ① 경계값: OT가 스케줄과 1분 겹침/정확히 접함(07:00~07:12: schEnd=07:00에 접함=겹침 아님, 허용) / 자정 넘김 2구간 / 스케줄 없는 날(면제). FE 사전차단과 BE 거부 결과 일치.
- ② 미처리만 차단(반려·취소·승인 건은 OT 허용). 01/02는 구간 단위, 10은 전일. IDOR: 모든 식별값 JWT 출처(본문 신뢰 금지).
- ③ OT 생성 카드 BEFORE 공란, 타 요청타입/스케줄수정 카드 회귀 없음.
- 시각 비교는 전부 (일자+시각) 기준인지(시:분 단독 비교 잔존 금지).
- 마이그레이션: 신규 에러코드는 코드상수만(메시지 프로퍼티 등 프로젝트 관례 따름). DB 스키마 변경 불필요(기존 컬럼만 사용). 스케줄수정 REQ_TYPE='10'은 마이그 prafta-app-007-attd-req-extensions.sql 적용 전제(기존).
