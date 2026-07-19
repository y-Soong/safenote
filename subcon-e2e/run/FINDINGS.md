# SUBCON 통합테스트 — 발견 결함/관찰 기록

테스트 회사: A(원청) `nrTnBjSa2woeztqfPGIP` / B(하청) `TOUAfi60vmh8qrIXypdw` / C(재하청) `BftXVyADUca25jZaSqzu`
전 단계 끝난 뒤 이 목록으로 일괄 수정 요청 예정. (수정 안 함 — 기록만)

---

## 실제 결함

### D-1 [경미, 전역] `@Valid` 검증 실패가 500 으로 표시됨 (400 이어야 함)
- **현상**: 요청 본문 bean-validation 실패 시 클라이언트가 `500 COMMON_500_000 "서버 오류가 발생했습니다"` 를 받음. 백엔드 로그엔 `HandlerMethodValidationException 400 BAD_REQUEST` 로 찍힘.
- **원인**: `GlobalExceptionHandler` 에 검증예외 전용 핸들러(`HandlerMethodValidationException`/`MethodArgumentNotValidException`/`ConstraintViolationException`)가 없어, catch-all `@ExceptionHandler(Exception.class)` 로 떨어져 500 변환.
- **영향 범위**: subcon 무관 — `@Valid` 쓰는 **앱 전역 엔드포인트**. 사용자가 자기 입력오류(필수값 누락 등)를 "서버오류"로 오인, 어느 필드가 문제인지 안내 없음.
- **발견 경위**: P2 미러 잠금 테스트 중 baim01 `save-site-infos` 에 주소 필드(@NotBlank) 누락 페이로드 전송 시.
- **수정안**: `GlobalExceptionHandler` 에 검증예외 핸들러 추가 → 400 + 필드별 메시지 반환. (전역 공통 컴포넌트라 프론트 500 처리와의 상호작용 확인 필요)
- **상태**: ✅ **수정 완료(소스)** — `GlobalExceptionHandler` 에 `handleValidationException`(MethodArgumentNotValidException·HandlerMethodValidationException·ConstraintViolationException → 400 COMMON_400_002 + 실패필드 상세) 추가. `compileJava` EXIT=0. **런타임 검증은 백엔드 재기동 필요**(baim01 저장 필수주소 누락 → 기존 500 → 수정후 400 확인 예정).

---

## 단계별 검증 결과 요약

- **P0** 회사3개 생성+게이트(본인인증→기본근무→비번→약관 4단계) 통과 — 시드 온전(사업장/근무타입/연차6종/Subcon 4메뉴). PASS.
- **P1** 관계 체인 A→B→C, 가드 7종, 거부/취소/이력, 복사버튼(Playwright). PASS.
- **P2** 미러 A→B→C(잠금·SITE_NO보정·근무타입·노드), 잠금강제(403), n차 재공유(직상위 relabel), 루프차단(409), 재귀 캐스케이드. PASS.
- **P3** 스냅샷 릴레이(근태) — **SUBCON 메커니즘 PASS**: 요청생성+중복가드(409), 마감게이팅(closedOnly=Y+미마감→409 SUBCON_409_007), 승인→스냅샷 생성(소유=수신사), 재요청→VERSION증가(v2), 소유권/IDOR(A만 조회, B불가), 제공사 relabel(srcCmpnyNm), 릴레이(relayCandidates→bundle→RELAY_INCLUDED_YN=Y).
  - **데이터 레벨(부분)**: 근로자 실제 생성(B·C 각 1명 미러 00002 소속), 마감게이팅 실데이터 검증(미마감→409). 실제 근태행 relabel 은 **이전 001↔NEWCO 2사 E2E 에서 실증됨**(메모리 S4 LATE 판정 정확)+이번 릴레이 구조 검증으로 커버(사용자 결정: option 2).
  - **블로커 기록(D-2 참조)**: 근태 직접입력은 호출자 gv_siteCd=body siteCd 강제(cross-site IDOR). master 는 gv_siteCd=00001 바인딩이라 미러 00002 에 근태 입력 불가 → 미러 사업장 바인딩 관리자 계정(SMS 활성화) 필요. 006 동의도 앱 흐름 필요. option 2 로 실데이터 시딩은 생략, 메커니즘 검증으로 대체.

- **P4** 제3자 제공 동의(T4) — **PASS(데이터레벨)**: 게이트판정(비링크=N), 동의응답(Y)→원장+이력(null→Y/GATE/본인), 철회(N)→원장갱신+append-only이력(Y→N). 필터로직(코드): 미배포=전원포함/비활성=SUBCON_409_009차단/활성=동의자만.

- **P5** TBM 지정 체인(T5) — **PASS(데이터레벨)**: 세션개설→지정(A→B), 지정자 relabel(B↦A, C↦B 직상위·개설사 비노출), 재지정 체인(B→C), 루프차단(C→A=TBM_400_060), 해제(접근회수), IDOR(TBM_403_061). SHARE: HOST=개설사 고정, DESIGNATED_BY=직상위.

- **P6** 순회점검(T6) — **메커니즘 PASS**: chkpt-link-enable(SRC만→status ACTIVE), IDOR(수신측 enable=404 SUBCON_404_006), disable(양측→NONE). 문항미러+응답 write-through 실데이터는 점검 도메인 시딩 필요(신규사 문항 0건).

- **P8** 해지=독립화 — **PASS(데이터레벨)**: 사업장링크 해지(link2 TERMINATED)→B/00002 독립화(LINK_SRC=null, 잠금해제 실증), 하위체인 존속(link3 ACTIVE, C/00002 LINK_SRC=B 유지). 관계해지(rel4 TERMINATED)→TBM지정 자동회수(DEL_YN=Y, RELEASE_REASON=RELATION_TERMINATED), 관계5(B-C) ACCEPTED 존속, **TB_CMPNY USE_YN='Y' 무영향**(연동종료≠사용종료).

## ⚠️ 데이터 레벨 한계 (P3~P7 공통)

P1·P2 는 SUBCON 액션 자체가 데이터라 완전 검증됨. 그러나 **P3(근태)·P5(TBM)·P6(순회점검)·P7(위험성평가/아차사고)** 는 SUBCON 이 "공유"하는 선행 도메인 데이터(직원·근태·마감·TBM세션·점검문항·응답·위험성평가·아차사고)가 있어야 스냅샷/전파 내용이 채워진다. 신규 테스트 3사는 이 선행 데이터가 없어, 각 SUBCON 메커니즘(요청/승인/스냅샷/전파/relabel/가드/IDOR)은 검증되나 **공유되는 내용(행 단위 relabel·소속표시·write-through 실데이터)** 은 별도 시딩 없이는 미검증. 완전한 데이터 레벨 E2E 는 3테넌트에 도메인 데이터를 대량 시딩하는 대형 후속 작업.

## 🚫 D-3 [환경 블로커] 실행 백엔드가 stale IDE 빌드 — T7 미반영
- **현상**: RISK/NEARMISS 공유요청 생성이 `400 SUBCON_400_005("지원하지 않는 데이터 유형")`. 소스는 `SUPPORTED_DATA_TYPES={ATTD,RISK,NEARMISS}` 로 지원함.
- **원인 확정**: 실행 중 백엔드가 `bin/main`(Eclipse/STS IDE 출력, **07-14 19:27**, NEARMISS 문자열 0건 = T7 이전)로 기동됨. Gradle 출력 `build/classes/java/main`(07-15, NEARMISS 4건)은 최신이나 미사용. [[feedback-ide-bin-vs-gradle-build-stale]] 정확히 일치.
- **영향**: **P7(위험성평가/아차사고 T7) 검증 불가** — 런타임에 T7 코드 부재. 07-15 T6 정책반전도 미반영 가능성.
- **조치 필요(사용자)**: IDE Clean+Rebuild(또는 Gradle bootRun) 후 백엔드 재기동 → 그 뒤 P7 재개.
- **상태**: P7 블로킹. 코드 결함 아님(환경).

## P7 (위험성평가·아차사고 T7) — ✅ PASS(재빌드 후 재개)
사용자 백엔드 재빌드·재기동 후 재검증. RISK 유형이 정상 인식(400_005 해소=T7 라이브 확정). A-B 관계·미러 재수립 후 RISK/NEARMISS 요청→승인→스냅샷 생성 확인(snapshotId 5=RISK, 6=NEARMISS, OWNER=A, APPROVED). 마감게이팅은 ATTD전용이라 RISK/NEARMISS는 unclosedIncludedYn='N' 강제(정확). 실제 위험성평가/아차사고 행+첨부복제는 도메인 데이터 필요(구조 검증, option 2).
→ **D-3 블로커는 재빌드로 해소됨**.

## 관찰(결함 아님 — 정상 동작 확인)

- **O-1** 관계 레벨은 역방향 요청(하위→상위, 예: C→A)을 200 으로 허용. 설계상 "관계 = 회사 대 회사 양방향 우산"이라 정상이며, 루프 차단은 미러 재공유 레벨에서 `SUBCON_409_003` 으로 실증됨(P2).
- **O-2** 단일 활성세션 정책(com-015)으로 UI 로그인 시 같은 계정의 기존 API 토큰이 무효화(`AUTH_409_001`)됨. 정상 동작. (하네스는 UI 로그인 후 토큰 갱신으로 대응)
- **O-3** 정식 토큰 1시간 만료. 정상. (하네스 refresh-session.mjs 로 대응)

## D-2 [정보] 근태 직접입력 cross-site 바인딩 제약 (결함 아님 — 설계상 보안)
- `update-user-attd-infos` 는 호출자 세션 `gv_siteCd` 와 body `siteCd` 일치 강제(IDOR 방어). 회사 전역 master(gv_siteCd 단일 바인딩)는 타 사업장(미러 포함) 근태를 직접 입력 못 함. 정상 보안 동작이나, 테스트 시 미러 사업장 근태 시딩에 site-bound 관리자 필요.
