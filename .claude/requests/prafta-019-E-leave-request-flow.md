# prafta-019-E · 연차 신청 · 결재 흐름 본체

> **목적**: 사용자 연차 신청(사전/사후, 시간차 포함) → 결재 여부 판단 → (Y면) 결재라인 진행 / (N이면) 즉시 확정 → 차감·출근차단.
> **의존**: A(사용단위·차감), C(근태마감), D(결재라인). **마지막에 착수.** 참조: `prafta-019-plan.md`

---

## 1. 결재 여부 판단 (결정 #1·#2)

- **회사정의 연차**: `tb_leave_type_mgmt.APRV_USE_YN` (기존)
- **법정연차**: `tb_leave_policy`에 결재 여부 Y/N 컬럼 **신규 추가**(DDL) — 현재 없음.

```sql
ALTER TABLE tb_leave_policy
  ADD COLUMN APRV_USE_YN char(1) NOT NULL DEFAULT 'N' COMMENT '법정연차 신청 결재 여부' AFTER AXIS7_USE_PROMOTION;
```

- **Y** → 작업 D의 결재라인을 태운다(사용자가 신청 시 라인 구성). **사전·사후 동일**.
- **N** → 결재 없이 즉시 확정.
- 자기결재 자동승인: 라인의 해당 결재자가 신청자 본인뿐인 경우 등은 §9.5(자기 승인 원칙) 정합으로 처리.

## 2. 사전 / 사후 + 사후 기한 (결정 #4)

- 사전/사후를 결재 여부의 기준으로 삼지 않는다(결재 여부는 §1).
- **사후 신청(시작 시각/일자 경과)** 은 **근태 마감 전까지만 허용**(작업 C의 근태 마감 참조). 마감된 기간은 신청 차단(예외는 근태 보정 — §8.4).

## 3. 시간차 신청 검증 (작업 A 유틸 사용)

순서:
1. 해당 일자 `tb_user_work_plan` 존재(없으면 거부). FULL(1일)은 스케줄 무관 가능.
2. 1일 소정근로분 계산(A의 `getDailyStdWorkMinutes`).
3. 허용 단위 검증 — 회사정의: 타입 `USE_UNIT_TYPE` / 법정: `tb_leave_usage_policy.ALLOW_*`.
4. 신청 시각 + 길이가 스케줄 범위 내.
5. **휴게 가로지름 거부**(§8.5.9) — §4의 휴게 시각 컬럼 사용.
6. 신청분 ≤ 1일 소정근로분.
7. 같은 날 겹침 + `MAX_DAILY_REQUEST` 건수 검증.
8. 차감 = 신청분 ÷ 1일 소정근로분 (`decimal(8,5)`). 반차는 0.5 고정.
9. 잔여 검증(부족 시 더 작은 허용 단위 안내, 없으면 부족 안내).

## 4. 휴게 시각 컬럼 추가 (선행 DDL + 스케줄 화면 파급)

현재 `tb_sch_mgmt`는 휴게를 "분"(`FST_SCH_BRK_MIN`)으로만 저장 → 휴게 가로지름 판정 불가. 시작/종료 시각 신규 추가.

```sql
ALTER TABLE tb_sch_mgmt
  ADD COLUMN FST_BRK_STR_TIME varchar(4) NULL COMMENT '1구간 휴게 시작(HHMM)' AFTER FST_SCH_BRK_MIN,
  ADD COLUMN FST_BRK_END_TIME varchar(4) NULL COMMENT '1구간 휴게 종료(HHMM)' AFTER FST_BRK_STR_TIME,
  ADD COLUMN SEC_BRK_STR_TIME varchar(4) NULL COMMENT '2구간 휴게 시작(HHMM)' AFTER SEC_SCH_BRK_MIN,
  ADD COLUMN SEC_BRK_END_TIME varchar(4) NULL COMMENT '2구간 휴게 종료(HHMM)' AFTER SEC_BRK_STR_TIME;
-- tb_sch_mgmt_hist 에도 동일 4컬럼 추가
```

- **수반**: 스케줄관리 화면(`Attd_01.vue` / 교대 `Attd_06.vue`)에 휴게 시작/종료 입력 추가, 기존 스케줄 NULL 처리(휴게 시각 없으면 가로지름 검증 skip 또는 분 기준 보수 처리).

## 5. 반차 처리

- 1구간만 / 1+2구간 / 교대 분기 처리(정책 §8 참조). `LEAVE_DAYS=0.5` 고정, `LEAVE_MINUTES`=1일 소정근로분/2.

## 6. 차감 · 적용 · 출근 차단

- `tb_user_leave_use` 저장: `USE_UNIT_TYPE`(SYS025), `LEAVE_DAYS`(decimal 8,5), `LEAVE_MINUTES`, `START/END_TIME`, `REQ_ID`, `GRANT_ID`, `LEAVE_STATUS=CONFIRMED`.
- `tb_user_leave_grant.USED_DAYS` 동기화.
- 승인 시 출근 차단 — 일 단위: 출근 자체 차단 / 시간 단위: 해당 구간 차단(§8.3).
- `tb_user_attd_req.LEAVE_DAYS`가 `decimal(3,1)`이라 시간차 차감을 못 담음 → **정밀도 상향 또는 분(LEAVE_MINUTES) 기반으로 처리**(planner 확정).

```sql
ALTER TABLE tb_user_attd_req MODIFY COLUMN LEAVE_DAYS decimal(8,5) NULL COMMENT '사용 일수(시간차 환산)';
```

## 7. 결재 진행 (작업 D 라인 사용)

- 신청 시 라인 일괄 생성. Step1=신청(01), 이후=대기(00).
- 승인 → 다음 Step 신청(01) 전환 + 알림. 마지막 승인 → `tb_user_attd_req.REQ_STATUS=02 승인` + 차감/적용.
- 반려 → `REQ_STATUS=03 반려`, 후속 Step 대기 그대로 종결.

## 8. 영향 파일

- 테이블: `tb_leave_policy`(APRV_USE_YN), `tb_sch_mgmt`(+hist 휴게시각), `tb_user_attd_req`(LEAVE_DAYS), `tb_user_leave_use`, `tb_user_leave_grant`, `tb_user_attd_req_approval`(D)
- 백엔드: `web/attd/attd05`(근무계획/신청), `web/attd/attd07`(요청·승인 — 기존 반려 로직 연계), `web/attd/attd09`(잔여/대시보드)
- 프론트: `Attd_05.vue`, `Attd_07.vue`, `Attd_09.vue`, (스케줄 휴게입력) `Attd_01.vue`/`Attd_06.vue`

## 9. 정책 출처

- 근태 §8(휴가), §9(요청·승인), §10(근태 계산)
- 요청승인관리 재기획서 §6.4(연차 상신), §9(데이터 구조)

## 10. 주의

- 사후 정정(승인 완료 후 잘못된 결재)은 HR 담당자가 연차 관리 메뉴에서 직접 처리(`LEAVE_STATUS=CANCELLED`) — **별도 후속 작업**(본 범위 외).
- 미래 일자 스케줄 변경 시 영향받는 시간차 재계산/동의 후 삭제 — 후속 작업으로 분리 가능.

### 10.1 출근 차단 구현 한계 (prafta-019-E 후속, 2026-05-23)

- **일 단위(00) 연차 확정 시** `tb_user_work_plan.WORK_PLAN_CD`를 `LEAVE_CD`로 덮어 출근을 차단한다(즉시확정/최종승인). 반려 시 해당 연차 블록 행을 삭제한다.
  - **한계**: 덮어쓰기 전 원 근무계획(`SCH_CD`)을 보관하지 않으므로, 명시적 근무계획이 있던 일자에 일 단위 연차를 신청→반려하면 그 일자의 근무계획 행이 사라진다(기본 스케줄로 회귀). 대부분의 일자는 명시 행 없이 기본 스케줄을 쓰므로 영향이 없으나, **원 스케줄 복원이 필요하면 별도 후속 작업**(덮기 전 SCH_CD 보관 후 반려 시 복원)으로 처리한다.
- **반차/시간차 출근 차단**은 `tb_user_leave_use`(CONFIRMED) 기록 기반으로 **출퇴근(check-in) 단계에서 해당 구간을 차단**해야 하며, 이는 check-in 로직 연계가 필요한 **후속 작업**이다(본 후속 범위에서는 일 단위 work_plan 차단만 구현).
