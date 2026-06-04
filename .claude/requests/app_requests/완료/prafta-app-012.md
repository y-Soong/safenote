# PRAFTA-app-012 — 아차사고/사건 보고 도메인 (앱 파트) 작업지시서

> **상태: 지시서(요청서)만 작성. 분해·구현 금지.** 사용자가 웹 파트(PRAFTA-040) 결과를 피드백한 뒤 앱 착수 예정.
> 영역: app (`prafta-app-frontend` webview Vue + `PRAFTA_FLUTTER/safenote` 셸).
> 설계 출처(단일): `.claude/context/near-miss-incident-design.md` (§3 채널매트릭스, §4-A/4-B, §5-A/5-B, §6 API, §7 결정필요).
> 웹 파트(공유 자원): `.claude/requests/web_requests/prafta-040.md` — 테이블 `tb_near_miss`, 코드 SYS061~063, 메뉴/권한은 웹 마이그레이션(prafta-040-near-miss.sql)에서 이미 생성됨. **앱은 같은 테이블/코드를 쓰되 백엔드는 완전 분리**.

---

## 0. 핵심 원칙 (app-010 기조 — 엄수)
- **앱 백엔드 완전 분리**: 앱 화면은 `/appApi/nearmiss/*` 신규 컨트롤러(`com.prafta.app.nearmiss.*`)만 호출. **web 컨트롤러(`/webApi/nearmiss01/*`) 호출 절대 금지.** 동일 테이블을 보더라도 mapper/service는 앱 패키지에 별도 구현(공유는 DTO/유틸 한정).
- **식별자는 JWT 클레임에서만**: cmpnyCd/siteCd/userCd 는 토큰에서 도출. 요청 바디 신뢰 금지. 사업장 스코프 + 관리노드 권한 가드로 IDOR 차단.
- **Flutter는 비즈니스 로직 금지**: 권한(카메라/위치)·QR·webview 호스팅만. 사건 보고 로직은 전부 Vue.

---

## 1. 앱 화면 (구현 대상, 착수 시 분해)

### 1-A. 근로자 — 아차사고 보고 (설계 5-A)
- 위치(예상): `prafta-app-frontend` `src/views/nearmiss/NearMissReport.vue` (실측 후 확정).
- 입력: 유형(SYS061 라디오), 발생일시(date+time), 발생장소(텍스트), 경위(textarea, 필수), 잠재중대성(SYS062 라디오), 사진(카메라/갤러리 다건), 즉시조치(텍스트) → [보고하기].
- 제출 시 `INSERT tb_near_miss` (REPORT_STATUS_CD='100' 접수, REPORTER_ID=본인, SRC_*=NULL).
- 사진 첨부는 `tb_file_info` 관리코드 — 단일 관리코드로 다중 사진 묶임 여부 확인(불가 시 첨부 자식테이블 검토, 설계 §2 확장여지).

### 1-B. 현장/안전관리자 — 1차 확인 (설계 5-B)
- 위치(예상): `src/views/nearmiss/NearMissManageList.vue` + 상세.
- 목록: 사업장 스코프, 상태 카운트(접수/검토중/조치중), 잠재중대성 배지, 행 → 상세.
- 상세: 보고내용 읽기 + 임시조치 메모 입력 + [접수→검토중(200) 전환] + [웹에서 정밀조사 필요 표시].
- **앱 관리자 조치 범위는 "상태전환 + 임시조치 메모"까지만**(정밀조사=웹 전용). 설계 §7-1 권고 = 전자. → 결정필요 D1 확정 반영.

---

## 2. 앱 백엔드 — `/appApi/nearmiss/*` 신규 (`com.prafta.app.nearmiss.*`)
- 보고 등록: POST `/appApi/nearmiss/report` (근로자, NEAR_MISS_ID 채번 NM+YYYYMMDD+SEQ).
- 내 보고 목록: GET `/appApi/nearmiss/my-reports`.
- 사업장 목록(관리자): GET `/appApi/nearmiss/site-incidents` (권한 가드).
- 상세: GET `/appApi/nearmiss/detail`.
- 1차 확인/상태전환: POST `/appApi/nearmiss/change-status` (100→200, 임시조치 메모 IMMEDIATE_ACTION_DESC 또는 별도 메모 컬럼 — 설계 최소안엔 즉시조치 1칸뿐, 임시조치 메모 저장 위치 확인 필요).
- 사진 업로드: 기존 app 파일 업로드 규약 재사용.
- multipart/JWT/스코프 규약은 app-003/app-004 보안 패턴 따름.

> ⚠️ 설계 최소안 DDL에는 "임시조치 메모"(관리자) 전용 컬럼이 없음(IMMEDIATE_ACTION_DESC는 보고자 즉시조치). 앱 1차확인 메모 저장 위치를 확정(기존 컬럼 재사용 vs 컬럼 추가) — 착수 시 결정.

---

## 3. 푸시 알림 (tb_noti_outbox 재사용)
- 트리거: 잠재중대성 ≥ 중대(SYS062 200/300) 신규 보고 시 사업장 안전관리자에게 푸시. 경미는 목록 배지만(설계 §4-A 2단계).
- 인프라: 신규 알림 테이블 만들지 말 것. 기존 `tb_noti_outbox` + SYS045(알림유형)에 NEAR_MISS 유형 코드 1건 추가(prafta-031 패턴). consumer 미구현 현황 확인(MEMORY prafta-031).

---

## 4. Flutter 셸 연동
- 권한: 카메라(사진 촬영), 위치(발생장소 보조 — 선택), 저장소. `permission_handler` 사전 안내.
- webview JS-bridge: 신규 브리지 필요 시 양쪽 명세 동시 갱신(카메라 호출 등은 기존 브리지 재사용 우선).
- `assets/vue_app/` 직접 편집 금지 — `prafta-app-frontend` 빌드 후 복사.

---

## 5. 착수 시 선행 확인 (결정필요 — 설계 §7)
- D1 앱 관리자 조치범위(권고: 상태전환+임시조치만). → 본 지시서는 권고안 전제로 기술.
- 임시조치 메모 저장 컬럼(신규 컬럼 vs 기존 재사용).
- 사진 다건 첨부 구조(tb_file_info 단일 관리코드 한계).
- SYS045 NEAR_MISS 알림유형 코드값 확정.
- 일용직(비정규) 보고 허용 범위(설계 채널매트릭스 근로자=정규 가정).

---

## 6. 비범위 (본 지시서에서 다루지 않음)
- 웹 정밀조사/완결/통계/위험성평가 연계 → PRAFTA-040(웹).
- 원 위험성평가요청 재분류(RiskAssessInfo 전환) → 웹 전용(PRAFTA-040-5).
