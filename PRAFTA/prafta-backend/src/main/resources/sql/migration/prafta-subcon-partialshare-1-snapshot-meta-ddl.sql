-- ============================================================================
-- PRAFTA-SUBCON-PARTIALSHARE-1 — 스냅샷 헤더 "마감분만 = 부분 포함 필터" 메타 컬럼 확장
-- 작성일: 2026-07-22
-- 적용 환경: MySQL 8.0.42
-- 출처: 작업지시서_하도급-근태공유-마감게이트-개선.plan.md §PS-02 (D-1~D-4 설계 결정, 2026-07-22)
-- 선행: prafta-subcon-t3-1-share-ddl.sql(tb_cmpny_share_snapshot — 이미 DB 반영됨. 그 파일은 수정하지 않는다)
--
-- 배경: "마감분만"(CLOSED_ONLY_YN='Y') 옵션이 승인 차단 게이트에서 (월×부서) 커버리지 부분 포함
--       필터로 재정의된다(D-1/D-2). 스냅샷이 실제로 일부만 포함됐는지 표시하고, 제외 내역(부서명·월
--       단위까지 — PII 금지)을 기록하기 위한 컬럼 2개를 추가한다.
--
-- 신규 컬럼:
--   CLOSED_PARTIAL_YN — 'Y':커버리지 필터로 일부 제외됨(자체 필터 또는 릴레이 병합) / 'N':필터 결과
--                        전체 포함 또는 미마감 포함 옵션(필터 미적용) / NULL:구본(메타 없음 — 전체
--                        포함 간주, D-4). 신규 생성 스냅샷은 반드시 'Y'/'N' 중 하나를 채운다(NULL 금지
--                        — NULL 은 구본 전용 값).
--   COVERAGE_META      — 마감 커버리지 요약 JSON(월·부서명 단위까지만 — 성명/USER_CD/사번 등 개인
--                        식별 정보 절대 금지, 공통 §11). CLOSED_ONLY_YN='N' 이거나 RISK/NEARMISS
--                        유형이면 NULL(가이드 대상 아님). 스키마(계약 — 백/프론트 공용):
--                          {
--                            "closedOnly": "Y",
--                            "months": [
--                              { "ym": "202606", "status": "FULL" },
--                              { "ym": "202607", "status": "PARTIAL",
--                                "excludedDeptNms": ["부서1", "부서2"], "orphanUnclosedYn": "Y" }
--                            ],
--                            "relayPartialIncludedYn": "N"
--                          }
--                        status: FULL(그 월 제외 행 0건) / PARTIAL(일부 행 제외) / NONE(그 월 포함 행
--                        0건). excludedDeptNms 는 실제 제외된 행의 부서명(최대 20개 + 초과 시 "외 N개
--                        부서" 항목 1개). 무부서/고아 행 제외는 orphanUnclosedYn:"Y" 로 표기(부서명
--                        대신). relayPartialIncludedYn: 릴레이로 묶은 하위 스냅샷 중 부분 포함이
--                        있었는지(D-3 병합 표식) — 하위 스냅샷의 months/부서명 상세는 승계하지 않는다
--                        (relabel 원칙 — 하위 조직 구조 비노출).
--
-- 구본 backfill 없음(D-4 — NULL=전체 포함 간주는 표시 레이어 규칙. 데이터 마이그레이션 불필요).
--
-- 배포 게이트: 본 DDL 을 코드 배포보다 먼저 적용한다(신규 컬럼 nullable 이라 선적용은 무해하다.
--   반대로 코드를 먼저 배포하면 insertSnapshot 이 존재하지 않는 컬럼을 참조해 즉시 장애가 난다 —
--   T3 OT_TYPE 선례와 동일한 실패 패턴).
--
-- 적용 전 확인(운영 적용 직전 권장 — 0건이어야 함, 이미 반영된 환경이면 아래 ALTER 는 skip):
--   SHOW COLUMNS FROM tb_cmpny_share_snapshot LIKE 'CLOSED_%';
--
-- 멱등성 주의: 이미 반영된 환경에서 재실행하면 1060(Duplicate column name) 에러가 난다.
--   위 확인 쿼리가 1건 이상이면 이 파일의 ALTER 문을 건너뛸 것.
--
-- 운영 적용: 사용자 수동(read-only MCP 로는 DDL 불가). 본 파일은 작성만, DB 직접 적용 금지.
-- ============================================================================

ALTER TABLE `tb_cmpny_share_snapshot`
    ADD COLUMN `CLOSED_PARTIAL_YN` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL
        COMMENT '마감분만 부분 포함 여부[Y:커버리지 필터로 일부 제외됨(자체 또는 릴레이 병합)/N:필터 결과 전체 포함 또는 미마감 포함 옵션/NULL:구본(메타 없음 — 전체 포함 간주, D-4)]'
        AFTER `UNCLOSED_INCLUDED_YN`,
    ADD COLUMN `COVERAGE_META` json DEFAULT NULL
        COMMENT '마감 커버리지 요약(JSON — 월·부서명 단위까지만, PII 금지). NULL:구본'
        AFTER `CLOSED_PARTIAL_YN`;

-- ============================================================================
-- 롤백 (필요 시 수동 실행)
-- ----------------------------------------------------------------------------
-- ALTER TABLE `tb_cmpny_share_snapshot` DROP COLUMN `COVERAGE_META`;
-- ALTER TABLE `tb_cmpny_share_snapshot` DROP COLUMN `CLOSED_PARTIAL_YN`;
-- ============================================================================
