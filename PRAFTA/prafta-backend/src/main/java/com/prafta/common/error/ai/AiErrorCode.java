package com.prafta.common.error.ai;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * AI(RAG 검색) 도메인 에러코드.
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 *
 * <p>★주의: 앱 인터셉터는 COMMON_400_003 / COMMON_400_600 을 토큰 오류로 간주해 강제 로그아웃시킨다.
 *    따라서 본문/외부호출 검증 실패에는 COMMON_400_003/600 을 쓰지 않고 본 AI_* 코드를 사용한다.
 */
public enum AiErrorCode implements ApiErrorCode {

    // 400: 검색어 누락/공백
    AI_400_001(HttpStatus.BAD_REQUEST, "검색어를 입력해 주세요.")
    // 400: 이미지 분석 요청인데 대상 평가건에 유해요인 사진이 첨부되어 있지 않음(PRAFTA-WEB_003)
    , AI_400_002(HttpStatus.BAD_REQUEST, "분석할 이미지가 없습니다.\n유해요인 사진이 첨부된 평가건에서만 이미지 분석을 실행할 수 있습니다.")
    // 400: 관리자 추가 이미지 형식/용량/개수 위반(PRAFTA-WEB_003 v3). ★AI_400_003 은 v2.1 폐기 결번 — 재사용 금지
    , AI_400_004(HttpStatus.BAD_REQUEST, "첨부 이미지 형식 또는 용량이 허용 범위를 벗어났습니다.\n(jpg/png/webp, 장당 3MB, 최대 2장)")
    // 400: 확정할 이미지 분석 내용(assistant 확인 질의)이 없음(PRAFTA-WEB_003 v3 confirm-image)
    , AI_400_005(HttpStatus.BAD_REQUEST, "확정할 이미지 분석 내용이 없습니다.")
    // 404: AI 도출 대상 위험성평가 건이 없거나 접근 권한이 없음(PRAFTA-WEB_003, 존재 미노출)
    , AI_404_001(HttpStatus.NOT_FOUND, "대상 위험성평가 건을 찾을 수 없습니다.")
    // 502: TEI 임베딩 서버 호출 실패(연결 실패/타임아웃/오류 응답)
    , AI_502_001(HttpStatus.BAD_GATEWAY, "임베딩 서버 호출에 실패했습니다.\n잠시 후 다시 시도해 주세요.")
    // 502: 임베딩 응답이 비었거나 차원(1024)이 맞지 않음
    , AI_502_002(HttpStatus.BAD_GATEWAY, "임베딩 결과가 유효하지 않습니다.")
    // 500: 코퍼스 검색 처리 중 오류
    , AI_500_001(HttpStatus.INTERNAL_SERVER_ERROR, "AI 검색 처리 중 오류가 발생했습니다.")
    // 502: LLM(HyperCLOVA X) 호출 실패(HTTP 4xx/5xx/타임아웃/status.code≠20000) — 원인은 서버 로그만
    , AI_502_003(HttpStatus.BAD_GATEWAY, "AI 답변 생성 서버 호출에 실패했습니다.\n잠시 후 다시 시도해 주세요.")
    // 502: LLM 응답 형식 오류(구조화 JSON 파싱 실패)
    , AI_502_004(HttpStatus.BAD_GATEWAY, "AI 답변 응답 형식이 올바르지 않습니다.")
    // 503: LLM 답변 기능 비활성(게이트 OFF/키 미주입)
    , AI_503_001(HttpStatus.SERVICE_UNAVAILABLE, "AI 답변 기능이 현재 비활성화되어 있습니다.")
    // 400: AI 분석 대상 항목이 아님(이미지·PDF 첨부 항목만 분석 가능) — TBM_AI T0
    , AI_400_006(HttpStatus.BAD_REQUEST, "AI 분석 대상 항목이 아닙니다.\n이미지 또는 PDF 첨부 항목만 AI 분석할 수 있습니다.")
    // 400: 확정되지 않은 AI 분석 항목이 있어 교육자료 등록 차단 — TBM_AI(§B-6 등록 가드)
    , AI_400_007(HttpStatus.BAD_REQUEST, "AI 분석이 확정되지 않은 항목이 있어 교육자료를 등록할 수 없습니다.\n이미지·PDF 항목을 모두 확정해 주세요.")
    // 404: 분석할 첨부 파일을 찾을 수 없음(행 없음/디스크 없음) — TBM_AI
    , AI_404_002(HttpStatus.NOT_FOUND, "분석할 첨부 파일을 찾을 수 없습니다.")
    // 502: PDF 문서를 이미지로 변환하지 못함(손상/암호화 등) — TBM_AI
    , AI_502_005(HttpStatus.BAD_GATEWAY, "PDF 문서를 이미지로 변환하지 못했습니다.\n손상되었거나 암호화된 PDF일 수 있습니다.")
    // 400: HCX Vision 전송 전 정규화 시 이미지 해상도가 처리 한도(디코딩 폭탄 하드캡)를 초과 — HCX Vision 정규화
    , AI_400_008(HttpStatus.BAD_REQUEST, "이미지 해상도가 처리 한도를 초과했습니다.\n크기를 줄여 다시 시도해 주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    AiErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    @Override
    public String message() {
        return message;
    }
}
