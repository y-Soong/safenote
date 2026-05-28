# TBM 작업지시서 작성 진행 상황

> 이 문서는 작업지시서 작성 작업의 진행 상황을 추적합니다.
> 새 세션에서 작업을 이어가려면 이 문서를 먼저 읽고 시작하세요.

---

## 작업 목적

Prafta v2 TBM(Tool Box Meeting) 모듈의 Claude Code 작업지시서를 작성합니다.
설계 토론은 모두 완료되었으며, 이 작업은 결정된 사양을 문서로 옮기는 작업입니다.

## 전체 문서 구조

```
/mnt/user-data/outputs/prafta-v2/tbm/
├── _PROGRESS.md                            # ← 이 문서 (작업 진행 추적)
├── 00_OVERVIEW.md                          # 전체 그림, 결정사항 동결
├── 01_DDL_SPEC.md                          # 테이블 DDL + ERD
├── 02_BACKEND_SPEC_COMMON.md               # 공통 백엔드 사양
├── 03_BACKEND_SPEC_WEB.md                  # 웹 전용 API
├── 04_BACKEND_SPEC_APP.md                  # 앱 전용 API
├── web/
│   ├── 05_01_CONTENT_LIBRARY.md            # W-01~03
│   ├── 05_02_SESSION_MANAGEMENT.md         # W-04~06
│   ├── 05_03_LIVE_SESSION.md               # W-07~11
│   └── 05_04_HISTORY.md                    # W-12~15
└── app/
    ├── 06_01_WORKER_FLOW.md                # M-01~10
    ├── 06_02_MANAGER_FLOW.md               # M-11~13
    └── 06_03_PUSH_NOTIFICATION.md          # 푸시
```

## 진행 상황 체크리스트

### Phase 1: 기반 문서
- [x] `_PROGRESS.md` (이 문서)
- [x] `00_OVERVIEW.md` — 전체 그림, 결정사항, 용어집 (GPS 정책 보강 + 기존 구현 매핑 안내 포함)
- [x] `01_DDL_SPEC.md` — 테이블 DDL

### Phase 2: 백엔드 사양
- [x] `02_BACKEND_SPEC_COMMON.md` — DTO 플로우, MyBatis 패턴, 예외, 권한, SSE
- [x] `03_BACKEND_SPEC_WEB.md` — 웹 API 전체
- [x] `04_BACKEND_SPEC_APP.md` — 앱 API 전체

### Phase 3: 웹 화면 (Vue)
- [ ] `web/05_01_CONTENT_LIBRARY.md` — 콘텐츠 라이브러리
- [ ] `web/05_02_SESSION_MANAGEMENT.md` — TBM 세션 개설/수정
- [ ] `web/05_03_LIVE_SESSION.md` — 실시간 TBM 진행
- [ ] `web/05_04_HISTORY.md` — 이력/출결 관리

### Phase 4: 앱 화면 (Flutter + Vue WebApp)
- [ ] `app/06_01_WORKER_FLOW.md` — 근로자 TBM 참여
- [ ] `app/06_02_MANAGER_FLOW.md` — 모바일 관리자 + 일용직 QR
- [ ] `app/06_03_PUSH_NOTIFICATION.md` — 푸시 알림

## 핵심 결정사항 요약 (작업지시서 작성 시 반드시 반영)

### 도메인 결정사항
1. **TBM은 자기규율 예방체계의 한 축** (순회점검 → 위험성평가 → TBM → 사고관리)
2. **TBM ≠ 정기교육**, 단 향후 정기교육 시간 인정 환산은 별도 모듈
3. **위험성평가 연계는 약한 강제 (B+)** — 옵션이되 UI에서 유도 안내
4. **콘텐츠 스코프** — 회사 공통(SITE_CD NULL, master/safe만 등록) + 사업장(SITE_CD 지정)
5. **MVP는 TBM만**, 정기교육·채용 시 교육·특별교육 등은 향후 확장
6. **확장 가능 구조** — `EDU_TYPE_CD = 'TBM'` 고정값으로 시작, 향후 타입 추가

### 인증 결정사항
1. **정규직**: 본인 디바이스 + GPS 100m(개설 시 설정, default 100) + 랜덤 비번 + 서명
2. **일용직(QR 사용자)**: 관리자가 QR 스캔 + 관리자 폰에서 일용직이 직접 서명
3. **일용직 만료**: 당일 자정 자동 만료, 슬롯 고정 시 만료 없음 (고정 해제 시 그날 자정 만료)
4. **만료된 QR**: 스캔 자체 차단 (TBM 입실 시 검증)
5. **비번 유효시간**: 없음, 단 상태 기반 차단 (IN_PROGRESS 이후 입실 불가, 종료 후엔 종료 비번도 입력 불가)
6. **비번 실패**: 무한 재시도 + 로깅

### 흐름 결정사항
1. **세션 상태 전이**: DRAFT → OPENED → IN_PROGRESS → COMPLETED (+ CANCELLED)
2. **시작 후 입실 차단** (IN_PROGRESS 이후), 지각자는 별도 회차로 처리
3. **동일 교육 다회 개설 허용** (제약 없음)
4. **이탈 자동 판정 X** — 이벤트는 모두 로깅하되 자동 처리 안 함
5. **종료 미처리자**: 관리자 강제 종료 + 이수/미이수 관리자 지정 + 사유 필수 입력
6. **콘텐츠 동기화 수준**: Level 1.5 (슬라이드/콘텐츠 단위, 영상 정밀 동기화 X)
7. **동기화 기술**: SSE (Server-Sent Events)
8. **첨부자료 풀스크린**: 개별 토글, 우측 상단 X로 닫기

### UI/콘텐츠 결정사항
1. **교육 내용**: 리치 텍스트 (HTML 저장), 필수 입력 (없으면 개설 차단)
2. **첨부자료**: 이미지/영상(YouTube/자체)/PDF, 콘텐츠 라이브러리에서 선택
3. **콘텐츠별 설명 텍스트**: 콘텐츠 자체 + 세션별 override 가능

### 권한 결정사항
1. **기본 권한**: master(최고), safe(안전 최고), hr(근태 최고), 999999(일반)
2. **회사별 커스텀 권한** 시스템 활용
3. **TBM 개설/관리**: safe + 회사별 커스텀 권한
4. **회사 공통 콘텐츠 등록**: master, safe만
5. **사업장 콘텐츠 등록**: safe + 사업장 관리자

### 데이터 결정사항
1. **출결 통합 테이블** — 정규직(USER_ID) / 일용직(DAILY_USER_NO) 한 테이블
2. **ENTRY_TYPE_CD**로 인증 경로 구분 (SELF_DEVICE / MANAGER_QR_SCAN)
3. **이벤트 로그** — `TB_EDU_ATTENDANCE_EVENT` 분리
4. **실시간 상태** — `TB_EDU_SESSION_STATE` 분리 (메인 세션 테이블 update 도배 방지)

### 책임 분리 철학 (Prafta 일관 원칙)
- **시스템은 기록만, 판단은 사람**
- **자동 처리하지 않음** — 관리자가 위임받은 영역
- **부정 사용·실수는 관리자 책임**으로 명확히 분리
- 시스템은 **정보 제공**(이상 신호 플래그)만, **자동 판정 X**

### MVP 범위 결정
- **포함**: TBM 세션, 콘텐츠 라이브러리, 출결, 일용직 QR, 위험성평가 연계, 이력 조회
- **제외 (Phase 2)**: 정기교육 시간 환산, 다국어, AI 콘텐츠 생성, 사고 관리 연계, 순회점검 연계, AI 퀴즈, 위험성평가 자동 생성

## Prafta 환경 컨텍스트 (작업지시서 작성 시 반영)

### 기술 스택
- **백엔드**: Java + Spring Boot + MyBatis (XML mapper)
- **DB**: MySQL 8.0.42
- **웹 프론트**: Vue.js (VITE_API_BASE_URL)
- **모바일**: Flutter + Vue WebApp 번들 (APK)
- **IDE**: IntelliJ IDEA / Android Studio (Claude Code JetBrains plugin)

### 컨벤션 (작업지시서에 반드시 명시)
- 컬럼명: 대문자 + 언더스코어 (`UPPER_SNAKE_CASE`)
- 멀티테넌시: 모든 테이블에 `CMPNY_CD` 스코핑, 사업장 분리는 `SITE_CD`
- 감사 컬럼: `INSERT_NO`, `INSERT_DATE`, `UPDATE_NO`, `UPDATE_DATE`
- 플래그: `XXX_YN` 패턴 ('Y'/'N')
- 복합 PK 적극 활용
- MyBatis: `ON DUPLICATE KEY UPDATE ... AS NEW` row alias 문법
- SQL 코멘트: `/* MapperName.methodName */`
- DTO 플로우: request → param → query → result → response
- Param/Query/Result: Java record
- Request/Response: Lombok DTO
- 예외 처리: `ApiException.appendf(CommonErrorCode.COMMON_400_001, ...)`
- 정적 팩토리: `from()` 메서드 패턴
- 레이어: Controller → Service → ServiceImpl → Mapper → XML

### 외부 의존 (Claude Code가 이미 파악)
- 사용자 테이블, 위험성평가 테이블, 일용직 테이블, 슬롯 테이블 등은 Claude Code 로컬에 이미 구현되어 있음
- 작업지시서에서는 일반적 이름(`TB_USER`, `TB_RISK_ASSESSMENT`, `TB_DAILY_USER` 등)으로 참조하되, 실제 컬럼명/구조는 Claude Code가 로컬에서 확인하도록 안내
- 헷갈리면 사용자에게 질문하도록 명시

## 세션 인계 시 시작 방법

새 세션에서 이어갈 때:

1. `_PROGRESS.md`(이 문서) 읽기
2. 체크리스트에서 다음 작업할 문서 확인
3. 필요시 이미 작성된 이전 문서 참고 (`00_OVERVIEW.md`, `01_DDL_SPEC.md` 등)
4. 사용자에게 "다음 문서 [문서명] 작성 진행할까요?" 확인 후 진행
5. 작성 완료 시마다 이 문서의 체크리스트 업데이트

## 작성 원칙 (모든 문서 공통)

1. **개발자 친화** — Claude Code가 의문 갖지 않게 명시적
2. **흐름 단위 자체완결** — 다른 문서 참조는 명시적으로
3. **메모리상 컨벤션 반영** — 매번 다시 설명 안 해도 되게
4. **결정사항 우선** — 위의 핵심 결정사항을 반영
5. **누락 방지** — "이 부분은 다른 문서에서 개발" 명시
6. **이미 구현된 부분 명시** — Claude Code가 로컬 확인하도록 안내

---

**최종 업데이트**: Phase 1 + Phase 2 완료 (백엔드 사양 모두)
**다음 작업**: `web/05_01_CONTENT_LIBRARY.md` (웹 화면 시작)
