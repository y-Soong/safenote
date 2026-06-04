# prafta-app-018-F — [웹 BE+FE] 일자상세 팝업에 확정 연차(시간차) 표시

상위: `prafta-app-018-leave-apply-plan.md`. 연차 테스트 중 발견(이슈 3). 웹 영역. D(요청 카드 표시)의 후속 — D는 "미처리 요청 카드"만 고쳤고, **자동확정(결재 불필요) 연차는 요청으로 안 떠서 팝업에 아무것도 안 보임**.

## 배경/문제
근무관리(Attd_07) 셀 더블클릭 → `AttdDayDetailPop.vue`. 결재 불필요 연차(예 법정 월차, 정책 APRV_USE_YN='N')는 신청 즉시 REQ_STATUS='02' 자동확정 → 결재선 0건. `selectMonthlyAttdReq`는 "미처리(01) + 결재자 EXISTS" 요청만 내려주므로 자동확정 연차는 카드로 안 뜨고, 팝업에 **확정 연차 사용 표시 자체가 없다**. 관리자가 그날 연차(특히 시간차)를 전혀 못 봄.

## 목표
일자상세 팝업에 그날 **확정 연차 사용 내역**(TB_USER_LEAVE_USE, LEAVE_STATUS='CONFIRMED')을 표시. 시간차 상세 포함 — 예: `월차 · 시간차 1시간 · 03:00~04:30 · 0.19일 차감`(D 카드 포맷과 동일 톤). 결재 유무·요청 상태와 무관하게 확정된 연차는 보이게.

## 현황 (조사 완료/추정 — planner 재확인)
- BE `daily-attd-details`(Attd07) `DailyAttdDetailsResult`: 스케줄+근태만, **연차 사용 필드 없음**. reqCards는 `selectMonthlyAttdReq`(미처리 요청 only).
- 확정 연차는 TB_USER_LEAVE_USE 에 USE_UNIT_TYPE/START_TIME/END_TIME/LEAVE_MINUTES/LEAVE_DAYS/LEAVE_CD 로 존재. 단위 라벨 FNC SYS025(D에서 사용한 `FNC_CMM_INFO_SRCH(...,'SYS025')`).

## 구현
### BE (web attd07)
- 그날 확정 연차 사용 조회 신규 매퍼: `selectDailyConfirmedLeave(cmpny,site,user,workYmd)` — TB_USER_LEAVE_USE WHERE CONFIRMED·DEL_YN='N'·START_DATE≤workYmd≤END_DATE(또는 해당일 매칭), JOIN TB_LEAVE_TYPE_MGMT(LEAVE_NM)·SYS025 라벨. 컬럼: leaveCd/leaveNm/useUnitType/unitNm/startTime/endTime/leaveDays. 식별값 JWT/스코프 가드(기존 daily-details와 동일 권한). `daily-attd-details` 응답(DailyAttdDetailsResponse)에 `confirmedLeaves[]` 추가 또는 별도. SELECT *금지·#{}·leading콤마. record 위치매핑.
  - ⚠️ 권한: daily-details는 결재자/관리자 스코프 가드가 있는지 확인 후 동일 적용(cross-site IDOR 금지). 본인 외 타인 연차 노출은 그 관리자 권한 범위 내에서만.
### FE (`AttdDayDetailPop.vue`)
- 확정 연차 섹션/카드 신규 표시(요청 카드 영역과 구분, "연차 사용" 헤더 등). `confirmedLeaves[]` 를 `월차 · {단위라벨} · (시간차면 시각) · {정규화 일수}일 차감` 으로. D의 normalizeDays/연차카드 스타일 재사용. 요청 카드(미처리)와 중복 표시 안 되게(같은 연차가 요청+확정 동시 노출 방지 — 자동확정은 요청 없음, 결재형은 요청만 또는 상태표기).

## 수용 기준/엣지
- 자동확정 시간차 연차가 팝업에 단위/시각/일수로 표시.
- 종일/반차/시간차 3종, 06(연차수정) 고려. 결재형 연차(요청 카드로 뜨는 건)와 이중표시 없음.
- 기존 카드(근태보정/OT/스케줄수정/연차요청)·승인반려·월마감 회귀 없음.
- 권한/스코프 가드 유지(타 사용자 연차 무단노출 없음). 빌드/SFC.

## 정책 출처
attd §8(연차)·§9(근무관리 표시), prafta-019(시간차). D `prafta-app-018-D-tasks.md` 포맷/조인 참조.
