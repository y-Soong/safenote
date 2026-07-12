package com.prafta.web.risk.riskimpr01.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 개선완료(005→003 전이) 요청.
 * 최종 개선 후 위험도(reval*)를 tb_risk_assessment.REVAL_* 로 승격한다.
 * 서버가 개선 후 위험도 매우낮음(1~3) 가드(D1) + 005 상태 + 개선항목 1건 이상을 검증한다.
 * 위험도(REVAL_RISK_LV)는 클라 revalRiskLv 를 신뢰하지 않고 서버에서 빈도×강도로 재계산한다(Low-1B).
 * cmpnyCd 는 JWT 에서만 도출(IDOR 차단).
 * 입력 검증(Low-2): 점수 범위/일자 형식/개선내용 길이를 Bean Validation 으로 강제한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ImprovementCompleteRequest {
    private String siteCd;
    private String processCd;
    private String assessmentCd;
    @Min(1) @Max(5)
    private Integer revalLikelihoodScore;   // 개선 후 발생빈도(1~5)
    @Min(1) @Max(4)
    private Integer revalSeverityScore;     // 개선 후 중대성(1~4)
    private String revalRiskLv;             // 개선 후 위험도LEVEL(클라값은 미신뢰, 서버 재계산)
    // 개선내용(요약): REVAL_DESC 컬럼 길이(varchar(500)) 일치
    @Size(max = 500)
    private String revalDesc;
    // 개선예정일(선택): null/빈값 허용, 값이 있으면 YYYYMMDD 8자리만 허용
    @Pattern(regexp = "^(\\d{8})?$")
    private String revalDate;
}
