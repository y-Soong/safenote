package com.prafta.common.config;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG(AI 검색) 설정 바인딩.
 *
 * <p>prefix = "prafta.ai" 아래의 tei / search 설정을 담는다.
 *    데이터소스(prafta.ai.datasource.*)는 별도로 {@code DataSourceProperties} 가 바인딩하므로
 *    본 클래스에는 두지 않는다(알 수 없는 하위 키는 관대 모드로 무시된다).
 */
@ConfigurationProperties(prefix = "prafta.ai")
public class AiProperties {

    /** TEI 임베딩 서버 설정. */
    private final Tei tei = new Tei();
    /** 검색 topK 클램프 설정. */
    private final Search search = new Search();
    /** LLM 근거답변(ai01/answer, Phase 2) 설정. */
    private final Llm llm = new Llm();
    /** TBM AI 교육생성(T0) 설정. */
    private final Tbm tbm = new Tbm();

    public Tei getTei() {
        return tei;
    }

    public Search getSearch() {
        return search;
    }

    public Llm getLlm() {
        return llm;
    }

    public Tbm getTbm() {
        return tbm;
    }

    /** TEI(BGE-m3) 임베딩 서버. */
    public static class Tei {
        /** 서버 베이스 URL(예: http://localhost:8090). 호출 시 '/embed' 를 붙인다. */
        private String url;
        /** 연결/읽기 타임아웃(ms). 임베딩은 다소 오래 걸릴 수 있어 기본 120초. */
        private int timeoutMs = 120000;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    /** 검색 topK 기본값/상한. */
    public static class Search {
        /** 설정값 검증 경고용(prafta-064 D3 — setter 에서 범위/허용값 검증, 기동 시 1회 warn). */
        private static final Logger log = LoggerFactory.getLogger(AiProperties.class);

        /** 검색·선택용 질의 모드 허용값(prafta-064 S2): 라벨 없는 관리자 확정문(기본). */
        public static final String SEARCH_QUERY_MODE_COMPACT = "compact";
        /** 검색·선택용 질의 모드 허용값(prafta-064 S2): 종전 buildDeriveQuery 결과(즉시 원복 수단). */
        public static final String SEARCH_QUERY_MODE_FULL = "full";

        /** verbatim 트랙 최소 유사도 기본값(prafta-064 S1 — deriveMinScore 와 동일 출발). */
        private static final double DEFAULT_VERBATIM_MIN_SCORE = 0.40d;

        /** topK 미지정 시 기본값. */
        private int defaultTopK = 5;
        /** topK 상한(초과 시 이 값으로 클램프). */
        private int maxTopK = 20;
        /**
         * 위험성평가 AI 도출(riskai01/derive) 그라운딩 최소 유사도(cosine, score=1-distance).
         * 이 값 미만인 청크는 근거로 채택하지 않는다(무관한 코퍼스를 억지 근거로 삼는 것 방지).
         * 채택 청크가 0건이면 "코퍼스 근거부재 → 자유생성(ABSTAINED)" 경로로 폴백한다.
         * ★환경 의존 튜닝값: 실 코퍼스의 점수 분포(도출 로그의 topScore)를 보고 조정한다.
         *   너무 높으면 정상 질의도 근거부재로 빠지고, 너무 낮으면 필터가 무의미해진다.
         */
        private double deriveMinScore = 0.40d;

        /**
         * 법령 전용 트랙(prafta-062, riskai01 searchLawTrack) 최소 유사도(cosine, score=1-distance).
         * 이 값 미만인 조문은 첨부하지 않는다(무관 조문 억지 첨부 방지 — 오인용이 신뢰를 깎는다).
         * ★초기값은 deriveMinScore 와 동일한 0.40 으로 출발(D4=(b), 무회귀) — 법령 문체(추상적 조문)는
         *   사례 코퍼스와 점수 분포가 다를 수 있어 별도 knob 로 분리, 관측 로그("법령 트랙 판정")의
         *   최상위 점수 분포를 보고 조정한다.
         */
        private double lawMinScore = 0.40d;

        /**
         * 법령 조문 사후 첨부 스위치(riskai01 attachLawRefs). 기본 OFF.
         * ★2026-09-04 사용자 결정으로 보류: 벡터 유사도가 맞는 조문과 무관 조문을 구분하지 못해(운영 실측
         *   최상위 점수 0.58~0.61 동일 구간) 매 유해요인에 무관 조문이 붙었고, 실무 위험성평가는 조문보다
         *   "발생 가능한 위험 발굴"이 본체라 화면 가독성만 깎았다. LLM 선별 단계 등 재설계 검토 후 재개.
         * false 면 법령 트랙 검색·임베딩 호출 자체를 생략하고 lawRefs 는 null 유지 → 화면 관련 법령 블록 미표시
         *   (prafta-062 이전 형태). 코퍼스 법령 청크는 다른 트랙에서 배제 필터로 격리돼 있어 그대로 둔다.
         */
        private boolean lawRefsEnabled = false;

        /**
         * verbatim(참고 원문·SIF 재해개요) 트랙 최소 유사도(prafta-064 S1, riskai01 selectVerbatimRefs).
         * cosine score(=1-distance)가 이 값 미만인 후보는 선택 전용 호출에 넘기지 않는다(오매칭 원문 차단 —
         * "틀린 원문을 안 내보내는 것"이 "더 많이 찾는 것"보다 우선). 통과 후보가 0건이면 선택 호출 자체를 생략한다.
         * ★초기값은 deriveMinScore 와 동일한 0.40 으로 출발 — SIF 재해개요(1문장, 사망사고 서술체)는 사례 코퍼스와
         *   점수 분포가 다를 수 있어 독립 튜닝: 관측 로그("verbatim 트랙 판정")의 최상위점수 분포를 보고 조정한다.
         * 0 으로 두면 사실상 임계 해제(롤백 수단 — 단 선택 실패 시 폴백 제거는 코드라 그대로 유지).
         * 허용 범위 [0,1] 밖이면 setter 가 기동 시 warn 후 기본값(0.40)을 유지한다.
         */
        private double verbatimMinScore = DEFAULT_VERBATIM_MIN_SCORE;

        /**
         * 검색·선택용 질의 모드(prafta-064 S2, riskai01 derive). 허용값 compact|full, 기본 compact.
         * compact = 라벨([공정명] 등) 없는 관리자 확정문(buildSearchQuery)을 recompose 검색·verbatim 전용 검색·
         *   선택 프롬프트의 [위험성평가 요청] 에 사용(동음이의어·라벨 잡음으로 벡터가 끌리는 오매칭 완화).
         * full = 종전 buildDeriveQuery 결과를 검색에 사용(즉시 원복 수단).
         * ★생성 호출(그라운딩/자유생성) 입력은 모드와 무관하게 항상 full. 허용값 외면 setter 가 기동 시 warn 후 compact.
         */
        private String searchQueryMode = SEARCH_QUERY_MODE_COMPACT;

        public int getDefaultTopK() {
            return defaultTopK;
        }

        public void setDefaultTopK(int defaultTopK) {
            this.defaultTopK = defaultTopK;
        }

        public int getMaxTopK() {
            return maxTopK;
        }

        public void setMaxTopK(int maxTopK) {
            this.maxTopK = maxTopK;
        }

        public double getDeriveMinScore() {
            return deriveMinScore;
        }

        public void setDeriveMinScore(double deriveMinScore) {
            this.deriveMinScore = deriveMinScore;
        }

        public double getLawMinScore() {
            return lawMinScore;
        }

        public void setLawMinScore(double lawMinScore) {
            this.lawMinScore = lawMinScore;
        }

        public boolean isLawRefsEnabled() {
            return lawRefsEnabled;
        }

        public void setLawRefsEnabled(boolean lawRefsEnabled) {
            this.lawRefsEnabled = lawRefsEnabled;
        }

        public double getVerbatimMinScore() {
            return verbatimMinScore;
        }

        /** prafta-064 D3: [0,1] 밖(NaN 포함)이면 warn 후 기본값 유지 — 서비스는 값을 신뢰하고 읽기만 한다. */
        public void setVerbatimMinScore(double verbatimMinScore) {
            if (Double.isNaN(verbatimMinScore) || verbatimMinScore < 0d || verbatimMinScore > 1d) {
                log.warn("prafta.ai.search.verbatim-min-score 설정값이 허용 범위 [0,1] 밖 - 기본값 {} 적용",
                    DEFAULT_VERBATIM_MIN_SCORE);
                this.verbatimMinScore = DEFAULT_VERBATIM_MIN_SCORE;
                return;
            }
            this.verbatimMinScore = verbatimMinScore;
        }

        public String getSearchQueryMode() {
            return searchQueryMode;
        }

        /** prafta-064 D3: trim + 소문자 정규화 후 compact|full 외면 warn 후 compact — fail-safe 방향 = 기본값. */
        public void setSearchQueryMode(String searchQueryMode) {
            String v = (searchQueryMode == null) ? "" : searchQueryMode.trim().toLowerCase(Locale.ROOT);
            if (!SEARCH_QUERY_MODE_COMPACT.equals(v) && !SEARCH_QUERY_MODE_FULL.equals(v)) {
                log.warn("prafta.ai.search.search-query-mode 설정값이 허용값(compact|full) 밖 - 기본값 {} 적용",
                    SEARCH_QUERY_MODE_COMPACT);
                this.searchQueryMode = SEARCH_QUERY_MODE_COMPACT;
                return;
            }
            this.searchQueryMode = v;
        }
    }

    /**
     * LLM 근거답변 설정(네이버 HyperCLOVA X, HCX-005).
     *
     * <p>{@code enabled} 는 게이트(기본 false). false 면 {@code AiLlmConfig} 가 back-off 되어
     *    HCX REST 클라이언트 빈이 생성되지 않고, answer 엔드포인트는 AI_503_001 로 응답한다.
     *    비밀(CLOVA_STUDIO_API_KEY)은 여기에 두지 않는다 — 설정 빈이 OS 환경변수에서 읽는다.
     */
    public static class Llm {
        /** HCX 출력 토큰 상한(HCX-005 규격). maxTokens 는 이 값으로 강제 클램프된다. */
        private static final int MAX_OUTPUT_TOKENS_CEILING = 4096;

        /** LLM 답변 기능 게이트(기본 OFF). */
        private boolean enabled = false;
        /** CLOVA Studio 호스트(기본 운영 게이트웨이). 사설/프록시 경유 시 오버라이드. */
        private String host = "https://clovastudio.stream.ntruss.com";
        /** 모델 식별자(기본 HCX-005). 문자열 오버라이드로 tier 교체 가능. */
        private String model = "HCX-005";
        /** 최대 출력 토큰(HCX-005 상한 4096, ≤4096 강제). getter 에서 상한 클램프한다. */
        private int maxTokens = MAX_OUTPUT_TOKENS_CEILING;
        /** 샘플링 temperature(0~1). 근거답변은 낮게(0.2) 둬 결정성 확보. */
        private double temperature = 0.2;
        /** nucleus 샘플링 topP(0<≤1). */
        private double topP = 0.8;
        /** topK(0~128, 0=미사용). */
        private int topK = 0;
        /** 반복 억제(0<≤2). HCX 필드명은 repetitionPenalty. */
        private double repetitionPenalty = 1.1;
        /**
         * 입력 1k 토큰당 단가(공급자 통화·KRW 가정, 기본 0).
         * ★Naver CLOVA Studio 단가 확정 후 설정. 미설정(0)이면 COST=0 저장.
         */
        private double costPer1kInput = 0d;
        /** 출력 1k 토큰당 단가(공급자 통화·KRW 가정, 기본 0). */
        private double costPer1kOutput = 0d;
        /** answer 전용 검색 topK(근거 청크 상한). */
        private int answerTopK = 6;
        /**
         * HCX Vision 전송 이미지 긴 변 상한(px). 전송 직전 정규화 시 max(w,h)가 이 값을 초과하면 다운스케일한다.
         * HCX-005 Vision 한도(긴 변 ≤ 2240px)보다 보수적으로 기본 2000.
         */
        private int visionMaxImageSide = 2000;
        /**
         * 이미지 디코딩 폭탄 방어 하드캡(megapixels, w*h). 헤더로 읽은 픽셀 수가 이 값을 초과하면
         * 전체 디코드하지 않고 AI_400_008 로 거부한다. 기본 60_000_000(60MP).
         */
        private long maxDecodeMegapixels = 60_000_000L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        /** ★HCX-005 출력 상한(4096) 강제. 설정값이 더 커도 4096 을 넘지 않는다. */
        public int getMaxTokens() {
            return Math.min(maxTokens, MAX_OUTPUT_TOKENS_CEILING);
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public double getTopP() {
            return topP;
        }

        public void setTopP(double topP) {
            this.topP = topP;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getRepetitionPenalty() {
            return repetitionPenalty;
        }

        public void setRepetitionPenalty(double repetitionPenalty) {
            this.repetitionPenalty = repetitionPenalty;
        }

        public double getCostPer1kInput() {
            return costPer1kInput;
        }

        public void setCostPer1kInput(double costPer1kInput) {
            this.costPer1kInput = costPer1kInput;
        }

        public double getCostPer1kOutput() {
            return costPer1kOutput;
        }

        public void setCostPer1kOutput(double costPer1kOutput) {
            this.costPer1kOutput = costPer1kOutput;
        }

        public int getAnswerTopK() {
            return answerTopK;
        }

        public void setAnswerTopK(int answerTopK) {
            this.answerTopK = answerTopK;
        }

        public int getVisionMaxImageSide() {
            return visionMaxImageSide;
        }

        public void setVisionMaxImageSide(int visionMaxImageSide) {
            this.visionMaxImageSide = visionMaxImageSide;
        }

        public long getMaxDecodeMegapixels() {
            return maxDecodeMegapixels;
        }

        public void setMaxDecodeMegapixels(long maxDecodeMegapixels) {
            this.maxDecodeMegapixels = maxDecodeMegapixels;
        }
    }

    /**
     * TBM AI 교육생성(T0) 설정.
     *
     * <p>PDF 첨부는 페이지 이미지로 렌더링해 VLM 에 전송하므로, 토큰/용량 통제를 위한
     *    샘플링 knob(간격/상한)과 교육안 목표 글자수(min/max)를 담는다. 모두 기본값 내장이라
     *    properties 미기재 시에도 기본값으로 동작한다(relaxed-binding).
     */
    public static class Tbm {
        /** PDF 페이지 샘플링 간격(성기게 샘플, ≥1). */
        private int pdfPageStride = 5;
        /** 샘플 페이지 상한(토큰 통제, ≥1). */
        private int pdfMaxPages = 10;
        /** 교육안 목표 최소 글자수. */
        private int tbmContentMinChars = 1000;
        /** 교육안 목표 최대 글자수. */
        private int tbmContentMaxChars = 1500;

        public int getPdfPageStride() {
            return pdfPageStride;
        }

        public void setPdfPageStride(int pdfPageStride) {
            this.pdfPageStride = pdfPageStride;
        }

        public int getPdfMaxPages() {
            return pdfMaxPages;
        }

        public void setPdfMaxPages(int pdfMaxPages) {
            this.pdfMaxPages = pdfMaxPages;
        }

        public int getTbmContentMinChars() {
            return tbmContentMinChars;
        }

        public void setTbmContentMinChars(int tbmContentMinChars) {
            this.tbmContentMinChars = tbmContentMinChars;
        }

        public int getTbmContentMaxChars() {
            return tbmContentMaxChars;
        }

        public void setTbmContentMaxChars(int tbmContentMaxChars) {
            this.tbmContentMaxChars = tbmContentMaxChars;
        }
    }
}
