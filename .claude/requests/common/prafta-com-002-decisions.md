# prafta-com-002 확정 결정사항 (YJ 승인 완료)

작업: FCM 공용 PUSH 전송 워커(consumer). 작업지시서 단일출처: `refs/prafta-com-002/01_작업지시서_FCM전송워커.md`. 본 문서는 착수 전 사용자 확정 결정만 기록.

## A. 선행조치 (YJ 완료 확인됨)
- A-1/A-2: FCM 서비스 계정 키 발급 완료. 주입 환경변수 = **`FIREBASE_CREDENTIALS_PATH`** (JSON 파일 경로). 런타임 주입 완료.
- A-3(앱 토큰 등록): 별개 영역. 미완이어도 워커 구현 진행(실제 도달 테스트만 보류).

## B. 운영 정책 (확정)
- **B-1 주기/게이트**: `fixedDelay 30초`. 게이트 `prafta.push.worker.enabled=${PUSH_WORKER_ENABLED:false}` (기본 비활성, 수동 ON).
- **B-2 재시도/무효토큰**: 최대 **3회** 재시도 후 `FAILED` 고정. 무효 토큰(FCM UNREGISTERED/INVALID_ARGUMENT) → **soft-delete (옵션 A)**.
  - ⚠️ `tb_user_device`에 soft-delete 컬럼 없음 확인됨 → **신규 컬럼 `DEL_YN char(1) NOT NULL DEFAULT 'N'` 추가(마이그레이션)**. 프로젝트 DEL_YN 컨벤션 일치.
  - 무효 토큰 시 해당 디바이스 행 `DEL_YN='Y'` 마킹. 토큰 조회는 `DEL_YN='N' AND PUSH_TOKEN IS NOT NULL`만.
  - 마이그 파일 작성·**운영 미적용**(수동 적용). 적용 전엔 soft-delete UPDATE가 DEL_YN 컬럼 부재로 실패하므로, 마이그 선적용 필요(보고).
- **B-3 동시성**: **단일 인스턴스** 전제. @Scheduled fixedDelay는 비중첩이므로 동시성 위험 낮음 → 단순 SELECT 후 처리. 단 크래시 복구·중복발송 방지 위해 PENDING→SENDING claim 전이만 가볍게 둠.
- **토큰 0건**: `FAILED` + `ERROR_MSG="NO_DEVICE_TOKEN"`.

## 범위 (작업지시서 §0 준수)
- consumer만. 생산자(INSERT) 무수정. tb_noti_outbox 신규 컬럼 없음.
- 비범위: iOS APNs, 앱 토큰 등록, 알림센터/읽음 UI, opt-out, 통계.

## 처리 워크플로우
planner → developer → qa → security. 메인 세션 Notion 대행.
