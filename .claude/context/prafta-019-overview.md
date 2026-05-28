# PRAFTA-019 정리 — 시간차 연차 + 결재 흐름 (2026-05-23)

> 시간차 연차 사용단위 도입 + 사용자별 직급 + 근태 마감 + 연차 요청별 결재라인 + 연차 신청·결재 흐름.
> 원본 초안 `prafta-019.md` → 5개 작업(A~E)로 분해(`prafta-019-plan.md`) 후 구현. 본 문서는 산출물·테이블·규칙 통합 정리.

---

## 1. 작업 단위 요약

| 코드 | 작업 | 핵심 |
|---|---|---|
| **A** | 시간차 연차 사용단위 | SYS025 재정렬, 사용정책 재모델, 동적 차감 유틸 |
| **B** | 사용자별 직급 | `tb_user.RANK_CD` + 직급 코드그룹(COM007) |
| **C** | 근태 마감 | 마감 테이블 + 마감/해제 + 사후신청 차단 기준 |
| **D** | 연차 요청별 결재라인 | 사용자 직접 구성 결재라인 + SYS044 |
| **E** | 연차 신청·결재 흐름 | 신청→검증→차감→결재→확정 (A·C·D 조립) |
| **E 후속** | 휴게시각 입력 / 휴게 가로지름 / 출근차단 / 신청 폼 / 요청승인관리·내 결재함 | |

---

## 2. 신규 테이블 (3종)

| 테이블 | 용도 | PK | 작업 |
|---|---|---|---|
| `tb_attd_close` | 근태 마감 상태 (회사+사업장+월) | (CMPNY_CD, SITE_CD, CLOSE_YM) | C |
| `tb_attd_close_hist` | 마감/해제 이력 | (CMPNY_CD, HIST_ID) | C |
| `tb_user_attd_req_approval` | 연차 요청 결재라인 (단계=지정 결재자) | (REQ_ID, APPROVAL_STEP) | D |

### tb_attd_close 주요 컬럼
`CLOSE_STATUS`(OPEN/CLOSED), `CLOSE_DTIME`/`CLOSE_USER_CD`, `UNCLOSE_DTIME`/`UNCLOSE_USER_CD`, `CLOSE_DESC`.

### tb_user_attd_req_approval 주요 컬럼
`REQ_ID`(→ tb_user_attd_req), `APPROVAL_STEP`(1부터), `APPROVER_USER_CD`(사용자 지정 결재자), `APPROVAL_STATUS`[SYS044], `APPROVAL_COMMENT`, `APPROVAL_DATE`.
인덱스: `(APPROVER_USER_CD, APPROVAL_STATUS)` — 내 결재함 조회용.

---

## 3. 변경 테이블

| 테이블 | 변경 | 작업 |
|---|---|---|
| `tb_user` | `+ RANK_CD varchar(10)` (직급, COM007 참조, AFTER AUTH_CD) | B |
| `tb_leave_usage_policy` | `ALLOW_QUARTER_DAY`/`ALLOW_HOURLY` 제거 → `ALLOW_HOUR_2`/`ALLOW_HOUR_1`/`ALLOW_MIN_30` 신설 | A |
| `tb_leave_type_mgmt` | `APRV_STEP_CNT`/`HR_FINAL_APRV_YN` 제거 (APRV_USE_YN 유지) | A |
| `tb_leave_policy` | `+ APRV_USE_YN char(1)` (법정연차 결재 여부) | E |
| `tb_sch_mgmt` / `tb_sch_mgmt_hist` | `+ FST_BRK_STR/END_TIME`, `+ SEC_BRK_STR/END_TIME` (휴게 시각, HHMM) | E |
| `tb_user_attd_req` | `LEAVE_DAYS` decimal(3,1)→**(8,5)** | E |
| `tb_user_leave_use` | `LEAVE_DAYS` decimal(5,1)→**(8,5)** | A |
| `tb_user_leave_grant` | `USED_DAYS` decimal(5,1)→**(8,5)** (GRANT_DAYS는 일 단위 유지) | A |

---

## 4. 코드 (시스템/공통)

| 코드그룹 | 의미 | 값 | 작업 |
|---|---|---|---|
| **SYS025** (재정렬) | 연차 사용단위 | `00`=1일 / `01`=반차 / `02`=시간차2시간 / `03`=시간차1시간 / `04`=시간차30분 | A |
| **SYS044** (신규) | 결재 단계 상태 | `00`=대기중 / `01`=신청 / `02`=승인 / `03`=반려 | D |
| **COM007** (BAIM, 신규) | 직급 | 사원/주임/대리/과장/차장/부장/이사 (SORT_IDX 1~7) | B |

> ⚠️ SYS043은 prafta-017-2 "연차 부여 방식"이 점유하여 결재 단계 상태는 **SYS044**로 신설.
> SYS025는 원래 `01~05`였으나(이미 시간차 의미) 결정대로 `00~04`로 재정렬(참조값 -1 리매핑).

---

## 5. 테이블 관계도 (연차 흐름 중심)

```
                                 [SYS025]  ← 사용단위
                                    ▲
                                    │ USE_UNIT_TYPE
   tb_baim_val_d (COM007)           │
        ▲ BAIM_VAL_D_CD             │
        │ RANK_CD                   │
     tb_user ───────────────► tb_leave_type_mgmt (회사정의: APRV_USE_YN, USE_UNIT_TYPE)
        │ USER_CD                          │   (SYSTEM_YN='Y' → 법정시드)
        │                                  │
        │            tb_leave_policy (법정: APRV_USE_YN) ──1:1── tb_leave_usage_policy
        │                                                         (ALLOW_FULL/HALF/HOUR_2/HOUR_1/MIN_30)
        │
        │  신청자/결재자
        ▼
   tb_user_attd_req (REQ_TYPE='05' 연차, REQ_STATUS[SYS033], LEAVE_DAYS 8,5)
        │ REQ_ID                         │ REQ_ID
        │ 1:N                            │ 1:1 (차감 예약)
        ▼                                ▼
   tb_user_attd_req_approval        tb_user_leave_use (LEAVE_STATUS: CONFIRMED/CANCELLED)
   (APPROVAL_STEP, APPROVER_USER_CD,     │ GRANT_ID         USE_UNIT_TYPE→[SYS025]
    APPROVAL_STATUS[SYS044])             ▼
                                    tb_user_leave_grant (GRANT_DAYS, USED_DAYS 8,5  ← 사용 합계 동기화)

   [출근차단/소정근로분]
   tb_user_work_plan (WORK_PLAN_CD = SCH_CD | LEAVE_CD)
        │ WORK_PLAN_CD = SCH_CD
        ▼
   tb_sch_mgmt (FST/SEC 시작·종료·휴게(분)·휴게시작/종료시각)  ← 1일 소정근로분 & 휴게 가로지름

   [사후 마감 차단]
   tb_attd_close (CMPNY_CD, SITE_CD, CLOSE_YM, CLOSE_STATUS)  ← 신청 시 isClosed 판정
```

핵심 링크 요약:
- `tb_user.RANK_CD` → `tb_baim_val_d`(COM007) — 직급명/순서(결재 프리셋).
- `tb_user_attd_req`(연차 1건) ─1:N→ `tb_user_attd_req_approval`(결재 단계), ─1:1→ `tb_user_leave_use`(차감).
- `tb_user_leave_use.GRANT_ID` → `tb_user_leave_grant`(차감 대상 부여). 사용 시 `USED_DAYS` 재계산.
- 결재 여부: 회사정의=`tb_leave_type_mgmt.APRV_USE_YN`, 법정(SYSTEM_YN='Y')=`tb_leave_policy.APRV_USE_YN`.
- 1일 소정근로분/휴게: `tb_user_work_plan.WORK_PLAN_CD`(=SCH_CD) → `tb_sch_mgmt`.

---

## 6. 연차 규칙 (핵심)

### 6.1 사용 단위 & 차감
- **1일(00)** = 1.00000 일, **반차(01)** = 0.50000 일 (+ LEAVE_MINUTES = 소정근로분/2).
- **시간차(02/03/04)** 차감 = `신청분(분) ÷ 그날 1일 소정근로분`, decimal(8,5), 반올림 HALF_UP.
  - **1일 소정근로분** = (1구간 (종료−시작) − 휴게분) + (2구간 동일, 있으면). 야간(종료≤시작)은 +1440 보정. 스케줄 없으면 시간차 신청 불가.
  - 고정분수(1/0.5/0.25/0.125) 폐기 → 사용자/일자별 스케줄 기준 **동적 환산**.

### 6.2 시간차 신청 검증 (순서)
1. 해당 일자 `tb_user_work_plan` 존재 (없으면 시간차 거부, 1일은 무관).
2. 신청 시각 파싱(시작<종료), **단위 배수**(2h=120·1h=60·30분=30) 정합.
3. 1일 소정근로분 산출, 신청분 ≤ 소정근로분.
4. **휴게 가로지름 거부**(§8.5.9): 신청 [시작,종료)가 스케줄 휴게 구간과 겹치면 거부. 휴게시각 미설정이면 skip.

### 6.3 결재 여부 & 결재라인
- **Y**: 사용자가 신청 시 결재라인 직접 구성(직급 프리셋 "내 위 직급 순" 보조). step1=신청(01), 이후=대기(00).
  - 승인 → 다음 단계 신청(01) 전환. **마지막 승인 → 요청 승인(02)**. 반려 → 요청 반려(03) + 차감 해제.
  - 보안 가드(매 단계): ① 요청 회사 소유권 ② 결재자 본인(토큰=APPROVER_USER_CD) ③ 단계 순서(현재 단계 '01'만).
- **N**: 즉시 확정(요청 승인 02).

### 6.4 차감 모델 (예약 → 해제)
- **신청 시**: `tb_user_leave_use` CONFIRMED 1건 + `tb_user_leave_grant.USED_DAYS` 재계산(부여 잔여 예약). 부여 선택은 만료 임박(AVAIL_TO_DATE) 우선 + 잔여 충분, `SELECT … FOR UPDATE`로 동시 과차감 방지.
- **반려 시**: 사용 CANCELLED + USED_DAYS 재계산(예약 해제).

### 6.5 사후 신청 & 마감
- 사전/사후는 결재 여부 기준이 아님(결재 여부는 6.3).
- **사후 신청(workYmd < 오늘)** 은 해당 월 `tb_attd_close`가 CLOSED면 차단. (예외는 근태 보정 — 별도)
- 근태 마감: 회사+사업장+월 단위. **자동/강제 마감 금지** — 차단 사유(미결 요청·GPS 미해소·미승인 추가근무) 0건일 때만 마감. 마감/해제는 매니저 권한 + cross-site 가드.

### 6.6 출근 차단 (노무 수령 거부, §8.3)
- **일 단위(00) 확정** 시 `tb_user_work_plan.WORK_PLAN_CD = LEAVE_CD`로 덮어 출근 차단. 반려 시 해제.
  - ⚠️ 한계: 원 스케줄(SCH_CD) 미복원 — 명시 근무계획이 있던 일자는 반려 후 기본 스케줄로 회귀(후속 보완 대상).
- **반차/시간차**: `tb_user_leave_use` 기반 출퇴근(check-in) 단계 구간 차단 — **후속 작업**(check-in 연계 미구현).

### 6.7 잔여
- `tb_user_leave_grant.USED_DAYS`가 확정 사용 합계로 자동 동기화 → 사용자 연차관리(Attd_09)에 반영.

---

## 7. API 엔드포인트

| 도메인 | 엔드포인트 | 비고 |
|---|---|---|
| 연차 신청 | `POST /leaveflow/apply` | 검증·차감 예약·라인 생성/즉시확정 |
| 결재 승인 | `POST /leaveflow/approve` | 단계 승인 (가드) |
| 결재 반려 | `POST /leaveflow/reject` | 반려 + 차감 해제 |
| 내 결재함 | `GET /leaveflow/my-approvals` | 내가 현재 단계 결재자인 연차 목록 |
| 결재자 후보 | `GET /user04/approval-candidates` | 동일 회사/사업장 + 직급 순서(프리셋) |
| 근태 마감 상태 | `GET /attd07/attd-close-status` | 차단 사유 카운트 + 마감 가능 여부 |
| 근태 마감/해제 | `POST /attd07/attd-close` · `/attd-unclose` | 매니저 권한 |

---

## 8. 화면

| 화면 | 역할 | prafta-019 변경 |
|---|---|---|
| Attd_03 연차 타입 관리 | 회사정의 연차 타입 | 결재단계수/인사팀승인 컬럼 제거, 사용단위 라벨 |
| Baim_07 연차 정책 | 법정 연차정책 | 시간차 토글 3종, 법정 결재여부 토글 |
| User_01 사용자관리 | 사용자 | 직급 셀렉트(COM007) |
| **User_04 연차 결재라인 구성** (신규) | 결재라인 직접 구성 | 후보 + 직급 프리셋 |
| Attd_05 근무 계획 관리 | 근무계획 | **연차 신청** 버튼(LeaveApplyPop) |
| Attd_07 근무 관리 | 근태/마감 | **근태 마감/해제** |
| Attd_09 사용자 연차관리 | 잔여 대시보드 | USED_DAYS 자동 반영 |
| **Attd_10 요청 승인 관리** (신규) | 4탭 셸 + **내 결재함(연차)** | 연차 탭 기능 완성 |
| Attd_01 스케줄(SchInfoPop) | 스케줄 | 휴게 시작/종료 시각 입력 |

> 신규 화면 메뉴 등록: `prafta-019-F-menu-register.sql` (Attd_10, User_04 — 회사 '001').

---

## 9. 후속 작업 / 한계

1. **요청승인관리 타 3탭**(스케줄 수정/근태 보정/초과근무) 결재 흐름 (현재 연차 탭만 기능).
2. **본인 결재(selfYn) 자동승인/차단 분기** — §7.3/§9.5 노드 자체근태승인 설정 기반.
3. **반차/시간차 출근 차단** — check-in 로직에서 `tb_user_leave_use` 기반 구간 차단.
4. **일 단위 반려 시 원 스케줄 복원** — 덮기 전 SCH_CD 보관(§10.1 한계).
5. **B 보안 선결함**: `update-user-infos` 외 rankCd 화이트리스트 검증, `selectSchHistList` 테넌트 조인 정정.
6. **`schema-full.sql` 재덤프** — A~F 마이그레이션 반영.

---

## 10. 마이그레이션 파일 (적용 순서)

`prafta-backend/src/main/resources/sql/migration/`
- `prafta-019-A-leave-time-unit.sql` (SYS025, 사용정책, 정밀도, 타입 컬럼)
- `prafta-019-B-user-rank.sql` (RANK_CD, COM007 시드)
- `prafta-019-C-attendance-close.sql` (마감 테이블 2종)
- `prafta-019-D-approval-line.sql` (결재라인 테이블, SYS044)
- `prafta-019-E-leave-request-flow.sql` (정책 APRV_USE_YN, 휴게시각, LEAVE_DAYS)
- `prafta-019-F-menu-register.sql` (Attd_10, User_04 메뉴)
