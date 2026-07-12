# PRAFTA RAG 서빙 계층 — Phase 2: 근거 기반 답변(answer) 설계

> Phase 1(`ai01/search`, 검색 전용) 위에 LLM 답변 계층을 얹는다.
> 전제: `SERVING_DESIGN.md`(Phase 1) 구현 완료. 본 문서는 그 확장 설계.
> ★핵심 제약: 하드가드 #4 — **verbatim(제3유형·변경금지) 청크는 LLM 프롬프트에 절대 투입 금지**.

---

## 0. 원칙 (Phase 1 §0 + 답변 특화)

1. **근거만, 창작 금지** — 검색된 청크에 **없는 내용은 생성하지 않는다.** 관련 청크가 없으면 답변하지 않고 abstain.
2. **출처 각주 필수** — 모든 문장을 청크(`source_name` + `source_locator`)에 귀속.
3. **verbatim/recompose 물리 분리(하드가드 #4)** — track별로 처리 경로가 다르다(§4). verbatim은 LLM을 거치지 않는다.
4. **신뢰등급 반영** — 자율신고형 근거는 "참고 신고사례"로 약하게. 규범형>집계형>자율신고형.
5. **판단은 사람** — 답변은 "참고 근거"로 프레이밍. 말미에 고지. 결정 단정 금지.

---

## 1. 아키텍처

```
클라이언트 → POST /prafta/appApi/ai01/answer  (JWT)
  → Ai01Service.search(query, topK)              ← Phase 1 재사용(임베딩+pgvector)
  → 청크를 track으로 분리:
       recompose[] ──▶ Claude API(Java SDK) ──▶ 근거기반 합성 답변 + 각주
       verbatim[]  ──▶ (LLM 미투입) 원문+출처 그대로 응답에 첨부
  → 감사·과금 로깅(tb_ai_call)
  → { answer, citations, verbatimReferences, abstained, disclaimer }
```

- **LLM: Anthropic Java SDK**(공식). 기본 모델 `claude-opus-4-8`.
- **검색은 Phase 1 재사용** — 새 임베딩/쿼리 로직 없음.

---

## 2. 설정

### 2.1 의존성 (build.gradle)
```gradle
implementation("com.anthropic:anthropic-java:2.34.0")
```

### 2.2 application-*.properties
```properties
# Anthropic API (비밀은 env 전용)
prafta.ai.llm.enabled=${AI_LLM_ENABLED:false}          # ★기본 OFF(게이트). 키 주입+검증 후 ON
prafta.ai.llm.model=${AI_LLM_MODEL:claude-opus-4-8}
prafta.ai.llm.max-tokens=${AI_LLM_MAX_TOKENS:8192}    # ★adaptive thinking 토큰 포함 상한. 잘림→파싱실패(AI_502_004) 예방 위해 여유
prafta.ai.llm.answer-top-k=${AI_LLM_ANSWER_TOP_K:6}
# ANTHROPIC_API_KEY 는 SDK가 환경변수에서 자동 로드(properties에 두지 않음)
```
> **비밀 처리**: `ANTHROPIC_API_KEY`는 properties에 절대 넣지 않는다. SDK `AnthropicOkHttpClient.fromEnv()`가 OS 환경변수에서 읽는다. Phase 1의 pgvector 비밀 처리와 동일 원칙.
> **게이트**: `prafta.ai.llm.enabled=false` 기본. 키 없거나 게이트 OFF면 answer 엔드포인트는 `AI_503_001`(기능 비활성)로 응답 → 부분 배포 안전.
>
> ### ★운영 enable 전제조건 (게이트 ON 전 필수)
> `prafta.ai.llm.enabled=true` 로 전환하기 전, 다음을 반드시 선결한다. answer 는 호출당 유료 Claude 호출을 유발하므로, 데이터 접근 통제(JWT·조회 전용)와 별개로 **비용 통제**가 필요하다.
> - **[필수] 사용자/회사 단위 호출·토큰 쿼터 또는 rate limit** — 인증된 저권한 계정(일용직 등)이 `/answer` 를 반복 호출하면 회사 귀속 Anthropic 청구액이 무제한 증가(financial DoS)한다. 게이트 ON 전 최소 1개의 상한(시간당 rate limit 또는 일/월 토큰 쿼터)을 도입한다. 한도·윈도우 값은 과금 기획(§9.5)에서 확정. tb_ai_call 이 호출당 userCd/cmpnyCd/토큰을 이미 적재하므로 이를 근거로 구현 가능.
> - (권고) 감사·과금 로깅 실패(best-effort)의 유실 가시화(실패 카운터/알람) — 쿼터·원장 무결성과 직결.
> - (프론트) 앱 Vue 에서 `answer`/`verbatimReferences[].content` 는 표시 전용 텍스트로 렌더(v-html 금지) — LLM/코퍼스 유래 마크업 XSS 차단.

### 2.3 Anthropic 클라이언트 빈 (신규 com.prafta.common.config.AiLlmConfig)
```java
@Configuration
@ConditionalOnProperty(name = "prafta.ai.llm.enabled", havingValue = "true")
public class AiLlmConfig {
    @Bean
    public AnthropicClient anthropicClient() {
        // ANTHROPIC_API_KEY 환경변수 자동 로드
        return AnthropicOkHttpClient.fromEnv();
    }
}
```

---

## 3. 엔드포인트 계약

### `POST /prafta/appApi/ai01/answer`

**Request**
```json
{ "query": "이동식 크레인 전도 방지 대책",
  "topK": 6,
  "domainTag": null, "reliabilityIn": null, "trackIn": null }
```

**Response**
```json
{
  "abstained": false,
  "answer": "이동식 크레인 전도를 막으려면 ... [1][2]. 아웃트리거를 ... [1].",
  "citations": [
    {"marker":"[1]","chunkId":"15140227_ab..","sourceName":"주요 기인물별 유해위험요인 및 감소대책",
     "sourceLocator":"이동식 크레인|17","dataReliability":"집계형"}
  ],
  "verbatimReferences": [
    {"chunkId":"15140383_cd..","sourceName":"SIF 아카이브","dataReliability":"집계형",
     "content":"...원문 그대로...","sourceLocator":"건설업|123"}
  ],
  "usedChunkIds": ["15140227_ab..","15140383_cd.."],
  "disclaimer":"본 내용은 참고 근거이며 최종 판단은 담당자가 수행합니다."
}
```
- `answer` — recompose 청크만으로 합성한 LLM 답변(각주 `[n]`).
- `citations` — 답변 각주 ↔ recompose 청크 매핑.
- `verbatimReferences` — verbatim 청크의 **원문+출처**(LLM 미가공). 클라이언트가 "원문 인용"으로 별도 표시.
- `abstained=true` — 관련 recompose 근거가 없어 합성 답변을 생성하지 않음(answer는 빈 문자열 또는 안내).

---

## 4. ★거버넌스: track 분리 (하드가드 #4 강제)

검색 결과를 **코드로 두 갈래**로 나눈다:

```java
List<RagHit> hits = ai01Service.search(param);           // Phase 1
List<RagHit> recompose = hits.stream()
    .filter(h -> !"verbatim".equals(h.getTrack())).toList();
List<RagHit> verbatim  = hits.stream()
    .filter(h ->  "verbatim".equals(h.getTrack())).toList();
```

- **recompose** 청크만 LLM 프롬프트의 컨텍스트로 조립 → 합성 답변 생성.
- **verbatim** 청크는 **프롬프트에 절대 넣지 않는다.** 응답 `verbatimReferences`에 원문 그대로 첨부(출처 동반).
- recompose가 0개면 `abstained=true`(창작 금지). verbatim만 있으면 answer는 abstain하고 verbatimReferences만 제시.

> 이 분리가 하드가드 #4의 기술적 강제점이다. verbatim 콘텐츠 문자열이 LLM 요청 바디에 **바이트 단위로 들어가지 않음**을 코드·리뷰로 보장한다.

---

## 5. LLM 호출 (Anthropic Java SDK)

### 5.1 구조화 출력으로 각주 신뢰성 확보
답변+각주를 안정적으로 파싱하기 위해 **구조화 출력**(structured outputs) 사용. POJO로 스키마 자동 파생.

```java
// 응답 POJO (SDK가 스키마 자동 생성 → 타입 반환)
public record AnswerResult(
    boolean abstained,
    String answer,                 // 각주 [n] 포함, recompose 근거만
    List<CitationOut> citations    // [{marker, chunkId}]
) {}
public record CitationOut(String marker, String chunkId) {}
```

```java
StructuredMessageCreateParams<AnswerResult> params = MessageCreateParams.builder()
    .model(Model.CLAUDE_OPUS_4_8)                 // ★기본. 설정값으로 오버라이드 가능
    .maxTokens(aiProps.getLlm().getMaxTokens())   // 4096(짧은 근거답변). thinking 토큰 포함
    .thinking(ThinkingConfigAdaptive.builder().build())  // adaptive(권장). budget_tokens 사용 금지(400)
    .systemOfTextBlockParams(List.of(SYSTEM_BLOCK))      // §6, 캐시 가능
    .addUserMessage(buildContextPrompt(query, recompose))// recompose 청크 컨텍스트
    .outputConfig(AnswerResult.class)             // 구조화 출력
    .build();

AnswerResult out = anthropicClient.messages().create(params)
    .content().stream().flatMap(cb -> cb.text().stream())
    .findFirst().orElseThrow().text();            // 타입 반환
```
> ⚠️ Opus 4.8/4.7/Sonnet5: `temperature`/`top_p`/`top_k`/`budget_tokens` 전송 시 **400**. 넣지 않는다.
> 답변이 길어질 여지가 있으면 `client.messages().createStreaming(...)` + `get_final_message` 사용(HTTP 타임아웃 회피). MVP 근거답변은 짧아 non-stream 4096으로 충분.
> `citations[].chunkId`는 LLM이 컨텍스트에서 부여받은 청크 ID를 되돌려주게 하고, **서버가 실제 recompose 청크 ID 집합과 대조**(환각 인용 차단).

### 5.2 컨텍스트 프롬프트 조립 (recompose만)
```
[근거 청크]
[1] (출처: 건설안전사고사례 / 자율신고형)
<content>
[2] (출처: 주요 기인물별 유해위험요인 및 감소대책 / 집계형)
<content>
...
[질문] 이동식 크레인 전도 방지 대책
```
각 청크에 `[n]` 마커·출처·신뢰등급을 붙여 제시. LLM은 이 마커로 각주를 단다.

---

## 6. 시스템 프롬프트(그라운딩 규칙)

```
당신은 산업안전 근거 검색 보조자다. 아래 원칙을 반드시 지킨다.
1. 제공된 [근거 청크]에 있는 내용만으로 답한다. 청크에 없는 사실·수치·대책을 지어내지 않는다.
2. 답변의 모든 핵심 문장 끝에 근거 청크 번호를 [n] 형식으로 표기한다.
3. 관련 있는 근거가 없으면 답변을 만들지 말고 abstained=true 로 반환한다.
4. 근거의 신뢰등급을 반영한다: '자율신고형'은 단정하지 말고 "참고 신고사례에 따르면…"처럼 약하게 인용한다.
5. 이 답변은 참고용 근거 제시이며 최종 판단은 담당자가 한다. 결정을 단정하지 않는다.
6. citations 에는 실제 인용한 청크의 번호와 chunkId 만 담는다.
출력은 지정된 JSON 스키마를 따른다.
```
> 시스템 프롬프트·`prafta.ai.llm.*`는 요청마다 동일 → 프롬프트 캐시 적중(비용↓). 청크 컨텍스트만 요청별로 변동.

---

## 7. 에러 처리 (Phase 1 AiErrorCode 확장)

| 상황 | 처리 |
|---|---|
| 게이트 OFF/키 없음 | `AI_503_001`(LLM 기능 비활성) |
| Claude 4xx/네트워크 | `AI_502_003`(LLM 호출 실패) — 원인은 서버로그만 |
| `stop_reason == "refusal"` | `content` 읽기 전 분기. abstain 처리 + `AI_200`(정상, abstained=true) 또는 안내 |
| 429 rate limit | SDK 자동 재시도(2회). 초과 시 `AI_502_003` |
| 구조화 출력 파싱 실패 | `AI_502_004`(응답 형식 오류) |
| recompose 0개 | LLM 호출 없이 즉시 `abstained=true` 반환(비용 0) |

- ★ **refusal 가드 필수**: Opus 4.8도 안전분류로 200+refusal 가능. `stopReason()` 확인 후 `content()` 접근(빈 배열 인덱스 오류 방지).

---

## 8. 감사·과금 (정책서 §8: tb_ai_call / tb_ai_session)

LLM 호출은 **회사별 과금** 대상이므로 호출 단위 로깅. pgvector(prafta_ai) DB에 신규 테이블(aiJdbcTemplate 재사용).

```sql
CREATE TABLE tb_ai_call (
    CALL_ID         VARCHAR(40) PRIMARY KEY,     -- UUID
    USER_CD         VARCHAR(40) NOT NULL,        -- JWT gv_userCd
    CMPNY_CD        VARCHAR(40),                 -- JWT gv_cmpnyCd(과금 귀속)
    ENDPOINT        VARCHAR(40) NOT NULL,        -- 'ai01/answer'
    MODEL           VARCHAR(60),
    QUERY_LEN       INT,                         -- ★질의 원문 저장 안 함(PII). 길이만
    QUERY_HASH      VARCHAR(64),                 -- (선택) sha256, 중복분석용·복원불가
    INPUT_TOKENS    INT, OUTPUT_TOKENS INT, CACHE_READ_TOKENS INT,
    COST_USD        NUMERIC(10,5),               -- usage×모델단가 서버계산
    USED_CHUNK_IDS  JSONB,                       -- 인용 청크
    ABSTAINED       BOOLEAN,
    CREATED_AT      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- tb_ai_session(멀티턴 대화)은 MVP 단발 답변에선 생략. 향후 대화형 도입 시 추가.
```
- 토큰은 `response.usage()`에서(`inputTokens`/`outputTokens`/`cacheReadInputTokens`). 비용은 모델별 단가표로 서버 계산.
- **PII**: 질의 원문은 저장하지 않고 길이(+선택적 해시)만. Phase 1 보안검토의 질의-로깅 Medium과 동일 방침.

---

## 9. 미결정(사람 확정)

1. **모델 tier** — 기본 `claude-opus-4-8`(스킬 권장). 비용 우선이면 `claude-sonnet-5`($3/$15) 또는 `claude-haiku-4-5`($1/$5)로 설정값 교체 가능. 근거-합성 난이도가 높지 않아 Sonnet/Haiku도 실용적. → 운영 볼륨·품질 테스트로 결정.
2. **감사 DB 위치** — pgvector(prafta_ai) vs MySQL(앱 감사와 통일). 본 설계는 전자(AI 자립).
3. **멀티턴 세션** — MVP 단발. 대화형 필요 시 tb_ai_session + 대화 히스토리·프롬프트 캐시 설계.
4. **스트리밍 UX** — 근거답변이 길거나 실시간 표시가 필요하면 SSE 스트리밍 도입.
5. **과금 정책** — 회사별 호출/토큰 상한·요금 산정(별도 기획). ★이 중 **호출/토큰 상한(rate limit·쿼터)은 운영 enable 전제조건**이다(§2.2 참조). 요금 산정은 후속이어도, 비용 남용 방어용 상한은 게이트 ON 전 필수.

---

## 10. 구현 순서(제안)

1. build.gradle 의존성 + AiLlmConfig(게이트 `@ConditionalOnProperty`) + AiProperties.llm 확장
2. tb_ai_call 마이그레이션(prafta_ai) + AiCallRepository(aiJdbcTemplate)
3. AnswerService: search 재사용 → track 분리 → (recompose>0)면 Claude 호출(구조화 출력) → 인용 검증(chunkId 대조) → 감사 로깅
4. Ai01Controller `POST /ai01/answer` + DTO(AnswerRequest/Response) + AiErrorCode 확장(502_003/004, 503_001)
5. refusal·429·파싱실패 가드 → 통합 테스트(게이트 ON, 실 키로 1회 E2E)
