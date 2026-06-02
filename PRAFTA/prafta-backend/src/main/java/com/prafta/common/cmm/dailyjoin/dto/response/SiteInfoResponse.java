package com.prafta.common.cmm.dailyjoin.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 일일사용자 회원가입 - 회사/사업장 정보 응답.
 */
@Value
@Builder
public class SiteInfoResponse {
    String cmpnyCd;
    String cmpnyNm;
    String siteCd;
    String siteNm;
}
