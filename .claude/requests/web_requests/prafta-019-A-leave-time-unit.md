# prafta-019-A · 시간차 연차 사용단위 도입 (prafta-017 사용단위 로직 보완)

> **성격**: prafta-017의 불완전했던 사용단위 로직 보완. "하루=8시간 고정" 전제로 만든 고정분수(1/0.5/0.25/0.125)를
> **절대 시간 단위 + 그날 스케줄 기준 동적 환산**으로 교체한다.
> **의존**: 독립 (가장 먼저 착수 가능). 참조: `prafta-019-plan.md`

---

## 1. 배경 / 문제

기존 SYS025는 `01=1일 / 02=0.5일 / 03=0.25일 / 04=0.125일`로, 모든 사용자가 하루 8시간(9시간 중 1시간 휴게)을 일한다는 전제였다.
실제로는 7시간 이하 근무나 1구간+2구간 스케줄이 존재해 `0.125=1시간` 매핑이 깨지고 "52분" 같은 어중간한 값이 나와 사용자가 납득하지 못한다.
→ **시간 단위(2시간/1시간/30분)로 끊어 쓰고, 그날 스케줄 기준으로 차지하는 비율만큼 차감**한다.

## 2. 범위 (포함)

### 2.1 SYS025 재정의 (B안: 깨끗한 재정렬)

`tb_syst_val_d`의 SYS025 상세코드를 아래로 **재정의**한다. (착수 시 운영 연차 데이터 없음 1회 확인 → 기존 상세코드 정리 후 재등록)

| 코드 | 명 | 의미 |
|---|---|---|
| `00` | 1일 | 1일 전체 (FULL) |
| `01` | 반차 | 0.5일 고정 (HALF) |
| `02` | 시간차 2시간 | 120분 단위 (배수 사용) |
| `03` | 시간차 1시간 | 60분 단위 (배수 사용) |
| `04` | 시간차 30분 | 30분 단위 (배수 사용) |

- 기존 참조 컬럼(`tb_leave_type_mgmt.USE_UNIT_TYPE`, `tb_user_leave_use.USE_UNIT_TYPE`)의 잔존값은 초기화/정리.

### 2.2 `tb_leave_usage_policy` 재모델 (법정연차 사용단위)

현재 `ALLOW_FULL_DAY / ALLOW_HALF_DAY / ALLOW_QUARTER_DAY / ALLOW_HOURLY`를 SYS025 00~04에 1:1로 맞춘다.

```sql
ALTER TABLE tb_leave_usage_policy
  DROP COLUMN ALLOW_QUARTER_DAY,
  DROP COLUMN ALLOW_HOURLY,
  ADD COLUMN ALLOW_HOUR_2  char(1) NOT NULL DEFAULT 'N' COMMENT '시간차 2시간 허용 (SYS025-02)' AFTER ALLOW_HALF_DAY,
  ADD COLUMN ALLOW_HOUR_1  char(1) NOT NULL DEFAULT 'N' COMMENT '시간차 1시간 허용 (SYS025-03)' AFTER ALLOW_HOUR_2,
  ADD COLUMN ALLOW_MIN_30  char(1) NOT NULL DEFAULT 'N' COMMENT '시간차 30분 허용 (SYS025-04)' AFTER ALLOW_HOUR_1;
-- ALLOW_FULL_DAY(00, 항상 Y) / ALLOW_HALF_DAY(01) / MAX_DAILY_REQUEST 는 유지
```

- ⚠️ **PRAFTA-018 §8.5.3 강제 규칙 유지**: `AXIS4=HALF_DAY → ALLOW_HALF_DAY='Y'` (해제 불가). `LeavePolicyServiceImpl`의 매트릭스 #4 로직 보존.

### 2.3 정밀도 상향 (DDL)

```sql
ALTER TABLE tb_user_leave_use   MODIFY COLUMN LEAVE_DAYS decimal(8,5) NOT NULL COMMENT '사용 일수 (시간차 동적 환산)';
ALTER TABLE tb_user_leave_grant MODIFY COLUMN USED_DAYS  decimal(8,5) NOT NULL DEFAULT 0.00000 COMMENT '사용 일수 캐시';
-- GRANT_DAYS 는 변경하지 않는다 (부여는 일 단위)
-- LEAVE_MINUTES(int) 는 기존 컬럼 그대로 활용
```

### 2.4 1일 소정근로분 계산 유틸 + 동적 차감

- `getDailyStdWorkMinutes(cmpnyCd, siteCd, userCd, workYmd)`:
  `tb_user_work_plan.WORK_PLAN_CD(=SCH_CD)` → `tb_sch_mgmt` 조회 →
  `(FST_SCH_END-FST_SCH_STR) - FST_SCH_BRK_MIN + (SEC구간 있으면 동일)`. 스케줄 없으면 null.
- 차감 일수 = `신청분 ÷ 1일 소정근로분`, `decimal(8,5)` 반올림. (반차는 0.5 고정)

### 2.5 연차타입 화면 정리 (결정 #3 반영)

- DDL: `ALTER TABLE tb_leave_type_mgmt DROP COLUMN APRV_STEP_CNT, DROP COLUMN HR_FINAL_APRV_YN;` (`APRV_USE_YN` 유지)
- 백엔드 attd03: `LeaveTypeCommand / LeaveTypeParam / LeaveTypeRequest / LeaveTypeResult / Attd03Mapper.xml`에서 두 필드 제거.
- 프론트 `LeaveTypeCreatePop.vue`:
  - "결재 단계 수" 입력 + "인사팀 최종 승인" 셀렉트 **제거** (결재 여부 Y/N 토글·증빙은 유지)
  - 사용단위 셀렉트의 **`"일"` suffix 하드코딩 제거**(`systValDNm + "일"`) — 새 라벨이 자기설명적("반차","시간차 2시간")
  - stale 기본값 `ref("03")` + 잘못된 주석 수정

## 3. 범위 외 (다른 작업으로)

- 휴게 가로지름 검증·휴게 시각 컬럼·신청/검증 로직 → **E**
- 시간차 신청 화면·결재 → **D/E**

## 4. 영향 파일 (검증된 경로)

- 백엔드: `web/attd/attd03/**`, `web/baim/baim07/dto/request/LeavePolicySaveRequest.java`, `common/cmm/leave/**`(`LeavePolicyServiceImpl`, `LeavePolicyVO`, `LeavePolicyCommand`, `LeavePolicyMapper.xml`)
- 프론트: `src/views/attd/popup/LeaveTypeCreatePop.vue`, `src/views/baim/Baim_07.vue`
- 테이블: `tb_syst_val_d`(SYS025), `tb_leave_usage_policy`, `tb_user_leave_use`, `tb_user_leave_grant`, `tb_leave_type_mgmt`

## 5. 정책 출처

- 근태 §8.1.1(사용단위), §8.5.3(AXIS4↔ALLOW_HALF_DAY 강제), §8.5.9(휴게/시작시각/1일환산 시스템 강제)

## 6. 주의

- SYS025 코드명을 화면에서 코드테이블 기반으로 표시하는 다른 화면이 없는지 착수 시 1회 확인.
- 동적 환산이 도입되므로, 같은 1시간이라도 사용자/일자별 차감이 달라짐을 UI 안내에 반영(E와 연계).
