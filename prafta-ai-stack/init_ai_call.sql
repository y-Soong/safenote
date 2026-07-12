-- =============================================================================
-- PRAFTA RAG Phase 2 — LLM 호출 감사·과금 테이블(tb_ai_call)
-- 대상 DB: prafta_ai (Postgres, pgvector 코퍼스와 동일 인스턴스, aiJdbcTemplate 사용)
-- 설계 근거: SERVING_DESIGN_PHASE2.md §8
--
-- ★PII: 질의 원문·해시 모두 저장하지 않는다. QUERY_LEN(길이)만(PII 역추적 리스크 제거).
-- 적용: psql 로 prafta_ai(Postgres)에 실행. ★MySQL 아님(JSONB 등 Postgres 문법).
-- =============================================================================

CREATE TABLE IF NOT EXISTS tb_ai_call (
    call_id            VARCHAR(40)  PRIMARY KEY,          -- UUID
    user_cd            VARCHAR(40)  NOT NULL,             -- JWT gv_userCd
    cmpny_cd           VARCHAR(40),                       -- JWT gv_cmpnyCd(과금 귀속)
    endpoint           VARCHAR(40)  NOT NULL,             -- 'ai01/answer' 또는 'riskai01/*'
    model              VARCHAR(60),                       -- 사용 모델(HCX-005 등)
    query_len          INT,                               -- ★질의 원문·해시 미저장(길이만)
    input_tokens       INT,
    output_tokens      INT,
    cache_read_tokens  INT,
    cache_creation_tokens INT,                            -- 프롬프트 캐시 쓰기 토큰(HCX=0)
    cost_usd           NUMERIC(10,5),                     -- usage×모델단가 서버계산
    used_chunk_ids     JSONB,                             -- 인용 recompose + verbatim 청크 ID
    abstained          BOOLEAN,
    created_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 과금 집계(회사·기간)용 보조 인덱스.
CREATE INDEX IF NOT EXISTS ix_ai_call_cmpny_created ON tb_ai_call (cmpny_cd, created_at);
CREATE INDEX IF NOT EXISTS ix_ai_call_user_created  ON tb_ai_call (user_cd, created_at);

-- tb_ai_session(멀티턴 대화)은 MVP 단발 답변에선 생략. 향후 대화형 도입 시 추가.
