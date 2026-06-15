package com.prafta.app.safety.admin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * H5 위험성평가 상태전환 요청 (json).
 *
 * <p>리소스 키(siteCd/processCd/assessmentCd)는 본문으로 받되 서버가 CMPNY+SITE 스코프로 재검증한다(IDOR).
 *    cmpnyCd/userCd/authCd 는 신뢰하지 않고 JWT 에서만 도출한다.
 *
 * <p>전이 규칙(서버 강제, plan §4.5): 001->002(개선예정일+임시조치 필수) / 002->003 / ->004(폐기).
 */
@Getter
@Setter
@NoArgsConstructor
public class RiskStatusRequest {
    private String siteCd;
    private String processCd;
    private String assessmentCd;
    private String targetStatus;     // '002' | '003' | '004'
    private String revalDate;        // 'YYYY-MM-DD' 또는 'YYYYMMDD' (002 전환 필수)
    private String revalBeforeDesc;  // 임시조치 내용(002 전환 필수, 004 선택)
}
