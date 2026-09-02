-- PRAFTA AI 코퍼스 스키마 (pgvector)
-- ※ PostgreSQL은 따옴표 없는 식별자를 소문자로 접는다.
--    아래 SOURCE_ID 등 대문자 컬럼은 실제로는 source_id 로 생성된다(정상).
--    MyBatis mapUnderscoreToCamelCase=true 면 source_id -> sourceId 로 자연 매핑되므로
--    따옴표로 대문자를 강제하지 않는다(강제하면 모든 쿼리에 따옴표가 전파됨).

-- 벡터 확장 활성화
CREATE EXTENSION IF NOT EXISTS vector;

-- 코퍼스 출처(라이선스/피드 관리 단위)
CREATE TABLE tb_ai_corpus_source (
    SOURCE_ID            VARCHAR(40)  PRIMARY KEY,
    SOURCE_ORG           VARCHAR(100) NOT NULL,
    SOURCE_NAME          VARCHAR(200) NOT NULL,
    SOURCE_URL           VARCHAR(500),
    LICENSE_TYPE         VARCHAR(20)  NOT NULL,
    ADOPTED_DATE         VARCHAR(8)   NOT NULL,
    LICENSE_CHECKED_DATE VARCHAR(8),
    SOURCE_UPDATE_CYCLE  VARCHAR(20),
    FEED_TYPE            VARCHAR(10)  NOT NULL,
    -- 근거 층위(prafta-062): LAW(법령)|GUIDE(권고 지침)|STAT(고위험 통계)|CASE(유사 재해)|REF(참고 자료)
    --   NULL 허용 = 미지정(화면 배지 미표시, 종전 동작). 라이브 DB엔 prafta-062-evidence-tier-1.sql 로 적용.
    EVIDENCE_TIER        VARCHAR(20),
    USE_YN               CHAR(1)      NOT NULL DEFAULT 'Y',
    INSERT_NO            VARCHAR(50)  DEFAULT 'SYSTEM',
    INSERT_DATE          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UPDATE_NO            VARCHAR(50),
    UPDATE_DATE          TIMESTAMP
);

-- 코퍼스 청크(임베딩 검색 단위)
CREATE TABLE tb_ai_corpus_chunk (
    CHUNK_ID       VARCHAR(40)   PRIMARY KEY,
    SOURCE_ID      VARCHAR(40)   NOT NULL REFERENCES tb_ai_corpus_source(SOURCE_ID),
    CONTENT        TEXT          NOT NULL,
    EMBEDDING      VECTOR(1024)  NOT NULL,           -- BGE-m3 = 1024차원
    HAZARD_TEXT    TEXT,
    MEASURE_TEXT   TEXT,
    DOMAIN_TAG     VARCHAR(50),
    CAUSE_AGENT    VARCHAR(100),
    META_JSON      JSONB,
    SOURCE_LOCATOR VARCHAR(200),
    USE_YN         CHAR(1)       NOT NULL DEFAULT 'Y',
    INSERT_NO      VARCHAR(50)   DEFAULT 'SYSTEM',
    INSERT_DATE    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    UPDATE_NO      VARCHAR(50),
    UPDATE_DATE    TIMESTAMP
);

-- 벡터 유사도 검색 인덱스(코사인). 대량 적재 시엔 적재 후 재생성이 빠름.
CREATE INDEX ix_corpus_chunk_embedding
    ON tb_ai_corpus_chunk USING hnsw (EMBEDDING vector_cosine_ops);
CREATE INDEX ix_corpus_chunk_source ON tb_ai_corpus_chunk(SOURCE_ID, USE_YN);
CREATE INDEX ix_corpus_chunk_domain ON tb_ai_corpus_chunk(DOMAIN_TAG);

-- ★HNSW 사후필터 기아 방지(2026-07-05): track/신뢰등급 등 필터 검색 시 HNSW가 최근접
--   ef_search(기본 40)개만 뽑고 필터를 적용해, 소수 트랙(verbatim 등)이 0건이 되는 문제.
--   iterative scan(pgvector 0.8+)으로 필터 통과분이 LIMIT을 채울 때까지 계속 탐색한다.
--   (라이브 DB엔 ALTER DATABASE로 기적용 — 이 파일은 볼륨 재생성 대비.)
ALTER DATABASE prafta_ai SET hnsw.iterative_scan = relaxed_order;
