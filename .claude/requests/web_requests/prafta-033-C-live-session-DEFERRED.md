# prafta-033-C — TBM 실시간 진행 (W-07~11) 【🔴 보류 / DEFERRED】

> ════════════════════════════════════════════════════════════════
> **⛔ 이 작업지시서는 보류 상태다. developer는 착수하지 않는다.**
> 활성화 조건: **모바일 앱(M-01~13) 백엔드/프론트가 선행 또는 병행**될 때.
> 사용자 결정(2026-05-27, plan §1 D4): "C는 다음 단계로 보류, 앱쪽 이후에 진행."
> ════════════════════════════════════════════════════════════════

> 마스터 플랜: `prafta-033-plan.md` (C 보류 경계 §5)
> To-Be 사양: `ref/prafta-033/05_03_LIVE_SESSION.md`, `03_BACKEND_SPEC_WEB.md §6`, `02_BACKEND_SPEC_COMMON.md §7`(SSE)
> 선행: A(테이블 생성 완료), B(세션 OPENED까지), **+ 앱 백엔드/프론트(04, 06_xx)**

---

## 0. 왜 보류인가 (경계 근거)

C는 다음 이유로 웹 단독 완결이 불가능하다:

1. **출결 데이터 생성 주체가 모바일** — 정규직 입실/서명은 앱(M-03~08). 웹 단독으로는 출결이 거의 생성되지 않는다.
2. **SSE 동기화 구독자가 근로자 앱** — 슬라이드 broadcast 수신 대상이 모바일 참여자.
3. **일용직 QR 입실(W-09)조차 실시간 콘솔 내부 행위** — 세션 IN_PROGRESS 흐름과 묶여 있음.

따라서 C는 **앱 백엔드(04_BACKEND_SPEC_APP)·앱 프론트(06_01~03)와 함께** 진행해야 검증된다.

신규 모듈(활성화 시): 백엔드 `com.prafta.web.tbm.tbm03` + SSE 컴포넌트, 프론트 `src/views/tbm/Tbm_03.vue`.

---

## 1. C가 다룰 범위 (활성화 시)

| 화면 | 내용 |
|---|---|
| W-07 | TBM 진행 콘솔(메인): 교육 시작, 슬라이드 제어, 콘텐츠 표시 |
| W-08 | 입실자 모니터링(W-07 우측): 참여자 명단 + 이상신호 |
| W-09 | 일용직 QR 스캔 모달: 입실/종료(서명) |
| W-10 | 종료 처리: 미종료자 일괄/개별 분기 |
| W-11 | 강제 종료 모달(개별): 이수/미이수 + 사유 필수 |

## 2. C가 구현할 백엔드 (활성화 시) — 상태 쓰기 경로

> A에서 테이블은 이미 생성됨. C는 **INSERT/UPDATE + SSE**를 구현한다.

| 메서드/경로(안) | 용도 | 상태 전이 / 쓰기 |
|---|---|---|
| `POST /webApi/tbm03/sessions/start` | 교육 시작 | OPENED→IN_PROGRESS, STARTED_AT, STATE UPSERT, SSE `session.started` |
| `POST /webApi/tbm03/sessions/sync-state` | 슬라이드/콘텐츠 변경 | `TB_TBM_SESSION_STATE` UPSERT, SSE `session.state` |
| `GET /webApi/tbm03/sessions/attendances-live` | 진행중 참여자 명단 | 조회(이상신호 집계) |
| `POST /webApi/tbm03/sessions/qr-enter` | 일용직 QR 입실 | QR→USER_CD, **만료검증(WORK_EXPIRE_DATE/슬롯 FIXED_YN)**, `TB_TBM_ATTENDANCE` INSERT(DAILY/MANAGER_QR_SCAN) + EVENT |
| `POST /webApi/tbm03/sessions/qr-exit` | 일용직 QR 종료(서명) | ATTENDANCE UPDATE(EXIT_*, 서명파일) + EVENT |
| `POST /webApi/tbm03/sessions/end` | 교육 종료 | IN_PROGRESS→COMPLETED, ENDED_AT, (옵션)미종료자 일괄 강제종료, SSE `session.ended` |
| `POST /webApi/tbm03/sessions/force-end` | 미종료자 개별 강제종료 | ATTENDANCE UPDATE(MANAGER_FORCED, 사유, 이수상태) + EVENT |
| `GET /webApi/tbm03/sessions/sse` | 관리자 SSE 구독 | `attendance.updated` 등 |

### 2.1 SSE 인프라 (02_BACKEND_SPEC_COMMON §7)
- `TbmSseEmitterManager`(세션별 SseEmitter in-memory Map, subscribe/broadcast/remove).
- 트랜잭션 커밋 후(`afterCommit`) broadcast — 롤백 시 오발송 방지.
- 인증: SSE 연결 시 JWT 검증(헤더/쿼리). EventSource 헤더 제약 → `fetch-event-source` 등 검토(plan/사양 05_03 §6.1).
- MVP 단일 인스턴스 가정(다중 인스턴스는 Redis Pub/Sub 향후).

### 2.2 핵심 비즈니스 규칙 (00_OVERVIEW 결정사항)
- **입실 차단**: OPENED만 입실, IN_PROGRESS 이후 차단(00 §5.2).
- **비번**: 입실≠종료, 상태 기반 차단(IN_PROGRESS 후 입실비번 불가 / COMPLETED 후 종료비번 불가), 실패 무한재시도+로깅(`TB_TBM_PWD_FAIL`).
- **GPS 검증**: AUTO만 반경 검증(Haversine), MANUAL/DISABLED는 스킵(00 §4.4).
- **일용직 만료**: `WORK_EXPIRE_DATE` 경과 시 QR 입실 차단(고정 슬롯 `FIXED_YN='Y'`는 만료 없음, 00 §4.3).
- **이탈 자동판정 없음**: 백그라운드/GPS이탈은 이벤트 로그만, 관리자가 종료 시 판단(00 §5.3).
- **강제 종료**: 사유 필수, 이수/미이수 관리자 지정, 서명 없음(관리자 책임 분리, 00 §5.4).
- **동기화 Level 1.5**: 슬라이드/콘텐츠 단위, 영상 정밀동기화 X, 풀스크린 개별 토글(00 §5.5).

## 3. C가 구현할 프론트 (활성화 시)
- W-07 진행 콘솔(3영역), SSE 클라이언트(`useSseConnection.js`), 슬라이드 제어, QR 스캐너(html5-qrcode 등), 서명 캔버스(signature_pad 등), 종료/강제종료 모달.
- 상세 컴포넌트 구조: 05_03 §6 참조.

## 4. 활성화 시 사전 확인 (plan §8 + 05_03 §9)
1. 앱 백엔드/프론트 진행 상태(출결 생성·SSE 구독 주체 확보)
2. SSE 클라이언트 인증 방식(JWT 전달)
3. QR 스캐너 / 서명 캔버스 라이브러리
4. 풀스크린 API, 카메라 권한 폴백
5. 위험성평가 표시명(B에서 확정된 방식 재사용)

## 5. C 활성화 절차
1. 본 파일명에서 `-DEFERRED` 제거 또는 상단 보류 배너 해제.
2. 앱 작업지시서(별도, 본 prafta-033 웹 묶음 범위 밖)와 의존 정렬.
3. A의 출결/이벤트/상태 테이블 스키마 재확인(변경 없었는지).
4. B의 세션이 OPENED→IN_PROGRESS 전이 가능하도록 start 연동.

---

**상태**: 🔴 보류. A·B·D 완료 및 모바일 앱 착수 후 본 문서를 활성화한다.
