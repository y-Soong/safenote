package com.prafta.common.error.risk;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 위험성평가 개선항목(지속평가대상 관리, prafta-058) 도메인 에러코드.
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 */
public enum RiskImprErrorCode implements ApiErrorCode {

    // 400: 첨부 사진(Base64) 크기 상한 초과 (Low-3)
    RISKIMPR_400_001(HttpStatus.BAD_REQUEST, "첨부 사진 용량이 허용 한도를 초과했습니다.")
    // 403: 해당 사업장에 대한 접근 권한 없음 (cross-site IDOR 차단)
    , RISKIMPR_403_001(HttpStatus.FORBIDDEN, "해당 사업장에 대한 접근 권한이 없습니다.")
    // 404: 대상 위험성평가 건 없음 (또는 사업장 스코프 밖)
    , RISKIMPR_404_001(HttpStatus.NOT_FOUND, "대상 위험성평가 건을 찾을 수 없습니다.")
    // 404: 대상 개선항목 없음 (또는 사업장 스코프 밖)
    , RISKIMPR_404_002(HttpStatus.NOT_FOUND, "대상 개선항목을 찾을 수 없습니다.")
    // 422: 지속개선대상(005) 상태가 아니면 개선완료 불가
    , RISKIMPR_422_001(HttpStatus.UNPROCESSABLE_ENTITY, "지속개선대상 상태에서만 개선완료할 수 있습니다.")
    // 422: 개선 후 위험도가 매우낮음(1~3) 이 아니면 개선완료 불가 (D1)
    , RISKIMPR_422_002(HttpStatus.UNPROCESSABLE_ENTITY, "개선 후 위험도가 '매우낮음'(1~3)일 때만 개선완료할 수 있습니다.")
    // 422: 개선완료(003)/미처리대상(004) 상태의 평가는 개선항목 편집 불가
    , RISKIMPR_422_003(HttpStatus.UNPROCESSABLE_ENTITY, "개선완료된 위험성평가는 개선항목을 편집할 수 없습니다.")
    // 422: 개선항목이 1건도 없으면 개선완료 불가
    , RISKIMPR_422_004(HttpStatus.UNPROCESSABLE_ENTITY, "개선항목이 1건 이상 등록되어야 개선완료할 수 있습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    RiskImprErrorCode(HttpStatus httpStatus, String message) {
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
