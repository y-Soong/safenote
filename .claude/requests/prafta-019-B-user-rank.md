# prafta-019-B · 사용자별 직급 관리

> **목적**: 연차 결재라인 구성·프리셋의 기반이 될 **직급 체계**를 도입한다.
> **의존**: 독립. (작업 D가 직급 프리셋에 사용) 참조: `prafta-019-plan.md`

---

## 1. 범위 (포함)

### 1.1 `tb_user` 직급 컬럼 추가 (DDL)

```sql
ALTER TABLE tb_user
  ADD COLUMN RANK_CD varchar(10) NULL COMMENT '직급 코드 (BAIM_VAL 직급 코드그룹 참조)' AFTER AUTH_CD;
```

- `tb_user`에는 현재 `NODE_CD`(소속부서)·`AUTH_CD`(권한)만 있고 직급이 없음 → 신규.

### 1.2 BAIM_VAL 직급 코드그룹 + 기본 세트 시드

- `tb_baim_val_m`에 직급 코드그룹 1건 신규. **`BAIM_VAL_CD`는 다음 가용 `COMxxx` 번호**(착수 시 `SELECT ... FROM tb_baim_val_m`로 확인하여 확정).
- `tb_baim_val_d`에 **기본 직급 세트**를 회사별 시드. `SORT_IDX`로 직급 순서를 부여한다(결재라인 프리셋이 직급순으로 구성됨).

기본 세트(제안 — 회사별로 Baim_02에서 수정 가능):

| SORT_IDX | 상세코드 | 직급명 |
|---|---|---|
| 1 | (01) | 사원 |
| 2 | (02) | 주임 |
| 3 | (03) | 대리 |
| 4 | (04) | 과장 |
| 5 | (05) | 차장 |
| 6 | (06) | 부장 |
| 7 | (07) | 이사 |

- 코드→명 변환은 기존 공통함수 `FNC_CMM_INFO_SRCH(cmpnyCd, 'BAIM_VAL', RANK_CD, '<COMxxx>')` 패턴 사용.

### 1.3 User_01 직급 배정 UI

- `src/views/user/User_01.vue`(사용자 관리)에 **직급 셀렉트** 추가 — BAIM_VAL 직급 코드그룹을 바인딩, `RANK_CD` 저장.
- 백엔드 `web/user` 모듈의 사용자 조회/저장 DTO·Mapper에 `RANK_CD` 반영.

### 1.4 직급 코드 관리

- 별도 관리 화면 신규 개발 **불요**. Baim_02(`web/baim/baim02`, 운영사 공통코드 M/D 관리)가 범용 코드관리라 직급 코드그룹을 거기서 등록·수정한다. (그룹 등록·시드만 본 작업에 포함)

## 2. 영향 파일

- 테이블: `tb_user`, `tb_baim_val_m`, `tb_baim_val_d`
- 백엔드: `web/user/**`(사용자 DTO/Mapper에 RANK_CD), (기존) `web/baim/baim02/**`
- 프론트: `src/views/user/User_01.vue`, (기존) `src/views/baim/Baim_02.vue`

## 3. 주의

- `SORT_IDX` = 결재라인 프리셋 순서의 근거이므로 반드시 일관된 순서로 시드.
- `COMxxx` 코드값은 추측 금지 — 착수 시 운영DB에서 다음 가용 번호 확정.
- 직급은 회사별로 다를 수 있어 "기본 세트 시드 + 회사가 Baim_02에서 수정" 방식.
