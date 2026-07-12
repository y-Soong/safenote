package com.prafta.web.risk.riskimpr01.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 개선항목 upsert 요청.
 * 사진은 risk03 선례와 동일하게 JSON 본문에 Base64(itemBase64) 로 담아 받는다.
 * improvementSeq 가 null/공백이면 신규(INSERT), 값이 있으면 수정(UPDATE).
 * cmpnyCd 는 JWT 에서만 도출(IDOR 차단).
 * 입력 검증(Low-2): 점수 범위/일자 형식/개선내용 길이를 Bean Validation 으로 강제한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ImprovementItemSaveRequest {
    private String siteCd;
    private String processCd;
    private String assessmentCd;
    private Integer improvementSeq;     // null = 신규
    // 개선일자(선택): null/빈값 허용, 값이 있으면 YYYYMMDD 8자리만 허용
    @Pattern(regexp = "^(\\d{8})?$")
    private String improveDate;
    // 개선내용: 컬럼 길이(varchar(500)) 일치
    @Size(max = 500)
    private String improveDesc;
    @Min(1) @Max(5)
    private Integer likelihoodScore;    // 개선 후 발생빈도(1~5)
    @Min(1) @Max(4)
    private Integer severityScore;      // 개선 후 중대성(1~4)
    private String riskLv;              // 개선 후 위험도LEVEL(빈도×강도)

    // 사진(선택) — Base64 인코딩 + 원본 파일명
    private String itemBase64;
    private String itemOriginalFilename;
}
