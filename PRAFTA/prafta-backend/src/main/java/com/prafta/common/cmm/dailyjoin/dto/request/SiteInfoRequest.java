package com.prafta.common.cmm.dailyjoin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일일사용자 회원가입 - 회사/사업장 정보 조회 요청.
 * joinCd({회사코드}-{사업장코드5자리})를 프론트에서 분리하여 전달한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class SiteInfoRequest {
    private String cmpnyCd;
    private String siteCd;
}
