# 작업 요청서: 사용자 안전점검 수행 화면 (모바일 앱)

> **버전**: 1.0
> **작성일**: 2026-05-28
> **대상 에이전트**: planner
> **참조 산출물**: `prafta_safety_inspect_v1.html` (7개 화면 케이스 — QR 스캔 / 인식·미답 / 진행·불량 펼침 / 전체 완료 / 저장 완료 / 권한 거부 / 미등록 QR)
> **선행 작업 요청서**:
> - `../앱_사용자_메인/prafta-request-home.md` §3.4.2 (안전 활동 카드의 "안전점검 시작" 진입 동선 — 본 작업이 그 진입 대상)
>
> **본 작업에서 다루는 것**: QR 스캔 → 체크포인트 식별 → 점검 항목 일괄 응답 → 일괄 저장. 카메라 권한 폴백, 미등록 QR 폴백.
>
> **본 작업에서 다루지 않는 것 (후속 작업으로 분리)**: 점검 1건 상세 조회(`../앱_사용자_안전점검내역조회/`의 펼침 영역으로 처리), 시정조치 등록·진행, 위험성평가 점수, 다중 첨부 사진, 일시 저장(임시 보관), 점검 개소 리스트 화면(QR 스캔이 단일 진입점), 백오피스에서의 QR 발급·인쇄.

---

## 1. 개요

### 1.1 배경

PRAFTA 산업안전 정책서 §5.4(안전점검 흐름)의 점검 수행 화면은 모바일 사용자가 본인이 위치한 사업장의 체크포인트(설비·구역·차량 등)에 대해 일일 점검을 등록하는 화면이다. 기존 모바일 앱에는 메인 홈의 "안전 활동 > 안전점검 시작" 버튼만 정의되어 있고(`prafta-request-home.md` §3.4.2), 실제 점검을 수행하는 화면이 없었다.

본 화면은 다음 사용 시나리오를 충족한다:
- 사용자가 사업장 현장의 점검 개소(예: 지게차, 분전반, 비상구)에 부착된 **QR 코드를 스캔**하여 곧장 해당 체크포인트의 점검 화면으로 진입
- 점검 항목 N개에 대해 **양호/불량 단 2종**으로 응답
- 불량 발견 시 **사유 + 사진 1장**으로 근거 기록
- 모든 항목 응답 후 **일괄 저장**

핵심 설계 원칙:
- **QR 스캔이 단일 진입점**: 점검 개소 리스트를 별도로 노출하지 않는다. 현장에서 QR을 보고 스캔하는 것이 자연스러운 동선이며, 잘못된 사업장의 QR은 시스템이 막는다 (§3.1)
- **양호/불량은 단 2종**: DB 코드 마스터(`SYS009`)에 Y/N만 정의되어 있으며, "해당 없음" 등의 회피값을 도입하지 않는다. 모든 항목에 명확한 판정이 요구된다 (§3.3)
- **불량 펼침으로 입력 부담 분산**: 불량 항목에만 사유·사진 입력 영역이 펼쳐진다. 양호 항목은 토글만으로 처리 완결
- **전체 응답 후 일괄 저장**: 항목별 즉시 저장이 아닌 화면 단위 일괄 저장. 점검 도중 화면 이탈 시 입력 내용은 폐기되며, 부분 저장(임시 보관) 기능은 본 작업 범위 외

### 1.2 화면 위치

- **IA 경로**: 메인 > 안전 활동 > 안전점검 시작 (QR 스캐너) → 안전점검 (응답 화면) → 저장 완료
- **앱 네비게이션 진입점**: 메인 홈의 "안전 활동" 카드 > "안전점검 시작" 버튼 (`HomeView.vue` 내 `SafetyActivityCard.vue`)
- **파일 위치**:
  - `prafta-web-frontend/src/views/safety/SafetyQrScanView.vue` (QR 스캐너 풀스크린)
  - `prafta-web-frontend/src/views/safety/SafetyInspectView.vue` (점검 응답 화면)
- **컴포넌트 위치**: `prafta-web-frontend/src/components/safety/`
  - `SafetyInspectContextCard.vue` (체크포인트 컨텍스트 카드)
  - `SafetyInspectProgress.vue` (응답 진행 카운터 + 진행 바)
  - `SafetyInspectItem.vue` (점검 항목 1개 카드 — 양호/불량 토글 + 불량 펼침)
  - `SafetyInspectBadForm.vue` (불량 항목의 사유 + 사진 입력 영역)
  - `SafetyInspectSavedView.vue` (저장 완료 안내)
  - `SafetyQrErrorOverlay.vue` (미등록 QR 안내 토스트)
  - `SafetyCameraPermissionView.vue` (카메라 권한 거부 폴백)

### 1.3 화면 구성

본 작업은 **QR 스캐너 화면 + 점검 응답 화면**의 2개 풀스크린 + 1개 완료 안내로 구성된다.

#### 화면 A: QR 스캐너 (`SafetyQrScanView`)
1. 다크 헤더 (좌측 X 닫기 + 중앙 "QR 스캔")
2. 카메라 뷰파인더 (전체 영역)
3. 가이드 프레임 (240×240, 4모서리 코너 마커)
4. 그린 스캔 라인 (애니메이션)
5. 안내 텍스트 ("점검 개소의 QR 코드를 스캔해 주세요" + 보조)
6. 하단 원형 닫기 버튼 (56×56)

#### 화면 B: 점검 응답 (`SafetyInspectView`)
1. 헤더 (좌측 ← + 중앙 "안전점검")
2. 체크포인트 컨텍스트 카드 (아이콘 + 명칭 + 메타)
3. 진행 카운터 (응답 N/M · 양호/불량 분포 + 진행 바)
4. 점검 항목 카드 리스트 (항목 = 카드 1개, 양호/불량 토글, 불량 시 사유 + 사진 펼침)
5. 푸터 [저장] (전 항목 응답 시에만 Primary 활성)

#### 화면 C: 저장 완료 (`SafetyInspectSavedView`)
1. 헤더 (좌측 ← + 중앙 "안전점검")
2. 본문 중앙: 성공 아이콘 + 타이틀 + 요약 텍스트
3. 푸터 [다른 개소 점검](Secondary) + [메인으로](Primary)

### 1.4 시안 파일

구체적 UI는 `prafta_safety_inspect_v1.html` 참조. 총 7개 케이스를 담고 있다:
1. QR 스캔 대기 (뷰파인더 + 가이드 + 스캔 라인)
2. 인식 성공 · 전체 미답 (점검 항목 6개 모두 미응답, 저장 disabled)
3. 답변 진행 중 (양호 3 + 불량 1 펼침 + 미답 2, 저장 "2개 남음" disabled)
4. 전체 완료 (양호 5 + 불량 1, 진행 100%, 저장 Primary 활성)
5. 저장 완료 안내 (성공 아이콘 + 요약 텍스트)
6. 카메라 권한 거부 폴백
7. 미등록 QR 인식 실패 (Danger 토스트)

---

## 2. 핵심 정보 모델

### 2.1 화면이 표현하는 정보 단위

| 정보 단위 | 화면 영역 | 데이터 출처 |
|---|---|---|
| QR 인식 결과 (체크포인트 식별) | 화면 A → 화면 B 전환 | QR 인코딩값을 파싱한 `SITE_CD + CHKPT_CD` |
| 체크포인트 메타 | 컨텍스트 카드 | `TB_CHKPT_TYPE_MGMT` (CHKPT_NM, CHKPT_DESC, CHKLST_TYPE) |
| 점검 항목 리스트 | 항목 카드 N개 | `TB_CHKPT_INSPECT_ITEM` (CHKLST_TYPE 일치, USE_YN=Y, SORT_IDX 정렬) |
| 사용자 응답 (양호/불량 + 사유 + 사진) | 항목 카드 내부 + 펼침 | 화면 메모리 (저장 시 `TB_CHKPT_INSPECT_ANSWER` UPSERT) |
| 진행 카운터 | 진행 카운터 영역 | 화면 메모리 (응답 N / 전체 M, 양호·불량 분포) |

### 2.2 상태값 정의

| 상태 | 조건 | UI |
|---|---|---|
| QR 스캐닝 | 화면 A 진입, 카메라 권한 허용, 인식 대기 | 뷰파인더 + 그린 스캔 라인 애니메이션 + 안내문구 (케이스 1) |
| 카메라 권한 거부 | 시스템 권한 거부 또는 사용자 거절 | Warning 아이콘 + 안내 + [설정으로 이동] (케이스 6) |
| QR 인식 실패 (미등록) | 인식한 코드가 본인 소속 사업장의 USE_YN=Y 체크포인트가 아님 | 다크 뷰파인더 위 Danger 토스트 + [다시 스캔] (케이스 7) |
| 점검 미답 | 화면 B 진입 직후, 응답 0/M | 토글 모두 비활성 톤, 푸터 [저장] disabled (케이스 2) |
| 점검 진행 중 | 0 < 응답 < M | 일부 항목 카드 ok/bad 톤, 푸터 "저장 (N개 남음)" disabled (케이스 3) |
| 점검 완료 | 응답 == M | 전 항목 ok/bad 톤, 푸터 [저장] Primary 활성 (케이스 4) |
| 저장 완료 | POST 성공 | 화면 C 노출 (케이스 5) |

### 2.3 항목 카드의 시각 상태 3종

| 상태 | 토글 | 카드 보더·배경 | 번호 칩 |
|---|---|---|---|
| 미답 | 양쪽 토글 비활성(`--color-text-secondary`) | `--color-border` + `--color-surface` | `--color-border-light` 회색 |
| 양호 | 좌측 토글 활성(`--color-primary-tint` 배경 + `--color-primary` 보더) | `--color-primary-tint-border` + 옅은 그린 (`#F7FDF9`) | `--color-primary` + 흰 글씨 |
| 불량 | 우측 토글 활성(`--color-danger-tint` 배경 + `--color-danger` 보더) | `#FECACA` 보더 + 옅은 적 (`#FEF7F7`) | `--color-danger` + 흰 글씨 |

색 단독 정보 전달 금지 원칙에 따라 번호 칩 배경색 + 토글 활성 위치 + 카드 톤 3중으로 표현.

---

## 3. 비즈니스 규칙 (정책서 출처 명시)

### 3.1 QR 코드 인코딩 및 검증 (확정 사항)

**QR 인코딩 형식**: `SITE_CD|CHKPT_CD` 복합 (파이프 구분 또는 동등한 JSON 직렬화 — §7.1 질문 사항 참조)

QR 스캔 후 검증 절차:
1. QR 페이로드를 파싱해 `siteCd`, `chkptCd` 추출
2. `siteCd`가 **현재 인증 사용자의 소속 사업장(JWT의 SITE_CD)과 일치**하는지 검증
3. `(CMPNY_CD, SITE_CD, CHKPT_CD)`로 `TB_CHKPT_TYPE_MGMT`에서 `USE_YN='Y'`인 레코드 존재 확인
4. 모두 통과 시 화면 B 전환. 하나라도 실패 시 케이스 7 노출

**미등록 QR 안내 문구** (케이스 7): "등록되지 않은 QR 코드예요. 소속 사업장({사업장명})의 점검 개소가 아니거나 관리자 등록 전 코드입니다. 다시 스캔해 주세요."

타 사업장 QR 차단의 근거: **공통 정책서 §7~§8 조직·권한 정책** — 사용자는 본인 소속 사업장만 사용 가능 (메인 홈 작업지시서 §3.1과 정합).

### 3.2 점검 항목 로딩 정책

- `TB_CHKPT_INSPECT_ITEM`에서 `(CMPNY_CD, CHKLST_TYPE)` 일치 + `USE_YN='Y'` 항목을 `SORT_IDX ASC`로 조회
- `CHKLST_TYPE`은 화면 A에서 식별한 체크포인트의 `CHKLST_TYPE` 값을 사용
- 점검 항목은 **회사 + 체크리스트 타입 단위로 공유**됨 (테이블에 SITE_CD 없음). 즉 같은 CHKLST_TYPE이면 모든 사업장의 체크포인트가 동일한 항목을 사용
- 항목의 `STR_DATE`(varchar 6, YYYYMM)는 항목 유효 시작월로 추정됨. **[정책 확정 필요]** §7.2 참조 — 현재 작업일자(`YYYYMM`)가 `STR_DATE` 이상인 항목만 노출할지, 모든 USE_YN=Y 항목을 노출할지

### 3.3 양호/불량 응답 정책

- 응답 타입은 `SYS009` 코드 마스터에 정의된 **2종만** (`Y`=양호, `N`=불량)
- "해당 없음" / "확인 불가" 등의 회피 응답 코드는 도입하지 않음
- 모든 항목 응답은 **필수**. 미답 1건이라도 있으면 저장 버튼 disabled
- 응답 시점 UI 갱신:
  - 양호 선택 → 카드 ok 톤 전환, 진행 카운터 +1, 양호 분포 +1
  - 불량 선택 → 카드 bad 톤 전환 + 사유·사진 입력 영역 펼침, 진행 카운터 +1, 불량 분포 +1
  - 양호 → 불량 전환 시: 입력 영역 펼침, 사유 빈 값으로 리셋
  - 불량 → 양호 전환 시: 입력 영역 접힘, 사유·사진 입력값 폐기 (확인 모달 없음 — UX 단순화)

### 3.4 불량 입력 영역 (사유 + 사진)

#### 3.4.1 사유 (`ANSWER_DESC`)

- 라벨: "불량 사유" (좌측 `*` 필수 마커)
- 텍스트 영역: `min-height: 72px`, `resize: none`, 최대 500자 (`ANSWER_DESC text` 컬럼)
- placeholder: "발견한 불량 상태를 구체적으로 입력해 주세요."
- 글자 수 카운터: `{현재} / 500` 우측 정렬, tertiary 톤
- **필수 입력**: 사유가 비어 있으면 (공백 trim 후 0자) 해당 항목은 미답으로 간주 — 저장 버튼 활성 조건에서 제외
- 시안 케이스 3·4에서 카운터를 `52 / 500`으로 표기

#### 3.4.2 사진 (`FILE_MGMT_CD`)

- 라벨: "현장 사진" + 보조 "(선택 · 1장)"
- **DB 스키마 제약**: `TB_CHKPT_INSPECT_ANSWER.FILE_MGMT_CD`가 varchar(50) 단일 컬럼 → **항목당 1장만** 첨부 가능. 다중 첨부 필요 시 §6.3 스키마 보강 작업 선행 필요
- 미첨부 상태: 72×72 점선 보더 박스 + 카메라 아이콘 + "사진 추가" 텍스트
- 첨부 상태: 72×72 미리보기 + 우상단 X(제거 버튼) + 우측 메타 (`파일명` strong / `방금 촬영 · 2.4MB`)
- 탭 시 시스템 카메라 또는 갤러리 선택 시트 (네이티브 권한 사용). 촬영 후 즉시 업로드 → `FILE_MGMT_CD` 반환받아 메모리 보관
- 업로드는 항목 응답 시점에 처리 (저장 시점에 일괄 업로드하지 않음 — UX 즉시성)
- 제거 버튼 탭 시 즉시 메모리에서 폐기. 서버 측 파일 삭제는 본 작업 범위 외 (TTL 정책으로 미참조 파일 정리)

### 3.5 진행 카운터 표시 규칙

- 좌측: "응답 N / M" (N=응답 수, M=전 항목 수) — N은 primary text 굵게, M은 secondary
- 우측: "양호 X · 불량 Y" — X는 `--color-primary` 굵게, Y는 `--color-warning-text` 굵게
- 진행 바: `(N / M) × 100%`, `--color-primary` 채움, h4px, radius 2

### 3.6 푸터 [저장] 활성 조건 및 동작

- 활성 조건: 모든 점검 항목이 응답 완료 + 모든 불량 항목의 사유가 1자 이상
- 비활성 상태:
  - 화면 진입 직후 (응답 0): "저장" 텍스트만, 회색 disabled
  - 일부 응답 (0 < N < M): "저장 (X개 남음)" 텍스트, 회색 disabled. X = M − N
  - 불량 사유 미입력: "저장" 텍스트, 회색 disabled (사유 미입력 항목 수 표시는 시안 v1 범위 외 — planner 검토)
- 활성 시: Primary Green + `device-floppy` 아이콘 + "저장"
- 클릭 시: 단일 POST 호출로 모든 항목 응답을 UPSERT (§5.2). 성공 시 화면 C로 전환

### 3.7 재점검 정책 (확정 사항)

- 동일 사용자가 동일 체크포인트의 동일 항목을 같은 날(`WORK_DATE`) 다시 점검하면 **조용한 UPSERT** 수행
- `TB_CHKPT_INSPECT_ANSWER`의 PK가 `(CMPNY_CD, SITE_CD, CHKPT_CD, INSPECT_ITEM_CD, WORK_DATE)`이므로 같은 키에 INSERT 시 UPDATE로 처리
- 사용자에게 **별도 안내·확인 모달 없음**. 이전 응답값과 다르더라도 그대로 덮어쓰기
- 별도 사용자 컬럼이 없으므로 동일 날짜에 **다른 사용자가 같은 항목을 응답**하면 마지막 점검자의 응답으로 덮어쓰기됨 (§6.4 검토 항목 참조)

### 3.8 카메라 권한 거부 정책

- 화면 A 진입 시 시스템 카메라 권한 미허용 상태이면 권한 요청 다이얼로그 노출
- 사용자가 거부 또는 시스템 설정에서 차단되어 있으면 케이스 6 노출
- 안내 문구: "QR 코드를 인식하려면 카메라 사용 권한을 허용해 주세요. 설정 앱에서 PRAFTA 권한을 켤 수 있어요."
- 푸터: [취소](Secondary) + [설정으로 이동](Primary)
- [설정으로 이동] 클릭 시 OS 설정 앱의 본 앱 권한 화면으로 deep link

### 3.9 화면 이탈·취소 정책

- 화면 A의 X 버튼 또는 시스템 백 → 메인 홈으로 복귀 (확인 모달 없음, 입력 없음)
- 화면 B의 ← 버튼 또는 시스템 백:
  - 응답 0건이면 즉시 메인 홈 복귀
  - 응답 1건 이상이면 확인 모달: "입력 중인 점검 응답이 사라져요. 계속할까요?" + [취소] / [나가기](Danger)
  - **[정책 확정 필요]** §7.3 — 확인 모달 패턴이 정책서 §13.3 확인 모달 표준과 정합한지

### 3.10 점검자 식별

- `TB_CHKPT_INSPECT_ANSWER`에 별도 `INSPECTOR_USER_CD` 컬럼 없음 → **`INSERT_NO` 컬럼에 점검자 사용자 ID 저장** (현재 스키마 그대로 활용)
- JWT의 USER_CD를 INSERT_NO에 INSERT/UPDATE 시 기록
- **[정책 확정 필요]** §7.4 — 점검자를 명시적 컬럼으로 분리할지(예: `INSPECTOR_USER_CD`) §6.4 참조

---

## 4. 화면별 상세 명세

### 4.1 공통 UI 요소

- 본문 배경: `--color-bg` (#F9FAFB)
- 카드: `--color-surface` + 1px `--color-border` + radius 12px (점검 항목 카드) / 14px (컨텍스트 카드)
- 푸터: `--color-surface` + 상단 1px `--color-border` + padding `10 16 calc(10 + env(safe-area-inset-bottom))` + flex gap 8
- 터치 영역 최소 44×44px (단, 카메라 닫기 56×56)
- 아이콘: 인라인 SVG (Tabler 또는 prafta 기존 공통 아이콘 — CDN 의존 금지)

### 4.2 화면 A: QR 스캐너

#### 4.2.1 상태바 & 헤더
- 상태바: 다크 (`#000` 배경 + 흰 글씨)
- 헤더: `rgba(0,0,0,0.4)` 반투명 + 흰 글씨 + 카메라 위에 absolute 배치
- 좌측 X: 44×44, 클릭 시 메인 홈 복귀
- 중앙: "QR 스캔" 17/700 흰색

#### 4.2.2 카메라 뷰파인더
- 전체 영역 `#0A0A0A` 배경 (실제 구현은 카메라 스트림)
- 시안에서는 `radial-gradient`로 카메라 명도 시뮬레이션

#### 4.2.3 가이드 프레임
- 240×240 정사각형, 화면 중앙에서 상단으로 약간 올려 배치 (`top: 45%`)
- 4모서리에만 코너 마커 (32×32, 3px 흰색 보더, radius 6)
- 가운데 영역은 비워둠 (실제 카메라 화면이 보이도록)

#### 4.2.4 스캔 라인
- 좌우 8px 마진, height 2px, `--color-primary` 배경 + box-shadow 12px primary glow
- 위·아래 무한 왕복 애니메이션 (구현 시 `@keyframes`로 처리)

#### 4.2.5 안내 텍스트
- 메인: "점검 개소의 QR 코드를 스캔해 주세요" — 14/600 흰색, 하단 88px
- 보조: "QR이 사각형 안에 들어오면 자동으로 인식돼요" — 12 / `rgba(255,255,255,0.7)`, 하단 64px

#### 4.2.6 하단 닫기 버튼
- 56×56 원형, 중앙 정렬, `rgba(255,255,255,0.12)` 배경 + 1px `rgba(255,255,255,0.25)` 보더
- 안에 X 아이콘 22px 흰색
- 클릭 시 메인 홈 복귀

### 4.3 화면 B: 점검 응답

#### 4.3.1 헤더
- 라이트 헤더 (`--color-surface` + 하단 1px `--color-border-light`)
- 좌측 ← 백 버튼 / 중앙 "안전점검" / 우측 빈 영역

#### 4.3.2 컨텍스트 카드 (`SafetyInspectContextCard`)
- padding 14 16, radius 14, 1px `--color-border`
- 좌측 아이콘 박스 36×36, radius 10, `--color-primary-tint` + `--color-primary`
  - **[검토]** 체크리스트 타입에 따라 아이콘 매핑이 필요할 수도 있음. 현재 시안은 지게차 케이스라 `truck` 아이콘. 다른 타입(분전반·비상구 등) 추가 시 매핑 테이블 필요 — §6.1 참조
- 우측: 체크포인트명 (15/700 `--color-text-primary`) + 메타 (11 `--color-text-secondary`)
  - 메타 포맷: `{체크리스트 타입명} · {사업장명} · {작업일자 YYYY-MM-DD}`
  - **[정책 확정 필요]** §7.5 — 체크리스트 타입명의 출처. 현재 DB에 별도 마스터 미확인 (§6.2 참조)

#### 4.3.3 진행 카운터 (`SafetyInspectProgress`)
- padding 0 4, font 12 / `--color-text-secondary`
- 좌측: `응답 N / M`. N은 strong, M은 secondary
- 우측: `양호 X · 불량 Y`. X는 `--color-primary` 700, Y는 `--color-warning-text` 700
- 그 아래 진행 바: h4, radius 2, 배경 `--color-border-light`, 채움 `--color-primary`

#### 4.3.4 점검 항목 카드 (`SafetyInspectItem`)
- 카드 패딩 12 14, 1px 보더, radius 12. 상태별 톤 §2.3 참조
- 상단 헤더: 번호 칩 22×22 원형 + 항목명 (14/600)
- 토글: 2분할 grid, gap 8, 각 항목 h40, radius 10, 1.5px 보더
  - 좌측 "양호" / 우측 "불량"
  - 활성 시 배경 tint + 보더 컬러 + 텍스트 컬러 모두 해당 톤
- 불량 선택 시 토글 아래 1px dashed `#FECACA` 디바이더 + 입력 영역 펼침

#### 4.3.5 불량 입력 영역 (`SafetyInspectBadForm`)
- 디바이더 위 12px / 아래 12px 간격, gap 10
- 사유 라벨: 12/600 `#991B1B` + 좌측 `*` `--color-danger`
- 텍스트 영역: 1px `--color-border`, radius 8, padding 10 12, font 13, `min-height: 72`, `resize: none`, font-family inherit
- 글자 수 카운터: 11px `--color-text-tertiary`, 우측 정렬
- 사진 라벨: 12/600 `#991B1B` + 보조 "(선택 · 1장)" `--color-text-secondary` 11/500
- 사진 박스 row: gap 8
  - 미첨부: 72×72 점선 1.5px `--color-border` + radius 10 + 카메라 아이콘 22 + "사진 추가" 11/500 `--color-text-secondary`
  - 첨부: 72×72 미리보기 + 우상단 20×20 원형 X (rgba(0,0,0,0.6) 배경) + 우측 메타 11px (`파일명` 600 primary / `방금 촬영 · 2.4MB`)

#### 4.3.6 푸터 [저장]
- h48, radius 10, 14/600
- disabled: `--color-border-light` 배경 + `--color-text-tertiary` 텍스트 + 1px `--color-border` + `cursor: not-allowed`
- 활성: `--color-primary` + 흰 글씨 + 좌측 `device-floppy` 아이콘
- 응답 진행 중 텍스트: "저장 (N개 남음)" (시안 케이스 3)

### 4.4 화면 C: 저장 완료

#### 4.4.1 본문
- flex column center, padding 32 24, gap 8
- 성공 아이콘: 72×72 원형, `--color-primary-tint` 배경 + `--color-primary` 글씨, `circle-check` 32px
- 타이틀: 17/700 `--color-text-primary` "점검을 저장했어요"
- 요약: 13 / `--color-text-secondary`, 1.5 line-height, max-width 280
  - 포맷: `{체크포인트명} · 양호 N건 · 불량 M건` 줄바꿈 `발견한 불량은 관리자에게 자동 전달돼요`

#### 4.4.2 푸터
- 2분할 버튼 gap 8
- 좌: [다른 개소 점검] Secondary (`--color-surface` + 1.5px `--color-primary` 보더 + `--color-primary` 텍스트). 클릭 시 화면 A로 복귀
- 우: [메인으로] Primary. 클릭 시 메인 홈으로

### 4.5 폴백 화면

#### 4.5.1 카메라 권한 거부 (`SafetyCameraPermissionView`, 케이스 6)
- 라이트 헤더 ("QR 스캔" + 좌측 X)
- 본문 중앙: 72×72 원형 `--color-warning-tint` + `--color-warning` 글씨 + `camera-off` 32px
- 타이틀: "카메라 접근 권한이 필요해요" 17/700
- 안내: §3.8 안내 문구. 13 / `--color-text-secondary`
- 푸터: [취소](Secondary) + [설정으로 이동](Primary, `settings` 아이콘)

#### 4.5.2 미등록 QR 토스트 (`SafetyQrErrorOverlay`, 케이스 7)
- 카메라 뷰파인더 위 absolute 배치, 하단 100 + safe-area
- 좌우 16 마진, `rgba(239,68,68,0.95)` 배경, radius 12, padding 14 16
- 좌측 `alert-circle` 20px 흰색
- 우측: 타이틀 14/700 흰색 "등록되지 않은 QR 코드예요" + 보조 12 `rgba(255,255,255,0.85)` (§3.1 안내 문구)
- 하단 닫기 버튼이 `refresh` 아이콘으로 교체 (스캔 재시도 의미)
- 가이드 프레임은 opacity 0.5로 흐려짐

---

## 5. 백엔드 API 명세

> 백엔드 작업은 별도 백엔드 개발자(외부, 사람)가 담당. 아래 명세는 단일 출처로서 frontend의 API 호출 기준이 된다.

### 5.0 사용 테이블 (필수)

| 테이블 | 사용 컬럼 (read) | 사용 컬럼 (write) | 용도 |
|---|---|---|---|
| `TB_CHKPT_TYPE_MGMT` | `CMPNY_CD`, `SITE_CD`, `CHKLST_TYPE`, `CHKPT_CD`, `CHKPT_NM`, `CHKPT_DESC`, `USE_YN` | — | QR 검증 + 체크포인트 메타 |
| `TB_CHKPT_INSPECT_ITEM` | `CMPNY_CD`, `CHKLST_TYPE`, `INSPECT_ITEM_CD`, `INSPECT_ITEM_SUBJ`, `SORT_IDX`, `STR_DATE`, `USE_YN` | — | 점검 항목 리스트 |
| `TB_CHKPT_INSPECT_ANSWER` | (UPSERT 시 PK 충돌 확인용 SELECT) | `CMPNY_CD`, `SITE_CD`, `CHKPT_CD`, `INSPECT_ITEM_CD`, `WORK_DATE`, `INSPECT_ANSWER_TYPE`, `ANSWER_DESC`, `FILE_MGMT_CD`, `INSERT_NO`, `INSERT_DATE`, `UPDATE_NO`, `UPDATE_DATE` | 점검 응답 저장/UPSERT |
| `TB_FILE_INFO` | `FILE_MGMT_CD`, `FILE_NM`, `FILE_PATH`, `FILE_TYPE`, `FILE_EXT` | (사진 업로드 시) `CMPNY_CD`, `FILE_MGMT_CD`, `FILE_NM`, `FILE_TYPE`, `FILE_PATH`, `FILE_EXT`, `INSERT_NO`, `INSERT_DATE` | 불량 사진 첨부 |
| `TB_SYST_VAL_D` | `SYST_VAL_CD='SYS009'`, `SYST_VAL_D_CD`, `SYST_VAL_D_NM` | — | 양호/불량 코드 확인 (`Y`/`N`) |

JWT의 USER_CD → INSERT_NO / UPDATE_NO 컬럼에 기록.

### 5.1 GET /api/app/safety/inspect/by-qr

**용도**: QR 인식 후 체크포인트 검증 + 점검 항목 리스트 조회 (화면 A → 화면 B 전환 시 단일 호출)

**요청 파라미터** (Query):
- `siteCd`: string (QR에서 추출)
- `chkptCd`: string (QR에서 추출)

**처리 로직**:
1. JWT의 USER_CD → CMPNY_CD, 사용자 SITE_CD 추출
2. 파라미터 `siteCd` ≠ 사용자 SITE_CD → HTTP 403 `{ "error": "SITE_MISMATCH", "userSiteName": "중곡사업장" }`
3. `(CMPNY_CD, siteCd, chkptCd)`로 `TB_CHKPT_TYPE_MGMT` 조회, USE_YN='Y' 아니면 → HTTP 404 `{ "error": "CHKPT_NOT_FOUND" }`
4. 체크포인트의 CHKLST_TYPE으로 `TB_CHKPT_INSPECT_ITEM` 조회 (USE_YN='Y', SORT_IDX ASC)
5. **[정책 확정 필요]** §7.2 — STR_DATE 필터 적용 여부

**응답 구조** (HTTP 200):
```json
{
  "checkpoint": {
    "cmpnyCd": "001",
    "siteCd": "00001",
    "siteName": "중곡사업장",
    "chklstType": "00001",
    "chklstTypeName": "지게차 일상점검",
    "chkptCd": "000006",
    "chkptName": "중곡사업장_지게차",
    "chkptDesc": "테스트11"
  },
  "items": [
    {
      "inspectItemCd": "DCHK_1_00001",
      "inspectItemSubj": "지게차 비상등 점등상태 여부",
      "sortIdx": 1,
      "previousAnswer": null
    }
  ],
  "workDate": "20260528",
  "hasTodayInspection": false
}
```

**필드 설명**:
- `chklstTypeName`: §6.2 마스터 테이블 확정 후 매핑. 현재 미정의 상태이면 CHKLST_TYPE 그대로 또는 하드코딩
- `previousAnswer`: 같은 항목에 대해 오늘 이미 저장된 응답이 있으면 `{ "answerType": "Y", "answerDesc": null, "fileMgmtCd": null }`. 재점검 시 화면에 프리필 — **[검토]** v1 시안에는 프리필 미반영. planner 검토
- `hasTodayInspection`: 어떤 항목이라도 오늘자 응답이 있으면 true (재점검임을 백엔드 로깅 용도)

**JSON 키 ↔ DB 컬럼 매핑**:
- `siteCd` ↔ `SITE_CD`, `chkptCd` ↔ `CHKPT_CD`, `chklstType` ↔ `CHKLST_TYPE`
- `inspectItemCd` ↔ `INSPECT_ITEM_CD`, `inspectItemSubj` ↔ `INSPECT_ITEM_SUBJ`, `sortIdx` ↔ `SORT_IDX`
- `workDate` ↔ `WORK_DATE` (varchar 8 YYYYMMDD)

### 5.2 POST /api/app/safety/inspect/save

**용도**: 점검 응답 일괄 저장 (UPSERT). 화면 B의 [저장] 버튼 클릭 시 단일 호출.

**요청 Body**:
```json
{
  "siteCd": "00001",
  "chkptCd": "000006",
  "workDate": "20260528",
  "answers": [
    { "inspectItemCd": "DCHK_1_00001", "answerType": "Y", "answerDesc": null, "fileMgmtCd": null },
    { "inspectItemCd": "DCHK_1_00002", "answerType": "N", "answerDesc": "우측 후륜 타이어 측면에 약 5cm 가량 갈라짐 발견. 공기압도 정상보다 낮음.", "fileMgmtCd": "F20260528001" },
    { "inspectItemCd": "DCHK_1_00003", "answerType": "Y", "answerDesc": null, "fileMgmtCd": null }
  ]
}
```

**처리 로직**:
1. JWT에서 CMPNY_CD, USER_CD 추출
2. siteCd 사용자 사업장 검증 (§5.1과 동일 401/403 처리)
3. `(CMPNY_CD, siteCd, chkptCd)` 체크포인트 USE_YN='Y' 검증
4. 각 answer에 대해:
   - `answerType` ∈ {`Y`, `N`} 검증 (SYS009)
   - `answerType='N'`이면 `answerDesc` 비어있지 않은지 검증 (실패 시 HTTP 400 `INVALID_BAD_REASON`)
5. PK 충돌 시 UPDATE, 신규는 INSERT (UPSERT — `INSERT ... ON DUPLICATE KEY UPDATE`)
6. INSERT 시 INSERT_NO = USER_CD, INSERT_DATE = NOW. UPDATE 시 UPDATE_NO = USER_CD, UPDATE_DATE = NOW
7. answers 배열의 항목 수가 해당 체크포인트의 USE_YN=Y 항목 수와 일치하는지 검증 (불일치 시 HTTP 400 — 전 항목 응답 정책 §3.3 서버 측 강제)

**응답 구조** (HTTP 200):
```json
{
  "savedCount": 6,
  "okCount": 5,
  "badCount": 1,
  "workDate": "20260528",
  "chkptName": "중곡사업장_지게차"
}
```

화면 C에서 `chkptName`, `okCount`, `badCount`를 그대로 노출.

**JSON 키 ↔ DB 컬럼 매핑**:
- `answerType` ↔ `INSPECT_ANSWER_TYPE`, `answerDesc` ↔ `ANSWER_DESC`, `fileMgmtCd` ↔ `FILE_MGMT_CD`

### 5.3 POST /api/app/files/upload (기존 endpoint 재사용 검토)

**용도**: 불량 사진 1장 업로드 후 `FILE_MGMT_CD` 반환. 사용자가 사진 첨부 시점에 호출 (저장 시점 X).

**요청**: `multipart/form-data`
- `file`: binary
- `domain`: "SAFETY_INSPECT" (파일 분류 태그)

**응답** (HTTP 200):
```json
{
  "fileMgmtCd": "F20260528001",
  "fileName": "tire_damage.jpg",
  "fileSize": 2516582,
  "fileExt": "jpg",
  "filePath": "/files/safety/2026/05/F20260528001.jpg"
}
```

**[planner 확인 필요]** §6.6 — `TB_FILE_INFO` 기반의 업로드 endpoint가 기존에 정의되어 있는지, 신규 작성인지 확인. 기존 endpoint가 있으면 그대로 사용.

### 5.4 정책서 참조 출처

- 정확한 정책서 섹션 매핑은 planner가 `.claude/context/policies/` 하위 산업안전 정책서(현재 미분할)를 매핑하여 확정
- 본 작업 요청서에 명시된 비즈니스 규칙(§3)은 시안 작성 시 검토된 내용이며, planner가 정책서 출처를 작업 로그 "상세 설명"에 명시해야 한다
- 주요 매핑 예상:
  - §3.1 → 공통 정책서 §7~§8 (소속 사업장 권한)
  - §3.2 → 산업안전 정책서 §5.4 (안전점검 흐름)
  - §3.3, §3.4 → 산업안전 정책서 §5.4 점검 응답 정책 (미분할)
  - §3.7 → 산업안전 정책서 §5.4 점검 이력 정책 (재점검·UPSERT)
  - §3.8 → 공통 정책서 §13 UI/UX (권한 거부 처리)

---

## 6. DB 구조 검토 및 추가 작업 필요 사항

### 6.1 §5.0 사용 테이블의 현 스키마 충족 검증

- 현재 4개 테이블 모두 본 화면 요구를 **기본적으로 충족**한다
- 단, 다음 항목이 한계로 작용하므로 §6.2~§6.5 보강 검토 필요

### 6.2 체크리스트 타입(`CHKLST_TYPE`) 마스터 테이블 부재

- `TB_CHKPT_TYPE_MGMT.CHKLST_TYPE`(varchar 10)이 어떤 값을 갖는지 정의하는 마스터 테이블이 현재 DB에서 확인되지 않음 (검색 결과: `SYS***` / `tb_syst_val_*` 패턴에 CHKLST 관련 코드 없음)
- 현재 DB에는 `00001` 1종만 사용 중. 시안에서 "지게차 일상점검"으로 표기한 부분도 추정값
- **[planner 확인 필요]** §7.6 — 다음 중 하나 결정:
  1. `tb_syst_val_d`에 `SYST_VAL_CD='CHKLST_TYPE'` 마스터 추가
  2. 별도 `TB_CHKLST_TYPE_MGMT` 테이블 신설
  3. 기존에 누락된 마스터가 다른 위치에 있는지 재확인

### 6.3 첨부 사진 다중화 (현재 단일 컬럼 제약)

- `TB_CHKPT_INSPECT_ANSWER.FILE_MGMT_CD`가 단일 varchar(50) → 항목당 1장만 가능
- 산업안전 현장 점검에서 다중 각도 사진이 필요한 경우가 있을 수 있음
- **[planner 확인 필요]** §7.7 — 다음 중 하나 결정:
  1. 본 작업은 1장 유지, 추후 별도 작업으로 보강
  2. `TB_CHKPT_INSPECT_ANSWER_FILE` 연결 테이블 신설 (PK: ANSWER PK + FILE_MGMT_CD, SEQ)
- v1 시안은 1장 제약을 그대로 반영

### 6.4 점검자 컬럼 명시화 검토

- `TB_CHKPT_INSPECT_ANSWER`에 별도 `INSPECTOR_USER_CD` 컬럼 없음 → `INSERT_NO` 활용
- 단점: 같은 날 다른 사용자가 같은 항목을 응답하면 마지막 점검자만 남음 (PK가 사용자를 포함하지 않음)
- **[planner 확인 필요]** §7.8 — 다음 중 하나 결정:
  1. PK에 INSPECTOR_USER_CD 추가 (사용자별 응답 보존, 관리자 화면에서 평균/대표값 산출)
  2. INSERT_NO 그대로 활용 (마지막 점검자만 유효)
  3. 별도 `TB_CHKPT_INSPECT_ANSWER_HIST` 히스토리 테이블로 모든 응답 보존

본 작업의 v1 시안은 현 스키마(2번)를 가정하고 작성됨.

### 6.5 STR_DATE 유효 시작월의 의미 확인

- `TB_CHKPT_INSPECT_ITEM.STR_DATE` (varchar 6 = YYYYMM, NOT NULL) — 항목 유효 시작월로 추정
- 종료월(`END_DATE`) 컬럼은 없음 → 항목을 끝내려면 `USE_YN='N'` 처리만 가능
- **[planner 확인 필요]** §7.9 — STR_DATE 의미와 운영 정책 확인:
  - 항목의 첫 노출 월? 그 이전 점검에는 항목 미노출?
  - 또는 단순 등록 시점 메타? 노출 필터 미적용?

### 6.6 파일 업로드 endpoint 기존 정의 확인

- `TB_FILE_INFO` 기반 업로드 endpoint가 기존 시스템에 이미 정의되어 있는지 확인 필요
- 있으면 §5.3 재사용, 없으면 신규 작성

### 6.7 메인 홈 작업지시서 §3.4.2 갱신 필요

- `prafta-request-home.md` §3.4.2에는 "안전점검 시작" 진입 시 "**점검 개소 리스트 화면 (별도 작업)**"으로 명시되어 있음
- 본 작업은 그 대신 **QR 스캐너로 직행**하는 흐름으로 확정
- planner가 메인 홈 작업지시서를 갱신하여 진입 동선을 "본 작업의 QR 스캐너"로 변경

---

## 7. 정책서 출처 확인 필요 사항 (planner 작업)

| # | 항목 | 본 시안의 가정 | 정책서 확인 필요 |
|---|---|---|---|
| 7.1 | QR 인코딩 형식 | `SITE_CD|CHKPT_CD` 파이프 구분 (또는 동등 JSON) | 산업안전 정책서 §5.4 또는 QR 관리 정책에서 명시 여부. 백오피스 QR 발급·인쇄 작업과의 정합 |
| 7.2 | STR_DATE 기준 항목 필터 | 모든 USE_YN=Y 항목 노출 (STR_DATE 필터 미적용) | §6.5 결과에 따라 결정. 작업일자 ≥ STR_DATE만 노출할지 |
| 7.3 | 화면 B 이탈 시 확인 모달 | 응답 1건 이상 입력 시 확인 모달 | 공통 정책서 §13.3 확인 모달 표준 정합 |
| 7.4 | 점검자 컬럼 분리 여부 | INSERT_NO 그대로 사용 (분리 컬럼 없음) | §6.4 검토 결과 |
| 7.5 | 체크리스트 타입명 표시 | "지게차 일상점검" 등 시안 더미 | §6.2 마스터 테이블 확정 후 매핑 |
| 7.6 | 사진 다중 첨부 | 1장 제약 (현 스키마) | §6.3 검토 결과 |
| 7.7 | 재점검 UX 정책 | 조용한 UPSERT, 사용자 알림 없음 | 정책서에 명시 또는 본 작업으로 정책 신설 |
| 7.8 | 응답 회피 코드 (해당없음 등) | 도입 없음 (Y/N 2종만) | 정책서 §5.4 응답 정책 확인 |
| 7.9 | 다른 사업장 QR 차단 안내 문구 | "소속 사업장(중곡사업장)의 점검 개소가 아니거나..." | 공통 정책서 §7~§8 권한 차단 안내 워딩 정합 |
| 7.10 | 일시 저장(임시 보관) 도입 | 본 작업 범위 외 (전체 응답 후 일괄 저장만) | 정책서 §5.4 일시 저장 정책 유무 |

---

## 8. 비기능 요구사항

### 8.1 디자인 시스템 준수
- 색상/폰트/간격은 모두 CSS 변수만 사용 (하드코딩 금지)
- 공통 컴포넌트 우선 사용 (`src/components/common/`)
- 모바일 first (360~414px 기준 폭)
- 터치 영역 최소 44×44px (단, 카메라 닫기 56×56)
- 아이콘은 인라인 SVG (CDN 의존 금지)

### 8.2 성능
- 화면 B 진입 시 단일 API 호출 (`GET /api/app/safety/inspect/by-qr`)
- 화면 B 저장 시 단일 API 호출 (`POST /api/app/safety/inspect/save`)
- 사진 업로드는 첨부 시점에 즉시 호출 (저장 지연 회피)
- QR 인식은 클라이언트 측 (예: `@zxing/library` 또는 네이티브 카메라 API). 서버 호출 최소화

### 8.3 접근성
- 색상으로만 정보 전달 금지: 양호/불량은 색 + 토글 위치 + 텍스트 + 번호 칩 색 4중 표현
- 텍스트 입력 필드 모두 라벨 명시. 필수 항목은 `*` 마커 + `aria-required="true"`
- 카메라 닫기·X·refresh 버튼 모두 `aria-label` 명시
- 진행 카운터: 스크린 리더로 "응답 4 of 6, 양호 3, 불량 1" 형태로 발화

### 8.4 에러 처리
- 네트워크 오류 / 401 / 403 / 500 등 표준 패턴 (prafta 기존 화면 패턴 따름)
- 저장 시 일부 항목 검증 실패 시 (예: 불량 사유 빈값) HTTP 400 + 에러 항목 ID 반환 → 해당 카드로 스크롤 + 경고 톤
- 사진 업로드 실패 시 인라인 토스트 "사진 업로드에 실패했어요. 다시 시도해 주세요."

### 8.5 카메라 권한
- 첫 진입 시 시스템 권한 요청 다이얼로그 노출
- 거부 시 케이스 6 (`SafetyCameraPermissionView`) 노출
- [설정으로 이동] 클릭 시 OS 설정 앱 deep link (`app-settings:` 등 플랫폼별 처리)

---

## 9. 화면 분해 (planner가 구체화)

본 작업은 다음과 같이 분해될 것으로 예상됨 (planner 최종 결정):

| 예상 작업 ID | 유형 | 담당 | 산출물 |
|---|---|---|---|
| PRAFTA-{N1} | backend | 백엔드 개발자 | §6 DB 구조 검토 결과 (체크리스트 타입 마스터·다중 첨부·점검자 컬럼 등 결정) |
| PRAFTA-{N2} | backend | 백엔드 개발자 | §5.1 GET /api/app/safety/inspect/by-qr endpoint |
| PRAFTA-{N3} | backend | 백엔드 개발자 | §5.2 POST /api/app/safety/inspect/save endpoint (UPSERT) |
| PRAFTA-{N4} | backend | 백엔드 개발자 | §5.3 사진 업로드 endpoint 확인·재사용 또는 신규 |
| PRAFTA-{N5} | frontend-screen | developer | `SafetyQrScanView.vue` (QR 스캐너 + 권한 폴백 + 미등록 토스트) |
| PRAFTA-{N6} | frontend-screen | developer | `SafetyInspectView.vue` (점검 응답 화면 상위 컨테이너 + 상태 관리 + 저장) |
| PRAFTA-{N7} | frontend-component | developer | `SafetyInspectContextCard.vue` |
| PRAFTA-{N8} | frontend-component | developer | `SafetyInspectProgress.vue` |
| PRAFTA-{N9} | frontend-component | developer | `SafetyInspectItem.vue` (양호/불량 토글) |
| PRAFTA-{N10} | frontend-component | developer | `SafetyInspectBadForm.vue` (사유 + 사진) |
| PRAFTA-{N11} | frontend-component | developer | `SafetyInspectSavedView.vue` (저장 완료) |
| PRAFTA-{N12} | frontend-component | developer | `SafetyCameraPermissionView.vue` (권한 거부 폴백) |
| PRAFTA-{N13} | frontend-component | developer | `SafetyQrErrorOverlay.vue` (미등록 QR 토스트) |
| PRAFTA-{N14} | docs | planner | 메인 홈 작업지시서 §3.4.2 갱신 (진입 동선 변경 — 점검 개소 리스트 → QR 스캐너) |

**선행 관계**:
- N1 (DB 구조 결정) → N2~N4 (API)
- N2~N4 (API) → N5, N6 (frontend-screen). N5는 N2의 QR 검증 API 의존
- N5, N6 골격 위에서 N7~N13 병렬 작업
- N14는 본 작업과 독립적으로 즉시 수행 가능

---

## 10. 결정 사항 요약 (시안 작성 시점)

본 시안은 사용자와의 협의를 거쳐 v1로 확정됨. 주요 결정 사항:

1. **QR 인코딩**: `SITE_CD|CHKPT_CD` 복합 (타 사업장 QR 차단을 시스템 레벨에서 보장)
2. **응답 정책**: 전 항목 필수. 미답 1건이라도 있으면 저장 disabled
3. **재점검 정책**: 조용한 UPSERT. 같은 날 같은 항목 재응답 시 별도 안내 없이 덮어쓰기
4. **점검 단일 진입점**: 점검 개소 리스트 화면 별도 노출하지 않음. QR 스캔만이 진입 동선
5. **사진 첨부**: 항목당 1장. DB 단일 컬럼 제약 그대로 반영 (다중화는 후속 작업)
6. **응답 회피값 없음**: 양호/불량 2종만. "해당 없음" 등 추가 코드 도입 안 함
7. **저장 단위**: 화면 단위 일괄 저장. 항목별 즉시 저장 없음. 일시 저장(임시 보관) 미도입
8. **시안 케이스**: 표준 7개 케이스 (스캔 / 인식·미답 / 진행·불량 펼침 / 전체 완료 / 저장 완료 / 권한 거부 / 미등록 QR)
9. **저장 완료 화면**: 안내문만 노출. 불량 항목 요약 카드는 제거 (시안 v1 → 컨펌 시 삭제)
10. **메인 홈 진입 동선 변경**: 기존 메인 홈 작업지시서 §3.4.2의 "점검 개소 리스트 화면(별도 작업)"을 본 작업의 QR 스캐너로 대체. 갱신 작업 N14로 분리

---

## 11. 산출물 체크리스트 (planner 작업 완료 시점)

- [ ] 본 작업 요청서를 정독, `.claude/context/policies/` 산업안전 정책서 매핑하여 정책서 출처 확정 (산업안전 정책서가 아직 분할 미완료일 경우 분할 작업 선행 검토)
- [ ] §3.1 QR 인코딩 형식이 정책서 §5.4 또는 QR 관리 정책에 명시되어 있는지 확인 (없으면 신설)
- [ ] §3.3 응답 회피 코드 미도입 결정이 정책서와 정합한지 확인
- [ ] §3.7 재점검 UPSERT 정책이 정책서 §5.4에 반영되어 있는지 확인 (없으면 신설 제안)
- [ ] §6 DB 구조 검토 항목 6종을 백엔드 개발자에게 확인 요청 → 답변 받아 §10 결정 사항 갱신
- [ ] §5 API 명세를 백엔드 작업 분해 시 그대로 상세 설명에 기재 (백엔드 개발자가 곧바로 착수 가능하도록)
- [ ] 화면 명세 UI-{순번} 작성 및 Notion "도메인 지식 베이스" 등록
- [ ] Vue 골격 7~9개 파일 작성 (script 영역은 `// TODO(developer):` 마커만, 비즈니스 로직 미작성)
- [ ] **메인 홈 작업지시서 §3.4.2 갱신** (본 작업 N14): "점검 개소 리스트 화면(별도 작업)" → "QR 스캐너 화면(본 작업)"
- [ ] 메인 홈 KPI(`안전 활동` 카드의 활성·차단 조건)와 본 화면 진입 가능 여부 일치 확인 (§3.4 차단 정책 정합)
- [ ] 사용자 승인 후 Notion "작업 로그"에 일괄 등록
- [ ] developer 에이전트가 후속 처리 가능한 상태로 전달

---

## 12. 첨부

- `prafta_safety_inspect_v1.html` — 7개 화면 케이스가 담긴 HTML 시안 (단일 페이지, 브라우저 열람용)
  - 케이스 1: QR 스캔 대기
  - 케이스 2: 인식 성공 · 전체 미답
  - 케이스 3: 답변 진행 · 불량 펼침
  - 케이스 4: 전체 완료 · 저장 활성
  - 케이스 5: 저장 완료
  - 케이스 6: 카메라 권한 거부
  - 케이스 7: 미등록 QR · 인식 실패
- 본 작업 요청서 정독 시 시안 HTML과 함께 참조할 것
- 선행 작업 요청서:
  - `../앱_사용자_메인/prafta-request-home.md` — 메인 홈 안전 활동 카드 진입 동선 (§3.4.2가 본 작업으로 대체됨)
- 참고:
  - `../앱_사용자_안전점검내역조회/prafta-request-my-inspections.md` — 본 작업으로 저장된 점검 이력을 사용자가 회고하는 후속 화면 (동일 도메인)
