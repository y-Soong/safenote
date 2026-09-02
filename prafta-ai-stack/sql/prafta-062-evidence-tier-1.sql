-- ============================================================================
-- prafta-062 [배포 A] 근거 층위(evidence tier) 도입 — pgvector(prafta_ai) 마이그레이션
--
-- 대상 DB : pgvector `prafta_ai` (개발·운영 양쪽 모두 적용 — 한쪽만 적용 금지)
-- 실행자  : 사용자 수동 실행 (Claude Code 는 이 파일을 실행하지 않는다)
-- ★적용 순서 강제: 이 DDL 을 개발·운영 양 DB 에 먼저 적용한 뒤 백엔드(PRAFTA-062-02)를
--   배포한다. 순서가 뒤집히면 SEARCH_SQL 이 존재하지 않는 컬럼(s.evidence_tier)을
--   SELECT 해 AI 검색이 전면 실패한다(도출·앱 답변 동시).
--
-- 층위 값 목록: LAW(법적 의무) | GUIDE(권고 지침) | STAT(고위험 통계)
--             | CASE(유사 재해) | REF(참고 자료)   — NULL = 미지정(배지 미표시)
-- 초기 매핑 근거: prafta-062-plan.md §2 D1 (2026-09-02 사용자 확정)
-- ============================================================================

-- ① 컬럼 추가 (재실행 안전 — IF NOT EXISTS)
ALTER TABLE tb_ai_corpus_source
    ADD COLUMN IF NOT EXISTS evidence_tier VARCHAR(20);

-- ② 초기 층위 매핑 (D1 확정 표 — 8개 기존 출처)
--    법령 6종(LAW_*)은 배포 B(S2) 적재 시 registry 의 evidence_tier=LAW 로 들어오므로
--    이 파일에서는 다루지 않는다.
UPDATE tb_ai_corpus_source SET evidence_tier = 'GUIDE', update_date = CURRENT_TIMESTAMP
 WHERE source_id = '15144147';   -- 기술지원규정(코샤가이드) — 공적 기술지침

UPDATE tb_ai_corpus_source SET evidence_tier = 'STAT', update_date = CURRENT_TIMESTAMP
 WHERE source_id = '15140383';   -- 산업재해 고위험요인(SIF) 아카이브 — 통계

UPDATE tb_ai_corpus_source SET evidence_tier = 'CASE', update_date = CURRENT_TIMESTAMP
 WHERE source_id IN (
     '15108262'   -- 건설안전사고사례
   , '15121001'   -- 국내재해사례 게시판 정보 조회서비스
   , '15069200'   -- 화학 사고 정보
   , '15053339'   -- 사고사례 정보조회(시설물)
   , '15121008'   -- 국내재해사례 첨부파일
 );

UPDATE tb_ai_corpus_source SET evidence_tier = 'REF', update_date = CURRENT_TIMESTAMP
 WHERE source_id = '15140227';   -- 주요 기인물별 유해위험요인 및 감소대책(한국남동발전) — 참고 자료(D1=(b))

-- ③ 사후 검증
-- 3-1. 층위 분포 확인 (기대: GUIDE 1 / STAT 1 / CASE 5 / REF 1 / 미지정 0)
SELECT COALESCE(evidence_tier, '(미지정)') AS evidence_tier
     , COUNT(*)                            AS source_cnt
  FROM tb_ai_corpus_source
 GROUP BY COALESCE(evidence_tier, '(미지정)')
 ORDER BY 1;

-- 3-2. 출처별 매핑 결과 전수 확인 (8행 전부 층위가 채워졌는지)
SELECT source_id
     , source_name
     , evidence_tier
  FROM tb_ai_corpus_source
 ORDER BY source_id
 LIMIT 50;

-- 3-3. 허용 외 값 검사 (기대: 0건)
SELECT source_id, evidence_tier
  FROM tb_ai_corpus_source
 WHERE evidence_tier IS NOT NULL
   AND evidence_tier NOT IN ('LAW', 'GUIDE', 'STAT', 'CASE', 'REF')
 LIMIT 50;
