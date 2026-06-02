# prafta-app-011 구현 브리프 (메인 세션 작성 — 단일 출처)

> 원본 요청: `.claude/requests/app_requests/prafta-app-011.md`
> 시안/스펙: `.claude/requests/app_requests/refs/prafta-app-011/prafta_safety_inspect_v1.html`, `prafta-request-safety-inspect.md`
> 본 문서는 시안을 **실제 코드 현실**과 대조해 확정한 구현 기준이다. 시안과 충돌 시 본 문서가 우선.

---

## 0. 가장 중요한 정정 (시안 ≠ 현실)

시안은 greenfield(`prafta-web-frontend/src/views/safety/...` 신규)로 작성됐으나, **이미 동작 중인 기존 흐름의 전면 리디자인**이다.

기존 자산(모두 `prafta-app-frontend/prafta-app-frontend/` 하위, app FE):
- `src/views/main/components/SafetyActivityCard.vue` "안전점검 시작" → MainView `onSafetyCheck()` → `router.push('/QrScanner')`
- `src/views/_common/QrScanner.vue` — Vue `html5-qrcode` 웹캠 스캐너. 스캔 시 `router.replace('/ChkLst', { query: { qr: decodedText } })`
- `src/views/chkLst/ChkLst.vue` — 점검 응답 화면(양호/불량 라디오 + 불량 시 사진1장+메모). QR을 `route.query.qr`로 받아 파싱
- 라우트: `src/router/index.js` 에 `/QrScanner`, `/ChkLst`, `/Risk_01` 등록됨

백엔드(우리 코드, `prafta-backend`):
- `com.prafta.app.chkLst.chkLst01` 모듈 (`AppChkLst01Controller`, base `@RequestMapping("/chkLst01")`, 앱 프리픽스 `/appApi`)
- `GET /appApi/chkLst01/checklist-infos?siteCd&chkptCd`
- `POST /appApi/chkLst01/save-inspect-result` (multipart/form-data)

## 1. 확정 결정 (사용자 승인)

1. **영역**: app-frontend(`prafta-app-frontend`) + prafta-backend. 시안의 web-frontend 경로는 오기.
2. **화면 A(QR 스캐너)**: 시안 디자인(다크 뷰파인더+가이드프레임+스캔라인+카메라권한 폴백+미등록 QR 토스트)으로 교체하되 **스캔 엔진은 기존 `html5-qrcode` 유지**.
3. **백엔드 전략**: 시안 신규 엔드포인트 신설 안 함. **기존 `chkLst01` 확장**.
   - `checklist-infos` 응답에 컨텍스트(chkptNm, siteNm, chklstType, 전체 항목수 산출 가능) 추가.
   - 저장은 **기존 multipart 일괄 방식 유지**(첨부 즉시 업로드 미도입).
4. **타 사업장 QR**: 현재의 "토큰값 덮어쓰기"를 **불일치 시 403 차단**으로 변경(+ 미등록 토스트).

추가 (사용자 별도 요청, 이미 메인 세션이 직접 완료):
- Flutter 셸 카메라 권한 **하드 게이트** 추가 (`PRAFTA_FLUTTER/safenote/lib/camera_gate.dart` 신규, `main.dart` 체이닝 `LocationGate(next: CameraGate())`). 위치 게이트(`location_gate.dart`)는 기존 완비 — `next` 파라미터만 추가. `flutter analyze` 통과.

## 2. 실제 DB 스키마 (MCP 확인, 2026-05-30)

### TB_CHKPT_TYPE_MGMT (체크포인트 마스터)
PK (CMPNY_CD, SITE_CD, CHKLST_TYPE, CHKPT_CD)
- CHKPT_NM varchar(100) '체크포인트명' ← 컨텍스트 카드 제목
- CHKPT_DESC varchar(500) '비고'
- CHKLST_TYPE varchar(10) '체크리스트 타입'
- MGMT_USER_CD, USE_YN varchar(2) 기본 'Y'

### TB_CHKPT_INSPECT_ITEM (점검 항목 마스터)
PK (CMPNY_CD, CHKLST_TYPE, INSPECT_ITEM_CD)  ← **SITE_CD 없음**(회사+타입 단위 공유)
- INSPECT_ITEM_SUBJ varchar(200) NOT NULL '점검항목명칭'
- SORT_IDX int '정렬순서'
- STR_DATE varchar(6) NOT NULL '시행일자(YYYYMM)'
- USE_YN varchar(2) 기본 'Y'

### TB_CHKPT_INSPECT_ANSWER (점검 응답)
PK (CMPNY_CD, SITE_CD, CHKPT_CD, INSPECT_ITEM_CD, WORK_DATE)  ← **점검자 컬럼 없음**
- INSPECT_ANSWER_TYPE varchar(2) NOT NULL '점검답변타입[SYS009]'
- ANSWER_DESC text '점검답변상세'
- FILE_MGMT_CD varchar(50) '첨부사진코드' (단일 → 항목당 1장)
- INSERT_NO/UPDATE_NO ← 점검자(token userCd)

### TB_SITE
- SITE_NM varchar '사업장명' ← 컨텍스트 카드 메타

### SYS009 (TB_SYST_VAL_D)
- Y=양호, N=불량 (2종 확정. 회피값 없음)

### CHKLST_TYPE 마스터 → **없음**
- `chklstTypeName` 출처 테이블 부재. 폴백: 컨텍스트 카드 메타는 `{사업장명} · {작업일자}` 위주로 구성하고, 타입명은 **CHKPT_NM 사용 또는 생략**. (별도 마스터 신설은 본 작업 범위 외)

## 3. 백엔드 변경 (developer)

### 3.1 checklist-infos 응답 확장
- 현재 `selectChkLstInfo` SELECT: cmpnyCd, chklstType, chkptDesc, inspectItemCd, inspectItemSubj, 'Y' as inspectValue, sortIdx (응답 키 `checklistInfos` 리스트)
- 추가 필요: 체크포인트 컨텍스트(chkptNm, siteNm). 방법:
  - mapper에 `TB_SITE` 조인(`A.CMPNY_CD=S.CMPNY_CD AND A.SITE_CD=S.SITE_CD`)하여 SITE_NM 별칭 siteNm, CHKPT_NM 별칭 chkptNm 추가
  - 응답 구조: 기존 `checklistInfos` 키 **보존**(FE 하위호환), 추가로 `checkpoint` 객체(chkptNm, siteNm, chklstType, chkptDesc, totalCount) 동봉. 또는 각 row에 chkptNm/siteNm 포함 후 FE가 첫 row에서 추출(리스트 row마다 중복이지만 단순). **권장: 응답 DTO에 checkpoint 객체 + items 리스트 분리** (단, 기존 `checklistInfos` 키도 당분간 유지하거나 FE를 새 구조로 동시 전환).
- `inspectValue`가 현재 하드코딩 'Y' → 신규 화면은 "미답"이 기본이어야 하므로 FE에서 무시(초기값 미설정). (previousAnswer 프리필은 범위 외 — 시안 §5.1 검토와 동일)

### 3.2 siteCd 403 차단 (결정 4)
- `ChecklistInfoParam.from` / `InspectResultSaveParam.from`: 현재 req siteCd ≠ token siteCd 시 **경고 후 토큰값 강제** → **불일치 시 403(SITE_MISMATCH)로 차단**하도록 변경.
- 403 바디에 `userSiteName`(token siteCd의 SITE_NM) 포함 권장 → FE 미등록 토스트 문구 구성.
- 체크포인트 미존재/USE_YN!=Y → 404(CHKPT_NOT_FOUND). (현재는 response==null → COMMON_400_002. 케이스 7과 매핑되게 정리)
- 적절한 CommonErrorCode 신설 또는 기존 코드 매핑은 developer가 기존 에러 컨벤션 확인 후 결정.

### 3.3 save 응답 보강
- 현재 `save-inspect-result`는 200 빈 바디. 화면 C(저장완료)용으로 `{ chkptName, okCount, badCount, savedCount, workDate }` 반환 권장(FE가 응답으로 요약 표시). 미반환 시 FE가 메모리값으로 요약 구성도 가능(폴백).
- 서버측 전 항목 응답 강제(§5.2-7), 불량 사유 빈값 검증(§5.2-4)은 **선택적 강화** — 기존 동작 회귀 주의. 우선 FE 게이팅으로 충족하고, 서버 검증은 추가 시 기존 호출 호환 확인.

### 3.4 UPSERT/주입 — 이미 충족
- `mergeChkptInspectAnswer` 이미 `ON DUPLICATE KEY UPDATE`(재점검 §3.7 OK). cmpnyCd/userCd는 token 값 사용(주입 안전). 변경 불필요.

## 4. 프론트 화면 (planner 골격 → developer script)

경로: `prafta-app-frontend/prafta-app-frontend/src/` (이중 중첩 Vite 루트). viewResolver 자동 로딩 규칙은 메모리 `project_prafta_frontend_layout` 참조 — 단 본 건은 기존처럼 `router/index.js` 명시 등록 방식 유지.

### 화면 A — QrScanner.vue 리디자인 (`views/_common/QrScanner.vue`)
- 다크 헤더(좌X "QR 스캔"), 카메라 뷰파인더(html5-qrcode 영역), 240×240 가이드 프레임(4모서리 코너 마커), 그린 스캔라인 애니메이션(@keyframes), 안내문구, 하단 56×56 원형 닫기.
- 카메라 권한 거부 폴백(케이스 6): 경고 아이콘+안내+[설정으로 이동]. **단 Flutter 카메라 게이트가 선행 차단하므로 웹뷰 내 폴백은 보조**(html5-qrcode getCameras 실패 시).
- 미등록 QR 토스트(케이스 7): 스캔값 파싱 후 siteCd/chkptCd 추출 → `/ChkLst` 이동 시 백엔드 403/404면 ChkLst에서 토스트. (스캐너 단계에서 형식 오류만 토스트, 등록여부 검증은 백엔드 응답 기반)
- 스캔 엔진/라우팅 동작(`router.replace('/ChkLst', {query:{qr}})`) 보존.
- 색: CSS 변수 사용. 스캔라인/가이드 등 시안 §4.2 수치 준수.

### 화면 B — 점검 응답 리디자인 (`views/chkLst/ChkLst.vue` 교체)
시안 §4.3. 컴포넌트 분해(아래)를 `views/chkLst/components/` 또는 `components/safety/` 에 배치.
- 라이트 헤더(← "안전점검")
- 컨텍스트 카드: 아이콘+CHKPT_NM+메타(`{사업장명} · {YYYY-MM-DD}`; 타입명은 폴백 정책 §2)
- 진행 카운터: `응답 N/M`, `양호 X · 불량 Y`, 진행 바
- 점검 항목 카드: 양호/불량 2분할 토글, 상태 3종 톤(§2.3), 불량 시 사유(필수,≤500자,카운터)+사진(1장) 펼침
- 푸터 [저장]: 전 항목 응답 + 불량 사유 1자+ 일 때만 활성. 진행중 "저장 (N개 남음)" disabled
- 저장: 기존 multipart 계약 유지(`save-inspect-result`). 성공 시 화면 C.
- 이탈(←/백): 응답 0건 즉시 복귀, 1건+ 시 확인 모달("입력 중인 점검 응답이 사라져요...")

### 화면 C — 저장 완료 (`SafetyInspectSavedView`, 신규 라우트)
시안 §4.4. 성공 아이콘+타이틀+요약(`{체크포인트명} · 양호N · 불량M`)+[다른 개소 점검](→화면A)/[메인으로](Primary).

### 컴포넌트 분해 (시안 §1.2, app FE 경로로)
- `SafetyInspectContextCard.vue`, `SafetyInspectProgress.vue`, `SafetyInspectItem.vue`(양호/불량 토글), `SafetyInspectBadForm.vue`(사유+사진), `SafetyInspectSavedView.vue`, `SafetyQrErrorOverlay.vue`(미등록 토스트), `SafetyCameraPermissionView.vue`(폴백). 배치 디렉토리는 developer 판단(기존 컨벤션 우선).

### 화면 작업 절대 규칙 (CLAUDE.md)
- 색/폰트/간격은 CSS 변수만. TS 금지. `<style scoped>`. 공통 컴포넌트 우선. 인라인 SVG(CDN 금지). 모바일 360~414px. 터치 44×44(카메라 닫기 56).
- 버튼 초록은 직전 작업 기준색 사용: primary `#16a34a`(green-600), pressed `#15803d`(green-700).

## 5. 범위 외 (시안 §명시)
다중 첨부 사진, 일시저장(임시보관), previousAnswer 프리필, 점검개소 리스트 화면, 시정조치/위험성평가, CHKLST_TYPE 마스터 신설, 점검자 컬럼 분리, STR_DATE 필터(기본 미적용 — 전 USE_YN=Y 노출).

## 6. 검증 (qa, security)
- qa: 상태 전이(미답/진행/완료), 저장 게이팅, 이탈 모달, 멀티파트 저장 회귀, 컨텍스트 카드 데이터 정합.
- security: 403 site-block IDOR 차단(타 사업장 chkpt 접근), token 기반 cmpny/user 강제 유지, 파일 업로드 경로 주입(기존 H-3 가드 유지), QR 페이로드 파싱 안전.
