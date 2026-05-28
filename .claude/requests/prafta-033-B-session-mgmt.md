# prafta-033-B — TBM 세션 관리 (W-04~06)

> 마스터 플랜: `prafta-033-plan.md` (매핑표 §3, C 보류 경계 §5 우선 숙지)
> 선행: `prafta-033-A-ddl-content.md` (세션/매핑 테이블이 생성되어 있어야 함)
> To-Be 사양: `ref/prafta-033/05_02_SESSION_MANAGEMENT.md`, `03_BACKEND_SPEC_WEB.md §5`

---

## 0. 범위 / 경계

| 포함 (B) | 제외 → 다른 단계 |
|---|---|
| W-04 세션 목록 (상태별 필터) | 교육 시작(`start`, OPENED→IN_PROGRESS) → **C** |
| W-05 세션 개설 (임시저장 DRAFT / 개설 OPENED) | 슬라이드 동기화/종료/강제종료 → **C** |
| W-06 세션 상세/수정/취소/비번 재발급 | 출결 모니터링/QR 입실 → **C** |
| 콘텐츠/위험성평가 선택 모달 | 이력 조회(W-12~15) → **D** |

> **핵심 경계**: B는 세션을 **`OPENED`까지** 다룬다. `IN_PROGRESS` 전이 버튼("교육 시작")은 화면에 표시하되, **클릭 동작(start API)·이동 대상(W-07 콘솔)은 C 단계**에서 연결한다. B에서는 해당 버튼을 "C 단계 연동 예정"으로 **비활성** 처리하거나 안내 토스트만 띄운다.

신규 모듈: 백엔드 `com.prafta.web.tbm.tbm02`, 프론트 `src/views/tbm/Tbm_02.vue`(+팝업).

---

## 1. 엔드포인트 (PRAFTA 규약 `/webApi/tbm02/*`)

| 화면 | 메서드/경로 | 용도 | To-Be 대응 |
|---|---|---|---|
| W-04 | `GET /webApi/tbm02/sessions` | 세션 목록(필터/페이징/집계) | §5.1 |
| W-06 | `GET /webApi/tbm02/session-detail` | 세션 단건 상세(+콘텐츠/위험성평가) | §5.2 |
| W-05 | `POST /webApi/tbm02/save-session` | 개설(OPENED) / 임시저장(DRAFT) | §5.3 |
| W-06 | `POST /webApi/tbm02/update-session` | 수정(DRAFT/OPENED만) | §5.4 |
| W-06 | `POST /webApi/tbm02/cancel-session` | 취소(DRAFT/OPENED만) | §5.5 |
| W-06 | `POST /webApi/tbm02/regenerate-passwords` | 비번 재발급(OPENED만) | §5.6 |
| 보조 | `GET /webApi/tbm02/content-options` | 콘텐츠 선택 모달(스코프 필터) | W-01 압축본 (tbm01 조회 재사용 가능) |
| 보조 | `GET /webApi/tbm02/risk-options` | 위험성평가 선택 모달 | 기존 위험성평가 모듈 조회 활용 |
| 보조 | `GET /webApi/tbm02/site-options` | 사업장 드롭다운 | 기존 사업장 조회 활용 |

> `start`(교육 시작)는 **C 소관**이라 B에 만들지 않는다. C 문서에서 `POST /webApi/tbm03/sessions/start` 로 정의.

---

## 2. W-04: 세션 목록

### 2.1 조회 (`GET /sessions`)
- 파라미터: `siteCd`, `statusCd`, `startDate`, `endDate`, `managerUserCd`, `searchKeyword`, `page`, `pageSize`
- 스코프: `CMPNY_CD=#{gvCmpnyCd}` + 사업장 권한자는 자기 `SITE_CD`만. `DEL_YN='N'`.
- 집계 컬럼(서브쿼리/조인): `attendanceCount`(출결 수), `completedCount`(이수 수), `notCompletedCount`, `riskCount`(연계 위험성평가 수). 출결 테이블이 비어있으면 0.
- 정렬: 상태 우선순위(IN_PROGRESS > OPENED > DRAFT > COMPLETED > CANCELLED) 또는 개설일 desc — 기본 개설일 desc.

### 2.2 상태별 액션 (05_02 §1.3)
| STATUS_CD | 액션(B 범위) |
|---|---|
| DRAFT | 수정 / 삭제(=취소 또는 DEL_YN) |
| OPENED | 수정 / 취소 / **진행(C 연동 예정)** |
| IN_PROGRESS | 진행 콘솔 이동(C) — B에서는 보기만 |
| COMPLETED | 이력 보기(D 이동) |
| CANCELLED | 보기만 |

### 2.3 위험성평가 연계 표시
- 행에 `🔗 N건` 아이콘. **0건이면 ⚠️ 경고색** (영업 메시지, 05_02 §1.4).

### 2.4 DTO
`SessionListRequest`(Lombok) → `SessionListParam`(record, `from(req, claims)`) → `SessionListQuery`(record) → `SessionListResult`(record) → `SessionListResponse`(Lombok @Builder, list+totalCount).

---

## 3. W-05: 세션 개설

### 3.1 입력 필드 (05_02 §2.2)
| 필드 | 필수 | 검증 |
|---|---|---|
| siteCd | Y | 자기 회사 사업장 |
| title | Y | ≤200자, trim |
| contentBody (리치 HTML) | Y(개설 시) | HTML 태그 제거 후 텍스트 ≥10자 (빈 `<p></p>` 거부) |
| contentMtrlCds (첨부 묶음 목록) | N | TB_TBM_EDU_MTRL 존재·스코프 검증 |
| riskRefs (위험성평가 목록) | N | 0건 허용(경고만) |
| gpsVerifyTypeCd | Y | AUTO/MANUAL/DISABLED (기본 AUTO) |
| managerGpsLat/Lng | AUTO 시 Y | decimal |
| gpsVerifyRadiusM | N | 50~1000 (기본 100) |
| gpsManualConfirmYn | MANUAL 시 'Y' 필수 | |
| overrideDescriptions (콘텐츠별 세션 설명) | N | MTRL_CD별 |

### 3.2 저장 동작 (05_02 §2.4)
- **임시 저장**: `STATUS_CD='DRAFT'`, 비번 미생성, 입실 불가.
- **개설하기**: `STATUS_CD='OPENED'`, **비번 자동 생성(서버)**, `OPENED_AT=NOW()`.
  - 입실 비번/종료 비번: 랜덤 6자리, **입실≠종료**. 02_BACKEND_SPEC_COMMON §11.2 `PwdGenerator` 패턴(기존 유틸 있으면 재사용).

### 3.3 트랜잭션 (한 번에)
1. 권한 게이트: safe + 회사별 커스텀 (개설 권한). 위반 시 `ApiException`.
2. 검증 (위 §3.1, 서버 권위).
3. `TB_TBM_SESSION` INSERT (SESSION_CD 채번 `FNC_CMM_SEQ_NEXTVAL(cmpnyCd,'TBM_SESSION_CD')`, prefix `T`+YYYYMMDD+SEQ).
4. `TB_TBM_SESSION_CONTENT` 다건 INSERT (DISPLAY_ORDER, OVERRIDE_DESC).
5. `TB_TBM_SESSION_RISK` 다건 INSERT (옵션).
6. (개설 시) `TB_TBM_SESSION_STATE` 초기 row UPSERT (CURRENT_SLIDE_INDEX=0, SYNC_STATE_CD='PAUSED').

### 3.4 응답
```json
{ "sessionCd": "T20260527001", "statusCd": "OPENED",
  "entryPwd": "123456", "exitPwd": "654321",
  "warningMessage": "위험성평가 연동되지 않은 TBM입니다. 사고 발생 시 설득력이 떨어질 수 있습니다." }
```
- `warningMessage`는 riskRefs 0건일 때만 포함.

### 3.5 개설 후 이동
- 임시저장 → 토스트 + W-04 이동.
- 개설 → To-Be는 W-07 자동 이동이나 **C 보류**이므로 B에서는 **W-06 상세(비번 표시)로 이동**. (W-07 이동은 C에서 연결)

### 3.6 GPS 처리 (프론트, 05_02 §2.3)
- 페이지 로드 시 `navigator.geolocation.getCurrentPosition`.
- 성공→AUTO+좌표 / 실패(timeout)→MANUAL 안내 모달(체크박스 확인→confirmYn='Y') / 권한거부→DISABLED 안내.
- 기존 GPS 수집 컴포저블 있으면 재사용(없으면 신규 `useGpsCapture.js`).

---

## 4. W-06: 세션 상세 / 수정 / 취소 / 비번 재발급

### 4.1 상세 (`GET /session-detail`)
- 응답: `session`(전 필드) + `contents`(매핑 묶음 목록, MTRL 제목/카테고리/항목수/override) + `risks`(연계 위험성평가, 표시명).
- ⚠️ **위험성평가 표시명(plan §8-1)**: TB_RISK_ASSESSMENT에 TITLE 없음. 공정명(COM002)+유해요인 등으로 구성하거나 위험성평가 모듈 조회 API 활용. **developer는 착수 시 위험성평가 담당 코드 확인 후 표시 문자열 확정**(불명확 시 사용자 질의).
- 비번(`entryPwd`/`exitPwd`)은 **OPENED·IN_PROGRESS 상태 + 관리자**에게만 응답에 포함. COMPLETED/CANCELLED에선 제외.

### 4.2 수정 (`POST /update-session`)
- 가능 시점: `STATUS_CD IN ('DRAFT','OPENED')`만. 그 외 거부(409).
- 수정 가능: title, contentBody, 콘텐츠 매핑, 위험성평가 매핑, GPS 설정.
- 수정 불가: entryPwd/exitPwd(재발급 API 별도).
- 매핑 갱신: 기존 매핑 delete 후 재insert(트랜잭션) 또는 diff 반영.

### 4.3 취소 (`POST /cancel-session`)
- 가능 시점: `STATUS_CD IN ('DRAFT','OPENED')`만 (IN_PROGRESS 이후는 강제종료=C).
- `STATUS_CD='CANCELLED'`, `CANCELLED_AT`, `CANCEL_REASON`(필수) 기록.
- SSE 알림(이미 입실자 존재 시 session.cancelled)은 **C 인프라** — B에서는 DB 상태만 변경(입실자 없음 전제).

### 4.4 비번 재발급 (`POST /regenerate-passwords`)
- 가능 시점: `STATUS_CD='OPENED'`만.
- 새 입실/종료 비번 생성(입실≠종료) UPDATE 후 응답.
- 확인 다이얼로그(프론트): "기존 비번 무효화 후 재발급".

### 4.5 상태별 화면 (05_02 §3)
- DRAFT/OPENED: W-05 폼 재사용(수정 모드) + 비번 표시 영역(OPENED) + "교육 시작" 버튼(C 연동 예정, 비활성).
- IN_PROGRESS: readonly + "진행 콘솔 보기"(C 이동, B에서는 비활성/안내).
- COMPLETED: readonly + "이력 상세"(D 이동).
- CANCELLED: readonly + 취소 사유.

---

## 5. 콘텐츠/위험성평가/사업장 선택 모달

- **콘텐츠 선택**(`GET /content-options`): tbm01 목록 조회를 스코프 필터(회사공통+자기사업장, USE_YN='Y')로 재사용. 다중 선택, 묶음(MTRL_CD) 단위.
- **위험성평가 선택**(`GET /risk-options`): 기존 위험성평가 모듈의 조회를 활용(자기 사업장 스코프). 다중 선택. 표시명 이슈는 §4.1과 동일.
- **사업장 선택**(`GET /site-options`): 기존 사업장 조회 활용.

---

## 6. Vue 컴포넌트 (권장, JS)

```
src/views/tbm/
├── Tbm_02.vue                      # W-04 목록 (메인)
└── popup/
    ├── TbmSessionForm.vue          # W-05 개설 / W-06 수정 공통
    ├── TbmSessionDetail.vue        # W-06 조회 모드
    ├── TbmContentSelector.vue      # 콘텐츠 선택 모달
    └── TbmRiskSelector.vue         # 위험성평가 선택 모달
```
- 라우팅/뷰는 기존 PRAFTA viewResolver 관례 따름(컴포넌트명 자동 로드). 색상/간격 CSS 변수, scoped, 공통 컴포넌트 우선.
- 리치 텍스트 에디터(plan §8-4): 기존 사용 여부 확인 후 도입(없으면 사용자 협의).

---

## 7. 작업 항목 분해 (developer)

1. **B-BE-1**: tbm02 모듈 골격 + 세션 목록 조회(집계 포함)
2. **B-BE-2**: 세션 개설/임시저장(비번 생성, 매핑 INSERT, 트랜잭션)
3. **B-BE-3**: 세션 상세 조회(콘텐츠/위험성평가 조인, 비번 노출 게이트)
4. **B-BE-4**: 수정/취소/비번재발급 (상태 게이트)
5. **B-BE-5**: 보조 조회(content-options/risk-options/site-options)
6. **B-FE-1**: Tbm_02 목록 화면(필터/상태배지/위험성평가 경고)
7. **B-FE-2**: 세션 개설/수정 폼(리치텍스트/GPS/첨부/위험성평가)
8. **B-FE-3**: 세션 상세(상태별) + 비번 표시/재발급
9. **B-FE-4**: 콘텐츠·위험성평가 선택 모달

---

## 8. 검증 기준

- [ ] 개설 시 비번 입실≠종료, OPENED만 생성, 임시저장은 비번 없음
- [ ] contentBody 빈 HTML 개설 거부(텍스트 ≥10자)
- [ ] AUTO인데 좌표 없음 / MANUAL인데 confirmYn≠Y → 거부
- [ ] 수정·취소는 DRAFT/OPENED만, 그 외 409
- [ ] 비번은 OPENED/IN_PROGRESS+관리자만 응답 노출
- [ ] 권한: 개설=safe+커스텀, 타 사업장 세션 접근 차단
- [ ] 위험성평가 0건 개설 허용 + 경고 메시지 반환
- [ ] "교육 시작" 등 C 소관 동작이 B에서 실행되지 않음(비활성/안내)
- [ ] 모든 쿼리 CMPNY_CD 스코프

---

**다음 단계**: `prafta-033-D-history.md` (이력). C(실시간)는 보류 — `prafta-033-C-live-session-DEFERRED.md` 참조.
