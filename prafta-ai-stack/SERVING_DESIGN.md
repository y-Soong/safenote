# PRAFTA RAG 서빙 계층 설계 (Spring Boot)

> 로컬 코퍼스(pgvector, 44,030청크 4자료)를 `prafta-backend`에서 질의하는 서빙 계층 설계.
> 구현은 planner→developer→qa→security 워크플로우로 진행(본 문서 = 설계/plan 산출물).

---

## 0. 핵심 원칙 (정책서 §8 정합)

1. **판단은 사람** — AI는 "근거를 창작"하지 않는다. 검색된 실제 코퍼스를 **출처와 함께 제시**하고, 최종 판단은 관리자/근로자가 한다. 응답은 항상 "참고 근거"로 프레이밍(결정 단정 금지).
2. **출처 명시** — 모든 결과에 `source_name`(제공기관/데이터셋) + `source_locator`(원본 역추적) 부착.
3. **신뢰등급 노출** — `data_reliability`(규범형>집계형>자율신고형)를 응답에 실어, 자율신고형은 "참고 신고사례"로 약하게 표시.
4. **verbatim 게이팅(하드가드#4)** — `track=verbatim`(제3유형·변경금지, 예: SIF) 청크는 **원문 그대로 인용만** 허용. LLM 재구성/변형 금지. `track=recompose`만 재구성 대상.

---

## 1. 아키텍처

```
[앱/웹 클라이언트]
   │  POST /prafta/appApi/ai01/search   (JWT)
   ▼
[prafta-backend (Spring Boot 단일 모놀리스)]
   ├─ 기존: MySQL(primary DataSource, MyBatis)  ← 근태/결재 등 업무
   └─ 신규: AI 서빙 모듈  com.prafta.app.ai.ai01
        ├─ EmbeddingClient ──HTTP──▶ TEI(BGE-m3) http://localhost:8090/embed
        └─ AiCorpusRepository ──JDBC──▶ pgvector(Postgres) prafta_ai:5432
                                          tb_ai_corpus_chunk (HNSW cosine)
```

- **모놀리스 내 2차 데이터소스**로 시작(MVP). 나중에 별도 AI 마이크로서비스로 추출 가능.
- 벡터 질의는 **JdbcTemplate**(2차 MyBatis 대신) — `<=> ?::vector` 바인딩이 단순.

---

## 2. 설정 변경

### 2.1 의존성 (build.gradle)
```gradle
runtimeOnly 'org.postgresql:postgresql'      // pgvector Postgres JDBC
// (선택, phase2) Anthropic Claude SDK 또는 WebClient 직접 호출
```

### 2.2 application-*.properties (env 주입)
```properties
# pgvector 코퍼스 DB (로컬)
prafta.ai.datasource.url=${AI_DB_URL:jdbc:postgresql://localhost:5432/prafta_ai}
prafta.ai.datasource.username=${AI_DB_USERNAME:prafta}
prafta.ai.datasource.password=${AI_DB_PASSWORD:prafta1234}
# TEI 임베딩 서버
prafta.ai.tei.url=${AI_TEI_URL:http://localhost:8090}
prafta.ai.tei.timeout-ms=${AI_TEI_TIMEOUT:120000}
# 검색 기본값
prafta.ai.search.default-top-k=5
prafta.ai.search.max-top-k=20
```
> ⚠️ 비밀(비밀번호)은 운영에서 환경변수로만. 로컬 기본값은 개발 편의.

### 2.3 2차 데이터소스 (신규 com.prafta.common.config.AiDbConfig)
```java
@Configuration
public class AiDbConfig {
    @Bean
    @ConfigurationProperties("prafta.ai.datasource")
    public DataSource aiDataSource() {
        // HikariCP 풀(운영 권장). primary(MySQL)는 기존 DBConfig 유지.
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }
    @Bean
    public JdbcTemplate aiJdbcTemplate(@Qualifier("aiDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}
```
> ★ 기존 `DBConfig.dataSource()`에 `@Primary` 부착 필요(빈 2개 → MyBatis가 primary 참조하도록).
> AI 데이터소스는 트랜잭션 매니저를 공유하지 않는다(읽기 전용 조회).

---

## 3. 레이어 구조 (기존 모듈 컨벤션 준수)

```
com.prafta.app.ai.ai01
├─ controller/ Ai01Controller        # @RequestMapping("/ai01") → /prafta/appApi/ai01/*
├─ service/    Ai01Service, Ai01ServiceImpl
├─ repository/ AiCorpusRepository     # aiJdbcTemplate 사용(pgvector)
├─ client/     EmbeddingClient        # TEI 호출(RestTemplate 재사용)
└─ dto/        RagSearchRequest, RagHit, RagSearchResponse
```

---

## 4. 엔드포인트 ① — 검색 (MVP, 필수)

### `POST /prafta/appApi/ai01/search`
질의를 임베딩 → pgvector top-K 검색 → 출처·신뢰등급·트랙 포함 반환. **LLM 없음.**

**Request** (JSON, camelCase)
```json
{ "query": "이동식 크레인 전도 방지 대책",
  "topK": 5,
  "domainTag": null,
  "reliabilityIn": null,        // ["규범형","집계형"] 등 필터(옵션)
  "trackIn": null }             // ["recompose"] 등(옵션)
```

**Response**
```json
{ "query": "...",
  "hits": [
    { "chunkId":"15140227_ab12...", "sourceId":"15140227",
      "sourceName":"주요 기인물별 유해위험요인 및 감소대책",
      "dataReliability":"집계형", "track":"recompose", "quotable":true, "modifiable":true,
      "domainTag":"발전설비안전", "causeAgent":"이동식 크레인",
      "content":"...", "hazardText":"...", "measureText":"...",
      "sourceLocator":"이동식 크레인|17", "distance":0.326, "score":0.674 }
  ] }
```

**핵심 SQL (aiJdbcTemplate)**
```sql
SELECT chunk_id, source_id, content, hazard_text, measure_text, domain_tag, cause_agent,
       source_locator,
       meta_json->>'source_name'       AS source_name,
       meta_json->>'data_reliability'  AS data_reliability,
       meta_json->>'track'             AS track,
       (embedding <=> ?::vector)       AS distance
  FROM tb_ai_corpus_chunk
 WHERE use_yn = 'Y'
   AND (?::text IS NULL OR domain_tag = ?)
   AND (?::text[] IS NULL OR meta_json->>'data_reliability' = ANY(?))
   AND (?::text[] IS NULL OR meta_json->>'track'            = ANY(?))
 ORDER BY embedding <=> ?::vector          -- HNSW 인덱스 사용
 LIMIT ?
```
- 쿼리 벡터는 `"[0.1,0.2,...]"` 문자열로 바인딩(`?::vector`). SELECT·ORDER BY 2회 전달.
- `score = 1 - distance`(코사인). `quotable=true`(항상), `modifiable = (track != 'verbatim')`.

**서비스 흐름**
```
query → EmbeddingClient.embed(query) : float[1024]
      → vecLiteral = "[" + join(",") + "]"
      → AiCorpusRepository.search(vecLiteral, filters, topK)
      → RagHit 매핑(RowMapper) + modifiable 계산
```

**(선택) 신뢰등급 가중 재랭크**: `adjScore = score - w*penalty(reliability)`
(규범형 0 / 집계형 0.02 / 자율신고형 0.05) — 동점 시 신뢰 높은 자료 우선. MVP는 raw score 반환 + 클라이언트가 표시 강도 결정.

---

## 5. 엔드포인트 ② — 근거 기반 답변 (Phase 2, LLM)

### `POST /prafta/appApi/ai01/answer`
검색 → **근거에 엄격히 grounding된 답변** 생성. LLM 필요(Claude API 권장).

**엄격 규칙(프롬프트·후처리로 강제)**
- 검색된 청크에 **없는 내용은 생성 금지**. 관련 청크가 없으면 "관련 근거 없음"으로 abstain.
- 모든 문장에 **출처 각주**(source_name + source_locator).
- `track=verbatim` 청크는 **원문 그대로 인용**(따옴표), 재구성 금지. `track=recompose`만 요약/재구성.
- 답변 말미 **"본 내용은 참고 근거이며 최종 판단은 담당자가 수행"** 고지.
- **신뢰등급 표시**: 자율신고형 근거는 "참고 신고사례"로 명시.

**Response**
```json
{ "answer":"...(근거 기반, 각주 포함)...",
  "abstained": false,
  "citations":[ {"marker":"[1]","sourceName":"...","sourceLocator":"...","track":"verbatim"} ],
  "usedChunks":[ "15140383_...", "15108262_..." ],
  "disclaimer":"본 내용은 참고 근거이며 최종 판단은 담당자가 수행합니다." }
```
> LLM 공급자 결정 필요(Claude API 키). 미결정 시 ①검색 엔드포인트만으로도 "출처 붙은 근거 제시"는 충족.

---

## 6. 인증·감사

- **인증**: `/prafta/appApi/**` 는 기존 JWT 필터 + DailyUserActiveGate 적용. AI 엔드포인트도 상속. 사내 사용자면 별도 역할 게이트 불요(정책상 조회는 광범위 허용).
- **감사/과금(Phase 2, tb_ai_session/tb_ai_call — 정책서 §8 별도작업)**: `/answer`는 호출자(userCd·회사)·질의·사용청크·토큰수 로깅. 검색 전용 ①은 경량 로깅(질의·hit수)만.
- **PII**: 코퍼스는 이미 마스킹됨(전처리 단계). 서빙은 저장된 값 그대로 반환(추가 노출 없음).

---

## 7. 거버넌스 규칙 요약

| track | 의미 | 서빙 처리 |
|---|---|---|
| recompose | 제한없음/제1유형 | 검색·인용·**LLM 재구성 허용** |
| verbatim | 제3유형(변경금지) | 검색·**원문 인용만**, 재구성 금지(응답 `modifiable=false`) |

| data_reliability | 표시 강도 |
|---|---|
| 규범형 | "법적 기준"(현재 자료엔 없음) |
| 집계형 | "기관 집계 자료" |
| 자율신고형 | "참고 신고사례"(약하게) |

---

## 8. 미결정(사람 확정 필요)

1. **엔드포인트 위치**: `appApi`(현장 근로자/모바일) 우선 권장. 관리자 웹도 쓰면 `comApi`로 승격 또는 미러.
2. **Phase 2 LLM 공급자**: Claude API 사용 여부·키·과금 모델.
3. **재랭크 정책**: 순수 코사인 vs 신뢰등급 가중.
4. **운영 DB**: 로컬 pgvector → 운영 이관 시 접속정보·백업·인덱스(HNSW 파라미터) 재검토.

---

## 9. 구현 순서(제안)

1. AiDbConfig(2차 DS) + DBConfig @Primary + Postgres 의존성 → 기동 검증
2. EmbeddingClient(TEI) → 단위 호출 검증(1024차원)
3. AiCorpusRepository + Ai01Service + Ai01Controller(검색 ①) → 실질의 E2E
4. (검색 안정화 후) Phase 2 answer + 감사 로깅
