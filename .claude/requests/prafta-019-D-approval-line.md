# prafta-019-D · 연차 요청별 결재라인

> **목적**: 결재가 필요한 연차 신청에서 **사용자가 결재라인을 직접 구성**하고, 그 라인을 저장·추적한다.
> (초안의 NODE 자동 상향추적은 폐기 — 결정 #1)
> **의존**: B(직급 프리셋). 작업 E가 이 라인을 태운다. 참조: `prafta-019-plan.md`

---

## 1. 범위 (포함)

### 1.1 결재라인 구성 화면 (User_04 신규)

- `src/views/user/User_04.vue` **신규** — 연차 신청 시 결재자를 순서대로 구성하는 UI.
- **직급 프리셋 보조**: 작업 B의 직급(`RANK_CD`)·순서(`SORT_IDX`)를 활용해 "내 위 직급 순으로 자동 구성" 같은 프리셋 제공. 사용자가 가감 가능.
- 결재자 후보: 동일 회사/사업장 사용자(권한·소속 스코프는 공통 정책 준수).

### 1.2 결재라인 저장 테이블 (`tb_user_attd_req_approval` 신규)

초안 테이블을 **NODE 파생이 아니라 "사용자 정의 라인"** 으로 재설계한다. 컬럼 후보(planner 확정):

| 컬럼 | 설명 |
|---|---|
| APPROVAL_ID (PK) | 결재 단계 식별자 |
| REQ_ID | 연관 요청 (`tb_user_attd_req.REQ_ID`) |
| CMPNY_CD | 회사 코드 |
| APPROVAL_STEP | 결재 단계 (1부터, 사용자가 구성한 순서) |
| APPROVER_USER_CD | **사용자가 지정한 결재자** (NODE 파생 아님) |
| APPROVAL_STATUS | 단계 상태 [SYS043] 00 대기/01 신청/02 승인/03 반려 |
| APPROVAL_COMMENT | 결재 코멘트 |
| APPROVAL_DATE | 처리 일시 |
| 표준 메타 | INSERT/UPDATE |

- UNIQUE(REQ_ID, APPROVAL_STEP). 인덱스: 결재자별 대기 목록 조회용(APPROVER_USER_CD, APPROVAL_STATUS).

### 1.3 SYS043 (결재 단계 상태) 신규 코드그룹

```
SYS043: 00 대기중 / 01 신청 / 02 승인 / 03 반려
```
- `tb_syst_val_m` 1건 + `tb_syst_val_d` 4건 등록.

## 2. 범위 외 (E에서 처리)

- 라인 생성 시점·진행(승인/반려 시 다음 단계 전환)·즉시확정 판단은 **E**.
- 자기결재 자동승인 처리 규칙은 **E**에서 §9.5(자기 승인 원칙) 정합으로 확정 (본 작업은 라인 저장 구조만 제공).

## 3. 영향 파일

- 테이블: `tb_user_attd_req_approval`(신규), `tb_syst_val_m/d`(SYS043)
- 백엔드: 결재라인 구성·조회 API (`web/user` 또는 결재 모듈 — planner 배치 확정)
- 프론트: `src/views/user/User_04.vue`(신규)

## 4. 정책 출처

- 요청승인관리 재기획서 §9.1(ApprovalRequest), §9.2(annual_leave: approvalSteps), §9.3(처리 권한 스코프)
- 근태 §9.5(승인/반려 공통 규칙)

## 5. 주의

- "결재라인 직접 구성"이 재기획서의 단일 ApprovalRequest 엔티티 + approvalSteps와 정합되도록 매핑(스텝 = 사용자 지정 결재자).
- 결재자 후보 가시 범위는 권한·사업장 스코프(공통 §8.4) 준수.
