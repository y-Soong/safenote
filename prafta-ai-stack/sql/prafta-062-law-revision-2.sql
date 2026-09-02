-- ============================================================================
-- prafta-062 [배포 B] 법령 개정 재적재 절차 — pgvector(prafta_ai) 운영 SQL (문서용)
--
-- 대상 DB : pgvector `prafta_ai` (개발·운영 양쪽 동일 절차 — 한쪽만 적용 금지)
-- 실행자  : 사용자 수동 실행 (Claude Code 는 이 파일을 실행하지 않는다)
--
-- 배경 (plan 062-05 9):
--   chunk_id = {SOURCE_ID}_{sha256(SOURCE_ID|locator|CONTENT)[:16]} — content 해시라
--   개정된 조문을 재적재하면 **새 행으로 들어오고 구 행이 그대로 남는다**(둘 다 USE_YN='Y').
--   같은 로케이터의 유효 행이 2건이 되면 검색에 신·구 조문이 동시에 뽑혀 오인용 위험.
--
-- 실행 시점 (매 개정 재적재 후):
--   ① 22_collect_law_articles.py --check-revision 으로 개정 감지
--   ② registry(sources.xlsx) api_fixed_params 의 MST 갱신(사람)
--   ③ 22_collect_law_articles.py → 10_preprocess → 30_embed_load 재실행(변경 조문만 신규 행)
--   ④ 본 파일 §1 UPDATE 실행 → §2 검증 SELECT 로 확인
-- ============================================================================

-- ① 같은 (source_id, source_locator) 에 신행이 있는 구 행을 USE_YN='N' 으로 내린다.
--    "구 행" 판정 = insert_date 가 더 이른 행(동시각 tie 는 chunk_id 사전순 뒤가 신행).
--    ★법령(LAW_*)만 대상 — 다른 소스의 청킹 체계에 손대지 않는다.
UPDATE tb_ai_corpus_chunk c
   SET use_yn = 'N'
     , update_date = CURRENT_TIMESTAMP
 WHERE c.source_id LIKE 'LAW/_%' ESCAPE '/'
   AND c.use_yn = 'Y'
   AND EXISTS (
         SELECT 1
           FROM tb_ai_corpus_chunk n
          WHERE n.source_id = c.source_id
            AND n.source_locator = c.source_locator
            AND n.use_yn = 'Y'
            AND (n.insert_date > c.insert_date
                 OR (n.insert_date = c.insert_date AND n.chunk_id > c.chunk_id))
       );

-- ② 사후 검증
-- 2-1. 로케이터당 유효 행 1건 검증 (기대: 0건 — 1건이라도 나오면 ①을 재확인)
SELECT source_id
     , source_locator
     , COUNT(*) AS active_cnt
  FROM tb_ai_corpus_chunk
 WHERE source_id LIKE 'LAW/_%' ESCAPE '/'
   AND use_yn = 'Y'
 GROUP BY source_id, source_locator
HAVING COUNT(*) > 1
 ORDER BY source_id, source_locator
 LIMIT 100;

-- 2-2. 법령별 유효 청크 분포 확인
--      (2026-09-02 초도 적재 기준 기대치: LAW_OSHA 184 / LAW_OSHA_ENF 125 / LAW_OSHA_RULE 252
--       / LAW_OSHA_STD 669 / LAW_SAPA 16 / LAW_SAPA_ENF 13 — 합계 1,259.
--       개정 재적재 후에는 조문 증감분만큼 달라질 수 있다.)
SELECT source_id
     , COUNT(*) FILTER (WHERE use_yn = 'Y') AS active_cnt
     , COUNT(*) FILTER (WHERE use_yn = 'N') AS retired_cnt
  FROM tb_ai_corpus_chunk
 WHERE source_id LIKE 'LAW/_%' ESCAPE '/'
 GROUP BY source_id
 ORDER BY source_id
 LIMIT 50;

-- ③ 참고 — 개정으로 조문이 "삭제/이동"되어 로케이터 자체가 사라진 경우:
--    같은 로케이터의 신행이 없으므로 ①로는 내려가지 않는다(구 행이 'Y' 로 잔존).
--    이 케이스는 최신 processed 산출물(corpus/processed/LAW_*_processed.json)의 로케이터
--    목록과 대조해 **사람이 확인 후** 개별 UPDATE 로 내린다(자동 일괄 내림 금지 — 수집
--    실패로 로케이터가 비어 보이는 경우와 구분이 안 되기 때문).
--    대조 쿼리(잔존 후보 조회 — 실행해도 데이터 변경 없음):
--      SELECT source_id, source_locator
--        FROM tb_ai_corpus_chunk
--       WHERE source_id = 'LAW_XXXX' AND use_yn = 'Y'
--         AND source_locator NOT IN ( ...최신 processed 로케이터 목록... )
--       LIMIT 100;
