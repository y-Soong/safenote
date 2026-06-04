# prafta-023 H — EXPIRE_YN deprecation · 작업 분해 계획서

> prafta-023 follow-up H. 정책서 `attd/08-leave.md` §8.5.8 #4 ("EXPIRE_YN deprecation은 단계 2 이후 별도 작업").
> 데이터모델 마이그레이션이라 부여 엔진(#1~#3)과 성격이 다르고 광범위 → 단계로 분해하고 **Phase 1(무위험)만 즉시 적용**, 컬럼 DROP은 승인·검증 후.
> 작성일: 2026-05-24

---

## 1. 배경 / 현황

- **STATUS가 단일 진실원(SSOT)**: `LeaveGrantStatusServiceImpl.updateStatusWithSync`가 STATUS 전이 시 EXPIRE_YN/DEL_YN을 §8.5.8 매핑표대로 단방향 동기화. (ACTIVE/EXHAUSTED/CANCELED → EXPIRE_YN='N', EXPIRED → 'Y')
- EXPIRE_YN은 사실상 STATUS의 파생값. deprecation 목표 = 읽기 의존 제거 → (최종) 컬럼 DROP.

### EXPIRE_YN 사용처 실측 (TB_USER_LEAVE_GRANT)
| 구분 | 위치 | 처리 |
| --- | --- | --- |
| **읽기(실 필터)** | `LeaveFlowMapper.selectDeductibleGrant` `AND G.EXPIRE_YN='N'` | **STATUS='ACTIVE'와 중복** → Phase 1에서 제거 |
| 쓰기/동기화 | `LeaveGrantStatusMapper.updateStatusWithSync` / 만료배치 update | STATUS에서 동기화 — Phase 2까지 유지 |
| INSERT 기본값 | `LeaveDashboardMapper.insertManualGrant` (`EXPIRE_YN='N'`) | 컬럼 NOT NULL 대비 — Phase 2까지 유지 |
| 무관 | `WORK_EXPIRE_DATE`(dailyjoin/baim05) | 다른 컬럼(근무 만료일) — 대상 아님 |

→ **실제 읽기 의존은 selectDeductibleGrant 한 곳뿐**이고 그마저 redundant.

---

## 2. 단계 분해

### Phase 1 — EXPIRE_YN 읽기 의존 제거 (무위험, 스키마 변경 없음) ✅ 이번 적용
- `selectDeductibleGrant`의 `AND G.EXPIRE_YN='N'` 제거. STATUS='ACTIVE'(EXPIRED 배제) + AVAIL_TO_DATE>=workYmd(만료일 배제)로 충분 → **동작 불변**.
- 결과: EXPIRE_YN은 **쓰기 전용(STATUS 동기화 산출물)** 으로 강등. 읽는 코드 없음.
- 컬럼/동기화/INSERT 기본값은 그대로(보존).

### Phase 2 — 컬럼 폐기 (승인·검증 필요, 미적용)
- 선행 확인: **외부(모바일 앱 / 리포트 / BI / 운영 쿼리)에서 EXPIRE_YN을 읽는 곳이 없는지** 점검(코드 밖이라 자동 확인 불가 — 사용자/운영 확인 필요).
- 작업: (a) `updateStatusWithSync`·만료배치·insertManualGrant에서 EXPIRE_YN 동기화/세팅 제거, (b) `LeaveGrantStatusVO.expireYn` 등 제거, (c) `ALTER TABLE ... DROP COLUMN EXPIRE_YN`(+ EXPIRE_DATE 동반 폐기 여부 검토) 마이그레이션 + 롤백 스크립트.
- 리스크: 운영 데이터/외부 의존 → 백업·dev 선검증·롤백 필수(M1과 동급 승인 대상).

---

## 3. 검증
- Phase 1: 연차 차감(`selectDeductibleGrant`)이 **만료 부여를 여전히 제외**하는지 런타임 확인 — 만료(STATUS=EXPIRED 또는 AVAIL_TO_DATE 경과) 부여가 차감 후보에서 빠지는지.
- Phase 2: 별도 기획 시 회귀 전반.

## 4. 결론
Phase 1만 즉시 적용(읽기 의존 해소). 컬럼 DROP(Phase 2)은 외부 읽기 점검 + 승인 후 별도 진행.
