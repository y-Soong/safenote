package com.prafta.web.risk.risklink01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * L1 연결 후보 아차사고 검색 요청.
 * 같은 사업장(siteCd) + 완료(REPORT_STATUS_CD='400') + 해당 평가건에 아직 미연결 + (검색어 옵션).
 * cmpnyCd 는 신뢰하지 않고 JWT 에서만 도출(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class AvailableNearMissRequest {
    private String siteCd;
    private String processCd;     // 이미 연결 제외 판정용(평가 키)
    private String assessmentCd;  // 이미 연결 제외 판정용(평가 키)
    private String keyword;       // 경위(DESCRIPTION)/장소(LOCATION_DESC) LIKE 검색어
}
