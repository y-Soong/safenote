# prafta-app-018-B — [앱 BE] 연차 신청 쓰기 + 결재선 INSERT

상위: `prafta-app-018-leave-apply-plan.md`. 018-A(조회) 선행 권장. 본 단위가 **핵심 트랜잭션**(가장 큼) — 웹 `LeaveFlowServiceImpl.submitLeave` 를 앱으로 미러 + D1/D2 반영.

## 목표
`POST /appApi/leaveflow/apply` 신규. 단일 `@Transactional(rollbackFor=Exception.class)` 안에서 신청 처리. 식별값 JWT만(IDOR). 신규 테이블 없음.

## 요청 본문(camelCase, JSON)
- `leaveCd`(필수), `workYmd`(필수, YYYYMMDD), `useUnitType`(필수, SYS025), `startTime`/`endTime`(시간차 단위일 때 필수, HHMM), `reason`(사유), `approverUserCds`(결재 필요 시 결재자 순서 배열; 또는 `presetId`로 프리셋 선택 — planner가 둘 중/병행 결정).
- ⚠️ 식별값(cmpny/site/user/node)·WORK_SEQ 등은 본문 비신뢰. Param.from 에서 TokenInfo gv_* 강제.

## 처리 순서 (web submitLeave 미러 + 변경점)
1. 타입 메타 조회(`selectLeaveTypeInfo`) — 없으면 ATTD_404_030. 결재필요 판정(법정=정책 APRV_USE_YN, 비법정=타입 APRV_USE_YN).
2. **단위 게이팅(신규, D2)** — 제출 `useUnitType` 이 해당 종류 허용단위(법정=USAGE_UNIT, 비법정=USE_UNIT_TYPE; D2-a 의미 적용)에 **포함되지 않으면 거부**(신규 에러코드, 예 `ATTD_400_0xx` — AttdErrorCode 미사용 확인 후 배정). 웹에 없는 신규 검증.
3. 단위 구조검증 + 차감계산(web 99~135 동일): 종일=1.0, 반차=0.5(+leaveMinutes=daily/2), 시간차=시각필수·`(e-s)%unitMin==0`·일한도·휴게 가로지름 거부·calcDeductionDays. 위반 시 052/054/055.
4. 사후 신청 마감(web 137~146): workYmd<today면 소속부서 마감(attdClose) 확인 — 마감이면 050. (앱 attdClose 의존 — planner가 앱에서의 호출 경로 확정.)
5. 잔여 부여 선택(`selectDeductibleGrant`, 만료 임박 우선) — 없으면 051.
6. 요청 INSERT `TB_USER_ATTD_REQ`(REQ_TYPE='05', REQ_STATUS = 결재필요?'01':'02').
7. **결재선(D1, app-009 연차범위)**: 결재 필요 시 `approverUserCds`(또는 preset 전개)로 `TB_USER_ATTD_REQ_APPROVAL` 단계 INSERT. 자기승인 원칙(§9.5, web 161~227 미러): 본인이 결재자인 단계는 소속노드 SELF_ATTD_APPRV_YN ON + 본인이 그 노드 담당일 때만 자동승인('02'), 자격 미달이면 본인 지정 불가(056). 첫 수동 단계만 '01', 나머지 '00'. 전 단계 자동승인이면 즉시 확정.
8. 연차 사용기록 `TB_USER_LEAVE_USE` INSERT(LEAVE_STATUS='CONFIRMED', REQ_ID 연결, GRANT_ID, 단위/일수/분) + `TB_USER_LEAVE_GRANT` USED_DAYS 재계산(recomputeGrantUsedDays) — web 동일.

## 수용 기준
- 웹 submitLeave 와 동등한 결과(요청/사용기록/부여 USED_DAYS/결재선 상태)를 앱 경로로 재현. 단 D2 게이팅은 앱이 추가(웹보다 엄격).
- 모든 게이트 fail-closed(예외 시 INSERT 전체 롤백). 식별값 JWT 출처. `#{}` 바인딩·`SELECT *` 금지.
- 결재선/자기승인 분기 정확(본인지정 불가 056, 자동승인, 즉시확정). 타입혼동 방지(REQ_TYPE='05' 고정).
- 에러코드 050/051/052/054/055/056/404_030 재사용 + 단위게이팅 신규코드 1개.
- MyBatis record 위치매핑 주의. 빌드 통과.

## 정책 출처
attd §8/§8.5(사용단위·휴게·잔여), §9/§9.5(결재·자기승인), prafta-019(시간차/결재라인), prafta-028(부서마감), prafta-031(향후 알림 outbox는 본 작업 미포함).
